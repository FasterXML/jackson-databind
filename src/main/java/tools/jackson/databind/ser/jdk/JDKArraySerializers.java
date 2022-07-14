package tools.jackson.databind.ser.jdk;

import java.util.HashMap;

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
        new HashMap<String, ValueSerializer<?>>();
    static {
        // Arrays of various types (including common object types)
        _arraySerializers.put(boolean[].class.getName(), new JDKArraySerializers.BooleanArraySerializer());
        _arraySerializers.put(byte[].class.getName(), new ByteArraySerializer());
        _arraySerializers.put(char[].class.getName(), new JDKArraySerializers.CharArraySerializer());
        _arraySerializers.put(short[].class.getName(), new JDKArraySerializers.ShortArraySerializer());
        _arraySerializers.put(int[].class.getName(), new JDKArraySerializers.IntArraySerializer());
        _arraySerializers.put(long[].class.getName(), new JDKArraySerializers.LongArraySerializer());
        _arraySerializers.put(float[].class.getName(), new JDKArraySerializers.FloatArraySerializer());
        _arraySerializers.put(double[].class.getName(), new JDKArraySerializers.DoubleArraySerializer());
    }

    protected JDKArraySerializers() { }

    /**
     * Accessor for checking to see if there is a standard serializer for
     * given primitive value type.
     */
    public static ValueSerializer<?> findStandardImpl(Class<?> cls) {
        return _arraySerializers.get(cls.getName());
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

        protected TypedPrimitiveArraySerializer(TypedPrimitiveArraySerializer<T> src,
                BeanProperty prop, Boolean unwrapSingle) {
            super(src, prop, unwrapSingle);
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
        @SuppressWarnings("deprecation")
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Boolean.class);

        public BooleanArraySerializer() { super(boolean[].class); }

        protected BooleanArraySerializer(BooleanArraySerializer src,
                BeanProperty prop, Boolean unwrapSingle) {
            super(src, prop, unwrapSingle);
        }
        
        @Override
        public ValueSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
            return new BooleanArraySerializer(this, prop, unwrapSingle);
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
        public boolean isEmpty(SerializerProvider prov, boolean[] value) {
            return value.length == 0;
        }

        @Override
        public boolean hasSingleElement(boolean[] value) {
            return (value.length == 1);
        }

        @Override
        public final void serialize(boolean[] value, JsonGenerator g, SerializerProvider provider) throws JacksonException
        {
            final int len = value.length;
            if ((len == 1) && _shouldUnwrapSingle(provider)) {
                serializeContents(value, g, provider);
                return;
            }
            g.writeStartArray(value, len);
            serializeContents(value, g, provider);
            g.writeEndArray();
        }

        @Override
        public void serializeContents(boolean[] value, JsonGenerator g, SerializerProvider provider)
            throws JacksonException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
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
        @SuppressWarnings("deprecation")
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Short.TYPE);

        public ShortArraySerializer() { super(short[].class); }
        public ShortArraySerializer(ShortArraySerializer src, BeanProperty prop,
                 Boolean unwrapSingle) {
            super(src, prop, unwrapSingle);
        }

        @Override
        public ValueSerializer<?> _withResolved(BeanProperty prop,Boolean unwrapSingle) {
            return new ShortArraySerializer(this, prop, unwrapSingle);
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
        public boolean isEmpty(SerializerProvider prov, short[] value) {
            return value.length == 0;
        }

        @Override
        public boolean hasSingleElement(short[] value) {
            return (value.length == 1);
        }

        @Override
        public final void serialize(short[] value, JsonGenerator g, SerializerProvider provider) throws JacksonException
        {
            final int len = value.length;
            if ((len == 1) && _shouldUnwrapSingle(provider)) {
                serializeContents(value, g, provider);
                return;
            }
            g.writeStartArray(value, len);
            serializeContents(value, g, provider);
            g.writeEndArray();
        }

        @Override
        public void serializeContents(short[] value, JsonGenerator g, SerializerProvider provider)
            throws JacksonException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
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
        public boolean isEmpty(SerializerProvider prov, char[] value) {
            return value.length == 0;
        }
        
        @Override
        public void serialize(char[] value, JsonGenerator g, SerializerProvider provider)
            throws JacksonException
        {
            // [JACKSON-289] allows serializing as 'sparse' char array too:
            if (provider.isEnabled(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS)) {
                g.writeStartArray(value, value.length);
                _writeArrayContents(g, value);
                g.writeEndArray();
            } else {
                g.writeString(value, 0, value.length);
            }
        }

        @Override
        public void serializeWithType(char[] value, JsonGenerator g, SerializerProvider ctxt,
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
        @SuppressWarnings("deprecation")
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Integer.TYPE);

        public IntArraySerializer() { super(int[].class); }

        protected IntArraySerializer(IntArraySerializer src,
                BeanProperty prop, Boolean unwrapSingle) {
            super(src, prop, unwrapSingle);
        }
        
        @Override
        public ValueSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
            return new IntArraySerializer(this, prop, unwrapSingle);
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
        public boolean isEmpty(SerializerProvider prov, int[] value) {
            return value.length == 0;
        }

        @Override
        public boolean hasSingleElement(int[] value) {
            return (value.length == 1);
        }

        @Override
        public final void serialize(int[] value, JsonGenerator g, SerializerProvider provider) throws JacksonException
        {
            final int len = value.length;
            if ((len == 1) && _shouldUnwrapSingle(provider)) {
                serializeContents(value, g, provider);
                return;
            }
            // 11-May-2016, tatu: As per [core#277] we have efficient `writeArray(...)` available
            g.writeArray(value, 0, value.length);
        }

        @Override
        public void serializeContents(int[] value, JsonGenerator g, SerializerProvider provider)
            throws JacksonException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
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
        @SuppressWarnings("deprecation")
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Long.TYPE);

        public LongArraySerializer() { super(long[].class); }
        public LongArraySerializer(LongArraySerializer src, BeanProperty prop,
                Boolean unwrapSingle) {
            super(src, prop, unwrapSingle);
        }

        @Override
        public ValueSerializer<?> _withResolved(BeanProperty prop,Boolean unwrapSingle) {
            return new LongArraySerializer(this, prop, unwrapSingle);
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
        public boolean isEmpty(SerializerProvider prov, long[] value) {
            return value.length == 0;
        }

        @Override
        public boolean hasSingleElement(long[] value) {
            return (value.length == 1);
        }

        @Override
        public final void serialize(long[] value, JsonGenerator g, SerializerProvider provider) throws JacksonException
        {
            final int len = value.length;
            if ((len == 1) && _shouldUnwrapSingle(provider)) {
                serializeContents(value, g, provider);
                return;
            }
            // 11-May-2016, tatu: As per [core#277] we have efficient `writeArray(...)` available
            g.writeArray(value, 0, value.length);
        }
        
        @Override
        public void serializeContents(long[] value, JsonGenerator g, SerializerProvider provider)
            throws JacksonException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
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
        @SuppressWarnings("deprecation")
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Float.TYPE);
        
        public FloatArraySerializer() {
            super(float[].class);
        }
        public FloatArraySerializer(FloatArraySerializer src, BeanProperty prop,
                Boolean unwrapSingle) {
            super(src, prop, unwrapSingle);
        }

        @Override
        public ValueSerializer<?> _withResolved(BeanProperty prop,Boolean unwrapSingle) {
            return new FloatArraySerializer(this, prop, unwrapSingle);
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
        public boolean isEmpty(SerializerProvider prov, float[] value) {
            return value.length == 0;
        }

        @Override
        public boolean hasSingleElement(float[] value) {
            return (value.length == 1);
        }

        @Override
        public final void serialize(float[] value, JsonGenerator g, SerializerProvider provider) throws JacksonException
        {
            final int len = value.length;
            if ((len == 1) && _shouldUnwrapSingle(provider)) {
                serializeContents(value, g, provider);
                return;
            }
            g.writeStartArray(value, len);
            serializeContents(value, g, provider);
            g.writeEndArray();
        }
        
        @Override
        public void serializeContents(float[] value, JsonGenerator g, SerializerProvider provider)
            throws JacksonException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
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
        @SuppressWarnings("deprecation")
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Double.TYPE);

        public DoubleArraySerializer() { super(double[].class); }

        /**
         * @since 2.6
         */
        protected DoubleArraySerializer(DoubleArraySerializer src,
                BeanProperty prop, Boolean unwrapSingle) {
            super(src, prop, unwrapSingle);
        }

        @Override
        public ValueSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
            return new DoubleArraySerializer(this, prop, unwrapSingle);
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
        public boolean isEmpty(SerializerProvider prov, double[] value) {
            return value.length == 0;
        }

        @Override
        public boolean hasSingleElement(double[] value) {
            return (value.length == 1);
        }

        @Override
        public final void serialize(double[] value, JsonGenerator g, SerializerProvider provider) throws JacksonException
        {
            final int len = value.length;
            if ((len == 1) && _shouldUnwrapSingle(provider)) {
                serializeContents(value, g, provider);
                return;
            }
            // 11-May-2016, tatu: As per [core#277] we have efficient `writeArray(...)` available
            g.writeArray(value, 0, value.length);
        }

        @Override
        public void serializeContents(double[] value, JsonGenerator g, SerializerProvider provider) throws JacksonException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                g.writeNumber(value[i]);
            }
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
            visitArrayFormat(visitor, typeHint, JsonFormatTypes.NUMBER);
        }
    }
}
