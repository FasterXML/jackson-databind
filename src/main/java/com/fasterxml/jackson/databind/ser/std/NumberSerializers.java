package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.NumberOutput;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * Container class for serializers used for handling standard JDK-provided
 * primitve number types and their wrapper counterparts (like {@link java.lang.Integer}).
 */
@SuppressWarnings("serial")
public class NumberSerializers {
    protected NumberSerializers() { }

    public static void addAll(Map<String, JsonSerializer<?>> allDeserializers) {
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
     * Shared base class for actual primitive/wrapper number serializers.
     * Note that this class is not meant as general-purpose base class nor
     * is it part of public API: you may extend it with the caveat that not
     * being part of public API its implementation and interfaces may change
     * in minor releases; however deprecation markers will be used to allow
     * code evolution.
     *<p>
     * NOTE: {@code public} since 2.10: previously had {@code protected} access.
     */
    public abstract static class Base<T> extends StdScalarSerializer<T>
            implements ContextualSerializer
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

        /**
         * @deprecated Since 2.15
         */
        @Deprecated
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
            return createSchemaNode(_schemaType, true);
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor,
                JavaType typeHint) throws JsonMappingException
        {
            if (_isInt) {
                visitIntFormat(visitor, typeHint, _numberType);
            } else {
                visitFloatFormat(visitor, typeHint, _numberType);
            }
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider prov,
                BeanProperty property) throws JsonMappingException
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
                SerializerProvider provider) throws IOException {
            gen.writeNumber(((Short) value).shortValue());
        }
    }

    /**
     * This is the special serializer for regular {@link java.lang.Integer}s
     * (and primitive ints)
     * <p>
     * Since this is one of "natural" types, no type information is ever included
     * on serialization (unlike for most scalar types, except for {@code double}).
     * <p>
     * NOTE: as of 2.6, generic signature changed to Object, to avoid generation
     * of bridge methods.
     */
    @JacksonStdImpl
    public static class IntegerSerializer extends Base<Object> {
        public IntegerSerializer(Class<?> type) {
            super(type, JsonParser.NumberType.INT, "integer");
        }

        @Override
        public void serialize(Object value, JsonGenerator gen,
                SerializerProvider provider) throws IOException {
            gen.writeNumber(((Integer) value).intValue());
        }

        // IMPORTANT: copied from `NonTypedScalarSerializerBase`
        @Override
        public void serializeWithType(Object value, JsonGenerator gen,
                SerializerProvider provider, TypeSerializer typeSer)
                throws IOException {
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
                SerializerProvider provider) throws IOException {
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
                SerializerProvider provider) throws IOException {
            gen.writeNumber(((Long) value).longValue());
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
                SerializerProvider provider) throws IOException {
            gen.writeNumber(((Float) value).floatValue());
        }
    }

    /**
     * This is the special serializer for regular {@link java.lang.Double}s (and
     * primitive doubles)
     * <p>
     * Since this is one of "native" types, no type information is ever included
     * on serialization (unlike for most scalar types other than {@code long}).
     */
    @JacksonStdImpl
    public static class DoubleSerializer extends Base<Object> {
        public DoubleSerializer(Class<?> cls) {
            super(cls, JsonParser.NumberType.DOUBLE, "number");
        }

        @Override
        public void serialize(Object value, JsonGenerator gen,
                SerializerProvider provider) throws IOException
        {
            gen.writeNumber(((Double) value).doubleValue());
        }

        // IMPORTANT: copied from `NonTypedScalarSerializerBase`
        @Override
        public void serializeWithType(Object value, JsonGenerator g,
                SerializerProvider provider, TypeSerializer typeSer)
            throws IOException
        {
            // 08-Feb-2018, tatu: [databind#2236], NaN values need special care
            Double d = (Double) value;
            if (NumberOutput.notFinite(d)) {
                WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                        // whether to indicate it's number or string is arbitrary; important it is scalar
                        typeSer.typeId(value, JsonToken.VALUE_NUMBER_FLOAT));
                g.writeNumber(d);
                typeSer.writeTypeSuffix(g, typeIdDef);
            } else {
                g.writeNumber(d);
            }
        }

        @Deprecated // since 2.13, call NumberOutput.notFinite() directly
        public static boolean notFinite(double value) {
            return NumberOutput.notFinite(value);
        }
    }
}
