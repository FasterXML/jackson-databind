package tools.jackson.databind;

import tools.jackson.core.*;
import tools.jackson.databind.cfg.*;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.introspect.*;
import tools.jackson.databind.jsontype.*;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.util.ArrayIterator;
import tools.jackson.databind.util.LinkedNode;
import tools.jackson.databind.util.RootNameLookup;

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
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************************
    /* Deserialization, parser, format features
    /**********************************************************************
     */

    /**
     * Set of {@link DeserializationFeature}s enabled.
     */
    protected final int _deserFeatures;

    /**
     * States of {@link tools.jackson.core.StreamReadFeature}s to enable/disable.
     */
    protected final int _streamReadFeatures;

    /**
     * States of {@link tools.jackson.core.FormatFeature}s to enable/disable.
     */
    protected final int _formatReadFeatures;

    /*
    /**********************************************************************
    /* Configured helper objects
    /**********************************************************************
     */

    /**
     * Linked list that contains all registered problem handlers.
     * Implementation as front-added linked list allows for sharing
     * of the list (tail) without copying the list.
     */
    protected final LinkedNode<DeserializationProblemHandler> _problemHandlers;

    /**
     * List of objects that may be able to resolve abstract types to
     * concrete types. Used by functionality like "mr Bean" to materialize
     * types as needed, although may be used for other kinds of defaulting
     * as well.
     */
    protected final AbstractTypeResolver[] _abstractTypeResolvers;

    /**
     * Configured coercion rules for coercions from secondary input
     * shapes.
     */
    protected final CoercionConfigs _coercionConfigs;

    /*
    /**********************************************************************
    /* Life-cycle, primary constructors for new instances
    /**********************************************************************
     */

    /**
     * @since 3.0
     */
    public DeserializationConfig(MapperBuilder<?,?> b, long mapperFeatures,
            int deserFeatures, int streamReadFeatures, int formatReadFeatures,
            ConfigOverrides configOverrides, CoercionConfigs coercionConfigs,
            TypeFactory tf, ClassIntrospector classIntr, MixInHandler mixins, SubtypeResolver str,
            ContextAttributes defaultAttrs, RootNameLookup rootNames,
            AbstractTypeResolver[] atrs)
    {
        super(b, mapperFeatures, tf, classIntr, mixins, str, configOverrides,
                defaultAttrs, rootNames);
        _deserFeatures = deserFeatures;
        _streamReadFeatures = streamReadFeatures;
        _formatReadFeatures = formatReadFeatures;
        _problemHandlers = b.deserializationProblemHandlers();
        _coercionConfigs = coercionConfigs;
        _abstractTypeResolvers = atrs;
    }

    /*
    /**********************************************************************
    /* Life-cycle, secondary constructors to support
    /* "mutant factories", with single property changes
    /**********************************************************************
     */

    private DeserializationConfig(DeserializationConfig src,
            int deserFeatures, int streamReadFeatures, int formatReadFeatures)
    {
        super(src);
        _deserFeatures = deserFeatures;
        _streamReadFeatures = streamReadFeatures;
        _formatReadFeatures = formatReadFeatures;
        _coercionConfigs = src._coercionConfigs;
        _problemHandlers = src._problemHandlers;
        _abstractTypeResolvers = src._abstractTypeResolvers;
    }

    private DeserializationConfig(DeserializationConfig src, BaseSettings base)
    {
        super(src, base);
        _deserFeatures = src._deserFeatures;
        _streamReadFeatures = src._streamReadFeatures;
        _formatReadFeatures = src._formatReadFeatures;
        _coercionConfigs = src._coercionConfigs;
        _problemHandlers = src._problemHandlers;
        _abstractTypeResolvers = src._abstractTypeResolvers;
    }

    private DeserializationConfig(DeserializationConfig src,
            LinkedNode<DeserializationProblemHandler> problemHandlers,
            AbstractTypeResolver[] atr)
    {
        super(src);
        _deserFeatures = src._deserFeatures;
        _streamReadFeatures = src._streamReadFeatures;
        _formatReadFeatures = src._formatReadFeatures;
        _coercionConfigs = src._coercionConfigs;
        _problemHandlers = problemHandlers;
        _abstractTypeResolvers = atr;
    }

    private DeserializationConfig(DeserializationConfig src, PropertyName rootName)
    {
        super(src, rootName);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _streamReadFeatures = src._streamReadFeatures;
        _coercionConfigs = src._coercionConfigs;
        _formatReadFeatures = src._formatReadFeatures;
        _abstractTypeResolvers = src._abstractTypeResolvers;
    }

    private DeserializationConfig(DeserializationConfig src, Class<?> view)
    {
        super(src, view);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _streamReadFeatures = src._streamReadFeatures;
        _coercionConfigs = src._coercionConfigs;
        _formatReadFeatures = src._formatReadFeatures;
        _abstractTypeResolvers = src._abstractTypeResolvers;
    }

    protected DeserializationConfig(DeserializationConfig src, ContextAttributes attrs)
    {
        super(src, attrs);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _coercionConfigs = src._coercionConfigs;
        _streamReadFeatures = src._streamReadFeatures;
        _formatReadFeatures = src._formatReadFeatures;
        _abstractTypeResolvers = src._abstractTypeResolvers;
    }

    protected DeserializationConfig(DeserializationConfig src, DatatypeFeatures dtFeatures)
    {
        super(src, dtFeatures);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _coercionConfigs = src._coercionConfigs;
        _streamReadFeatures = src._streamReadFeatures;
        _formatReadFeatures = src._formatReadFeatures;
        _abstractTypeResolvers = src._abstractTypeResolvers;
    }

    // for unit tests only:
    protected BaseSettings getBaseSettings() { return _base; }

    /*
    /**********************************************************************
    /* Life-cycle, general factory methods from MapperConfig(Base)
    /**********************************************************************
     */

    @Override
    protected final DeserializationConfig _withBase(BaseSettings newBase) {
        return (_base == newBase) ? this : new DeserializationConfig(this, newBase);
    }

    @Override
    protected final DeserializationConfig _with(DatatypeFeatures dtFeatures) {
        return new DeserializationConfig(this, dtFeatures);
    }

    /*
    /**********************************************************************
    /* Life-cycle, specific factory methods from MapperConfig
    /**********************************************************************
     */

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
    /**********************************************************************
    /* Life-cycle, DeserializationFeature-based factory methods
    /**********************************************************************
     */

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig with(DeserializationFeature feature)
    {
        int newDeserFeatures = (_deserFeatures | feature.getMask());
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, newDeserFeatures, _streamReadFeatures,
                    _formatReadFeatures);
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
            new DeserializationConfig(this, newDeserFeatures, _streamReadFeatures,
                    _formatReadFeatures);
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
            new DeserializationConfig(this, newDeserFeatures,
                    _streamReadFeatures, _formatReadFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     */
    public DeserializationConfig without(DeserializationFeature feature)
    {
        int newDeserFeatures = _deserFeatures & ~feature.getMask();
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, newDeserFeatures,
                    _streamReadFeatures, _formatReadFeatures);
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
            new DeserializationConfig(this, newDeserFeatures, _streamReadFeatures,
                    _formatReadFeatures);
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
            new DeserializationConfig(this,
                    newDeserFeatures, _streamReadFeatures, _formatReadFeatures);
    }

    /*
    /**********************************************************************
    /* Life-cycle, JsonParser.Feature-based factory methods
    /**********************************************************************
     */

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig with(StreamReadFeature feature)
    {
        int newSet = _streamReadFeatures | feature.getMask();
        return (_streamReadFeatures == newSet)? this :
            new DeserializationConfig(this,
                    _deserFeatures, newSet, _formatReadFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig withFeatures(StreamReadFeature... features)
    {
        int newSet = _streamReadFeatures;
        for (StreamReadFeature f : features) {
            newSet |= f.getMask();
        }
        return (_streamReadFeatures == newSet) ? this :
            new DeserializationConfig(this, _deserFeatures, newSet,
                    _formatReadFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     */
    public DeserializationConfig without(StreamReadFeature feature)
    {
        int newSet = _streamReadFeatures & ~feature.getMask();
        return (_streamReadFeatures == newSet) ? this :
            new DeserializationConfig(this, _deserFeatures, newSet,
                    _formatReadFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public DeserializationConfig withoutFeatures(StreamReadFeature... features)
    {
        int newSet = _streamReadFeatures;
        for (StreamReadFeature f : features) {
            newSet &= ~f.getMask();
        }
        return (_streamReadFeatures == newSet)? this :
            new DeserializationConfig(this, _deserFeatures, newSet, _formatReadFeatures);
    }

    /*
    /**********************************************************************
    /* Life-cycle, JsonParser.FormatFeature-based factory methods
    /**********************************************************************
     */

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig with(FormatFeature feature)
    {
        int newSet = _formatReadFeatures | feature.getMask();
        return (_formatReadFeatures == newSet) ? this
                : new DeserializationConfig(this,
                        _deserFeatures, _streamReadFeatures,  newSet);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig withFeatures(FormatFeature... features)
    {
        int newSet = _formatReadFeatures;
        for (FormatFeature f : features) {
            newSet |= f.getMask();
        }
        return (_formatReadFeatures == newSet) ? this
                : new DeserializationConfig(this,
                        _deserFeatures, _streamReadFeatures, newSet);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     */
    public DeserializationConfig without(FormatFeature feature)
    {
        int newSet = _formatReadFeatures & ~feature.getMask();
        return (_formatReadFeatures == newSet) ? this
                : new DeserializationConfig(this,
                        _deserFeatures, _streamReadFeatures, newSet);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public DeserializationConfig withoutFeatures(FormatFeature... features)
    {
        int newSet = _formatReadFeatures;
        for (FormatFeature f : features) {
            newSet &= ~f.getMask();
        }
        return (_formatReadFeatures == newSet) ? this
                : new DeserializationConfig(this,
                        _deserFeatures, _streamReadFeatures, newSet);
    }

    /*
    /**********************************************************************
    /* Life-cycle, deserialization-specific factory methods
    /**********************************************************************
     */

    /**
     * Method that can be used to add a handler that can (try to)
     * resolve non-fatal deserialization problems.
     */
    public DeserializationConfig withHandler(DeserializationProblemHandler h)
    {
        // Sanity check: let's prevent adding same handler multiple times
        return LinkedNode.contains(_problemHandlers, h) ? this
                : new DeserializationConfig(this,
                        new LinkedNode<DeserializationProblemHandler>(h, _problemHandlers),
                        _abstractTypeResolvers);
    }

    /**
     * Method for removing all configured problem handlers; usually done to replace
     * existing handler(s) with different one(s)
     */
    public DeserializationConfig withNoProblemHandlers() {
        return (_problemHandlers == null) ? this
                : new DeserializationConfig(this,
                        (LinkedNode<DeserializationProblemHandler>) null,
                        _abstractTypeResolvers);
    }

    /*
    /**********************************************************************
    /* Support for ObjectReadContext
    /**********************************************************************
     */

    /**
     * @since 3.0
     */
    public int getStreamReadFeatures() {
        return _streamReadFeatures;
    }

    /**
     * @since 3.0
     */
    public int getFormatReadFeatures() {
        return _formatReadFeatures;
    }

    /*
    /**********************************************************************
    /* MapperConfig implementation/overrides: other
    /**********************************************************************
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

    public final boolean isEnabled(StreamReadFeature f) {
        return (_streamReadFeatures & f.getMask()) != 0;
    }

    public final boolean hasFormatFeature(FormatFeature f) {
        return (_formatReadFeatures & f.getMask()) != 0;
    }

    /**
     * Bulk access method for checking that all features specified by
     * mask are enabled.
     */
    public final boolean hasDeserializationFeatures(int featureMask) {
        return (_deserFeatures & featureMask) == featureMask;
    }

    /**
     * Bulk access method for checking that at least one of features specified by
     * mask is enabled.
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
     */
    public final boolean requiresFullValue() {
        return DeserializationFeature.FAIL_ON_TRAILING_TOKENS.enabledIn(_deserFeatures);
    }

    /*
    /**********************************************************************
    /* Abstract type mapping
    /**********************************************************************
     */

    /**
     * @since 3.0
     */
    public boolean hasAbstractTypeResolvers() { return _abstractTypeResolvers.length > 0; }

    /**
     * @since 3.0
     */
    public Iterable<AbstractTypeResolver> abstractTypeResolvers() {
        return new ArrayIterator<AbstractTypeResolver>(_abstractTypeResolvers);
    }

    /**
     * @since 3.0
     */
    public JavaType mapAbstractType(JavaType type)
    {
        if (!hasAbstractTypeResolvers()) {
            return type;
        }
        // first, general mappings
        while (true) {
            JavaType next = _mapAbstractType2(type);
            if (next == null) {
                return type;
            }
            // Should not have to worry about cycles; but better verify since they will invariably occur... :-)
            // (also: guard against invalid resolution to a non-related type)
            Class<?> prevCls = type.getRawClass();
            Class<?> nextCls = next.getRawClass();
            if ((prevCls == nextCls) || !prevCls.isAssignableFrom(nextCls)) {
                throw new IllegalArgumentException("Invalid abstract type resolution from "+type+" to "+next+": latter is not a subtype of former");
            }
            type = next;
        }
    }

    /**
     * Method that will find abstract type mapping for specified type, doing a single
     * lookup through registered abstract type resolvers; will not do recursive lookups.
     */
    private JavaType _mapAbstractType2(JavaType type)
    {
        Class<?> currClass = type.getRawClass();
        for (AbstractTypeResolver resolver : abstractTypeResolvers()) {
            JavaType concrete = resolver.findTypeMapping(this, type);
            if ((concrete != null) && !concrete.hasRawClass(currClass)) {
                return concrete;
            }
        }
        return null;
    }

    /*
    /**********************************************************************
    /* Other configuration
    /**********************************************************************
     */

    /**
     * Method for getting head of the problem handler chain. May be null,
     * if no handlers have been added.
     */
    public LinkedNode<DeserializationProblemHandler> getProblemHandlers() {
        return _problemHandlers;
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
