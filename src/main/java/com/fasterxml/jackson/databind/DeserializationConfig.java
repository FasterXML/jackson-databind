package com.fasterxml.jackson.databind;

import java.text.DateFormat;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.cfg.BaseSettings;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.cfg.MapperConfigBase;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.DeserializerFactory;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.type.ClassKey;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.LinkedNode;

/**
 * Object that contains baseline configuration for deserialization
 * process. An instance is owned by {@link ObjectMapper}, which makes
 * a copy that is passed during serialization process to
 * {@link DeserializerProvider} and {@link DeserializerFactory}.
 *<p>
 * Note: although configuration settings can be changed at any time
 * (for factories and instances), they are not guaranteed to have
 * effect if called after constructing relevant mapper or deserializer
 * instance. This because some objects may be configured, constructed and
 * cached first time they are needed.
 *<p>
 * Note: as of 2.0, goal is still to make config instances fully immutable.
 */
public class DeserializationConfig
    extends MapperConfigBase<DeserializationConfig.Feature, DeserializationConfig>
{
    /**
     * Enumeration that defines togglable features that guide
     * the serialization feature.
     * 
     * Note that some features can only be set for
     * {@link ObjectMapper} (as default for all deserializations),
     * while others can be changed on per-call basis using {@link ObjectReader}.
     * Ones that can be used on per-call basis will return <code>true</code>
     * from {@link #canUseForInstance}.
     * Trying enable/disable ObjectMapper-only feature will result in
     * an {@link IllegalArgumentException}.
     */
    public enum Feature implements MapperConfig.ConfigFeature
    {
        /*
        /******************************************************
         *  Introspection features
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
        USE_ANNOTATIONS(true, false),

        /**
         * Feature that determines whether "setter" methods are
         * automatically detected based on standard Bean naming convention
         * or not. If yes, then all public one-argument methods that
         * start with prefix "set"
         * are considered setters. If disabled, only methods explicitly
         * annotated are considered setters.
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<P>
         * Feature is enabled by default.
         */
        AUTO_DETECT_SETTERS(true, false),

        /**
         * Feature that determines whether "creator" methods are
         * automatically detected by consider public constructors,
         * and static single argument methods with name "valueOf".
         * If disabled, only methods explicitly annotated are considered
         * creator methods (except for the no-arg default constructor which
         * is always considered a factory method).
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<P>
         * Feature is enabled by default.
         */
        AUTO_DETECT_CREATORS(true, false),

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
         *<P>
         * Feature is enabled by default.
         */
        AUTO_DETECT_FIELDS(true, false),

        /**
         * Feature that determines whether otherwise regular "getter"
         * methods (but only ones that handle Collections and Maps,
         * not getters of other type)
         * can be used for purpose of getting a reference to a Collection
         * and Map to modify the property, without requiring a setter
         * method.
         * This is similar to how JAXB framework sets Collections and
         * Maps: no setter is involved, just setter.
         *<p>
         * Note that such getters-as-setters methods have lower
         * precedence than setters, so they are only used if no
         * setter is found for the Map/Collection property.
         *<p>
         * Feature is enabled by default.
         */
        USE_GETTERS_AS_SETTERS(true, false),

        /**
         * Feature that determines whether method and field access
         * modifier settings can be overridden when accessing
         * properties. If enabled, method
         * {@link java.lang.reflect.AccessibleObject#setAccessible}
         * may be called to enable access to otherwise unaccessible
         * objects.
         */
        CAN_OVERRIDE_ACCESS_MODIFIERS(true, false),

        /*
        /******************************************************
        /* Type conversion features
        /******************************************************
         */

        /**
         * Feature that determines whether Json floating point numbers
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
         * Feature <b>can</b> be changed
         * after first call to serialization; that is, it is changeable
         * via {@link ObjectWriter}
         */
        USE_BIG_DECIMAL_FOR_FLOATS(false, true),

        /**
         * Feature that determines whether Json integral (non-floating-point)
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
         * Feature <b>can</b> be changed
         * after first call to serialization; that is, it is changeable
         * via {@link ObjectWriter}
         */
        USE_BIG_INTEGER_FOR_INTS(false, true),

        // [JACKSON-652]
        /**
         * Feature that determines whether JSON Array is mapped to
         * <code>Object[]</code> or <code>List&lt;Object></code> when binding
         * "untyped" objects (ones with nominal type of <code>java.lang.Object</code>).
         * If true, binds as <code>Object[]</code>; if false, as <code>List&lt;Object></code>.
         *<p>
         * Feature is disabled by default, meaning that JSON arrays are bound as
         * {@link java.util.List}s. It <b>can</b> be changed
         * after first call to serialization; that is, it is changeable
         * via {@link ObjectWriter}
         */
        USE_JAVA_ARRAY_FOR_JSON_ARRAY(false, true),
        
        /**
         * Feature that determines standard deserialization mechanism used for
         * Enum values: if enabled, Enums are assumed to have been serialized  using
         * return value of <code>Enum.toString()</code>;
         * if disabled, return value of <code>Enum.name()</code> is assumed to have been used.
         * Since pre-1.6 method was to use Enum name, this is the default.
         *<p>
         * Note: this feature should usually have same value
         * as {@link SerializationConfig.Feature#WRITE_ENUMS_USING_TO_STRING}.
         *<p>
         * Feature is disabled by default. It <b>can</b> be changed
         * after first call to serialization; that is, it is changeable
         * via {@link ObjectWriter}
         */
        READ_ENUMS_USING_TO_STRING(false, true),
        
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
         * is encountered). It <b>can</b> be changed
         * after first call to serialization; that is, it is changeable
         * via {@link ObjectWriter}
         */
        FAIL_ON_UNKNOWN_PROPERTIES(true, true),

        /**
         * Feature that determines whether encountering of JSON null
         * is an error when deserializing into Java primitive types
         * (like 'int' or 'double'). If it is, a JsonProcessingException
         * is thrown to indicate this; if not, default value is used
         * (0 for 'int', 0.0 for double, same defaulting as what JVM uses).
         *<p>
         * Feature is disabled by default. It <b>can</b> be changed
         * after first call to serialization; that is, it is changeable
         * via {@link ObjectWriter}
         */
        FAIL_ON_NULL_FOR_PRIMITIVES(false, true),

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
         * Feature is disabled by default. It <b>can</b> be changed
         * after first call to serialization; that is, it is changeable
         * via {@link ObjectWriter}
         */
        FAIL_ON_NUMBERS_FOR_ENUMS(false, true),

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
         * Feature is enabled by default. It <b>can</b> be changed
         * after first call to serialization; that is, it is changeable
         * via {@link ObjectWriter}
         */
        WRAP_EXCEPTIONS(true, true),
        
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
         * Feature is disabled by default. It <b>can</b> be changed
         * after first call to serialization; that is, it is changeable
         * via {@link ObjectWriter}
         */
        ACCEPT_SINGLE_VALUE_AS_ARRAY(false, true),
        
        /**
         * Feature to allow "unwrapping" root-level JSON value, to match setting of
         * {@link SerializationConfig.Feature#WRAP_ROOT_VALUE} used for serialization.
         * Will verify that the root JSON value is a JSON Object, and that it has
         * a single property with expected root name. If not, a
         * {@link JsonMappingException} is thrown; otherwise value of the wrapped property
         * will be deserialized as if it was the root value.
         *<p>
         * Feature is disabled by default. It <b>can</b> be changed
         * after first call to serialization; that is, it is changeable
         * via {@link ObjectWriter}
         */
        UNWRAP_ROOT_VALUE(false, true),

        /*
        /******************************************************
         *  Value conversion features
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
         * Feature is enabled by default. It <b>can</b> be changed
         * after first call to serialization; that is, it is changeable
         * via {@link ObjectWriter}
         */
        ACCEPT_EMPTY_STRING_AS_NULL_OBJECT(false, true)
        
        ;

        private final boolean _defaultState;

        /**
         * Whether feature can be used and changed on per-call basis (true),
         * or just for <code>ObjectMapper</code> (false).
         */
        private final boolean _canUseForInstance;
        
        private Feature(boolean defaultState, boolean canUseForInstance) {
            _defaultState = defaultState;
            _canUseForInstance = canUseForInstance;
        }

        @Override
        public boolean enabledByDefault() { return _defaultState; }

        @Override
        public boolean canUseForInstance() { return _canUseForInstance; }
    
        @Override
        public int getMask() { return (1 << ordinal()); }
    }

    /*
    /**********************************************************
    /* Configuration settings for deserialization
    /**********************************************************
     */

    /**
     * Linked list that contains all registered problem handlers.
     * Implementation as front-added linked list allows for sharing
     * of the list (tail) without copying the list.
     */
    protected LinkedNode<DeserializationProblemHandler> _problemHandlers;
    
    /**
     * Factory used for constructing {@link com.fasterxml.jackson.core.JsonNode} instances.
     */
    protected final JsonNodeFactory _nodeFactory;

    /**
     * Feature flag from {@link SerializationConfig} which is needed to
     * know if serializer will by default sort properties in
     * alphabetic order.
     *<p>
     * Note that although this property is not marked as final,
     * it is handled like it was, except for the fact that it is
     * assigned with a call to {@link #passSerializationFeatures}
     * instead of constructor.
     */
    protected boolean _sortPropertiesAlphabetically;
    
    /*
    /**********************************************************
    /* Life-cycle, constructors
    /**********************************************************
     */

    /**
     * Constructor used by ObjectMapper to create default configuration object instance.
     */
    public DeserializationConfig(ClassIntrospector<? extends BeanDescription> intr,
            AnnotationIntrospector annIntr, VisibilityChecker<?> vc,
            SubtypeResolver subtypeResolver, PropertyNamingStrategy propertyNamingStrategy,
            TypeFactory typeFactory, HandlerInstantiator handlerInstantiator)
    {
        super(intr, annIntr, vc, subtypeResolver, propertyNamingStrategy, typeFactory, handlerInstantiator,
                collectFeatureDefaults(DeserializationConfig.Feature.class));
        _nodeFactory = JsonNodeFactory.instance;
    }
    
    protected DeserializationConfig(DeserializationConfig src) {
        this(src, src._base);
    }

    /**
     * Copy constructor used to create a non-shared instance with given mix-in
     * annotation definitions and subtype resolver.
     */
    private DeserializationConfig(DeserializationConfig src,
            HashMap<ClassKey,Class<?>> mixins, SubtypeResolver str)
    {
        this(src, src._base);
        _mixInAnnotations = mixins;
        _subtypeResolver = str;
    }

    private DeserializationConfig(DeserializationConfig src,
            HashMap<ClassKey,Class<?>> mixins, SubtypeResolver str,
            int features)
    {
        super(src, src._base, str, features);

        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _sortPropertiesAlphabetically = src._sortPropertiesAlphabetically;

        _mixInAnnotations = mixins;
    }
    
    protected DeserializationConfig(DeserializationConfig src, BaseSettings base)
    {
        super(src, base, src._subtypeResolver, src._featureFlags);
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _sortPropertiesAlphabetically = src._sortPropertiesAlphabetically;
    }
    
    protected DeserializationConfig(DeserializationConfig src, JsonNodeFactory f)
    {
        super(src);
        _problemHandlers = src._problemHandlers;
        _nodeFactory = f;
        _sortPropertiesAlphabetically = src._sortPropertiesAlphabetically;
    }

    protected DeserializationConfig(DeserializationConfig src, int featureFlags)
    {
        super(src, featureFlags);
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
        _sortPropertiesAlphabetically = src._sortPropertiesAlphabetically;
    }
    
    /**
     * Helper method to be called right after creating a non-shared
     * instance, needed to pass state of feature(s) shared with
     * SerializationConfig.
     */
    protected DeserializationConfig passSerializationFeatures(int serializationFeatureFlags)
    {
        _sortPropertiesAlphabetically = (serializationFeatureFlags
                & SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY.getMask()) != 0;
        return this;
    }
    
    /*
    /**********************************************************
    /* Life-cycle, factory methods from MapperConfig
    /**********************************************************
     */

    @Override
    public DeserializationConfig withClassIntrospector(ClassIntrospector<? extends BeanDescription> ci) {
        return new DeserializationConfig(this, _base.withClassIntrospector(ci));
    }

    @Override
    public DeserializationConfig withAnnotationIntrospector(AnnotationIntrospector ai) {
        return new DeserializationConfig(this, _base.withAnnotationIntrospector(ai));
    }

    @Override
    public DeserializationConfig withVisibilityChecker(VisibilityChecker<?> vc) {
        return new DeserializationConfig(this, _base.withVisibilityChecker(vc));
    }

    @Override
    public DeserializationConfig withVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility) {
        return new DeserializationConfig(this, _base.withVisibility(forMethod, visibility));
    }
    
    @Override
    public DeserializationConfig withTypeResolverBuilder(TypeResolverBuilder<?> trb) {
        return new DeserializationConfig(this, _base.withTypeResolverBuilder(trb));
    }

    @Override
    public DeserializationConfig withSubtypeResolver(SubtypeResolver str)
    {
        DeserializationConfig cfg = new DeserializationConfig(this);
        cfg._subtypeResolver = str;
        return cfg;
    }
    
    @Override
    public DeserializationConfig withPropertyNamingStrategy(PropertyNamingStrategy pns) {
        return new DeserializationConfig(this, _base.withPropertyNamingStrategy(pns));
    }
    
    @Override
    public DeserializationConfig withTypeFactory(TypeFactory tf) {
        return (tf == _base.getTypeFactory()) ? this : new DeserializationConfig(this, _base.withTypeFactory(tf));
    }

    @Override
    public DeserializationConfig withDateFormat(DateFormat df) {
        return (df == _base.getDateFormat()) ? this : new DeserializationConfig(this, _base.withDateFormat(df));
    }
    
    @Override
    public DeserializationConfig withHandlerInstantiator(HandlerInstantiator hi) {
        return (hi == _base.getHandlerInstantiator()) ? this : new DeserializationConfig(this, _base.withHandlerInstantiator(hi));
    }

    @Override
    public DeserializationConfig withInsertedAnnotationIntrospector(AnnotationIntrospector ai) {
        return new DeserializationConfig(this, _base.withInsertedAnnotationIntrospector(ai));
    }

    @Override
    public DeserializationConfig withAppendedAnnotationIntrospector(AnnotationIntrospector ai) {
        return new DeserializationConfig(this, _base.withAppendedAnnotationIntrospector(ai));
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
        return new DeserializationConfig(this, f);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    @Override
    public DeserializationConfig with(DeserializationConfig.Feature... features)
    {
        int flags = _featureFlags;
        for (Feature f : features) {
            flags |= f.getMask();
        }
        return new DeserializationConfig(this, flags);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    @Override
    public DeserializationConfig without(DeserializationConfig.Feature... features)
    {
        int flags = _featureFlags;
        for (Feature f : features) {
            flags &= ~f.getMask();
        }
        return new DeserializationConfig(this, flags);
    }
    
    /*
    /**********************************************************
    /* MapperConfig implementation
    /**********************************************************
     */
    
    /**
     * Method that is called to create a non-shared copy of the configuration
     * to be used for a deserialization operation.
     * Note that if sub-classing
     * and sub-class has additional instance methods,
     * this method <b>must</b> be overridden to produce proper sub-class
     * instance.
     */
    @Override
    public DeserializationConfig createUnshared(SubtypeResolver subtypeResolver)
    {
        HashMap<ClassKey,Class<?>> mixins = _mixInAnnotations;
        // ensure that we assume sharing at this point:
        _mixInAnnotationsShared = true;
        return new DeserializationConfig(this, mixins, subtypeResolver);
    }


    @Override
    public DeserializationConfig createUnshared(SubtypeResolver subtypeResolver, int features) {
        HashMap<ClassKey,Class<?>> mixins = _mixInAnnotations;
        // ensure that we assume sharing at this point:
        _mixInAnnotationsShared = true;
        return new DeserializationConfig(this, mixins, subtypeResolver, features);
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
        if (isEnabled(Feature.USE_ANNOTATIONS)) {
            return super.getAnnotationIntrospector();
        }
        return NopAnnotationIntrospector.instance;
    }
    
    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
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
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspectDirectClassAnnotations(JavaType type) {
        return (T) getClassIntrospector().forDirectClassAnnotations(this, type, this);
    }
    
    @Override
    public boolean isAnnotationProcessingEnabled() {
        return isEnabled(Feature.USE_ANNOTATIONS);
    }

    @Override
    public boolean canOverrideAccessModifiers() {
        return isEnabled(Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);
    }

    @Override
    public boolean shouldSortPropertiesAlphabetically() {
        return _sortPropertiesAlphabetically;
    }

    @Override
    public VisibilityChecker<?> getDefaultVisibilityChecker()
    {
        VisibilityChecker<?> vchecker = super.getDefaultVisibilityChecker();
        if (!isEnabled(DeserializationConfig.Feature.AUTO_DETECT_SETTERS)) {
            vchecker = vchecker.withSetterVisibility(Visibility.NONE);
        }
        if (!isEnabled(DeserializationConfig.Feature.AUTO_DETECT_CREATORS)) {
            vchecker = vchecker.withCreatorVisibility(Visibility.NONE);
        }
        if (!isEnabled(DeserializationConfig.Feature.AUTO_DETECT_FIELDS)) {
            vchecker = vchecker.withFieldVisibility(Visibility.NONE);
        }
        return vchecker;
    }

    public boolean isEnabled(DeserializationConfig.Feature f) {
        return (_featureFlags & f.getMask()) != 0;
    }
    
    /*
    /**********************************************************
    /* Problem handlers
    /**********************************************************
     */

    /**
     * Method for getting head of the problem handler chain. May be null,
     * if no handlers have been added.
     */
    public LinkedNode<DeserializationProblemHandler> getProblemHandlers()
    {
        return _problemHandlers;
    }
    
    /**
     * Method that can be used to add a handler that can (try to)
     * resolve non-fatal deserialization problems.
     */
    public void addHandler(DeserializationProblemHandler h)
    {
        /* Sanity check: let's prevent adding same handler multiple
         * times
         */
        if (!LinkedNode.contains(_problemHandlers, h)) {
            _problemHandlers = new LinkedNode<DeserializationProblemHandler>(h, _problemHandlers);
        }
    }

    /**
     * Method for removing all configured problem handlers; usually done to replace
     * existing handler(s) with different one(s)
     */
    public void clearHandlers()
    {
        _problemHandlers = null;
    }

    /*
    /**********************************************************
    /* Other configuration
    /**********************************************************
     */

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
    
    /*
    /**********************************************************
    /* Extended API: handler instantiation
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public JsonDeserializer<Object> deserializerInstance(Annotated annotated,
            Class<?> deserClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            JsonDeserializer<?> deser = hi.deserializerInstance(this, annotated,
                    (Class<JsonDeserializer<?>>)deserClass);
            if (deser != null) {
                return (JsonDeserializer<Object>) deser;
            }
        }
        return (JsonDeserializer<Object>) ClassUtil.createInstance(deserClass, canOverrideAccessModifiers());
    }

    public KeyDeserializer keyDeserializerInstance(Annotated annotated,
            Class<?> keyDeserClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            @SuppressWarnings("unchecked")
            KeyDeserializer keyDeser = hi.keyDeserializerInstance(this, annotated,
                    (Class<KeyDeserializer>)keyDeserClass);
            if (keyDeser != null) {
                return (KeyDeserializer) keyDeser;
            }
        }
        return (KeyDeserializer) ClassUtil.createInstance(keyDeserClass, canOverrideAccessModifiers());
    }

    public ValueInstantiator valueInstantiatorInstance(Annotated annotated,
            Class<?> instClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            @SuppressWarnings("unchecked")
            ValueInstantiator inst = hi.valueInstantiatorInstance(this, annotated,
                    (Class<ValueInstantiator>)instClass);
            if (inst != null) {
                return (ValueInstantiator) inst;
            }
        }
        return (ValueInstantiator) ClassUtil.createInstance(instClass, canOverrideAccessModifiers());
    }
}
