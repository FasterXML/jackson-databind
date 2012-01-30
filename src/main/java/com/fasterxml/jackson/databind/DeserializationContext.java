package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.annotation.NoClass;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.LinkedNode;
import com.fasterxml.jackson.databind.util.ObjectBuffer;

/**
 * Context for the process of deserialization a single root-level value.
 * Used to allow passing in configuration settings and reusable temporary
 * objects (scrap arrays, containers).
 */
public class DeserializationContext
{
    /**
     * Let's limit length of error messages, for cases where underlying data
     * may be very large -- no point in spamming logs with megs of meaningless
     * data.
     */
    final static int MAX_ERROR_STR_LEN = 500;

    // // // Configuration
    
    protected final DeserializationConfig _config;

    protected final int _featureFlags;

    protected final Class<?> _view;

    /**
     * Currently active parser used for deserialization.
     * May be different from the outermost parser
     * when content is buffered.
     */
    protected JsonParser _parser;

    protected final DeserializerCache _deserCache;

    protected final InjectableValues _injectableValues;
    
    // // // Helper object recycling

    protected ArrayBuilders _arrayBuilders;

    protected ObjectBuffer _objectBuffer;

    protected DateFormat _dateFormat;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public DeserializationContext(DeserializationConfig config, JsonParser jp,
            DeserializerCache cache, InjectableValues injectableValues)
    {
        _config = config;
        _featureFlags = config.getDeserializationFeatures();
        _view = config.getActiveView();
        _parser = jp;
        _deserCache = cache;
        _injectableValues = injectableValues;
    }

    /*
    /**********************************************************
    /* Public API, accessors
    /**********************************************************
     */

    /**
     * Method for accessing configuration setting object for
     * currently active deserialization.
     */
    public DeserializationConfig getConfig() { return _config; }

    /**
     * Convenience method for checking whether specified on/off
     * feature is enabled
     */
    public final boolean isEnabled(DeserializationConfig.Feature feat) {
        /* 03-Dec-2010, tatu: minor shortcut; since this is called quite often,
         *   let's use a local copy of feature settings:
         */
        return (_featureFlags & feat.getMask()) != 0;
    }

    public final boolean isEnabled(MapperConfig.Feature feat) {
        return _config.isEnabled(feat);
    }
    
    public final AnnotationIntrospector getAnnotationIntrospector() {
        return _config.getAnnotationIntrospector();
    }
    
    public final DeserializerCache getDeserializerCache() {
        return _deserCache;
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

    public final Class<?> getActiveView() {
        return _view;
    }
    
    public final boolean canOverrideAccessModifiers() {
        return _config.canOverrideAccessModifiers();
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

    public final JsonNodeFactory getNodeFactory() {
        return _config.getNodeFactory();
    }

    public final JavaType constructType(Class<?> cls) {
        return _config.constructType(cls);
    }

    public final TypeFactory getTypeFactory() {
        return _config.getTypeFactory();
    }


    /*
    /**********************************************************
    /* Public API, pass-through to DeserializerCache
    /**********************************************************
     */
    /**
     * Convenience method, functionally same as:
     *<pre>
     *  getDeserializerProvider().findValueDeserializer(getConfig(), propertyType, property);
     *</pre>
     */
    public final JsonDeserializer<Object> findValueDeserializer(JavaType type,
            BeanProperty property) throws JsonMappingException {
        return _deserCache.findValueDeserializer(this, type, property);
    }
    
    /**
     * Convenience method, functionally same as:
     *<pre>
     *  getDeserializerProvider().findTypedValueDeserializer(getConfig(), propertyType, property);
     *</pre>
     */
    public final JsonDeserializer<Object> findTypedValueDeserializer(JavaType type,
            BeanProperty property) throws JsonMappingException {
        return _deserCache.findTypedValueDeserializer(this, type, property);
    }

    /**
     * Convenience method, functionally same as:
     *<pre>
     *  getDeserializerProvider().findKeyDeserializer(getConfig(), propertyType, property);
     *</pre>
     */
    public final KeyDeserializer findKeyDeserializer(JavaType keyType,
            BeanProperty property) throws JsonMappingException {
        return _deserCache.findKeyDeserializer(this, keyType, property);
    }
    
    /*
    /**********************************************************
    /* Extended API: handler instantiation
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public JsonDeserializer<Object> deserializerInstance(Annotated annotated,
            BeanProperty property, Object deserDef)
        throws JsonMappingException
    {
        if (deserDef == null) {
            return null;
        }
        JsonDeserializer<?> deser;
        
        if (deserDef instanceof JsonDeserializer) {
            deser = (JsonDeserializer<?>) deserDef;
        } else {
            /* Alas, there's no way to force return type of "either class
             * X or Y" -- need to throw an exception after the fact
             */
            if (!(deserDef instanceof Class)) {
                throw new IllegalStateException("AnnotationIntrospector returned deserializer definition of type "+deserDef.getClass().getName()+"; expected type JsonDeserializer or Class<JsonDeserializer> instead");
            }
            Class<?> deserClass = (Class<?>)deserDef;
            // there are some known "no class" markers to consider too:
            if (deserClass == JsonDeserializer.None.class || deserClass == NoClass.class) {
                return null;
            }
            if (!JsonDeserializer.class.isAssignableFrom(deserClass)) {
                throw new IllegalStateException("AnnotationIntrospector returned Class "+deserClass.getName()+"; expected Class<JsonDeserializer>");
            }
            HandlerInstantiator hi = _config.getHandlerInstantiator();
            if (hi != null) {
                deser = hi.deserializerInstance(_config, annotated, deserClass);
            } else {
                deser = (JsonDeserializer<?>) ClassUtil.createInstance(deserClass,
                        _config.canOverrideAccessModifiers());
            }
        }
        // First: need to resolve
        if (deser instanceof ResolvableDeserializer) {
            ((ResolvableDeserializer) deser).resolve(this);
        }
        // Second: contextualize:
        if (deser instanceof ContextualDeserializer<?>) {
            deser = ((ContextualDeserializer<?>) deser).createContextual(this, property);
        }
        return (JsonDeserializer<Object>) deser;
    }

    public final KeyDeserializer keyDeserializerInstance(Annotated annotated,
            BeanProperty property, Object deserDef)
        throws JsonMappingException
    {
        if (deserDef == null) {
            return null;
        }

        KeyDeserializer deser;
        
        if (deserDef instanceof KeyDeserializer) {
            deser = (KeyDeserializer) deserDef;
        } else {
            if (!(deserDef instanceof Class)) {
                throw new IllegalStateException("AnnotationIntrospector returned key deserializer definition of type "
                        +deserDef.getClass().getName()
                        +"; expected type KeyDeserializer or Class<KeyDeserializer> instead");
            }
            Class<?> deserClass = (Class<?>)deserDef;
            // there are some known "no class" markers to consider too:
            if (deserClass == KeyDeserializer.None.class || deserClass == NoClass.class) {
                return null;
            }
            if (!KeyDeserializer.class.isAssignableFrom(deserClass)) {
                throw new IllegalStateException("AnnotationIntrospector returned Class "+deserClass.getName()
                        +"; expected Class<KeyDeserializer>");
            }
            HandlerInstantiator hi = _config.getHandlerInstantiator();
            if (hi != null) {
                deser = hi.keyDeserializerInstance(_config, annotated, deserClass);
            } else {
                deser = (KeyDeserializer) ClassUtil.createInstance(deserClass,
                        _config.canOverrideAccessModifiers());
            }
        }
        // First: need to resolve
        if (deser instanceof ResolvableDeserializer) {
            ((ResolvableDeserializer) deser).resolve(this);
        }
        // Second: contextualize:
        if (deser instanceof ContextualKeyDeserializer) {
            deser = ((ContextualKeyDeserializer) deser).createContextual(this, property);
        }
        return deser;
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
    public Date parseDate(String dateStr)
        throws IllegalArgumentException
    {
        try {
            return getDateFormat().parse(dateStr);
        } catch (ParseException pex) {
            throw new IllegalArgumentException(pex.getMessage());
        }
    }

    /**
     * Convenience method for constructing Calendar instance set
     * to specified time, to be modified and used by caller.
     */
    public Calendar constructCalendar(Date d)
    {
        /* 08-Jan-2008, tatu: not optimal, but should work for the
         *   most part; let's revise as needed.
         */
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
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
    public boolean handleUnknownProperty(JsonParser jp, JsonDeserializer<?> deser, Object instanceOrClass, String propName)
        throws IOException, JsonProcessingException
    {
        LinkedNode<DeserializationProblemHandler> h = _config.getProblemHandlers();
        if (h != null) {
            /* 04-Jan-2009, tatu: Ugh. Need to mess with currently active parser
             *   since parser is not explicitly passed to handler... that was a mistake
             */
            JsonParser oldParser = _parser;
            _parser = jp;
            try {
                while (h != null) {
                    // Can bail out if it's handled
                    if (h.value().handleUnknownProperty(this, deser, instanceOrClass, propName)) {
                        return true;
                    }
                    h = h.next();
                }
            } finally {
                _parser = oldParser;
            }
        }
        return false;
    }

    /**
     * Helper method for constructing generic mapping exception for specified type
     */
    public JsonMappingException mappingException(Class<?> targetClass) {
        return mappingException(targetClass, _parser.getCurrentToken());
    }

    public JsonMappingException mappingException(Class<?> targetClass, JsonToken token)
    {
        String clsName = _calcName(targetClass);
        return JsonMappingException.from(_parser, "Can not deserialize instance of "+clsName+" out of "+token+" token");
    }
    
    /**
     * Helper method for constructing generic mapping exception with specified
     * message and current location information
     */
    public JsonMappingException mappingException(String message)
    {
        return JsonMappingException.from(getParser(), message);
    }
    
    /**
     * Helper method for constructing instantiation exception for specified type,
     * to indicate problem with physically constructing instance of
     * specified class (missing constructor, exception from constructor)
     */
    public JsonMappingException instantiationException(Class<?> instClass, Throwable t)
    {
        return JsonMappingException.from(_parser,
                "Can not construct instance of "+instClass.getName()+", problem: "+t.getMessage(),
                t);
    }

    public JsonMappingException instantiationException(Class<?> instClass, String msg)
    {
        return JsonMappingException.from(_parser, "Can not construct instance of "+instClass.getName()+", problem: "+msg);
    }
    
    /**
     * Method that will construct an exception suitable for throwing when
     * some String values are acceptable, but the one encountered is not.
     */
    public JsonMappingException weirdStringException(Class<?> instClass, String msg)
    {
        return JsonMappingException.from(_parser, "Can not construct instance of "+instClass.getName()+" from String value '"+_valueDesc()+"': "+msg);
    }

    /**
     * Helper method for constructing exception to indicate that input JSON
     * Number was not suitable for deserializing into given type.
     */
    public JsonMappingException weirdNumberException(Class<?> instClass, String msg)
    {
        return JsonMappingException.from(_parser, "Can not construct instance of "+instClass.getName()+" from number value ("+_valueDesc()+"): "+msg);
    }

    /**
     * Helper method for constructing exception to indicate that given JSON
     * Object field name was not in format to be able to deserialize specified
     * key type.
     */
    public JsonMappingException weirdKeyException(Class<?> keyClass, String keyValue, String msg)
    {
        return JsonMappingException.from(_parser, "Can not construct Map key of type "+keyClass.getName()+" from String \""+_desc(keyValue)+"\": "+msg);
    }

    /**
     * Helper method for indicating that the current token was expected to be another
     * token.
     */
    public JsonMappingException wrongTokenException(JsonParser jp, JsonToken expToken, String msg)
    {
        return JsonMappingException.from(jp, "Unexpected token ("+jp.getCurrentToken()+"), expected "+expToken+": "+msg);
    }
    
    /**
     * Helper method for constructing exception to indicate that JSON Object
     * field name did not map to a known property of type being
     * deserialized.
     * 
     * @param instanceOrClass Either value being populated (if one has been
     *   instantiated), or Class that indicates type that would be (or
     *   have been) instantiated
     */
    public JsonMappingException unknownFieldException(Object instanceOrClass, String fieldName)
    {
        return UnrecognizedPropertyException.from(_parser, instanceOrClass, fieldName);
    }

    /**
     * Helper method for constructing exception to indicate that given
     * type id (parsed from JSON) could not be converted to a Java type.
     */
    public JsonMappingException unknownTypeException(JavaType type, String id)
    {
        return JsonMappingException.from(_parser, "Could not resolve type id '"+id+"' into a subtype of "+type);
    }

    /*
    /**********************************************************
    /* Overridable internal methods
    /**********************************************************
     */

    protected DateFormat getDateFormat()
    {
        if (_dateFormat == null) {
            // must create a clone since Formats are not thread-safe:
            _dateFormat = (DateFormat)_config.getDateFormat().clone();
        }
        return _dateFormat;
    }

    protected String determineClassName(Object instance)
    {
        return ClassUtil.getClassDescription(instance);
    }
    
    /*
    /**********************************************************
    /* Other internal methods
    /**********************************************************
     */

    protected String _calcName(Class<?> cls)
    {
        if (cls.isArray()) {
            return _calcName(cls.getComponentType())+"[]";
        }
        return cls.getName();
    }
    
    protected String _valueDesc()
    {
        try {
            return _desc(_parser.getText());
        } catch (Exception e) {
            return "[N/A]";
        }
    }
    protected String _desc(String desc)
    {
        // !!! should we quote it? (in case there are control chars, linefeeds)
        if (desc.length() > MAX_ERROR_STR_LEN) {
            desc = desc.substring(0, MAX_ERROR_STR_LEN) + "]...[" + desc.substring(desc.length() - MAX_ERROR_STR_LEN);
        }
        return desc;
    }
}
