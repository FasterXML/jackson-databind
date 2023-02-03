package tools.jackson.databind.ext.jdk8;

import java.util.OptionalInt;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import tools.jackson.databind.ser.std.StdScalarSerializer;

public class OptionalIntSerializer extends StdScalarSerializer<OptionalInt>
{
    public OptionalIntSerializer() {
        super(OptionalInt.class);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, OptionalInt value) {
        return (value == null) || !value.isPresent();
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor,
            JavaType typeHint)
    {
        JsonIntegerFormatVisitor v2 = visitor
                .expectIntegerFormat(typeHint);
        if (v2 != null) {
            v2.numberType(JsonParser.NumberType.INT);
        }
    }

    @Override
    public void serialize(OptionalInt value, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        if (value.isPresent()) {
            gen.writeNumber(value.getAsInt());
        } else {
            gen.writeNull();
        }
    }
}
