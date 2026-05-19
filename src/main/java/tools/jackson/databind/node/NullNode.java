package tools.jackson.databind.node;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import tools.jackson.core.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;

/**
 * This singleton value class is used to contain explicit JSON null
 * value.
 */
public class NullNode
    extends ValueNode
{
    @Serial
    private static final long serialVersionUID = 3L;

    // // Just need a fly-weight singleton

    public final static NullNode instance = new NullNode();

    protected NullNode() { }

    public static NullNode getInstance() { return instance; }

    // To support JDK serialization, recovery of Singleton instance
    @Serial
    protected Object readResolve() {
        return instance;
    }

    @Override
    public JsonNodeType getNodeType() {
        return JsonNodeType.NULL;
    }

    @Override public JsonToken asToken() { return JsonToken.VALUE_NULL; }

    @Override
    public NullNode deepCopy() { return this; }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access, non-numeric
    /**********************************************************************
     */

    @Override
    protected Boolean _asBoolean() {
        return Boolean.FALSE;
    }

    @Override
    public boolean asBoolean(boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public Optional<Boolean> asBooleanOpt() {
        return Optional.empty();
    }

    @Override
    protected String _asString() {
        return "";
    }

    @Override
    public String asString(String defaultValue) {
        return defaultValue;
    }

    @Override
    public Optional<String> asStringOpt() {
        return Optional.empty();
    }

    // Explicit overrides for all overloads for documentation purposes

    @Override
    public String stringValue() { return null; }

    @Override
    public String stringValue(String defaultValue) { return defaultValue; }

    @Override
    public Optional<String> stringValueOpt() {
        return Optional.empty();
    }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access, numeric
    /**********************************************************************
     */

    // `shortValue()` (etc) fine as defaults (fail); but need to override `asShort()`

    @Override
    public short asShort() {
        return 0;
    }

    // `intValue()` (etc) fine as defaults (fail); but need to override `asInt()`

    @Override
    public int asInt() {
        return 0;
    }

    // `longValue()` (etc) fine as defaults (fail); but need to override `asLong()`

    @Override
    public long asLong() { return 0L; }

    // `bigIntegerValue()` (etc) fine as defaults (fail); but need to override `asBigInteger()`

    @Override
    public BigInteger asBigInteger() {
        return BigInteger.ZERO;
    }

    // `floatValue()` (etc) fine as defaults (fail); but need to override `asFloat()`

    @Override
    public float asFloat() {
        return 0.0f;
    }

    // `doubleValue()` (etc) fine as defaults (fail); but need to override `asDouble()`

    @Override
    public double asDouble() {
        return 0.0d;
    }

    // `decimalValue()` (etc) fine as defaults (fail); but need to override `asDecimal()`

    @Override
    public BigDecimal asDecimal() {
        return BigDecimal.ZERO;
    }

    /*
    public int asInt(int defaultValue);
    public long asLong(long defaultValue);
    public double asDouble(double defaultValue);
    public boolean asBoolean(boolean defaultValue);
    */

    /*
    /**********************************************************************
    /* Overridden methods, other
    /**********************************************************************
     */

    @Override
    protected String _valueDesc() {
        return "<null>";
    }

    @SuppressWarnings("unchecked")
    @Override
    public JsonNode requireNonNull() {
        return _reportRequiredViolation("requireNonNull() called on `NullNode`");
    }
    
    @Override
    public final void serialize(JsonGenerator g, SerializationContext provider)
        throws JacksonException
    {
        provider.defaultSerializeNullValue(g);
    }

    @Override
    public boolean equals(Object o) {
        // 29-Aug-2019, tatu: [databind#2433] Since custom sub-classes are allowed (bad idea probably),
        //     need to do better comparison
        return (o == this) || (o instanceof NullNode);
    }

    @Override
    public int hashCode() {
        return JsonNodeType.NULL.ordinal();
    }
}
