package com.fasterxml.jackson.databind;

import java.text.DateFormat;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.cfg.*;
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
 * process. An instance is owned by {@link ObjectMapper}, which
 * passes an immutable instance for serialization process to
 * {@link SerializerProvider} and {@link SerializerFactory}
 * (either directly, or through {@link ObjectWriter}.
 *<p>
 * Note that instances are considered immutable and as such no copies
 * should need to be created (there are some implementation details
 * with respect to mix-in annotations; where this is guaranteed as
 * long as caller follow "copy-then-use" pattern)
 */
public final class SerializationConfig
    extends MapperConfigBase<SerializationConfig.Feature, SerializationConfig>
{
    /**
     * Enumeration that defines simple on/off features that affect
     * the way Java objects are serialized.
     *<p>
     * Note that features can be set both through
     * {@link ObjectMapper} (as sort of defaults) and through
     * {@link ObjectWriter}.
     * In first case these defaults must follow "config-then-use" patterns
     * (i.e. defined once, not changed afterwards); all per-call
     * changes must be done using {@link ObjectWriter}.
     */
    public enum Feature implements ConfigFeature
    {
        /*
        /******************************************************
        /* Generic output features
        /******************************************************
         */
        
        /**
         * Feature that can be enabled to make root value (usually JSON
         * Object but can be any type) wrapped within a single property
         * JSON object, where key as the "root name", as determined by
         * annotation introspector (esp. for JAXB that uses
         * <code>@XmlRootElement.name</code>) or fallback (non-qualified
         * class name).
         * Feature is mostly intended for JAXB compatibility.
         *<p>
         * Feature is enabled by default.
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
         *<p>
         * Feature is enabled by default.
         */
        INDENT_OUTPUT(false),
        
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
         *<p>
         * Feature is enabled by default.
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
         *<p>
         * Feature is disabled by default.
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
         *<p>
         * Feature is enabled by default.
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
         *<p>
         * Feature is enabled by default.
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
         *<p>
         * Feature is disabled by default.
         */
        WRITE_DATE_KEYS_AS_TIMESTAMPS(false),

        /**
         * Feature that determines how type <code>char[]</code> is serialized:
         * when enabled, will be serialized as an explict JSON array (with
         * single-character Strings as values); when disabled, defaults to
         * serializing them as Strings (which is more compact).
         *<p>
         * Feature is disabled by default.
         */
        WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS(false),

        /**
         * Feature that determines standard serialization mechanism used for
         * Enum values: if enabled, return value of <code>Enum.toString()</code>
         * is used; if disabled, return value of <code>Enum.name()</code> is used.
         *<p>
         * Note: this feature should usually have same value
         * as {@link DeserializationConfig.Feature#READ_ENUMS_USING_TO_STRING}.
         *<p>
         * Feature is disabled by default.
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
         *<p>
         * Feature is disabled by default.
         */
        WRITE_ENUMS_USING_INDEX(false),
        
        /**
         * Feature that determines whether Map entries with null values are
         * to be serialized (true) or not (false).
         *<p>
         * For further details, check out [JACKSON-314]
         *<p>
         * Feature is enabled by default.
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
         *<p>
         * Feature is enabled by default.
         */
        WRITE_EMPTY_JSON_ARRAYS(true)
        
            ;

        private final boolean _defaultState;
        
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
    /* Serialization settings
    /**********************************************************
     */

    /**
     * Set of features enabled; actual type (kind of features)
     * depends on sub-classes.
     */
    protected final int _serFeatures;
    
    /**
     * Which Bean/Map properties are to be included in serialization?
     * Default settings is to include all regardless of value; can be
     * changed to only include non-null properties, or properties
     * with non-default values.
     */
    protected JsonInclude.Include _serializationInclusion = null;

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
    public SerializationConfig(BaseSettings base,
            SubtypeResolver str, Map<ClassKey,Class<?>> mixins)
    {
        super(base, str, mixins);
        _serFeatures = collectFeatureDefaults(SerializationConfig.Feature.class);
        _filterProvider = null;
    }
    
    private SerializationConfig(SerializationConfig src, SubtypeResolver str)
    {
        super(src, str);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _serializationView = src._serializationView;
        _filterProvider = src._filterProvider;
    }

    private SerializationConfig(SerializationConfig src,
            int mapperFeatures, int serFeatures)
    {
        super(src, mapperFeatures);
        _serFeatures = serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _serializationView = src._serializationView;
        _filterProvider = src._filterProvider;
    }
    
    private SerializationConfig(SerializationConfig src, BaseSettings base)
    {
        super(src, base);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _serializationView = src._serializationView;
        _filterProvider = src._filterProvider;
    }

    private SerializationConfig(SerializationConfig src, FilterProvider filters)
    {
        super(src);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _serializationView = src._serializationView;
        _filterProvider = filters;
    }

    private SerializationConfig(SerializationConfig src, Class<?> view)
    {
        super(src);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _serializationView = view;
        _filterProvider = src._filterProvider;
    }

    private SerializationConfig(SerializationConfig src, JsonInclude.Include incl)
    {
        super(src);
        _serFeatures = src._serFeatures;
        _serializationInclusion = incl;
        _serializationView = src._serializationView;
        _filterProvider = src._filterProvider;
    }

    private SerializationConfig(SerializationConfig src, String rootName)
    {
        super(src, rootName);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _serializationView = src._serializationView;
        _filterProvider = src._filterProvider;
    }
    
    /*
    /**********************************************************
    /* Life-cycle, factory methods from MapperConfig
    /**********************************************************
     */

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    @Override
    public SerializationConfig with(MapperConfig.Feature... features)
    {
        int newMapperFlags = _mapperFeatures;
        for (MapperConfig.Feature f : features) {
            newMapperFlags |= f.getMask();
        }
        return (newMapperFlags == _mapperFeatures) ? this
                : new SerializationConfig(this, newMapperFlags, _serFeatures);
    }
    
    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    @Override
    public SerializationConfig without(MapperConfig.Feature... features)
    {
        int newMapperFlags = _mapperFeatures;
        for (MapperConfig.Feature f : features) {
             newMapperFlags &= ~f.getMask();
        }
        return (newMapperFlags == _mapperFeatures) ? this
                : new SerializationConfig(this, newMapperFlags, _serFeatures);
    }
    
    @Override
    public SerializationConfig withClassIntrospector(ClassIntrospector ci) {
        return _withBase(_base.withClassIntrospector(ci));
    }

    @Override
    public SerializationConfig withAnnotationIntrospector(AnnotationIntrospector ai) {
        return _withBase(_base.withAnnotationIntrospector(ai));
    }

    @Override
    public SerializationConfig withInsertedAnnotationIntrospector(AnnotationIntrospector ai) {
        return _withBase(_base.withInsertedAnnotationIntrospector(ai));
    }

    @Override
    public SerializationConfig withAppendedAnnotationIntrospector(AnnotationIntrospector ai) {
        return _withBase(_base.withAppendedAnnotationIntrospector(ai));
    }
    
    @Override
    public SerializationConfig withVisibilityChecker(VisibilityChecker<?> vc) {
        return _withBase(_base.withVisibilityChecker(vc));
    }

    @Override
    public SerializationConfig withVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility) {
        return _withBase(_base.withVisibility(forMethod, visibility));
    }
    
    @Override
    public SerializationConfig withTypeResolverBuilder(TypeResolverBuilder<?> trb) {
        return _withBase(_base.withTypeResolverBuilder(trb));
    }
    
    @Override
    public SerializationConfig withSubtypeResolver(SubtypeResolver str) {
        return (str == _subtypeResolver)? this : new SerializationConfig(this, str);
    }
    
    @Override
    public SerializationConfig withPropertyNamingStrategy(PropertyNamingStrategy pns) {
        return _withBase(_base.withPropertyNamingStrategy(pns));
    }

    @Override
    public SerializationConfig withRootName(String rootName) {
        if (rootName == null) {
            if (_rootName == null) {
                return this;
            }
        } else if (rootName.equals(_rootName)) {
            return this;
        }
        return new SerializationConfig(this, rootName);
    }
    
    @Override
    public SerializationConfig withTypeFactory(TypeFactory tf) {
        return _withBase(_base.withTypeFactory(tf));
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
        return _withBase(_base.withHandlerInstantiator(hi));
    }

    private final SerializationConfig _withBase(BaseSettings newBase) {
        return (_base == newBase) ? this : new SerializationConfig(this, newBase);
    }
    
    /*
    /**********************************************************
    /* Life-cycle, SerializationConfig specific factory methods
    /**********************************************************
     */
    
    public SerializationConfig withFilters(FilterProvider filterProvider) {
        return (filterProvider == _filterProvider) ? this : new SerializationConfig(this, filterProvider);
    }

    public SerializationConfig withView(Class<?> view) {
        return (_serializationView == view) ? this : new SerializationConfig(this, view);
    }

    public SerializationConfig withSerializationInclusion(JsonInclude.Include incl) {
        return (_serializationInclusion == incl) ? this:  new SerializationConfig(this, incl);
    }
    
    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature enabled.
     */
    public SerializationConfig with(Feature feature)
    {
        int newSerFeatures = _serFeatures | feature.getMask();
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this, _mapperFeatures, newSerFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public SerializationConfig with(Feature first, Feature... features)
    {
        int newSerFeatures = _serFeatures | first.getMask();
        for (Feature f : features) {
            newSerFeatures |= f.getMask();
        }
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this, _mapperFeatures, newSerFeatures);
    }
    
    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     */
    public SerializationConfig without(Feature feature)
    {
        int newSerFeatures = _serFeatures & ~feature.getMask();
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this, _mapperFeatures, newSerFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public SerializationConfig without(Feature first, Feature... features)
    {
        int newSerFeatures = _serFeatures & ~first.getMask();
        for (Feature f : features) {
            newSerFeatures &= ~f.getMask();
        }
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this, _mapperFeatures, newSerFeatures);
    }
    
    /*
    /**********************************************************
    /* MapperConfig implementation/overrides
    /**********************************************************
     */

    @Override
    public final int getFeatureFlags() {
        return _serFeatures;
    }
    
    @Override
    public boolean useRootWrapping()
    {
        if (_rootName != null) { // empty String disables wrapping; non-empty enables
            return (_rootName.length() > 0);
        }
        return isEnabled(SerializationConfig.Feature.WRAP_ROOT_VALUE);
    }
    
    @Override
    public AnnotationIntrospector getAnnotationIntrospector()
    {
        /* 29-Jul-2009, tatu: it's now possible to disable use of
         *   annotations; can be done using "no-op" introspector
         */
        if (isEnabled(MapperConfig.Feature.USE_ANNOTATIONS)) {
            return super.getAnnotationIntrospector();
        }
        return AnnotationIntrospector.nopInstance();
    }

    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
     */
    @Override
    public BeanDescription introspectClassAnnotations(JavaType type) {
        return getClassIntrospector().forClassAnnotations(this, type, this);
    }

    /**
     * Accessor for getting bean description that only contains immediate class
     * annotations: ones from the class, and its direct mix-in, if any, but
     * not from super types.
     */
    @Override
    public BeanDescription introspectDirectClassAnnotations(JavaType type) {
        return getClassIntrospector().forDirectClassAnnotations(this, type, this);
    }
    
    @Override
    public VisibilityChecker<?> getDefaultVisibilityChecker()
    {
        VisibilityChecker<?> vchecker = super.getDefaultVisibilityChecker();
        if (!isEnabled(MapperConfig.Feature.AUTO_DETECT_GETTERS)) {
            vchecker = vchecker.withGetterVisibility(Visibility.NONE);
        }
        // then global overrides (disabling)
        if (!isEnabled(MapperConfig.Feature.AUTO_DETECT_IS_GETTERS)) {
            vchecker = vchecker.withIsGetterVisibility(Visibility.NONE);
        }
        if (!isEnabled(MapperConfig.Feature.AUTO_DETECT_FIELDS)) {
            vchecker = vchecker.withFieldVisibility(Visibility.NONE);
        }
        return vchecker;
    }

    public boolean isEnabled(SerializationConfig.Feature f) {
        return (_serFeatures & f.getMask()) != 0;
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

    public JsonInclude.Include getSerializationInclusion()
    {
        if (_serializationInclusion != null) {
            return _serializationInclusion;
        }
        return JsonInclude.Include.ALWAYS;
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
    public JsonSerializer<Object> serializerInstance(Annotated annotated, Class<?> serClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            JsonSerializer<?> ser = hi.serializerInstance(this, annotated,
                    (Class<JsonSerializer<?>>)serClass);
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
        return "[SerializationConfig: flags=0x"+Integer.toHexString(_serFeatures)+"]";
    }
}
