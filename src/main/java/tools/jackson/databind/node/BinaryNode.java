package tools.jackson.databind.node;

import java.util.Arrays;
import java.util.Objects;

import tools.jackson.core.*;
import tools.jackson.databind.SerializerProvider;

/**
 * Value node that contains Base64 encoded binary value, which will be
 * output and stored as Json String value.
 */
public class BinaryNode
    extends ValueNode
{
    private static final long serialVersionUID = 3L;

    final static BinaryNode EMPTY_BINARY_NODE = new BinaryNode(new byte[0]);

    protected final byte[] _data;

    public BinaryNode(byte[] data)
    {
        // 01-Mar-2024, tatu: [databind#4381] No null-valued JsonNodes
        _data = Objects.requireNonNull(data);
    }

    public BinaryNode(byte[] data, int offset, int length)
    {
        if (offset == 0 && length == data.length) {
            _data = data;
        } else {
            _data = Arrays.copyOfRange(data, offset, offset + length);
        }
    }

    public static BinaryNode valueOf(byte[] data)
    {
        if (data == null) {
            return null;
        }
        if (data.length == 0) {
            return EMPTY_BINARY_NODE;
        }
        return new BinaryNode(data);
    }

    public static BinaryNode valueOf(byte[] data, int offset, int length)
    {
        if (data == null) {
            return null;
        }
        if (length == 0) {
            return EMPTY_BINARY_NODE;
        }
        return new BinaryNode(data, offset, length);
    }

    @Override
    public JsonNodeType getNodeType()
    {
        return JsonNodeType.BINARY;
    }

    @Override
    public JsonToken asToken() {
        /* No distinct type; could use one for textual values,
         * but given that it's not in text form at this point,
         * embedded-object is closest
         */
        return JsonToken.VALUE_EMBEDDED_OBJECT;
    }

    /**
     *<p>
     * Note: caller is not to modify returned array in any way, since
     * it is not a copy but reference to the underlying byte array.
     */
    @Override
    public byte[] binaryValue() { return _data; }

    /**
     * Hmmh. This is not quite as efficient as using {@link #serialize},
     * but will work correctly.
     */
    @Override
    public String asText() {
        return Base64Variants.getDefaultVariant().encode(_data, false);
    }

    @Override
    public final void serialize(JsonGenerator g, SerializerProvider provider)
        throws JacksonException
    {
        g.writeBinary(provider.getConfig().getBase64Variant(),
                _data, 0, _data.length);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof BinaryNode) {
            byte[] otherData = ((BinaryNode) o)._data;
            return Arrays.equals(_data, otherData);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _data.length;
    }
}
