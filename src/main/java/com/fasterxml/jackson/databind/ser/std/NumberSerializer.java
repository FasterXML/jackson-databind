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
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNumberFormatVisitor;

/**
 * As a fallback, we may need to use this serializer for other
 * types of {@link Number}s: both custom types and "big" numbers
 * like {@link BigInteger} and {@link BigDecimal}.
 */
@JacksonStdImpl
@SuppressWarnings("serial")
public class NumberSerializer
    extends StdScalarSerializer<Number>
{
    /**
     * Static instance that is only to be used for {@link java.lang.Number}.
     */
    public final static NumberSerializer instance = new NumberSerializer(Number.class);

    protected final boolean _isInt;

    @Deprecated // since 2.5
    public NumberSerializer() {
        super(Number.class);
        _isInt = false;
    }

    /**
     * @since 2.5
     */
    public NumberSerializer(Class<? extends Number> rawType) {
        super(rawType, false);
        // since this will NOT be constructed for Integer or Long, only case is:
        _isInt = (rawType == BigInteger.class);
    }

    @Override
    public void serialize(Number value, JsonGenerator jgen, SerializerProvider provider) throws IOException
    {
        // should mostly come in as one of these two:
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
        return createSchemaNode(_isInt ? "integer" : "number", true);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
    {
        if (_isInt) {
            JsonIntegerFormatVisitor v2 = visitor.expectIntegerFormat(typeHint);
            if (v2 != null) {
                v2.numberType(JsonParser.NumberType.BIG_INTEGER);
            }
        } else {
            JsonNumberFormatVisitor v2 = visitor.expectNumberFormat(typeHint);
            if (v2 != null) {
                Class<?> h = handledType();
                if (h == BigDecimal.class) {
                    v2.numberType(JsonParser.NumberType.BIG_DECIMAL);
                } // otherwise it's for Number... anything we could do there?
            }
        }
    }
}