package com.fasterxml.jackson.databind.cfg;

import java.text.DateFormat;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AccessorNamingStrategy;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Interface that defines functionality accessible through both
 * serialization and deserialization configuration objects;
 * accessors to mode-independent configuration settings
 * and such.
 * In addition, shared features are defined
 * in {@link MapperFeature}.
 *<p>
 * Small part of implementation is included here by aggregating
 * {@link BaseSettings} instance that contains configuration
 * that is shared between different types of instances.
 */
public abstract class MapperConfig<T extends MapperConfig<T>>
    implements ClassIntrospector.MixInResolver,
        java.io.Serializable
{
    private static final long serialVersionUID = 2L; // since 2.9

    /**
     * @since 2.7
     */
    protected final static JsonInclude.Value EMPTY_INCLUDE = JsonInclude.Value.empty();

    /**
     * @since 2.7
     */
    protected final static JsonFormat.Value EMPTY_FORMAT = JsonFormat.Value.empty();

    /**
     * Set of shared mapper features enabled.
     *<p>
     * NOTE: changed from {@code int} (in Jackson 2.12 and prior} to {@code long}
     * (2.13 and later)
     */
    protected final long _mapperFeatures;

    /**
     * Immutable container object for simple configuration settings.
     */
    protected final BaseSettings _base;

    /*
    /**********************************************************
    /* Life-cycle: constructors
    /**********************************************************
     */

    protected MapperConfig(BaseSettings base, long mapperFeatures)
    {
        _base = base;
        _mapperFeatures = mapperFeatures;
    }

    protected MapperConfig(MapperConfig<T> src, long mapperFeatures)
    {
        _base = src._base;
        _mapperFeatures = mapperFeatures;
    }

    protected MapperConfig(MapperConfig<T> src, BaseSettings base)
    {
        _base = base;
        _mapperFeatures = src._mapperFeatures;
    }

    protected MapperConfig(MapperConfig<T> src)
    {
        _base = src._base;
        _mapperFeatures = src._mapperFeatures;
    }

    /**
     * Method that calculates bit set (flags) of all features that
     * are enabled by default.
     */
    public static <F extends Enum<F> & ConfigFeature> int collectFeatureDefaults(Class<F> enumClass)
    {
        int flags = 0;
        for (F value : enumClass.getEnumConstants()) {
            if (value.enabledByDefault()) {
                flags |= value.getMask();
            }
        }
        return flags;
    }

    /*
    /**********************************************************
    /* Life-cycle: factory methods
    /**********************************************************
     */

    /**
     * Method for constructing and returning a new instance with specified
     * mapper features enabled.
     */
    public abstract T with(MapperFeature... features);

    /**
     * Method for constructing and returning a new instance with specified
     * mapper features disabled.
     */
    public abstract T without(MapperFeature... features);

    /**
     * @since 2.3
     */
    public abstract T with(MapperFeature feature, boolean state);

    /*
    /**********************************************************
    /* Configuration: simple features
    /**********************************************************
     */

    /**
     * Accessor for simple mapper features (which are shared for
     * serialization, deserialization)
     */
    public final boolean isEnabled(MapperFeature f) {
        return f.enabledIn(_mapperFeatures);
    }

    /**
     * "Bulk" access method for checking that all features specified by
     * mask are enabled.
     *
     * @since 2.3
     *
     * @deprecated Since 2.13 -- no replacement
     */
    @Deprecated
    public final boolean hasMapperFeatures(int featureMask) {
        return (_mapperFeatures & featureMask) == featureMask;
    }

    /**
     * Accessor for checking whether give {@link DatatypeFeature}
     * is enabled or not.
     *
     * @param feature Feature to check
     *
     * @return True if feature is enabled; false otherwise
     *
     * @since 2.15
     */
    public abstract boolean isEnabled(DatatypeFeature feature);

    /**
     * @since 2.15
     */
    public abstract DatatypeFeatures getDatatypeFeatures();

    /**
     * Method for determining whether annotation processing is enabled or not
     * (default settings are typically that it is enabled; must explicitly disable).
     *
     * @return True if annotation processing is enabled; false if not
     */
    public final boolean isAnnotationProcessingEnabled() {
        return isEnabled(MapperFeature.USE_ANNOTATIONS);
    }

    /**
     * Accessor for determining whether it is ok to try to force override of access
     * modifiers to be able to get or set values of non-public Methods, Fields;
     * to invoke non-public Constructors, Methods; or to instantiate non-public
     * Classes. By default this is enabled, but on some platforms it needs to be
     * prevented since if this would violate security constraints and cause failures.
     *
     * @return True if access modifier overriding is allowed (and may be done for
     *   any Field, Method, Constructor or Class); false to prevent any attempts
     *   to override.
     */
    public final boolean canOverrideAccessModifiers() {
        return isEnabled(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS);
    }

    /**
     * Accessor for checking whether default settings for property handling
     * indicate that properties should be alphabetically ordered or not.
     */
    public final boolean shouldSortPropertiesAlphabetically() {
        return isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
    }

    /**
     * Accessor for checking whether configuration indicates that
     * "root wrapping" (use of an extra property/name pair at root level)
     * is expected or not.
     */
    public abstract boolean useRootWrapping();

    /*
    /**********************************************************
    /* Configuration: factory methods
    /**********************************************************
     */

    /**
     * Method for constructing a specialized textual object that can typically
     * be serialized faster than basic {@link java.lang.String} (depending
     * on escaping needed if any, char-to-byte encoding if needed).
     *
     * @param src Text to represent
     *
     * @return Optimized text object constructed
     *
     * @since 2.4
     */
    public SerializableString compileString(String src) {
        /* 20-Jan-2014, tatu: For now we will just construct it directly, but
         *    in future should allow overriding to support non-standard extensions
         *    to be used by extensions like Afterburner.
         */
        return new SerializedString(src);
    }

    /*
    /**********************************************************
    /* Configuration: introspectors, mix-ins
    /**********************************************************
     */

    public ClassIntrospector getClassIntrospector() {
        return _base.getClassIntrospector();
    }

    /**
     * Method for getting {@link AnnotationIntrospector} configured
     * to introspect annotation values used for configuration.
     *<p>
     * Non-final since it is actually overridden by sub-classes (for now?)
     */
    public AnnotationIntrospector getAnnotationIntrospector() {
        if (isEnabled(MapperFeature.USE_ANNOTATIONS)) {
            return _base.getAnnotationIntrospector();
        }
        return NopAnnotationIntrospector.instance;
    }

    public final PropertyNamingStrategy getPropertyNamingStrategy() {
        return _base.getPropertyNamingStrategy();
    }

    // @since 2.12
    public final AccessorNamingStrategy.Provider getAccessorNaming() {
        return _base.getAccessorNaming();
    }

    public final HandlerInstantiator getHandlerInstantiator() {
        return _base.getHandlerInstantiator();
    }

    /*
    /**********************************************************
    /* Configuration: type and subtype handling
    /**********************************************************
     */

    /**
     * Method called to locate a type info handler for types that do not have
     * one explicitly declared via annotations (or other configuration).
     * If such default handler is configured, it is returned; otherwise
     * null is returned.
     */
    public final TypeResolverBuilder<?> getDefaultTyper(JavaType baseType) {
        return _base.getTypeResolverBuilder();
    }

    public abstract SubtypeResolver getSubtypeResolver();

    /**
     * Simple accessor for default {@link PolymorphicTypeValidator} to use for
     * legacy Default Typing methods ({@code ObjectMapper.enableDefaultTyping()})
     * and annotation based enabling.
     *<p>
     * Since 2.11 will also check {@link MapperFeature#BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES}
     * to possibly override default to more restrictive implementation, see
     * {@link com.fasterxml.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator}).
     *
     * @since 2.10
     */
    public PolymorphicTypeValidator getPolymorphicTypeValidator() {
        PolymorphicTypeValidator ptv = _base.getPolymorphicTypeValidator();
        // [databind#2587]: allow stricter default settings:
        if (ptv == LaissezFaireSubTypeValidator.instance) {
            if (isEnabled(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)) {
                ptv = new DefaultBaseTypeLimitingValidator();
            }
        }
        return ptv;
    }

    public final TypeFactory getTypeFactory() {
        return _base.getTypeFactory();
    }

    /**
     * Helper method that will construct {@link JavaType} for given
     * raw class.
     * This is a simple short-cut for:
     *<pre>
     *    getTypeFactory().constructType(cls);
     *</pre>
     */
    public final JavaType constructType(Class<?> cls) {
        return getTypeFactory().constructType(cls);
    }

    /**
     * Helper method that will construct {@link JavaType} for given
     * type reference
     * This is a simple short-cut for:
     *<pre>
     *    getTypeFactory().constructType(valueTypeRef);
     *</pre>
     */
    public final JavaType constructType(TypeReference<?> valueTypeRef) {
        return getTypeFactory().constructType(valueTypeRef.getType());
    }

    public JavaType constructSpecializedType(JavaType baseType, Class<?> subclass) {
        // note: since 2.11 specify "strict" resolution
        return getTypeFactory().constructSpecializedType(baseType, subclass, true);
    }

    /*
    /**********************************************************
    /* Configuration: introspection support
    /**********************************************************
     */

    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
     */
    public BeanDescription introspectClassAnnotations(Class<?> cls) {
        return introspectClassAnnotations(constructType(cls));
    }

    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
     */
    public BeanDescription introspectClassAnnotations(JavaType type) {
        return getClassIntrospector().forClassAnnotations(this, type, this);
    }

    /**
     * Accessor for getting bean description that only contains immediate class
     * annotations: ones from the class, and its direct mix-in, if any, but
     * not from super types.
     */
    public BeanDescription introspectDirectClassAnnotations(Class<?> cls) {
        return introspectDirectClassAnnotations(constructType(cls));
    }

    /**
     * Accessor for getting bean description that only contains immediate class
     * annotations: ones from the class, and its direct mix-in, if any, but
     * not from super types.
     */
    public final BeanDescription introspectDirectClassAnnotations(JavaType type) {
        return getClassIntrospector().forDirectClassAnnotations(this, type, this);
    }

    /*
    /**********************************************************
    /* Configuration: default settings with per-type overrides
    /**********************************************************
     */

    /**
     * Accessor for finding {@link ConfigOverride} to use for
     * properties of given type, if any exist; or return `null` if not.
     *<p>
     * Note that only directly associated override
     * is found; no type hierarchy traversal is performed.
     *
     * @since 2.8
     *
     * @return Override object to use for the type, if defined; null if none.
     */
    public abstract ConfigOverride findConfigOverride(Class<?> type);

    /**
     * Accessor for finding {@link ConfigOverride} to use for
     * properties of given type, if any exist; or if none, return an immutable
     * "empty" instance with no overrides.
     *<p>
     * Note that only directly associated override
     * is found; no type hierarchy traversal is performed.
     *
     * @since 2.9
     *
     * @return Override object to use for the type, never null (but may be empty)
     */
    public abstract ConfigOverride getConfigOverride(Class<?> type);

    /**
     * Accessor for default property inclusion to use for serialization,
     * used unless overridden by per-type or per-property overrides.
     *
     * @since 2.7
     */
    public abstract JsonInclude.Value getDefaultPropertyInclusion();

    /**
     * Accessor for default property inclusion to use for serialization,
     * considering possible per-type override for given base type.<br>
     * NOTE: if no override found, defaults to value returned by
     * {@link #getDefaultPropertyInclusion()}.
     *
     * @since 2.7
     */
    public abstract JsonInclude.Value getDefaultPropertyInclusion(Class<?> baseType);

    /**
     * Accessor for default property inclusion to use for serialization,
     * considering possible per-type override for given base type; but
     * if none found, returning given <code>defaultIncl</code>
     *
     * @param defaultIncl Inclusion setting to return if no overrides found.
     *
     * @since 2.8.2
     */
    public JsonInclude.Value getDefaultPropertyInclusion(Class<?> baseType,
            JsonInclude.Value defaultIncl)
    {
        JsonInclude.Value v = getConfigOverride(baseType).getInclude();
        if (v != null) {
            return v;
        }
        return defaultIncl;
    }

    /**
     * Accessor for default property inclusion to use for serialization,
     * considering possible per-type override for given base type and
     * possible per-type override for given property type.<br>
     * NOTE: if no override found, defaults to value returned by
     * {@link #getDefaultPropertyInclusion()}.
     *
     * @param baseType Type of the instance containing the targeted property.
     * @param propertyType Type of the property to look up inclusion setting for.
     *
     * @since 2.9
     */
    public abstract JsonInclude.Value getDefaultInclusion(Class<?> baseType,
            Class<?> propertyType);

    /**
     * Accessor for default property inclusion to use for serialization,
     * considering possible per-type override for given base type and
     * possible per-type override for given property type; but
     * if none found, returning given <code>defaultIncl</code>
     *
     * @param baseType Type of the instance containing the targeted property.
     * @param propertyType Type of the property to look up inclusion setting for.
     * @param defaultIncl Inclusion setting to return if no overrides found.
     *
     * @since 2.9
     */
    public JsonInclude.Value getDefaultInclusion(Class<?> baseType,
            Class<?> propertyType, JsonInclude.Value defaultIncl)
    {
        JsonInclude.Value baseOverride = getConfigOverride(baseType).getInclude();
        JsonInclude.Value propOverride = getConfigOverride(propertyType).getIncludeAsProperty();

        JsonInclude.Value result = JsonInclude.Value.mergeAll(defaultIncl, baseOverride, propOverride);
        return result;
    }

    /**
     * Accessor for default format settings to use for serialization (and, to a degree
     * deserialization), considering baseline settings and per-type defaults
     * for given base type (if any).
     *
     * @since 2.7
     */
    public abstract JsonFormat.Value getDefaultPropertyFormat(Class<?> baseType);

    /**
     * Accessor for default property ignorals to use, if any, for given base type,
     * based on config overrides settings (see {@link #findConfigOverride(Class)}).
     *
     * @since 2.8
     */
    public abstract JsonIgnoreProperties.Value getDefaultPropertyIgnorals(Class<?> baseType);

    /**
     * Helper method that may be called to see if there are property ignoral
     * definitions from annotations (via {@link AnnotatedClass}) or through
     * "config overrides". If both exist, config overrides have precedence
     * over class annotations.
     *
     * @since 2.8
     */
    public abstract JsonIgnoreProperties.Value getDefaultPropertyIgnorals(Class<?> baseType,
            AnnotatedClass actualClass);

    /**
     * Helper method that may be called to see if there are property inclusion
     * definitions from annotations (via {@link AnnotatedClass}).
     *
     * TODO: config override.
     *
     * @since 2.12
     */
    public abstract JsonIncludeProperties.Value getDefaultPropertyInclusions(Class<?> baseType,
            AnnotatedClass actualClass);

    /**
     * Accessor for object used for determining whether specific property elements
     * (method, constructors, fields) can be auto-detected based on
     * their visibility (access modifiers). Can be changed to allow
     * different minimum visibility levels for auto-detection. Note
     * that this is the global handler; individual types (classes)
     * can further override active checker used (using
     * {@link JsonAutoDetect} annotation)
     */
    public abstract VisibilityChecker<?> getDefaultVisibilityChecker();

    /**
     * Accessor for object used for determining whether specific property elements
     * (method, constructors, fields) can be auto-detected based on
     * their visibility (access modifiers). This is based on global defaults
     * (as would be returned by {@link #getDefaultVisibilityChecker()}, but
     * then modified by possible class annotation (see {@link JsonAutoDetect})
     * and/or per-type config override (see {@link ConfigOverride#getVisibility()}).
     *
     * @since 2.9
     */
    public abstract VisibilityChecker<?> getDefaultVisibilityChecker(Class<?> baseType,
            AnnotatedClass actualClass);

    /**
     * Accessor for the baseline setter info used as the global baseline,
     * not considering possible per-type overrides.
     *
     * @return Global base settings; never null
     *
     * @since 2.9
     */
    public abstract JsonSetter.Value getDefaultSetterInfo();

    /**
     * Accessor for the baseline merge info used as the global baseline,
     * not considering possible per-type overrides.
     *
     * @return Global base settings, if any; `null` if none.
     *
     * @since 2.9
     */
    public abstract Boolean getDefaultMergeable();

    /**
     * Accessor for the baseline merge info used for given type, including global
     * defaults if no type-specific overrides defined.
     *
     * @return Type-specific settings (if any); global defaults (same as
     *    {@link #getDefaultMergeable()}) otherwise, if any defined; or `null`
     *    if neither defined
     *
     * @since 2.9
     */
    public abstract Boolean getDefaultMergeable(Class<?> baseType);

    /*
    /**********************************************************
    /* Configuration: other
    /**********************************************************
     */

    /**
     * Method for accessing currently configured (textual) date format
     * that will be used for reading or writing date values (in case
     * of writing, only if textual output is configured; not if dates
     * are to be serialized as time stamps).
     *<p>
     * Note that typically {@link DateFormat} instances are <b>not thread-safe</b>
     * (at least ones provided by JDK):
     * this means that calling code should clone format instance before
     * using it.
     *<p>
     * This method is usually only called by framework itself, since there
     * are convenience methods available via
     * {@link DeserializationContext} and {@link SerializerProvider} that
     * take care of cloning and thread-safe reuse.
     */
    public final DateFormat getDateFormat() { return _base.getDateFormat(); }

    /**
     * Method for accessing the default {@link java.util.Locale} to use
     * for formatting, unless overridden by local annotations.
     * Initially set to {@link Locale#getDefault()}.
     */
    public final Locale getLocale() { return _base.getLocale(); }

    /**
     * Method for accessing the default {@link java.util.TimeZone} to use
     * for formatting, unless overridden by local annotations.
     * Initially set to {@link TimeZone#getDefault()}.
     */
    public final TimeZone getTimeZone() { return _base.getTimeZone(); }

    /**
     * Method for checking whether a {@link java.util.TimeZone} has been explicitly
     * set for this configuring during construction of {@code ObjectMapper}
     * or if it still has the default timezone/offset (zero-offset, "zulu").
     *
     * @return {@code true} if this configuration has explicitly specified
     *    {@link java.util.TimeZone}, or {@code false} if it uses the default
     *    time zone
     *
     * @since 2.12
     */
    public boolean hasExplicitTimeZone() {
        return _base.hasExplicitTimeZone();
    }

    /**
     * Accessor for finding currently active view, if any (null if none)
     */
    public abstract Class<?> getActiveView();

    /**
     * Method called during deserialization if Base64 encoded content
     * needs to be decoded. Default version just returns default Jackson
     * uses, which is modified-mime which does not add linefeeds (because
     * those would have to be escaped in JSON strings); but this can
     * be configured on {@link ObjectWriter}.
     */
    public Base64Variant getBase64Variant() {
        return _base.getBase64Variant();
    }

    /**
     * Method for accessing per-instance shared (baseline/default)
     * attribute values; these are used as the basis for per-call
     * attributes.
     *
     * @since 2.3
     */
    public abstract ContextAttributes getAttributes();

    /**
     * @since 2.6
     */
    public abstract PropertyName findRootName(JavaType rootType);

    /**
     * @since 2.6
     */
    public abstract PropertyName findRootName(Class<?> rawRootType);

    /*
    /**********************************************************
    /* Methods for instantiating handlers
    /**********************************************************
     */

    /**
     * Method that can be called to obtain an instance of <code>TypeIdResolver</code> of
     * specified type.
     */
    public TypeResolverBuilder<?> typeResolverBuilderInstance(Annotated annotated,
            Class<? extends TypeResolverBuilder<?>> builderClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            TypeResolverBuilder<?> builder = hi.typeResolverBuilderInstance(this, annotated, builderClass);
            if (builder != null) {
                return builder;
            }
        }
        return (TypeResolverBuilder<?>) ClassUtil.createInstance(builderClass, canOverrideAccessModifiers());
    }

    /**
     * Method that can be called to obtain an instance of <code>TypeIdResolver</code> of
     * specified type.
     */
    public TypeIdResolver typeIdResolverInstance(Annotated annotated,
            Class<? extends TypeIdResolver> resolverClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            TypeIdResolver builder = hi.typeIdResolverInstance(this, annotated, resolverClass);
            if (builder != null) {
                return builder;
            }
        }
        return (TypeIdResolver) ClassUtil.createInstance(resolverClass, canOverrideAccessModifiers());
    }
}
