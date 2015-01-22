package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId;
import com.fasterxml.jackson.databind.deser.impl.TypeWrappedDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.*;

/**
 * Context for the process of deserialization a single root-level value.
 * Used to allow passing in configuration settings and reusable temporary
 * objects (scrap arrays, containers).
 *<p>
 * Instance life-cycle is such that an partially configured "blueprint" object
 * is registered with {@link ObjectMapper} (and {@link ObjectReader},
 * and when an actual instance is needed for deserialization,
 * a fully configured instance will
 * be created using a method in excented API of sub-class
 * ({@link com.fasterxml.jackson.databind.deser.DefaultDeserializationContext#createInstance}).
 * Each instance is guaranteed to only be used from single-threaded context;
 * instances may be reused iff no configuration has changed.
 *<p>
 * Defined as abstract class so that implementations must define methods
 * for reconfiguring blueprints and creating instances.
 */
public abstract class DeserializationContext
    extends DatabindContext
    implements java.io.Serializable
{
    private static final long serialVersionUID = -4290063686213707727L;

    /**
     * Let's limit length of error messages, for cases where underlying data
     * may be very large -- no point in spamming logs with megs of meaningless
     * data.
     */
    private final static int MAX_ERROR_STR_LEN = 500;

    /*
    /**********************************************************
    /* Configuration, immutable
    /**********************************************************
     */
    
    /**
     * Object that handle details of {@link JsonDeserializer} caching.
     */
    protected final DeserializerCache _cache;

    /*
    /**********************************************************
    /* Configuration, changeable via fluent factories
    /**********************************************************
     */

    /**
     * Read-only factory instance; exposed to let
     * owners (<code>ObjectMapper</code>, <code>ObjectReader</code>)
     * access it.
     */
    protected final DeserializerFactory _factory;

    /*
    /**********************************************************
    /* Configuration that gets set for instances (not blueprints)
    /* (partly denormalized for performance)
    /**********************************************************
     */

    /**
     * Generic deserialization processing configuration
     */
    protected final DeserializationConfig _config;

    /**
     * Bitmap of {@link DeserializationFeature}s that are enabled
     */
    protected final int _featureFlags;

    /**
     * Currently active view, if any.
     */
    protected final Class<?> _view;

    /**
     * Currently active parser used for deserialization.
     * May be different from the outermost parser
     * when content is buffered.
     */
    protected transient JsonParser _parser;
    
    /**
     * Object used for resolving references to injectable
     * values.
     */
    protected final InjectableValues _injectableValues;
    
    /*
    /**********************************************************
    /* Per-operation reusable helper objects (not for blueprints)
    /**********************************************************
     */

    protected transient ArrayBuilders _arrayBuilders;

    protected transient ObjectBuffer _objectBuffer;

    protected transient DateFormat _dateFormat;

    /**
     * Lazily-constructed holder for per-call attributes.
     * 
     * @since 2.3
     */
    protected transient ContextAttributes _attributes;

    /**
     * Type of {@link JsonDeserializer} (or, more specifically,
     *   {@link ContextualizableDeserializer}) that is being
     *   contextualized currently.
     *
     * @since 2.5
     */
    protected LinkedNode<JavaType> _currentType;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected DeserializationContext(DeserializerFactory df) {
        this(df, null);
    }
    
    protected DeserializationContext(DeserializerFactory df,
            DeserializerCache cache)
    {
        if (df == null) {
            throw new IllegalArgumentException("Can not pass null DeserializerFactory");
        }
        _factory = df;
        _cache = (cache == null) ? new DeserializerCache() : cache;
        
        _featureFlags = 0;
        _config = null;
        _injectableValues = null;
        _view = null;
        _attributes = null;
    }

    protected DeserializationContext(DeserializationContext src,
            DeserializerFactory factory)
    {
        _cache = src._cache;
        _factory = factory;
        
        _config = src._config;
        _featureFlags = src._featureFlags;
        _view = src._view;
        _parser = src._parser;
        _injectableValues = src._injectableValues;
        _attributes = src._attributes;
    }

    /**
     * Constructor used for creating actual per-call instances.
     */
    protected DeserializationContext(DeserializationContext src,
            DeserializationConfig config, JsonParser p,
            InjectableValues injectableValues)
    {
        _cache = src._cache;
        _factory = src._factory;
        
        _config = config;
        _featureFlags = config.getDeserializationFeatures();
        _view = config.getActiveView();
        _parser = p;
        _injectableValues = injectableValues;
        _attributes = config.getAttributes();
    }

    /**
     * Copy-constructor for use with <code>copy()</code> by {@link ObjectMapper#copy()}
     */
    protected DeserializationContext(DeserializationContext src) {
        _cache = new DeserializerCache();
        _factory = src._factory;

        _config = src._config;
        _featureFlags = src._featureFlags;
        _view = src._view;
        _injectableValues = null;
    }
    
    /*
    /**********************************************************
    /* DatabindContext implementation
    /**********************************************************
     */

    @Override
    public DeserializationConfig getConfig() { return _config; }

    @Override
    public final Class<?> getActiveView() { return _view; }

    @Override
    public final AnnotationIntrospector getAnnotationIntrospector() {
        return _config.getAnnotationIntrospector();
    }

    @Override
    public final TypeFactory getTypeFactory() {
        return _config.getTypeFactory();
    }

    /*
    /**********************************************************
    /* Access to per-call state, like generic attributes (2.3+)
    /**********************************************************
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
     * {@link ContextualDeserializer}, if any.
     * This is sometimes useful for generic {@link JsonDeserializer}s that
     * do not get passed (or do not retain) type information when being
     * constructed: happens for example for deserializers constructed
     * from annotations.
     * 
     * @since 2.5
     *
     * @return Type of {@link ContextualDeserializer} being contextualized,
     *   if process is on-going; null if not.
     */
    public JavaType getContextualType() {
        return (_currentType == null) ? null : _currentType.value();
    }

    /*
    /**********************************************************
    /* Public API, config setting accessors
    /**********************************************************
     */

    /**
     * Method for getting current {@link DeserializerFactory}.
     */
    public DeserializerFactory getFactory() {
        return _factory;
    }
    
    /**
     * Convenience method for checking whether specified on/off
     * feature is enabled
     */
    public final boolean isEnabled(DeserializationFeature feat) {
        /* 03-Dec-2010, tatu: minor shortcut; since this is called quite often,
         *   let's use a local copy of feature settings:
         */
        return (_featureFlags & feat.getMask()) != 0;
    }

    /**
     * "Bulk" access method for checking that all features specified by
     * mask are enabled.
     * 
     * @since 2.3
     */
    public final boolean hasDeserializationFeatures(int featureMask) {
        return _config.hasDeserializationFeatures(featureMask);
    }
    
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
            throw new IllegalStateException("No 'injectableValues' configured, can not inject value with id ["+valueId+"]");
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

    /**
     * Method for accessing default Locale to use: convenience method for
     *<pre>
     *   getConfig().getLocale();
     *</pre>
     */
    public Locale getLocale() {
        return _config.getLocale();
    }

    /**
     * Method for accessing default TimeZone to use: convenience method for
     *<pre>
     *   getConfig().getTimeZone();
     *</pre>
     */
    public TimeZone getTimeZone() {
        return _config.getTimeZone();
    }

    /*
    /**********************************************************
    /* Public API, pass-through to DeserializerCache
    /**********************************************************
     */

    @Deprecated // since 2.3, use overloaded variant
    public boolean hasValueDeserializerFor(JavaType type) {
        return hasValueDeserializerFor(type, null);
    }

    /**
     * Method for checking whether we could find a deserializer
     * for given type.
     *
     * @param type
     * @since 2.3
     */
    public boolean hasValueDeserializerFor(JavaType type, AtomicReference<Throwable> cause) {
        try {
            return _cache.hasValueDeserializerFor(this, _factory, type);
        } catch (JsonMappingException e) {
            if (cause != null) {
                cause.set(e);
            }
        } catch (RuntimeException e) {
            if (cause == null) { // earlier behavior
                throw e;
            }
            cause.set(e);
        }
        return false;
    }
    
    /**
     * Method for finding a value deserializer, and creating a contextual
     * version if necessary, for value reached via specified property.
     */
    @SuppressWarnings("unchecked")
    public final JsonDeserializer<Object> findContextualValueDeserializer(JavaType type,
            BeanProperty prop) throws JsonMappingException
    {
        JsonDeserializer<Object> deser = _cache.findValueDeserializer(this, _factory, type);
        if (deser != null) {
            deser = (JsonDeserializer<Object>) handleSecondaryContextualization(deser, prop, type);
        }
        return deser;
    }

    /**
     * Variant that will try to locate deserializer for current type, but without
     * performing any contextualization (unlike {@link #findContextualValueDeserializer})
     * or checking for need to create a {@link TypeDeserializer} (unlike
     * {@link #findRootValueDeserializer(JavaType)}.
     * This method is usually called from within {@link ResolvableDeserializer#resolve},
     * and expectation is that caller then calls either
     * {@link #handlePrimaryContextualization(JsonDeserializer, BeanProperty, JavaType)} or
     * {@link #handleSecondaryContextualization(JsonDeserializer, BeanProperty, JavaType)} at a
     * later point, as necessary.
     *
     * @since 2.5
     */
    public final JsonDeserializer<Object> findNonContextualValueDeserializer(JavaType type)
        throws JsonMappingException
    {
        return _cache.findValueDeserializer(this, _factory, type);
    }
    
    /**
     * Method for finding a deserializer for root-level value.
     */
    @SuppressWarnings("unchecked")
    public final JsonDeserializer<Object> findRootValueDeserializer(JavaType type)
        throws JsonMappingException
    {
        JsonDeserializer<Object> deser = _cache.findValueDeserializer(this,
                _factory, type);
        if (deser == null) { // can this occur?
            return null;
        }
        deser = (JsonDeserializer<Object>) handleSecondaryContextualization(deser, null, type);
        TypeDeserializer typeDeser = _factory.findTypeDeserializer(_config, type);
        if (typeDeser != null) {
            // important: contextualize to indicate this is for root value
            typeDeser = typeDeser.forProperty(null);
            return new TypeWrappedDeserializer(typeDeser, deser);
        }
        return deser;
    }

    /**
     * Convenience method, functionally same as:
     *<pre>
     *  getDeserializerProvider().findKeyDeserializer(getConfig(), prop.getType(), prop);
     *</pre>
     */
    public final KeyDeserializer findKeyDeserializer(JavaType keyType,
            BeanProperty prop) throws JsonMappingException {
        KeyDeserializer kd = _cache.findKeyDeserializer(this,
                _factory, keyType);
        // Second: contextualize?
        if (kd instanceof ContextualKeyDeserializer) {
            kd = ((ContextualKeyDeserializer) kd).createContextual(this, prop);
        }
        return kd;
    }
    
    /*
    /**********************************************************
    /* Public API, ObjectId handling
    /**********************************************************
     */

    /**
     * Method called to find and return entry corresponding to given
     * Object Id: will add an entry if necessary, and never returns null
     */
    public abstract ReadableObjectId findObjectId(Object id, ObjectIdGenerator<?> generator, ObjectIdResolver resolver);

    @Deprecated // since 2.4
    public abstract ReadableObjectId findObjectId(Object id, ObjectIdGenerator<?> generator);

    /**
     * Method called to ensure that every object id encounter during processing
     * are resolved.
     * 
     * @throws UnresolvedForwardReference
     */
    public abstract void checkUnresolvedObjectId()
        throws UnresolvedForwardReference;

    /*
    /**********************************************************
    /* Public API, type handling
    /**********************************************************
     */
    
    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *  getConfig().constructType(cls);
     * </pre>
     */
    public final JavaType constructType(Class<?> cls) {
        return _config.constructType(cls);
    }

    /**
     * Helper method to use for locating Class for given name. Should be used
     * instead of basic <code>Class.forName(className);</code> as it can
     * try using contextual class loader, or use platform-specific workarounds
     * (like on Android, GAE).
     */
    public Class<?> findClass(String className) throws ClassNotFoundException
    {
        // By default, delegate to ClassUtil: can be overridden with custom handling
        return ClassUtil.findClass(className);
    }

    /*
    /**********************************************************
    /* Public API, helper object recycling
    /**********************************************************
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
        /* Already have a reusable buffer? Let's retain bigger one
         * (or if equal, favor newer one, shorter life-cycle)
         */
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
    /**********************************************************
    /* Extended API: handler instantiation
    /**********************************************************
     */

    public abstract JsonDeserializer<Object> deserializerInstance(Annotated annotated,
            Object deserDef)
        throws JsonMappingException;

    public abstract KeyDeserializer keyDeserializerInstance(Annotated annotated,
            Object deserDef)
        throws JsonMappingException;

    /*
    /**********************************************************
    /* Extended API: resolving contextual deserializers; called
    /* by structured deserializers for their value/component
    /* deserializers
    /**********************************************************
     */

    /**
     * Method called for primary property deserializers (ones
     * directly created to deserialize values of a POJO property),
     * to handle details of resolving
     * {@link ContextualDeserializer} with given property context.
     * 
     * @param prop Property for which the given primary deserializer is used; never null.
     * 
     * @since 2.5
     */
    public JsonDeserializer<?> handlePrimaryContextualization(JsonDeserializer<?> deser,
            BeanProperty prop, JavaType type)
        throws JsonMappingException
    {
        if (deser instanceof ContextualDeserializer) {
            _currentType = new LinkedNode<JavaType>(type, _currentType);
            try {
                deser = ((ContextualDeserializer) deser).createContextual(this, prop);
            } finally {
                _currentType = _currentType.next();
            }
        }
        return deser;
    }

    @Deprecated // since 2.5; remove from 2.6
    public JsonDeserializer<?> handlePrimaryContextualization(JsonDeserializer<?> deser,
            BeanProperty prop)
        throws JsonMappingException
    {
        return handlePrimaryContextualization(deser, prop, TypeFactory.unknownType());
    }
    
    /**
     * Method called for secondary property deserializers (ones
     * NOT directly created to deal with an annotatable POJO property,
     * but instead created as a component -- such as value deserializers
     * for structured types, or deserializers for root values)
     * to handle details of resolving
     * {@link ContextualDeserializer} with given property context.
     * Given that these deserializers are not directly related to given property
     * (or, in case of root value property, to any property), annotations
     * accessible may or may not be relevant.
     * 
     * @param prop Property for which deserializer is used, if any; null
     *    when deserializing root values
     * 
     * @since 2.5
     */
    public JsonDeserializer<?> handleSecondaryContextualization(JsonDeserializer<?> deser,
            BeanProperty prop, JavaType type)
        throws JsonMappingException
    {
        if (deser instanceof ContextualDeserializer) {
            _currentType = new LinkedNode<JavaType>(type, _currentType);
            try {
                deser = ((ContextualDeserializer) deser).createContextual(this, prop);
            } finally {
                _currentType = _currentType.next();
            }
        }
        return deser;
    }

    @Deprecated // since 2.5; remove from 2.6
    public JsonDeserializer<?> handleSecondaryContextualization(JsonDeserializer<?> deser,
            BeanProperty prop)
        throws JsonMappingException
    {
        if (deser instanceof ContextualDeserializer) {
            deser = ((ContextualDeserializer) deser).createContextual(this, prop);
        }
        return deser;
    }

    /*
    /**********************************************************
    /* Parsing methods that may use reusable/-cyclable objects
    /**********************************************************
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
            DateFormat df = getDateFormat();
            return df.parse(dateStr);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse Date value '"+dateStr+"': "+e.getMessage());
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
    /**********************************************************
    /* Convenience methods for reading parsed values
    /**********************************************************
     */

    /**
     * Convenience method that may be used by composite or container deserializers,
     * for reading one-off values contained (for sequences, it is more efficient
     * to actually fetch deserializer once for the whole collection).
     *<p>
     * NOTE: when deserializing values of properties contained in composite types,
     * rather use {@link #readPropertyValue(JsonParser, BeanProperty, Class)};
     * this method does not allow use of contextual annotations.
     * 
     * @since 2.4
     */
    public <T> T readValue(JsonParser p, Class<T> type) throws IOException {
        return readValue(p, getTypeFactory().constructType(type));
    }

    /**
     * @since 2.4
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, JavaType type) throws IOException {
        JsonDeserializer<Object> deser = findRootValueDeserializer(type);
        if (deser == null) {
        }
        return (T) deser.deserialize(p, this);
    }

    /**
     * Convenience method that may be used by composite or container deserializers,
     * for reading one-off values for the composite type, taking into account
     * annotations that the property (passed to this method -- usually property that
     * has custom serializer that called this method) has.
     * 
     * @since 2.4
     */
    public <T> T readPropertyValue(JsonParser p, BeanProperty prop, Class<T> type) throws IOException {
        return readPropertyValue(p, prop, getTypeFactory().constructType(type));
    }

    /**
     * @since 2.4
     */
    @SuppressWarnings("unchecked")
    public <T> T readPropertyValue(JsonParser p, BeanProperty prop, JavaType type) throws IOException {
        JsonDeserializer<Object> deser = findContextualValueDeserializer(type, prop);
        if (deser == null) {
            
        }
        return (T) deser.deserialize(p, this);
    }
    
    /*
    /**********************************************************
    /* Methods for problem handling, reporting
    /**********************************************************
     */

    /**
     * Method deserializers can call to inform configured {@link DeserializationProblemHandler}s
     * of an unrecognized property.
     * 
     * @return True if there was a configured problem handler that was able to handle the
     *   problem
     */
    /**
     * Method deserializers can call to inform configured {@link DeserializationProblemHandler}s
     * of an unrecognized property.
     */
    public boolean handleUnknownProperty(JsonParser p, JsonDeserializer<?> deser,
            Object instanceOrClass, String propName)
        throws IOException, JsonProcessingException
    {
        LinkedNode<DeserializationProblemHandler> h = _config.getProblemHandlers();
        if (h != null) {
            while (h != null) {
                // Can bail out if it's handled
                if (h.value().handleUnknownProperty(this, p, deser, instanceOrClass, propName)) {
                    return true;
                }
                h = h.next();
            }
        }
        return false;
    }

    /**
     * Helper method for reporting a problem with unhandled unknown exception
     * 
     * @param instanceOrClass Either value being populated (if one has been
     *   instantiated), or Class that indicates type that would be (or
     *   have been) instantiated
     * @param deser Deserializer that had the problem, if called by deserializer
     *   (or on behalf of one)
     */
    public void reportUnknownProperty(Object instanceOrClass, String fieldName,
            JsonDeserializer<?> deser)
        throws JsonMappingException
    {
        if (!isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
            return;
        }
        // Do we know properties that are expected instead?
        Collection<Object> propIds = (deser == null) ? null : deser.getKnownPropertyNames();
        throw UnrecognizedPropertyException.from(_parser,
                instanceOrClass, fieldName, propIds);
    }
    
    /*
    /**********************************************************
    /* Methods for constructing exceptions
    /**********************************************************
     */
    
    /**
     * Helper method for constructing generic mapping exception for specified type
     */
    public JsonMappingException mappingException(Class<?> targetClass) {
        return mappingException(targetClass, _parser.getCurrentToken());
    }

    public JsonMappingException mappingException(Class<?> targetClass, JsonToken token) {
        return JsonMappingException.from(_parser, "Can not deserialize instance of "+_calcName(targetClass)+" out of "+token+" token");
    }
    
    /**
     * Helper method for constructing generic mapping exception with specified
     * message and current location information
     */
    public JsonMappingException mappingException(String message) {
        return JsonMappingException.from(getParser(), message);
    }
    
    /**
     * Helper method for constructing instantiation exception for specified type,
     * to indicate problem with physically constructing instance of
     * specified class (missing constructor, exception from constructor)
     */
    public JsonMappingException instantiationException(Class<?> instClass, Throwable t) {
        return JsonMappingException.from(_parser,
                "Can not construct instance of "+instClass.getName()+", problem: "+t.getMessage(), t);
    }

    public JsonMappingException instantiationException(Class<?> instClass, String msg) {
        return JsonMappingException.from(_parser, "Can not construct instance of "+instClass.getName()+", problem: "+msg);
    }
    
    /**
     * Method that will construct an exception suitable for throwing when
     * some String values are acceptable, but the one encountered is not.
     * 
     * 
     * @deprecated Since 2.1 should use variant that takes value
     */
    @Deprecated
    public JsonMappingException weirdStringException(Class<?> instClass, String msg) {
        return weirdStringException(null, instClass, msg);
    }

    /**
     * Method that will construct an exception suitable for throwing when
     * some String values are acceptable, but the one encountered is not.
     * 
     * @param value String value from input being deserialized
     * @param instClass Type that String should be deserialized into
     * @param msg Message that describes specific problem
     * 
     * @since 2.1
     */
    public JsonMappingException weirdStringException(String value, Class<?> instClass, String msg) {
        return InvalidFormatException.from(_parser,
                "Can not construct instance of "+instClass.getName()+" from String value '"+_valueDesc()+"': "+msg,
                value, instClass);
    }

    /**
     * Helper method for constructing exception to indicate that input JSON
     * Number was not suitable for deserializing into given type.
     */
    @Deprecated
    public JsonMappingException weirdNumberException(Class<?> instClass, String msg) {
        return weirdStringException(null, instClass, msg);
    }

    /**
     * Helper method for constructing exception to indicate that input JSON
     * Number was not suitable for deserializing into given target type.
     */
    public JsonMappingException weirdNumberException(Number value, Class<?> instClass, String msg) {
        return InvalidFormatException.from(_parser,
                "Can not construct instance of "+instClass.getName()+" from number value ("+_valueDesc()+"): "+msg,
                null, instClass);
    }
    
    /**
     * Helper method for constructing exception to indicate that given JSON
     * Object field name was not in format to be able to deserialize specified
     * key type.
     */
    public JsonMappingException weirdKeyException(Class<?> keyClass, String keyValue, String msg) {
        return InvalidFormatException.from(_parser,
                "Can not construct Map key of type "+keyClass.getName()+" from String \""+_desc(keyValue)+"\": "+msg,
                keyValue, keyClass);
    }

    /**
     * Helper method for indicating that the current token was expected to be another
     * token.
     */
    public JsonMappingException wrongTokenException(JsonParser p, JsonToken expToken, String msg0) {
        String msg = "Unexpected token ("+p.getCurrentToken()+"), expected "+expToken;
        if (msg0 != null) {
            msg = msg + ": "+msg0;
        }
        return JsonMappingException.from(p, msg);
    }

    /**
     * Helper method for constructing exception to indicate that given
     * type id (parsed from JSON) could not be converted to a Java type.
     */
    @Deprecated // since 2.5, use overloaded variant
    public JsonMappingException unknownTypeException(JavaType type, String id) {
        return JsonMappingException.from(_parser, "Could not resolve type id '"+id+"' into a subtype of "+type);
    }

    /**
     * @since 2.5
     */
    public JsonMappingException unknownTypeException(JavaType type, String id,
            String extraDesc) {
        String msg = "Could not resolve type id '"+id+"' into a subtype of "+type;
        if (extraDesc != null) {
            msg = msg + ": "+extraDesc;
        }
        return JsonMappingException.from(_parser, msg);
    }

    public JsonMappingException endOfInputException(Class<?> instClass) {
        return JsonMappingException.from(_parser, "Unexpected end-of-input when trying to deserialize a "
                +instClass.getName());
    }

    /*
    /**********************************************************
    /* Overridable internal methods
    /**********************************************************
     */

    protected DateFormat getDateFormat()
    {
        if (_dateFormat != null) {
            return _dateFormat;
        }
        /* 24-Feb-2012, tatu: At this point, all timezone configuration
         *    should have occurred, with respect to default dateformat
         *    and timezone configuration. But we still better clone
         *    an instance as formatters may be stateful.
         */
        DateFormat df = _config.getDateFormat();
        _dateFormat = df = (DateFormat) df.clone();
        return df;
    }

    protected String determineClassName(Object instance) {
        return ClassUtil.getClassDescription(instance);
    }
    
    /*
    /**********************************************************
    /* Other internal methods
    /**********************************************************
     */

    protected String _calcName(Class<?> cls) {
        if (cls.isArray()) {
            return _calcName(cls.getComponentType())+"[]";
        }
        return cls.getName();
    }
    
    protected String _valueDesc() {
        try {
            return _desc(_parser.getText());
        } catch (Exception e) {
            return "[N/A]";
        }
    }

    protected String _desc(String desc) {
        // !!! should we quote it? (in case there are control chars, linefeeds)
        if (desc.length() > MAX_ERROR_STR_LEN) {
            desc = desc.substring(0, MAX_ERROR_STR_LEN) + "]...[" + desc.substring(desc.length() - MAX_ERROR_STR_LEN);
        }
        return desc;
    }
}
