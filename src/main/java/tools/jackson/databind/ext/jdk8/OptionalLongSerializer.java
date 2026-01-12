package tools.jackson.databind.ext.jdk8;

import java.util.OptionalLong;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import tools.jackson.databind.ser.std.StdScalarSerializer;

public class OptionalLongSerializer extends StdScalarSerializer<OptionalLong>
{
    static final OptionalLongSerializer INSTANCE = new OptionalLongSerializer();

    public OptionalLongSerializer() {
        super(OptionalLong.class);
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, OptionalLong value) {
        return (value == null) || !value.isPresent();
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor,
            JavaType typeHint)
    {
        JsonIntegerFormatVisitor v2 = visitor
                .expectIntegerFormat(typeHint);
        if (v2 != null) {
            v2.numberType(JsonParser.NumberType.LONG);
        }
    }

    @Override
    public void serialize(OptionalLong value, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        if (value.isPresent()) {
            g.writeNumber(value.getAsLong());
        } else { // should we get here?
            g.writeNull();
        }
    }
}
