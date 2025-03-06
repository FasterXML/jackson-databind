package tools.jackson.databind.node;

import java.util.Objects;

import tools.jackson.core.*;
import tools.jackson.core.io.NumberInput;
import tools.jackson.core.util.ByteArrayBuilder;
import tools.jackson.databind.SerializationContext;

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

    @Override public JsonToken asToken() { return JsonToken.VALUE_STRING; }

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
    /* Overridden JsonNode methods, scalar access
    /**********************************************************************
     */

    @Override
    public String stringValue() {
        return _value;
    }

    /**
     * Method for accessing content String assuming they were
     * base64 encoded; if so, content is decoded and resulting binary
     * data is returned.
     *
     * @throws JacksonException if String contents are not valid Base64 encoded content
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

    @Override
    public String asString() {
        return _value;
    }

    @Override
    public String asString(String defaultValue) {
        return _value;
    }

    // note: neither fast nor elegant, but these work for now:

    @Override
    public boolean asBoolean(boolean defaultValue) {
        String v = _value.trim();
        if ("true".equals(v)) {
            return true;
        }
        if ("false".equals(v)) {
            return false;
        }
        return defaultValue;
    }

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
