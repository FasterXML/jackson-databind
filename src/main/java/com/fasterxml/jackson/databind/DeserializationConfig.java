package com.fasterxml.jackson.databind;

import java.text.DateFormat;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.cfg.BaseSettings;
import com.fasterxml.jackson.databind.cfg.ConfigFeature;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.cfg.MapperConfigBase;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.type.ClassKey;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.LinkedNode;

/**
 * Object that contains baseline configuration for deserialization
 * process. An instance is owned by {@link ObjectMapper}, which
 * passes an immutable instance to be used for deserialization process.
 *<p>
 * Note that instances are considered immutable and as such no copies
 * should need to be created (there are some implementation details
 * with respect to mix-in annotations; where this is guaranteed as
 * long as caller follow "copy-then-use" pattern)
 */
public final class DeserializationConfig
    extends MapperConfigBase<DeserializationConfig.Feature, DeserializationConfig>
{
    /**
     * Enumeration that defines simple on/off features that affect
     * the way Java objects are deserialized from JSON
     *<p>
     * Note that features can be set both through
     * {@link ObjectMapper} (as sort of defaults) and through
     * {@link ObjectReader}.
     * In first case these defaults must follow "config-then-use" patterns
     * (i.e. defined once, not changed afterwards); all per-call
     * changes must be done using {@link ObjectReader}.
     */
    public enum Feature implements ConfigFeature
    {
        /*
        /******************************************************
        /* Type conversion features
        /******************************************************
         */

        /**
         * Feature that determines whether JSON floating point numbers
         * are to be deserialized into {@link java.math.BigDecimal}s
         * if only generic type description (either {@link Object} or
         * {@link Number}, or within untyped {@link java.util.Map}
         * or {@link java.util.Collection} context) is available.
         * If enabled such values will be deserialized as {@link java.math.BigDecimal}s;
         * if disabled, will be deserialized as {@link Double}s.
         * <p>
         * Feature is disabled by default, meaning that "untyped" floating
         * point numbers will by default be deserialized as {@link Double}s
         * (choice is for performance reason -- BigDecimals are slower than
         * Doubles).
         */
        USE_BIG_DECIMAL_FOR_FLOATS(false),

        /**
         * Feature that determines whether JSON integral (non-floating-point)
         * numbers are to be deserialized into {@link java.math.BigInteger}s
         * if only generic type description (either {@link Object} or
         * {@link Number}, or within untyped {@link java.util.Map}
         * or {@link java.util.Collection} context) is available.
         * If enabled such values will be deserialized as
         * {@link java.math.BigInteger}s;
         * if disabled, will be deserialized as "smallest" available type,
         * which is either {@link Integer}, {@link Long} or
         * {@link java.math.BigInteger}, depending on number of digits.
         * <p>
         * Feature is disabled by default, meaning that "untyped" floating
         * point numbers will by default be deserialized using whatever
         * is the most compact integral type, to optimize efficiency.
         */
        USE_BIG_INTEGER_FOR_INTS(false),

        // [JACKSON-652]
        /**
         * Feature that determines whether JSON Array is mapped to
         * <code>Object[]</code> or <code>List&lt;Object></code> when binding
         * "untyped" objects (ones with nominal type of <code>java.lang.Object</code>).
         * If true, binds as <code>Object[]</code>; if false, as <code>List&lt;Object></code>.
         *<p>
         * Feature is disabled by default, meaning that JSON arrays are bound as
         * {@link java.util.List}s.
         */
        USE_JAVA_ARRAY_FOR_JSON_ARRAY(false),
        
        /**
         * Feature that determines standard deserialization mechanism used for
         * Enum values: if enabled, Enums are assumed to have been serialized  using
         * return value of <code>Enum.toString()</code>;
         * if disabled, return value of <code>Enum.name()</code> is assumed to have been used.
         *<p>
         * Note: this feature should usually have same value
         * as {@link SerializationConfig.Feature#WRITE_ENUMS_USING_TO_STRING}.
         *<p>
         * Feature is disabled by default.
         */
        READ_ENUMS_USING_TO_STRING(false),
        
        /*
        /******************************************************
         *  Error handling features
        /******************************************************
         */

        /**
         * Feature that determines whether encountering of unknown
         * properties (ones that do not map to a property, and there is
         * no "any setter" or handler that can handle it)
         * should result in a failure (by throwing a
         * {@link JsonMappingException}) or not.
         * This setting only takes effect after all other handling
         * methods for unknown properties have been tried, and
         * property remains unhandled.
         *<p>
         * Feature is enabled by default (meaning that a
         * {@link JsonMappingException} will be thrown if an unknown property
         * is encountered).
         */
        FAIL_ON_UNKNOWN_PROPERTIES(true),

        /**
         * Feature that determines whether encountering of JSON null
         * is an error when deserializing into Java primitive types
         * (like 'int' or 'double'). If it is, a JsonProcessingException
         * is thrown to indicate this; if not, default value is used
         * (0 for 'int', 0.0 for double, same defaulting as what JVM uses).
         *<p>
         * Feature is disabled by default.
         */
        FAIL_ON_NULL_FOR_PRIMITIVES(false),

        /**
         * Feature that determines whether JSON integer numbers are valid
         * values to be used for deserializing Java enum values.
         * If set to 'false' numbers are acceptable and are used to map to
         * ordinal() of matching enumeration value; if 'true', numbers are
         * not allowed and a {@link JsonMappingException} will be thrown.
         * Latter behavior makes sense if there is concern that accidental
         * mapping from integer values to enums might happen (and when enums
         * are always serialized as JSON Strings)
         *<p>
         * Feature is disabled by default.
         */
        FAIL_ON_NUMBERS_FOR_ENUMS(false),

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
         *  Structural conversion features
        /******************************************************
         */

        /**
         * Feature that determines whether it is acceptable to coerce non-array
         * (in JSON) values to work with Java collection (arrays, java.util.Collection)
         * types. If enabled, collection deserializers will try to handle non-array
         * values as if they had "implicit" surrounding JSON array.
         * This feature is meant to be used for compatibility/interoperability reasons,
         * to work with packages (such as XML-to-JSON converters) that leave out JSON
         * array in cases where there is just a single element in array.
         *<p>
         * Feature is disabled by default.
         */
        ACCEPT_SINGLE_VALUE_AS_ARRAY(false),
        
        /**
         * Feature to allow "unwrapping" root-level JSON value, to match setting of
         * {@link SerializationConfig.Feature#WRAP_ROOT_VALUE} used for serialization.
         * Will verify that the root JSON value is a JSON Object, and that it has
         * a single property with expected root name. If not, a
         * {@link JsonMappingException} is thrown; otherwise value of the wrapped property
         * will be deserialized as if it was the root value.
         *<p>
         * Feature is disabled by default.
         */
        UNWRAP_ROOT_VALUE(false),

        /*
        /******************************************************
        /* Value conversion features
        /******************************************************
         */
        
        /**
         * Feature that can be enabled to allow JSON empty String
         * value ("") to be bound to POJOs as null.
         * If disabled, standard POJOs can only be bound from JSON null or
         * JSON Object (standard meaning that no custom deserializers or
         * constructors are defined; both of which can add support for other
         * kinds of JSON values); if enable, empty JSON String can be taken
         * to be equivalent of JSON null.
         *<p>
         * Feature is enabled by default.
         */
        ACCEPT_EMPTY_STRING_AS_NULL_OBJECT(false)
        
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
    /* Configuration settings for deserialization
    /**********************************************************
     */

    /**
     * Set of features enabled; actual type (kind of features)
     * depends on sub-classes.
     */
    protected final int _deserFeatures;

    /**
     * Linked list that contains all registered problem handlers.
     * Implementation as front-added linked list allows for sharing
     * of the list (tail) without copying the list.
     */
    protected final LinkedNode<DeserializationProblemHandler> _problemHandlers;
    
    /**
     * Factory used for constructing {@link com.fasterxml.jackson.core.JsonNode} instances.
     */
    protected final JsonNodeFactory _nodeFactory;
    
    /*
    /**********************************************************
    /* Life-cycle, constructors
    /**********************************************************
     */

    /**
     * Constructor used by ObjectMapper to create default configuration object instance.
     */
    public DeserializationConfig(BaseSettings base,
            SubtypeResolver str, Map<ClassKey,Class<?>> mixins)
    {
        super(base, str, mixins);
        _deserFeatures = collectFeatureDefaults(DeserializationConfig.Feature.class);
        _nodeFactory = JsonNodeFactory.instance;
        _problemHandlers = null;
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
    }

    private DeserializationConfig(DeserializationConfig src,
            int mapperFeatures, int deserFeatures)
    {
        super(src, mapperFeatures);
        _deserFeatures = deserFeatures;
        _nodeFactory = src._nodeFactory;
        _problemHandlers = src._problemHandlers;
    }
    
    private DeserializationConfig(DeserializationConfig src, BaseSettings base)
    {
        super(src, base);
        _deserFeatures = src._deserFeatures;
        _nodeFactory = src._nodeFactory;
        _problemHandlers = src._problemHandlers;
    }
    
    private DeserializationConfig(DeserializationConfig src, JsonNodeFactory f)
    {
        super(src);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = f;
    }

    private DeserializationConfig(DeserializationConfig src,
            LinkedNode<DeserializationProblemHandler> problemHandlers)
    {
        super(src);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = problemHandlers;
        _nodeFactory = src._nodeFactory;
    }

    private DeserializationConfig(DeserializationConfig src, String rootName)
    {
        super(src, rootName);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
    }

    private DeserializationConfig(DeserializationConfig src, Class<?> view)
    {
        super(src, view);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
    }
    
    /*
    /**********************************************************
    /* Life-cycle, factory methods from MapperConfig
    /**********************************************************
     */

    @Override
    public DeserializationConfig with(MapperConfig.Feature... features)
    {
        int newMapperFlags = _mapperFeatures;
        for (MapperConfig.Feature f : features) {
            newMapperFlags |= f.getMask();
        }
        return (newMapperFlags == _mapperFeatures) ? this :
            new DeserializationConfig(this, newMapperFlags, _deserFeatures);
    }

    @Override
    public DeserializationConfig without(MapperConfig.Feature... features)
    {
        int newMapperFlags = _mapperFeatures;
        for (MapperConfig.Feature f : features) {
             newMapperFlags &= ~f.getMask();
        }
        return (newMapperFlags == _mapperFeatures) ? this :
            new DeserializationConfig(this, newMapperFlags, _deserFeatures);
    }
    
    @Override
    public DeserializationConfig withClassIntrospector(ClassIntrospector ci) {
        return _withBase(_base.withClassIntrospector(ci));
    }

    @Override
    public DeserializationConfig withAnnotationIntrospector(AnnotationIntrospector ai) {
        return _withBase(_base.withAnnotationIntrospector(ai));
    }

    @Override
    public DeserializationConfig withVisibilityChecker(VisibilityChecker<?> vc) {
        return _withBase(_base.withVisibilityChecker(vc));
    }

    @Override
    public DeserializationConfig withVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility) {
        return _withBase( _base.withVisibility(forMethod, visibility));
    }
    
    @Override
    public DeserializationConfig withTypeResolverBuilder(TypeResolverBuilder<?> trb) {
        return _withBase(_base.withTypeResolverBuilder(trb));
    }

    @Override
    public DeserializationConfig withSubtypeResolver(SubtypeResolver str) {
        return (_subtypeResolver == str) ? this : new DeserializationConfig(this, str);
    }
    
    @Override
    public DeserializationConfig withPropertyNamingStrategy(PropertyNamingStrategy pns) {
        return _withBase(_base.withPropertyNamingStrategy(pns));
    }

    @Override
    public DeserializationConfig withRootName(String rootName) {
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
    public DeserializationConfig withTypeFactory(TypeFactory tf) {
        return _withBase( _base.withTypeFactory(tf));
    }

    @Override
    public DeserializationConfig withDateFormat(DateFormat df) {
        return _withBase(_base.withDateFormat(df));
    }
    
    @Override
    public DeserializationConfig withHandlerInstantiator(HandlerInstantiator hi) {
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
    
    private final DeserializationConfig _withBase(BaseSettings newBase) {
        return (_base == newBase) ? this : new DeserializationConfig(this, newBase);
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
    public DeserializationConfig withNodeFactory(JsonNodeFactory f) {
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

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig with(DeserializationConfig.Feature feature)
    {
        int newDeserFeatures = (_deserFeatures | feature.getMask());
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig with(DeserializationConfig.Feature first,
            DeserializationConfig.Feature... features)
    {
        int newDeserFeatures = _deserFeatures | first.getMask();
        for (Feature f : features) {
            newDeserFeatures |= f.getMask();
        }
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     */
    public DeserializationConfig without(DeserializationConfig.Feature feature)
    {
        int newDeserFeatures = _deserFeatures & ~feature.getMask();
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public DeserializationConfig without(DeserializationConfig.Feature first,
            DeserializationConfig.Feature... features)
    {
        int newDeserFeatures = _deserFeatures & ~first.getMask();
        for (Feature f : features) {
            newDeserFeatures &= ~f.getMask();
        }
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures);
    }
    
    /*
    /**********************************************************
    /* MapperConfig implementation
    /**********************************************************
     */

    @Override
    public final int getFeatureFlags() {
        return _deserFeatures;
    }
    
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
        if (isEnabled(MapperConfig.Feature.USE_ANNOTATIONS)) {
            return super.getAnnotationIntrospector();
        }
        return NopAnnotationIntrospector.instance;
    }

    @Override
    public boolean useRootWrapping()
    {
        if (_rootName != null) { // empty String disables wrapping; non-empty enables
            return (_rootName.length() > 0);
        }
        return isEnabled(DeserializationConfig.Feature.UNWRAP_ROOT_VALUE);
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
        if (!isEnabled(MapperConfig.Feature.AUTO_DETECT_SETTERS)) {
            vchecker = vchecker.withSetterVisibility(Visibility.NONE);
        }
        if (!isEnabled(MapperConfig.Feature.AUTO_DETECT_CREATORS)) {
            vchecker = vchecker.withCreatorVisibility(Visibility.NONE);
        }
        if (!isEnabled(MapperConfig.Feature.AUTO_DETECT_FIELDS)) {
            vchecker = vchecker.withFieldVisibility(Visibility.NONE);
        }
        return vchecker;
    }

    public final boolean isEnabled(DeserializationConfig.Feature f) {
        return (_deserFeatures & f.getMask()) != 0;
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
    
    /**
     * Method called during deserialization if Base64 encoded content
     * needs to be decoded. Default version just returns default Jackson
     * uses, which is modified-mime which does not add linefeeds (because
     * those would have to be escaped in JSON strings).
     */
    public Base64Variant getBase64Variant() {
        return Base64Variants.getDefaultVariant();
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
}
