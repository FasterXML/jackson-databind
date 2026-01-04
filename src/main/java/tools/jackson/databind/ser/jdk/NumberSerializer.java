package tools.jackson.databind.ser.jdk;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.databind.ser.std.ToStringSerializerBase;

/**
 * As a fallback, we may need to use this serializer for other
 * types of {@link Number}s: both custom types and "big" numbers
 * like {@link BigInteger} and {@link BigDecimal}.
 */
@JacksonStdImpl
public class NumberSerializer
    extends StdScalarSerializer<Number>
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
    public ValueSerializer<?> createContextual(SerializationContext ctxt,
            BeanProperty property)
    {
        JsonFormat.Value format = findFormatOverrides(ctxt, property, handledType());
        if (format != null) {
            switch (format.getShape()) {
            case STRING:
                // [databind#2264]: Need special handling for `BigDecimal`
                if (handledType() == BigDecimal.class) {
                    return bigDecimalAsStringSerializer();
                }
                return NumberSerializer.createStringSerializer(ctxt, format, _isInt);
            default:
            }
        }
        return this;
    }

    @Override
    public void serialize(Number value, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        // should mostly come in as one of these two:
        if (value instanceof BigDecimal bd) {
            g.writeNumber(bd);
        } else if (value instanceof BigInteger bi) {
            g.writeNumber(bi);

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
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
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
     * Method used to create a string serializer for a number. If the number is integer, and configuration is set properly,
     * we create an alternative radix serializer {@link NumberToStringWithRadixSerializer}.
     *
     * @since 3.1
     */
    public static ToStringSerializerBase createStringSerializer(SerializationContext ctxt,
            JsonFormat.Value format, boolean isInt) {
        if (isInt && _hasRadixOverride(format)) {
            int radix = format.getRadix();
            return new NumberToStringWithRadixSerializer(radix);
        }
        return ToStringSerializer.instance;
    }

    /**
     * Check if we have a proper {@link JsonFormat} annotation for serializing a number
     * using an alternative radix specified in the annotation.
     *
     * @since 3.1
     */
    private static boolean _hasRadixOverride(JsonFormat.Value format) {
        return format.hasNonDefaultRadix();
    }

    public static ValueSerializer<?> bigDecimalAsStringSerializer() {
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
        public boolean isEmpty(SerializationContext prov, Object value) {
            // As per [databind#2513], should not delegate; also, never empty (numbers do
            // have "default value" to filter by, just not "empty"
            return false;
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt)
            throws JacksonException
        {
            final String text;
            if (gen.isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)) {
                final BigDecimal bd = (BigDecimal) value;
                // 24-Aug-2016, tatu: [core#315] prevent possible DoS vector, so we need this
                if (!_verifyBigDecimalRange(gen, bd)) {
                    // ... but wouldn't it be nice to trigger error via generator? Alas,
                    // no method to do that. So we'll do...
                    final String errorMsg = String.format(
"Attempt to write plain `java.math.BigDecimal` (see JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN) with illegal scale (%d): needs to be between [-%d, %d]",
bd.scale(), MAX_BIG_DECIMAL_SCALE, MAX_BIG_DECIMAL_SCALE);
                    ctxt.reportMappingProblem(errorMsg);
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
        protected boolean _verifyBigDecimalRange(JsonGenerator gen, BigDecimal value) {
            int scale = value.scale();
            return ((scale >= -MAX_BIG_DECIMAL_SCALE) && (scale <= MAX_BIG_DECIMAL_SCALE));
        }
    }
}
