package com.fasterxml.jackson.databind;

import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.cfg.DatatypeFeature;
import com.fasterxml.jackson.databind.cfg.DatatypeFeatures;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator.Validity;
import com.fasterxml.jackson.databind.jsontype.impl.ApprovedBaseTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.SubTypeValidator;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;

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
        // [CVE-2026-110879]: element type of an array type id is attacker-selected
        // as well and needs validation like type parameters of generic type ids
        // (approval of array type, by base type or by sub-type, does not extend to it)
        if (subType.isArrayType()) {
            if (!_configuredValidator(ptv).isArrayTypeAllowedExplicitly(config, baseType, subType)) {
                _validateArrayElementType(baseType, subClass, baseType, subType, ptv, config);
            } else {
                // [databind#6496]: explicit approval of array type covers its element
                // type, but not key/content types its class declares (`[LMyGadgetList;`)
                JavaType declaredType = baseType;
                JavaType elementType = subType;
                do {
                    declaredType = _declaredContentType(declaredType);
                    elementType = elementType.getContentType();
                } while (elementType.isArrayType());
                _validateDeclaredContentTypes(baseType, subClass, declaredType, elementType, ptv, config);
            }
        } else {
            // [databind#6496]: key/content types from class declaration (like `Gadget` for
            // `class MyGadgetList extends ArrayList<Gadget>`) need validation as well
            _validateDeclaredContentTypes(baseType, subClass, baseType, subType, ptv, config);
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
        // [CVE-2026-110879]: ... and validated as if named as a direct type id,
        // see `_validateContainedType()`
        _validateContainedTypes(baseType, subClass, subType, ptv, config);
        // [databind#6496]: ... as well as key/content types from class declaration,
        // which need not be type parameters (`class MyList<T> extends ArrayList<Gadget>`)
        _validateDeclaredContentTypes(baseType, subClass, baseType, subType, ptv, config);
        return subType;
    }

    // Approval of base type does not extend to contained types: need configured validator
    private static PolymorphicTypeValidator _configuredValidator(PolymorphicTypeValidator ptv) {
        if (ptv instanceof ApprovedBaseTypeValidator) {
            return ((ApprovedBaseTypeValidator) ptv).getTypeParameterValidator();
        }
        return ptv;
    }

    // Declared type of array elements: element type of declared type if it is an
    // array type; `Object` for other declared types (which can only be `Object`,
    // `Serializable` or `Cloneable`, supertypes of all arrays); `null` (none) if
    // no declared type
    private static JavaType _declaredContentType(JavaType declaredType) {
        if (declaredType == null) {
            return null;
        }
        if (declaredType.isArrayType()) {
            return declaredType.getContentType();
        }
        return TypeFactory.unknownType();
    }

    /**
     * Helper for [databind#5988]: validate all type parameters of given type,
     * recursively (for nested generics like {@code Map<String, List<Evil>>}
     * or {@code List<Evil[]>}).
     *
     * @since 2.18.12
     */
    private void _validateContainedTypes(JavaType baseType, String typeId, JavaType type,
            PolymorphicTypeValidator ptv, MapperConfig<?> config)
        throws JsonMappingException
    {
        for (int i = 0, n = type.containedTypeCount(); i < n; ++i) {
            _validateContainedType(baseType, typeId, null,
                    type.containedType(i), ptv, config);
        }
    }

    /**
     * Helper for validating a class named in a type id as type parameter (or as element
     * type of an array type): it gets the same checks a class named directly as type id
     * would get. Unless it is a safe type (see {@link #_isSafeTypeParameter}) or exactly
     * its declared type, the {@link SubTypeValidator} deny list applies, and:
     *<ul>
     * <li>If assignable to polymorphic base type, checked as if a direct type id for it
     *   </li>
     * <li>Otherwise checked by configured {@link PolymorphicTypeValidator} as a direct type
     *   id for its declared type (element type of declared array type for array
     *   elements; none -- validated as {@code Object} -- otherwise), starting with
     *   {@link PolymorphicTypeValidator#validateBaseType}: approval of polymorphic base
     *   type does not extend to it. {@link LaissezFaireSubTypeValidator} (which allows
     *   all subtypes of base type) only allows subtypes of declared type, if any
     *   </li>
     *</ul>
     * Assignment compatibility with declared type parameters of the base type is
     * NOT verified: classes named in the type id are the only attacker-selected
     * ones (other types come from the declared base type or from class
     * declarations), so validating them suffices to prevent instantiation of
     * unapproved types.
     *
     * @param baseType Polymorphic base type
     * @param declaredType Declared type of the contained type, if any ({@code null} if none)
     * @param ptv Validator used for validating type id as subtype of {@code baseType}
     *
     * @since 2.18.12
     */
    private void _validateContainedType(JavaType baseType, String typeId, JavaType declaredType,
            JavaType param, PolymorphicTypeValidator ptv, MapperConfig<?> config)
        throws JsonMappingException
    {
        if (!_isSafeTypeParameter(param)
                && !((declaredType != null) && param.hasRawClass(declaredType.getRawClass()))) {
            final PolymorphicTypeValidator vldr;
            final JavaType vldBaseType;
            Validity vld;
            if (param.isTypeOrSubTypeOf(baseType.getRawClass())) {
                // validity of base type itself already checked
                vldr = ptv;
                vldBaseType = baseType;
                vld = Validity.INDETERMINATE;
            } else if ((ptv instanceof ApprovedBaseTypeValidator)
                    && (declaredType != null) && !ClassUtil.isArraySuperType(declaredType.getRawClass())
                    && param.isTypeOrSubTypeOf(declaredType.getRawClass())) {
                // Approval of base type (declared array type) covers subtypes of its
                // element type (except for `Object[]`, `Serializable[]` and
                // `Cloneable[]`, which would cover everything)
                vldr = ptv;
                vldBaseType = declaredType;
                vld = Validity.ALLOWED;
            } else {
                vldr = _configuredValidator(ptv);
                vldBaseType = (declaredType == null) ? TypeFactory.unknownType() : declaredType;
                if (vldr instanceof LaissezFaireSubTypeValidator) {
                    // Allows all subtypes of declared type (element type of declared
                    // array type, including `Object` for `Object[]`), but nothing
                    // without one (type parameters)
                    if (declaredType == null) {
                        throw invalidTypeIdException(baseType, typeId,
                                "contained type " + ClassUtil.getTypeDescription(param)
                                + " not a subtype of base type (and no `PolymorphicTypeValidator`"
                                + " configured to allow it)");
                    }
                    vld = Validity.ALLOWED;
                } else {
                    vld = vldr.validateBaseType(config, vldBaseType);
                }
            }
            if ((vld == Validity.INDETERMINATE)
                    && _isApprovedSubType(vldr, config, vldBaseType, param)) {
                vld = Validity.ALLOWED;
            }
            if (vld != Validity.ALLOWED) {
                throw invalidTypeIdException(baseType, typeId,
                        "Configured `PolymorphicTypeValidator` (of type "
                                + ClassUtil.classNameOf(vldr)
                                + ") denied resolution of contained type "
                                + ClassUtil.getTypeDescription(param));
            }
            // NOTE: `BeanDeserializerFactory` also checks deny list for types it creates
            // bean deserializers for, but not all contained types get one (Map keys,
            // for example, may be deserialized via single-String constructor)
            // (same exception as `BeanDeserializerFactory` would throw)
            if (SubTypeValidator.instance().isIllegalType(param.getRawClass())) {
                reportBadDefinition(param, String.format(
                        "Illegal type (%s) to deserialize: prevented for security reasons",
                        param.getRawClass().getName()));
            }
        }
        if (param.isArrayType()) {
            _validateArrayElementType(baseType, typeId, declaredType, param, ptv, config);
        } else {
            _validateContainedTypes(baseType, typeId, param, ptv, config);
            // [databind#6496]: ... and key/content types its class declares
            _validateDeclaredContentTypes(baseType, typeId, declaredType, param, ptv, config);
        }
    }

    /**
     * Helper for [databind#6496]: validate key and content types of a Collection-,
     * Map- or reference type named by type id (directly or as type parameter) that
     * come from its class declaration (or ones it extends), like {@code Gadget} for
     * {@code class MyGadgetList extends ArrayList<Gadget>} or for
     * {@code class MyList<T> extends ArrayList<Gadget>}: approval of a container type
     * (often by a rule matching all containers) does not cover them. They are validated
     * by configured validator like type parameters named in type id (see
     * {@link #_validateContainedType}), except that:
     *<ul>
     * <li>Parts that come from type parameters are not validated here: ones named in
     *   type id are validated as such, and ones bound by declared base type come from
     *   it, not from type id
     *   </li>
     * <li>Key/content type exactly like that of declared base type is not validated,
     *   and approval of base type covers subtypes of it (as with arrays)
     *   </li>
     * <li>Explicit approval of the container class (see
     *   {@link PolymorphicTypeValidator#isContainerTypeAllowedExplicitly}) covers them
     *   </li>
     * <li>Default {@link LaissezFaireSubTypeValidator} does not validate them: it only
     *   limits classes named in type id (and declared structure of those classes is not
     *   validated, same as their properties)
     *   </li>
     *</ul>
     *
     * @param declaredType Declared type of {@code type} (polymorphic base type for
     *   type named directly by type id, element type of declared array type for array
     *   elements; {@code null} for type parameters)
     *
     * @since 2.18.13
     */
    private void _validateDeclaredContentTypes(JavaType baseType, String typeId,
            JavaType declaredType, JavaType type,
            PolymorphicTypeValidator ptv, MapperConfig<?> config)
        throws JsonMappingException
    {
        if (type.isArrayType()
                || ((type.getKeyType() == null) && (type.getContentType() == null))) {
            return;
        }
        final PolymorphicTypeValidator vldr = _configuredValidator(ptv);
        if ((vldr instanceof LaissezFaireSubTypeValidator)
                || vldr.isContainerTypeAllowedExplicitly(config, baseType, type)) {
            return;
        }
        // Type resolved without type parameters has the same declared parts, and ones
        // from type parameters differ (are type variable bounds): only walk parts of
        // `type` that match it. This also keeps validation linear wrt nesting depth of
        // type id: type parameters are never walked again
        final JavaType unparameterized = config.getTypeFactory().constructType(type.getRawClass());
        _validateDeclaredParts(baseType, typeId, declaredType, type, unparameterized, null,
                ptv, vldr, config);
    }

    // Walks key, content and type parameter types of `type` (except ones of
    // type parameters, or same as those of `declaredType`), in parallel with
    // `unparameterized`: those that come from class declarations. Ones that
    // also match `covered` (if any) come from declaration of explicitly approved
    // class, and are not validated
    private void _validateDeclaredParts(JavaType baseType, String typeId,
            JavaType declaredType, JavaType type, JavaType unparameterized,
            JavaType covered,
            PolymorphicTypeValidator ptv, PolymorphicTypeValidator vldr,
            MapperConfig<?> config)
        throws JsonMappingException
    {
        final JavaType keyType = type.getKeyType();
        final JavaType contentType = type.getContentType();
        if (!_isCovered(keyType, (covered == null) ? null : covered.getKeyType())) {
            _validateDeclaredPart(baseType, typeId,
                    (declaredType == null) ? null : declaredType.getKeyType(),
                    keyType, unparameterized.getKeyType(), ptv, vldr, config);
        }
        if (!_isCovered(contentType, (covered == null) ? null : covered.getContentType())) {
            _validateDeclaredPart(baseType, typeId,
                    (declaredType == null) ? null : declaredType.getContentType(),
                    contentType, unparameterized.getContentType(), ptv, vldr, config);
        }
        final int n = Math.min(type.containedTypeCount(), unparameterized.containedTypeCount());
        for (int i = 0; i < n; ++i) {
            final JavaType param = type.containedType(i);
            // (usually same as key or content type: no need to walk again)
            if ((param != keyType) && (param != contentType)
                    && !_isCovered(param, (covered == null) ? null : covered.containedType(i))) {
                _validateDeclaredPart(baseType, typeId, null,
                        param, unparameterized.containedType(i), ptv, vldr, config);
            }
        }
    }

    // Whether part of a type comes from declaration of an explicitly approved class:
    // same (by class) as for class resolved without type parameters
    private static boolean _isCovered(JavaType part, JavaType coveredPart) {
        return (part != null) && (coveredPart != null) && part.hasRawClass(coveredPart.getRawClass());
    }

    private void _validateDeclaredPart(JavaType baseType, String typeId,
            JavaType declaredType, JavaType type, JavaType unparameterized,
            PolymorphicTypeValidator ptv, PolymorphicTypeValidator vldr,
            MapperConfig<?> config)
        throws JsonMappingException
    {
        // From type parameter (so not class declaration), or exactly as declared?
        if ((type == null) || (unparameterized == null)
                || !type.hasRawClass(unparameterized.getRawClass())
                || type.equals(declaredType)) {
            return;
        }
        if (!_isSafeTypeParameter(type)) {
            final boolean allowed;
            if (type.isTypeOrSubTypeOf(baseType.getRawClass())) {
                // same as for direct type id
                allowed = _isApprovedSubType(ptv, config, baseType, type);
            } else if ((ptv instanceof ApprovedBaseTypeValidator)
                    && (declaredType != null) && !ClassUtil.isArraySuperType(declaredType.getRawClass())
                    && type.isTypeOrSubTypeOf(declaredType.getRawClass())) {
                // Approval of base type covers subtypes of its key/content type
                // (except for `Object`, `Serializable` and `Cloneable`)
                allowed = true;
            } else {
                // Validated as if direct type id for declared key/content type, if
                // subtype of it, or `Object`; but denial of base type (like of `Object`
                // by `DefaultBaseTypeLimitingValidator`) does not deny types explicitly
                // allowed
                final JavaType objectType = TypeFactory.unknownType();
                boolean ok = false;
                if ((declaredType != null) && type.isTypeOrSubTypeOf(declaredType.getRawClass())) {
                    ok = (vldr.validateBaseType(config, declaredType) == Validity.ALLOWED)
                            || _isApprovedSubType(vldr, config, declaredType, type);
                }
                allowed = ok || (vldr.validateBaseType(config, objectType) == Validity.ALLOWED)
                        || _isApprovedSubType(vldr, config, objectType, type);
            }
            if (!allowed) {
                throw invalidTypeIdException(baseType, typeId,
                        "Configured `PolymorphicTypeValidator` (of type "
                                + ClassUtil.classNameOf(vldr)
                                + ") denied resolution of contained type "
                                + ClassUtil.getTypeDescription(type));
            }
            // (same as for type parameters, see `_validateContainedType()`)
            if (SubTypeValidator.instance().isIllegalType(type.getRawClass())) {
                reportBadDefinition(type, String.format(
                        "Illegal type (%s) to deserialize: prevented for security reasons",
                        type.getRawClass().getName()));
            }
            // Explicit approval covers parts the container class itself declares,
            // but not its type parameters (from enclosing class declaration)
            if (vldr.isContainerTypeAllowedExplicitly(config, baseType, type)) {
                _validateDeclaredParts(baseType, typeId, declaredType, type, unparameterized,
                        config.getTypeFactory().constructType(type.getRawClass()),
                        ptv, vldr, config);
                return;
            }
        }
        _validateDeclaredParts(baseType, typeId, declaredType, type, unparameterized, null,
                ptv, vldr, config);
    }

    /**
     * Helper for validating element type of an array type: like other contained types
     * (including abstract types: they may still be deserializable, via
     * {@code @JsonDeserialize(as=...)} or factory methods). Approval of the array
     * type does not by itself cover the element type: only the validator can say
     * it does (see {@link PolymorphicTypeValidator#isArrayTypeAllowedExplicitly},
     * checked by the caller for array types named by type id). Inner dimensions
     * and array type parameters always get here.
     *
     * @param declaredType Declared type of the array, if any ({@code null} if none)
     *
     * @since 2.18.12
     */
    private void _validateArrayElementType(JavaType baseType, String typeId,
            JavaType declaredType, JavaType arrayType,
            PolymorphicTypeValidator ptv, MapperConfig<?> config)
        throws JsonMappingException
    {
        _validateContainedType(baseType, typeId, _declaredContentType(declaredType),
                arrayType.getContentType(), ptv, config);
    }

    private static boolean _isApprovedSubType(PolymorphicTypeValidator vldr,
            MapperConfig<?> config, JavaType baseType, JavaType subType)
        throws JsonMappingException
    {
        Validity vld = vldr.validateSubClassName(config, baseType, subType.getRawClass().getName());
        if (vld == Validity.INDETERMINATE) {
            vld = vldr.validateSubType(config, baseType, subType);
        }
        return (vld == Validity.ALLOWED);
    }

    /**
     * Types that may be used as type parameters without validation: ones that
     * cannot be used as "gadget" types, since no attacker-controlled
     * constructor or setter can be invoked.
     *<p>
     * {@code Object} is the canonical resolution of wildcards and unbound parameters;
     * enum types are JVM-managed singletons resolved by name lookup;
     * {@code String}, primitive types (including wrappers), {@code Number},
     * {@code BigDecimal}, {@code BigInteger} and {@code UUID} are handled by Jackson
     * as scalars (subtypes of {@code Number} other than these need type ids); and arrays
     * and {@code java.util} container (Collection, Map, reference) types are JDK types
     * whose contained types are validated separately.
     *<p>
     * [databind#6496]: also value types read from scalars by standard deserializers:
     * {@code java.time} types (from {@code java.time} package itself, not sub-packages),
     * {@code Date}, {@code Calendar}, {@code GregorianCalendar}, {@code Locale},
     * {@code Currency} and {@code TimeZone} (but not all of {@code java.util}:
     * some types there have side effects on construction, like {@code Timer});
     * {@code java.net.URI}, {@code java.io.File} and {@code java.nio.file.Path} (no file
     * access); {@code java.sql.Date}, {@code java.sql.Time} and {@code java.sql.Timestamp}
     * (matched by name: {@code java.sql} is an optional module); {@code CharSequence}
     * (read as {@code String}); and {@link JsonNode} (and its standard subtypes), read by
     * tree model deserializers. NOTE: not {@code Serializable}, although read as
     * {@code Object} (untyped values only) by default.
     * NOTE: not {@code java.net.URL} or {@code java.net.InetAddress}, use of which may
     * cause DNS lookups.
     */
    private static boolean _isSafeTypeParameter(JavaType param)
    {
        if (param.isJavaLangObject() || param.isEnumType() || param.isArrayType()) {
            return true;
        }
        final Class<?> raw = param.getRawClass();
        if ((param.isContainerType() || param.isReferenceType())
                && raw.getName().startsWith("java.util.")) {
            return true;
        }
        if ((raw == String.class) || (ClassUtil.primitiveType(raw) != null)
                || (raw == Number.class) || (raw == BigDecimal.class) || (raw == BigInteger.class) || (raw == UUID.class)) {
            return true;
        }
        // [databind#6496]: well-known value types
        if ((raw == Date.class) || (raw == Calendar.class) || (raw == GregorianCalendar.class)
                || (raw == Locale.class) || (raw == Currency.class) || (raw == TimeZone.class)
                || (raw == URI.class) || (raw == File.class) || (raw == Path.class)
                || (raw == CharSequence.class) || (raw == JsonNode.class)
                // only standard node types: others may have custom deserializers
                || (JsonNode.class.isAssignableFrom(raw)
                        && raw.getName().startsWith("com.fasterxml.jackson.databind.node."))) {
            return true;
        }
        final String name = raw.getName();
        if (name.startsWith("java.sql.")) {
            return name.equals("java.sql.Date") || name.equals("java.sql.Time")
                    || name.equals("java.sql.Timestamp");
        }
        return name.startsWith("java.time.") && (name.indexOf('.', "java.time.".length()) < 0);
    }

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
