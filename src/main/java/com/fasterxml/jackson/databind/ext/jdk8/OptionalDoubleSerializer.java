package com.fasterxml.jackson.databind.ext.jdk8;

import java.io.IOException;
import java.util.OptionalDouble;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNumberFormatVisitor;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

public class OptionalDoubleSerializer extends StdScalarSerializer<OptionalDouble>
{
    private static final long serialVersionUID = 1L;

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
            JavaType typeHint) throws JsonMappingException {
        JsonNumberFormatVisitor v2 = visitor.expectNumberFormat(typeHint);
        if (v2 != null) {
            v2.numberType(JsonParser.NumberType.DOUBLE);
        }
    }

    @Override
    public void serialize(OptionalDouble value, JsonGenerator gen, SerializerProvider provider)
        throws IOException
    {
        if (value.isPresent()) {
            gen.writeNumber(value.getAsDouble());
        } else {
            gen.writeNull();
        }
    }
}
