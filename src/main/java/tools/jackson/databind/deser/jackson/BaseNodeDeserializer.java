package tools.jackson.databind.deser.jackson;

import java.math.BigDecimal;
import java.util.Arrays;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ContainerNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.util.RawValue;

/**
 * Base class for all actual {@link JsonNode} deserializer implementations.
 * Uses iteration instead of recursion: this allows
 * handling of very deeply nested input structures.
 *<p>
 * This class should only be extended by internal Jackson deserializers.
 * It is not intended to be used by custom deserializers.
 */
public abstract class BaseNodeDeserializer<T extends JsonNode>
        extends StdDeserializer<T>
{
    protected final Boolean _supportsUpdates;

    protected final boolean _mergeArrays;
    protected final boolean _mergeObjects;

    public BaseNodeDeserializer(Class<T> vc, Boolean supportsUpdates) {
        super(vc);
        _supportsUpdates = supportsUpdates;
        _mergeArrays = true;
        _mergeObjects = true;
    }

    protected BaseNodeDeserializer(BaseNodeDeserializer<?> base,
            boolean mergeArrays, boolean mergeObjects)
    {
        super(base);
        _supportsUpdates = base._supportsUpdates;
        _mergeArrays = mergeArrays;
        _mergeObjects = mergeObjects;
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws JacksonException
    {
        // Output can be as JSON Object, Array or scalar: no way to know a priori:
        return typeDeserializer.deserializeTypedFromAny(p, ctxt);
    }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Untyped;
    }

    // 07-Nov-2014, tatu: When investigating [databind#604], realized that it makes
    //   sense to also mark this is cachable, since lookup not exactly free, and
    //   since it's not uncommon to "read anything"
    @Override
    public boolean isCachable() { return true; }

    @Override
    public Boolean supportsUpdate(DeserializationConfig config) {
        return _supportsUpdates;
    }

    @Override // @since 2.14
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        // 13-Jun-2022, tatu: Should we care about property? For now, let's not yet.
        //   (merge info there accessible via "property.getMetadata().getMergeInfo()")
        final DeserializationConfig cfg = ctxt.getConfig();
        Boolean mergeArr = cfg.getDefaultMergeable(ArrayNode.class);
        Boolean mergeObj = cfg.getDefaultMergeable(ObjectNode.class);
        Boolean mergeNode = cfg.getDefaultMergeable(JsonNode.class);

        final boolean mergeArrays = _shouldMerge(mergeArr, mergeNode);
        final boolean mergeObjects = _shouldMerge(mergeObj, mergeNode);

        if ((mergeArrays != _mergeArrays)
                || (mergeObjects != _mergeObjects)) {
            return _createWithMerge(mergeArrays, mergeObjects);
        }

        return this;
    }

    private static boolean _shouldMerge(Boolean specificMerge, Boolean generalMerge) {
        if (specificMerge != null) {
            return specificMerge.booleanValue();
        }
        if (generalMerge != null) {
            return generalMerge.booleanValue();
        }
        return true;
    }

    // @since 2.14
    protected abstract BaseNodeDeserializer<?> _createWithMerge(boolean mergeArrays,
                                                                boolean mergeObjects);

    /*
    /**********************************************************************
    /* Duplicate handling
    /**********************************************************************
     */

    /**
     * Method called when there is a duplicate value for an Object property.
     * By default we don't care, and the last value is used.
     * Can be overridden to provide alternate handling, such as throwing
     * an exception, or choosing different strategy for combining values
     * or choosing which one to keep.
     *
     * @param propName Name of the property for which duplicate value was found
     * @param objectNode Object node that contains values
     * @param oldValue Value that existed for the object node before newValue
     *   was added
     * @param newValue Newly added value just added to the object node
     */
    protected void _handleDuplicateProperty(JsonParser p, DeserializationContext ctxt,
                                            JsonNodeFactory nodeFactory,
                                            String propName, ObjectNode objectNode,
                                            JsonNode oldValue, JsonNode newValue)
            throws JacksonException
    {
        // [databind#237]: Report an error if asked to do so:
        if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)) {
            // 11-Sep-2019, tatu: Can not pass "property name" because we may be
            //    missing enclosing JSON content context...
// ctxt.reportPropertyInputMismatch(JsonNode.class, propName,
            ctxt.reportInputMismatch(JsonNode.class,
                    "Duplicate property \"%s\" for `ObjectNode`: not allowed when `DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY` enabled",
                    propName);
        }
        // [databind#2732]: Special case for XML; automatically coerce into `ArrayNode`
        if (ctxt.isEnabled(StreamReadCapability.DUPLICATE_PROPERTIES)) {
            // Note that ideally we wouldn't have to shuffle things but... Map.putIfAbsent()
            // only added in JDK 8, to efficiently check for add. So...
            if (oldValue.isArray()) { // already was array, to append
                ((ArrayNode) oldValue).add(newValue);
                objectNode.replace(propName, oldValue);
            } else { // was not array, convert
                ArrayNode arr = nodeFactory.arrayNode();
                arr.add(oldValue);
                arr.add(newValue);
                objectNode.replace(propName, arr);
            }
        }
    }

    /*
    /**********************************************************************
    /* Helper methods, deserialization
    /**********************************************************************
     */

    /**
     * Alternate deserialization method used when parser already points to first
     * PROPERTY_NAME and not START_OBJECT.
     */
    protected final ObjectNode _deserializeObjectAtName(JsonParser p, DeserializationContext ctxt,
                                                        final JsonNodeFactory nodeFactory, final ContainerStack stack)
            throws JacksonException
    {
        final ObjectNode node = nodeFactory.objectNode();
        String key = p.currentName();
        for (; key != null; key = p.nextName()) {
            JsonNode value;
            JsonToken t = p.nextToken();
            if (t == null) { // can this ever occur?
                t = JsonToken.NOT_AVAILABLE; // can this ever occur?
            }
            switch (t.id()) {
                case JsonTokenId.ID_START_OBJECT:
                    value = _deserializeContainerNoRecursion(p, ctxt, nodeFactory,
                            stack, nodeFactory.objectNode());
                    break;
                case JsonTokenId.ID_START_ARRAY:
                    value = _deserializeContainerNoRecursion(p, ctxt, nodeFactory,
                            stack, nodeFactory.arrayNode());
                    break;
                default:
                    value = _deserializeAnyScalar(p, ctxt);
            }
            JsonNode old = node.replace(key, value);
            if (old != null) {
                _handleDuplicateProperty(p, ctxt, nodeFactory,
                        key, node, old, value);
            }
        }
        return node;
    }

    /**
     * Alternate deserialization method that is to update existing {@link ObjectNode}
     * if possible.
     */
    protected final JsonNode updateObject(JsonParser p, DeserializationContext ctxt,
                                          final ObjectNode node, final ContainerStack stack)
            throws JacksonException
    {
        String key;
        if (p.isExpectedStartObjectToken()) {
            key = p.nextName();
        } else {
            if (!p.hasToken(JsonToken.PROPERTY_NAME)) {
                return deserialize(p, ctxt);
            }
            key = p.currentName();
        }
        final JsonNodeFactory nodeFactory = ctxt.getNodeFactory();
        for (; key != null; key = p.nextName()) {
            // If not, fall through to regular handling
            JsonToken t = p.nextToken();

            // First: see if we can merge things:
            JsonNode old = node.get(key);
            if (old != null) {
                if (old instanceof ObjectNode) {
                    // [databind#3056]: merging only if had Object and
                    // getting an Object
                    if ((t == JsonToken.START_OBJECT) && _mergeObjects) {
                        JsonNode newValue = updateObject(p, ctxt, (ObjectNode) old, stack);
                        if (newValue != old) {
                            node.set(key, newValue);
                        }
                        continue;
                    }
                } else if (old instanceof ArrayNode) {
                    // [databind#3056]: related to Object handling, ensure
                    // Array values also match for mergeability
                    if ((t == JsonToken.START_ARRAY) && _mergeArrays) {
                        // 28-Mar-2021, tatu: We'll only append entries so not very different
                        //    from "regular" deserializeArray...
                        _deserializeContainerNoRecursion(p, ctxt, nodeFactory,
                                stack, (ArrayNode) old);
                        continue;
                    }
                }
            }
            if (t == null) { // can this ever occur?
                t = JsonToken.NOT_AVAILABLE;
            }
            JsonNode value;
            switch (t.id()) {
                case JsonTokenId.ID_START_OBJECT:
                    value = _deserializeContainerNoRecursion(p, ctxt, nodeFactory,
                            stack, nodeFactory.objectNode());
                    break;
                case JsonTokenId.ID_START_ARRAY:
                    value = _deserializeContainerNoRecursion(p, ctxt, nodeFactory,
                            stack, nodeFactory.arrayNode());
                    break;
                case JsonTokenId.ID_STRING:
                    value = nodeFactory.textNode(p.getText());
                    break;
                case JsonTokenId.ID_NUMBER_INT:
                    value = _fromInt(p, ctxt, nodeFactory);
                    break;
                case JsonTokenId.ID_TRUE:
                    value = nodeFactory.booleanNode(true);
                    break;
                case JsonTokenId.ID_FALSE:
                    value = nodeFactory.booleanNode(false);
                    break;
                case JsonTokenId.ID_NULL:
                    // 20-Mar-2022, tatu: [databind#3421] Allow skipping `null`s from JSON
                    if (!ctxt.isEnabled(JsonNodeFeature.READ_NULL_PROPERTIES)) {
                        continue;
                    }
                    value = nodeFactory.nullNode();
                    break;
                default:
                    value = _deserializeRareScalar(p, ctxt);
            }
            // 15-Feb-2021, tatu: I don't think this should have been called
            //   on update case (was until 2.12.2) and was simply result of
            //   copy-paste.
            /*
            if (old != null) {
                _handleDuplicateProperty(p, ctxt, nodeFactory,
                        key, node, old, value);
            }
            */
            node.set(key, value);
        }
        return node;
    }

    // Non-recursive alternative
    protected final ContainerNode<?> _deserializeContainerNoRecursion(JsonParser p, DeserializationContext ctxt,
                                                                      JsonNodeFactory nodeFactory, ContainerStack stack, final ContainerNode<?> root)
            throws JacksonException
    {
        ContainerNode<?> curr = root;
        final int intCoercionFeats = ctxt.getDeserializationFeatures() & F_MASK_INT_COERCIONS;

        outer_loop:
        do {
            if (curr instanceof ObjectNode) {
                ObjectNode currObject = (ObjectNode) curr;
                String propName = p.nextName();

                objectLoop:
                for (; propName != null; propName = p.nextName()) {
                    JsonNode value;
                    JsonToken t = p.nextToken();
                    if (t == null) { // unexpected end-of-input (or bad buffering?)
                        t = JsonToken.NOT_AVAILABLE; // to trigger an exception
                    }
                    switch (t.id()) {
                        case JsonTokenId.ID_START_OBJECT:
                        {
                            ObjectNode newOb = nodeFactory.objectNode();
                            JsonNode old = currObject.replace(propName, newOb);
                            if (old != null) {
                                _handleDuplicateProperty(p, ctxt, nodeFactory,
                                        propName, currObject, old, newOb);
                            }
                            stack.push(curr);
                            curr = currObject = newOb;
                            // We can actually take a short-cut with nested Objects...
                            continue objectLoop;
                        }
                        case JsonTokenId.ID_START_ARRAY:
                        {
                            ArrayNode newOb = nodeFactory.arrayNode();
                            JsonNode old = currObject.replace(propName, newOb);
                            if (old != null) {
                                _handleDuplicateProperty(p, ctxt, nodeFactory,
                                        propName, currObject, old, newOb);
                            }
                            stack.push(curr);
                            curr = newOb;
                        }
                        continue outer_loop;
                        case JsonTokenId.ID_STRING:
                            value = nodeFactory.textNode(p.getText());
                            break;
                        case JsonTokenId.ID_NUMBER_INT:
                            value = _fromInt(p, intCoercionFeats, nodeFactory);
                            break;
                        case JsonTokenId.ID_NUMBER_FLOAT:
                            value = _fromFloat(p, ctxt, nodeFactory);
                            break;
                        case JsonTokenId.ID_TRUE:
                            value = nodeFactory.booleanNode(true);
                            break;
                        case JsonTokenId.ID_FALSE:
                            value = nodeFactory.booleanNode(false);
                            break;
                        case JsonTokenId.ID_NULL:
                            // 20-Mar-2022, tatu: [databind#3421] Allow skipping `null`s from JSON
                            if (!ctxt.isEnabled(JsonNodeFeature.READ_NULL_PROPERTIES)) {
                                continue;
                            }
                            value = nodeFactory.nullNode();
                            break;
                        default:
                            value = _deserializeRareScalar(p, ctxt);
                    }
                    JsonNode old = currObject.replace(propName, value);
                    if (old != null) {
                        _handleDuplicateProperty(p, ctxt, nodeFactory,
                                propName, currObject, old, value);
                    }
                }
                // reached not-property-name, should be END_OBJECT (verify?)
            } else {
                // Otherwise we must have an array
                final ArrayNode currArray = (ArrayNode) curr;

                arrayLoop:
                while (true) {
                    JsonToken t = p.nextToken();
                    if (t == null) { // unexpected end-of-input (or bad buffering?)
                        t = JsonToken.NOT_AVAILABLE; // to trigger an exception
                    }
                    switch (t.id()) {
                        case JsonTokenId.ID_START_OBJECT:
                            stack.push(curr);
                            curr = nodeFactory.objectNode();
                            currArray.add(curr);
                            continue outer_loop;
                        case JsonTokenId.ID_START_ARRAY:
                            stack.push(curr);
                            curr = nodeFactory.arrayNode();
                            currArray.add(curr);
                            continue outer_loop;
                        case JsonTokenId.ID_END_ARRAY:
                            break arrayLoop;
                        case JsonTokenId.ID_STRING:
                            currArray.add(nodeFactory.textNode(p.getText()));
                            continue arrayLoop;
                        case JsonTokenId.ID_NUMBER_INT:
                            currArray.add(_fromInt(p, intCoercionFeats, nodeFactory));
                            continue arrayLoop;
                        case JsonTokenId.ID_NUMBER_FLOAT:
                            currArray.add(_fromFloat(p, ctxt, nodeFactory));
                            continue arrayLoop;
                        case JsonTokenId.ID_TRUE:
                            currArray.add(nodeFactory.booleanNode(true));
                            continue arrayLoop;
                        case JsonTokenId.ID_FALSE:
                            currArray.add(nodeFactory.booleanNode(false));
                            continue arrayLoop;
                        case JsonTokenId.ID_NULL:
                            currArray.add(nodeFactory.nullNode());
                            continue arrayLoop;
                        default:
                            currArray.add(_deserializeRareScalar(p, ctxt));
                            continue arrayLoop;
                    }
                }
                // Reached end of array (or input), so...
            }

            // Either way, Object or Array ended, return up nesting level:
            curr = stack.popOrNull();
        } while (curr != null);
        return root;
    }

    // Was called "deserializeAny()" in 2.12 and prior
    protected final JsonNode _deserializeAnyScalar(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
    {
        final JsonNodeFactory nodeF = ctxt.getNodeFactory();
        switch (p.currentTokenId()) {
            case JsonTokenId.ID_END_OBJECT:
                return nodeF.objectNode();
            case JsonTokenId.ID_STRING:
                return nodeF.textNode(p.getText());
            case JsonTokenId.ID_NUMBER_INT:
                return _fromInt(p, ctxt, nodeF);
            case JsonTokenId.ID_NUMBER_FLOAT:
                return _fromFloat(p, ctxt, nodeF);
            case JsonTokenId.ID_TRUE:
                return nodeF.booleanNode(true);
            case JsonTokenId.ID_FALSE:
                return nodeF.booleanNode(false);
            case JsonTokenId.ID_NULL:
                return nodeF.nullNode();
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                return _fromEmbedded(p, ctxt);

            // Caller should check for anything else
            default:
        }
        return (JsonNode) ctxt.handleUnexpectedToken(handledType(), p);
    }

    protected final JsonNode _deserializeRareScalar(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
    {
        // 28-Mar-2021, tatu: Only things that caller does not check
        switch (p.currentTokenId()) {
            case JsonTokenId.ID_END_OBJECT: // for empty JSON Objects we may point to this?
                return ctxt.getNodeFactory().objectNode();
            case JsonTokenId.ID_NUMBER_FLOAT:
                return _fromFloat(p, ctxt, ctxt.getNodeFactory());
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                return _fromEmbedded(p, ctxt);

            // Caller should check for anything else
            default:
        }
        return (JsonNode) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    protected final JsonNode _fromInt(JsonParser p, int coercionFeatures,
                                      JsonNodeFactory nodeFactory)
            throws JacksonException
    {
        if (coercionFeatures != 0) {
            if (DeserializationFeature.USE_BIG_INTEGER_FOR_INTS.enabledIn(coercionFeatures)) {
                return nodeFactory.numberNode(p.getBigIntegerValue());
            }
            return nodeFactory.numberNode(p.getLongValue());
        }
        final JsonParser.NumberType nt = p.getNumberType();
        if (nt == JsonParser.NumberType.INT) {
            return nodeFactory.numberNode(p.getIntValue());
        }
        if (nt == JsonParser.NumberType.LONG) {
            return nodeFactory.numberNode(p.getLongValue());
        }
        return nodeFactory.numberNode(p.getBigIntegerValue());
    }

    protected final JsonNode _fromInt(JsonParser p, DeserializationContext ctxt,
                                      JsonNodeFactory nodeFactory)
            throws JacksonException
    {
        JsonParser.NumberType nt;
        int feats = ctxt.getDeserializationFeatures();
        if ((feats & F_MASK_INT_COERCIONS) != 0) {
            if (DeserializationFeature.USE_BIG_INTEGER_FOR_INTS.enabledIn(feats)) {
                nt = JsonParser.NumberType.BIG_INTEGER;
            } else if (DeserializationFeature.USE_LONG_FOR_INTS.enabledIn(feats)) {
                nt = JsonParser.NumberType.LONG;
            } else {
                nt = p.getNumberType();
            }
        } else {
            nt = p.getNumberType();
        }
        if (nt == JsonParser.NumberType.INT) {
            return nodeFactory.numberNode(p.getIntValue());
        }
        if (nt == JsonParser.NumberType.LONG) {
            return nodeFactory.numberNode(p.getLongValue());
        }
        return nodeFactory.numberNode(p.getBigIntegerValue());
    }

    protected final JsonNode _fromFloat(JsonParser p, DeserializationContext ctxt,
            final JsonNodeFactory nodeFactory)
        throws JacksonException
    {
        // 13-Jan-2024, tatu: With 2.17 we have `JsonParser.getNumberTypeFP()` which
        //   will return concrete FP type if format has one (JSON doesn't; most binary
        //   formats do.
        //   But with `DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS` we have mechanism
        //   that may override optimal physical representation. So heuristics to use are:
        //
        //   1. If physical type is `BigDecimal`, use `BigDecimal`
        //   2. If `DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS` enabled, use `BigDecimal`
        //      UNLESS we have "Not-a-Number" (in which case  will coerce to `Double` if allowed
        //   3. Otherwise if physical type `Float`, use (32-bit) `Float`
        //   4. Otherwise use `Double`

        JsonParser.NumberTypeFP nt = p.getNumberTypeFP();
        if (nt == JsonParser.NumberTypeFP.BIG_DECIMAL) {
            BigDecimal nr = p.getDecimalValue();
            if (ctxt.isEnabled(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)) {
                nr = _normalize(nr);
            }
            return nodeFactory.numberNode(nr);
        }
        if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
            // [databind#4194] Add an option to fail coercing NaN to BigDecimal
            // Currently, Jackson 2.x allows such coercion, but Jackson 3.x will not
            if (p.isNaN()) {
                if (ctxt.isEnabled(JsonNodeFeature.FAIL_ON_NAN_TO_BIG_DECIMAL_COERCION)) {
                    return (JsonNode) ctxt.handleWeirdNumberValue(handledType(), p.getDoubleValue(),
                        "Cannot convert NaN into BigDecimal");
                }
                return nodeFactory.numberNode(p.getDoubleValue());
            }
            BigDecimal nr = p.getDecimalValue();
            if (ctxt.isEnabled(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)) {
                nr = _normalize(nr);
            }
            return nodeFactory.numberNode(nr);
        }
        if (nt == JsonParser.NumberTypeFP.FLOAT32) {
            return nodeFactory.numberNode(p.getFloatValue());
        }
        return nodeFactory.numberNode(p.getDoubleValue());
    }

    protected BigDecimal _normalize(BigDecimal nr) {
        // 24-Mar-2021, tatu: [dataformats-binary#264] barfs on a specific value...
        //   Must skip normalization in that particular case. Alas, haven't found
        //   another way to check it instead of getting "Overflow", catching
        try {
            nr = nr.stripTrailingZeros();
        } catch (ArithmeticException e) {
            // If we can't, we can't...
            ;
        }
        return nr;
    }

    protected final JsonNode _fromEmbedded(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
    {
        final JsonNodeFactory nodeF = ctxt.getNodeFactory();
        final Object ob = p.getEmbeddedObject();

        if (ob == null) { // should this occur?
            return nodeF.nullNode();
        }
        Class<?> type = ob.getClass();
        if (type == byte[].class) { // most common special case
            return nodeF.binaryNode((byte[]) ob);
        }
        // [databind#743]: Don't forget RawValue
        if (ob instanceof RawValue) {
            return nodeF.rawValueNode((RawValue) ob);
        }
        if (ob instanceof JsonNode) {
            // [databind#433]: but could also be a JsonNode hiding in there!
            return (JsonNode) ob;
        }
        // any other special handling needed?
        return nodeF.pojoNode(ob);
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Optimized variant similar in functionality to (a subset of)
     * {@link java.util.ArrayDeque}; used to hold enclosing Array/Object
     * nodes during recursion-as-iteration.
     */
    @SuppressWarnings("rawtypes")
    final static class ContainerStack
    {
        private ContainerNode[] _stack;
        private int _top, _end;

        public ContainerStack() { }

        // Not used yet but useful for limits (fail at [some high depth])
        public int size() { return _top; }

        public void push(ContainerNode node)
        {
            if (_top < _end) {
                _stack[_top++] = node; // lgtm [java/dereferenced-value-may-be-null]
                return;
            }
            if (_stack == null) {
                _end = 10;
                _stack = new ContainerNode[_end];
            } else {
                // grow by 50%, for most part
                _end += Math.min(4000, Math.max(20, _end>>1));
                _stack = Arrays.copyOf(_stack, _end);
            }
            _stack[_top++] = node;
        }

        public ContainerNode popOrNull() {
            if (_top == 0) {
                return null;
            }
            // note: could clean up stack but due to usage pattern, should not make
            // any difference -- all nodes joined during and after construction and
            // after construction the whole stack is discarded
            return _stack[--_top];
        }
    }
}
