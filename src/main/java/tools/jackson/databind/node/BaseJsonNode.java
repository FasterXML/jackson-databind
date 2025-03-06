package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import tools.jackson.core.*;
import tools.jackson.databind.JacksonSerializable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.util.ClassUtil;

/**
 * Abstract base class common to all standard {@link JsonNode}
 * implementations.
 * The main addition here is that we declare that sub-classes must
 * implement {@link JacksonSerializable}.
 * This simplifies object mapping aspects a bit, as no external serializers are needed.
 *<p>
 * Note that support for {@link java.io.Serializable} is added here and so all subtypes
 * are fully JDK serializable: but also note that serialization is as JSON and should
 * only be used for interoperability purposes where other approaches are not available.
 */
public abstract class BaseJsonNode
    extends JsonNode
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    // Simplest way is by using a helper
    Object writeReplace() {
        return NodeSerialization.from(this);
    }

    protected BaseJsonNode() { }

    /*
    /**********************************************************************
    /* Defaulting for introspection
    /**********************************************************************
     */

    @Override
    public boolean isMissingNode() { return false; }

    @Override
    public boolean isEmbeddedValue() { return false; }

    /*
    /**********************************************************************
    /* Defaulting for number access
    /**********************************************************************
     */

    @Override
    public Number numberValue() {
        return _reportCoercionFail("numberValue()", Number.class, "value type not numeric");
    }

    @Override
    public short shortValue() {
        return _reportCoercionFail("shortValue()", Short.TYPE, "value type not numeric");
    }

    @Override
    public int intValue() {
        return _reportCoercionFail("intValue()", Integer.TYPE, "value type not numeric");
    }

    @Override
    public int intValue(int defaultValue) {
        // Overridden by NumericNode, for other types return default
        return defaultValue;
    }

    @Override
    public OptionalInt intValueOpt() {
        // Overridden by NumericNode, for other types return default
        return OptionalInt.empty();
    }

    @Override
    public int asInt() {
        return asInt(0);
    }

    @Override
    public int asInt(int defaultValue) {
        return defaultValue;
    }

    @Override
    public long longValue() {
        return _reportCoercionFail("longValue()", Long.TYPE, "value type not numeric");
    }

    @Override
    public long longValue(long defaultValue) {
        // Overridden by NumericNode, for other types return default
        return defaultValue;
    }

    @Override
    public OptionalLong longValueOpt() {
        // Overridden by NumericNode, for other types return default
        return OptionalLong.empty();
    }

    @Override
    public long asLong() {
        return asLong(0L);
    }

    @Override
    public long asLong(long defaultValue) {
        return defaultValue;
    }

    @Override
    public BigInteger bigIntegerValue() {
        return _reportCoercionFail("bigIntegerValue()", BigInteger.class, "value type not numeric");
    }

    @Override
    public float floatValue() {
        return _reportCoercionFail("floatValue()", Float.TYPE, "value type not numeric");
    }

    @Override
    public double doubleValue() {
        return _reportCoercionFail("doubleValue()", Double.TYPE, "value type not numeric");
    }

    @Override
    public double doubleValue(double defaultValue) {
        // Overridden by NumericNode, for other types return default
        return defaultValue;
    }

    @Override
    public OptionalDouble doubleValueOpt() {
        // Overridden by NumericNode, for other types return default
        return OptionalDouble.empty();
    }

    @Override
    public double asDouble() {
        return asDouble(0.0);
    }

    @Override
    public double asDouble(double defaultValue) {
        return defaultValue;
    }
    
    @Override
    public BigDecimal decimalValue() {
        return _reportCoercionFail("decimalValue()", BigDecimal.class, "value type not numeric");
    }

    @Override
    public BigDecimal decimalValue(BigDecimal defaultValue) {
        // Overridden by NumericNode, for other types return default
        return defaultValue;
    }

    @Override
    public Optional<BigDecimal> decimalValueOpt() {
        return Optional.empty();
    }

    @Override
    public BigDecimal asDecimal() {
        return asDecimal(BigDecimal.ZERO);
    }
    
    @Override
    public BigDecimal asDecimal(BigDecimal defaultValue) {
        // !!! TODO
        return decimalValue(defaultValue);
    }

    /*
    /**********************************************************************
    /* Defaulting for non-number scalar access
    /**********************************************************************
     */

    @Override
    public byte[] binaryValue() {
        return null;
    }

    @Override
    public boolean booleanValue() {
        return _reportCoercionFail("booleanValue()", Boolean.TYPE, "value type not boolean");
    }

    @Override
    public boolean booleanValue(boolean defaultValue) {
        // Overridden by BooleanNode, for other types return default
        return defaultValue;
    }

    @Override
    public Optional<Boolean> booleanValueOpt() {
        // Overridden by BooleanNode, for other types return default
        return Optional.empty();
    }

    @Override
    public boolean asBoolean() {
        return asBoolean(false);
    }

    @Override
    public boolean asBoolean(boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public String asString(String defaultValue) {
        String str = asString();
        return (str == null) ? defaultValue : str;
    }

    /*
    /**********************************************************************
    /* Basic definitions for non-container types
    /**********************************************************************
     */

    @Override
    public final JsonNode findPath(String fieldName)
    {
        JsonNode value = findValue(fieldName);
        if (value == null) {
            return MissingNode.getInstance();
        }
        return value;
    }

    // Also, force (re)definition
    @Override public abstract int hashCode();

    /*
    /**********************************************************************
    /* Improved required-ness checks for standard JsonNode implementations
    /**********************************************************************
     */

    @Override
    public JsonNode required(String fieldName) {
        return _reportRequiredViolation("Node of type %s has no fields",
                ClassUtil.nameOf(getClass()));
    }

    @Override
    public JsonNode required(int index) {
        return _reportRequiredViolation("Node of type %s has no indexed values",
                ClassUtil.nameOf(getClass()));
    }

    /*
    /**********************************************************************
    /* Support for traversal-as-stream
    /**********************************************************************
     */

    @Override
    public JsonParser traverse(ObjectReadContext readCtxt) {
        return new TreeTraversingParser(this, readCtxt);
    }

    /**
     * Method that can be used for efficient type detection
     * when using stream abstraction for traversing nodes.
     * Will return the first {@link JsonToken} that equivalent
     * stream event would produce (for most nodes there is just
     * one token but for structured/container types multiple)
     */
    @Override
    public abstract JsonToken asToken();

    /**
     * Returns code that identifies type of underlying numeric
     * value, if (and only if) node is a number node.
     */
    @Override
    public JsonParser.NumberType numberType() {
        // most types non-numeric, so:
        return null;
    }

    /*
    /**********************************************************************
    /* With-Traversal
    /**********************************************************************
     */

    @Override
    public ObjectNode withObject(JsonPointer ptr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        // Degenerate case of using with "empty" path; ok if ObjectNode
        if (ptr.matches()) {
            if (this instanceof ObjectNode) {
                return (ObjectNode) this;
            }
            _reportWrongNodeType("Can only call `withObject()` with empty JSON Pointer on `ObjectNode`, not %s",
                    ClassUtil.nameOf(getClass()));
        }
        // Otherwise check recursively
        ObjectNode n = _withObject(ptr, ptr, overwriteMode, preferIndex);
        if (n == null) {
            _reportWrongNodeType("Cannot replace context node (of type %s) using `withObject()` with  JSON Pointer '%s'",
                    ClassUtil.nameOf(getClass()), ptr);
        }
        return n;
    }

    protected ObjectNode _withObject(JsonPointer origPtr,
            JsonPointer currentPtr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        // Three-part logic:
        //
        // 1) If we are at the end of JSON Pointer; if so, return
        //    `this` if Object node, `null` if not (for caller to handle)
        // 2) If not at the end, if we can follow next segment, call recursively
        //    handle non-null (existing Object node, return)
        //    vs `null` (must replace; may not be allowed to)
        // 3) Can not follow the segment? Try constructing, adding path
        //
        // But the default implementation assumes non-container behavior so
        // it'll simply return `null`
        return null;
    }

    protected void _withXxxVerifyReplace(JsonPointer origPtr,
            JsonPointer currentPtr,
            OverwriteMode overwriteMode, boolean preferIndex,
            JsonNode toReplace)
    {
        if (!_withXxxMayReplace(toReplace, overwriteMode)) {
            _reportWrongNodeType(
"Cannot replace `JsonNode` of type %s for property \"%s\" in JSON Pointer \"%s\" (mode `OverwriteMode.%s`)",
                ClassUtil.nameOf(toReplace.getClass()), currentPtr.getMatchingProperty(),
                origPtr, overwriteMode);
        }
    }

    protected boolean _withXxxMayReplace(JsonNode node, OverwriteMode overwriteMode) {
        switch (overwriteMode) {
        case NONE:
            return false;
        case NULLS:
            return node.isNull();
        case SCALARS:
            return !node.isContainer();
        default:
        case ALL:
            return true;
        }
    }

    @Override
    public ArrayNode withArray(JsonPointer ptr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        // Degenerate case of using with "empty" path; ok if ArrayNode
        if (ptr.matches()) {
            if (this instanceof ArrayNode) {
                return (ArrayNode) this;
            }
            _reportWrongNodeType("Can only call `withArray()` with empty JSON Pointer on `ArrayNode`, not %s",
                    ClassUtil.nameOf(getClass()));
        }
        // Otherwise check recursively
        ArrayNode n = _withArray(ptr, ptr, overwriteMode, preferIndex);
        if (n == null) {
            _reportWrongNodeType("Cannot replace context node (of type %s) using `withArray()` with  JSON Pointer '%s'",
                    ClassUtil.nameOf(getClass()), ptr);
        }
        return n;
    }

    protected ArrayNode _withArray(JsonPointer origPtr,
            JsonPointer currentPtr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        // Similar logic to "_withObject()" but the default implementation
        // used for non-container behavior so it'll simply return `null`
        return null;
    }

    /*
    /**********************************************************************
    /* JacksonSerializable
    /**********************************************************************
     */

    /**
     * Method called to serialize node instances using given generator.
     */
    @Override
    public abstract void serialize(JsonGenerator jgen, SerializationContext ctxt)
        throws JacksonException;

    /**
     * Type information is needed, even if JsonNode instances are "plain" JSON,
     * since they may be mixed with other types.
     */
    @Override
    public abstract void serializeWithType(JsonGenerator jgen, SerializationContext ctxt,
            TypeSerializer typeSer)
        throws JacksonException;

    /*
    /**********************************************************************
    /* Standard method overrides
    /**********************************************************************
     */

    @Override
    public String toString() {
        return InternalNodeSerializer.toString(this);
    }

    @Override
    public String toPrettyString() {
        return InternalNodeSerializer.toPrettyString(this);
    }

    /*
    /**********************************************************************
    /* Helper method: error reporting
    /**********************************************************************
     */

    protected <T> T _reportCoercionFail(String method, Class<?> targetType,
            String message)
    {
        throw JsonNodeException.from(this, "'%s' method `%s` cannot convert value %s to %s: %s",
                getClass().getSimpleName(), method,
                _valueDesc(), ClassUtil.nameOf(targetType), message);
    }

    protected short _reportShortCoercionRangeFail(String method) {
        return _reportCoercionFail(method, Short.TYPE,
            "value not in 16-bit `short` range");
    }

    protected int _reportIntCoercionRangeFail(String method) {
        return _reportCoercionFail(method, Integer.TYPE,
            "value not in 32-bit `int` range");
    }

    protected long _reportLongCoercionRangeFail(String method) {
        return _reportCoercionFail(method, Long.TYPE,
            "value not in 64-bit `long` range");
    }

    protected float _reportFloatCoercionRangeFail(String method) {
        return _reportCoercionFail(method, Float.TYPE,
            "value not in 32-bit `float` range");
    }

    protected double _reportDoubleCoercionRangeFail(String method) {
        return _reportCoercionFail(method, Double.TYPE,
            "value not in 64-bit `double` range");
    }

    protected short _reportShortCoercionFractionFail(String method) {
        return _reportCoercionFail(method, Short.TYPE,
                "value has fractional part");
    }

    protected int _reportIntCoercionFractionFail(String method) {
        return _reportCoercionFail(method, Integer.TYPE,
                "value has fractional part");
    }

    protected long _reportLongCoercionFractionFail(String method) {
        return _reportCoercionFail(method, Long.TYPE,
                "value has fractional part");
    }

    protected BigInteger _reportBigIntegerCoercionFractionFail(String method) {
        return _reportCoercionFail(method, BigInteger.class,
                "value has fractional part");
    }

    /**
     * Helper method that throws {@link JsonNodeException} as a result of
     * this node being of wrong type
     */
    protected <T> T _reportWrongNodeType(String msgTemplate, Object...args) {
        throw JsonNodeException.from(this, String.format(msgTemplate, args));
    }

    /*
    /**********************************************************************
    /* Other helper methods for subtypes
    /**********************************************************************
     */

    protected JsonPointer _jsonPointerIfValid(String exprOrProperty) {
        if (exprOrProperty.isEmpty() || exprOrProperty.charAt(0) == '/') {
            return JsonPointer.compile(exprOrProperty);
        }
        return null;
    }

    /**
     * Method for implementation classes to return a short description of contained
     * value, to be used in error messages.
     */
    protected abstract String _valueDesc();
}
