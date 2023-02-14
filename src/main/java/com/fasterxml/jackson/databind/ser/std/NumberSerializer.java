package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * As a fallback, we may need to use this serializer for other
 * types of {@link Number}s: both custom types and "big" numbers
 * like {@link BigInteger} and {@link BigDecimal}.
 */
@JacksonStdImpl
@SuppressWarnings("serial")
public class NumberSerializer
    extends StdScalarSerializer<Number>
    implements ContextualSerializer
{
    /**
     * Static instance that is only to be used for {@link java.lang.Number}.
     */
    public final static NumberSerializer instance = new NumberSerializer(Number.class);

    /**
     * Copied from `jackson-core` class `GeneratorBase`
     */
    protected final static int MAX_BIG_DECIMAL_SCALE = 9999;

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
                // [databind#2264]: Need special handling for `BigDecimal`
                if (((Class<?>) handledType()) == BigDecimal.class) {
                    return bigDecimalAsStringSerializer();
                }
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

    /**
     * @deprecated Since 2.15
     */
    @Deprecated
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
        return createSchemaNode(_isInt ? "integer" : "number", true);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
    {
        if (_isInt) {
            visitIntFormat(visitor, typeHint, JsonParser.NumberType.BIG_INTEGER);
        } else {
            if (((Class<?>) handledType()) == BigDecimal.class) {
                visitFloatFormat(visitor, typeHint, JsonParser.NumberType.BIG_DECIMAL);
            } else {
                // otherwise bit unclear what to call... but let's try:
                /*JsonNumberFormatVisitor v2 =*/ visitor.expectNumberFormat(typeHint);
            }
        }
    }

    /**
     * @since 2.10
     */
    public static JsonSerializer<?> bigDecimalAsStringSerializer() {
        return BigDecimalAsStringSerializer.BD_INSTANCE;
    }

    final static class BigDecimalAsStringSerializer
        extends ToStringSerializerBase
    {
        final static BigDecimalAsStringSerializer BD_INSTANCE = new BigDecimalAsStringSerializer();

        public BigDecimalAsStringSerializer() {
            super(BigDecimal.class);
        }

        @Override
        public boolean isEmpty(SerializerProvider prov, Object value) {
            // As per [databind#2513], should not delegate; also, never empty (numbers do
            // have "default value" to filter by, just not "empty"
            return false;
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider)
            throws IOException
        {
            final String text;
            if (gen.isEnabled(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)) {
                final BigDecimal bd = (BigDecimal) value;
                // 24-Aug-2016, tatu: [core#315] prevent possible DoS vector, so we need this
                if (!_verifyBigDecimalRange(gen, bd)) {
                    // ... but wouldn't it be nice to trigger error via generator? Alas,
                    // no method to do that. So we'll do...
                    final String errorMsg = String.format(
"Attempt to write plain `java.math.BigDecimal` (see JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN) with illegal scale (%d): needs to be between [-%d, %d]",
bd.scale(), MAX_BIG_DECIMAL_SCALE, MAX_BIG_DECIMAL_SCALE);
                    provider.reportMappingProblem(errorMsg);
                }
                text = bd.toPlainString();
            } else {
                text = value.toString();
            }
            gen.writeString(text);
        }

        @Override
        public String valueToString(Object value) {
            // should never be called
            throw new IllegalStateException();
        }

        // 24-Aug-2016, tatu: [core#315] prevent possible DoS vector, so we need this
        protected boolean _verifyBigDecimalRange(JsonGenerator gen, BigDecimal value) throws IOException {
            int scale = value.scale();
            return ((scale >= -MAX_BIG_DECIMAL_SCALE) && (scale <= MAX_BIG_DECIMAL_SCALE));
        }
    }
}
