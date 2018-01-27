package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;

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

    /**
     * @since 2.5
     */
    public NumberSerializer(Class<? extends Number> rawType) {
        super(rawType, false);
        // since this will NOT be constructed for Integer or Long, only case is:
        _isInt = (rawType == BigInteger.class);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov,
            BeanProperty property) throws JsonMappingException
    {
        JsonFormat.Value format = findFormatOverrides(prov, property, handledType());
        if (format != null) {
            switch (format.getShape()) {
            case STRING:
                return ToStringSerializer.instance;
            default:
            }
        }
        return this;
    }

    @Override
    public void serialize(Number value, JsonGenerator g, SerializerProvider provider) throws IOException
    {
        // should mostly come in as one of these two:
        if (value instanceof BigDecimal) {
            g.writeNumber((BigDecimal) value);
        } else if (value instanceof BigInteger) {
            g.writeNumber((BigInteger) value);
            
        // These should not occur, as more specific methods should have been called; but
        // just in case let's cover all bases:
        } else if (value instanceof Long) {
            g.writeNumber(value.longValue());
        } else if (value instanceof Double) {
            g.writeNumber(value.doubleValue());
        } else if (value instanceof Float) {
            g.writeNumber(value.floatValue());
        } else if (value instanceof Integer || value instanceof Byte || value instanceof Short) {
            g.writeNumber(value.intValue()); // doesn't need to be cast to smaller numbers
        } else {
            // We'll have to use fallback "untyped" number write method
            g.writeNumber(value.toString());
        }
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
    {
        if (_isInt) {
            visitIntFormat(visitor, typeHint, JsonParser.NumberType.BIG_INTEGER);
        } else {
            Class<?> h = handledType();
            if (h == BigDecimal.class) {
                visitFloatFormat(visitor, typeHint, JsonParser.NumberType.BIG_DECIMAL);
            } else {
                // otherwise bit unclear what to call... but let's try:
                /*JsonNumberFormatVisitor v2 =*/ visitor.expectNumberFormat(typeHint);
            }
        }
    }
}