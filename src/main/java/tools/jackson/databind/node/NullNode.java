package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

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
    private static final long serialVersionUID = 3L;

    // // Just need a fly-weight singleton

    public final static NullNode instance = new NullNode();

    protected NullNode() { }

    public static NullNode getInstance() { return instance; }

    // To support JDK serialization, recovery of Singleton instance
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
    protected String _asString() {
        return "";
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

    @Override
    public short asShort(short defaultValue) {
        return 0;
    }

    // `intValue()` (etc) fine as defaults (fail); but need to override `asInt()`

    @Override
    public int asInt() {
        return 0;
    }

    @Override
    public int asInt(int defaultValue) {
        return 0;
    }

    @Override
    public OptionalInt asIntOpt() {
        return OptionalInt.of(0);
    }

    // `longValue()` (etc) fine as defaults (fail); but need to override `asLong()`

    @Override
    public long asLong() { return 0L; }

    @Override
    public long asLong(long defaultValue) { return 0L; }

    @Override
    public OptionalLong asLongOpt() {
        return OptionalLong.of(0L);
    }

    // `bigIntegerValue()` (etc) fine as defaults (fail); but need to override `asBigInteger()`

    @Override
    public BigInteger asBigInteger() {
        return BigInteger.ZERO;
    }

    @Override
    public BigInteger asBigInteger(BigInteger defaultValue) {
        return asBigInteger();
    }

    @Override
    public Optional<BigInteger> asBigIntegerOpt() {
        return Optional.of(asBigInteger());
    }

    // `floatValue()` (etc) fine as defaults (fail); but need to override `asFloat()`

    @Override
    public float asFloat() {
        return 0.0f;
    }

    @Override
    public float asFloat(float defaultValue) {
        return asFloat();
    }

    // `doubleValue()` (etc) fine as defaults (fail); but need to override `asDouble()`

    @Override
    public double asDouble() {
        return 0.0d;
    }

    @Override
    public double asDouble(double defaultValue) {
        return asDouble();
    }

    @Override
    public OptionalDouble asDoubleOpt() {
        return OptionalDouble.of(asDouble());
    }
    
    // `decimalValue()` (etc) fine as defaults (fail); but need to override `asDecimal()`

    @Override
    public BigDecimal asDecimal() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal asDecimal(BigDecimal defaultValue) {
        return asDecimal();
    }

    @Override
    public Optional<BigDecimal> asDecimalOpt() {
        return Optional.of(asDecimal());
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
