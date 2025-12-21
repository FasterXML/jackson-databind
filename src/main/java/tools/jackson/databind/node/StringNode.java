package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import tools.jackson.core.*;
import tools.jackson.core.io.NumberInput;
import tools.jackson.core.util.ByteArrayBuilder;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.exc.JsonNodeException;

/**
 * Value node that contains a String value.
 */
public class StringNode
    extends ValueNode
{
    private static final long serialVersionUID = 3L;

    final static StringNode EMPTY_STRING_NODE = new StringNode("");

    protected final String _value;

    public StringNode(String v) {
        // 01-Mar-2024, tatu: [databind#4381] No null-valued StringNodes
        _value = Objects.requireNonNull(v);
    }

    /**
     * Factory method that should be used to construct instances.
     * For some common cases, can reuse canonical instances: currently
     * this is the case for empty Strings, in future possible for
     * others as well. If null is passed, will return null.
     *
     * @return Resulting {@link StringNode} object, if <b>v</b>
     *   is NOT null; null if it is.
     */
    public static StringNode valueOf(String v)
    {
        if (v == null) {
            return null;
        }
        if (v.isEmpty()) {
            return EMPTY_STRING_NODE;
        }
        return new StringNode(v);
    }

    @Override
    public JsonNodeType getNodeType() {
        return JsonNodeType.STRING;
    }

    @Override
    public JsonToken asToken() { return JsonToken.VALUE_STRING; }

    @Override
    protected String _valueDesc() {
        String s = _value;
        if (s.length() > 100) {
             return "\"" + s.substring(0, 100) + "\"[...]";
        }
        return "\""+_value+"\"";
    }

    @Override
    public StringNode deepCopy() { return this; }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access, non-numeric
    /**********************************************************************
     */

    @Override
    public Boolean _asBoolean() {
        if ("true".equals(_value)) {
            return Boolean.TRUE;
        }
        if ("false".equals(_value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    @Override
    public String stringValue() {
        return _value;
    }

    @Override
    public String stringValue(String defaultValue) {
        return _value;
    }

    @Override
    public Optional<String> stringValueOpt() {
        return Optional.of(_value);
    }

    @Override
    protected String _asString() {
        return _value;
    }

    // Directly override "asText()" variants as minor optimization

    @Override
    public String asString() {
        return _value;
    }

    @Override
    public String asString(String defaultValue) {
        return _value;
    }

    @Override
    public Optional<String> asStringOpt() {
        return Optional.of(_value);
    }

    /**
     * Method for accessing content String assuming they were
     * base64 encoded; if so, content is decoded and resulting binary
     * data is returned.
     *
     * @throws JsonNodeException if String contents are not valid Base64 encoded content
     */
    @SuppressWarnings("resource")
    public byte[] getBinaryValue(Base64Variant b64variant) throws JacksonException
    {
        final String str = _value.trim();
        // 04-Sep-2020, tatu: Let's limit the size of the initial block to 64k,
        //    no point in trying to exactly match the size beyond certain point
        // (plus it could even lead to unnecessarily high retention with block
        // recycling)
        final int initBlockSize = 4 + ((str.length() >> 2) * 3);
        ByteArrayBuilder builder = new ByteArrayBuilder(Math.max(16,
                Math.min(0x10000, initBlockSize)));
        try {
            b64variant.decode(str, builder);
        } catch (IllegalArgumentException e) {
            return _reportCoercionFail("binaryValue()", byte[].class,
                    "value type not binary and Base64-decoding failed with: "+e.getMessage());
        }
        return builder.toByteArray();
    }

    @Override
    public byte[] binaryValue() throws JacksonException {
        return getBinaryValue(Base64Variants.getDefaultVariant());
    }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access, numeric
    /**********************************************************************
     */

    @Override
    public short asShort() {
        Short S = _tryParseAsShort();
        if (S == null) {
            return _reportCoercionFail("asShort()", Short.TYPE,
                    "value not a valid String representation of `short`");
        }
        return S;
    }

    @Override
    public short asShort(short defaultValue) {
        Short S = _tryParseAsShort();
        return (S == null) ? defaultValue : S;
    }

    @Override
    public Optional<Short> asShortOpt() {
        Short S = _tryParseAsShort();
        return (S == null) ? Optional.empty() : Optional.of(S);
    }

    @Override
    public int asInt() {
        Integer I = _tryParseAsInteger();
        if (I == null) {
            return _reportCoercionFail("asInt()", Integer.TYPE,
                    "value not a valid String representation of `int`");
        }
        return I;
    }

    @Override
    public int asInt(int defaultValue) {
        Integer I = _tryParseAsInteger();
        return (I == null) ? defaultValue : I;
    }

    @Override
    public OptionalInt asIntOpt() {
        Integer I = _tryParseAsInteger();
        return (I == null) ? OptionalInt.empty() : OptionalInt.of(I);
    }
    
    @Override
    public long asLong() {
        Long L = _tryParseAsLong();
        if (L == null) {
            return _reportCoercionFail("asLong()", Long.TYPE,
                    "value not a valid String representation of `long`");
        }
        return L;
    }

    @Override
    public long asLong(long defaultValue) {
        Long L = _tryParseAsLong();
        return (L == null) ? defaultValue : L;
    }

    @Override
    public OptionalLong asLongOpt() {
        Long L = _tryParseAsLong();
        return (L == null) ? OptionalLong.empty() : OptionalLong.of(L);
    }

    // `bigIntegerValue()` (etc) fine as defaults (fail); but need to override `asBigInteger()`

    @Override
    public BigInteger asBigInteger() {
        BigInteger big = _tryParseAsBigInteger();
        if (big == null) {
            return _reportCoercionFail("asBigInteger()", BigInteger.class,
                    "value not a valid String representation of `BigInteger`");
        }
        return big;
    }

    @Override
    public BigInteger asBigInteger(BigInteger defaultValue) {
        BigInteger big = _tryParseAsBigInteger();
        return (big == null) ? defaultValue : big;
    }

    @Override
    public Optional<BigInteger> asBigIntegerOpt() {
        BigInteger big = _tryParseAsBigInteger();
        return (big == null) ? Optional.empty() : Optional.of(big);
    }

    // `floatValue()` (etc) fine as defaults (fail); but need to override `asFloat()`

    @Override
    public float asFloat()
    {
        Float F = _tryParseAsFloat();
        if (F == null) {
            return _reportCoercionFail("asFloat()", Float.TYPE,
                    "value not a valid String representation of `float`");
        }
        return F;
    }

    @Override
    public float asFloat(float defaultValue)
    {
        Float F = _tryParseAsFloat();
        return (F == null) ? defaultValue : F;
    }

    @Override
    public Optional<Float> asFloatOpt() {
        Float F = _tryParseAsFloat();
        return (F == null) ? Optional.empty() : Optional.of(F);
    }

    // `doubleValue()` (etc) fine as defaults (fail); but need to override `asDouble()`

    @Override
    public double asDouble()
    {
        Double d = _tryParseAsDouble();
        if (d == null) {
            return _reportCoercionFail("asDouble()", Double.TYPE,
                    "value not a valid String representation of `double`");
        }
        return d;
    }

    @Override
    public double asDouble(double defaultValue)
    {
        Double d = _tryParseAsDouble();
        return (d == null) ? defaultValue : d;
    }

    @Override
    public OptionalDouble asDoubleOpt() {
        Double d = _tryParseAsDouble();
        return (d == null) ? OptionalDouble.empty() : OptionalDouble.of(d);
    }
    
    // `decimalValue()` (etc) fine as defaults (fail); but need to override `asDecimal()`

    @Override
    public BigDecimal asDecimal() {
        BigDecimal dec = _tryParseAsBigDecimal();
        if (dec == null) {
            return _reportCoercionFail("asDecimal()", BigDecimal.class,
                    "value not a valid String representation of `BigDecimal`");
        }
        return dec;
    }

    @Override
    public BigDecimal asDecimal(BigDecimal defaultValue) {
        BigDecimal dec = _tryParseAsBigDecimal();
        return (dec == null) ? defaultValue : dec;
    }

    @Override
    public Optional<BigDecimal> asDecimalOpt() {
        BigDecimal dec = _tryParseAsBigDecimal();
        return (dec == null) ? Optional.empty() : Optional.of(dec);
    }

    protected Short _tryParseAsShort() {
        Integer I = _tryParseAsInteger();
        if (I != null && I >= Short.MIN_VALUE && I <= Short.MAX_VALUE) {
            return I.shortValue();
        }
        return null;
    }

    protected Integer _tryParseAsInteger() {
        if (NumberInput.looksLikeValidNumber(_value)) {
            try {
                // NumberInput does not have a good match so..
                return Integer.parseInt(_value);
            } catch (NumberFormatException e) {
                ;
            }
        }
        return null;
    }

    protected Long _tryParseAsLong() {
        if (NumberInput.looksLikeValidNumber(_value)) {
            try {
                return NumberInput.parseLong(_value);
            } catch (NumberFormatException e) {
                ;
            }
        }
        return null;
    }

    protected BigInteger _tryParseAsBigInteger() {
        if (NumberInput.looksLikeValidNumber(_value)) {
            try {
                return NumberInput.parseBigInteger(_value, true);
            } catch (NumberFormatException e) {
                ;
            }
        }
        return null;
    }

    protected Float _tryParseAsFloat() {
        if (NumberInput.looksLikeValidNumber(_value)) {
            try {
                return NumberInput.parseFloat(_value, true);
            } catch (NumberFormatException e) {
                ;
            }
        }
        return null;
    }

    protected Double _tryParseAsDouble() {
        if (NumberInput.looksLikeValidNumber(_value)) {
            try {
                return NumberInput.parseDouble(_value, true);
            } catch (NumberFormatException e) {
                ;
            }
        }
        return null;
    }

    protected BigDecimal _tryParseAsBigDecimal() {
        if (NumberInput.looksLikeValidNumber(_value)) {
            try {
                return NumberInput.parseBigDecimal(_value, true);
            } catch (NumberFormatException e) {
                ;
            }
        }
        return null;
    }
    
    /*
    /**********************************************************************
    /* Serialization
    /**********************************************************************
     */

    @Override
    public final void serialize(JsonGenerator g, SerializationContext provider)
        throws JacksonException
    {
        g.writeString(_value);
    }

    /*
    /**********************************************************************
    /* Overridden standard methods
    /**********************************************************************
     */

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof StringNode other) {
            return Objects.equals(other._value, _value);
        }
        return false;
    }

    @Override
    public int hashCode() { return _value.hashCode(); }
}
