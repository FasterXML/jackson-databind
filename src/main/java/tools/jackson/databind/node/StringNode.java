package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

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
             return String.format("\"%s\"[...]", s.substring(0, 100));
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
    public int asInt(int defaultValue) {
        return NumberInput.parseAsInt(_value, defaultValue);
    }

    @Override
    public long asLong(long defaultValue) {
        return NumberInput.parseAsLong(_value, defaultValue);
    }

    @Override
    public double asDouble(double defaultValue) {
        return NumberInput.parseAsDouble(_value, defaultValue, false);
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
        if (o instanceof StringNode) {
            return Objects.equals(((StringNode) o)._value, _value);
        }
        return false;
    }

    @Override
    public int hashCode() { return _value.hashCode(); }
}
