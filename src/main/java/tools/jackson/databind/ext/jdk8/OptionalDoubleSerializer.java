package tools.jackson.databind.ext.jdk8;

import java.util.OptionalDouble;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonNumberFormatVisitor;
import tools.jackson.databind.ser.std.StdScalarSerializer;

public class OptionalDoubleSerializer extends StdScalarSerializer<OptionalDouble>
{
    static final OptionalDoubleSerializer INSTANCE = new OptionalDoubleSerializer();

    public OptionalDoubleSerializer() {
        super(OptionalDouble.class);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, OptionalDouble value) {
        return (value == null) || !value.isPresent();
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor,
            JavaType typeHint)
    {
        JsonNumberFormatVisitor v2 = visitor.expectNumberFormat(typeHint);
        if (v2 != null) {
            v2.numberType(JsonParser.NumberType.DOUBLE);
        }
    }

    @Override
    public void serialize(OptionalDouble value, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        if (value.isPresent()) {
            gen.writeNumber(value.getAsDouble());
        } else {
            gen.writeNull();
        }
    }
}
