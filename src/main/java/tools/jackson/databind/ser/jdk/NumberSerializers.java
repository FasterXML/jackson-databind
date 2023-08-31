package tools.jackson.databind.ser.jdk;

import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.core.io.NumberOutput;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Container class for serializers used for handling standard JDK-provided
 * types.
 */
public class NumberSerializers {
    protected NumberSerializers() { }

    public static void addAll(Map<String, ValueSerializer<?>> allDeserializers) {
        allDeserializers.put(Integer.class.getName(), new IntegerSerializer(Integer.class));
        allDeserializers.put(Integer.TYPE.getName(), new IntegerSerializer(Integer.TYPE));
        allDeserializers.put(Long.class.getName(), new LongSerializer(Long.class));
        allDeserializers.put(Long.TYPE.getName(), new LongSerializer(Long.TYPE));

        allDeserializers.put(Byte.class.getName(), IntLikeSerializer.instance);
        allDeserializers.put(Byte.TYPE.getName(), IntLikeSerializer.instance);
        allDeserializers.put(Short.class.getName(), ShortSerializer.instance);
        allDeserializers.put(Short.TYPE.getName(), ShortSerializer.instance);

        // Numbers, limited length floating point
        allDeserializers.put(Double.class.getName(), new DoubleSerializer(Double.class));
        allDeserializers.put(Double.TYPE.getName(), new DoubleSerializer(Double.TYPE));
        allDeserializers.put(Float.class.getName(), FloatSerializer.instance);
        allDeserializers.put(Float.TYPE.getName(), FloatSerializer.instance);
    }

    /*
    /**********************************************************
    /* Shared base class
    /**********************************************************
     */

    /**
     * Base class for actual primitive/wrapper value serializers.
     *<p>
     * NOTE: while you can extend this class yourself it is not designed as
     * an extension point, and as such is not part of public API. This means that
     * the compatibility across minor versions is only guaranteed on minor-to-minor
     * basis, and class methods may be changed and/or removed via deprecation
     * mechanism. Intent is, however, to allow for gradual upgrading so that methods
     * to remove are marked deprecated for at least one minor version.
     */
    public abstract static class Base<T> extends StdScalarSerializer<T>
    {
        protected final JsonParser.NumberType _numberType;
        protected final String _schemaType;
        protected final boolean _isInt;

        protected Base(Class<?> cls, JsonParser.NumberType numberType,
                String schemaType) {
            super(cls, false);
            _numberType = numberType;
            _schemaType = schemaType;
            _isInt = (numberType == JsonParser.NumberType.INT)
                    || (numberType == JsonParser.NumberType.LONG)
                    || (numberType == JsonParser.NumberType.BIG_INTEGER);
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor,
                JavaType typeHint)
        {
            if (_isInt) {
                visitIntFormat(visitor, typeHint, _numberType);
            } else {
                visitFloatFormat(visitor, typeHint, _numberType);
            }
        }

        @Override
        public ValueSerializer<?> createContextual(SerializerProvider prov,
                BeanProperty property)
        {
            JsonFormat.Value format = findFormatOverrides(prov, property, handledType());
            if (format != null) {
                switch (format.getShape()) {
                case STRING:
                    if (((Class<?>) handledType()) == BigDecimal.class) {
                        return NumberSerializer.bigDecimalAsStringSerializer();
                    }
                    return ToStringSerializer.instance;
                default:
                }
            }
            return this;
        }
    }

    /*
     *************************************************************
     * Concrete serializers, numerics
     *************************************************************
     */

    @JacksonStdImpl
    public static class ShortSerializer extends Base<Object> {
        final static ShortSerializer instance = new ShortSerializer();

        public ShortSerializer() {
            super(Short.class, JsonParser.NumberType.INT, "integer");
        }

        @Override
        public void serialize(Object value, JsonGenerator gen,
                SerializerProvider provider) throws JacksonException {
            gen.writeNumber((Short) value);
        }
    }

    /**
     * This is the special serializer for regular {@link java.lang.Integer}s
     * (and primitive ints)
     * <p>
     * Since this is one of "natural" types, no type information is ever included
     * on serialization (unlike for most scalar types, except for {@code double}).
     */
    @JacksonStdImpl
    public static class IntegerSerializer extends Base<Object> {
        public IntegerSerializer(Class<?> type) {
            super(type, JsonParser.NumberType.INT, "integer");
        }

        @Override
        public void serialize(Object value, JsonGenerator gen,
                SerializerProvider provider) throws JacksonException {
            gen.writeNumber((Integer) value);
        }

        // IMPORTANT: copied from `NonTypedScalarSerializerBase`
        @Override
        public void serializeWithType(Object value, JsonGenerator gen,
                SerializerProvider provider, TypeSerializer typeSer)
                throws JacksonException {
            // no type info, just regular serialization
            serialize(value, gen, provider);
        }
    }

    /**
     * Similar to {@link IntegerSerializer}, but will not cast to Integer:
     * instead, cast is to {@link java.lang.Number}, and conversion is by
     * calling {@link java.lang.Number#intValue}.
     */
    @JacksonStdImpl
    public static class IntLikeSerializer extends Base<Object> {
        final static IntLikeSerializer instance = new IntLikeSerializer();

        public IntLikeSerializer() {
            super(Number.class, JsonParser.NumberType.INT, "integer");
        }

        @Override
        public void serialize(Object value, JsonGenerator gen,
                SerializerProvider provider) throws JacksonException {
            gen.writeNumber(((Number) value).intValue());
        }
    }

    @JacksonStdImpl
    public static class LongSerializer extends Base<Object> {
        public LongSerializer(Class<?> cls) {
            super(cls, JsonParser.NumberType.LONG, "integer");
        }

        @Override
        public void serialize(Object value, JsonGenerator gen,
                SerializerProvider provider) throws JacksonException {
            gen.writeNumber((Long) value);
        }
    }

    @JacksonStdImpl
    public static class FloatSerializer extends Base<Object> {
        final static FloatSerializer instance = new FloatSerializer();

        public FloatSerializer() {
            super(Float.class, JsonParser.NumberType.FLOAT, "number");
        }

        @Override
        public void serialize(Object value, JsonGenerator gen,
                SerializerProvider provider) throws JacksonException {
            gen.writeNumber((Float) value);
        }
    }

    /**
     * This is the special serializer for regular {@link java.lang.Double}s (and
     * primitive doubles)
     * <p>
     * Since this is one of "native" types, no type information is ever included
     * on serialization (unlike for most scalar types as of 1.5)
     */
    @JacksonStdImpl
    public static class DoubleSerializer extends Base<Object> {
        public DoubleSerializer(Class<?> cls) {
            super(cls, JsonParser.NumberType.DOUBLE, "number");
        }

        @Override
        public void serialize(Object value, JsonGenerator gen,
                SerializerProvider provider) throws JacksonException
        {
            gen.writeNumber((Double) value);
        }

        // IMPORTANT: copied from `NonTypedScalarSerializerBase`
        @Override
        public void serializeWithType(Object value, JsonGenerator g,
                SerializerProvider ctxt, TypeSerializer typeSer)
            throws JacksonException
        {
            // no type info, just regular serialization
            // 08-Feb-2018, tatu: But as per [databind#2236], NaN values need special care
            Double d = (Double) value;
            if (NumberOutput.notFinite(d)) {
                WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                // whether to indicate it's number or string is arbitrary; important it is scalar
                        typeSer.typeId(value, JsonToken.VALUE_NUMBER_FLOAT));
                g.writeNumber(d);
                typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
            } else {
                g.writeNumber(d);
            }
        }
    }
}
