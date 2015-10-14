package com.fasterxml.jackson.databind;

import java.text.DateFormat;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.LinkedNode;
import com.fasterxml.jackson.databind.util.RootNameLookup;

/**
 * Object that contains baseline configuration for deserialization
 * process. An instance is owned by {@link ObjectMapper}, which
 * passes an immutable instance to be used for deserialization process.
 *<p>
 * Note that instances are considered immutable and as such no copies
 * should need to be created for sharing; all copying is done with
 * "fluent factory" methods.
 */
public final class DeserializationConfig
    extends MapperConfigBase<DeserializationFeature, DeserializationConfig>
    implements java.io.Serializable // since 2.1
{
    // since 2.5
    private static final long serialVersionUID = 1;

    /*
    /**********************************************************
    /* Configured helper objects
    /**********************************************************
     */
    
    /**
     * Linked list that contains all registered problem handlers.
     * Implementation as front-added linked list allows for sharing
     * of the list (tail) without copying the list.
     */
    protected final LinkedNode<DeserializationProblemHandler> _problemHandlers;
    
    /**
     * Factory used for constructing {@link com.fasterxml.jackson.databind.JsonNode} instances.
     */
    protected final JsonNodeFactory _nodeFactory;

    /*
    /**********************************************************
    /* Deserialization features 
    /**********************************************************
     */

    /**
     * Set of {@link DeserializationFeature}s enabled.
     */
    protected final int _deserFeatures;
    
    /*
    /**********************************************************
    /* Parser features: generic, format-specific
    /**********************************************************
     */
    
    /**
     * States of {@link com.fasterxml.jackson.core.JsonParser.Feature}s to enable/disable.
     */
    protected final int _parserFeatures;

    /**
     * Bitflag of {@link com.fasterxml.jackson.core.JsonParser.Feature}s to enable/disable
     */
    protected final int _parserFeaturesToChange;

    /**
     * States of {@link com.fasterxml.jackson.core.FormatFeature}s to enable/disable.
     *
     * @since 2.7
     */
    protected final int _formatReadFeatures;

    /**
     * Bitflag of {@link com.fasterxml.jackson.core.FormatFeature}s to enable/disable
     *
     * @since 2.7
     */
    protected final int _formatReadFeaturesToChange;

    /*
    /**********************************************************
    /* Life-cycle, constructors
    /**********************************************************
     */

    /**
     * Constructor used by ObjectMapper to create default configuration object instance.
     */
    public DeserializationConfig(BaseSettings base,
            SubtypeResolver str, SimpleMixInResolver mixins,
            RootNameLookup rootNames)
    {
        super(base, str, mixins, rootNames);
        _deserFeatures = collectFeatureDefaults(DeserializationFeature.class);
        _nodeFactory = JsonNodeFactory.instance;
        _problemHandlers = null;
        _parserFeatures = 0;
        _parserFeaturesToChange = 0;
        _formatReadFeatures = 0;
        _formatReadFeaturesToChange = 0;
    }

    private DeserializationConfig(DeserializationConfig src,
            int mapperFeatures, int deserFeatures,
            int parserFeatures, int parserFeatureMask,
            int formatFeatures, int formatFeatureMask)
    {
        super(src, mapperFeatures);
        _deserFeatures = deserFeatures;
        _nodeFactory = src._nodeFactory;
        _problemHandlers = src._problemHandlers;
        _parserFeatures = parserFeatures;
        _parserFeaturesToChange = parserFeatureMask;
        _formatReadFeatures = formatFeatures;
        _formatReadFeaturesToChange = formatFeatureMask;
    }
    
    /**
     * Copy constructor used to create a non-shared instance with given mix-in
     * annotation definitions and subtype resolver.
     */
    private DeserializationConfig(DeserializationConfig src, SubtypeResolver str)
    {
        super(src, str);
        _deserFeatures = src._deserFeatures;
        _nodeFactory = src._nodeFactory;
        _problemHandlers = src._problemHandlers;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }

    private DeserializationConfig(DeserializationConfig src, BaseSettings base)
    {
        super(src, base);
        _deserFeatures = src._deserFeatures;
        _nodeFactory = src._nodeFactory;
        _problemHandlers = src._problemHandlers;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }
    
    private DeserializationConfig(DeserializationConfig src, JsonNodeFactory f)
    {
        super(src);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = f;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }

    private DeserializationConfig(DeserializationConfig src,
            LinkedNode<DeserializationProblemHandler> problemHandlers)
    {
        super(src);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = problemHandlers;
        _nodeFactory = src._nodeFactory;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }

    private DeserializationConfig(DeserializationConfig src, PropertyName rootName)
    {
        super(src, rootName);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }

    private DeserializationConfig(DeserializationConfig src, Class<?> view)
    {
        super(src, view);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }

    protected DeserializationConfig(DeserializationConfig src, ContextAttributes attrs)
    {
        super(src, attrs);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }

    protected DeserializationConfig(DeserializationConfig src, SimpleMixInResolver mixins)
    {
        super(src, mixins);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }

    /**
     * Copy-constructor used for making a copy to be used by new {@link ObjectMapper}
     * or {@link ObjectReader}.
     *
     * @since 2.6
     */
    protected DeserializationConfig(DeserializationConfig src, SimpleMixInResolver mixins,
            RootNameLookup rootNames)
    {
        super(src, mixins, rootNames);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }

    // for unit tests only:
    protected BaseSettings getBaseSettings() { return _base; }

    /*
    /**********************************************************
    /* Life-cycle, factory methods from MapperConfig
    /**********************************************************
     */
    
    @Override
    public DeserializationConfig with(MapperFeature... features)
    {
        int newMapperFlags = _mapperFeatures;
        for (MapperFeature f : features) {
            newMapperFlags |= f.getMask();
        }
        return (newMapperFlags == _mapperFeatures) ? this :
            new DeserializationConfig(this, newMapperFlags, _deserFeatures,
                    _parserFeatures, _parserFeaturesToChange,
                    _formatReadFeatures, _formatReadFeaturesToChange);
                    
    }

    @Override
    public DeserializationConfig without(MapperFeature... features)
    {
        int newMapperFlags = _mapperFeatures;
        for (MapperFeature f : features) {
             newMapperFlags &= ~f.getMask();
        }
        return (newMapperFlags == _mapperFeatures) ? this :
            new DeserializationConfig(this, newMapperFlags, _deserFeatures,
                    _parserFeatures, _parserFeaturesToChange,
                    _formatReadFeatures, _formatReadFeaturesToChange);
    }

    @Override
    public DeserializationConfig with(MapperFeature feature, boolean state)
    {
        int newMapperFlags;
        if (state) {
            newMapperFlags = _mapperFeatures | feature.getMask();
        } else {
            newMapperFlags = _mapperFeatures & ~feature.getMask();
        }
        return (newMapperFlags == _mapperFeatures) ? this :
            new DeserializationConfig(this, newMapperFlags, _deserFeatures,
                    _parserFeatures, _parserFeaturesToChange,
                    _formatReadFeatures, _formatReadFeaturesToChange);
    }

    @Override
    public DeserializationConfig with(ClassIntrospector ci) {
        return _withBase(_base.withClassIntrospector(ci));
    }

    @Override
    public DeserializationConfig with(AnnotationIntrospector ai) {
        return _withBase(_base.withAnnotationIntrospector(ai));
    }

    @Override
    public DeserializationConfig with(VisibilityChecker<?> vc) {
        return _withBase(_base.withVisibilityChecker(vc));
    }

    @Override
    public DeserializationConfig withVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility) {
        return _withBase( _base.withVisibility(forMethod, visibility));
    }
    
    @Override
    public DeserializationConfig with(TypeResolverBuilder<?> trb) {
        return _withBase(_base.withTypeResolverBuilder(trb));
    }

    @Override
    public DeserializationConfig with(SubtypeResolver str) {
        return (_subtypeResolver == str) ? this : new DeserializationConfig(this, str);
    }
    
    @Override
    public DeserializationConfig with(PropertyNamingStrategy pns) {
        return _withBase(_base.withPropertyNamingStrategy(pns));
    }

    @Override
    public DeserializationConfig withRootName(PropertyName rootName) {
        if (rootName == null) {
            if (_rootName == null) {
                return this;
            }
        } else if (rootName.equals(_rootName)) {
            return this;
        }
        return new DeserializationConfig(this, rootName);
    }

    @Override
    public DeserializationConfig with(TypeFactory tf) {
        return _withBase( _base.withTypeFactory(tf));
    }

    @Override
    public DeserializationConfig with(DateFormat df) {
        return _withBase(_base.withDateFormat(df));
    }
    
    @Override
    public DeserializationConfig with(HandlerInstantiator hi) {
        return _withBase(_base.withHandlerInstantiator(hi));
    }

    @Override
    public DeserializationConfig withInsertedAnnotationIntrospector(AnnotationIntrospector ai) {
        return _withBase(_base.withInsertedAnnotationIntrospector(ai));
    }

    @Override
    public DeserializationConfig withAppendedAnnotationIntrospector(AnnotationIntrospector ai) {
        return _withBase(_base.withAppendedAnnotationIntrospector(ai));
    }

    @Override
    public DeserializationConfig withView(Class<?> view) {
        return (_view == view) ? this : new DeserializationConfig(this, view);
    }

    @Override
    public DeserializationConfig with(Locale l) {
        return _withBase(_base.with(l));
    }

    @Override
    public DeserializationConfig with(TimeZone tz) {
        return _withBase(_base.with(tz));
    }

    @Override
    public DeserializationConfig with(Base64Variant base64) {
        return _withBase(_base.with(base64));
    }

    @Override
    public DeserializationConfig with(ContextAttributes attrs) {
        return (attrs == _attributes) ? this : new DeserializationConfig(this, attrs);
    }
    
    private final DeserializationConfig _withBase(BaseSettings newBase) {
        return (_base == newBase) ? this : new DeserializationConfig(this, newBase);
    }

    /*
    /**********************************************************
    /* Life-cycle, DeserializationFeature-based factory methods
    /**********************************************************
     */

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig with(DeserializationFeature feature)
    {
        int newDeserFeatures = (_deserFeatures | feature.getMask());
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures,
                    _parserFeatures, _parserFeaturesToChange,
                    _formatReadFeatures, _formatReadFeaturesToChange);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig with(DeserializationFeature first,
            DeserializationFeature... features)
    {
        int newDeserFeatures = _deserFeatures | first.getMask();
        for (DeserializationFeature f : features) {
            newDeserFeatures |= f.getMask();
        }
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures,
                    _parserFeatures, _parserFeaturesToChange,
                    _formatReadFeatures, _formatReadFeaturesToChange);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig withFeatures(DeserializationFeature... features)
    {
        int newDeserFeatures = _deserFeatures;
        for (DeserializationFeature f : features) {
            newDeserFeatures |= f.getMask();
        }
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures,
                    _parserFeatures, _parserFeaturesToChange,
                    _formatReadFeatures, _formatReadFeaturesToChange);
    }
    
    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     */
    public DeserializationConfig without(DeserializationFeature feature)
    {
        int newDeserFeatures = _deserFeatures & ~feature.getMask();
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures,
                    _parserFeatures, _parserFeaturesToChange,
                    _formatReadFeatures, _formatReadFeaturesToChange);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public DeserializationConfig without(DeserializationFeature first,
            DeserializationFeature... features)
    {
        int newDeserFeatures = _deserFeatures & ~first.getMask();
        for (DeserializationFeature f : features) {
            newDeserFeatures &= ~f.getMask();
        }
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures,
                    _parserFeatures, _parserFeaturesToChange,
                    _formatReadFeatures, _formatReadFeaturesToChange);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public DeserializationConfig withoutFeatures(DeserializationFeature... features)
    {
        int newDeserFeatures = _deserFeatures;
        for (DeserializationFeature f : features) {
            newDeserFeatures &= ~f.getMask();
        }
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures,
                    _parserFeatures, _parserFeaturesToChange,
                    _formatReadFeatures, _formatReadFeaturesToChange);
    }

    /*
    /**********************************************************
    /* Life-cycle, JsonParser.Feature-based factory methods
    /**********************************************************
     */

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     *
     * @since 2.5
     */
    public DeserializationConfig with(JsonParser.Feature feature)
    {
        int newSet = _parserFeatures | feature.getMask();
        int newMask = _parserFeaturesToChange | feature.getMask();
        return ((_parserFeatures == newSet) && (_parserFeaturesToChange == newMask)) ? this :
            new DeserializationConfig(this,  _mapperFeatures, _deserFeatures,
                    newSet, newMask,
                    _formatReadFeatures, _formatReadFeaturesToChange);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     *
     * @since 2.5
     */
    public DeserializationConfig withFeatures(JsonParser.Feature... features)
    {
        int newSet = _parserFeatures;
        int newMask = _parserFeaturesToChange;
        for (JsonParser.Feature f : features) {
            int mask = f.getMask();
            newSet |= mask;
            newMask |= mask;
        }
        return ((_parserFeatures == newSet) && (_parserFeaturesToChange == newMask)) ? this :
            new DeserializationConfig(this,  _mapperFeatures, _deserFeatures,
                    newSet, newMask,
                    _formatReadFeatures, _formatReadFeaturesToChange);
    }
    
    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     *
     * @since 2.5
     */
    public DeserializationConfig without(JsonParser.Feature feature)
    {
        int newSet = _parserFeatures & ~feature.getMask();
        int newMask = _parserFeaturesToChange | feature.getMask();
        return ((_parserFeatures == newSet) && (_parserFeaturesToChange == newMask)) ? this :
            new DeserializationConfig(this,  _mapperFeatures, _deserFeatures,
                    newSet, newMask,
                    _formatReadFeatures, _formatReadFeaturesToChange);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     *
     * @since 2.5
     */
    public DeserializationConfig withoutFeatures(JsonParser.Feature... features)
    {
        int newSet = _parserFeatures;
        int newMask = _parserFeaturesToChange;
        for (JsonParser.Feature f : features) {
            int mask = f.getMask();
            newSet &= ~mask;
            newMask |= mask;
        }
        return ((_parserFeatures == newSet) && (_parserFeaturesToChange == newMask)) ? this :
            new DeserializationConfig(this,  _mapperFeatures, _deserFeatures,
                    newSet, newMask,
                    _formatReadFeatures, _formatReadFeaturesToChange);
    }

    /*
    /**********************************************************
    /* Life-cycle, JsonParser.FormatFeature-based factory methods
    /**********************************************************
     */

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     *
     * @since 2.7
     */
    public DeserializationConfig with(FormatFeature feature)
    {
        int newSet = _formatReadFeatures | feature.getMask();
        int newMask = _formatReadFeaturesToChange | feature.getMask();
        return ((_formatReadFeatures == newSet) && (_formatReadFeaturesToChange == newMask)) ? this :
            new DeserializationConfig(this,  _mapperFeatures, _deserFeatures,
                    _parserFeatures, _parserFeaturesToChange,
                    newSet, newMask);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     *
     * @since 2.7
     */
    public DeserializationConfig withFeatures(FormatFeature... features)
    {
        int newSet = _formatReadFeatures;
        int newMask = _formatReadFeaturesToChange;
        for (FormatFeature f : features) {
            int mask = f.getMask();
            newSet |= mask;
            newMask |= mask;
        }
        return ((_formatReadFeatures == newSet) && (_formatReadFeaturesToChange == newMask)) ? this :
            new DeserializationConfig(this,  _mapperFeatures, _deserFeatures,
                    _parserFeatures, _parserFeaturesToChange,
                    newSet, newMask);
    }
    
    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     *
     * @since 2.7
     */
    public DeserializationConfig without(FormatFeature feature)
    {
        int newSet = _formatReadFeatures & ~feature.getMask();
        int newMask = _formatReadFeaturesToChange | feature.getMask();
        return ((_formatReadFeatures == newSet) && (_formatReadFeaturesToChange == newMask)) ? this :
            new DeserializationConfig(this,  _mapperFeatures, _deserFeatures,
                    _parserFeatures, _parserFeaturesToChange,
                    newSet, newMask);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     *
     * @since 2.7
     */
    public DeserializationConfig withoutFeatures(FormatFeature... features)
    {
        int newSet = _formatReadFeatures;
        int newMask = _formatReadFeaturesToChange;
        for (FormatFeature f : features) {
            int mask = f.getMask();
            newSet &= ~mask;
            newMask |= mask;
        }
        return ((_formatReadFeatures == newSet) && (_formatReadFeaturesToChange == newMask)) ? this :
            new DeserializationConfig(this,  _mapperFeatures, _deserFeatures,
                    _parserFeatures, _parserFeaturesToChange,
                    newSet, newMask);
    }    

    /*
    /**********************************************************
    /* Life-cycle, deserialization-specific factory methods
    /**********************************************************
     */

    /**
     * Fluent factory method that will construct a new instance with
     * specified {@link JsonNodeFactory}
     */
    public DeserializationConfig with(JsonNodeFactory f) {
        if (_nodeFactory == f) {
            return this;
        }
        return new DeserializationConfig(this, f);
    }

    /**
     * Method that can be used to add a handler that can (try to)
     * resolve non-fatal deserialization problems.
     */
    public DeserializationConfig withHandler(DeserializationProblemHandler h)
    {
        // Sanity check: let's prevent adding same handler multiple times
        if (LinkedNode.contains(_problemHandlers, h)) {
            return this;
        }
        return new DeserializationConfig(this,
                new LinkedNode<DeserializationProblemHandler>(h, _problemHandlers));
    }

    /**
     * Method for removing all configured problem handlers; usually done to replace
     * existing handler(s) with different one(s)
     */
    public DeserializationConfig withNoProblemHandlers() {
        if (_problemHandlers == null) {
            return this;
        }
        return new DeserializationConfig(this,
                (LinkedNode<DeserializationProblemHandler>) null);
    }

    /*
    /**********************************************************
    /* JsonParser initialization
    /**********************************************************
     */

    /**
     * Method called by {@link ObjectMapper} and {@link ObjectReader}
     * to modify those {@link com.fasterxml.jackson.core.JsonParser.Feature} settings
     * that have been configured via this config instance.
     * 
     * @since 2.5
     */
    public void initialize(JsonParser p) {
        if (_parserFeaturesToChange != 0) {
            p.overrideStdFeatures(_parserFeatures, _parserFeaturesToChange);
        }
        if (_formatReadFeaturesToChange != 0) {
            p.overrideFormatFeatures(_formatReadFeatures, _formatReadFeaturesToChange);
        }
    }

    /*
    /**********************************************************
    /* MapperConfig implementation/overrides: introspection
    /**********************************************************
     */

    /**
     * Method for getting {@link AnnotationIntrospector} configured
     * to introspect annotation values used for configuration.
     */
    @Override
    public AnnotationIntrospector getAnnotationIntrospector()
    {
        /* 29-Jul-2009, tatu: it's now possible to disable use of
         *   annotations; can be done using "no-op" introspector
         */
        if (isEnabled(MapperFeature.USE_ANNOTATIONS)) {
            return super.getAnnotationIntrospector();
        }
        return NopAnnotationIntrospector.instance;
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
        if (!isEnabled(MapperFeature.AUTO_DETECT_SETTERS)) {
            vchecker = vchecker.withSetterVisibility(Visibility.NONE);
        }
        if (!isEnabled(MapperFeature.AUTO_DETECT_CREATORS)) {
            vchecker = vchecker.withCreatorVisibility(Visibility.NONE);
        }
        if (!isEnabled(MapperFeature.AUTO_DETECT_FIELDS)) {
            vchecker = vchecker.withFieldVisibility(Visibility.NONE);
        }
        return vchecker;
    }

    /*
    /**********************************************************
    /* Configuration: default settings with per-type overrides
    /**********************************************************
     */
    
    // property inclusion not used on deserialization yet (2.7): may be added in future
    @Override
    public JsonInclude.Value getDefaultPropertyInclusion() {
        return EMPTY_INCLUDE;
    }

    @Override
    public JsonInclude.Value getDefaultPropertyInclusion(Class<?> baseType) {
        return EMPTY_INCLUDE;
    }

    @Override
    public JsonFormat.Value getDefaultPropertyFormat(Class<?> baseType) {
        // !!! TODO: per-type defaults
        return EMPTY_FORMAT;
    }

    /*
    /**********************************************************
    /* MapperConfig implementation/overrides: other
    /**********************************************************
     */

    @Override
    public boolean useRootWrapping()
    {
        if (_rootName != null) { // empty String disables wrapping; non-empty enables
            return !_rootName.isEmpty();
        }
        return isEnabled(DeserializationFeature.UNWRAP_ROOT_VALUE);
    }

    public final boolean isEnabled(DeserializationFeature f) {
        return (_deserFeatures & f.getMask()) != 0;
    }

    public final boolean isEnabled(JsonParser.Feature f, JsonFactory factory) {
        int mask = f.getMask();
        if ((_parserFeaturesToChange & mask) != 0) {
            return (_parserFeatures & f.getMask()) != 0;
        }
        return factory.isEnabled(f);
    }

    /**
     * Bulk access method for checking that all features specified by
     * mask are enabled.
     * 
     * @since 2.3
     */
    public final boolean hasDeserializationFeatures(int featureMask) {
        return (_deserFeatures & featureMask) == featureMask;
    }

    /**
     * Bulk access method for checking that at least one of features specified by
     * mask is enabled.
     * 
     * @since 2.6
     */
    public final boolean hasSomeOfFeatures(int featureMask) {
        return (_deserFeatures & featureMask) != 0;
    }

    /**
     * Bulk access method for getting the bit mask of all {@link DeserializationFeature}s
     * that are enabled.
     */
    public final int getDeserializationFeatures() {
        return _deserFeatures;
    }

    /*
    /**********************************************************
    /* Other configuration
    /**********************************************************
     */

    /**
     * Method for getting head of the problem handler chain. May be null,
     * if no handlers have been added.
     */
    public LinkedNode<DeserializationProblemHandler> getProblemHandlers() {
        return _problemHandlers;
    }

    public final JsonNodeFactory getNodeFactory() {
        return _nodeFactory;
    }

    /*
    /**********************************************************
    /* Introspection methods
    /**********************************************************
     */

    /**
     * Method that will introspect full bean properties for the purpose
     * of building a bean deserializer
     *
     * @param type Type of class to be introspected
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspect(JavaType type) {
        return (T) getClassIntrospector().forDeserialization(this, type, this);
    }

    /**
     * Method that will introspect subset of bean properties needed to
     * construct bean instance.
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspectForCreation(JavaType type) {
        return (T) getClassIntrospector().forCreation(this, type, this);
    }

    /**
     * @since 2.0
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspectForBuilder(JavaType type) {
        return (T) getClassIntrospector().forDeserializationWithBuilder(this, type, this);
    }

    /*
    /**********************************************************
    /* Support for polymorphic type handling
    /**********************************************************
     */
    
    /**
     * Helper method that is needed to properly handle polymorphic referenced
     * types, such as types referenced by {@link java.util.concurrent.atomic.AtomicReference},
     * or various "optional" types.
     * 
     * @since 2.4
     */
    public TypeDeserializer findTypeDeserializer(JavaType baseType)
        throws JsonMappingException
    {
        BeanDescription bean = introspectClassAnnotations(baseType.getRawClass());
        AnnotatedClass ac = bean.getClassInfo();
        TypeResolverBuilder<?> b = getAnnotationIntrospector().findTypeResolver(this, ac, baseType);

        /* Ok: if there is no explicit type info handler, we may want to
         * use a default. If so, config object knows what to use.
         */
        Collection<NamedType> subtypes = null;
        if (b == null) {
            b = getDefaultTyper(baseType);
            if (b == null) {
                return null;
            }
        } else {
            subtypes = getSubtypeResolver().collectAndResolveSubtypesByTypeId(this, ac);
        }
        /* 04-May-2014, tatu: When called from DeserializerFactory, additional code like
         *   this is invoked. But here we do not actually have access to mappings, so not
         *   quite sure what to do, if anything. May need to revisit if the underlying
         *   problem re-surfaces...
         */
        /*
        if ((b.getDefaultImpl() == null) && baseType.isAbstract()) {
            JavaType defaultType = mapAbstractType(config, baseType);
            if (defaultType != null && defaultType.getRawClass() != baseType.getRawClass()) {
                b = b.defaultImpl(defaultType.getRawClass());
            }
        }
        */
        return b.buildTypeDeserializer(this, baseType, subtypes);
    }
}
