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
    public final static UntypedObjectDeserializer instance = new UntypedObjectDeserializer(null, null);

    /*
    /**********************************************************
    /* Possible custom deserializer overrides we need to use
    /**********************************************************
     */

    protected JsonDeserializer<Object> _mapDeserializer;

    protected JsonDeserializer<Object> _listDeserializer;

    protected JsonDeserializer<Object> _stringDeserializer;

    protected JsonDeserializer<Object> _numberDeserializer;

    /**
     * If {@link java.util.List} has been mapped to non-default implementation,
     * we'll store type here
     *
     * @since 2.6
     */
    protected JavaType _listType;

    /**
     * If {@link java.util.Map} has been mapped to non-default implementation,
     * we'll store type here
     *
     * @since 2.6
     */
    protected JavaType _mapType;

    /**
     * @deprecated Since 2.6 use variant takes type arguments
     */
    @Deprecated
    public UntypedObjectDeserializer() {
        this(null, null);
    }

    public UntypedObjectDeserializer(JavaType listType, JavaType mapType) {
        super(Object.class);
        _listType = listType;
        _mapType = mapType;
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
        _listType = base._listType;
        _mapType = base._mapType;
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
    @SuppressWarnings("unchecked")
    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException
    {
        JavaType obType = ctxt.constructType(Object.class);
        JavaType stringType = ctxt.constructType(String.class);
        TypeFactory tf = ctxt.getTypeFactory();

        /* 26-Nov-2014, tatu: This is highly unusual, as in general contextualization
         *    should always be called separately, from within "createContextual()".
         *    But this is a very singular deserializer since it operates on `Object`
         *    (and often for `?` type parameter), and as a result, easily and commonly
         *    results in cycles, being value deserializer for various Maps and Collections.
         *    Because of this, we must somehow break the cycles. This is done here by
         *    forcing pseudo-contextualization with null property.
         */

        // So: first find possible custom instances
        if (_listType == null) {
            _listDeserializer = _clearIfStdImpl(_findCustomDeser(ctxt, tf.constructCollectionType(List.class, obType)));
        } else {
            // NOTE: if non-default List type, always consider to be non-standard deser
            _listDeserializer = _findCustomDeser(ctxt, _listType);
        }
        if (_mapType == null) {
            _mapDeserializer = _clearIfStdImpl(_findCustomDeser(ctxt, tf.constructMapType(Map.class, stringType, obType)));
        } else {
            // NOTE: if non-default Map type, always consider to be non-standard deser
            _mapDeserializer = _findCustomDeser(ctxt, _mapType);
        }
        _stringDeserializer = _clearIfStdImpl(_findCustomDeser(ctxt, stringType));
        _numberDeserializer = _clearIfStdImpl(_findCustomDeser(ctxt, tf.constructType(Number.class)));

        // and then do bogus contextualization, in case custom ones need to resolve dependencies of
        // their own
        JavaType unknown = TypeFactory.unknownType();
        _mapDeserializer = (JsonDeserializer<Object>) ctxt.handleSecondaryContextualization(_mapDeserializer, null, unknown);
        _listDeserializer = (JsonDeserializer<Object>) ctxt.handleSecondaryContextualization(_listDeserializer, null, unknown);
        _stringDeserializer = (JsonDeserializer<Object>) ctxt.handleSecondaryContextualization(_stringDeserializer, null, unknown);
        _numberDeserializer = (JsonDeserializer<Object>) ctxt.handleSecondaryContextualization(_numberDeserializer, null, unknown);
    }

    protected JsonDeserializer<Object> _findCustomDeser(DeserializationContext ctxt, JavaType type)
        throws JsonMappingException
    {
        // Since we are calling from `resolve`, we should NOT try to contextualize yet;
        // contextualization will only occur at a later point
        return ctxt.findNonContextualValueDeserializer(type);
    }

    protected JsonDeserializer<Object> _clearIfStdImpl(JsonDeserializer<Object> deser) {
        return ClassUtil.isJacksonStdImpl(deser) ? null : deser;
    }

    /**
     * We only use contextualization for optimizing the case where no customization
     * occurred; if so, can slip in a more streamlined version.
     */
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

    /* 07-Nov-2014, tatu: When investigating [databind#604], realized that it makes
     *   sense to also mark this is cachable, since lookup not exactly free, and
     *   since it's not uncommon to "read anything"
     */
    @Override
    public boolean isCachable() {
        /* 26-Mar-2015, tatu: With respect to [databind#735], there are concerns over
         *   cachability. It seems like we SHOULD be safe here; but just in case there
         *   are problems with false sharing, this may need to be revisited.
         */
        return true;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        switch (p.getCurrentTokenId()) {
        case JsonTokenId.ID_START_OBJECT:
        case JsonTokenId.ID_FIELD_NAME:
            // 28-Oct-2015, tatu: [databind#989] We may also be given END_OBJECT (similar to FIELD_NAME),
            //    if caller has advanced to the first token of Object, but for empty Object
        case JsonTokenId.ID_END_OBJECT:
            if (_mapDeserializer != null) {
                return _mapDeserializer.deserialize(p, ctxt);
            }
            return mapObject(p, ctxt);
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
                return mapArrayToArray(p, ctxt);
            }
            if (_listDeserializer != null) {
                return _listDeserializer.deserialize(p, ctxt);
            }
            return mapArray(p, ctxt);
        case JsonTokenId.ID_EMBEDDED_OBJECT:
            return p.getEmbeddedObject();
        case JsonTokenId.ID_STRING:
            if (_stringDeserializer != null) {
                return _stringDeserializer.deserialize(p, ctxt);
            }
            return p.getText();

        case JsonTokenId.ID_NUMBER_INT:
            if (_numberDeserializer != null) {
                return _numberDeserializer.deserialize(p, ctxt);
            }
            /* Caller may want to get all integral values returned as {@link java.math.BigInteger},
             * or {@link java.lang.Long} for consistency
             */
            if (ctxt.hasSomeOfFeatures(F_MASK_INT_COERCIONS)) {
                return _coerceIntegral(p, ctxt);
            }
            return p.getNumberValue(); // should be optimal, whatever it is

        case JsonTokenId.ID_NUMBER_FLOAT:
            if (_numberDeserializer != null) {
                return _numberDeserializer.deserialize(p, ctxt);
            }
            /* [JACKSON-72]: need to allow overriding the behavior regarding
             *   which type to use
             */
            if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                return p.getDecimalValue();
            }
            return p.getDoubleValue();

        case JsonTokenId.ID_TRUE:
            return Boolean.TRUE;
        case JsonTokenId.ID_FALSE:
            return Boolean.FALSE;

        case JsonTokenId.ID_NULL: // should not get this but...
            return null;

//        case JsonTokenId.ID_END_ARRAY: // invalid
        default:
        }
        throw ctxt.mappingException(Object.class);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException
    {
        switch (p.getCurrentTokenId()) {
        // First: does it look like we had type id wrapping of some kind?
        case JsonTokenId.ID_START_ARRAY:
        case JsonTokenId.ID_START_OBJECT:
        case JsonTokenId.ID_FIELD_NAME:
            /* Output can be as JSON Object, Array or scalar: no way to know
             * a this point:
             */
            return typeDeserializer.deserializeTypedFromAny(p, ctxt);

        case JsonTokenId.ID_EMBEDDED_OBJECT:
            return p.getEmbeddedObject();
            
        /* Otherwise we probably got a "native" type (ones that map
         * naturally and thus do not need or use type ids)
         */
        case JsonTokenId.ID_STRING:
            if (_stringDeserializer != null) {
                return _stringDeserializer.deserialize(p, ctxt);
            }
            return p.getText();

        case JsonTokenId.ID_NUMBER_INT:
            if (_numberDeserializer != null) {
                return _numberDeserializer.deserialize(p, ctxt);
            }
            // May need coercion to "bigger" types:
            if (ctxt.hasSomeOfFeatures(F_MASK_INT_COERCIONS)) {
                return _coerceIntegral(p, ctxt);
            }
            return p.getNumberValue(); // should be optimal, whatever it is

        case JsonTokenId.ID_NUMBER_FLOAT:
            if (_numberDeserializer != null) {
                return _numberDeserializer.deserialize(p, ctxt);
            }
            if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                return p.getDecimalValue();
            }
            return Double.valueOf(p.getDoubleValue());

        case JsonTokenId.ID_TRUE:
            return Boolean.TRUE;
        case JsonTokenId.ID_FALSE:
            return Boolean.FALSE;

        case JsonTokenId.ID_NULL: // should not get this far really but...
            return null;
        default:
        }
        throw ctxt.mappingException(Object.class);
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
    protected Object mapObject(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        String key1;

        JsonToken t = p.getCurrentToken();
        
        if (t == JsonToken.START_OBJECT) {
            key1 = p.nextFieldName();
        } else if (t == JsonToken.FIELD_NAME) {
            key1 = p.getCurrentName();
        } else {
            if (t != JsonToken.END_OBJECT) {
                throw ctxt.mappingException(handledType(), p.getCurrentToken());
            }
            key1 = null;
        }
        if (key1 == null) {
            // empty map might work; but caller may want to modify... so better just give small modifiable
            return new LinkedHashMap<String,Object>(2);
        }
        // minor optimization; let's handle 1 and 2 entry cases separately
        // 24-Mar-2015, tatu: Ideally, could use one of 'nextXxx()' methods, but for
        //   that we'd need new method(s) in JsonDeserializer. So not quite yet.
        p.nextToken();
        Object value1 = deserialize(p, ctxt);

        String key2 = p.nextFieldName();
        if (key2 == null) { // has to be END_OBJECT, then
            // single entry; but we want modifiable
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(2);
            result.put(key1, value1);
            return result;
        }
        p.nextToken();
        Object value2 = deserialize(p, ctxt);

        String key = p.nextFieldName();

        if (key == null) {
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(4);
            result.put(key1, value1);
            result.put(key2, value2);
            return result;
        }
        // And then the general case; default map size is 16
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put(key1, value1);
        result.put(key2, value2);

        do {
            p.nextToken();
            result.put(key, deserialize(p, ctxt));
        } while ((key = p.nextFieldName()) != null);
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
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            switch (p.getCurrentTokenId()) {
            case JsonTokenId.ID_START_OBJECT:
                {
                    JsonToken t = p.nextToken();
                    if (t == JsonToken.END_OBJECT) {
                        return new LinkedHashMap<String,Object>(2);
                    }
                }
            case JsonTokenId.ID_FIELD_NAME:
                return mapObject(p, ctxt);
            case JsonTokenId.ID_START_ARRAY:
                {
                    JsonToken t = p.nextToken();
                    if (t == JsonToken.END_ARRAY) { // and empty one too
                        if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
                            return NO_OBJECTS;
                        }
                        return new ArrayList<Object>(2);
                    }
                }
                if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
                    return mapArrayToArray(p, ctxt);
                }
                return mapArray(p, ctxt);
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                return p.getEmbeddedObject();
            case JsonTokenId.ID_STRING:
                return p.getText();

            case JsonTokenId.ID_NUMBER_INT:
                if (ctxt.hasSomeOfFeatures(F_MASK_INT_COERCIONS)) {
                    return _coerceIntegral(p, ctxt);
                }
                return p.getNumberValue(); // should be optimal, whatever it is

            case JsonTokenId.ID_NUMBER_FLOAT:
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                    return p.getDecimalValue();
                }
                return Double.valueOf(p.getDoubleValue());

            case JsonTokenId.ID_TRUE:
                return Boolean.TRUE;
            case JsonTokenId.ID_FALSE:
                return Boolean.FALSE;

            case JsonTokenId.ID_NULL: // should not get this but...
                return null;

            case JsonTokenId.ID_END_OBJECT:
                // 28-Oct-2015, tatu: [databind#989] We may also be given END_OBJECT (similar to FIELD_NAME),
                //    if caller has advanced to the first token of Object, but for empty Object
                return new LinkedHashMap<String,Object>(2);

            //case JsonTokenId.ID_END_ARRAY: // invalid
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
        protected Object mapObject(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            // will point to FIELD_NAME at this point, guaranteed
            String key1 = p.getText();
            p.nextToken();
            Object value1 = deserialize(p, ctxt);

            String key2 = p.nextFieldName();
            if (key2 == null) { // single entry; but we want modifiable
                LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(2);
                result.put(key1, value1);
                return result;
            }
            p.nextToken();
            Object value2 = deserialize(p, ctxt);

            String key = p.nextFieldName();
            if (key == null) {
                LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(4);
                result.put(key1, value1);
                result.put(key2, value2);
                return result;
            }
            // And then the general case; default map size is 16
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
            result.put(key1, value1);
            result.put(key2, value2);
            do {
                p.nextToken();
                result.put(key, deserialize(p, ctxt));
            } while ((key = p.nextFieldName()) != null);
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
