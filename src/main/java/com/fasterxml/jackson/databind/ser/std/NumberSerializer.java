package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNumberFormatVisitor;

/**
 * As a fallback, we may need to use this serializer for other
 * types of {@link Number}s: both custom types and "big" numbers
 * like {@link BigInteger} and {@link BigDecimal}.
 */
@JacksonStdImpl
public final class NumberSerializer
    extends StdScalarSerializer<Number>
{
    public final static NumberSerializer instance = new NumberSerializer();

    public NumberSerializer() { super(Number.class); }

    @Override
    public void serialize(Number value, JsonGenerator jgen, SerializerProvider provider) throws IOException
    {
        if (value instanceof BigDecimal) {
            jgen.writeNumber((BigDecimal) value);
        } else if (value instanceof BigInteger) {
            jgen.writeNumber((BigInteger) value);
            
        /* These shouldn't match (as there are more specific ones),
         * but just to be sure:
         */
        } else if (value instanceof Integer) {
            jgen.writeNumber(value.intValue());
        } else if (value instanceof Long) {
            jgen.writeNumber(value.longValue());
        } else if (value instanceof Double) {
            jgen.writeNumber(value.doubleValue());
        } else if (value instanceof Float) {
            jgen.writeNumber(value.floatValue());
        } else if ((value instanceof Byte) || (value instanceof Short)) {
            jgen.writeNumber(value.intValue()); // doesn't need to be cast to smaller numbers
        } else {
            // We'll have to use fallback "untyped" number write method
            jgen.writeNumber(value.toString());
        }
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
        return createSchemaNode("number", true);
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
    {
        // Hmmh. What should it be? Ideally should probably indicate BIG_DECIMAL
        // to ensure no information is lost? But probably won't work that well...
        JsonNumberFormatVisitor v2 = visitor.expectNumberFormat(typeHint);
        if (v2 != null) {
            v2.numberType(JsonParser.NumberType.BIG_DECIMAL);
        }
    }
}