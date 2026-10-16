package com.fasterxml.jackson.databind;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.cfg.DatatypeFeature;
import com.fasterxml.jackson.databind.cfg.DatatypeFeatures;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator.Validity;
import com.fasterxml.jackson.databind.type.PlaceholderForType;
import com.fasterxml.jackson.databind.type.ResolvedRecursiveType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.LRUMap;

/**
 * Shared base class for {@link DeserializationContext} and
 * {@link SerializerProvider}, context objects passed through data-binding
 * process. Designed so that some of implementations can rely on shared
 * aspects like access to secondary contextual objects like type factories
 * or handler instantiators.
 *
 * @since 2.2
 */
public abstract class DatabindContext
{
    private final static JavaType[] NO_TYPES = new JavaType[0];

    /**
     * Let's limit length of error messages, for cases where underlying data
     * may be very large -- no point in spamming logs with megabytes of meaningless
     * data.
     *
     * @since 2.9
     */
    private final static int MAX_ERROR_STR_LEN = 500;

    /*
    /**********************************************************
    /* Generic config access
    /**********************************************************
     */

    /**
     * Accessor to currently active configuration (both per-request configs
     * and per-mapper config).
     */
    public abstract MapperConfig<?> getConfig();

    /**
     * Convenience method for accessing serialization view in use (if any); equivalent to:
     *<pre>
     *   getConfig().getAnnotationIntrospector();
     *</pre>
     */
    public abstract AnnotationIntrospector getAnnotationIntrospector();

    /*
    /**********************************************************
    /* Access to specific config settings
    /**********************************************************
     */

    /**
     * Convenience method for checking whether specified Mapper
     * feature is enabled or not.
     * Shortcut for:
     *<pre>
     *  getConfig().isEnabled(feature);
     *</pre>
     */
    public abstract boolean isEnabled(MapperFeature feature);

    /**
     * Method for checking whether specified datatype
     * feature is enabled or not.
     *
     * @since 2.14
     */
    public abstract boolean isEnabled(DatatypeFeature feature);

    /**
     * @since 2.15
     */
    public abstract DatatypeFeatures getDatatypeFeatures();

    /**
     * Convenience method for accessing serialization view in use (if any); equivalent to:
     *<pre>
     *   getConfig().canOverrideAccessModifiers();
     *</pre>
     */
    public abstract boolean canOverrideAccessModifiers();

    /**
     * Accessor for locating currently active view, if any;
     * returns null if no view has been set.
     */
    public abstract Class<?> getActiveView();

    /**
     * @since 2.6
     */
    public abstract Locale getLocale();

    /**
     * @since 2.6
     */
    public abstract TimeZone getTimeZone();

    /**
     * @since 2.7
     */
    public abstract JsonFormat.Value getDefaultPropertyFormat(Class<?> baseType);

    /*
    /**********************************************************
    /* Generic attributes (2.3+)
    /**********************************************************
     */

    /**
     * Method for accessing attributes available in this context.
     * Per-call attributes have highest precedence; attributes set
     * via {@link ObjectReader} or {@link ObjectWriter} have lower
     * precedence.
     *
     * @param key Key of the attribute to get
     * @return Value of the attribute, if any; null otherwise
     *
     * @since 2.3
     */
    public abstract Object getAttribute(Object key);

    /**
     * Method for setting per-call value of given attribute.
     * This will override any previously defined value for the
     * attribute within this context.
     *
     * @param key Key of the attribute to set
     * @param value Value to set attribute to
     *
     * @return This context object, to allow chaining
     *
     * @since 2.3
     */
    public abstract DatabindContext setAttribute(Object key, Object value);

    /*
    /**********************************************************
    /* Type instantiation/resolution
    /**********************************************************
     */

    /**
     * Convenience method for constructing {@link JavaType} for given JDK
     * type (usually {@link java.lang.Class})
     */
    public JavaType constructType(Type type) {
        if (type == null) {
            return null;
        }
        return getTypeFactory().constructType(type);
    }

    /**
     * Convenience method for constructing subtypes, retaining generic
     * type parameter (if any).
     *<p>
     * Note: since 2.11 handling has varied a bit across serialization, deserialization.
     */
    public abstract JavaType constructSpecializedType(JavaType baseType, Class<?> subclass);

    /**
     * Lookup method called when code needs to resolve class name from input;
     * usually simple lookup.
     * Note that unlike {@link #resolveAndValidateSubType} this method DOES NOT
     * validate subtype against configured {@link PolymorphicTypeValidator}: usually
     * because such check has already been made.
     *
     * @since 2.9
     */
    public JavaType resolveSubType(JavaType baseType, String subClassName)
        throws JsonMappingException
    {
        // 30-Jan-2010, tatu: Most ids are basic class names; so let's first
        //    check if any generics info is added; and only then ask factory
        //    to do translation when necessary
        if (subClassName.indexOf('<') > 0) {
            // note: may want to try combining with specialization (esp for EnumMap)?
            // 17-Aug-2017, tatu: As per [databind#1735] need to ensure assignment
            //    compatibility -- needed later anyway, and not doing so may open
            //    security issues.
            JavaType t = getTypeFactory().constructFromCanonical(subClassName);
            if (t.isTypeOrSubTypeOf(baseType.getRawClass())) {
                return t;
            }
        } else {
            Class<?> cls;
            try {
                cls =  getTypeFactory().findClass(subClassName);
            } catch (ClassNotFoundException e) { // let caller handle this problem
                return null;
            } catch (Exception e) {
                throw invalidTypeIdException(baseType, subClassName, String.format(
                        "problem: (%s) %s",
                        e.getClass().getName(),
                        ClassUtil.exceptionMessage(e)));
            }
            if (baseType.isTypeOrSuperTypeOf(cls)) {
                return getTypeFactory().constructSpecializedType(baseType, cls);
            }
        }
        throw invalidTypeIdException(baseType, subClassName, "Not a subtype");
    }

    /**
     * Lookup method similar to {@link #resolveSubType} but one that also validates
     * that resulting subtype is valid according to given {@link PolymorphicTypeValidator}.
     *
     * @since 2.10
     */
    public JavaType resolveAndValidateSubType(JavaType baseType, String subClass,
            PolymorphicTypeValidator ptv)
        throws JsonMappingException
    {
        // Off-line the special case of generic (parameterized) type:
        final int ltIndex = subClass.indexOf('<');
        if (ltIndex > 0) {
            return _resolveAndValidateGeneric(baseType, subClass, ptv, ltIndex);
        }
        final MapperConfig<?> config = getConfig();
        PolymorphicTypeValidator.Validity vld = ptv.validateSubClassName(config, baseType, subClass);
        if (vld == Validity.DENIED) {
            return _throwSubtypeNameNotAllowed(baseType, subClass, ptv);
        }
        final Class<?> cls;
        try {
            cls =  getTypeFactory().findClass(subClass);
        } catch (ClassNotFoundException e) { // let caller handle this problem
            return null;
        } catch (Exception e) {
            throw invalidTypeIdException(baseType, subClass, String.format(
                    "problem: (%s) %s",
                    e.getClass().getName(),
                    ClassUtil.exceptionMessage(e)));
        }
        if (!baseType.isTypeOrSuperTypeOf(cls)) {
            return _throwNotASubtype(baseType, subClass);
        }
        final JavaType subType = config.getTypeFactory().constructSpecializedType(baseType, cls);
        // May skip check if type was allowed by subclass name already
        if (vld == Validity.INDETERMINATE) {
            vld = ptv.validateSubType(config, baseType, subType);
            if (vld != Validity.ALLOWED) {
                return _throwSubtypeClassNotAllowed(baseType, subClass, ptv);
            }
        }
        return subType;
    }

    private JavaType _resolveAndValidateGeneric(JavaType baseType, String subClass,
            PolymorphicTypeValidator ptv, int ltIndex)
        throws JsonMappingException
    {
        final MapperConfig<?> config = getConfig();
        // 24-Apr-2019, tatu: Not 100% sure if we should pass name with type parameters
        //    or not, but guessing it's more convenient not to have to worry about it so
        //    strip out
        PolymorphicTypeValidator.Validity vld = ptv.validateSubClassName(config, baseType, subClass.substring(0, ltIndex));
        if (vld == Validity.DENIED) {
            return _throwSubtypeNameNotAllowed(baseType, subClass, ptv);
        }
        JavaType subType = getTypeFactory().constructFromCanonical(subClass);
        if (!subType.isTypeOrSubTypeOf(baseType.getRawClass())) {
            return _throwNotASubtype(baseType, subClass);
        }
        // Unless we were approved already by name, check that actual sub-class acceptable:
        if (vld != Validity.ALLOWED) {
            if (ptv.validateSubType(config, baseType, subType) != Validity.ALLOWED) {
                return _throwSubtypeClassNotAllowed(baseType, subClass, ptv);
            }
        }
        // [databind#5988]: even when the container class is approved (by name or
        // by sub-type matcher), the attacker-supplied type parameters must still
        // be validated -- otherwise a name-based allow-list for a safe container
        // (e.g. "java.util.ArrayList") can be bypassed by smuggling a gadget
        // class as a type argument (e.g. "java.util.ArrayList<EvilGadget>").
        // The container itself was already validated above. Validate its type
        // parameters against the corresponding declared base parameters.
        _validateTypeParameters(baseType, subClass, baseType, subType, ptv, config);
        return subType;
    }

    /**
     * Validate the type arguments of {@code subType} against the type parameters
     * of {@code declaredType}.
     *<p>
     * A subtype declaration may reorder, fix, nest or drop the type parameters
     * it inherits, so the arguments cannot be compared position by position.
     * Instead the subtype is resolved once with a placeholder for each of its
     * own type parameters, and the projection of that placeholder type onto
     * {@code declaredType} is walked alongside {@code declaredType}
     * (see {@link #_validateProjectedType}). A placeholder met during the walk
     * stands for the argument the type id gave at that position, which is
     * validated in full against the declared type found there. Any other type
     * met comes from the class declarations, not from the type id, and only has
     * to fit the declared type. Own arguments the projection never reaches are
     * validated against the unknown type.
     *
     * @param polymorphicBase Base type the complete type id is resolved against;
     *    only used for error messages
     * @param typeId Complete type id being resolved; only used for error messages
     * @param declaredType Type whose type parameters {@code subType} has to be
     *    compatible with
     */
    private void _validateTypeParameters(JavaType polymorphicBase, String typeId,
            JavaType declaredType, JavaType subType,
            PolymorphicTypeValidator ptv, MapperConfig<?> config)
        throws JsonMappingException
    {
        // `TypeParser` cannot produce an array with type arguments today; handled
        // anyway so the check does not depend on that.
        if (subType.isArrayType()) {
            _validateTypeParameter(polymorphicBase, typeId,
                    _componentTypeOrUnknown(declaredType), subType.getContentType(), ptv, config);
            return;
        }
        final int count = subType.containedTypeCount();
        // The walk only records what it reaches; the descent into the arguments
        // happens after it returns, to keep the stack depth per nesting level
        // the same as before
        final ProjectedArguments walk;
        if (declaredType.isJavaLangObject()) {
            // A projection onto `Object` has no type arguments, so it reaches none
            walk = new ProjectedArguments(NO_TYPES, subType);
        } else if ((count > 0) && (declaredType.getRawClass() == subType.getRawClass())) {
            // The subtype is its own projection: the arguments line up
            walk = new ProjectedArguments(NO_TYPES, subType);
            for (int i = 0; i < count; ++i) {
                walk.reach(i, declaredType.containedTypeOrUnknown(i),
                        _objectAllowedAt(declaredType, i));
            }
        } else {
            final JavaType[] placeholders;
            final JavaType projected;
            if (count == 0) {
                // Nothing to substitute: the resolved subtype is its own projection
                placeholders = NO_TYPES;
                projected = subType;
            } else {
                placeholders = new JavaType[count];
                for (int i = 0; i < count; ++i) {
                    placeholders[i] = new PlaceholderForType(i);
                }
                projected = _placeholderType(subType.getRawClass(), placeholders);
            }
            walk = new ProjectedArguments(placeholders, subType);
            _validateProjectedType(polymorphicBase, typeId, declaredType, projected, false, walk);
        }
        for (int i = 0, n = walk.reachedArguments.size(); i < n; ++i) {
            _validateTypeParameter(polymorphicBase, typeId,
                    walk.reachedDeclaredTypes.get(i), walk.reachedArguments.get(i), ptv, config);
        }
        for (int i = 0; i < count; ++i) {
            if (!walk.reached[i]) {
                _validateTypeParameter(polymorphicBase, typeId,
                        getTypeFactory().unknownType(), walk.ownArguments[i], ptv, config);
            }
        }
    }

    /**
     * Whether a type argument of {@code Object} fits type parameter {@code index}
     * of {@code declaredType} whatever that parameter is: {@link TypeFactory}
     * accepts {@code Object} for the key type of any map-like type (see
     * [databind#1964] and [databind#2422]), since keys are read from property
     * names either way.
     */
    private static boolean _objectAllowedAt(JavaType declaredType, int index) {
        return (index == 0) && declaredType.isMapLikeType();
    }

    /**
     * State of one walk over a placeholder projection, see
     * {@link #_validateProjectedType}.
     */
    private static final class ProjectedArguments
    {
        /**
         * Placeholders standing for the subtype's own type parameters, matched
         * by identity
         */
        final JavaType[] placeholders;

        /**
         * Type arguments the type id gave for those parameters
         */
        final JavaType[] ownArguments;

        /**
         * For each own argument, whether {@code Object} fits whatever type
         * is declared for it
         */
        final boolean[] objectAllowed;

        /**
         * For each own argument, whether the projection reached it
         */
        final boolean[] reached;

        /**
         * Own arguments the projection reached, once per position it reached
         * them at (a subtype may use one type parameter several times), with the
         * declared type found at that position
         */
        final List<JavaType> reachedArguments = new ArrayList<>();
        final List<JavaType> reachedDeclaredTypes = new ArrayList<>();

        /**
         * Self-referenced types already walked, to end a cycle
         */
        final Set<JavaType> seen = Collections.newSetFromMap(
                new IdentityHashMap<JavaType, Boolean>());

        ProjectedArguments(JavaType[] placeholders, JavaType subType) {
            this.placeholders = placeholders;
            final int count = subType.containedTypeCount();
            ownArguments = new JavaType[count];
            for (int i = 0; i < count; ++i) {
                ownArguments[i] = subType.containedType(i);
            }
            objectAllowed = new boolean[count];
            reached = new boolean[count];
            // `ClassNameIdResolver` writes `EnumMap<E,Object>` as the id of every
            // `EnumMap` value, whatever value type is declared
            if ((subType.getRawClass() == EnumMap.class) && (count == 2)) {
                objectAllowed[1] = true;
            }
        }

        /**
         * @param objectAllowedHere Whether {@code Object} fits at the position
         *    reached whatever {@code declaredType} is
         */
        void reach(int index, JavaType declaredType, boolean objectAllowedHere) {
            JavaType argument = ownArguments[index];
            if (argument.isJavaLangObject() && (objectAllowedHere || objectAllowed[index])) {
                // Fits here whatever is declared, and names no class to construct
                declaredType = TypeFactory.unknownType();
            }
            reached[index] = true;
            reachedArguments.add(argument);
            reachedDeclaredTypes.add(declaredType);
        }
    }

    /**
     * Resolve {@code rawType} with the given placeholders as its type arguments.
     *<p>
     * The resolution goes through an empty type cache so that the result depends
     * on the class declarations alone. {@link TypeFactory} treats a reference to
     * a class it is still resolving as a self-reference unless the cache already
     * holds that exact type; the outcome of this check must not change with what
     * other type ids were resolved before it. It also keeps the placeholder types,
     * which no later lookup can match, out of the shared cache.
     */
    private JavaType _placeholderType(Class<?> rawType, JavaType[] placeholders)
    {
        return getTypeFactory().withCache(new LRUMap<Object, JavaType>(4, 16))
                .constructParametricType(rawType, placeholders);
    }

    /**
     * Walk one type of the placeholder projection next to the declared type at
     * the same position; see {@link #_validateTypeParameters} for the idea.
     * A placeholder is recorded in {@code walk} together with the declared type;
     * any other type is fixed by the class declarations, so not the type id's
     * choice, and only has to fit the declared type.
     */
    private void _validateProjectedType(JavaType polymorphicBase, String typeId,
            JavaType declaredType, JavaType projected, boolean objectAllowed,
            ProjectedArguments walk)
        throws JsonMappingException
    {
        final JavaType[] placeholders = walk.placeholders;
        for (int i = 0; i < placeholders.length; ++i) {
            if (projected == placeholders[i]) {
                walk.reach(i, declaredType, objectAllowed);
                return;
            }
        }
        if (projected instanceof ResolvedRecursiveType) {
            // A reference to a type still being resolved (`SelfSupplier<T>`
            // implementing `Supplier<SelfSupplier<T>>`) reports no type
            // parameters of its own; walk the type it stands for, once.
            JavaType referenced = ((ResolvedRecursiveType) projected).getSelfReferencedType();
            if ((referenced == null) || !walk.seen.add(referenced)) {
                return;
            }
            projected = referenced;
        }
        if (projected.isArrayType()) {
            // The component decides whether `T[]` fits a declared array type
            if (!declaredType.isArrayType()) {
                _verifyDeclaredType(polymorphicBase, typeId, declaredType, projected, false);
            }
            _validateProjectedType(polymorphicBase, typeId,
                    _componentTypeOrUnknown(declaredType), projected.getContentType(), false, walk);
            return;
        }
        _verifyDeclaredType(polymorphicBase, typeId, declaredType, projected, objectAllowed);
        JavaType projectedBase = projected.findSuperType(declaredType.getRawClass());
        if (projectedBase != null) {
            for (int i = 0, n = projectedBase.containedTypeCount(); i < n; ++i) {
                _validateProjectedType(polymorphicBase, typeId,
                        declaredType.containedTypeOrUnknown(i), projectedBase.containedType(i),
                        _objectAllowedAt(declaredType, i), walk);
            }
        }
    }

    /**
     * Check that {@code type} fits {@code declaredType}: it has to be a subtype,
     * a primitive counting as its wrapper since that is what gets deserialized.
     *
     * @param objectAllowed Whether {@code Object} fits whatever {@code declaredType} is
     */
    private void _verifyDeclaredType(JavaType polymorphicBase, String typeId,
            JavaType declaredType, JavaType type, boolean objectAllowed)
        throws JsonMappingException
    {
        if (declaredType.isJavaLangObject()) {
            return;
        }
        Class<?> raw = type.getRawClass();
        if (raw == Object.class) {
            if (objectAllowed) {
                return;
            }
        } else if (raw.isPrimitive()) {
            raw = (raw == Void.TYPE) ? Void.class : ClassUtil.wrapperType(raw);
        }
        if (!declaredType.getRawClass().isAssignableFrom(raw)) {
            throw invalidTypeIdException(polymorphicBase, typeId,
                    _typeParameterDescription(declaredType, type)
                    + " is not a subtype of its declared type");
        }
    }

    private JavaType _componentTypeOrUnknown(JavaType declaredType) {
        return declaredType.isArrayType()
                ? declaredType.getContentType() : getTypeFactory().unknownType();
    }

    /**
     * Helper for [databind#5988]: validate one type parameter of a generic
     * type id, then recurse into its own type parameters or array component
     * (for nested generics like {@code Map<String, List<Evil>>} or
     * {@code List<String[]>}).
     *<p>
     * {@code declaredType} is the type parameter the base type declares at the
     * same position (or the unknown type if it declares none): {@code param}
     * must be a subtype of it, and the {@link PolymorphicTypeValidator} is
     * consulted with it as the base type. A base type the validator approves
     * outright approves the argument; a base type it denies does not reject
     * the argument, since {@code BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES} and
     * {@code denyForExactBaseType} describe what may not be declared
     * polymorphic, and the declared type parameter was not. Name-based and
     * class-based allow rules are both consulted (matching the top-level
     * container check), so a name-prefix configuration like
     * {@code allowIfSubType("com.example.")} applies to type parameters as well.
     *<p>
     * Primitive types and {@code Object} only have to fit {@code declaredType},
     * a primitive as its wrapper since that is what gets deserialized: a
     * primitive cannot name an attacker-selected class, and {@code Object} is
     * the canonical resolution of wildcards and unbound parameters, which cannot
     * themselves carry attacker-controlled types. {@code Object} also fits the
     * key type of a map-like type and the value type of an {@code EnumMap}
     * whatever is declared, see {@link #_objectAllowedAt} and
     * {@link ProjectedArguments}.
     * Enum types are exempt from the {@link PolymorphicTypeValidator} only:
     * they are JVM-managed singletons resolved by name lookup (no
     * attacker-controlled constructor or setter runs), so they cannot serve as
     * gadget classes -- and a name-prefix configuration that allow-lists a
     * container like {@code EnumSet} should not have to also allow-list every
     * enum class that may legitimately appear as its element. They are still
     * checked against {@code declaredType} and by {@link #_validateGenericSubType}.
     *
     * @param polymorphicBase Base type the complete type id is resolved against;
     *    only used for error messages
     * @param typeId Complete type id being resolved; only used for error messages
     *
     * @since 2.18.8
     */
    private void _validateTypeParameter(JavaType polymorphicBase, String typeId,
            JavaType declaredType, JavaType param,
            PolymorphicTypeValidator ptv, MapperConfig<?> config)
        throws JsonMappingException
    {
        _verifyDeclaredType(polymorphicBase, typeId, declaredType, param, false);
        if (param.isPrimitive() || param.isJavaLangObject()) {
            return;
        }
        if (!param.isEnumType()) {
            // A base type the validator approves outright approves every subtype;
            // a denied base type only says what may not be declared polymorphic,
            // which the declared type parameter is not, so it decides nothing here.
            // Otherwise consult the name-based allow rules first (mirrors the
            // container check in _resolveAndValidateGeneric), then fall back to
            // the class-based check so all configured matchers can approve the
            // parameter.
            Validity vld = (ptv.validateBaseType(config, declaredType) == Validity.ALLOWED)
                    ? Validity.ALLOWED
                    : ptv.validateSubClassName(config, declaredType, param.getRawClass().getName());
            if (vld == Validity.DENIED) {
                throw _typeParameterDeniedException(polymorphicBase, typeId,
                        declaredType, param, ptv);
            }
            if (vld != Validity.ALLOWED) {
                if (ptv.validateSubType(config, declaredType, param) != Validity.ALLOWED) {
                    throw _typeParameterDeniedException(polymorphicBase, typeId,
                            declaredType, param, ptv);
                }
            }
        }
        _validateGenericSubType(param);
        if (param.isArrayType()) {
            _validateTypeParameter(polymorphicBase, typeId,
                    _componentTypeOrUnknown(declaredType), param.getContentType(), ptv, config);
        } else {
            _validateTypeParameters(polymorphicBase, typeId, declaredType, param, ptv, config);
        }
    }

    private JsonMappingException _typeParameterDeniedException(JavaType polymorphicBase,
            String typeId, JavaType declaredType, JavaType param,
            PolymorphicTypeValidator ptv)
    {
        return invalidTypeIdException(polymorphicBase, typeId,
                "Configured `PolymorphicTypeValidator` (of type " + ClassUtil.classNameOf(ptv)
                + ") denied resolution of " + _typeParameterDescription(declaredType, param));
    }

    private static String _typeParameterDescription(JavaType declaredType, JavaType param)
    {
        return "type parameter " + ClassUtil.getTypeDescription(param)
                + " (declared as " + ClassUtil.getTypeDescription(declaredType) + ")";
    }

    /**
     * Hook called for each type parameter of a generic type id after the checks
     * in {@code _validateTypeParameter} passed (enums skip the
     * {@link PolymorphicTypeValidator} there). The deserialization side applies
     * its built-in deny list of known unsafe classes here; the default
     * implementation does nothing.
     *
     * @since 2.18.12
     */
    protected void _validateGenericSubType(JavaType type) throws JsonMappingException { }

    protected <T> T _throwNotASubtype(JavaType baseType, String subType) throws JsonMappingException {
        throw invalidTypeIdException(baseType, subType, "Not a subtype");
    }

    protected <T> T _throwSubtypeNameNotAllowed(JavaType baseType, String subType,
            PolymorphicTypeValidator ptv) throws JsonMappingException {
        throw invalidTypeIdException(baseType, subType,
                "Configured `PolymorphicTypeValidator` (of type "+ClassUtil.classNameOf(ptv)+") denied resolution");
    }

    protected <T> T _throwSubtypeClassNotAllowed(JavaType baseType, String subType,
            PolymorphicTypeValidator ptv) throws JsonMappingException {
        throw invalidTypeIdException(baseType, subType,
                "Configured `PolymorphicTypeValidator` (of type "+ClassUtil.classNameOf(ptv)+") denied resolution");
    }

    /**
     * Helper method for constructing exception to indicate that given type id
     * could not be resolved to a valid subtype of specified base type.
     * Most commonly called during polymorphic deserialization.
     *<p>
     * Note that most of the time this method should NOT be called directly: instead,
     * method <code>handleUnknownTypeId()</code> should be called which will call this method
     * if necessary.
     *
     * @since 2.9
     */
    protected abstract JsonMappingException invalidTypeIdException(JavaType baseType, String typeId,
            String extraDesc);

    public abstract TypeFactory getTypeFactory();

    /*
    /**********************************************************
    /* Helper object construction
    /**********************************************************
     */

    public ObjectIdGenerator<?> objectIdGeneratorInstance(Annotated annotated,
            ObjectIdInfo objectIdInfo)
        throws JsonMappingException
    {
        Class<?> implClass = objectIdInfo.getGeneratorType();
        final MapperConfig<?> config = getConfig();
        HandlerInstantiator hi = config.getHandlerInstantiator();
        ObjectIdGenerator<?> gen = (hi == null) ? null : hi.objectIdGeneratorInstance(config, annotated, implClass);
        if (gen == null) {
            gen = (ObjectIdGenerator<?>) ClassUtil.createInstance(implClass,
                    config.canOverrideAccessModifiers());
        }
        return gen.forScope(objectIdInfo.getScope());
    }

    public ObjectIdResolver objectIdResolverInstance(Annotated annotated, ObjectIdInfo objectIdInfo)
    {
        Class<? extends ObjectIdResolver> implClass = objectIdInfo.getResolverType();
        final MapperConfig<?> config = getConfig();
        HandlerInstantiator hi = config.getHandlerInstantiator();
        ObjectIdResolver resolver = (hi == null) ? null : hi.resolverIdGeneratorInstance(config, annotated, implClass);
        if (resolver == null) {
            resolver = ClassUtil.createInstance(implClass, config.canOverrideAccessModifiers());
        }

        return resolver;
    }

    /**
     * Helper method to use to construct a {@link Converter}, given a definition
     * that may be either actual converter instance, or Class for instantiating one.
     *
     * @since 2.2
     */
    @SuppressWarnings("unchecked")
    public Converter<Object,Object> converterInstance(Annotated annotated,
            Object converterDef)
        throws JsonMappingException
    {
        if (converterDef == null) {
            return null;
        }
        if (converterDef instanceof Converter<?,?>) {
            return (Converter<Object,Object>) converterDef;
        }
        if (!(converterDef instanceof Class)) {
            throw new IllegalStateException("AnnotationIntrospector returned Converter definition of type "
                    +converterDef.getClass().getName()+"; expected type Converter or Class<Converter> instead");
        }
        Class<?> converterClass = (Class<?>)converterDef;
        // there are some known "no class" markers to consider too:
        if (converterClass == Converter.None.class || ClassUtil.isBogusClass(converterClass)) {
            return null;
        }
        if (!Converter.class.isAssignableFrom(converterClass)) {
            throw new IllegalStateException("AnnotationIntrospector returned Class "
                    +converterClass.getName()+"; expected Class<Converter>");
        }
        final MapperConfig<?> config = getConfig();
        HandlerInstantiator hi = config.getHandlerInstantiator();
        Converter<?,?> conv = (hi == null) ? null : hi.converterInstance(config, annotated, converterClass);
        if (conv == null) {
            conv = (Converter<?,?>) ClassUtil.createInstance(converterClass,
                    config.canOverrideAccessModifiers());
        }
        return (Converter<Object,Object>) conv;
    }

    /*
    /**********************************************************
    /* Error reporting
    /**********************************************************
     */

    /**
     * Helper method called to indicate a generic problem that stems from type
     * definition(s), not input data, or input/output state; typically this
     * means throwing a {@link com.fasterxml.jackson.databind.exc.InvalidDefinitionException}.
     *
     * @since 2.9
     */
    public abstract <T> T reportBadDefinition(JavaType type, String msg) throws JsonMappingException;

    /**
     * @since 2.9
     */
    public <T> T reportBadDefinition(Class<?> type, String msg) throws JsonMappingException {
        return reportBadDefinition(constructType(type), msg);
    }

    /**
     * @since 2.18
     */
    public abstract <T> T reportBadTypeDefinition(BeanDescription bean,
            String msg, Object... msgArgs) throws JsonMappingException;

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * @since 2.9
     */
    protected final String _format(String msg, Object... msgArgs) {
        if (msgArgs.length > 0) {
            return String.format(msg, msgArgs);
        }
        return msg;
    }

    /**
     * @since 2.9
     */
    protected final String _truncate(String desc) {
        if (desc == null) {
            return "";
        }
        if (desc.length() <= MAX_ERROR_STR_LEN) {
            return desc;
        }
        return desc.substring(0, MAX_ERROR_STR_LEN) + "]...[" + desc.substring(desc.length() - MAX_ERROR_STR_LEN);
    }

    /**
     * @since 2.9
     */
    protected String _quotedString(String desc) {
        if (desc == null) {
            return "[N/A]";
        }
        // !!! should we quote it? (in case there are control chars, linefeeds)
        return String.format("\"%s\"", _truncate(desc));
    }

    /**
     * @since 2.9
     */
    protected String _colonConcat(String msgBase, String extra) {
        if (extra == null) {
            return msgBase;
        }
        return msgBase + ": " + extra;
    }

    /**
     * @since 2.9
     */
    protected String _desc(String desc) {
        if (desc == null) {
            return "[N/A]";
        }
        // !!! should we quote it? (in case there are control chars, linefeeds)
        return _truncate(desc);
    }
}
