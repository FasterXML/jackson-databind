package com.fasterxml.jackson.databind;

import java.text.DateFormat;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.type.ClassKey;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Object that contains baseline configuration for serialization
 * process. An instance is owned by {@link ObjectMapper}, which makes
 * a copy that is passed during serialization process to
 * {@link SerializerProvider} and {@link SerializerFactory}.
 *<p>
 * Note: although configuration settings can be changed at any time
 * (for factories and instances), they are not guaranteed to have
 * effect if called after constructing relevant mapper or serializer
 * instance. This because some objects may be configured, constructed and
 * cached first time they are needed.
 *<p>
 * As of version 1.9, the goal is to make this class eventually immutable.
 * Because of this, existing methods that allow changing state of this
 * instance are deprecated in favor of methods that create new instances
 * with different configuration ("fluent factories")
 */
public class SerializationConfig
    extends MapperConfig.Impl<SerializationConfig.Feature, SerializationConfig>
{
    /**
     * Enumeration that defines togglable features that guide
     * the serialization feature.
     */
    public enum Feature implements MapperConfig.ConfigFeature
    {
        /*
        /******************************************************
        /*  Introspection features
        /******************************************************
         */
        
        /**
         * Feature that determines whether annotation introspection
         * is used for configuration; if enabled, configured
         * {@link AnnotationIntrospector} will be used: if disabled,
         * no annotations are considered.
         *<P>
         * Feature is enabled by default.
         */
        USE_ANNOTATIONS(true),

        /**
         * Feature that determines whether regualr "getter" methods are
         * automatically detected based on standard Bean naming convention
         * or not. If yes, then all public zero-argument methods that
         * start with prefix "get" 
         * are considered as getters.
         * If disabled, only methods explicitly  annotated are considered getters.
         *<p>
         * Note that since version 1.3, this does <b>NOT</b> include
         * "is getters" (see {@link #AUTO_DETECT_IS_GETTERS} for details)
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<P>
         * Feature is enabled by default.
         */
        AUTO_DETECT_GETTERS(true),

        /**
         * Feature that determines whether "is getter" methods are
         * automatically detected based on standard Bean naming convention
         * or not. If yes, then all public zero-argument methods that
         * start with prefix "is", and whose return type is boolean
         * are considered as "is getters".
         * If disabled, only methods explicitly annotated are considered getters.
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<P>
         * Feature is enabled by default.
         */
        AUTO_DETECT_IS_GETTERS(true),

        /**
         * Feature that determines whether non-static fields are recognized as
         * properties.
         * If yes, then all public member fields
         * are considered as properties. If disabled, only fields explicitly
         * annotated are considered property fields.
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<p>
         * Feature is enabled by default.
         */
         AUTO_DETECT_FIELDS(true),

        /**
         * Feature that determines whether method and field access
         * modifier settings can be overridden when accessing
         * properties. If enabled, method
         * {@link java.lang.reflect.AccessibleObject#setAccessible}
         * may be called to enable access to otherwise unaccessible
         * objects.
         *<p>
         * Feature is enabled by default.
         */
        CAN_OVERRIDE_ACCESS_MODIFIERS(true),

        /**
         * Feature that determines whether getters (getter methods)
         * can be auto-detected if there is no matching mutator (setter,
         * constructor parameter or field) or not: if set to true,
         * only getters that match a mutator are auto-discovered; if
         * false, all auto-detectable getters can be discovered.
         *<p>
         * Feature is disabled by default for backwards compatibility
         * reasons.
         */
        REQUIRE_SETTERS_FOR_GETTERS(false),
        
        /*
        /******************************************************
        /* Generic output features
        /******************************************************
         */

        /**
         * Feature that determines whether the type detection for
         * serialization should be using actual dynamic runtime type,
         * or declared static type.
         * Default value is false, to use dynamic runtime type.
         *<p>
         * This global default value can be overridden at class, method
         * or field level by using {@link JsonSerialize#typing} annotation
         * property
         */
        USE_STATIC_TYPING(false),

        /**
         * Feature that determines whether properties that have no view
         * annotations are included in JSON serialization views (see
         * {@link com.fasterxml.jackson.annotation.JsonView} for more
         * details on JSON Views).
         * If enabled, non-annotated properties will be included;
         * when disabled, they will be excluded. So this feature
         * changes between "opt-in" (feature disabled) and
         * "opt-out" (feature enabled) modes.
         *<p>
         * Default value is enabled, meaning that non-annotated
         * properties are included in all views if there is no
         * {@link com.fasterxml.jackson.annotation.JsonView} annotation.
         */
        DEFAULT_VIEW_INCLUSION(true),
        
        /**
         * Feature that can be enabled to make root value (usually JSON
         * Object but can be any type) wrapped within a single property
         * JSON object, where key as the "root name", as determined by
         * annotation introspector (esp. for JAXB that uses
         * <code>@XmlRootElement.name</code>) or fallback (non-qualified
         * class name).
         * Feature is mostly intended for JAXB compatibility.
         *<p>
         * Default setting is false, meaning root value is not wrapped.
         */
        WRAP_ROOT_VALUE(false),

        /**
         * Feature that allows enabling (or disabling) indentation
         * for the underlying generator, using the default pretty
         * printer (see
         * {@link com.fasterxml.jackson.core.JsonGenerator#useDefaultPrettyPrinter}
         * for details).
         *<p>
         * Note that this only affects cases where
         * {@link com.fasterxml.jackson.core.JsonGenerator}
         * is constructed implicitly by ObjectMapper: if explicit
         * generator is passed, its configuration is not changed.
         *<p>
         * Also note that if you want to configure details of indentation,
         * you need to directly configure the generator: there is a
         * method to use any <code>PrettyPrinter</code> instance.
         * This feature will only allow using the default implementation.
         */
        INDENT_OUTPUT(false),

        /**
         * Feature that defines default property serialization order used
         * for POJO fields (note: does <b>not</b> apply to {@link java.util.Map}
         * serialization!):
         * if enabled, default ordering is alphabetic (similar to
         * how {@link com.fasterxml.jackson.annotation.JsonPropertyOrder#alphabetic()}
         * works); if disabled, order is unspecified (based on what JDK gives
         * us, which may be declaration order, but not guaranteed).
         *<p>
         * Note that this is just the default behavior, and can be overridden by
         * explicit overrides in classes.
         */
        SORT_PROPERTIES_ALPHABETICALLY(false),
        
        /*
        /******************************************************
        /*  Error handling features
        /******************************************************
         */
        
        /**
         * Feature that determines what happens when no accessors are
         * found for a type (and there are no annotations to indicate
         * it is meant to be serialized). If enabled (default), an
         * exception is thrown to indicate these as non-serializable
         * types; if disabled, they are serialized as empty Objects,
         * i.e. without any properties.
         *<p>
         * Note that empty types that this feature has only effect on
         * those "empty" beans that do not have any recognized annotations
         * (like <code>@JsonSerialize</code>): ones that do have annotations
         * do not result in an exception being thrown.
         */
        FAIL_ON_EMPTY_BEANS(true),

        /**
         * Feature that determines whether Jackson code should catch
         * and wrap {@link Exception}s (but never {@link Error}s!)
         * to add additional information about
         * location (within input) of problem or not. If enabled,
         * most exceptions will be caught and re-thrown (exception
         * specifically being that {@link java.io.IOException}s may be passed
         * as is, since they are declared as throwable); this can be
         * convenient both in that all exceptions will be checked and
         * declared, and so there is more contextual information.
         * However, sometimes calling application may just want "raw"
         * unchecked exceptions passed as is.
         *<p>
         * Feature is enabled by default.
         */
        WRAP_EXCEPTIONS(true),

        /*
        /******************************************************
        /* Output life cycle features
        /******************************************************
         */
        
         /**
          * Feature that determines whether <code>close</code> method of
          * serialized <b>root level</b> objects (ones for which <code>ObjectMapper</code>'s
          * writeValue() (or equivalent) method is called)
          * that implement {@link java.io.Closeable} 
          * is called after serialization or not. If enabled, <b>close()</b> will
          * be called after serialization completes (whether succesfully, or
          * due to an error manifested by an exception being thrown). You can
          * think of this as sort of "finally" processing.
          *<p>
          * NOTE: only affects behavior with <b>root</b> objects, and not other
          * objects reachable from the root object. Put another way, only one
          * call will be made for each 'writeValue' call.
          */
        CLOSE_CLOSEABLE(false),

        /**
         * Feature that determines whether <code>JsonGenerator.flush()</code> is
         * called after <code>writeValue()</code> method <b>that takes JsonGenerator
         * as an argument</b> completes (i.e. does NOT affect methods
         * that use other destinations); same for methods in {@link ObjectWriter}.
         * This usually makes sense; but there are cases where flushing
         * should not be forced: for example when underlying stream is
         * compressing and flush() causes compression state to be flushed
         * (which occurs with some compression codecs).
         */
        FLUSH_AFTER_WRITE_VALUE(true),
         
        /*
        /******************************************************
        /* Data type - specific serialization configuration
        /******************************************************
         */

        /**
         * Feature that determines whether {@link java.util.Date} values
         * (and Date-based things like {@link java.util.Calendar}s) are to be
         * serialized as numeric timestamps (true; the default),
         * or as something else (usually textual representation).
         * If textual representation is used, the actual format is
         * one returned by a call to {@link #getDateFormat}.
         *<p>
         * Note: whether this feature affects handling of other date-related
         * types depend on handlers of those types, although ideally they
         * should use this feature
         *<p>
         * Note: whether {@link java.util.Map} keys are serialized as Strings
         * or not is controlled using {@link #WRITE_DATE_KEYS_AS_TIMESTAMPS}.
         */
        WRITE_DATES_AS_TIMESTAMPS(true),

        /**
         * Feature that determines whether {@link java.util.Date}s
         * (and sub-types) used as {@link java.util.Map} keys are serialized
         * as timestamps or not (if not, will be serialized as textual
         * values).
         *<p>
         * Default value is 'false', meaning that Date-valued Map keys are serialized
         * as textual (ISO-8601) values.
         */
        WRITE_DATE_KEYS_AS_TIMESTAMPS(false),

        /**
         * Feature that determines how type <code>char[]</code> is serialized:
         * when enabled, will be serialized as an explict JSON array (with
         * single-character Strings as values); when disabled, defaults to
         * serializing them as Strings (which is more compact).
         */
        WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS(false),

        /**
         * Feature that determines standard serialization mechanism used for
         * Enum values: if enabled, return value of <code>Enum.toString()</code>
         * is used; if disabled, return value of <code>Enum.name()</code> is used.
         * Since pre-1.6 method was to use Enum name, this is the default.
         *<p>
         * Note: this feature should usually have same value
         * as {@link DeserializationConfig.Feature#READ_ENUMS_USING_TO_STRING}.
         * 
         * @since 1.6
         */
        WRITE_ENUMS_USING_TO_STRING(false),

        /**
         * Feature that determines whethere Java Enum values are serialized
         * as numbers (true), or textual values (false). If textual values are
         * used, other settings are also considered.
         * If this feature is enabled,
         *  return value of <code>Enum.ordinal()</code>
         * (an integer) will be used as the serialization.
         *<p>
         * Note that this feature has precedence over {@link #WRITE_ENUMS_USING_TO_STRING},
         * which is only considered if this feature is set to false.
         */
        WRITE_ENUMS_USING_INDEX(false),
        
        /**
         * Feature that determines whether Map entries with null values are
         * to be serialized (true) or not (false).
         *<p>
         * For further details, check out [JACKSON-314]
         */
        WRITE_NULL_MAP_VALUES(true),

        /**
         * Feature that determines whether Container properties (POJO properties
         * with declared value of Collection or array; i.e. things that produce JSON
         * arrays) that are empty (have no elements)
         * will be serialized as empty JSON arrays (true), or suppressed from output (false).
         *<p>
         * Note that this does not change behavior of {@link java.util.Map}s, or
         * "Collection-like" types.
         */
        WRITE_EMPTY_JSON_ARRAYS(true)
        
            ;

        final boolean _defaultState;
        
        private Feature(boolean defaultState) {
            _defaultState = defaultState;
        }

        @Override
        public boolean enabledByDefault() { return _defaultState; }
    
        @Override
        public int getMask() { return (1 << ordinal()); }
    }

    /*
    /**********************************************************
    /* Configuration settings
    /**********************************************************
     */

    /**
     * Which Bean/Map properties are to be included in serialization?
     * Default settings is to include all regardless of value; can be
     * changed to only include non-null properties, or properties
     * with non-default values.
     */
    protected JsonSerialize.Inclusion _serializationInclusion = null;

    /**
     * View to use for filtering out properties to serialize.
     * Null if none (will also be assigned null if <code>Object.class</code>
     * is defined), meaning that all properties are to be included.
     */
    protected Class<?> _serializationView;
    
    /**
     * Object used for resolving filter ids to filter instances.
     * Non-null if explicitly defined; null by default.
     */
    protected FilterProvider _filterProvider;
    
    /*
    /**********************************************************
    /* Life-cycle, constructors
    /**********************************************************
     */

    /**
     * Constructor used by ObjectMapper to create default configuration object instance.
     */
    public SerializationConfig(ClassIntrospector<? extends BeanDescription> intr,
            AnnotationIntrospector annIntr, VisibilityChecker<?> vc,
            SubtypeResolver subtypeResolver, PropertyNamingStrategy propertyNamingStrategy,
            TypeFactory typeFactory, HandlerInstantiator handlerInstantiator)
    {
        super(intr, annIntr, vc, subtypeResolver, propertyNamingStrategy, typeFactory, handlerInstantiator,
                collectFeatureDefaults(SerializationConfig.Feature.class));
        _filterProvider = null;
    }
    
    protected SerializationConfig(SerializationConfig src) {
        this(src, src._base);
    }

    /**
     * Constructor used to make a private copy of specific mix-in definitions.
     */
    protected SerializationConfig(SerializationConfig src,
            HashMap<ClassKey,Class<?>> mixins, SubtypeResolver str)
    {
        this(src, src._base);
        _mixInAnnotations = mixins;
        _subtypeResolver = str;
    }
    
    protected SerializationConfig(SerializationConfig src, MapperConfig.Base base)
    {
        super(src, base, src._subtypeResolver);
        _serializationInclusion = src._serializationInclusion;
        _serializationView = src._serializationView;
        _filterProvider = src._filterProvider;
    }

    protected SerializationConfig(SerializationConfig src, FilterProvider filters)
    {
        super(src);
        _serializationInclusion = src._serializationInclusion;
        _serializationView = src._serializationView;
        _filterProvider = filters;
    }

    protected SerializationConfig(SerializationConfig src, Class<?> view)
    {
        super(src);
        _serializationInclusion = src._serializationInclusion;
        _serializationView = view;
        _filterProvider = src._filterProvider;
    }

    protected SerializationConfig(SerializationConfig src, JsonSerialize.Inclusion incl)
    {
        super(src);
        _serializationInclusion = incl;
        _serializationView = src._serializationView;
        _filterProvider = src._filterProvider;
    }

    protected SerializationConfig(SerializationConfig src, int features)
    {
        super(src, features);
        _serializationInclusion = src._serializationInclusion;
        _serializationView = src._serializationView;
        _filterProvider = src._filterProvider;
    }
    
    /*
    /**********************************************************
    /* Life-cycle, factory methods from MapperConfig
    /**********************************************************
     */

    @Override
    public SerializationConfig withClassIntrospector(ClassIntrospector<? extends BeanDescription> ci) {
        return new SerializationConfig(this, _base.withClassIntrospector(ci));
    }

    @Override
    public SerializationConfig withAnnotationIntrospector(AnnotationIntrospector ai) {
        return new SerializationConfig(this, _base.withAnnotationIntrospector(ai));
    }

    @Override
    public SerializationConfig withInsertedAnnotationIntrospector(AnnotationIntrospector ai) {
        return new SerializationConfig(this, _base.withInsertedAnnotationIntrospector(ai));
    }

    @Override
    public SerializationConfig withAppendedAnnotationIntrospector(AnnotationIntrospector ai) {
        return new SerializationConfig(this, _base.withAppendedAnnotationIntrospector(ai));
    }
    
    @Override
    public SerializationConfig withVisibilityChecker(VisibilityChecker<?> vc) {
        return new SerializationConfig(this, _base.withVisibilityChecker(vc));
    }

    @Override
    public SerializationConfig withVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility) {
        return new SerializationConfig(this, _base.withVisibility(forMethod, visibility));
    }
    
    @Override
    public SerializationConfig withTypeResolverBuilder(TypeResolverBuilder<?> trb) {
        return new SerializationConfig(this, _base.withTypeResolverBuilder(trb));
    }

    @Override
    public SerializationConfig withSubtypeResolver(SubtypeResolver str) {
        SerializationConfig cfg =  new SerializationConfig(this);
        cfg._subtypeResolver = str;
        return cfg;
    }
    
    @Override
    public SerializationConfig withPropertyNamingStrategy(PropertyNamingStrategy pns) {
        return new SerializationConfig(this, _base.withPropertyNamingStrategy(pns));
    }
    
    @Override
    public SerializationConfig withTypeFactory(TypeFactory tf) {
        return new SerializationConfig(this, _base.withTypeFactory(tf));
    }

    /**
     * In addition to constructing instance with specified date format,
     * will enable or disable <code>Feature.WRITE_DATES_AS_TIMESTAMPS</code>
     * (enable if format set as null; disable if non-null)
     */
    @Override
    public SerializationConfig withDateFormat(DateFormat df) {
        SerializationConfig cfg =  new SerializationConfig(this, _base.withDateFormat(df));
        // Also need to toggle this feature based on existence of date format:
        if (df == null) {
            cfg = cfg.with(Feature.WRITE_DATES_AS_TIMESTAMPS);
        } else {
            cfg = cfg.without(Feature.WRITE_DATES_AS_TIMESTAMPS);
        }
        return cfg;
    }
    
    @Override
    public SerializationConfig withHandlerInstantiator(HandlerInstantiator hi) {
        return new SerializationConfig(this, _base.withHandlerInstantiator(hi));
    }
        
    /*
    /**********************************************************
    /* Life-cycle, SerializationConfig specific factory methods
    /**********************************************************
     */
    
    public SerializationConfig withFilters(FilterProvider filterProvider) {
        return new SerializationConfig(this, filterProvider);
    }

    public SerializationConfig withView(Class<?> view) {
        return new SerializationConfig(this, view);
    }

    public SerializationConfig withSerializationInclusion(JsonSerialize.Inclusion incl) {
        return new SerializationConfig(this, incl);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    @Override
    public SerializationConfig with(Feature... features)
    {
        int flags = _featureFlags;
        for (Feature f : features) {
            flags |= f.getMask();
        }
        return new SerializationConfig(this, flags);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    @Override
    public SerializationConfig without(Feature... features)
    {
        int flags = _featureFlags;
        for (Feature f : features) {
            flags &= ~f.getMask();
        }
        return new SerializationConfig(this, flags);
    }
    
    /*
    /**********************************************************
    /* MapperConfig implementation/overrides
    /**********************************************************
     */
    
    @Override
    public SerializationConfig createUnshared(SubtypeResolver subtypeResolver)
    {
        HashMap<ClassKey,Class<?>> mixins = _mixInAnnotations;
        _mixInAnnotationsShared = true;
        return new SerializationConfig(this, mixins, subtypeResolver);
    }
    
    @Override
    public AnnotationIntrospector getAnnotationIntrospector()
    {
        /* 29-Jul-2009, tatu: it's now possible to disable use of
         *   annotations; can be done using "no-op" introspector
         */
        if (isEnabled(Feature.USE_ANNOTATIONS)) {
            return super.getAnnotationIntrospector();
        }
        return AnnotationIntrospector.nopInstance();
    }

    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
     *<p>
     * Note: part of {@link MapperConfig} since 1.7
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends BeanDescription> T introspectClassAnnotations(JavaType type) {
        return (T) getClassIntrospector().forClassAnnotations(this, type, this);
    }

    /**
     * Accessor for getting bean description that only contains immediate class
     * annotations: ones from the class, and its direct mix-in, if any, but
     * not from super types.
     *<p>
     * Note: part of {@link MapperConfig} since 1.7
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends BeanDescription> T introspectDirectClassAnnotations(JavaType type) {
        return (T) getClassIntrospector().forDirectClassAnnotations(this, type, this);
    }

    @Override
    public boolean isAnnotationProcessingEnabled() {
        return isEnabled(SerializationConfig.Feature.USE_ANNOTATIONS);
    }
    
    @Override
    public boolean canOverrideAccessModifiers() {
        return isEnabled(Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);
    }

    @Override
    public boolean shouldSortPropertiesAlphabetically() {
        return isEnabled(Feature.SORT_PROPERTIES_ALPHABETICALLY);
    }
    
    @Override
    public VisibilityChecker<?> getDefaultVisibilityChecker()
    {
        VisibilityChecker<?> vchecker = super.getDefaultVisibilityChecker();
        if (!isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS)) {
            vchecker = vchecker.withGetterVisibility(Visibility.NONE);
        }
        // then global overrides (disabling)
        if (!isEnabled(SerializationConfig.Feature.AUTO_DETECT_IS_GETTERS)) {
            vchecker = vchecker.withIsGetterVisibility(Visibility.NONE);
        }
        if (!isEnabled(SerializationConfig.Feature.AUTO_DETECT_FIELDS)) {
            vchecker = vchecker.withFieldVisibility(Visibility.NONE);
        }
        return vchecker;
    }

    /*
    /**********************************************************
    /* MapperConfig overrides for 1.8 backwards compatibility
    /**********************************************************
     */

    /* NOTE: these are overloads we MUST have, but that were missing
     * from 1.9.0 and 1.9.1. Type erasure can bite in the ass...
     *<p>
     * NOTE: will remove either these variants, or base class one, in 2.0.
     */
    
    /** 
     * Alias for {@link MapperConfig#isEnabled(com.fasterxml.jackson.databind.MapperConfig.ConfigFeature)}.
     * 
     * @since 1.0 However, note that version 1.9.0 and 1.9.1 accidentally missed
     *    this overloaded variant
     */
    public boolean isEnabled(SerializationConfig.Feature f) {
        return (_featureFlags & f.getMask()) != 0;
    }
    
    /*
    /**********************************************************
    /* Configuration: other
    /**********************************************************
     */

    /**
     * Method for checking which serialization view is being used,
     * if any; null if none.
     */
    public Class<?> getSerializationView() { return _serializationView; }

    public JsonSerialize.Inclusion getSerializationInclusion()
    {
        if (_serializationInclusion != null) {
            return _serializationInclusion;
        }
        return JsonSerialize.Inclusion.ALWAYS;
    }
    
    /**
     * Method for getting provider used for locating filters given
     * id (which is usually provided with filter annotations).
     * Will be null if no provided was set for {@link ObjectWriter}
     * (or if serialization directly called from {@link ObjectMapper})
     */
    public FilterProvider getFilterProvider() {
        return _filterProvider;
    }

    /*
    /**********************************************************
    /* Introspection methods
    /**********************************************************
     */

    /**
     * Method that will introspect full bean properties for the purpose
     * of building a bean serializer
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspect(JavaType type) {
        return (T) getClassIntrospector().forSerialization(this, type, this);
    }

    /*
    /**********************************************************
    /* Extended API: serializer instantiation
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> serializerInstance(Annotated annotated, Class<? extends JsonSerializer<?>> serClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            JsonSerializer<?> ser = hi.serializerInstance(this, annotated, serClass);
            if (ser != null) {
                return (JsonSerializer<Object>) ser;
            }
        }
        return (JsonSerializer<Object>) ClassUtil.createInstance(serClass, canOverrideAccessModifiers());
    }
    
    /*
    /**********************************************************
    /* Debug support
    /**********************************************************
     */
    
    @Override public String toString()
    {
        return "[SerializationConfig: flags=0x"+Integer.toHexString(_featureFlags)+"]";
    }
}
