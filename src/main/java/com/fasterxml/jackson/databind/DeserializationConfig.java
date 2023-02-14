package com.fasterxml.jackson.databind;

import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.type.LogicalType;
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
    // since 2.9
    private static final long serialVersionUID = 2;

    // since 2.10.1
    private final static int DESER_FEATURE_DEFAULTS = collectFeatureDefaults(DeserializationFeature.class);

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

    /**
     * @since 2.12
     */
    protected final CoercionConfigs _coercionConfigs;

    /**
     * @since 2.12
     */
    protected final ConstructorDetector _ctorDetector;

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
    /* Life-cycle, primary constructors for new instances
    /**********************************************************
     */

    /**
     * Constructor used by ObjectMapper to create default configuration object instance.
     *
     * @since 2.14
     */
    public DeserializationConfig(BaseSettings base,
            SubtypeResolver str, SimpleMixInResolver mixins, RootNameLookup rootNames,
            ConfigOverrides configOverrides, CoercionConfigs coercionConfigs,
            DatatypeFeatures datatypeFeatures)
    {
        super(base, str, mixins, rootNames, configOverrides, datatypeFeatures);
        _deserFeatures = DESER_FEATURE_DEFAULTS;
        _problemHandlers = null;
        _nodeFactory = JsonNodeFactory.instance;
        _ctorDetector = null;
        _coercionConfigs = coercionConfigs;
        _parserFeatures = 0;
        _parserFeaturesToChange = 0;
        _formatReadFeatures = 0;
        _formatReadFeaturesToChange = 0;
    }

    /**
     * Copy-constructor used for making a copy used by new {@link ObjectMapper}.
     *
     * @since 2.14
     */
    protected DeserializationConfig(DeserializationConfig src,
            SubtypeResolver str, SimpleMixInResolver mixins, RootNameLookup rootNames,
            ConfigOverrides configOverrides,
            CoercionConfigs coercionConfigs)
    {
        super(src, str, mixins, rootNames, configOverrides);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _ctorDetector = src._ctorDetector;
        _coercionConfigs = coercionConfigs;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }

    /*
    /**********************************************************
    /* Life-cycle, secondary constructors to support
    /* "mutant factories", with single property changes
    /**********************************************************
     */

    private DeserializationConfig(DeserializationConfig src,
            long mapperFeatures, int deserFeatures,
            int parserFeatures, int parserFeatureMask,
            int formatFeatures, int formatFeatureMask)
    {
        super(src, mapperFeatures);
        _deserFeatures = deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _coercionConfigs = src._coercionConfigs;
        _ctorDetector = src._ctorDetector;
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
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _coercionConfigs = src._coercionConfigs;
        _ctorDetector = src._ctorDetector;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }

    private DeserializationConfig(DeserializationConfig src, BaseSettings base)
    {
        super(src, base);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _coercionConfigs = src._coercionConfigs;
        _ctorDetector = src._ctorDetector;
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
        _coercionConfigs = src._coercionConfigs;
        _ctorDetector = src._ctorDetector;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }

    // @since 2.12
    private DeserializationConfig(DeserializationConfig src, ConstructorDetector ctorDetector)
    {
        super(src);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _coercionConfigs = src._coercionConfigs;
        _ctorDetector = ctorDetector;
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
        _coercionConfigs = src._coercionConfigs;
        _ctorDetector = src._ctorDetector;
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
        _coercionConfigs = src._coercionConfigs;
        _ctorDetector = src._ctorDetector;
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
        _coercionConfigs = src._coercionConfigs;
        _ctorDetector = src._ctorDetector;
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
        _coercionConfigs = src._coercionConfigs;
        _ctorDetector = src._ctorDetector;
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
        _coercionConfigs = src._coercionConfigs;
        _ctorDetector = src._ctorDetector;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }

    /**
     * @since 2.14
     */
    protected DeserializationConfig(DeserializationConfig src,
            DatatypeFeatures datatypeFeatures)
    {
        super(src, datatypeFeatures);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _coercionConfigs = src._coercionConfigs;
        _ctorDetector = src._ctorDetector;
        _parserFeatures = src._parserFeatures;
        _parserFeaturesToChange = src._parserFeaturesToChange;
        _formatReadFeatures = src._formatReadFeatures;
        _formatReadFeaturesToChange = src._formatReadFeaturesToChange;
    }

    // for unit tests only:
    protected BaseSettings getBaseSettings() { return _base; }

    /*
    /**********************************************************
    /* Life-cycle, general factory methods from MapperConfig(Base)
    /**********************************************************
     */

    @Override
    protected final DeserializationConfig _withBase(BaseSettings newBase) {
        return (_base == newBase) ? this : new DeserializationConfig(this, newBase);
    }

    @Override
    protected final DeserializationConfig _withMapperFeatures(long mapperFeatures) {
        return new DeserializationConfig(this, mapperFeatures, _deserFeatures,
                _parserFeatures, _parserFeaturesToChange,
                _formatReadFeatures, _formatReadFeaturesToChange);
    }

    @Override
    protected final DeserializationConfig _with(DatatypeFeatures dtFeatures) {
        return new DeserializationConfig(this, dtFeatures);
    }

    /*
    /**********************************************************
    /* Life-cycle, specific factory methods from MapperConfig
    /**********************************************************
     */

    @Override
    public DeserializationConfig with(SubtypeResolver str) {
        return (_subtypeResolver == str) ? this : new DeserializationConfig(this, str);
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
    public DeserializationConfig withView(Class<?> view) {
        return (_view == view) ? this : new DeserializationConfig(this, view);
    }

    @Override
    public DeserializationConfig with(ContextAttributes attrs) {
        return (attrs == _attributes) ? this : new DeserializationConfig(this, attrs);
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
        // 08-Oct-2018, tatu: Alas, complexity due to newly (2.10) refactored json-features:
        if (feature instanceof JsonReadFeature) {
            return _withJsonReadFeatures(feature);
        }
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
        // 08-Oct-2018, tatu: Alas, complexity due to newly (2.10) refactored json-features:
        if (features.length > 0 && (features[0] instanceof JsonReadFeature)) {
            return _withJsonReadFeatures(features);
        }
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
        // 08-Oct-2018, tatu: Alas, complexity due to newly (2.10) refactored json-features:
        if (feature instanceof JsonReadFeature) {
            return _withoutJsonReadFeatures(feature);
        }
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
        // 08-Oct-2018, tatu: Alas, complexity due to newly (2.10) refactored json-features:
        if (features.length > 0 && (features[0] instanceof JsonReadFeature)) {
            return _withoutJsonReadFeatures(features);
        }
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

    // temporary for 2.10
    private DeserializationConfig _withJsonReadFeatures(FormatFeature... features) {
        int parserSet = _parserFeatures;
        int parserMask = _parserFeaturesToChange;
        int newSet = _formatReadFeatures;
        int newMask = _formatReadFeaturesToChange;
        for (FormatFeature f : features) {
            final int mask = f.getMask();
            newSet |= mask;
            newMask |= mask;

            if (f instanceof JsonReadFeature) {
                JsonParser.Feature oldF = ((JsonReadFeature) f).mappedFeature();
                if (oldF != null) {
                    final int pmask = oldF.getMask();
                    parserSet |= pmask;
                    parserMask |= pmask;
                }
            }
        }
        return ((_formatReadFeatures == newSet) && (_formatReadFeaturesToChange == newMask)
                && (_parserFeatures == parserSet) && (_parserFeaturesToChange == parserMask)
                ) ? this :
            new DeserializationConfig(this,  _mapperFeatures, _deserFeatures,
                    parserSet, parserMask, newSet, newMask);
    }

    // temporary for 2.10
    private DeserializationConfig _withoutJsonReadFeatures(FormatFeature... features) {
        int parserSet = _parserFeatures;
        int parserMask = _parserFeaturesToChange;
        int newSet = _formatReadFeatures;
        int newMask = _formatReadFeaturesToChange;
        for (FormatFeature f : features) {
            final int mask = f.getMask();
            newSet &= ~mask;
            newMask |= mask;

            if (f instanceof JsonReadFeature) {
                JsonParser.Feature oldF = ((JsonReadFeature) f).mappedFeature();
                if (oldF != null) {
                    final int pmask = oldF.getMask();
                    parserSet &= ~pmask;
                    parserMask |= pmask;
                }
            }
        }
        return ((_formatReadFeatures == newSet) && (_formatReadFeaturesToChange == newMask)
                && (_parserFeatures == parserSet) && (_parserFeaturesToChange == parserMask)
                ) ? this :
            new DeserializationConfig(this,  _mapperFeatures, _deserFeatures,
                    parserSet, parserMask, newSet, newMask);
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
     * @since 2.12
     */
    public DeserializationConfig with(ConstructorDetector ctorDetector) {
        if (_ctorDetector == ctorDetector) {
            return this;
        }
        return new DeserializationConfig(this, ctorDetector);
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
    public JsonParser initialize(JsonParser p) {
        if (_parserFeaturesToChange != 0) {
            p.overrideStdFeatures(_parserFeatures, _parserFeaturesToChange);
        }
        if (_formatReadFeaturesToChange != 0) {
            p.overrideFormatFeatures(_formatReadFeatures, _formatReadFeaturesToChange);
        }
        return p;
    }

    /**
     * @since 2.12
     */
    public JsonParser initialize(JsonParser p, FormatSchema schema) {
        if (_parserFeaturesToChange != 0) {
            p.overrideStdFeatures(_parserFeatures, _parserFeaturesToChange);
        }
        if (_formatReadFeaturesToChange != 0) {
            p.overrideFormatFeatures(_formatReadFeatures, _formatReadFeaturesToChange);
        }
        if (schema != null) {
            p.setSchema(schema);
        }
        return p;
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

    /**
     * Accessor for checking whether give {@link DeserializationFeature}
     * is enabled or not.
     *
     * @param feature Feature to check
     *
     * @return True if feature is enabled; false otherwise
     */
    public final boolean isEnabled(DeserializationFeature feature) {
        return (_deserFeatures & feature.getMask()) != 0;
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

    /**
     * Convenience method equivalent to:
     *<code>
     *   isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
     *</code>
     *
     * @since 2.9
     */
    public final boolean requiresFullValue() {
        return DeserializationFeature.FAIL_ON_TRAILING_TOKENS.enabledIn(_deserFeatures);
    }

    /**
     * Accessor for checking whether give {@link DatatypeFeature}
     * is enabled or not.
     *
     * @param feature Feature to check
     *
     * @return True if feature is enabled; false otherwise
     *
     * @since 2.14
     */
    @Override
    public final boolean isEnabled(DatatypeFeature feature) {
        return _datatypeFeatures.isEnabled(feature);
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

    /**
     * @since 2.12
     */
    public ConstructorDetector getConstructorDetector() {
        if (_ctorDetector == null) {
            return ConstructorDetector.DEFAULT;
        }
        return _ctorDetector;
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
    public BeanDescription introspect(JavaType type) {
        return getClassIntrospector().forDeserialization(this, type, this);
    }

    /**
     * Method that will introspect subset of bean properties needed to
     * construct bean instance.
     */
    public BeanDescription introspectForCreation(JavaType type) {
        return getClassIntrospector().forCreation(this, type, this);
    }

    /**
     * @since 2.12
     */
    public BeanDescription introspectForBuilder(JavaType builderType, BeanDescription valueTypeDesc) {
        return getClassIntrospector().forDeserializationWithBuilder(this,
                builderType, this, valueTypeDesc);
    }

    /**
     * @since 2.0
     * @deprecated Since 2.12 - use variant that takes both builder and value type
     */
    @Deprecated
    public BeanDescription introspectForBuilder(JavaType type) {
        return getClassIntrospector().forDeserializationWithBuilder(this, type, this);
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
        return b.buildTypeDeserializer(this, baseType, subtypes);
    }

    /*
    /**********************************************************************
    /* CoercionConfig access
    /**********************************************************************
     */

    /**
     * General-purpose accessor for finding what to do when specified coercion
     * from shape that is now always allowed to be coerced from is requested.
     *
     * @param targetType Logical target type of coercion
     * @param targetClass Physical target type of coercion
     * @param inputShape Input shape to coerce from
     *
     * @return CoercionAction configured for specific coercion
     *
     * @since 2.12
     */
    public CoercionAction findCoercionAction(LogicalType targetType,
            Class<?> targetClass, CoercionInputShape inputShape)
    {
        return _coercionConfigs.findCoercion(this,
                targetType, targetClass, inputShape);
    }

    /**
     * More specialized accessor called in case of input being a blank
     * String (one consisting of only white space characters with length of at least one).
     * Will basically first determine if "blank as empty" is allowed: if not,
     * returns {@code actionIfBlankNotAllowed}, otherwise returns action for
     * {@link CoercionInputShape#EmptyString}.
     *
     * @param targetType Logical target type of coercion
     * @param targetClass Physical target type of coercion
     * @param actionIfBlankNotAllowed Return value to use in case "blanks as empty"
     *    is not allowed
     *
     * @return CoercionAction configured for specified coercion from blank string
     *
     * @since 2.12
     */
    public CoercionAction findCoercionFromBlankString(LogicalType targetType,
            Class<?> targetClass,
            CoercionAction actionIfBlankNotAllowed)
    {
        return _coercionConfigs.findCoercionFromBlankString(this,
                targetType, targetClass, actionIfBlankNotAllowed);
    }
}
