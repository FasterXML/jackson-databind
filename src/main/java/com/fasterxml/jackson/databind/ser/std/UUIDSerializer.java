package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Specialized {@link JsonSerializer} to output {@link java.util.UUID}s.
 * Beyond optimized access and writing of textual representation (which
 * is the default handling in most cases), it will alternatively
 * allow serialization using raw binary output (as 16-byte block)
 * if underlying data format has efficient means to access that.
 */
@SuppressWarnings("serial")
public class UUIDSerializer
    extends StdScalarSerializer<UUID>
    implements ContextualSerializer // since 2.11.3 (for databind#2815)
{
    final static char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /**
     * Configuration setting that indicates if serialization as binary
     * (native or Base64-encoded) has been forced; {@code null} means
     * "use default heuristic"
     *
     * @since 2.11.3
     */
    protected final Boolean _asBinary;

    public UUIDSerializer() { this(null); }

    /**
     * @since 2.11.3
     */
    protected UUIDSerializer(Boolean asBinary) {
        super(UUID.class);
        _asBinary = asBinary;
    }

    @Override
    public boolean isEmpty(SerializerProvider prov, UUID value)
    {
        // Null UUID is empty, so...
        if (value.getLeastSignificantBits() == 0L
                && value.getMostSignificantBits() == 0L) {
            return true;
        }
        return false;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializers,
            BeanProperty property) throws JsonMappingException
    {
        JsonFormat.Value format = findFormatOverrides(serializers,
                property, handledType());
        Boolean asBinary = null;

        if (format != null) {
            JsonFormat.Shape shape = format.getShape();
            if (shape == JsonFormat.Shape.BINARY) {
                asBinary = true;
            } else if (shape == JsonFormat.Shape.STRING) {
                asBinary = false;
            }
            // otherwise leave as `null` meaning about same as NATURAL
        }
        if (!Objects.equals(asBinary, _asBinary)) {
            return new UUIDSerializer(asBinary);
        }
        return this;
    }

    @Override
    public void serialize(UUID value, JsonGenerator gen, SerializerProvider provider)
        throws IOException
    {
        // First: perhaps we could serialize it as raw binary data?
        if (_writeAsBinary(gen)) {
            gen.writeBinary(_asBytes(value));
            return;
        }

        // UUID.toString() works ok functionally, but we can make it go much faster
        // (by 4x with micro-benchmark)

        final char[] ch = new char[36];
        final long msb = value.getMostSignificantBits();
        _appendInt((int) (msb >> 32), ch, 0);
        ch[8] = '-';
        int i = (int) msb;
        _appendShort(i >>> 16, ch, 9);
        ch[13] = '-';
        _appendShort(i, ch, 14);
        ch[18] = '-';

        final long lsb = value.getLeastSignificantBits();
        _appendShort((int) (lsb >>> 48), ch, 19);
        ch[23] = '-';
        _appendShort((int) (lsb >>> 32), ch, 24);
        _appendInt((int) lsb, ch, 28);

        gen.writeString(ch, 0, 36);
    }

    // @since 2.11.3
    protected boolean _writeAsBinary(JsonGenerator g)
    {
        if (_asBinary != null) {
            return _asBinary;
        }
        // 07-Dec-2013, tatu: One nasty case; that of TokenBuffer. While it can
        //   technically retain binary data, we do not want to do use binary
        //   with it, as that results in UUIDs getting converted to Base64 for
        //   most conversions.
        return !(g instanceof TokenBuffer) && g.canWriteBinaryNatively();
    }


    // Need to add bit of extra info, format
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        visitStringFormat(visitor, typeHint, JsonValueFormat.UUID);
    }

    private static void _appendInt(int bits, char[] ch, int offset)
    {
        _appendShort(bits >> 16, ch, offset);
        _appendShort(bits, ch, offset+4);
    }

    private static void _appendShort(int bits, char[] ch, int offset)
    {
        ch[offset] = HEX_CHARS[(bits >> 12) & 0xF];
        ch[++offset] = HEX_CHARS[(bits >> 8) & 0xF];
        ch[++offset] = HEX_CHARS[(bits >> 4) & 0xF];
        ch[++offset] = HEX_CHARS[bits  & 0xF];
    }

    private final static byte[] _asBytes(UUID uuid)
    {
        byte[] buffer = new byte[16];
        long hi = uuid.getMostSignificantBits();
        long lo = uuid.getLeastSignificantBits();
        _appendInt((int) (hi >> 32), buffer, 0);
        _appendInt((int) hi, buffer, 4);
        _appendInt((int) (lo >> 32), buffer, 8);
        _appendInt((int) lo, buffer, 12);
        return buffer;
    }

    private final static void _appendInt(int value, byte[] buffer, int offset)
    {
        buffer[offset] = (byte) (value >> 24);
        buffer[++offset] = (byte) (value >> 16);
        buffer[++offset] = (byte) (value >> 8);
        buffer[++offset] = (byte) value;
    }
}
