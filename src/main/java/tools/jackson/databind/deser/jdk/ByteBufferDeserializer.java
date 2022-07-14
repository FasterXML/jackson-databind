package tools.jackson.databind.deser.jdk;

import java.nio.ByteBuffer;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.util.ByteBufferBackedOutputStream;

public class ByteBufferDeserializer extends StdScalarDeserializer<ByteBuffer>
{
    ByteBufferDeserializer() { super(ByteBuffer.class); }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Binary;
    }

    @Override
    public ByteBuffer deserialize(JsonParser parser, DeserializationContext ctxt) throws JacksonException {
        byte[] b = parser.getBinaryValue();
        return ByteBuffer.wrap(b);
    }

    @Override
    public ByteBuffer deserialize(JsonParser p, DeserializationContext ctxt, ByteBuffer intoValue) throws JacksonException {
        // Let's actually read in streaming manner...
        try (ByteBufferBackedOutputStream out = new ByteBufferBackedOutputStream(intoValue)) {
            p.readBinaryValue(ctxt.getBase64Variant(), out);
        }
        return intoValue;
    }
}
