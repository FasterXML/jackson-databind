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
            //return Vanilla.instance(preventMerge);
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
        case JsonTokenId.ID_START_ARRAY:
            return mapNonRecursive(p, ctxt, new RootLevel());
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
        case JsonTokenId.ID_START_ARRAY:
            return mapNonRecursive(p, ctxt, new RootLevel(intoValue));
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

    protected final Object mapNonRecursive(
            JsonParser p,
            DeserializationContext ctxt,
            RootLevel root
    ) throws IOException {
        Level level = root;
        while (level != null) {
            level = level.processSome(this, p, ctxt);
        }
        return root.value;
    }

    private abstract static class Level {
        private final Level parent;

        Level(Level parent) {
            this.parent = parent;
        }

        abstract boolean next(JsonParser p) throws IOException;

        abstract Object objectForReuse();

        abstract void add(UntypedObjectDeserializer deserializer, JsonParser p, DeserializationContext ctxt, Object o) throws IOException;

        abstract Object complete();

        Level processSome(UntypedObjectDeserializer deserializer, JsonParser p, DeserializationContext ctxt) throws IOException {
            while (next(p)) {
                JsonDeserializer<?> delegate;
                switch (p.currentTokenId()) {
                    case JsonTokenId.ID_START_OBJECT:
                    case JsonTokenId.ID_FIELD_NAME:
                    case JsonTokenId.ID_END_OBJECT:
                        if (deserializer._mapDeserializer == null) {
                            Object reuse = objectForReuse();
                            if (reuse instanceof Map<?,?>) {
                                //noinspection unchecked
                                return new MapLevel(this, (Map<String, Object>) reuse);
                            } else {
                                return new MapLevel(this);
                            }
                        }
                        delegate = deserializer._mapDeserializer;
                        break;
                    case JsonTokenId.ID_START_ARRAY:
                        if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
                            return new ArrayLevel(this);
                        } else {
                            if (deserializer._listDeserializer == null) {
                                Object reuse = objectForReuse();
                                if (reuse instanceof Collection<?>) {
                                    //noinspection unchecked
                                    return new ListLevel(this, (Collection<Object>) reuse);
                                } else {
                                    return new ListLevel(this);
                                }
                            }
                            delegate = deserializer._listDeserializer;
                            break;
                        }
                    default:
                        // this handles primitives
                        delegate = deserializer;
                }
                // either a scalar, in which case deserialize won't recurse, or a custom list/map deserializer
                add(deserializer, p, ctxt, delegate.deserialize(p, ctxt));
            }
            // finished structure
            if (parent != null) {
                parent.add(deserializer, p, ctxt, complete());
            }
            return parent;
        }
    }

    private static class RootLevel extends Level {
        final Object reuse;
        boolean done = false;
        Object value = null;

        RootLevel() {
            this(null);
        }

        RootLevel(Object reuse) {
            super(null);
            this.reuse = reuse;
        }

        @Override
        boolean next(JsonParser p) throws IOException {
            return !done;
        }

        @Override
        void add(UntypedObjectDeserializer deserializer, JsonParser p, DeserializationContext ctxt, Object o) throws IOException {
            done = true;
            value = o;
        }

        @Override
        Object objectForReuse() {
            return reuse;
        }

        @Override
        Object complete() {
            throw new UnsupportedOperationException();
        }
    }

    private static class MapLevel extends Level {
        final Map<String, Object> map;
        String nextFieldName;

        MapLevel(Level parent) {
            this(parent, new LinkedHashMap<>());
        }

        MapLevel(Level parent, Map<String, Object> map) {
            super(parent);
            this.map = map;
        }

        @Override
        boolean next(JsonParser p) throws IOException {
            nextFieldName = p.nextFieldName();
            if (nextFieldName == null) {
                return false;
            }
            p.nextToken();
            return true;
        }

        @Override
        void add(UntypedObjectDeserializer deserializer, JsonParser p, DeserializationContext ctxt, Object o) throws IOException {
            Object old = map.put(nextFieldName, o);
            if (old != null) {
                deserializer._handleDuplicateField(p, ctxt, nextFieldName, map, old, o);
            }
        }

        @Override
        Object objectForReuse() {
            return map.get(nextFieldName);
        }

        @Override
        Object complete() {
            return map;
        }
    }

    private static class ListLevel extends Level {
        final Collection<Object> list;

        ListLevel(Level parent) {
            this(parent, new ArrayList<>());
        }

        ListLevel(Level parent, Collection<Object> list) {
            super(parent);
            this.list = list;
        }

        @Override
        boolean next(JsonParser p) throws IOException {
            return p.nextToken() != JsonToken.END_ARRAY;
        }

        @Override
        void add(UntypedObjectDeserializer deserializer, JsonParser p, DeserializationContext ctxt, Object o) throws IOException {
            list.add(o);
        }

        @Override
        Object objectForReuse() {
            return null;
        }

        @Override
        Object complete() {
            return list;
        }
    }

    private static class ArrayLevel extends ListLevel {
        ArrayLevel(Level parent) {
            super(parent);
        }

        @Override
        Object complete() {
            return list.toArray();
        }
    }

    /**
     * Method called when there is a duplicate value for a field.
     * By default we don't care, and the last value is used.
     * Can be overridden to provide alternate handling, such as throwing
     * an exception, or choosing different strategy for combining values
     * or choosing which one to keep.
     *
     * @param fieldName Name of the field for which duplicate value was found
     * @param map Object node that contains values
     * @param oldValue Value that existed for the object node before newValue
     *   was added
     * @param newValue Newly added value just added to the object node
     */
    protected void _handleDuplicateField(JsonParser p, DeserializationContext ctxt,
                                         String fieldName, Map<String, Object> map,
                                         Object oldValue, Object newValue)
            throws IOException
    {
        /* FAIL_ON_READING_DUP_TREE_KEY only applies for tree nodes, we need a separate feature for untyped objects
        // [databind#237]: Report an error if asked to do so:
        if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)) {
            // 11-Sep-2019, tatu: Can not pass "property name" because we may be
            //    missing enclosing JSON content context...
// ctxt.reportPropertyInputMismatch(JsonNode.class, fieldName,
            ctxt.reportInputMismatch(JsonNode.class,
                    "Duplicate field '%s' for `ObjectNode`: not allowed when `DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY` enabled",
                    fieldName);
        }
        */
        // [databind#2732]: Special case for XML; automatically coerce into `ArrayNode`
        if (ctxt.isEnabled(StreamReadCapability.DUPLICATE_PROPERTIES)) {
            // Note that ideally we wouldn't have to shuffle things but... Map.putIfAbsent()
            // only added in JDK 8, to efficiently check for add. So...
            if (oldValue instanceof List<?>) { // already was array, to append
                //noinspection unchecked
                ((List<Object>) oldValue).add(newValue);
                map.replace(fieldName, oldValue);
            } else { // was not array, convert
                List<Object> arr = new ArrayList<>();
                arr.add(oldValue);
                arr.add(newValue);
                map.replace(fieldName, arr);
            }
        }
    }
}
