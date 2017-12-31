package com.fasterxml.jackson.databind.ext.jdk8;

import java.io.IOException;
import java.util.OptionalLong;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

public class OptionalLongSerializer extends StdScalarSerializer<OptionalLong>
{
    private static final long serialVersionUID = 1L;

    static final OptionalLongSerializer INSTANCE = new OptionalLongSerializer();

    public OptionalLongSerializer() {
        super(OptionalLong.class);
    }

    // @since 2.6
    @Override
    public boolean isEmpty(SerializerProvider provider, OptionalLong value) {
        return (value == null) || !value.isPresent();
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor,
            JavaType typeHint) throws JsonMappingException {
        JsonIntegerFormatVisitor v2 = visitor
                .expectIntegerFormat(typeHint);
        if (v2 != null) {
            v2.numberType(JsonParser.NumberType.LONG);
        }
    }

    @Override
    public void serialize(OptionalLong value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException
    {
        if (value.isPresent()) {
            jgen.writeNumber(value.getAsLong());
        } else { // should we get here?
            jgen.writeNull();
        }
    }
}
