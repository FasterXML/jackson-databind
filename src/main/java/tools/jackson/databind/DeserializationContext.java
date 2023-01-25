package tools.jackson.databind;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;

import tools.jackson.core.*;
import tools.jackson.core.tree.ArrayTreeNode;
import tools.jackson.core.tree.ObjectTreeNode;
import tools.jackson.core.type.ResolvedType;
import tools.jackson.core.type.TypeReference;

import tools.jackson.core.util.JacksonFeatureSet;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.ContextAttributes;
import tools.jackson.databind.cfg.DatatypeFeature;
import tools.jackson.databind.cfg.DatatypeFeatures;
import tools.jackson.databind.deser.*;
import tools.jackson.databind.deser.impl.ObjectIdReader;
import tools.jackson.databind.deser.impl.TypeWrappedDeserializer;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.exc.ValueInstantiationException;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.ClassIntrospector;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.TreeTraversingParser;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.util.*;

/**
 * Context for the process of deserialization a single root-level value.
 * Used to allow passing in configuration settings and reusable temporary
 * objects (scrap arrays, containers).
 * Constructed by {@link ObjectMapper} (and {@link ObjectReader} based on
 * configuration,
 * used mostly by {@link ValueDeserializer}s to access contextual information.
 */
public abstract class DeserializationContext
    extends DatabindContext
    implements ObjectReadContext // 3.0
{
    /*
    /**********************************************************************
    /* Per-mapper configuration (immutable via ObjectReader)
    /**********************************************************************
     */

    /**
     * Low-level {@link TokenStreamFactory} that may be used for constructing
     * embedded parsers.
     */
    final protected TokenStreamFactory _streamFactory;

    /**
     * Read-only factory instance; exposed to let
     * owners (<code>ObjectMapper</code>, <code>ObjectReader</code>)
     * access it.
     */
    final protected DeserializerFactory _factory;

    /**
     * Object that handle details of {@link ValueDeserializer} caching.
     */
    final protected DeserializerCache _cache;

    /*
    /**********************************************************************
    /* Configuration that may vary by ObjectReader
    /**********************************************************************
     */

    /**
     * Generic deserialization processing configuration
     */
    final protected DeserializationConfig _config;

    /**
     * Bitmap of {@link DeserializationFeature}s that are enabled
     */
    final protected int _featureFlags;

    /**
     * Currently active view, if any.
     */
    final protected Class<?> _activeView;

    /**
     * Schema for underlying parser to use, if any.
     */
    final protected FormatSchema _schema;

    /**
     * Object used for resolving references to injectable
     * values.
     */
    final protected InjectableValues _injectableValues;

    /*
    /**********************************************************************
    /* Other State
    /**********************************************************************
     */

    /**
     * Currently active parser used for deserialization.
     * May be different from the outermost parser
     * when content is buffered.
     */
    protected transient JsonParser _parser;

    /**
     * Capabilities of the input format.
     */
    protected transient JacksonFeatureSet<StreamReadCapability> _readCapabilities;

    /*
    /**********************************************************************
    /* Per-operation reusable helper objects (not for blueprints)
    /**********************************************************************
     */

    protected transient ArrayBuilders _arrayBuilders;

    protected transient ObjectBuffer _objectBuffer;

    protected transient DateFormat _dateFormat;

    /**
     * Lazily-constructed holder for per-call attributes.
     */
    protected transient ContextAttributes _attributes;

    /**
     * Type of {@link ValueDeserializer} on which {@link ValueDeserializer#createContextual}
     * is being called currently.
     */
    protected LinkedNode<JavaType> _currentType;

    /**
     * Lazily constructed {@link ClassIntrospector} instance: created from "blueprint"
     */
    protected transient ClassIntrospector _classIntrospector;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected DeserializationContext(TokenStreamFactory streamFactory,
            DeserializerFactory df, DeserializerCache cache,
            DeserializationConfig config, FormatSchema schema,
            InjectableValues injectableValues)
    {
        _streamFactory = streamFactory;
        _factory = Objects.requireNonNull(df, "Cannot pass null DeserializerFactory");
        _cache = cache;

        _config = config;
        _featureFlags = config.getDeserializationFeatures();
        _activeView = config.getActiveView();
        _schema = schema;

        _injectableValues = injectableValues;
        _attributes = config.getAttributes();
    }

    /*
    /**********************************************************************
    /* DatabindContext implementation
    /**********************************************************************
     */

    @Override
    public DeserializationConfig getConfig() { return _config; }

    @Override
    public final Class<?> getActiveView() { return _activeView; }

    @Override
    public final boolean canOverrideAccessModifiers() {
        return _config.canOverrideAccessModifiers();
    }

    @Override
    public final boolean isEnabled(MapperFeature feature) {
        return _config.isEnabled(feature);
    }

    @Override
    public final boolean isEnabled(DatatypeFeature feature) {
        return _config.isEnabled(feature);
    }

    @Override
    public final DatatypeFeatures getDatatypeFeatures() {
        return _config.getDatatypeFeatures();
    }

    @Override
    public final JsonFormat.Value getDefaultPropertyFormat(Class<?> baseType) {
        return _config.getDefaultPropertyFormat(baseType);
    }

    @Override
    public final AnnotationIntrospector getAnnotationIntrospector() {
        return _config.getAnnotationIntrospector();
    }

    @Override
    public final TypeFactory getTypeFactory() {
        return _config.getTypeFactory();
    }

    @Override // since 2.11
    public JavaType constructSpecializedType(JavaType baseType, Class<?> subclass)
        throws IllegalArgumentException
    {
        if (baseType.hasRawClass(subclass)) {
            return baseType;
        }
        // On deserialization side, still uses "strict" type-compatibility checking;
        // see [databind#2632] about serialization side
        return getConfig().getTypeFactory().constructSpecializedType(baseType, subclass, false);
    }

    /**
     * Method for accessing default Locale to use: convenience method for
     *<pre>
     *   getConfig().getLocale();
     *</pre>
     */
    @Override
    public Locale getLocale() {
        return _config.getLocale();
    }

    /**
     * Method for accessing default TimeZone to use: convenience method for
     *<pre>
     *   getConfig().getTimeZone();
     *</pre>
     */
    @Override
    public TimeZone getTimeZone() {
        return _config.getTimeZone();
    }

    /*
    /**********************************************************************
    /* Access to per-call state, like generic attributes
    /**********************************************************************
     */

    @Override
    public Object getAttribute(Object key) {
        return _attributes.getAttribute(key);
    }

    @Override
    public DeserializationContext setAttribute(Object key, Object value)
    {
        _attributes = _attributes.withPerCallAttribute(key, value);
        return this;
    }

    /**
     * Accessor to {@link JavaType} of currently contextualized
     * {@link ValueDeserializer}, if any.
     * This is sometimes useful for generic {@link ValueDeserializer}s that
     * do not get passed (or do not retain) type information when being
     * constructed: happens for example for deserializers constructed
     * from annotations.
     *
     * @return Type of {@link ValueDeserializer} being contextualized,
     *   if process is on-going; null if not.
     */
    public JavaType getContextualType() {
        return (_currentType == null) ? null : _currentType.value();
    }

    /*
    /**********************************************************************
    /* ObjectReadContext impl, config access
    /**********************************************************************
     */

    @Override
    public TokenStreamFactory tokenStreamFactory() {
        return _streamFactory;
    }

    @Override
    public FormatSchema getSchema() {
        return _schema;
    }

    @Override
    public StreamReadConstraints streamReadConstraints() {
        return _streamFactory.streamReadConstraints();
    }

    @Override
    public int getStreamReadFeatures(int defaults) {
        return _config.getStreamReadFeatures();
    }

    @Override
    public int getFormatReadFeatures(int defaults) {
        return _config.getFormatReadFeatures();
    }

    /*
    /**********************************************************************
    /* ObjectReadContext impl, Tree creation
    /**********************************************************************
     */

    @Override
    public ArrayTreeNode createArrayNode() {
        return getNodeFactory().arrayNode();
    }

    @Override
    public ObjectTreeNode createObjectNode() {
        return getNodeFactory().objectNode();
    }

    /*
    /**********************************************************************
    /* ObjectReadContext impl, databind
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public JsonNode readTree(JsonParser p) throws JacksonException
    {
        // NOTE: inlined version of `_bindAsTree()` from `ObjectReader`
        JsonToken t = p.currentToken();
        if (t == null) {
            t = p.nextToken();
            if (t == null) { // [databind#1406]: expose end-of-input as `null`
                return null;
            }
        }
        if (t == JsonToken.VALUE_NULL) {
            return getNodeFactory().nullNode();
        }
        ValueDeserializer<Object> deser = findRootValueDeserializer(ObjectReader.JSON_NODE_TYPE);
        return (JsonNode) deser.deserialize(p, this);
    }

    /**
     * Convenience method that may be used by composite or container deserializers,
     * for reading one-off values contained (for sequences, it is more efficient
     * to actually fetch deserializer once for the whole collection).
     *<p>
     * NOTE: when deserializing values of properties contained in composite types,
     * rather use {@link #readPropertyValue(JsonParser, BeanProperty, Class)};
     * this method does not allow use of contextual annotations.
     */
    @Override
    public <T> T readValue(JsonParser p, Class<T> type) throws JacksonException {
        return readValue(p, getTypeFactory().constructType(type));
    }

    @Override
    public <T> T readValue(JsonParser p, TypeReference<T> refType) throws JacksonException {
        return readValue(p, getTypeFactory().constructType(refType));
    }

    @Override
    public <T> T readValue(JsonParser p, ResolvedType type) throws JacksonException {
        if (!(type instanceof JavaType)) {
            throw new UnsupportedOperationException(
"Only support `JavaType` implementation of `ResolvedType`, not: "+type.getClass().getName());
        }
        return readValue(p, (JavaType) type);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, JavaType type) throws JacksonException {
        ValueDeserializer<Object> deser = findRootValueDeserializer(type);
        if (deser == null) {
            reportBadDefinition(type,
                    "Could not find `ValueDeserializer` for type "+ClassUtil.getTypeDescription(type));
        }
        return (T) deser.deserialize(p, this);
    }

    /*
    /**********************************************************************
    /* Public API, config feature accessors
    /**********************************************************************
     */

    /**
     * Convenience method for checking whether specified on/off
     * feature is enabled
     */
    public final boolean isEnabled(DeserializationFeature feat) {
        // 03-Dec-2010, tatu: minor shortcut; since this is called quite often,
        //   let's use a local copy of feature settings:
        return (_featureFlags & feat.getMask()) != 0;
    }

    /**
     * Bulk access method for getting the bit mask of all {@link DeserializationFeature}s
     * that are enabled.
     */
    public final int getDeserializationFeatures() {
        return _featureFlags;
    }

    /**
     * Bulk access method for checking that all features specified by
     * mask are enabled.
     */
    public final boolean hasDeserializationFeatures(int featureMask) {
        return (_featureFlags & featureMask) == featureMask;
    }

    /**
     * Bulk access method for checking that at least one of features specified by
     * mask is enabled.
     */
    public final boolean hasSomeOfFeatures(int featureMask) {
        return (_featureFlags & featureMask) != 0;
    }

    /**
     * Accessor for checking whether input format has specified capability
     * or not.
     *
     * @return True if input format has specified capability; false if not
     */
    public final boolean isEnabled(StreamReadCapability cap) {
        return _readCapabilities.isEnabled(cap);
    }

    /*
    /**********************************************************************
    /* Public API, accessor for helper objects
    /**********************************************************************
     */

    /**
     * Method for accessing the currently active parser.
     * May be different from the outermost parser
     * when content is buffered.
     *<p>
     * Use of this method is discouraged: if code has direct access
     * to the active parser, that should be used instead.
     */
    public final JsonParser getParser() { return _parser; }
    
    public final Object findInjectableValue(Object valueId,
            BeanProperty forProperty, Object beanInstance)
    {
        if (_injectableValues == null) {
            return reportBadDefinition(ClassUtil.classOf(valueId), String.format(
"No 'injectableValues' configured, cannot inject value with id [%s]", valueId));
        }
        return _injectableValues.findInjectableValue(valueId, this, forProperty, beanInstance);
    }

    /**
     * Convenience method for accessing the default Base64 encoding
     * used for decoding base64 encoded binary content.
     * Same as calling:
     *<pre>
     *  getConfig().getBase64Variant();
     *</pre>
     */
    public final Base64Variant getBase64Variant() {
        return _config.getBase64Variant();
    }

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *  getConfig().getNodeFactory();
     * </pre>
     */
    public final JsonNodeFactory getNodeFactory() {
        return _config.getNodeFactory();
    }

    /*
    /**********************************************************************
    /* Annotation, BeanDescription introspection
    /**********************************************************************
     */

    @Override
    protected ClassIntrospector classIntrospector() {
        if (_classIntrospector == null) {
            _classIntrospector = _config.classIntrospectorInstance();
        }
        return _classIntrospector;
    }

    @Override
    public BeanDescription introspectBeanDescription(JavaType type) {
        return classIntrospector().introspectForDeserialization(type);
    }

    public BeanDescription introspectBeanDescriptionForCreation(JavaType type) {
        return classIntrospector().introspectForCreation(type);
    }

    public BeanDescription introspectBeanDescriptionForBuilder(JavaType builderType,
            BeanDescription valueTypeDesc) {
        return classIntrospector().introspectForDeserializationWithBuilder(builderType,
                valueTypeDesc);
    }

    /*
    /**********************************************************************
    /* Misc config access
    /**********************************************************************
     */

    @Override
    public PropertyName findRootName(JavaType rootType) {
        return _config.findRootName(this, rootType);
    }

    @Override
    public PropertyName findRootName(Class<?> rawRootType) {
        return _config.findRootName(this, rawRootType);
    }

    /**
     * Method that can be used to see whether there is an explicitly registered deserializer
     * for given type: this is true for supported JDK types, as well as third-party types
     * for which {@code Module} provides support but is NOT true (that is, returns {@code false})
     * for POJO types for which {@code BeanDeserializer} is generated based on discovered
     * properties.
     *<p>
     * Note that it is up to {@code Module}s to implement support for this method: some
     * do (like basic {@code SimpleModule}).
     *
     * @param valueType Type-erased type to check
     *
     * @return True if this factory has explicit (non-POJO) deserializer for specified type,
     *    or has a provider (of type {@link Deserializers}) that has.
     */
    public boolean hasExplicitDeserializerFor(Class<?> valueType) {
        return _factory.hasExplicitDeserializerFor(this, valueType);
    }

    /*
    /**********************************************************************
    /* Public API, CoercionConfig access
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
     */
    public CoercionAction findCoercionAction(LogicalType targetType,
            Class<?> targetClass, CoercionInputShape inputShape)
    {
        return _config.findCoercionAction(targetType, targetClass, inputShape);
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
     */
    public CoercionAction findCoercionFromBlankString(LogicalType targetType,
            Class<?> targetClass,
            CoercionAction actionIfBlankNotAllowed)
    {
        return _config.findCoercionFromBlankString(targetType, targetClass, actionIfBlankNotAllowed);
    }

    /*
    /**********************************************************************
    /* Factory methods for getting appropriate TokenBuffer instances
    /* (possibly overridden by backends for alternate data formats)
    /**********************************************************************
     */

    /**
     * Factory method used for creating {@link TokenBuffer} to temporarily
     * contain copy of content read from specified parser; usually for purpose
     * of reading contents later on (possibly augmeneted with injected additional
     * content)
     */
    public TokenBuffer bufferForInputBuffering(JsonParser p) {
        return TokenBuffer.forBuffering(p, this);
    }

    /**
     * Convenience method that is equivalent to:
     *<pre>
     *   ctxt.bufferForInputBuffering(ctxt.getParser());
     *</pre>
     */
    public final TokenBuffer bufferForInputBuffering() {
        return bufferForInputBuffering(getParser());
    }

    /**
     * Convenience method, equivalent to:
     *<pre>
     * TokenBuffer buffer = ctxt.bufferForInputBuffering(parser);
     * buffer.copyCurrentStructure(parser);
     * return buffer;
     *</pre>
     *<p>
     * NOTE: the whole "current value" that parser points to is read and
     * buffered, including Object and Array values (if parser pointing to
     * start marker).
     */
    public TokenBuffer bufferAsCopyOfValue(JsonParser p) throws JacksonException
    {
        TokenBuffer buf = bufferForInputBuffering(p);
        buf.copyCurrentStructure(p);
        return buf;
    }

    /*
    /**********************************************************************
    /* Public API, value deserializer access
    /**********************************************************************
     */

    /**
     * Method for finding a value deserializer, and creating a contextual
     * version if necessary, for value reached via specified property.
     */
    @SuppressWarnings("unchecked")
    public final ValueDeserializer<Object> findContextualValueDeserializer(JavaType type,
            BeanProperty prop)
    {
        ValueDeserializer<Object> deser = _cache.findValueDeserializer(this, _factory, type);
        if (deser != null) {
            deser = (ValueDeserializer<Object>) handleSecondaryContextualization(deser, prop, type);
        }
        return deser;
    }

    /**
     * Variant that will try to locate deserializer for current type, but without
     * performing any contextualization (unlike {@link #findContextualValueDeserializer})
     * or checking for need to create a {@link TypeDeserializer} (unlike
     * {@link #findRootValueDeserializer(JavaType)}.
     * This method is usually called from within {@link ValueDeserializer#resolve},
     * and expectation is that caller then calls either
     * {@link #handlePrimaryContextualization(ValueDeserializer, BeanProperty, JavaType)} or
     * {@link #handleSecondaryContextualization(ValueDeserializer, BeanProperty, JavaType)} at a
     * later point, as necessary.
     */
    public final ValueDeserializer<Object> findNonContextualValueDeserializer(JavaType type)
    {
        return _cache.findValueDeserializer(this, _factory, type);
    }

    /**
     * Method for finding a deserializer for root-level value.
     */
    @SuppressWarnings("unchecked")
    public final ValueDeserializer<Object> findRootValueDeserializer(JavaType type)
    {
        ValueDeserializer<Object> deser = _cache.findValueDeserializer(this,
                _factory, type);
        if (deser == null) { // can this occur?
            return null;
        }
        deser = (ValueDeserializer<Object>) handleSecondaryContextualization(deser, null, type);
        TypeDeserializer typeDeser = findTypeDeserializer(type);
        if (typeDeser != null) {
            // important: contextualize to indicate this is for root value
            typeDeser = typeDeser.forProperty(null);
            return new TypeWrappedDeserializer(typeDeser, deser);
        }
        return deser;
    }

    /*
    /**********************************************************************
    /* Public API, (value) type deserializer access
    /**********************************************************************
     */

    /**
     * Method called to find and create a type information deserializer for given base type,
     * if one is needed. If not needed (no polymorphic handling configured for type),
     * should return null.
     *<p>
     * Note that this method is usually only directly called for values of container (Collection,
     * array, Map) types and root values, but not for bean property values.
     *
     * @param baseType Declared base type of the value to deserializer (actual
     *    deserializer type will be this type or its subtype)
     *
     * @return Type deserializer to use for given base type, if one is needed; null if not.
     */
    public TypeDeserializer findTypeDeserializer(JavaType baseType)
    {
        return findTypeDeserializer(baseType, introspectClassAnnotations(baseType));
    }

    public TypeDeserializer findTypeDeserializer(JavaType baseType,
            AnnotatedClass classAnnotations)
    {
        try {
            return _config.getTypeResolverProvider().findTypeDeserializer(this,
                    baseType, classAnnotations);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw InvalidDefinitionException.from(getParser(),
                    ClassUtil.exceptionMessage(e), baseType)
                .withCause(e);
        }
    }

    /**
     * Method called to create a type information deserializer for values of
     * given non-container property, if one is needed.
     * If not needed (no polymorphic handling configured for property), should return null.
     *<p>
     * Note that this method is only called for non-container bean properties,
     * and not for values in container types or root values (or container properties)
     *
     * @param baseType Declared base type of the value to deserializer (actual
     *    deserializer type will be this type or its subtype)
     *
     * @return Type deserializer to use for given base type, if one is needed; null if not.
     *
     * @since 3.0
     */
    public TypeDeserializer findPropertyTypeDeserializer(JavaType baseType,
            AnnotatedMember accessor)
    {
        try {
            return _config.getTypeResolverProvider().findPropertyTypeDeserializer(this,
                    accessor, baseType);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw InvalidDefinitionException.from(getParser(),
                    ClassUtil.exceptionMessage(e), baseType)
                .withCause(e);
        }
    }

    /**
     * Method called to find and create a type information deserializer for values of
     * given container (list, array, map) property, if one is needed.
     * If not needed (no polymorphic handling configured for property), should return null.
     *<p>
     * Note that this method is only called for container bean properties,
     * and not for values in container types or root values (or non-container properties)
     *
     * @param containerType Type of property; must be a container type
     * @param accessor Field or method that contains container property
     *
     * @since 3.0
     */
    public TypeDeserializer findPropertyContentTypeDeserializer(JavaType containerType,
            AnnotatedMember accessor)
    {
        try {
            return _config.getTypeResolverProvider().findPropertyContentTypeDeserializer(this,
                    accessor, containerType);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw InvalidDefinitionException.from(getParser(),
                    ClassUtil.exceptionMessage(e), containerType)
                .withCause(e);
        }
    }

    /*
    /**********************************************************************
    /* Public API, key deserializer access
    /**********************************************************************
     */

    public final KeyDeserializer findKeyDeserializer(JavaType keyType,
            BeanProperty prop)
    {
        KeyDeserializer kd;
        // 15-Jun-2021, tatu: Needed wrt [databind#3143]
        try {
            kd = _cache.findKeyDeserializer(this, _factory, keyType);
        } catch (IllegalArgumentException iae) {
            // We better only expose checked exceptions, since those
            // are what caller is expected to handle
            reportBadDefinition(keyType, ClassUtil.exceptionMessage(iae));
            kd = null;
        }
        // Second: contextualize?
        if (kd instanceof ContextualKeyDeserializer) {
            kd = ((ContextualKeyDeserializer) kd).createContextual(this, prop);
        }
        return kd;
    }

    /*
    /**********************************************************************
    /* Public API, ObjectId handling
    /**********************************************************************
     */

    /**
     * Method called to find and return entry corresponding to given
     * Object Id: will add an entry if necessary, and never returns null
     */
    public abstract ReadableObjectId findObjectId(Object id, ObjectIdGenerator<?> generator, ObjectIdResolver resolver);

    /**
     * Method called to ensure that every object id encounter during processing
     * are resolved.
     *
     * @throws UnresolvedForwardReference
     */
    public abstract void checkUnresolvedObjectId()
        throws UnresolvedForwardReference;

    /*
    /**********************************************************************
    /* Public API, type handling
    /**********************************************************************
     */

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *  getConfig().constructType(cls);
     * </pre>
     */
    public final JavaType constructType(Class<?> cls) {
        return (cls == null) ? null : _config.constructType(cls);
    }

    /**
     * Helper method that is to be used when resolving basic class name into
     * Class instance, the reason being that it may be necessary to work around
     * various ClassLoader limitations, as well as to handle primitive type
     * signatures.
     */
    public Class<?> findClass(String className) throws ClassNotFoundException
    {
        // By default, delegate to ClassUtil: can be overridden with custom handling
        return getTypeFactory().findClass(className);
    }

    /*
    /**********************************************************************
    /* Public API, helper object recycling
    /**********************************************************************
     */

    /**
     * Method that can be used to get access to a reusable ObjectBuffer,
     * useful for efficiently constructing Object arrays and Lists.
     * Note that leased buffers should be returned once deserializer
     * is done, to allow for reuse during same round of deserialization.
     */
    public final ObjectBuffer leaseObjectBuffer()
    {
        ObjectBuffer buf = _objectBuffer;
        if (buf == null) {
            buf = new ObjectBuffer();
        } else {
            _objectBuffer = null;
        }
        return buf;
    }

    /**
     * Method to call to return object buffer previously leased with
     * {@link #leaseObjectBuffer}.
     *
     * @param buf Returned object buffer
     */
    public final void returnObjectBuffer(ObjectBuffer buf)
    {
        // Already have a reusable buffer? Let's retain bigger one
        // (or if equal, favor newer one, shorter life-cycle)
        if (_objectBuffer == null
            || buf.initialCapacity() >= _objectBuffer.initialCapacity()) {
            _objectBuffer = buf;
        }
    }

    /**
     * Method for accessing object useful for building arrays of
     * primitive types (such as int[]).
     */
    public final ArrayBuilders getArrayBuilders()
    {
        if (_arrayBuilders == null) {
            _arrayBuilders = new ArrayBuilders();
        }
        return _arrayBuilders;
    }

    /*
    /**********************************************************************
    /* Extended API: handler instantiation
    /**********************************************************************
     */

    public abstract ValueDeserializer<Object> deserializerInstance(Annotated annotated,
            Object deserDef);

    public abstract KeyDeserializer keyDeserializerInstance(Annotated annotated,
            Object deserDef);

    /*
    /**********************************************************************
    /* Extended API: resolving contextual deserializers; called
    /* by structured deserializers for their value/component deserializers
    /**********************************************************************
     */

    /**
     * Method called for primary property deserializers (ones
     * directly created to deserialize values of a POJO property),
     * to handle details of calling
     * {@link ValueDeserializer#createContextual} with given property context.
     *
     * @param prop Property for which the given primary deserializer is used; never null.
     */
    public ValueDeserializer<?> handlePrimaryContextualization(ValueDeserializer<?> deser,
            BeanProperty prop, JavaType type)
    {
        if (deser != null) {
            _currentType = new LinkedNode<JavaType>(type, _currentType);
            try {
                deser = deser.createContextual(this, prop);
            } finally {
                _currentType = _currentType.next();
            }
        }
        return deser;
    }

    /**
     * Method called for secondary property deserializers (ones
     * NOT directly created to deal with an annotatable POJO property,
     * but instead created as a component -- such as value deserializers
     * for structured types, or deserializers for root values)
     * to handle details of resolving
     * {@link ValueDeserializer#createContextual} with given property context.
     * Given that these deserializers are not directly related to given property
     * (or, in case of root value property, to any property), annotations
     * accessible may or may not be relevant.
     *
     * @param prop Property for which deserializer is used, if any; null
     *    when deserializing root values
     */
    public ValueDeserializer<?> handleSecondaryContextualization(ValueDeserializer<?> deser,
            BeanProperty prop, JavaType type)
    {
        if (deser != null) {
            _currentType = new LinkedNode<JavaType>(type, _currentType);
            try {
                deser =deser.createContextual(this, prop);
            } finally {
                _currentType = _currentType.next();
            }
        }
        return deser;
    }

    /*
    /**********************************************************************
    /* Parsing methods that may use reusable/-cyclable objects
    /**********************************************************************
     */

    /**
     * Convenience method for parsing a Date from given String, using
     * currently configured date format (accessed using
     * {@link DeserializationConfig#getDateFormat()}).
     *<p>
     * Implementation will handle thread-safety issues related to
     * date formats such that first time this method is called,
     * date format is cloned, and cloned instance will be retained
     * for use during this deserialization round.
     */
    public Date parseDate(String dateStr) throws IllegalArgumentException
    {
        try {
            DateFormat df = _getDateFormat();
            return df.parse(dateStr);
        } catch (ParseException e) {
            throw new IllegalArgumentException(String.format(
                    "Failed to parse Date value '%s': %s", dateStr,
                    ClassUtil.exceptionMessage(e)));
        }
    }

    /**
     * Convenience method for constructing Calendar instance set
     * to specified time, to be modified and used by caller.
     */
    public Calendar constructCalendar(Date d) {
        // 08-Jan-2008, tatu: not optimal, but should work for the most part; let's revise as needed.
        Calendar c = Calendar.getInstance(getTimeZone());
        c.setTime(d);
        return c;
    }

    /*
    /**********************************************************************
    /* Extension points for more esoteric data coercion
    /**********************************************************************
     */

    /**
     * Method to call in case incoming shape is Object Value (and parser thereby
     * points to {@link tools.jackson.core.JsonToken#START_OBJECT} token),
     * but a Scalar value (potentially coercible from String value) is expected.
     * This would typically be used to deserializer a Number, Boolean value or some other
     * "simple" unstructured value type.
     * 
     * @param p Actual parser to read content from
     * @param deser Deserializer that needs extracted String value
     * @param scalarType Immediate type of scalar to extract; usually type deserializer
     *    handles but not always (for example, deserializer for {@code int[]} would pass
     *    scalar type of {@code int})
     *
     * @return String value found; not {@code null} (exception should be thrown if no suitable
     *     value found)
     *
     * @throws JacksonException If there are problems either reading content (underlying parser
     *    problem) or finding expected scalar value
     */
    public String extractScalarFromObject(JsonParser p, ValueDeserializer<?> deser,
            Class<?> scalarType)
        throws JacksonException
    {
        return (String) handleUnexpectedToken(constructType(scalarType), p);
    }

    /*
    /**********************************************************************
    /* Convenience methods for reading parsed values
    /**********************************************************************
     */

    /**
     * Convenience method that may be used by composite or container deserializers,
     * for reading one-off values for the composite type, taking into account
     * annotations that the property (passed to this method -- usually property that
     * has custom serializer that called this method) has.
     *
     * @param p Parser that points to the first token of the value to read
     * @param prop Logical property of a POJO being type
     * @return Value of type {@code type} that was read
     */
    public <T> T readPropertyValue(JsonParser p, BeanProperty prop, Class<T> type)
        throws JacksonException
    {
        return readPropertyValue(p, prop, getTypeFactory().constructType(type));
    }

    /**
     * Same as {@link #readPropertyValue(JsonParser, BeanProperty, Class)} but with
     * fully resolved {@link JavaType} as target: needs to be used for generic types,
     * for example.
     */
    @SuppressWarnings("unchecked")
    public <T> T readPropertyValue(JsonParser p, BeanProperty prop, JavaType type)
        throws JacksonException
    {
        ValueDeserializer<Object> deser = findContextualValueDeserializer(type, prop);
        if (deser == null) {
            return reportBadDefinition(type, String.format(
                    "Could not find `ValueDeserializer` for type %s (via property %s)",
                    ClassUtil.getTypeDescription(type), ClassUtil.nameOf(prop)));
        }
        return (T) deser.deserialize(p, this);
    }

    /**
     * Helper method similar to {@link ObjectReader#treeToValue(TreeNode, Class)}
     * which will read contents of given tree ({@link JsonNode})
     * and bind them into specified target type. This is often used in two-phase
     * deserialization in which content is first read as a tree, then manipulated
     * (adding and/or removing properties of Object values, for example),
     * and finally converted into actual target type using default deserialization
     * logic for the type.
     *<p>
     * NOTE: deserializer implementations should be careful not to try to recursively
     * deserialize into target type deserializer has registered itself to handle.
     *
     * @param n Tree value to convert, if not {@code null}: if {@code null}, will simply
     *     return {@code null}
     * @param targetType Type to deserialize contents of {@code n} into (if {@code n} not {@code null})
     *
     * @return Either {@code null} (if {@code n} was {@code null} or a value of
     *     type {@code type} that was read from non-{@code null} {@code n} argument
     */
    public <T> T readTreeAsValue(JsonNode n, Class<T> targetType)
        throws JacksonException
    {
        if (n == null) {
            return null;
        }
        try (TreeTraversingParser p = _treeAsTokens(n)) {
            return readValue(p, targetType);
        }
    }

    /**
     * Same as {@link #readTreeAsValue(JsonNode, Class)} but will fully resolved
     * {@link JavaType} as {@code targetType}
     *<p>
     * NOTE: deserializer implementations should be careful not to try to recursively
     * deserialize into target type deserializer has registered itself to handle.
     *
     * @param n Tree value to convert
     * @param targetType Type to deserialize contents of {@code n} into
     *
     * @return Value of type {@code type} that was read
     */
    public <T> T readTreeAsValue(JsonNode n, JavaType targetType)
        throws JacksonException
    {
        if (n == null) {
            return null;
        }
        try (TreeTraversingParser p = _treeAsTokens(n)) {
            return readValue(p, targetType);
        }
    }

    private TreeTraversingParser _treeAsTokens(JsonNode n)
    {
        TreeTraversingParser p = new TreeTraversingParser(n, this);
        // important: must initialize...
        p.nextToken();
        return p;
    }

    /*
    /**********************************************************************
    /* Methods for problem handling
    /**********************************************************************
     */

    /**
     * Method that deserializers should call if they encounter an unrecognized
     * property (and once that is not explicitly designed as ignorable), to
     * inform possibly configured {@link DeserializationProblemHandler}s and
     * let it handle the problem.
     *
     * @return True if there was a configured problem handler that was able to handle the
     *   problem
     */
    public boolean handleUnknownProperty(JsonParser p, ValueDeserializer<?> deser,
            Object instanceOrClass, String propName)
        throws JacksonException
    {
        LinkedNode<DeserializationProblemHandler> h = _config.getProblemHandlers();
        while (h != null) {
            // Can bail out if it's handled
            if (h.value().handleUnknownProperty(this, p, deser, instanceOrClass, propName)) {
                return true;
            }
            h = h.next();
        }
        // Nope, not handled. Potentially that's a problem...
        if (!isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
            p.skipChildren();
            return true;
        }
        // Do we know properties that are expected instead?
        Collection<Object> propIds = (deser == null) ? null : deser.getKnownPropertyNames();
        throw UnrecognizedPropertyException.from(_parser,
                instanceOrClass, propName, propIds);
    }

    /**
     * Method that deserializers should call if they encounter a String value
     * that cannot be converted to expected key of a {@link java.util.Map}
     * valued property.
     * Default implementation will try to call {@link DeserializationProblemHandler#handleWeirdNumberValue}
     * on configured handlers, if any, to allow for recovery; if recovery does not
     * succeed, will throw {@link InvalidFormatException} with given message.
     *
     * @param keyClass Expected type for key
     * @param keyValue String value from which to deserialize key
     * @param msg Error message template caller wants to use if exception is to be thrown
     * @param msgArgs Optional arguments to use for message, if any
     *
     * @return Key value to use
     *
     * @throws JacksonException To indicate unrecoverable problem, usually based on <code>msg</code>
     */
    public Object handleWeirdKey(Class<?> keyClass, String keyValue,
            String msg, Object... msgArgs)
        throws JacksonException
    {
        // but if not handled, just throw exception
        msg = _format(msg, msgArgs);
        LinkedNode<DeserializationProblemHandler> h = _config.getProblemHandlers();
        while (h != null) {
            // Can bail out if it's handled
            Object key = h.value().handleWeirdKey(this, keyClass, keyValue, msg);
            if (key != DeserializationProblemHandler.NOT_HANDLED) {
                // Sanity check for broken handlers, otherwise nasty to debug:
                if ((key == null) || keyClass.isInstance(key)) {
                    return key;
                }
                throw weirdStringException(keyValue, keyClass, String.format(
                        "DeserializationProblemHandler.handleWeirdStringValue() for type %s returned value of type %s",
                        ClassUtil.getClassDescription(keyClass),
                        ClassUtil.getClassDescription(key)
                ));
            }
            h = h.next();
        }
        throw weirdKeyException(keyClass, keyValue, msg);
    }

    /**
     * Method that deserializers should call if they encounter a String value
     * that cannot be converted to target property type, in cases where some
     * String values could be acceptable (either with different settings,
     * or different value).
     * Default implementation will try to call {@link DeserializationProblemHandler#handleWeirdStringValue}
     * on configured handlers, if any, to allow for recovery; if recovery does not
     * succeed, will throw {@link InvalidFormatException} with given message.
     *
     * @param targetClass Type of property into which incoming String should be converted
     * @param value String value from which to deserialize property value
     * @param msg Error message template caller wants to use if exception is to be thrown
     * @param msgArgs Optional arguments to use for message, if any
     *
     * @return Property value to use
     *
     * @throws JacksonException To indicate unrecoverable problem, usually based on <code>msg</code>
     */
    public Object handleWeirdStringValue(Class<?> targetClass, String value,
            String msg, Object... msgArgs)
        throws JacksonException
    {
        // but if not handled, just throw exception
        msg = _format(msg, msgArgs);
        LinkedNode<DeserializationProblemHandler> h = _config.getProblemHandlers();
        while (h != null) {
            // Can bail out if it's handled
            Object instance = h.value().handleWeirdStringValue(this, targetClass, value, msg);
            if (instance != DeserializationProblemHandler.NOT_HANDLED) {
                // Sanity check for broken handlers, otherwise nasty to debug:
                if (_isCompatible(targetClass, instance)) {
                    return instance;
                }
                throw weirdStringException(value, targetClass, String.format(
                        "DeserializationProblemHandler.handleWeirdStringValue() for type %s returned value of type %s",
                        ClassUtil.getClassDescription(targetClass),
                        ClassUtil.getClassDescription(instance)
                ));
            }
            h = h.next();
        }
        throw weirdStringException(value, targetClass, msg);
    }

    /**
     * Method that deserializers should call if they encounter a numeric value
     * that cannot be converted to target property type, in cases where some
     * numeric values could be acceptable (either with different settings,
     * or different numeric value).
     * Default implementation will try to call {@link DeserializationProblemHandler#handleWeirdNumberValue}
     * on configured handlers, if any, to allow for recovery; if recovery does not
     * succeed, will throw {@link InvalidFormatException} with given message.
     *
     * @param targetClass Type of property into which incoming number should be converted
     * @param value Number value from which to deserialize property value
     * @param msg Error message template caller wants to use if exception is to be thrown
     * @param msgArgs Optional arguments to use for message, if any
     *
     * @return Property value to use
     *
     * @throws JacksonException To indicate unrecoverable problem, usually based on <code>msg</code>
     */
    public Object handleWeirdNumberValue(Class<?> targetClass, Number value,
            String msg, Object... msgArgs)
        throws JacksonException
    {
        msg = _format(msg, msgArgs);
        LinkedNode<DeserializationProblemHandler> h = _config.getProblemHandlers();
        while (h != null) {
            // Can bail out if it's handled
            Object key = h.value().handleWeirdNumberValue(this, targetClass, value, msg);
            if (key != DeserializationProblemHandler.NOT_HANDLED) {
                // Sanity check for broken handlers, otherwise nasty to debug:
                if (_isCompatible(targetClass, key)) {
                    return key;
                }
                throw weirdNumberException(value, targetClass, _format(
                        "DeserializationProblemHandler.handleWeirdNumberValue() for type %s returned value of type %s",
                        ClassUtil.getClassDescription(targetClass),
                        ClassUtil.getClassDescription(key)
                ));
            }
            h = h.next();
        }
        throw weirdNumberException(value, targetClass, msg);
    }

    public Object handleWeirdNativeValue(JavaType targetType, Object badValue,
            JsonParser p)
        throws JacksonException
    {
        LinkedNode<DeserializationProblemHandler> h = _config.getProblemHandlers();
        final Class<?> raw = targetType.getRawClass();
        for (; h != null; h = h.next()) {
            // Can bail out if it's handled
            Object goodValue = h.value().handleWeirdNativeValue(this, targetType, badValue, p);
            if (goodValue != DeserializationProblemHandler.NOT_HANDLED) {
                // Sanity check for broken handlers, otherwise nasty to debug:
                if ((goodValue == null) || raw.isInstance(goodValue)) {
                    return goodValue;
                }
                throw DatabindException.from(p, _format(
"DeserializationProblemHandler.handleWeirdNativeValue() for type %s returned value of type %s",
                    ClassUtil.getClassDescription(targetType),
                    ClassUtil.getClassDescription(goodValue)
                ));
            }
        }
        throw weirdNativeValueException(badValue, raw);
    }

    /**
     * Method that deserializers should call if they fail to instantiate value
     * due to lack of viable instantiator (usually creator, that is, constructor
     * or static factory method). Method should be called at point where value
     * has not been decoded, so that handler has a chance to handle decoding
     * using alternate mechanism, and handle underlying content (possibly by
     * just skipping it) to keep input state valid
     *
     * @param instClass Type that was to be instantiated
     * @param valueInst (optional) Value instantiator to be used, if any; null if type does not
     *    use one for instantiation (custom deserialiers don't; standard POJO deserializer does)
     * @param p Parser that points to the JSON value to decode
     *
     * @return Object that should be constructed, if any; has to be of type <code>instClass</code>
     */
    @SuppressWarnings("resource")
    public Object handleMissingInstantiator(Class<?> instClass, ValueInstantiator valueInst,
            JsonParser p, String msg, Object... msgArgs)
        throws JacksonException
    {
        if (p == null) {
            p = getParser();
        }
        msg = _format(msg, msgArgs);
        LinkedNode<DeserializationProblemHandler> h = _config.getProblemHandlers();
        while (h != null) {
            // Can bail out if it's handled
            Object instance = h.value().handleMissingInstantiator(this,
                    instClass, valueInst, p, msg);
            if (instance != DeserializationProblemHandler.NOT_HANDLED) {
                // Sanity check for broken handlers, otherwise nasty to debug:
                if (_isCompatible(instClass, instance)) {
                    return instance;
                }
                reportBadDefinition(constructType(instClass), String.format(
"DeserializationProblemHandler.handleMissingInstantiator() for type %s returned value of type %s",
                    ClassUtil.getClassDescription(instClass),
                    ClassUtil.getClassDescription((instance)
                )));
            }
            h = h.next();
        }

        // 16-Oct-2016, tatu: This is either a definition problem (if no applicable creator
        //   exists), or input mismatch problem (otherwise) since none of existing creators
        //   match with token.
        // 24-Oct-2019, tatu: Further, as per [databind#2522], passing `null` ValueInstantiator
        //   should simply trigger definition problem
        if (valueInst == null ) {
            msg = String.format("Cannot construct instance of %s: %s",
                    ClassUtil.nameOf(instClass), msg);
            return reportBadDefinition(instClass, msg);
        }
        if (!valueInst.canInstantiate()) {
            msg = String.format("Cannot construct instance of %s (no Creators, like default constructor, exist): %s",
                    ClassUtil.nameOf(instClass), msg);
            return reportBadDefinition(instClass, msg);
        }
        msg = String.format("Cannot construct instance of %s (although at least one Creator exists): %s",
                ClassUtil.nameOf(instClass), msg);
        return reportInputMismatch(instClass, msg);
    }

    /**
     * Method that deserializers should call if they fail to instantiate value
     * due to an exception that was thrown by constructor (or other mechanism used
     * to create instances).
     * Default implementation will try to call {@link DeserializationProblemHandler#handleInstantiationProblem}
     * on configured handlers, if any, to allow for recovery; if recovery does not
     * succeed, will throw exception constructed with {@link #instantiationException}.
     *
     * @param instClass Type that was to be instantiated
     * @param argument (optional) Argument that was passed to constructor or equivalent
     *    instantiator; often a {@link java.lang.String}.
     * @param t Exception that caused failure
     *
     * @return Object that should be constructed, if any; has to be of type <code>instClass</code>
     */
    public Object handleInstantiationProblem(Class<?> instClass, Object argument,
            Throwable t)
        throws JacksonException
    {
        LinkedNode<DeserializationProblemHandler> h = _config.getProblemHandlers();
        while (h != null) {
            // Can bail out if it's handled
            Object instance = h.value().handleInstantiationProblem(this, instClass, argument, t);
            if (instance != DeserializationProblemHandler.NOT_HANDLED) {
                // Sanity check for broken handlers, otherwise nasty to debug:
                if (_isCompatible(instClass, instance)) {
                    return instance;
                }
                reportBadDefinition(constructType(instClass), String.format(
"DeserializationProblemHandler.handleInstantiationProblem() for type %s returned value of type %s",
                    ClassUtil.getClassDescription(instClass),
                    ClassUtil.classNameOf(instance)
                ));
            }
            h = h.next();
        }
        // 18-May-2016, tatu: Only wrap if not already a valid type to throw
        ClassUtil.throwIfJacksonE(t);
        // [databind#2164]: but see if wrapping is desired
        if (!isEnabled(DeserializationFeature.WRAP_EXCEPTIONS)) {
            ClassUtil.throwIfRTE(t);
        }
        throw instantiationException(instClass, t);
    }

// 15-Sep-2019, tatu: Remove from 3.0 due to [databind#2133] adding `JavaType` overloads
/*
    public Object handleUnexpectedToken(Class<?> instClass, JsonToken t,
            JsonParser p, String msg, Object... msgArgs)
        throws JacksonException
    {
        return handleUnexpectedToken(constructType(instClass), t, p, msg, msgArgs);
    }
*/

    public Object handleUnexpectedToken(Class<?> instClass, JsonParser p)
        throws JacksonException
    {
        return handleUnexpectedToken(constructType(instClass), p.currentToken(), p, null);
    }

    /**
     * Method that deserializers should call if the first token of the value to
     * deserialize is of unexpected type (that is, type of token that deserializer
     * cannot handle). This could occur, for example, if a Number deserializer
     * encounter {@link JsonToken#START_ARRAY} instead of
     * {@link JsonToken#VALUE_NUMBER_INT} or {@link JsonToken#VALUE_NUMBER_FLOAT}.
     *
     * @param targetType Type that was to be instantiated
     * @param p Parser that points to the JSON value to decode
     *
     * @return Object that should be constructed, if any; has to be of type <code>instClass</code>
     */
    public Object handleUnexpectedToken(JavaType targetType, JsonParser p)
        throws JacksonException
    {
        return handleUnexpectedToken(targetType, p.currentToken(), p, null);
    }

    /**
     * Method that deserializers should call if the first token of the value to
     * deserialize is of unexpected type (that is, type of token that deserializer
     * cannot handle). This could occur, for example, if a Number deserializer
     * encounter {@link JsonToken#START_ARRAY} instead of
     * {@link JsonToken#VALUE_NUMBER_INT} or {@link JsonToken#VALUE_NUMBER_FLOAT}.
     *
     * @param targetType Type that was to be instantiated
     * @param t Token encountered that does not match expected
     * @param p Parser that points to the JSON value to decode
     *
     * @return Object that should be constructed, if any; has to be of type <code>instClass</code>
     */
    public Object handleUnexpectedToken(JavaType targetType, JsonToken t,
            JsonParser p, String msg, Object... msgArgs)
        throws JacksonException
    {
        msg = _format(msg, msgArgs);
        LinkedNode<DeserializationProblemHandler> h = _config.getProblemHandlers();
        while (h != null) {
            Object instance = h.value().handleUnexpectedToken(this,
                    targetType, t, p, msg);
            if (instance != DeserializationProblemHandler.NOT_HANDLED) {
                if (_isCompatible(targetType.getRawClass(), instance)) {
                    return instance;
                }
                reportBadDefinition(targetType, String.format(
                        "DeserializationProblemHandler.handleUnexpectedToken() for type %s returned value of type %s",
                        ClassUtil.getTypeDescription(targetType),
                        ClassUtil.classNameOf(instance)
                ));
            }
            h = h.next();
        }
        if (msg == null) {
            final String targetDesc = ClassUtil.getTypeDescription(targetType);
            if (t == null) {
                msg = String.format("Unexpected end-of-input when trying read value of type %s",
                        targetDesc);
            } else {
                msg = String.format("Cannot deserialize value of type %s from %s (token `JsonToken.%s`)",
                        targetDesc, _shapeForToken(t), t);
            }
        }
        // 18-Jun-2020, tatu: to resolve [databind#2770], force access to `getText()` for scalars
        if ((t != null) && t.isScalarValue()) {
            p.getText();
        }
        reportInputMismatch(targetType, msg);
        return null; // never gets here
    }

    /**
     * Method that deserializers should call if they encounter a type id
     * (for polymorphic deserialization) that cannot be resolved to an
     * actual type; usually since there is no mapping defined.
     * Default implementation will try to call {@link DeserializationProblemHandler#handleUnknownTypeId}
     * on configured handlers, if any, to allow for recovery; if recovery does not
     * succeed, will throw exception constructed with {@link #invalidTypeIdException}.
     *
     * @param baseType Base type from which resolution starts
     * @param id Type id that could not be converted
     * @param extraDesc Additional problem description to add to default exception message,
     *    if resolution fails.
     *
     * @return {@link JavaType} that id resolves to
     *
     * @throws JacksonException To indicate unrecoverable problem, if resolution cannot
     *    be made to work
     */
    public JavaType handleUnknownTypeId(JavaType baseType, String id,
            TypeIdResolver idResolver, String extraDesc) throws JacksonException
    {
        LinkedNode<DeserializationProblemHandler> h = _config.getProblemHandlers();
        while (h != null) {
            // Can bail out if it's handled
            JavaType type = h.value().handleUnknownTypeId(this, baseType, id, idResolver, extraDesc);
            if (type != null) {
                if (type.hasRawClass(Void.class)) {
                    return null;
                }
                // But ensure there's type compatibility
                if (type.isTypeOrSubTypeOf(baseType.getRawClass())) {
                    return type;
                }
                throw invalidTypeIdException(baseType, id,
                        "problem handler tried to resolve into non-subtype: "+
                                ClassUtil.getTypeDescription(type));
            }
            h = h.next();
        }
        // 24-May-2016, tatu: Actually we may still not want to fail quite yet
        if (!isEnabled(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)) {
            return null;
        }
        throw invalidTypeIdException(baseType, id, extraDesc);
    }

    public JavaType handleMissingTypeId(JavaType baseType,
            TypeIdResolver idResolver, String extraDesc) throws JacksonException
    {
        LinkedNode<DeserializationProblemHandler> h = _config.getProblemHandlers();
        while (h != null) {
            // Can bail out if it's handled
            JavaType type = h.value().handleMissingTypeId(this, baseType, idResolver, extraDesc);
            if (type != null) {
                if (type.hasRawClass(Void.class)) {
                    return null;
                }
                // But ensure there's type compatibility
                if (type.isTypeOrSubTypeOf(baseType.getRawClass())) {
                    return type;
                }
                throw invalidTypeIdException(baseType, null,
                        "problem handler tried to resolve into non-subtype: "+
                                ClassUtil.getTypeDescription(type));
            }
            h = h.next();
        }
        // 09-Mar-2017, tatu: We may want to consider yet another feature at some
        //    point to allow returning `null`... but that seems bit risky for now
//        if (!isEnabled(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)) {
//            return null;
//        }
        throw missingTypeIdException(baseType, extraDesc);
    }

    /**
     * Method that deserializer may call if it is called to do an update ("merge")
     * but deserializer operates on a non-mergeable type. Although this should
     * usually be caught earlier, sometimes it may only be caught during operation
     * and if so this is the method to call.
     * Note that if {@link MapperFeature#IGNORE_MERGE_FOR_UNMERGEABLE} is enabled,
     * this method will simply return null; otherwise {@link InvalidDefinitionException}
     * will be thrown.
     */
    public void handleBadMerge(ValueDeserializer<?> deser) throws DatabindException
    {
        if (!isEnabled(MapperFeature.IGNORE_MERGE_FOR_UNMERGEABLE)) {
            JavaType type = constructType(deser.handledType());
            String msg = String.format("Invalid configuration: values of type %s cannot be merged",
                    ClassUtil.getTypeDescription(type));
            throw InvalidDefinitionException.from(getParser(), msg, type);
        }
    }

    protected boolean _isCompatible(Class<?> target, Object value)
    {
        if ((value == null) || target.isInstance(value)) {
            return true;
        }
        // [databind#1767]: Make sure to allow wrappers for primitive fields
        return target.isPrimitive()
                && ClassUtil.wrapperType(target).isInstance(value);
    }

    /*
    /**********************************************************************
    /* Methods for problem reporting, in cases where recovery
    /* is not considered possible: input problem
    /**********************************************************************
     */

    /**
     * Method for deserializers to call
     * when the token encountered was of type different than what <b>should</b>
     * be seen at that position, usually within a sequence of expected tokens.
     * Note that this method will throw a {@link DatabindException} and no
     * recovery is attempted (via {@link DeserializationProblemHandler}, as
     * problem is considered to be difficult to recover from, in general.
     */
    public void reportWrongTokenException(ValueDeserializer<?> deser,
            JsonToken expToken, String msg, Object... msgArgs)
        throws DatabindException
    {
        msg = _format(msg, msgArgs);
        throw wrongTokenException(getParser(), deser.handledType(), expToken, msg);
    }

    /**
     * Method for deserializers to call
     * when the token encountered was of type different than what <b>should</b>
     * be seen at that position, usually within a sequence of expected tokens.
     * Note that this method will throw a {@link DatabindException} and no
     * recovery is attempted (via {@link DeserializationProblemHandler}, as
     * problem is considered to be difficult to recover from, in general.
     */
    public void reportWrongTokenException(JavaType targetType,
            JsonToken expToken, String msg, Object... msgArgs)
        throws DatabindException
    {
        msg = _format(msg, msgArgs);
        throw wrongTokenException(getParser(), targetType, expToken, msg);
    }

    /**
     * Method for deserializers to call
     * when the token encountered was of type different than what <b>should</b>
     * be seen at that position, usually within a sequence of expected tokens.
     * Note that this method will throw a {@link DatabindException} and no
     * recovery is attempted (via {@link DeserializationProblemHandler}, as
     * problem is considered to be difficult to recover from, in general.
     */
    public void reportWrongTokenException(Class<?> targetType,
            JsonToken expToken, String msg, Object... msgArgs)
        throws DatabindException
    {
        msg = _format(msg, msgArgs);
        throw wrongTokenException(getParser(), targetType, expToken, msg);
    }

    public <T> T reportUnresolvedObjectId(ObjectIdReader oidReader, Object bean)
        throws DatabindException
    {
        String msg = String.format("No Object Id found for an instance of %s, to assign to property '%s'",
                ClassUtil.classNameOf(bean), oidReader.propertyName);
        return reportInputMismatch(oidReader.idProperty, msg);
    }

    /**
     * Helper method used to indicate a problem with input in cases where more
     * specific <code>reportXxx()</code> method was not available.
     */
    public <T> T reportInputMismatch(ValueDeserializer<?> src,
            String msg, Object... msgArgs)
        throws DatabindException
    {
        msg = _format(msg, msgArgs);
        throw MismatchedInputException.from(getParser(), src.handledType(), msg);
    }

    /**
     * Helper method used to indicate a problem with input in cases where more
     * specific <code>reportXxx()</code> method was not available.
     */
    public <T> T reportInputMismatch(Class<?> targetType,
            String msg, Object... msgArgs)
        throws DatabindException
    {
        msg = _format(msg, msgArgs);
        throw MismatchedInputException.from(getParser(), targetType, msg);
    }

    /**
     * Helper method used to indicate a problem with input in cases where more
     * specific <code>reportXxx()</code> method was not available.
     */
    public <T> T reportInputMismatch(JavaType targetType,
            String msg, Object... msgArgs)
        throws DatabindException
    {
        msg = _format(msg, msgArgs);
        throw MismatchedInputException.from(getParser(), targetType, msg);
    }

    /**
     * Helper method used to indicate a problem with input in cases where more
     * specific <code>reportXxx()</code> method was not available.
     */
    public <T> T reportInputMismatch(BeanProperty prop,
            String msg, Object... msgArgs)
        throws DatabindException
    {
        msg = _format(msg, msgArgs);
        JavaType type = (prop == null) ? null : prop.getType();
        final MismatchedInputException e = MismatchedInputException.from(getParser(), type, msg);
        // [databind#2357]: Include property name, if we have it
        if (prop != null) {
            AnnotatedMember member = prop.getMember();
            if (member != null) {
                e.prependPath(member.getDeclaringClass(), prop.getName());
            }
        }
        throw e;
    }

    /**
     * Helper method used to indicate a problem with input in cases where more
     * specific <code>reportXxx()</code> method was not available.
     */
    public <T> T reportPropertyInputMismatch(Class<?> targetType, String propertyName,
            String msg, Object... msgArgs)
        throws DatabindException
    {
        msg = _format(msg, msgArgs);
        MismatchedInputException e = MismatchedInputException.from(getParser(), targetType, msg);
        if (propertyName != null) {
            e.prependPath(targetType, propertyName);
        }
        throw e;
    }

    /**
     * Helper method used to indicate a problem with input in cases where more
     * specific <code>reportXxx()</code> method was not available.
     */
    public <T> T reportPropertyInputMismatch(JavaType targetType, String propertyName,
            String msg, Object... msgArgs)
            throws DatabindException
    {
        return reportPropertyInputMismatch(targetType.getRawClass(), propertyName, msg, msgArgs);
    }

    /**
     * Helper method used to indicate a problem with input in cases where specific
     * input coercion was not allowed.
     */
    public <T> T reportBadCoercion(ValueDeserializer<?> src,
            Class<?> targetType, Object inputValue,
            String msg, Object... msgArgs)
        throws DatabindException
    {
        msg = _format(msg, msgArgs);
        InvalidFormatException e = InvalidFormatException.from(getParser(),
                msg, inputValue, targetType);
        throw e;
    }

    public <T> T reportTrailingTokens(Class<?> targetType,
            JsonParser p, JsonToken trailingToken)
        throws DatabindException
    {
        throw MismatchedInputException.from(p, targetType, String.format(
"Trailing token (of type %s) found after value (bound as %s): not allowed as per `DeserializationFeature.FAIL_ON_TRAILING_TOKENS`",
trailingToken, ClassUtil.nameOf(targetType)
                ));
    }

    /*
    /**********************************************************************
    /* Methods for problem reporting, in cases where recovery
    /* is not considered possible: POJO definition problems
    /**********************************************************************
     */

    /**
     * Helper method called to indicate problem in POJO (serialization) definitions or settings
     * regarding specific Java type, unrelated to actual JSON content to map.
     * Default behavior is to construct and throw a {@link DatabindException}.
     */
    public <T> T reportBadTypeDefinition(BeanDescription bean,
            String msg, Object... msgArgs) throws DatabindException
    {
        msg = _format(msg, msgArgs);
        String beanDesc = ClassUtil.nameOf(bean.getBeanClass());
        msg = String.format("Invalid type definition for type %s: %s", beanDesc, msg);
        throw InvalidDefinitionException.from(_parser, msg, bean, null);
    }

    /**
     * Helper method called to indicate problem in POJO (serialization) definitions or settings
     * regarding specific property (of a type), unrelated to actual JSON content to map.
     * Default behavior is to construct and throw a {@link DatabindException}.
     */
    public <T> T reportBadPropertyDefinition(BeanDescription bean, BeanPropertyDefinition prop,
            String msg, Object... msgArgs)
        throws DatabindException
    {
        msg = _format(msg, msgArgs);
        String propName = ClassUtil.nameOf(prop);
        String beanDesc = ClassUtil.nameOf(bean.getBeanClass());
        msg = String.format("Invalid definition for property %s (of type %s): %s",
                propName, beanDesc, msg);
        throw InvalidDefinitionException.from(_parser, msg, bean, prop);
    }

    @Override
    public <T> T reportBadDefinition(JavaType type, String msg)
        throws DatabindException
    {
        throw InvalidDefinitionException.from(_parser, msg, type);
    }

    /*
    /**********************************************************************
    /* Methods for constructing semantic exceptions; usually not
    /* to be called directly, call `handleXxx()` instead
    /**********************************************************************
     */

    /**
     * Helper method for constructing {@link DatabindException} to indicate
     * that the token encountered was of type different than what <b>should</b>
     * be seen at that position, usually within a sequence of expected tokens.
     * Note that most of the time this method should NOT be directly called;
     * instead, {@link #reportWrongTokenException} should be called and will
     * call this method as necessary.
     */
    public DatabindException wrongTokenException(JsonParser p, JavaType targetType,
            JsonToken expToken, String extra)
    {
        String msg = String.format("Unexpected token (`JsonToken.%s`), expected `JsonToken.%s`",
                p.currentToken(), expToken);
        msg = _colonConcat(msg, extra);
        return MismatchedInputException.from(p, targetType, msg);
    }

    public DatabindException wrongTokenException(JsonParser p, Class<?> targetType,
            JsonToken expToken, String extra)
    {
        JsonToken t = (p == null) ? null : p.currentToken();
        String msg = String.format("Unexpected token (`JsonToken.%s`), expected `JsonToken.%s`", t, expToken);
        msg = _colonConcat(msg, extra);
        return MismatchedInputException.from(p, targetType, msg);
    }

    /**
     * Helper method for constructing exception to indicate that given JSON
     * Object field name was not in format to be able to deserialize specified
     * key type.
     * Note that most of the time this method should NOT be called; instead,
     * {@link #handleWeirdKey} should be called which will call this method
     * if necessary.
     */
    public DatabindException weirdKeyException(Class<?> keyClass, String keyValue,
            String msg) {
        return InvalidFormatException.from(_parser,
                String.format("Cannot deserialize Map key of type %s from String %s: %s",
                        ClassUtil.nameOf(keyClass), _quotedString(keyValue), msg),
                keyValue, keyClass);
    }

    /**
     * Helper method for constructing exception to indicate that input JSON
     * String was not suitable for deserializing into given target type.
     * Note that most of the time this method should NOT be called; instead,
     * {@link #handleWeirdStringValue} should be called which will call this method
     * if necessary.
     *
     * @param value String value from input being deserialized
     * @param instClass Type that String should be deserialized into
     * @param msgBase Message that describes specific problem
     */
    public DatabindException weirdStringException(String value, Class<?> instClass,
            String msgBase)
    {
        final String msg = String.format("Cannot deserialize value of type %s from String %s: %s",
                ClassUtil.nameOf(instClass), _quotedString(value), msgBase);
        return InvalidFormatException.from(_parser, msg, value, instClass);
    }

    /**
     * Helper method for constructing exception to indicate that input JSON
     * Number was not suitable for deserializing into given target type.
     * Note that most of the time this method should NOT be called; instead,
     * {@link #handleWeirdNumberValue} should be called which will call this method
     * if necessary.
     */
    public DatabindException weirdNumberException(Number value, Class<?> instClass,
            String msg) {
        return InvalidFormatException.from(_parser,
                String.format("Cannot deserialize value of type %s from number %s: %s",
                        ClassUtil.nameOf(instClass), String.valueOf(value), msg),
                value, instClass);
    }

    /**
     * Helper method for constructing exception to indicate that input JSON
     * token of type "native value" (see {@link JsonToken#VALUE_EMBEDDED_OBJECT})
     * is of incompatible type (and there is no delegating creator or such to use)
     * and can not be used to construct value of specified type (usually POJO).
     * Note that most of the time this method should NOT be called; instead,
     * {@link #handleWeirdNativeValue} should be called which will call this method
     */
    public DatabindException weirdNativeValueException(Object value, Class<?> instClass)
    {
        return InvalidFormatException.from(_parser, String.format(
"Cannot deserialize value of type %s from native value (`JsonToken.VALUE_EMBEDDED_OBJECT`) of type %s: incompatible types",
            ClassUtil.nameOf(instClass), ClassUtil.classNameOf(value)),
                value, instClass);
    }

    /**
     * Helper method for constructing instantiation exception for specified type,
     * to indicate problem with physically constructing instance of
     * specified class (missing constructor, exception from constructor)
     *<p>
     * Note that most of the time this method should NOT be called directly; instead,
     * {@link #handleInstantiationProblem} should be called which will call this method
     * if necessary.
     */
    public DatabindException instantiationException(Class<?> instClass, Throwable cause) {
        String excMsg;
        if (cause == null) {
            excMsg = "N/A";
        } else if ((excMsg = ClassUtil.exceptionMessage(cause)) == null) {
            excMsg = ClassUtil.nameOf(cause.getClass());
        }
        String msg = String.format("Cannot construct instance of %s, problem: %s",
                ClassUtil.nameOf(instClass), excMsg);
        // [databind#2162]: use specific exception type as we don't know if it's
        // due to type definition, input, or neither
        return ValueInstantiationException.from(_parser, msg, constructType(instClass), cause);
    }

    /**
     * Helper method for constructing instantiation exception for specified type,
     * to indicate that instantiation failed due to missing instantiator
     * (creator; constructor or factory method).
     *<p>
     * Note that most of the time this method should NOT be called; instead,
     * {@link #handleMissingInstantiator} should be called which will call this method
     * if necessary.
     */
    public DatabindException instantiationException(Class<?> instClass, String msg0) {
        // [databind#2162]: use specific exception type as we don't know if it's
        // due to type definition, input, or neither
        return ValueInstantiationException.from(_parser,
                String.format("Cannot construct instance of %s: %s",
                        ClassUtil.nameOf(instClass), msg0),
                constructType(instClass));
    }

    @Override
    public DatabindException invalidTypeIdException(JavaType baseType, String typeId,
            String extraDesc) {
        String msg = String.format("Could not resolve type id '%s' as a subtype of %s",
                typeId, ClassUtil.getTypeDescription(baseType));
        return InvalidTypeIdException.from(_parser, _colonConcat(msg, extraDesc), baseType, typeId);
    }

    public DatabindException missingTypeIdException(JavaType baseType,
            String extraDesc) {
        String msg = String.format("Could not resolve subtype of %s",
                baseType);
        return InvalidTypeIdException.from(_parser, _colonConcat(msg, extraDesc), baseType, null);
    }

    /*
    /**********************************************************************
    /* Other internal methods
    /**********************************************************************
     */

    /**
     * Helper method to get a non-shared instance of {@link DateFormat} with default
     * configuration; instance is lazily constructed, reused within same instance of
     * context (that is, within same life-cycle of <code>readValue()</code> from mapper
     * or reader). Reuse is safe since access will not occur from multiple threads
     * (unless caller somehow manages to share context objects across threads which is not
     * supported).
     */
    protected DateFormat _getDateFormat() {
        if (_dateFormat != null) {
            return _dateFormat;
        }
        // 24-Feb-2012, tatu: At this point, all timezone configuration should have
        // occurred, with respect to default date format and time zone configuration.
        // But we still better clone an instance as formatters may be stateful.
        DateFormat df = _config.getDateFormat();
        _dateFormat = df = (DateFormat) df.clone();
        return df;
    }

    /**
     * Helper method for constructing description like "Object value" given
     * {@link JsonToken} encountered.
     */
    protected String _shapeForToken(JsonToken t) {
        if (t != null) {
            switch (t) {
            // Likely Object values
            case START_OBJECT:
            case END_OBJECT:
            case PROPERTY_NAME:
                return "Object value";

            // Likely Array values
            case START_ARRAY:
            case END_ARRAY:
                return "Array value";

            case VALUE_FALSE:
            case VALUE_TRUE:
                return "Boolean value";

            case VALUE_EMBEDDED_OBJECT:
                return "Embedded Object";

            case VALUE_NUMBER_FLOAT:
                return "Floating-point value";
            case VALUE_NUMBER_INT:
                return "Integer value";
            case VALUE_STRING:
                return "String value";

            case VALUE_NULL:
                return "Null value";

            case NOT_AVAILABLE:
            default:
                return "[Unavailable value]";
            }
        }
        return "<end of input>";
    }
}
