package com.fasterxml.jackson.databind;

import java.text.DateFormat;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Instantiatable;
import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.SimpleMixInResolver;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.RootNameLookup;

/**
 * Object that contains baseline configuration for serialization
 * process. An instance is owned by {@link ObjectMapper}, which
 * passes an immutable instance for serialization process to
 * {@link SerializerProvider} and {@link SerializerFactory}
 * (either directly, or through {@link ObjectWriter}.
 *<p>
 * Note that instances are considered immutable and as such no copies
 * should need to be created for sharing; all copying is done with
 * "fluent factory" methods.
 */
public final class SerializationConfig
    extends MapperConfigBase<SerializationFeature, SerializationConfig>
    implements java.io.Serializable // since 2.1
{
    // since 2.5
    private static final long serialVersionUID = 1;

    // since 2.6
    protected final static PrettyPrinter DEFAULT_PRETTY_PRINTER = new DefaultPrettyPrinter();
    
    /**
     * Set of {@link SerializationFeature}s enabled.
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
     * Object used for resolving filter ids to filter instances.
     * Non-null if explicitly defined; null by default.
     */
    protected final FilterProvider _filterProvider;

    /**
     * If "default pretty-printing" is enabled, it will create the instance
     * from this blueprint object.
     *
     * @since 2.6
     */
    protected final PrettyPrinter _defaultPrettyPrinter;
    
    /**
     * States of {@link com.fasterxml.jackson.core.JsonGenerator.Feature}s to enable/disable.
     */
    protected final int _generatorFeatures;

    /**
     * Bitflag of {@link com.fasterxml.jackson.core.JsonGenerator.Feature}s to enable/disable
     */
    protected final int _generatorFeaturesToChange;
    
    /*
    /**********************************************************
    /* Life-cycle, constructors
    /**********************************************************
     */

    /**
     * Constructor used by ObjectMapper to create default configuration object instance.
     */
    public SerializationConfig(BaseSettings base,
            SubtypeResolver str, SimpleMixInResolver mixins,
            RootNameLookup rootNames)
    {
        super(base, str, mixins, rootNames);
        _serFeatures = collectFeatureDefaults(SerializationFeature.class);
        _filterProvider = null;
        _defaultPrettyPrinter = DEFAULT_PRETTY_PRINTER;
        _generatorFeatures = 0;
        _generatorFeaturesToChange = 0;
    }
    
    private SerializationConfig(SerializationConfig src, SubtypeResolver str)
    {
        super(src, str);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _generatorFeatures = src._generatorFeatures;
        _generatorFeaturesToChange = src._generatorFeaturesToChange;
    }

    private SerializationConfig(SerializationConfig src,
            int mapperFeatures, int serFeatures,
            int generatorFeatures, int generatorFeatureMask)
    {
        super(src, mapperFeatures);
        _serFeatures = serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _generatorFeatures = generatorFeatures;
        _generatorFeaturesToChange = generatorFeatureMask;
    }
    
    private SerializationConfig(SerializationConfig src, BaseSettings base)
    {
        super(src, base);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _generatorFeatures = src._generatorFeatures;
        _generatorFeaturesToChange = src._generatorFeaturesToChange;
    }

    private SerializationConfig(SerializationConfig src, FilterProvider filters)
    {
        super(src);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _filterProvider = filters;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _generatorFeatures = src._generatorFeatures;
        _generatorFeaturesToChange = src._generatorFeaturesToChange;
    }

    private SerializationConfig(SerializationConfig src, Class<?> view)
    {
        super(src, view);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _generatorFeatures = src._generatorFeatures;
        _generatorFeaturesToChange = src._generatorFeaturesToChange;
    }

    private SerializationConfig(SerializationConfig src, JsonInclude.Include incl)
    {
        super(src);
        _serFeatures = src._serFeatures;
        _serializationInclusion = incl;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _generatorFeatures = src._generatorFeatures;
        _generatorFeaturesToChange = src._generatorFeaturesToChange;
    }

    private SerializationConfig(SerializationConfig src, PropertyName rootName)
    {
        super(src, rootName);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _generatorFeatures = src._generatorFeatures;
        _generatorFeaturesToChange = src._generatorFeaturesToChange;
    }

    /**
     * @since 2.1
     */
    protected SerializationConfig(SerializationConfig src, ContextAttributes attrs)
    {
        super(src, attrs);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _generatorFeatures = src._generatorFeatures;
        _generatorFeaturesToChange = src._generatorFeaturesToChange;
    }

    /**
     * @since 2.1
     */
    protected SerializationConfig(SerializationConfig src, SimpleMixInResolver mixins)
    {
        super(src, mixins);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _generatorFeatures = src._generatorFeatures;
        _generatorFeaturesToChange = src._generatorFeaturesToChange;
    }
    
    /**
     * @since 2.6
     */
    protected SerializationConfig(SerializationConfig src, PrettyPrinter defaultPP)
    {
        super(src);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = defaultPP;
        _generatorFeatures = src._generatorFeatures;
        _generatorFeaturesToChange = src._generatorFeaturesToChange;
    }

    /**
     * Copy-constructor used for making a copy to be used by new {@link ObjectMapper}
     * or {@link ObjectReader}.
     *
     * @since 2.6
     */
    protected SerializationConfig(SerializationConfig src, SimpleMixInResolver mixins,
            RootNameLookup rootNames)
    {
        super(src, mixins, rootNames);
        _serFeatures = src._serFeatures;
        _serializationInclusion = src._serializationInclusion;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _generatorFeatures = src._generatorFeatures;
        _generatorFeaturesToChange = src._generatorFeaturesToChange;
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
    public SerializationConfig with(MapperFeature... features)
    {
        int newMapperFlags = _mapperFeatures;
        for (MapperFeature f : features) {
            newMapperFlags |= f.getMask();
        }
        return (newMapperFlags == _mapperFeatures) ? this
                : new SerializationConfig(this, newMapperFlags, _serFeatures,
                        _generatorFeatures, _generatorFeaturesToChange);
    }
    
    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    @Override
    public SerializationConfig without(MapperFeature... features)
    {
        int newMapperFlags = _mapperFeatures;
        for (MapperFeature f : features) {
             newMapperFlags &= ~f.getMask();
        }
        return (newMapperFlags == _mapperFeatures) ? this
                : new SerializationConfig(this, newMapperFlags, _serFeatures,
                        _generatorFeatures, _generatorFeaturesToChange);
    }

    @Override
    public SerializationConfig with(MapperFeature feature, boolean state)
    {
        int newMapperFlags;
        if (state) {
            newMapperFlags = _mapperFeatures | feature.getMask();
        } else {
            newMapperFlags = _mapperFeatures & ~feature.getMask();
        }
        return (newMapperFlags == _mapperFeatures) ? this
            : new SerializationConfig(this, newMapperFlags, _serFeatures,
                    _generatorFeatures, _generatorFeaturesToChange);
    }
    
    @Override
    public SerializationConfig with(AnnotationIntrospector ai) {
        return _withBase(_base.withAnnotationIntrospector(ai));
    }

    @Override
    public SerializationConfig withAppendedAnnotationIntrospector(AnnotationIntrospector ai) {
        return _withBase(_base.withAppendedAnnotationIntrospector(ai));
    }

    @Override
    public SerializationConfig withInsertedAnnotationIntrospector(AnnotationIntrospector ai) {
        return _withBase(_base.withInsertedAnnotationIntrospector(ai));
    }

    @Override
    public SerializationConfig with(ClassIntrospector ci) {
        return _withBase(_base.withClassIntrospector(ci));
    }

    /**
     * In addition to constructing instance with specified date format,
     * will enable or disable <code>SerializationFeature.WRITE_DATES_AS_TIMESTAMPS</code>
     * (enable if format set as null; disable if non-null)
     */
    @Override
    public SerializationConfig with(DateFormat df) {
        SerializationConfig cfg =  new SerializationConfig(this, _base.withDateFormat(df));
        // Also need to toggle this feature based on existence of date format:
        if (df == null) {
            cfg = cfg.with(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        } else {
            cfg = cfg.without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        return cfg;
    }

    @Override
    public SerializationConfig with(HandlerInstantiator hi) {
        return _withBase(_base.withHandlerInstantiator(hi));
    }

    @Override
    public SerializationConfig with(PropertyNamingStrategy pns) {
        return _withBase(_base.withPropertyNamingStrategy(pns));
    }

    @Override
    public SerializationConfig withRootName(PropertyName rootName) {
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
    public SerializationConfig with(SubtypeResolver str) {
        return (str == _subtypeResolver)? this : new SerializationConfig(this, str);
    }

    @Override
    public SerializationConfig with(TypeFactory tf) {
        return _withBase(_base.withTypeFactory(tf));
    }

    @Override
    public SerializationConfig with(TypeResolverBuilder<?> trb) {
        return _withBase(_base.withTypeResolverBuilder(trb));
    }

    @Override
    public SerializationConfig withView(Class<?> view) {
        return (_view == view) ? this : new SerializationConfig(this, view);
    }

    @Override
    public SerializationConfig with(VisibilityChecker<?> vc) {
        return _withBase(_base.withVisibilityChecker(vc));
    }

    @Override
    public SerializationConfig withVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility) {
        return _withBase(_base.withVisibility(forMethod, visibility));
    }

    @Override
    public SerializationConfig with(Locale l) {
        return _withBase(_base.with(l));
    }

    @Override
    public SerializationConfig with(TimeZone tz) {
        return _withBase(_base.with(tz));
    }

    @Override
    public SerializationConfig with(Base64Variant base64) {
        return _withBase(_base.with(base64));
    }

    @Override
    public SerializationConfig with(ContextAttributes attrs) {
        return (attrs == _attributes) ? this : new SerializationConfig(this, attrs);
    }

    private final SerializationConfig _withBase(BaseSettings newBase) {
        return (_base == newBase) ? this : new SerializationConfig(this, newBase);
    }

    /*
    /**********************************************************
    /* Factory methods for SerializationFeature
    /**********************************************************
     */

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature enabled.
     */
    public SerializationConfig with(SerializationFeature feature)
    {
        int newSerFeatures = _serFeatures | feature.getMask();
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this, _mapperFeatures, newSerFeatures,
                        _generatorFeatures, _generatorFeaturesToChange);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public SerializationConfig with(SerializationFeature first, SerializationFeature... features)
    {
        int newSerFeatures = _serFeatures | first.getMask();
        for (SerializationFeature f : features) {
            newSerFeatures |= f.getMask();
        }
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this, _mapperFeatures, newSerFeatures,
                        _generatorFeatures, _generatorFeaturesToChange);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public SerializationConfig withFeatures(SerializationFeature... features)
    {
        int newSerFeatures = _serFeatures;
        for (SerializationFeature f : features) {
            newSerFeatures |= f.getMask();
        }
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this, _mapperFeatures, newSerFeatures,
                        _generatorFeatures, _generatorFeaturesToChange);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     */
    public SerializationConfig without(SerializationFeature feature)
    {
        int newSerFeatures = _serFeatures & ~feature.getMask();
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this, _mapperFeatures, newSerFeatures,
                        _generatorFeatures, _generatorFeaturesToChange);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public SerializationConfig without(SerializationFeature first, SerializationFeature... features)
    {
        int newSerFeatures = _serFeatures & ~first.getMask();
        for (SerializationFeature f : features) {
            newSerFeatures &= ~f.getMask();
        }
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this, _mapperFeatures, newSerFeatures,
                        _generatorFeatures, _generatorFeaturesToChange);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public SerializationConfig withoutFeatures(SerializationFeature... features)
    {
        int newSerFeatures = _serFeatures;
        for (SerializationFeature f : features) {
            newSerFeatures &= ~f.getMask();
        }
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this, _mapperFeatures, newSerFeatures,
                        _generatorFeatures, _generatorFeaturesToChange);
    }

    /*
    /**********************************************************
    /* Factory methods for JsonGenerator.Feature
    /**********************************************************
     */
    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature enabled.
     *
     * @since 2.5
     */
    public SerializationConfig with(JsonGenerator.Feature feature)
    {
        int newSet = _generatorFeatures | feature.getMask();
        int newMask = _generatorFeaturesToChange | feature.getMask();
        return ((_generatorFeatures == newSet) && (_generatorFeaturesToChange == newMask)) ? this :
            new SerializationConfig(this,  _mapperFeatures, _serFeatures,
                    newSet, newMask);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     *
     * @since 2.5
     */
    public SerializationConfig withFeatures(JsonGenerator.Feature... features)
    {
        int newSet = _generatorFeatures;
        int newMask = _generatorFeaturesToChange;
        for (JsonGenerator.Feature f : features) {
            int mask = f.getMask();
            newSet |= mask;
            newMask |= mask;
        }
        return ((_generatorFeatures == newSet) && (_generatorFeaturesToChange == newMask)) ? this :
            new SerializationConfig(this,  _mapperFeatures, _serFeatures,
                    newSet, newMask);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     *
     * @since 2.5
     */
    public SerializationConfig without(JsonGenerator.Feature feature)
    {
        int newSet = _generatorFeatures & ~feature.getMask();
        int newMask = _generatorFeaturesToChange | feature.getMask();
        return ((_generatorFeatures == newSet) && (_generatorFeaturesToChange == newMask)) ? this :
            new SerializationConfig(this,  _mapperFeatures, _serFeatures,
                    newSet, newMask);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     *
     * @since 2.5
     */
    public SerializationConfig withoutFeatures(JsonGenerator.Feature... features)
    {
        int newSet = _generatorFeatures;
        int newMask = _generatorFeaturesToChange;
        for (JsonGenerator.Feature f : features) {
            int mask = f.getMask();
            newSet &= ~mask;
            newMask |= mask;
        }
        return ((_generatorFeatures == newSet) && (_generatorFeaturesToChange == newMask)) ? this :
            new SerializationConfig(this,  _mapperFeatures, _serFeatures,
                    newSet, newMask);
    }

    /*
    /**********************************************************
    /* Factory methods, other
    /**********************************************************
     */

    public SerializationConfig withFilters(FilterProvider filterProvider) {
        return (filterProvider == _filterProvider) ? this : new SerializationConfig(this, filterProvider);
    }

    public SerializationConfig withSerializationInclusion(JsonInclude.Include incl) {
        return (_serializationInclusion == incl) ? this:  new SerializationConfig(this, incl);
    }

    /**
     * @since 2.6
     */
    public SerializationConfig withDefaultPrettyPrinter(PrettyPrinter pp) {
        return (_defaultPrettyPrinter == pp) ? this:  new SerializationConfig(this, pp);
    }

    /*
    /**********************************************************
    /* Factories for objects configured here
    /**********************************************************
     */

    public PrettyPrinter constructDefaultPrettyPrinter() {
        PrettyPrinter pp = _defaultPrettyPrinter;
        if (pp instanceof Instantiatable<?>) {
            pp = (PrettyPrinter) ((Instantiatable<?>) pp).createInstance();
        }
        return pp;
    }
    
    /*
    /**********************************************************
    /* JsonParser initialization
    /**********************************************************
     */

    /**
     * Method called by {@link ObjectMapper} and {@link ObjectWriter}
     * to modify those {@link com.fasterxml.jackson.core.JsonGenerator.Feature} settings
     * that have been configured via this config instance.
     * 
     * @since 2.5
     */
    public void initialize(JsonGenerator g)
    {
        if (SerializationFeature.INDENT_OUTPUT.enabledIn(_serFeatures)) {
            // but do not override an explicitly set one
            if (g.getPrettyPrinter() == null) {
                PrettyPrinter pp = constructDefaultPrettyPrinter();
                if (pp != null) {
                    g.setPrettyPrinter(pp);
                }
            }
        }
        @SuppressWarnings("deprecation")
        boolean useBigDec = SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN.enabledIn(_serFeatures);
        if ((_generatorFeaturesToChange != 0) || useBigDec) {
            int orig = g.getFeatureMask();
            int newFlags = (orig & ~_generatorFeaturesToChange) | _generatorFeatures;
            // although deprecated, needs to be supported for now
            if (useBigDec) {
                newFlags |= JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN.getMask();
            }
            if (orig != newFlags) {
                g.setFeatureMask(newFlags);
            }
        }
    }
    
    /*
    /**********************************************************
    /* MapperConfig implementation/overrides
    /**********************************************************
     */

    @Override
    public boolean useRootWrapping()
    {
        if (_rootName != null) { // empty String disables wrapping; non-empty enables
            return !_rootName.isEmpty();
        }
        return isEnabled(SerializationFeature.WRAP_ROOT_VALUE);
    }

    @Override
    public AnnotationIntrospector getAnnotationIntrospector()
    {
        if (isEnabled(MapperFeature.USE_ANNOTATIONS)) {
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
        if (!isEnabled(MapperFeature.AUTO_DETECT_GETTERS)) {
            vchecker = vchecker.withGetterVisibility(Visibility.NONE);
        }
        // then global overrides (disabling)
        if (!isEnabled(MapperFeature.AUTO_DETECT_IS_GETTERS)) {
            vchecker = vchecker.withIsGetterVisibility(Visibility.NONE);
        }
        if (!isEnabled(MapperFeature.AUTO_DETECT_FIELDS)) {
            vchecker = vchecker.withFieldVisibility(Visibility.NONE);
        }
        return vchecker;
    }

    /*
    /**********************************************************
    /* Configuration: other
    /**********************************************************
     */

    public final boolean isEnabled(SerializationFeature f) {
        return (_serFeatures & f.getMask()) != 0;
    }

    /**
     * Accessor method that first checks if we have any overrides
     * for feature, and only if not, checks state of passed-in
     * factory.
     * 
     * @since 2.5
     */
    public final boolean isEnabled(JsonGenerator.Feature f, JsonFactory factory) {
        int mask = f.getMask();
        if ((_generatorFeaturesToChange & mask) != 0) {
            return (_generatorFeatures & f.getMask()) != 0;
        }
        return factory.isEnabled(f);
    }
    
    /**
     * "Bulk" access method for checking that all features specified by
     * mask are enabled.
     * 
     * @since 2.3
     */
    public final boolean hasSerializationFeatures(int featureMask) {
        return (_serFeatures & featureMask) == featureMask;
    }

    public final int getSerializationFeatures() {
        return _serFeatures;
    }

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

    /**
     * Accessor for configured blueprint "default" {@link PrettyPrinter} to
     * use, if default pretty-printing is enabled.
     *<p>
     * NOTE: returns the "blueprint" instance, and does NOT construct
     * an instance ready to use; call {@link #constructDefaultPrettyPrinter()} if
     * actually usable instance is desired.
     *
     * @since 2.6
     */
    public PrettyPrinter getDefaultPrettyPrinter() {
        return _defaultPrettyPrinter;
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
    /* Debug support
    /**********************************************************
     */

    @Override
    public String toString() {
        return "[SerializationConfig: flags=0x"+Integer.toHexString(_serFeatures)+"]";
    }
}
