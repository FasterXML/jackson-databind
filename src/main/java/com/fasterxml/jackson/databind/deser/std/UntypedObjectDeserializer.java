package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.ObjectBuffer;

/**
 * Deserializer implementation that is used if it is necessary to bind content of
 * "unknown" type; something declared as basic {@link java.lang.Object}
 * (either explicitly, or due to type erasure).
 * If so, "natural" mapping is used to convert JSON values to their natural
 * Java object matches: JSON arrays to Java {@link java.util.List}s (or, if configured,
 * Object[]), JSON objects to {@link java.util.Map}s, numbers to
 * {@link java.lang.Number}s, booleans to {@link java.lang.Boolean}s and
 * strings to {@link java.lang.String} (and nulls to nulls).
 */
@JacksonStdImpl
public class UntypedObjectDeserializer
    extends StdDeserializer<Object>
    implements ResolvableDeserializer, ContextualDeserializer
{
    private static final long serialVersionUID = 1L;

    protected final static Object[] NO_OBJECTS = new Object[0];

    /**
     * @deprecated Since 2.3, construct a new instance, needs to be resolved
     */
    @Deprecated
    public final static UntypedObjectDeserializer instance = new UntypedObjectDeserializer();

    /*
    /**********************************************************
    /* Possible custom deserializer overrides we need to use
    /**********************************************************
     */

    protected JsonDeserializer<Object> _mapDeserializer;

    protected JsonDeserializer<Object> _listDeserializer;

    protected JsonDeserializer<Object> _stringDeserializer;

    protected JsonDeserializer<Object> _numberDeserializer;
    
    public UntypedObjectDeserializer() {
        super(Object.class);
    }

    @SuppressWarnings("unchecked")
    public UntypedObjectDeserializer(UntypedObjectDeserializer base,
            JsonDeserializer<?> mapDeser, JsonDeserializer<?> listDeser,
            JsonDeserializer<?> stringDeser, JsonDeserializer<?> numberDeser)
    {
        super(Object.class);
        _mapDeserializer = (JsonDeserializer<Object>) mapDeser;
        _listDeserializer = (JsonDeserializer<Object>) listDeser;
        _stringDeserializer = (JsonDeserializer<Object>) stringDeser;
        _numberDeserializer = (JsonDeserializer<Object>) numberDeser;
    }
    
    /*
    /**********************************************************
    /* Initialization
    /**********************************************************
     */

    /**
     * We need to implement this method to properly find things to delegate
     * to: it can not be done earlier since delegated deserializers almost
     * certainly require access to this instance (at least "List" and "Map" ones)
     */
    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException
    {
        JavaType obType = ctxt.constructType(Object.class);
        JavaType stringType = ctxt.constructType(String.class);
        TypeFactory tf = ctxt.getTypeFactory();
        _mapDeserializer = _findCustomDeser(ctxt, tf.constructMapType(Map.class, stringType, obType));
        _listDeserializer = _findCustomDeser(ctxt, tf.constructCollectionType(List.class, obType));
        _stringDeserializer = _findCustomDeser(ctxt, stringType);
        _numberDeserializer = _findCustomDeser(ctxt, tf.constructType(Number.class));
    }

    @SuppressWarnings("unchecked")
    protected JsonDeserializer<Object> _findCustomDeser(DeserializationContext ctxt, JavaType type)
        throws JsonMappingException
    {
        // NOTE: since we don't yet have the referring property, this should be fine:
        JsonDeserializer<?> deser = ctxt.findRootValueDeserializer(type);
        if (ClassUtil.isJacksonStdImpl(deser)) {
            return null;
        }
        return (JsonDeserializer<Object>) deser;
    }
    
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        // 20-Apr-2014, tatu: If nothing custom, let's use "vanilla" instance,
        //     simpler and can avoid some of delegation
        if ((_stringDeserializer == null) && (_numberDeserializer == null)
                && (_mapDeserializer == null) && (_listDeserializer == null)
                &&  getClass() == UntypedObjectDeserializer.class) {
            return Vanilla.std;
        }
        JsonDeserializer<?> mapDeserializer = _mapDeserializer;
        if (mapDeserializer instanceof ContextualDeserializer) {
            mapDeserializer = ((ContextualDeserializer)mapDeserializer).createContextual(ctxt, property);
        }
        JsonDeserializer<?> listDeserializer = _listDeserializer;
        if (listDeserializer instanceof ContextualDeserializer) {
            listDeserializer = ((ContextualDeserializer)listDeserializer).createContextual(ctxt, property);
        }
        JsonDeserializer<?> stringDeserializer = _stringDeserializer;
        if (stringDeserializer instanceof ContextualDeserializer) {
            stringDeserializer = ((ContextualDeserializer)stringDeserializer).createContextual(ctxt, property);
        }
        JsonDeserializer<?> numberDeserializer = _numberDeserializer;
        if (numberDeserializer instanceof ContextualDeserializer) {
            numberDeserializer = ((ContextualDeserializer)numberDeserializer).createContextual(ctxt, property);
        }

        // And if anything changed, we'll need to change too!
        if ((mapDeserializer != _mapDeserializer)
                || (listDeserializer != _listDeserializer)
                || (stringDeserializer != _stringDeserializer)
                || (numberDeserializer != _numberDeserializer)
                ) {
            return _withResolved(mapDeserializer, listDeserializer,
                    stringDeserializer, numberDeserializer);
        }
        return this;
    }

    protected JsonDeserializer<?> _withResolved(JsonDeserializer<?> mapDeser,
            JsonDeserializer<?> listDeser,
            JsonDeserializer<?> stringDeser, JsonDeserializer<?> numberDeser) {
        return new UntypedObjectDeserializer(this,
                mapDeser, listDeser, stringDeser, numberDeser);
    }

    /*
    /**********************************************************
    /* Deserializer API
    /**********************************************************
     */
    
    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        switch (jp.getCurrentToken()) {
        case FIELD_NAME:
        case START_OBJECT:
            if (_mapDeserializer != null) {
                return _mapDeserializer.deserialize(jp, ctxt);
            }
            return mapObject(jp, ctxt);
        case START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
                return mapArrayToArray(jp, ctxt);
            }
            if (_listDeserializer != null) {
                return _listDeserializer.deserialize(jp, ctxt);
            }
            return mapArray(jp, ctxt);
        case VALUE_EMBEDDED_OBJECT:
            return jp.getEmbeddedObject();
        case VALUE_STRING:
            if (_stringDeserializer != null) {
                return _stringDeserializer.deserialize(jp, ctxt);
            }
            return jp.getText();

        case VALUE_NUMBER_INT:
            if (_numberDeserializer != null) {
                return _numberDeserializer.deserialize(jp, ctxt);
            }
            /* [JACKSON-100]: caller may want to get all integral values
             * returned as BigInteger, for consistency
             */
            if (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
                return jp.getBigIntegerValue(); // should be optimal, whatever it is
            }
            return jp.getNumberValue(); // should be optimal, whatever it is

        case VALUE_NUMBER_FLOAT:
            if (_numberDeserializer != null) {
                return _numberDeserializer.deserialize(jp, ctxt);
            }
            /* [JACKSON-72]: need to allow overriding the behavior regarding
             *   which type to use
             */
            if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                return jp.getDecimalValue();
            }
            return Double.valueOf(jp.getDoubleValue());

        case VALUE_TRUE:
            return Boolean.TRUE;
        case VALUE_FALSE:
            return Boolean.FALSE;

        case VALUE_NULL: // should not get this but...
            return null;

        case END_ARRAY: // invalid
        case END_OBJECT: // invalid
        default:
            throw ctxt.mappingException(Object.class);
        }
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException
    {
        JsonToken t = jp.getCurrentToken();
        switch (t) {
        // First: does it look like we had type id wrapping of some kind?
        case START_ARRAY:
        case START_OBJECT:
        case FIELD_NAME:
            /* Output can be as JSON Object, Array or scalar: no way to know
             * a this point:
             */
            return typeDeserializer.deserializeTypedFromAny(jp, ctxt);

        /* Otherwise we probably got a "native" type (ones that map
         * naturally and thus do not need or use type ids)
         */
        case VALUE_STRING:
            if (_stringDeserializer != null) {
                return _stringDeserializer.deserialize(jp, ctxt);
            }
            return jp.getText();

        case VALUE_NUMBER_INT:
            if (_numberDeserializer != null) {
                return _numberDeserializer.deserialize(jp, ctxt);
            }
            // For [JACKSON-100], see above:
            if (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
                return jp.getBigIntegerValue();
            }
            /* and as per [JACKSON-839], allow "upgrade" to bigger types: out-of-range
             * entries can not be produced without type, so this should "just work",
             * even if it is bit unclean
             */
            return jp.getNumberValue();

        case VALUE_NUMBER_FLOAT:
            if (_numberDeserializer != null) {
                return _numberDeserializer.deserialize(jp, ctxt);
            }
            // For [JACKSON-72], see above
            if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                return jp.getDecimalValue();
            }
            return Double.valueOf(jp.getDoubleValue());

        case VALUE_TRUE:
            return Boolean.TRUE;
        case VALUE_FALSE:
            return Boolean.FALSE;
        case VALUE_EMBEDDED_OBJECT:
            return jp.getEmbeddedObject();

        case VALUE_NULL: // should not get this far really but...
            return null;
        default:
            throw ctxt.mappingException(Object.class);
        }
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    /**
     * Method called to map a JSON Array into a Java value.
     */
    protected Object mapArray(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        // Minor optimization to handle small lists (default size for ArrayList is 10)
        if (jp.nextToken()  == JsonToken.END_ARRAY) {
            return new ArrayList<Object>(2);
        }
        Object value = deserialize(jp, ctxt);
        if (jp.nextToken()  == JsonToken.END_ARRAY) {
            ArrayList<Object> l = new ArrayList<Object>(2);
            l.add(value);
            return l;
        }
        Object value2 = deserialize(jp, ctxt);
        if (jp.nextToken()  == JsonToken.END_ARRAY) {
            ArrayList<Object> l = new ArrayList<Object>(2);
            l.add(value);
            l.add(value2);
            return l;
        }
        ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] values = buffer.resetAndStart();
        int ptr = 0;
        values[ptr++] = value;
        values[ptr++] = value2;
        int totalSize = ptr;
        do {
            value = deserialize(jp, ctxt);
            ++totalSize;
            if (ptr >= values.length) {
                values = buffer.appendCompletedChunk(values);
                ptr = 0;
            }
            values[ptr++] = value;
        } while (jp.nextToken() != JsonToken.END_ARRAY);
        // let's create full array then
        ArrayList<Object> result = new ArrayList<Object>(totalSize);
        buffer.completeAndClearBuffer(values, ptr, result);
        return result;
    }

    /**
     * Method called to map a JSON Object into a Java value.
     */
    protected Object mapObject(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        // minor optimization; let's handle 1 and 2 entry cases separately
        if (t == JsonToken.END_OBJECT) { // and empty one too
            // empty map might work; but caller may want to modify... so better just give small modifiable
            return new LinkedHashMap<String,Object>(2);
        }
        String field1 = jp.getCurrentName();
        jp.nextToken();
        Object value1 = deserialize(jp, ctxt);
        if (jp.nextToken() == JsonToken.END_OBJECT) { // single entry; but we want modifiable
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(2);
            result.put(field1, value1);
            return result;
        }
        String field2 = jp.getCurrentName();
        jp.nextToken();
        Object value2 = deserialize(jp, ctxt);
        if (jp.nextToken() == JsonToken.END_OBJECT) {
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(4);
            result.put(field1, value1);
            result.put(field2, value2);
            return result;
        }
        // And then the general case; default map size is 16
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put(field1, value1);
        result.put(field2, value2);
        do {
            String fieldName = jp.getCurrentName();
            jp.nextToken();
            result.put(fieldName, deserialize(jp, ctxt));
        } while (jp.nextToken() != JsonToken.END_OBJECT);
        return result;
    }

    /**
     * Method called to map a JSON Array into a Java Object array (Object[]).
     */
    protected Object[] mapArrayToArray(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        // Minor optimization to handle small lists (default size for ArrayList is 10)
        if (jp.nextToken()  == JsonToken.END_ARRAY) {
            return NO_OBJECTS;
        }
        ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] values = buffer.resetAndStart();
        int ptr = 0;
        do {
            Object value = deserialize(jp, ctxt);
            if (ptr >= values.length) {
                values = buffer.appendCompletedChunk(values);
                ptr = 0;
            }
            values[ptr++] = value;
        } while (jp.nextToken() != JsonToken.END_ARRAY);
        return buffer.completeAndClearBuffer(values, ptr);
    }

    /*
    /**********************************************************
    /* Separate "vanilla" implementation for common case of
    /* no custom deserializer overrides
    /**********************************************************
     */

    @JacksonStdImpl
    public static class Vanilla
        extends StdDeserializer<Object>
    {
        private static final long serialVersionUID = 1L;

        public final static Vanilla std = new Vanilla();

        public Vanilla() { super(Object.class); }
        
        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            switch (jp.getCurrentTokenId()) {
            case JsonTokenId.ID_START_OBJECT:
                {
                    JsonToken t = jp.nextToken();
                    if (t  == JsonToken.END_OBJECT) {
                        return new LinkedHashMap<String,Object>(2);
                    }
                }
            case JsonTokenId.ID_FIELD_NAME:
                return mapObject(jp, ctxt);
            case JsonTokenId.ID_START_ARRAY:
                {
                    JsonToken t = jp.nextToken();
                    if (t == JsonToken.END_ARRAY) { // and empty one too
                        if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
                            return NO_OBJECTS;
                        }
                        return new ArrayList<Object>(2);
                    }
                }
                if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
                    return mapArrayToArray(jp, ctxt);
                }
                return mapArray(jp, ctxt);
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                return jp.getEmbeddedObject();
            case JsonTokenId.ID_STRING:
                return jp.getText();

            case JsonTokenId.ID_NUMBER_INT:
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
                    return jp.getBigIntegerValue(); // should be optimal, whatever it is
                }
                return jp.getNumberValue(); // should be optimal, whatever it is

            case JsonTokenId.ID_NUMBER_FLOAT:
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                    return jp.getDecimalValue();
                }
                return Double.valueOf(jp.getDoubleValue());

            case JsonTokenId.ID_TRUE:
                return Boolean.TRUE;
            case JsonTokenId.ID_FALSE:
                return Boolean.FALSE;

            case JsonTokenId.ID_NULL: // should not get this but...
                return null;

            //case JsonTokenId.ID_END_ARRAY: // invalid
            //case JsonTokenId.ID_END_OBJECT: // invalid
            default:
                throw ctxt.mappingException(Object.class);
            }
        }

        @Override
        public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException
        {
            switch (jp.getCurrentTokenId()) {
            case JsonTokenId.ID_START_ARRAY:
            case JsonTokenId.ID_START_OBJECT:
            case JsonTokenId.ID_FIELD_NAME:
                return typeDeserializer.deserializeTypedFromAny(jp, ctxt);

            case JsonTokenId.ID_STRING:
                return jp.getText();

            case JsonTokenId.ID_NUMBER_INT:
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
                    return jp.getBigIntegerValue();
                }
                return jp.getNumberValue();

            case JsonTokenId.ID_NUMBER_FLOAT:
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                    return jp.getDecimalValue();
                }
                return Double.valueOf(jp.getDoubleValue());

            case JsonTokenId.ID_TRUE:
                return Boolean.TRUE;
            case JsonTokenId.ID_FALSE:
                return Boolean.FALSE;
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                return jp.getEmbeddedObject();

            case JsonTokenId.ID_NULL: // should not get this far really but...
                return null;
            default:
                throw ctxt.mappingException(Object.class);
            }
        }

        protected Object mapArray(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            Object value = deserialize(jp, ctxt);
            if (jp.nextToken()  == JsonToken.END_ARRAY) {
                ArrayList<Object> l = new ArrayList<Object>(2);
                l.add(value);
                return l;
            }
            Object value2 = deserialize(jp, ctxt);
            if (jp.nextToken()  == JsonToken.END_ARRAY) {
                ArrayList<Object> l = new ArrayList<Object>(2);
                l.add(value);
                l.add(value2);
                return l;
            }
            ObjectBuffer buffer = ctxt.leaseObjectBuffer();
            Object[] values = buffer.resetAndStart();
            int ptr = 0;
            values[ptr++] = value;
            values[ptr++] = value2;
            int totalSize = ptr;
            do {
                value = deserialize(jp, ctxt);
                ++totalSize;
                if (ptr >= values.length) {
                    values = buffer.appendCompletedChunk(values);
                    ptr = 0;
                }
                values[ptr++] = value;
            } while (jp.nextToken() != JsonToken.END_ARRAY);
            // let's create full array then
            ArrayList<Object> result = new ArrayList<Object>(totalSize);
            buffer.completeAndClearBuffer(values, ptr, result);
            return result;
        }

        /**
         * Method called to map a JSON Object into a Java value.
         */
        protected Object mapObject(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            // will point to FIELD_NAME at this point, guaranteed
            String field1 = jp.getText();
            jp.nextToken();
            Object value1 = deserialize(jp, ctxt);
            if (jp.nextToken() == JsonToken.END_OBJECT) { // single entry; but we want modifiable
                LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(2);
                result.put(field1, value1);
                return result;
            }
            String field2 = jp.getText();
            jp.nextToken();
            Object value2 = deserialize(jp, ctxt);
            if (jp.nextToken() == JsonToken.END_OBJECT) {
                LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(4);
                result.put(field1, value1);
                result.put(field2, value2);
                return result;
            }
            // And then the general case; default map size is 16
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
            result.put(field1, value1);
            result.put(field2, value2);
            do {
                String fieldName = jp.getText();
                jp.nextToken();
                result.put(fieldName, deserialize(jp, ctxt));
            } while (jp.nextToken() != JsonToken.END_OBJECT);
            return result;
        }

        /**
         * Method called to map a JSON Array into a Java Object array (Object[]).
         */
        protected Object[] mapArrayToArray(JsonParser jp, DeserializationContext ctxt) throws IOException {
            ObjectBuffer buffer = ctxt.leaseObjectBuffer();
            Object[] values = buffer.resetAndStart();
            int ptr = 0;
            do {
                Object value = deserialize(jp, ctxt);
                if (ptr >= values.length) {
                    values = buffer.appendCompletedChunk(values);
                    ptr = 0;
                }
                values[ptr++] = value;
            } while (jp.nextToken() != JsonToken.END_ARRAY);
            return buffer.completeAndClearBuffer(values, ptr);
        }
    }
}
