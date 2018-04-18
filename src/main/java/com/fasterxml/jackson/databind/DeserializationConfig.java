package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.*;
import com.fasterxml.jackson.databind.util.ArrayIterator;
import com.fasterxml.jackson.databind.util.ClassUtil;
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
     * States of {@link com.fasterxml.jackson.core.JsonParser.Feature}s to enable/disable.
     */
    protected final int _parserFeatures;

    /**
     * States of {@link com.fasterxml.jackson.core.FormatFeature}s to enable/disable.
     */
    protected final int _formatParserFeatures;

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
     *
     * @since 3.0
     */
    protected final AbstractTypeResolver[] _abstractTypeResolvers;

    /*
    /**********************************************************************
    /* Life-cycle, primary constructors for new instances
    /**********************************************************************
     */

    /**
     * @since 3.0
     */
    public DeserializationConfig(MapperBuilder<?,?> b, int mapperFeatures,
            int deserFeatures, int parserFeatures, int formatParserFeatures,
            MixInHandler mixins, RootNameLookup rootNames, ConfigOverrides configOverrides,
            AbstractTypeResolver[] atrs)
    {
        super(b, mapperFeatures, mixins, rootNames, configOverrides);
        _deserFeatures = deserFeatures;
        _parserFeatures = parserFeatures;
        _formatParserFeatures = formatParserFeatures;
        _problemHandlers = b.deserializationProblemHandlers();
        _abstractTypeResolvers = atrs;
    }

    /*
    /**********************************************************************
    /* Life-cycle, secondary constructors to support
    /* "mutant factories", with single property changes
    /**********************************************************************
     */

    private DeserializationConfig(DeserializationConfig src,
            int deserFeatures, int parserFeatures,
            int formatParserFeatures)
    {
        super(src);
        _deserFeatures = deserFeatures;
        _parserFeatures = parserFeatures;
        _formatParserFeatures = formatParserFeatures;
        _problemHandlers = src._problemHandlers;
        _abstractTypeResolvers = src._abstractTypeResolvers;
    }

    private DeserializationConfig(DeserializationConfig src, BaseSettings base)
    {
        super(src, base);
        _deserFeatures = src._deserFeatures;
        _parserFeatures = src._parserFeatures;
        _formatParserFeatures = src._formatParserFeatures;
        _problemHandlers = src._problemHandlers;
        _abstractTypeResolvers = src._abstractTypeResolvers;
    }

    private DeserializationConfig(DeserializationConfig src,
            LinkedNode<DeserializationProblemHandler> problemHandlers,
            AbstractTypeResolver[] atr)
    {
        super(src);
        _deserFeatures = src._deserFeatures;
        _parserFeatures = src._parserFeatures;
        _formatParserFeatures = src._formatParserFeatures;
        _problemHandlers = problemHandlers;
        _abstractTypeResolvers = atr;
    }

    private DeserializationConfig(DeserializationConfig src, PropertyName rootName)
    {
        super(src, rootName);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _parserFeatures = src._parserFeatures;
        _formatParserFeatures = src._formatParserFeatures;
        _abstractTypeResolvers = src._abstractTypeResolvers;
    }

    private DeserializationConfig(DeserializationConfig src, Class<?> view)
    {
        super(src, view);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _parserFeatures = src._parserFeatures;
        _formatParserFeatures = src._formatParserFeatures;
        _abstractTypeResolvers = src._abstractTypeResolvers;
    }

    protected DeserializationConfig(DeserializationConfig src, ContextAttributes attrs)
    {
        super(src, attrs);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _parserFeatures = src._parserFeatures;
        _formatParserFeatures = src._formatParserFeatures;
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
            new DeserializationConfig(this, newDeserFeatures, _parserFeatures,
                    _formatParserFeatures);
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
            new DeserializationConfig(this, newDeserFeatures, _parserFeatures,
                    _formatParserFeatures);
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
                    _parserFeatures, _formatParserFeatures);
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
                    _parserFeatures, _formatParserFeatures);
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
            new DeserializationConfig(this, newDeserFeatures, _parserFeatures,
                    _formatParserFeatures);
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
                    newDeserFeatures, _parserFeatures, _formatParserFeatures);
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
    public DeserializationConfig with(JsonParser.Feature feature)
    {
        int newSet = _parserFeatures | feature.getMask();
        return (_parserFeatures == newSet)? this :
            new DeserializationConfig(this,
                    _deserFeatures, newSet, _formatParserFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig withFeatures(JsonParser.Feature... features)
    {
        int newSet = _parserFeatures;
        for (JsonParser.Feature f : features) {
            newSet |= f.getMask();
        }
        return (_parserFeatures == newSet) ? this :
            new DeserializationConfig(this, _deserFeatures, newSet,
                    _formatParserFeatures);
    }
    
    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     */
    public DeserializationConfig without(JsonParser.Feature feature)
    {
        int newSet = _parserFeatures & ~feature.getMask();
        return (_parserFeatures == newSet) ? this :
            new DeserializationConfig(this, _deserFeatures, newSet,
                    _formatParserFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public DeserializationConfig withoutFeatures(JsonParser.Feature... features)
    {
        int newSet = _parserFeatures;
        for (JsonParser.Feature f : features) {
            newSet &= ~f.getMask();
        }
        return (_parserFeatures == newSet)? this :
            new DeserializationConfig(this, _deserFeatures, newSet, _formatParserFeatures);
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
        int newSet = _formatParserFeatures | feature.getMask();
        return (_formatParserFeatures == newSet) ? this
                : new DeserializationConfig(this,
                        _deserFeatures, _parserFeatures,  newSet);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig withFeatures(FormatFeature... features)
    {
        int newSet = _formatParserFeatures;
        for (FormatFeature f : features) {
            newSet |= f.getMask();
        }
        return (_formatParserFeatures == newSet) ? this
                : new DeserializationConfig(this,
                        _deserFeatures, _parserFeatures, newSet);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     */
    public DeserializationConfig without(FormatFeature feature)
    {
        int newSet = _formatParserFeatures & ~feature.getMask();
        return (_formatParserFeatures == newSet) ? this
                : new DeserializationConfig(this,
                        _deserFeatures, _parserFeatures, newSet);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public DeserializationConfig withoutFeatures(FormatFeature... features)
    {
        int newSet = _formatParserFeatures;
        for (FormatFeature f : features) {
            newSet &= ~f.getMask();
        }
        return (_formatParserFeatures == newSet) ? this
                : new DeserializationConfig(this,
                        _deserFeatures, _parserFeatures, newSet);
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
                        (LinkedNode<DeserializationProblemHandler>) null, _abstractTypeResolvers);
    }

    /*
    /**********************************************************************
    /* Support for ObjectReadContext
    /**********************************************************************
     */

    /**
     * @since 3.0
     */
    public int getParserFeatures() {
        return _parserFeatures;
    }

    /**
     * @since 3.0
     */
    public int getFormatReadFeatures() {
        return _formatParserFeatures;
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

    public final boolean isEnabled(JsonParser.Feature f) {
        return (_parserFeatures & f.getMask()) != 0;
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
            if (ClassUtil.rawClass(concrete) != currClass) {
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
    /* Introspection methods
    /**********************************************************************
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

    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspectForBuilder(JavaType type) {
        return (T) getClassIntrospector().forDeserializationWithBuilder(this, type, this);
    }

    /*
    /**********************************************************************
    /* Support for polymorphic type handling
    /**********************************************************************
     */

    /**
     * Helper method that is needed to properly handle polymorphic referenced
     * types, such as types referenced by {@link java.util.concurrent.atomic.AtomicReference},
     * or various "optional" types.
     */
    public TypeDeserializer findTypeDeserializer(JavaType baseType)
        throws JsonMappingException
    {
        BeanDescription beanDesc = introspectClassAnnotations(baseType.getRawClass());
        return getTypeResolverProvider().findTypeDeserializer(this, baseType,
                beanDesc.getClassInfo());
    }
}
