package com.fasterxml.jackson.databind.ext.jdk8;

import java.util.OptionalLong;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

public class OptionalLongSerializer extends StdScalarSerializer<OptionalLong>
{
    static final OptionalLongSerializer INSTANCE = new OptionalLongSerializer();

    public OptionalLongSerializer() {
        super(OptionalLong.class);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, OptionalLong value) {
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
    public void serialize(OptionalLong value, JsonGenerator jgen, SerializerProvider provider)
        throws JacksonException
    {
        if (value.isPresent()) {
            jgen.writeNumber(value.getAsLong());
        } else { // should we get here?
            jgen.writeNull();
        }
    }
}
