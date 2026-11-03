package tools.jackson.databind.ser.jdk;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.ArraySerializerBase;
import tools.jackson.databind.ser.std.StdContainerSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.type.TypeFactory;

/**
 * Dummy container class to group standard homogeneous array serializer implementations
 * (primitive arrays and String array).
 */
public class JDKArraySerializers
{
    protected final static HashMap<String, ValueSerializer<?>> _arraySerializers =
        new HashMap<>();
    static {
        // Arrays of various types (including common object types)
        _arraySerializers.put(boolean[].class.getName(), new BooleanArraySerializer());
        _arraySerializers.put(byte[].class.getName(), new ByteArraySerializer());
        _arraySerializers.put(char[].class.getName(), new CharArraySerializer());
        _arraySerializers.put(short[].class.getName(), new ShortArraySerializer());
        _arraySerializers.put(int[].class.getName(), new IntArraySerializer());
        _arraySerializers.put(long[].class.getName(), new LongArraySerializer());
        _arraySerializers.put(float[].class.getName(), new FloatArraySerializer());
        _arraySerializers.put(double[].class.getName(), new DoubleArraySerializer());
    }

    protected JDKArraySerializers() { }

    /**
     * Accessor for checking to see if there is a standard serializer for
     * given primitive value type.
     */
    public static ValueSerializer<?> findStandardImpl(SerializationContext ctxt,
            Class<?> cls, JsonFormat.Value formatOverrides) {
        if (formatOverrides != null) {
            if (formatOverrides.getShape() == JsonFormat.Shape.BINARY) {
                // Special handling for binary formats
                if (cls == float[].class) {
                    return BinaryFloatArraySerializer.instance;
                }
                if (cls == double[].class) {
                    return BinaryDoubleArraySerializer.instance;
                }
            }
        }
        return _arraySerializers.get(cls.getName());
    }

    static JavaType simpleElementType(Class<?> elemClass) {
        return TypeFactory.unsafeSimpleType(elemClass);
    }

    /*
     ****************************************************************
    /* Intermediate base classes
     ****************************************************************
     */

    /**
     * Intermediate base class used for cases where we may add
     * type information (excludes boolean/int/double arrays).
     */
    protected abstract static class TypedPrimitiveArraySerializer<T>
        extends ArraySerializerBase<T>
    {
        protected TypedPrimitiveArraySerializer(Class<T> cls) {
            super(cls);
        }

        /**
         * @since 3.1
         */
        protected TypedPrimitiveArraySerializer(TypedPrimitiveArraySerializer<T> src,
                BeanProperty prop, Boolean unwrapSingle,
                Object suppressableValue, boolean suppressNulls) {
            super(src, prop, unwrapSingle, suppressableValue, suppressNulls);
        }

        // 01-Dec-2016, tatu: Only now realized that due strong typing of Java arrays,
        //    we cannot really ever have value type serializers
        @Override
        public final StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
            // throw exception or just do nothing?
            return this;
        }
    }

    /*
    /****************************************************************
    /* Concrete serializers, arrays
    /****************************************************************
     */

    @JacksonStdImpl
    public static class BooleanArraySerializer
        extends ArraySerializerBase<boolean[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = simpleElementType(Boolean.TYPE);

        public BooleanArraySerializer() { super(boolean[].class); }

        /**
         * @since 3.1
         */
        protected BooleanArraySerializer(BooleanArraySerializer src,
                BeanProperty prop, Boolean unwrapSingle,
                Object suppressableValue, boolean suppressNulls) {
            super(src, prop, unwrapSingle, suppressableValue, suppressNulls);
        }

        @Override
        protected BooleanArraySerializer _withResolved(BeanProperty prop,
                Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
            return new BooleanArraySerializer(this, prop, unwrapSingle, suppressableValue, suppressNulls);
        }

        /**
         * Booleans never add type info; hence, even if type serializer is suggested,
         * we'll ignore it...
         */
        @Override
        public StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
            return this;
        }

        @Override
        public JavaType getContentType() {
            return VALUE_TYPE;
        }

        @Override
        public ValueSerializer<?> getContentSerializer() {
            // 14-Jan-2012, tatu: We could refer to an actual serializer if absolutely necessary
            return null;
        }

        @Override
        public boolean isEmpty(SerializationContext prov, boolean[] value) {
            return value.length == 0;
        }

        @Override
        public boolean hasSingleElement(boolean[] value) {
            return (value.length == 1);
        }

        @Override
        public final void serialize(boolean[] value, JsonGenerator g, SerializationContext ctxt) throws JacksonException
        {
            final int len = value.length;
            if ((len == 1) && _shouldUnwrapSingle(ctxt)) {
                serializeContents(value, g, ctxt);
                return;
            }
            g.writeStartArray(value, len);
            serializeContents(value, g, ctxt);
            g.writeEndArray();
        }

        @Override
        public void serializeContents(boolean[] value, JsonGenerator g, SerializationContext ctxt)
            throws JacksonException
        {
            final boolean filtered = _needToCheckFiltering(ctxt);
            for (int i = 0, len = value.length; i < len; ++i) {
                if (filtered && !_shouldSerializeElement(ctxt, Boolean.valueOf(value[i]))) {
                    continue;
                }
                g.writeBoolean(value[i]);
            }
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
            visitArrayFormat(visitor, typeHint, JsonFormatTypes.BOOLEAN);
        }
    }

    @JacksonStdImpl
    public static class ShortArraySerializer extends TypedPrimitiveArraySerializer<short[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = simpleElementType(Short.TYPE);

        public ShortArraySerializer() { super(short[].class); }

        /**
         * @since 3.1
         */
        protected ShortArraySerializer(ShortArraySerializer src, BeanProperty prop,
                Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
            super(src, prop, unwrapSingle, suppressableValue, suppressNulls);
        }

        @Override
        protected ShortArraySerializer _withResolved(BeanProperty prop,
                Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
            return new ShortArraySerializer(this, prop, unwrapSingle, suppressableValue, suppressNulls);
        }

        @Override
        public JavaType getContentType() {
            return VALUE_TYPE;
        }

        @Override
        public ValueSerializer<?> getContentSerializer() {
            // 14-Jan-2012, tatu: We could refer to an actual serializer if absolutely necessary
            return null;
        }

        @Override
        public boolean isEmpty(SerializationContext prov, short[] value) {
            return value.length == 0;
        }

        @Override
        public boolean hasSingleElement(short[] value) {
            return (value.length == 1);
        }

        @Override
        public final void serialize(short[] value, JsonGenerator g, SerializationContext ctxt) throws JacksonException
        {
            final int len = value.length;
            if ((len == 1) && _shouldUnwrapSingle(ctxt)) {
                serializeContents(value, g, ctxt);
                return;
            }
            g.writeStartArray(value, len);
            serializeContents(value, g, ctxt);
            g.writeEndArray();
        }

        @Override
        public void serializeContents(short[] value, JsonGenerator g, SerializationContext ctxt)
            throws JacksonException
        {
            final boolean filtered = _needToCheckFiltering(ctxt);
            for (int i = 0, len = value.length; i < len; ++i) {
                if (filtered && !_shouldSerializeElement(ctxt, Short.valueOf(value[i]))) {
                    continue;
                }
                g.writeNumber((int)value[i]);
            }
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
            visitArrayFormat(visitor, typeHint, JsonFormatTypes.INTEGER);
        }
    }

    /**
     * Character arrays are different from other integral number arrays in that
     * they are most likely to be textual data, and should be written as
     * Strings, not arrays of entries.
     *<p>
     * NOTE: since it is NOT serialized as an array, cannot use AsArraySerializer as base
     */
    @JacksonStdImpl
    public static class CharArraySerializer extends StdSerializer<char[]>
    {
        public CharArraySerializer() { super(char[].class); }

        @Override
        public boolean isEmpty(SerializationContext prov, char[] value) {
            return value.length == 0;
        }

        @Override
        public void serialize(char[] value, JsonGenerator g, SerializationContext ctxt)
            throws JacksonException
        {
            // [JACKSON-289] allows serializing as 'sparse' char array too:
            if (ctxt.isEnabled(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS)) {
                g.writeStartArray(value, value.length);
                _writeArrayContents(g, value);
                g.writeEndArray();
            } else {
                g.writeString(value, 0, value.length);
            }
        }

        @Override
        public void serializeWithType(char[] value, JsonGenerator g, SerializationContext ctxt,
                TypeSerializer typeSer)
            throws JacksonException
        {
            // [JACKSON-289] allows serializing as 'sparse' char array too:
            final boolean asArray = ctxt.isEnabled(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS);
            WritableTypeId typeIdDef;
            if (asArray) {
                typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                        typeSer.typeId(value, JsonToken.START_ARRAY));
                _writeArrayContents(g, value);
            } else { // default is to write as simple String
                typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                        typeSer.typeId(value, JsonToken.VALUE_STRING));
                g.writeString(value, 0, value.length);
            }
            typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
        }

        private final void _writeArrayContents(JsonGenerator g, char[] value)
            throws JacksonException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                g.writeString(value, i, 1);
            }
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
            visitArrayFormat(visitor, typeHint, JsonFormatTypes.STRING);
        }
    }

    @JacksonStdImpl
    public static class IntArraySerializer extends ArraySerializerBase<int[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = simpleElementType(Integer.TYPE);

        public IntArraySerializer() { super(int[].class); }

        /**
         * @since 3.1
         */
        protected IntArraySerializer(IntArraySerializer src,
                BeanProperty prop, Boolean unwrapSingle,
                Object suppressableValue, boolean suppressNulls) {
            super(src, prop, unwrapSingle, suppressableValue, suppressNulls);
        }

        @Override
        protected IntArraySerializer _withResolved(BeanProperty prop,
                Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
            return new IntArraySerializer(this, prop, unwrapSingle, suppressableValue, suppressNulls);
        }

        /**
         * Ints never add type info; hence, even if type serializer is suggested,
         * we'll ignore it...
         */
        @Override
        public StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
            return this;
        }

        @Override
        public JavaType getContentType() {
            return VALUE_TYPE;
        }

        @Override
        public ValueSerializer<?> getContentSerializer() {
            // 14-Jan-2012, tatu: We could refer to an actual serializer if absolutely necessary
            return null;
        }

        @Override
        public boolean isEmpty(SerializationContext prov, int[] value) {
            return value.length == 0;
        }

        @Override
        public boolean hasSingleElement(int[] value) {
            return (value.length == 1);
        }

        @Override
        public final void serialize(int[] value, JsonGenerator g, SerializationContext ctxt) throws JacksonException
        {
            final int len = value.length;
            if ((len == 1) && _shouldUnwrapSingle(ctxt)) {
                serializeContents(value, g, ctxt);
                return;
            }
            // Check if filtering is needed
            if (_needToCheckFiltering(ctxt)) {
                g.writeStartArray(value, len);
                serializeContents(value, g, ctxt);
                g.writeEndArray();
                return;
            }
            // 11-May-2016, tatu: As per [core#277] we have efficient `writeArray(...)` available
            g.writeArray(value, 0, value.length);
        }

        @Override
        public void serializeContents(int[] value, JsonGenerator g, SerializationContext ctxt)
            throws JacksonException
        {
            final boolean filtered = _needToCheckFiltering(ctxt);
            for (int i = 0, len = value.length; i < len; ++i) {
                if (filtered && !_shouldSerializeElement(ctxt, Integer.valueOf(value[i]))) {
                    continue;
                }
                g.writeNumber(value[i]);
            }
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
            visitArrayFormat(visitor, typeHint, JsonFormatTypes.INTEGER);
        }
    }

    @JacksonStdImpl
    public static class LongArraySerializer extends TypedPrimitiveArraySerializer<long[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = simpleElementType(Long.TYPE);

        public LongArraySerializer() { super(long[].class); }

        /**
         * @since 3.1
         */
        protected LongArraySerializer(LongArraySerializer src, BeanProperty prop,
                Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
            super(src, prop, unwrapSingle, suppressableValue, suppressNulls);
        }

        @Override
        protected LongArraySerializer _withResolved(BeanProperty prop,
                Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
            return new LongArraySerializer(this, prop, unwrapSingle, suppressableValue, suppressNulls);
        }

        @Override
        public JavaType getContentType() {
            return VALUE_TYPE;
        }

        @Override
        public ValueSerializer<?> getContentSerializer() {
            // 14-Jan-2012, tatu: We could refer to an actual serializer if absolutely necessary
            return null;
        }

        @Override
        public boolean isEmpty(SerializationContext prov, long[] value) {
            return value.length == 0;
        }

        @Override
        public boolean hasSingleElement(long[] value) {
            return (value.length == 1);
        }

        @Override
        public final void serialize(long[] value, JsonGenerator g, SerializationContext ctxt) throws JacksonException
        {
            final int len = value.length;
            if ((len == 1) && _shouldUnwrapSingle(ctxt)) {
                serializeContents(value, g, ctxt);
                return;
            }
            // Check if filtering is needed
            if (_needToCheckFiltering(ctxt)) {
                g.writeStartArray(value, len);
                serializeContents(value, g, ctxt);
                g.writeEndArray();
                return;
            }
            // 11-May-2016, tatu: As per [core#277] we have efficient `writeArray(...)` available
            g.writeArray(value, 0, value.length);
        }

        @Override
        public void serializeContents(long[] value, JsonGenerator g, SerializationContext ctxt)
            throws JacksonException
        {
            final boolean filtered = _needToCheckFiltering(ctxt);
            for (int i = 0, len = value.length; i < len; ++i) {
                if (filtered && !_shouldSerializeElement(ctxt, Long.valueOf(value[i]))) {
                    continue;
                }
                g.writeNumber(value[i]);
            }
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
            visitArrayFormat(visitor, typeHint, JsonFormatTypes.NUMBER);
        }
    }

    @JacksonStdImpl
    public static class FloatArraySerializer extends TypedPrimitiveArraySerializer<float[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = simpleElementType(Float.TYPE);

        // @since 2.20
        final static FloatArraySerializer instance = new FloatArraySerializer();

        public FloatArraySerializer() {
            super(float[].class);
        }

        /**
         * @since 3.1
         */
        protected FloatArraySerializer(FloatArraySerializer src, BeanProperty prop,
                Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
            super(src, prop, unwrapSingle, suppressableValue, suppressNulls);
        }

        @Override
        protected FloatArraySerializer _withResolved(BeanProperty prop,
                Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
            return new FloatArraySerializer(this, prop, unwrapSingle, suppressableValue, suppressNulls);
        }

        @Override
        public JavaType getContentType() {
            return VALUE_TYPE;
        }

        @Override
        public ValueSerializer<?> getContentSerializer() {
            // 14-Jan-2012, tatu: We could refer to an actual serializer if absolutely necessary
            return null;
        }

        @Override
        public boolean isEmpty(SerializationContext prov, float[] value) {
            return value.length == 0;
        }

        @Override
        public boolean hasSingleElement(float[] value) {
            return (value.length == 1);
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property)
        {
            JsonFormat.Value format = findFormatOverrides(ctxt, property,
                    handledType());
            if (format != null) {
                if (format.getShape() == JsonFormat.Shape.BINARY) {
                    return BinaryFloatArraySerializer.instance;
                }
            }
            return super.createContextual(ctxt, property);
        }

        @Override
        public final void serialize(float[] value, JsonGenerator g, SerializationContext ctxt) throws JacksonException
        {
            final int len = value.length;
            if ((len == 1) && _shouldUnwrapSingle(ctxt)) {
                serializeContents(value, g, ctxt);
                return;
            }
            g.writeStartArray(value, len);
            serializeContents(value, g, ctxt);
            g.writeEndArray();
        }

        @Override
        public void serializeContents(float[] value, JsonGenerator g, SerializationContext ctxt)
            throws JacksonException
        {
            final boolean filtered = _needToCheckFiltering(ctxt);
            for (int i = 0, len = value.length; i < len; ++i) {
                if (filtered && !_shouldSerializeElement(ctxt, Float.valueOf(value[i]))) {
                    continue;
                }
                g.writeNumber(value[i]);
            }
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
            visitArrayFormat(visitor, typeHint, JsonFormatTypes.NUMBER);
        }
    }

    @JacksonStdImpl
    public static class DoubleArraySerializer extends ArraySerializerBase<double[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = simpleElementType(Double.TYPE);

        // @since 2.20
        final static DoubleArraySerializer instance = new DoubleArraySerializer();

        public DoubleArraySerializer() { super(double[].class); }

        /**
         * @since 3.1
         */
        protected DoubleArraySerializer(DoubleArraySerializer src,
                BeanProperty prop, Boolean unwrapSingle,
                Object suppressableValue, boolean suppressNulls) {
            super(src, prop, unwrapSingle, suppressableValue, suppressNulls);
        }

        @Override
        protected DoubleArraySerializer _withResolved(BeanProperty prop,
                Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
            return new DoubleArraySerializer(this, prop, unwrapSingle, suppressableValue, suppressNulls);
        }

        /**
         * Doubles never add type info; hence, even if type serializer is suggested,
         * we'll ignore it...
         */
        @Override
        public StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
            return this;
        }

        @Override
        public JavaType getContentType() {
            return VALUE_TYPE;
        }

        @Override
        public ValueSerializer<?> getContentSerializer() {
            // 14-Jan-2012, tatu: We could refer to an actual serializer if absolutely necessary
            return null;
        }

        @Override
        public boolean isEmpty(SerializationContext prov, double[] value) {
            return value.length == 0;
        }

        @Override
        public boolean hasSingleElement(double[] value) {
            return (value.length == 1);
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property)
        {
            JsonFormat.Value format = findFormatOverrides(ctxt, property,
                    handledType());
            if (format != null) {
                if (format.getShape() == JsonFormat.Shape.BINARY) {
                    return BinaryDoubleArraySerializer.instance;
                }
            }
            return super.createContextual(ctxt, property);
        }

        @Override
        public void serialize(double[] value, JsonGenerator g, SerializationContext ctxt)
            throws JacksonException
        {
            final int len = value.length;
            if ((len == 1) && _shouldUnwrapSingle(ctxt)) {
                serializeContents(value, g, ctxt);
                return;
            }
            // Check if filtering is needed
            if (_needToCheckFiltering(ctxt)) {
                g.writeStartArray(value, len);
                serializeContents(value, g, ctxt);
                g.writeEndArray();
                return;
            }
            // 11-May-2016, tatu: As per [core#277] we have efficient `writeArray(...)` available
            g.writeArray(value, 0, value.length);
        }

        @Override
        public void serializeContents(double[] value, JsonGenerator g, SerializationContext ctxt) throws JacksonException
        {
            final boolean filtered = _needToCheckFiltering(ctxt);
            for (int i = 0, len = value.length; i < len; ++i) {
                if (filtered && !_shouldSerializeElement(ctxt, Double.valueOf(value[i]))) {
                    continue;
                }
                g.writeNumber(value[i]);
            }
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
            visitArrayFormat(visitor, typeHint, JsonFormatTypes.NUMBER);
        }
    }

    /*
    /**********************************************************************
    /* Concrete serializers, alternative "binary Vector" representations
    /**********************************************************************
     */

    /**
     * Alternative serializer for arrays of primitive floats, using "packed binary"
     * representation ("binary vector") instead of JSON array.
     *
     * @since 2.20
     */
    @JacksonStdImpl
    public static class BinaryFloatArraySerializer extends StdSerializer<float[]>
    {
        final static BinaryFloatArraySerializer instance = new BinaryFloatArraySerializer();
        
        public BinaryFloatArraySerializer() {
            super(float[].class);
        }

        @Override
        public boolean isEmpty(SerializationContext ctxt, float[] value) {
            return value.length == 0;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property)
        {
            JsonFormat.Value format = findFormatOverrides(ctxt, property,
                    handledType());
            if (format != null) {
                switch (format.getShape()) {
                case ARRAY:
                case NATURAL:
                    return FloatArraySerializer.instance;
                default:
                }
            }
            return this;
        }

        @Override
        public void serialize(float[] value, JsonGenerator g, SerializationContext ctxt)
            throws JacksonException
        {
            // First: "pack" the floats into bytes
            final int vectorLen = value.length;
            final byte[] b = new byte[vectorLen * 4];
            for (int i = 0, out = 0; i < vectorLen; i++) {
                final int floatBits = Float.floatToIntBits(value[i]);
                b[out++] = (byte) (floatBits >> 24);
                b[out++] = (byte) (floatBits >> 16);
                b[out++] = (byte) (floatBits >> 8);
                b[out++] = (byte) (floatBits);
            }
            // Second: write packed bytes (for JSON, Base64 encoded)
            g.writeBinary(ctxt.getConfig().getBase64Variant(),
                    b, 0, b.length);
        }

        @Override
        public void serializeWithType(float[] value, JsonGenerator g, SerializationContext ctxt,
                TypeSerializer typeSer)
            throws JacksonException
        {
            // most likely scalar
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                    typeSer.typeId(value, JsonToken.VALUE_EMBEDDED_OBJECT));
            serialize(value, g, ctxt);
            typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
            // 06-Aug-2025, tatu: while logically (and within JVM) binary, gets encoded as Base64 String,
            // let's try to indicate it is array of Float... difficult, thanks to JSON Schema's
            // lackluster listing of types
            visitArrayFormat(visitor, typeHint, JsonFormatTypes.NUMBER);
        }
    }

    /**
     * Alternative serializer for arrays of primitive doubles, using "packed binary"
     * representation ("binary vector") instead of JSON array.
     *
     * @since 2.20
     */
    @JacksonStdImpl
    public static class BinaryDoubleArraySerializer extends StdSerializer<double[]>
    {
        final static BinaryDoubleArraySerializer instance = new BinaryDoubleArraySerializer();
        
        public BinaryDoubleArraySerializer() {
            super(double[].class);
        }

        @Override
        public boolean isEmpty(SerializationContext ctxt, double[] value) {
            return value.length == 0;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property)
        {
            JsonFormat.Value format = findFormatOverrides(ctxt, property,
                    handledType());
            if (format != null) {
                switch (format.getShape()) {
                case ARRAY:
                case NATURAL:
                    return DoubleArraySerializer.instance;
                default:
                }
            }
            return this;
        }

        @Override
        public void serialize(double[] value, JsonGenerator g, SerializationContext ctxt)
            throws JacksonException
        {
            // First: "pack" the floats into bytes
            final int vectorLen = value.length;
            final byte[] b = new byte[vectorLen * 8];
            for (int i = 0, out = 0; i < vectorLen; i++) {
                long bits = Double.doubleToLongBits(value[i]);
                final int hi = (int) (bits >> 32);
                b[out] = (byte) (hi >> 24);
                b[out+1] = (byte) (hi >> 16);
                b[out+2] = (byte) (hi >> 8);
                b[out+3] = (byte) (hi);
                final int lo = (int) bits;
                b[out+4] = (byte) (lo >> 24);
                b[out+5] = (byte) (lo >> 16);
                b[out+6] = (byte) (lo >> 8);
                b[out+7] = (byte) (lo);
                out += 8;
            }
            // Second: write packed bytes (for JSON, Base64 encoded)
            g.writeBinary(ctxt.getConfig().getBase64Variant(),
                    b, 0, b.length);
        }

        @Override
        public void serializeWithType(double[] value, JsonGenerator g, SerializationContext ctxt,
                TypeSerializer typeSer)
            throws JacksonException
        {
            // most likely scalar
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                    typeSer.typeId(value, JsonToken.VALUE_EMBEDDED_OBJECT));
            serialize(value, g, ctxt);
            typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
            throws JacksonException
        {
            // 14-Mar-2016, tatu: while logically (and within JVM) binary, gets encoded as Base64 String,
            // let's try to indicate it is array of Float... difficult, thanks to JSON Schema's
            // lackluster listing of types
            visitArrayFormat(visitor, typeHint, JsonFormatTypes.NUMBER);
        }
    }
}
