package tools.jackson.databind.ser.jdk;

import java.nio.ByteBuffer;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.util.ByteBufferBackedInputStream;

public class ByteBufferSerializer extends StdScalarSerializer<ByteBuffer>
{
    public ByteBufferSerializer() { super(ByteBuffer.class); }

    @Override
    public void serialize(ByteBuffer bbuf, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        // first, simple case when wrapping an array...
        if (bbuf.hasArray()) {
            final int pos = bbuf.position();
            gen.writeBinary(bbuf.array(), bbuf.arrayOffset() + pos, bbuf.limit() - pos);
            return;
        }
        // the other case is more complicated however. Best to handle with InputStream wrapper.
        // Prior to jackson-databind#4164 we rewound here, but that didn't match heap buffer behavior.
        ByteBuffer copy = bbuf.asReadOnlyBuffer();
        try (ByteBufferBackedInputStream in = new ByteBufferBackedInputStream(copy)) {
            gen.writeBinary(in, copy.remaining());
        }
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        // 31-Mar-2017, tatu: Use same type as `ByteArraySerializer`: not optimal but has to do
        JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
        if (v2 != null) {
            v2.itemsFormat(JsonFormatTypes.INTEGER);
        }
    }
}
