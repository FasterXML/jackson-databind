package com.fasterxml.jackson.databind.node;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.NumberInput;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * Value node that contains a text value.
 */
public class TextNode
    extends ValueNode
{
    private static final long serialVersionUID = 2L;

    final static TextNode EMPTY_STRING_NODE = new TextNode("");

    protected final String _value;

    public TextNode(String v) { _value = v; }

    /**
     * Factory method that should be used to construct instances.
     * For some common cases, can reuse canonical instances: currently
     * this is the case for empty Strings, in future possible for
     * others as well. If null is passed, will return null.
     *
     * @return Resulting {@link TextNode} object, if <b>v</b>
     *   is NOT null; null if it is.
     */
    public static TextNode valueOf(String v)
    {
        if (v == null) {
            return null;
        }
        if (v.isEmpty()) {
            return EMPTY_STRING_NODE;
        }
        return new TextNode(v);
    }

    @Override
    public JsonNodeType getNodeType() {
        return JsonNodeType.STRING;
    }

    @Override public JsonToken asToken() { return JsonToken.VALUE_STRING; }

    @Override
    public String textValue() {
        return _value;
    }

    /**
     * Method for accessing textual contents assuming they were
     * base64 encoded; if so, they are decoded and resulting binary
     * data is returned.
     */
    @SuppressWarnings("resource")
    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException
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
            throw InvalidFormatException.from(null,
                    String.format(
"Cannot access contents of TextNode as binary due to broken Base64 encoding: %s",
e.getMessage()),
                    str, byte[].class);
        }
        return builder.toByteArray();
    }

    @Override
    public byte[] binaryValue() throws IOException {
        return getBinaryValue(Base64Variants.getDefaultVariant());
    }

    /*
    /**********************************************************
    /* General type coercions
    /**********************************************************
     */

    @Override
    public String asText() {
        return _value;
    }

    @Override
    public String asText(String defaultValue) {
        return (_value == null) ? defaultValue : _value;
    }

    // note: neither fast nor elegant, but these work for now:

    @Override
    public boolean asBoolean(boolean defaultValue) {
        if (_value != null) {
            String v = _value.trim();
            if ("true".equals(v)) {
                return true;
            }
            if ("false".equals(v)) {
                return false;
            }
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
        return NumberInput.parseAsDouble(_value, defaultValue);
    }

    /*
    /**********************************************************
    /* Serialization
    /**********************************************************
     */

    @Override
    public final void serialize(JsonGenerator g, SerializerProvider provider) throws IOException
    {
        if (_value == null) {
            g.writeNull();
        } else {
            g.writeString(_value);
        }
    }

    /*
    /**********************************************************
    /* Overridden standard methods
    /**********************************************************
     */

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof TextNode) {
            return ((TextNode) o)._value.equals(_value);
        }
        return false;
    }

    @Override
    public int hashCode() { return _value.hashCode(); }

    @Deprecated // since 2.10
    protected static void appendQuoted(StringBuilder sb, String content)
    {
        sb.append('"');
        CharTypes.appendQuoted(sb, content);
        sb.append('"');
    }
}
