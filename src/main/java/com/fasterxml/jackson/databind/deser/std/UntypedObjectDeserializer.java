package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
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
     * @since 2.9
     */
    protected final boolean _nonMerging;

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
        _nonMerging = false;
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
        _nonMerging = base._nonMerging;
    }

    /**
     * @since 2.9
     */
    protected UntypedObjectDeserializer(UntypedObjectDeserializer base,
            boolean nonMerging)
    {
        super(Object.class);
        _mapDeserializer = base._mapDeserializer;
        _listDeserializer = base._listDeserializer;
        _stringDeserializer = base._stringDeserializer;
        _numberDeserializer = base._numberDeserializer;
        _listType = base._listType;
        _mapType = base._mapType;
        _nonMerging = nonMerging;
    }

    /*
    /**********************************************************
    /* Initialization
    /**********************************************************
     */

    /**
     * We need to implement this method to properly find things to delegate
     * to: it cannot be done earlier since delegated deserializers almost
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
        // 14-Jun-2017, tatu: [databind#1625]: may want to block merging, for root value
        boolean preventMerge = (property == null)
                && Boolean.FALSE.equals(ctxt.getConfig().getDefaultMergeable(Object.class));
        // 20-Apr-2014, tatu: If nothing custom, let's use "vanilla" instance,
        //     simpler and can avoid some of delegation
        if ((_stringDeserializer == null) && (_numberDeserializer == null)
                && (_mapDeserializer == null) && (_listDeserializer == null)
                &&  getClass() == UntypedObjectDeserializer.class) {
            return UntypedObjectDeserializerNR.instance(preventMerge);
        }

        if (preventMerge != _nonMerging) {
            return new UntypedObjectDeserializer(this, preventMerge);
        }

        return this;
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
        // 26-Mar-2015, tatu: With respect to [databind#735], there are concerns over
        //   cachability. It seems like we SHOULD be safe here; but just in case there
        //   are problems with false sharing, this may need to be revisited.
        return true;
    }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Untyped;
    }

    @Override // since 2.9
    public Boolean supportsUpdate(DeserializationConfig config) {
        // 21-Apr-2017, tatu: Bit tricky... some values, yes. So let's say "dunno"
        return null;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        switch (p.currentTokenId()) {
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
            // Caller may want to get all integral values returned as {@link java.math.BigInteger},
            // or {@link java.lang.Long} for consistency
            if (ctxt.hasSomeOfFeatures(F_MASK_INT_COERCIONS)) {
                return _coerceIntegral(p, ctxt);
            }
            return p.getNumberValue(); // should be optimal, whatever it is

        case JsonTokenId.ID_NUMBER_FLOAT:
            if (_numberDeserializer != null) {
                return _numberDeserializer.deserialize(p, ctxt);
            }
            // Need to allow overriding the behavior regarding which type to use
            if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                return p.getDecimalValue();
            }
            // as per [databind#1453] should not assume Double but:
            return p.getNumberValue();

        case JsonTokenId.ID_TRUE:
            return Boolean.TRUE;
        case JsonTokenId.ID_FALSE:
            return Boolean.FALSE;

        case JsonTokenId.ID_NULL: // 08-Nov-2016, tatu: yes, occurs
            return null;

//        case JsonTokenId.ID_END_ARRAY: // invalid
        default:
        }
        return ctxt.handleUnexpectedToken(Object.class, p);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException
    {
        switch (p.currentTokenId()) {
        // First: does it look like we had type id wrapping of some kind?
        case JsonTokenId.ID_START_ARRAY:
        case JsonTokenId.ID_START_OBJECT:
        case JsonTokenId.ID_FIELD_NAME:
            // Output can be as JSON Object, Array or scalar: no way to know at this point:
            return typeDeserializer.deserializeTypedFromAny(p, ctxt);

        case JsonTokenId.ID_EMBEDDED_OBJECT:
            return p.getEmbeddedObject();

        // Otherwise we probably got a "native" type (ones that map
        // naturally and thus do not need or use type ids)
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
            return p.getNumberValue();

        case JsonTokenId.ID_TRUE:
            return Boolean.TRUE;
        case JsonTokenId.ID_FALSE:
            return Boolean.FALSE;

        case JsonTokenId.ID_NULL: // should not get this far really but...
            return null;
        default:
        }
        return ctxt.handleUnexpectedToken(Object.class, p);
    }

    @SuppressWarnings("unchecked")
    @Override // since 2.9 (to support deep merge)
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object intoValue)
        throws IOException
    {
        if (_nonMerging) {
            return deserialize(p, ctxt);
        }

        switch (p.currentTokenId()) {
        case JsonTokenId.ID_START_OBJECT:
        case JsonTokenId.ID_FIELD_NAME:
            // We may also be given END_OBJECT (similar to FIELD_NAME),
            // if caller has advanced to the first token of Object, but for empty Object
        case JsonTokenId.ID_END_OBJECT:
            if (_mapDeserializer != null) {
                return _mapDeserializer.deserialize(p, ctxt, intoValue);
            }
            if (intoValue instanceof Map<?,?>) {
                return mapObject(p, ctxt, (Map<Object,Object>) intoValue);
            }
            return mapObject(p, ctxt);
        case JsonTokenId.ID_START_ARRAY:
            if (_listDeserializer != null) {
                return _listDeserializer.deserialize(p, ctxt, intoValue);
            }
            if (intoValue instanceof Collection<?>) {
                return mapArray(p, ctxt, (Collection<Object>) intoValue);
            }
            if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
                return mapArrayToArray(p, ctxt);
            }
            return mapArray(p, ctxt);
        case JsonTokenId.ID_EMBEDDED_OBJECT:
            return p.getEmbeddedObject();
        case JsonTokenId.ID_STRING:
            if (_stringDeserializer != null) {
                return _stringDeserializer.deserialize(p, ctxt, intoValue);
            }
            return p.getText();

        case JsonTokenId.ID_NUMBER_INT:
            if (_numberDeserializer != null) {
                return _numberDeserializer.deserialize(p, ctxt, intoValue);
            }
            if (ctxt.hasSomeOfFeatures(F_MASK_INT_COERCIONS)) {
                return _coerceIntegral(p, ctxt);
            }
            return p.getNumberValue();

        case JsonTokenId.ID_NUMBER_FLOAT:
            if (_numberDeserializer != null) {
                return _numberDeserializer.deserialize(p, ctxt, intoValue);
            }
            if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                return p.getDecimalValue();
            }
            return p.getNumberValue();
        case JsonTokenId.ID_TRUE:
            return Boolean.TRUE;
        case JsonTokenId.ID_FALSE:
            return Boolean.FALSE;

        case JsonTokenId.ID_NULL:
            // 21-Apr-2017, tatu: May need to consider "skip nulls" at some point but...
            return null;
        default:
        }
        // easiest to just delegate to "dumb" version for the rest?
        return deserialize(p, ctxt);
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    /**
     * Method called to map a JSON Array into a Java value.
     */
    protected Object mapArray(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // Minor optimization to handle small lists (default size for ArrayList is 10)
        if (p.nextToken()  == JsonToken.END_ARRAY) {
            return new ArrayList<Object>(2);
        }
        Object value = deserialize(p, ctxt);
        if (p.nextToken()  == JsonToken.END_ARRAY) {
            ArrayList<Object> l = new ArrayList<Object>(2);
            l.add(value);
            return l;
        }
        Object value2 = deserialize(p, ctxt);
        if (p.nextToken()  == JsonToken.END_ARRAY) {
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
            value = deserialize(p, ctxt);
            ++totalSize;
            if (ptr >= values.length) {
                values = buffer.appendCompletedChunk(values);
                ptr = 0;
            }
            values[ptr++] = value;
        } while (p.nextToken() != JsonToken.END_ARRAY);
        // let's create full array then
        ArrayList<Object> result = new ArrayList<Object>(totalSize);
        buffer.completeAndClearBuffer(values, ptr, result);
        ctxt.returnObjectBuffer(buffer);
        return result;
    }

    protected Object mapArray(JsonParser p, DeserializationContext ctxt,
            Collection<Object> result) throws IOException
    {
        // we start by pointing to START_ARRAY. Also, no real merging; array/Collection
        // just appends always
        while (p.nextToken() != JsonToken.END_ARRAY) {
            result.add(deserialize(p, ctxt));
        }
        return result;
    }

    /**
     * Method called to map a JSON Object into a Java value.
     */
    protected Object mapObject(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        String key1;
        JsonToken t = p.currentToken();

        if (t == JsonToken.START_OBJECT) {
            key1 = p.nextFieldName();
        } else if (t == JsonToken.FIELD_NAME) {
            key1 = p.currentName();
        } else {
            if (t != JsonToken.END_OBJECT) {
                return ctxt.handleUnexpectedToken(handledType(), p);
            }
            key1 = null;
        }
        if (key1 == null) {
            // empty map might work; but caller may want to modify... so better just give small modifiable
            return new LinkedHashMap<>(2);
        }
        // minor optimization; let's handle 1 and 2 entry cases separately
        // 24-Mar-2015, tatu: Ideally, could use one of 'nextXxx()' methods, but for
        //   that we'd need new method(s) in JsonDeserializer. So not quite yet.
        p.nextToken();
        Object value1 = deserialize(p, ctxt);
        String key2 = p.nextFieldName();
        if (key2 == null) { // has to be END_OBJECT, then
            // single entry; but we want modifiable
            LinkedHashMap<String, Object> result = new LinkedHashMap<>(2);
            result.put(key1, value1);
            return result;
        }
        p.nextToken();
        Object value2 = deserialize(p, ctxt);

        String key = p.nextFieldName();
        if (key == null) {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>(4);
            result.put(key1, value1);
            if (result.put(key2, value2) != null) {
                // 22-May-2020, tatu: [databind#2733] may need extra handling
                return _mapObjectWithDups(p, ctxt, result, key1, value1, value2, key);
            }
            return result;
        }
        // And then the general case; default map size is 16
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put(key1, value1);
        if (result.put(key2, value2) != null) {
            // 22-May-2020, tatu: [databind#2733] may need extra handling
            return _mapObjectWithDups(p, ctxt, result, key1, value1, value2, key);
        }

        do {
            p.nextToken();
            final Object newValue = deserialize(p, ctxt);
            final Object oldValue = result.put(key, newValue);
            if (oldValue != null) {
                return _mapObjectWithDups(p, ctxt, result, key, oldValue, newValue,
                        p.nextFieldName());
            }
        } while ((key = p.nextFieldName()) != null);
        return result;
    }

    // @since 2.12 (wrt [databind#2733]
    protected Object _mapObjectWithDups(JsonParser p, DeserializationContext ctxt,
            final Map<String, Object> result, String key,
            Object oldValue, Object newValue, String nextKey) throws IOException
    {
        final boolean squashDups = ctxt.isEnabled(StreamReadCapability.DUPLICATE_PROPERTIES);

        if (squashDups) {
            _squashDups(result, key, oldValue, newValue);
        }

        while (nextKey != null) {
            p.nextToken();
            newValue = deserialize(p, ctxt);
            oldValue = result.put(nextKey, newValue);
            if ((oldValue != null) && squashDups) {
                _squashDups(result, key, oldValue, newValue);
            }
            nextKey = p.nextFieldName();
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private void _squashDups(final Map<String, Object> result, String key,
            Object oldValue, Object newValue)
    {
        if (oldValue instanceof List<?>) {
            ((List<Object>) oldValue).add(newValue);
            result.put(key, oldValue);
        } else {
            ArrayList<Object> l = new ArrayList<>();
            l.add(oldValue);
            l.add(newValue);
            result.put(key, l);
        }
    }

    /**
     * Method called to map a JSON Array into a Java Object array (Object[]).
     */
    protected Object[] mapArrayToArray(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // Minor optimization to handle small lists (default size for ArrayList is 10)
        if (p.nextToken()  == JsonToken.END_ARRAY) {
            return NO_OBJECTS;
        }
        ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] values = buffer.resetAndStart();
        int ptr = 0;
        do {
            Object value = deserialize(p, ctxt);
            if (ptr >= values.length) {
                values = buffer.appendCompletedChunk(values);
                ptr = 0;
            }
            values[ptr++] = value;
        } while (p.nextToken() != JsonToken.END_ARRAY);
        final Object[] result = buffer.completeAndClearBuffer(values, ptr);
        ctxt.returnObjectBuffer(buffer);
        return result;
    }

    protected Object mapObject(JsonParser p, DeserializationContext ctxt,
            Map<Object,Object> m) throws IOException
    {
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) {
            t = p.nextToken();
        }
        if (t == JsonToken.END_OBJECT) {
            return m;
        }
        // NOTE: we are guaranteed to point to FIELD_NAME
        String key = p.currentName();
        do {
            p.nextToken();
            // and possibly recursive merge here
            Object old = m.get(key);
            Object newV;

            if (old != null) {
                newV = deserialize(p, ctxt, old);
            } else {
                newV = deserialize(p, ctxt);
            }
            if (newV != old) {
                m.put(key, newV);
            }
        } while ((key = p.nextFieldName()) != null);
        return m;
    }

    /*
    /**********************************************************************
    /* Separate "vanilla" implementation for common case of no deser overrides
    /**********************************************************************
     */

    /**
     * Streamlined version of {@link UntypedObjectDeserializer} that has fewer checks and
     * is only used when no custom deserializer overrides are applied.
     */
    @JacksonStdImpl
    @Deprecated // since 2.14, to be removed in near future
    public static class Vanilla
        extends StdDeserializer<Object>
    {
        private static final long serialVersionUID = 1L;

        public final static Vanilla std = new Vanilla();

        // @since 2.9
        protected final boolean _nonMerging;

        public Vanilla() { this(false); }

        protected Vanilla(boolean nonMerging) {
            super(Object.class);
            _nonMerging = nonMerging;
        }

        public static Vanilla instance(boolean nonMerging) {
            if (nonMerging) {
                return new Vanilla(true);
            }
            return std;
        }

        @Override // since 2.12
        public LogicalType logicalType() {
            return LogicalType.Untyped;
        }

        @Override // since 2.9
        public Boolean supportsUpdate(DeserializationConfig config) {
            // 21-Apr-2017, tatu: Bit tricky... some values, yes. So let's say "dunno"
            // 14-Jun-2017, tatu: Well, if merging blocked, can say no, as well.
            return _nonMerging ? Boolean.FALSE : null;
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            switch (p.currentTokenId()) {
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
                return p.getNumberValue();

            case JsonTokenId.ID_TRUE:
                return Boolean.TRUE;
            case JsonTokenId.ID_FALSE:
                return Boolean.FALSE;

            case JsonTokenId.ID_END_OBJECT:
                // 28-Oct-2015, tatu: [databind#989] We may also be given END_OBJECT (similar to FIELD_NAME),
                //    if caller has advanced to the first token of Object, but for empty Object
                return new LinkedHashMap<String,Object>(2);

            case JsonTokenId.ID_NULL: // 08-Nov-2016, tatu: yes, occurs
                return null;

            //case JsonTokenId.ID_END_ARRAY: // invalid
            default:
            }
            return ctxt.handleUnexpectedToken(Object.class, p);
        }

        @Override
        public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException
        {
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_START_ARRAY:
            case JsonTokenId.ID_START_OBJECT:
            case JsonTokenId.ID_FIELD_NAME:
                return typeDeserializer.deserializeTypedFromAny(p, ctxt);

            case JsonTokenId.ID_STRING:
                return p.getText();

            case JsonTokenId.ID_NUMBER_INT:
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
                    return p.getBigIntegerValue();
                }
                return p.getNumberValue();

            case JsonTokenId.ID_NUMBER_FLOAT:
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                    return p.getDecimalValue();
                }
                return p.getNumberValue();

            case JsonTokenId.ID_TRUE:
                return Boolean.TRUE;
            case JsonTokenId.ID_FALSE:
                return Boolean.FALSE;
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                return p.getEmbeddedObject();

            case JsonTokenId.ID_NULL: // should not get this far really but...
                return null;
            default:
            }
            return ctxt.handleUnexpectedToken(Object.class, p);
        }

        @SuppressWarnings("unchecked")
        @Override // since 2.9 (to support deep merge)
        public Object deserialize(JsonParser p, DeserializationContext ctxt, Object intoValue)
            throws IOException
        {
            if (_nonMerging) {
                return deserialize(p, ctxt);
            }
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_END_OBJECT:
            case JsonTokenId.ID_END_ARRAY:
                return intoValue;
            case JsonTokenId.ID_START_OBJECT:
                {
                    JsonToken t = p.nextToken(); // to get to FIELD_NAME or END_OBJECT
                    if (t == JsonToken.END_OBJECT) {
                        return intoValue;
                    }
                }
            case JsonTokenId.ID_FIELD_NAME:
                if (intoValue instanceof Map<?,?>) {
                    Map<Object,Object> m = (Map<Object,Object>) intoValue;
                    // NOTE: we are guaranteed to point to FIELD_NAME
                    String key = p.currentName();
                    do {
                        p.nextToken();
                        // and possibly recursive merge here
                        Object old = m.get(key);
                        Object newV;
                        if (old != null) {
                            newV = deserialize(p, ctxt, old);
                        } else {
                            newV = deserialize(p, ctxt);
                        }
                        if (newV != old) {
                            m.put(key, newV);
                        }
                    } while ((key = p.nextFieldName()) != null);
                    return intoValue;
                }
                break;
            case JsonTokenId.ID_START_ARRAY:
                {
                    JsonToken t = p.nextToken(); // to get to FIELD_NAME or END_OBJECT
                    if (t == JsonToken.END_ARRAY) {
                        return intoValue;
                    }
                }

                if (intoValue instanceof Collection<?>) {
                    Collection<Object> c = (Collection<Object>) intoValue;
                    // NOTE: merge for arrays/Collections means append, can't merge contents
                    do {
                        c.add(deserialize(p, ctxt));
                    } while (p.nextToken() != JsonToken.END_ARRAY);
                    return intoValue;
                }
                // 21-Apr-2017, tatu: Should we try to support merging of Object[] values too?
                //    ... maybe future improvement
                break;
            }
            // Easiest handling for the rest, delegate. Only (?) question: how about nulls?
            return deserialize(p, ctxt);
        }

        protected Object mapArray(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            Object value = deserialize(p, ctxt);
            if (p.nextToken()  == JsonToken.END_ARRAY) {
                ArrayList<Object> l = new ArrayList<Object>(2);
                l.add(value);
                return l;
            }
            ObjectBuffer buffer = ctxt.leaseObjectBuffer();
            Object[] values = buffer.resetAndStart();
            int ptr = 0;
            values[ptr++] = value;
            int totalSize = ptr;
            do {
                value = deserialize(p, ctxt);
                ++totalSize;
                if (ptr >= values.length) {
                    values = buffer.appendCompletedChunk(values);
                    ptr = 0;
                }
                values[ptr++] = value;
            } while (p.nextToken() != JsonToken.END_ARRAY);
            // let's create full array then
            ArrayList<Object> result = new ArrayList<Object>(totalSize);
            buffer.completeAndClearBuffer(values, ptr, result);
            ctxt.returnObjectBuffer(buffer);
            return result;
        }

        protected Object[] mapArrayToArray(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectBuffer buffer = ctxt.leaseObjectBuffer();
            Object[] values = buffer.resetAndStart();
            int ptr = 0;
            do {
                Object value = deserialize(p, ctxt);
                if (ptr >= values.length) {
                    values = buffer.appendCompletedChunk(values);
                    ptr = 0;
                }
                values[ptr++] = value;
            } while (p.nextToken() != JsonToken.END_ARRAY);
            Object[] result = buffer.completeAndClearBuffer(values, ptr);
            ctxt.returnObjectBuffer(buffer);
            return result;
        }

        protected Object mapObject(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            // will point to FIELD_NAME at this point, guaranteed
            // 19-Jul-2021, tatu: Was incorrectly using "getText()" before 2.13, fixed for 2.13.0
            String key1 = p.currentName();
            p.nextToken();
            Object value1 = deserialize(p, ctxt);

            String key = p.nextFieldName();
            if (key == null) { // single entry; but we want modifiable
                LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(2);
                result.put(key1, value1);
                return result;
            }
            // And then the general case; default map size is 16
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
            result.put(key1, value1);
            do {
                p.nextToken();
                final Object newValue = deserialize(p, ctxt);
                final Object oldValue = result.put(key, newValue);
                if (oldValue != null) {
                    return _mapObjectWithDups(p, ctxt, result, key, oldValue, newValue,
                            p.nextFieldName());
                }
            } while ((key = p.nextFieldName()) != null);
            return result;
        }

        // NOTE: copied from above (alas, no easy way to share/reuse)
        // @since 2.12 (wrt [databind#2733]
        protected Object _mapObjectWithDups(JsonParser p, DeserializationContext ctxt,
                final Map<String, Object> result, String initialKey,
                Object oldValue, Object newValue, String nextKey) throws IOException
        {
            final boolean squashDups = ctxt.isEnabled(StreamReadCapability.DUPLICATE_PROPERTIES);

            if (squashDups) {
                _squashDups(result, initialKey, oldValue, newValue);
            }

            while (nextKey != null) {
                p.nextToken();
                newValue = deserialize(p, ctxt);
                oldValue = result.put(nextKey, newValue);
                if ((oldValue != null) && squashDups) {
                    _squashDups(result, nextKey, oldValue, newValue);
                }
                nextKey = p.nextFieldName();
            }

            return result;
        }

        // NOTE: copied from above (alas, no easy way to share/reuse)
        @SuppressWarnings("unchecked")
        private void _squashDups(final Map<String, Object> result, String key,
                Object oldValue, Object newValue)
        {
            if (oldValue instanceof List<?>) {
                ((List<Object>) oldValue).add(newValue);
                result.put(key, oldValue);
            } else {
                ArrayList<Object> l = new ArrayList<>();
                l.add(oldValue);
                l.add(newValue);
                result.put(key, l);
            }
        }
    }
}
