package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Dummy container class to group standard homogenous array serializer implementations
 * (primitive arrays and String array).
 */
public class StdArraySerializers
{
    protected final static HashMap<String, JsonSerializer<?>> _arraySerializers =
        new HashMap<String, JsonSerializer<?>>();
    static {
        // Arrays of various types (including common object types)
        _arraySerializers.put(boolean[].class.getName(), new StdArraySerializers.BooleanArraySerializer());
        _arraySerializers.put(byte[].class.getName(), new StdArraySerializers.ByteArraySerializer());
        _arraySerializers.put(char[].class.getName(), new StdArraySerializers.CharArraySerializer());
        _arraySerializers.put(short[].class.getName(), new StdArraySerializers.ShortArraySerializer());
        _arraySerializers.put(int[].class.getName(), new StdArraySerializers.IntArraySerializer());
        _arraySerializers.put(long[].class.getName(), new StdArraySerializers.LongArraySerializer());
        _arraySerializers.put(float[].class.getName(), new StdArraySerializers.FloatArraySerializer());
        _arraySerializers.put(double[].class.getName(), new StdArraySerializers.DoubleArraySerializer());
    }

    protected StdArraySerializers() { }

    /**
     * Accessor for checking to see if there is a standard serializer for
     * given primitive value type.
     */
    public static JsonSerializer<?> findStandardImpl(Class<?> cls) {
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
    protected abstract static class TypedPrimitiveArraySerializer<T> extends ArraySerializerBase<T>
    {
        /**
         * Type serializer to use for values, if any.
         */
        protected final TypeSerializer _valueTypeSerializer;
        
        protected TypedPrimitiveArraySerializer(Class<T> cls) {
            super(cls);
            _valueTypeSerializer = null;
        }

        protected TypedPrimitiveArraySerializer(TypedPrimitiveArraySerializer<T> src,
                BeanProperty prop, TypeSerializer vts) {
            super(src, prop);
            _valueTypeSerializer = vts;
        }
    }
    
    /*
    /****************************************************************
    /* Concrete serializers, arrays
    /****************************************************************
     */

    @JacksonStdImpl
    public final static class BooleanArraySerializer extends ArraySerializerBase<boolean[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Boolean.class);

        public BooleanArraySerializer() { super(boolean[].class, null); }

        /**
         * Booleans never add type info; hence, even if type serializer is suggested,
         * we'll ignore it...
         */
        @Override
        public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
            return this;
        }

        @Override
        public JavaType getContentType() {
            return VALUE_TYPE;
        }

        @Override
        public JsonSerializer<?> getContentSerializer() {
            // 14-Jan-2012, tatu: We could refer to an actual serializer if absolutely necessary
            return null;
        }
        
        @Override
        public boolean isEmpty(boolean[] value) {
            return (value == null) || (value.length == 0);
        }

        @Override
        public boolean hasSingleElement(boolean[] value) {
            return (value.length == 1);
        }
        
        @Override
        public void serializeContents(boolean[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeBoolean(value[i]);
            }
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            o.set("items", createSchemaNode("boolean"));
            return o;
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
                throws JsonMappingException
        {
            if (visitor != null) {
                JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
                if (v2 != null) {
                    v2.itemsFormat(JsonFormatTypes.BOOLEAN);
                }
            }
        }
    }

    /**
     * Unlike other integral number array serializers, we do not just print out byte values
     * as numbers. Instead, we assume that it would make more sense to output content
     * as base64 encoded bytes (using default base64 encoding).
     *<p>
     * NOTE: since it is NOT serialized as an array, can not use AsArraySerializer as base
     */
    @JacksonStdImpl
    public final static class ByteArraySerializer extends StdSerializer<byte[]>
    {
        public ByteArraySerializer() {
            super(byte[].class);
        }
        
        @Override
        public boolean isEmpty(byte[] value) {
            return (value == null) || (value.length == 0);
        }
        
        @Override
        public void serialize(byte[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeBinary(provider.getConfig().getBase64Variant(),
                    value, 0, value.length);
        }

        @Override
        public void serializeWithType(byte[] value, JsonGenerator jgen, SerializerProvider provider,
                TypeSerializer typeSer)
            throws IOException, JsonGenerationException
        {
            typeSer.writeTypePrefixForScalar(value, jgen);
            jgen.writeBinary(provider.getConfig().getBase64Variant(),
                    value, 0, value.length);
            typeSer.writeTypeSuffixForScalar(value, jgen);
        }
        
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            ObjectNode itemSchema = createSchemaNode("string"); //binary values written as strings?
            return o.set("items", itemSchema);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
                throws JsonMappingException
        {
            if (visitor != null) {
                JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
                if (v2 != null) {
                    v2.itemsFormat(JsonFormatTypes.STRING);
                }
            }
        }
    }

    @JacksonStdImpl
    public final static class ShortArraySerializer extends TypedPrimitiveArraySerializer<short[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Short.TYPE);

        public ShortArraySerializer() { super(short[].class); }
        public ShortArraySerializer(ShortArraySerializer src, BeanProperty prop, TypeSerializer vts) {
            super(src, prop, vts);
        }

        @Override
        public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
            return new ShortArraySerializer(this, _property, vts);
        }

        @Override
        public JavaType getContentType() {
            return VALUE_TYPE;
        }

        @Override
        public JsonSerializer<?> getContentSerializer() {
            // 14-Jan-2012, tatu: We could refer to an actual serializer if absolutely necessary
            return null;
        }
        
        @Override
        public boolean isEmpty(short[] value) {
            return (value == null) || (value.length == 0);
        }

        @Override
        public boolean hasSingleElement(short[] value) {
            return (value.length == 1);
        }
        
        @SuppressWarnings("cast")
        @Override
        public void serializeContents(short[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            if (_valueTypeSerializer != null) {
                for (int i = 0, len = value.length; i < len; ++i) {
                    _valueTypeSerializer.writeTypePrefixForScalar(null, jgen, Short.TYPE);
                    jgen.writeNumber(value[i]);
                    _valueTypeSerializer.writeTypeSuffixForScalar(null, jgen);
                }
                return;
            }
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber((int)value[i]);
            }
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            //no "short" type defined by json
            ObjectNode o = createSchemaNode("array", true);
            return o.set("items", createSchemaNode("integer"));
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
                throws JsonMappingException
        {
            if (visitor != null) {
                JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
                if (v2 != null) {
                    v2.itemsFormat(JsonFormatTypes.INTEGER);
                }
            }
        }
    }

    /**
     * Character arrays are different from other integral number arrays in that
     * they are most likely to be textual data, and should be written as
     * Strings, not arrays of entries.
     *<p>
     * NOTE: since it is NOT serialized as an array, can not use AsArraySerializer as base
     */
    @JacksonStdImpl
    public final static class CharArraySerializer extends StdSerializer<char[]>
    {
        public CharArraySerializer() { super(char[].class); }
        
        @Override
        public boolean isEmpty(char[] value) {
            return (value == null) || (value.length == 0);
        }
        
        @Override
        public void serialize(char[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            // [JACKSON-289] allows serializing as 'sparse' char array too:
            if (provider.isEnabled(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS)) {
                jgen.writeStartArray();
                _writeArrayContents(jgen, value);
                jgen.writeEndArray();
            } else {
                jgen.writeString(value, 0, value.length);
            }
        }

        @Override
        public void serializeWithType(char[] value, JsonGenerator jgen, SerializerProvider provider,
                TypeSerializer typeSer)
            throws IOException, JsonGenerationException
        {
            // [JACKSON-289] allows serializing as 'sparse' char array too:
            if (provider.isEnabled(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS)) {
                typeSer.writeTypePrefixForArray(value, jgen);
                _writeArrayContents(jgen, value);
                typeSer.writeTypeSuffixForArray(value, jgen);
            } else { // default is to write as simple String
                typeSer.writeTypePrefixForScalar(value, jgen);
                jgen.writeString(value, 0, value.length);
                typeSer.writeTypeSuffixForScalar(value, jgen);
            }
        }

        private final void _writeArrayContents(JsonGenerator jgen, char[] value)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeString(value, i, 1);
            }
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            ObjectNode itemSchema = createSchemaNode("string");
            itemSchema.put("type", "string");
            return o.set("items", itemSchema);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
                throws JsonMappingException
        {
            if (visitor != null) {
                JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
                if (v2 != null) {
                    v2.itemsFormat(JsonFormatTypes.STRING);
                }
            }
        }
    }

    @JacksonStdImpl
    public final static class IntArraySerializer extends ArraySerializerBase<int[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Integer.TYPE);

        public IntArraySerializer() { super(int[].class, null); }

        /**
         * Ints never add type info; hence, even if type serializer is suggested,
         * we'll ignore it...
         */
        @Override
        public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
            return this;
        }        

        @Override
        public JavaType getContentType() {
            return VALUE_TYPE;
        }

        @Override
        public JsonSerializer<?> getContentSerializer() {
            // 14-Jan-2012, tatu: We could refer to an actual serializer if absolutely necessary
            return null;
        }
        
        @Override
        public boolean isEmpty(int[] value) {
            return (value == null) || (value.length == 0);
        }

        @Override
        public boolean hasSingleElement(int[] value) {
            return (value.length == 1);
        }

        @Override
        public void serializeContents(int[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
            return createSchemaNode("array", true).set("items", createSchemaNode("integer"));
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
        {
            if (visitor != null) {
                JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
                if (v2 != null) {
                    v2.itemsFormat(JsonFormatTypes.INTEGER);
                }
            }
        }
    }

    @JacksonStdImpl
    public final static class LongArraySerializer extends TypedPrimitiveArraySerializer<long[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Long.TYPE);

        public LongArraySerializer() { super(long[].class); }
        public LongArraySerializer(LongArraySerializer src, BeanProperty prop,
                TypeSerializer vts) {
            super(src, prop, vts);
        }

        @Override
        public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
            return new LongArraySerializer(this, _property, vts);
        }

        @Override
        public JavaType getContentType() {
            return VALUE_TYPE;
        }

        @Override
        public JsonSerializer<?> getContentSerializer() {
            // 14-Jan-2012, tatu: We could refer to an actual serializer if absolutely necessary
            return null;
        }
        
        @Override
        public boolean isEmpty(long[] value) {
            return (value == null) || (value.length == 0);
        }

        @Override
        public boolean hasSingleElement(long[] value) {
            return (value.length == 1);
        }
        
        @Override
        public void serializeContents(long[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            if (_valueTypeSerializer != null) {
                for (int i = 0, len = value.length; i < len; ++i) {
                    _valueTypeSerializer.writeTypePrefixForScalar(null, jgen, Long.TYPE);
                    jgen.writeNumber(value[i]);
                    _valueTypeSerializer.writeTypeSuffixForScalar(null, jgen);
                }
                return;
            }
            
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("array", true)
                .set("items", createSchemaNode("number", true));
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
                throws JsonMappingException
        {
            if (visitor != null) {
                JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
                if (v2 != null) {
                    v2.itemsFormat(JsonFormatTypes.NUMBER);
                }
            }
        }
    }

    @JacksonStdImpl
    public final static class FloatArraySerializer extends TypedPrimitiveArraySerializer<float[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Float.TYPE);
        
        public FloatArraySerializer() {
            super(float[].class);
        }
        public FloatArraySerializer(FloatArraySerializer src, BeanProperty prop,
                TypeSerializer vts) {
            super(src, prop, vts);
        }

        @Override
        public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
            return new FloatArraySerializer(this, _property, vts);
        }

        @Override
        public JavaType getContentType() {
            return VALUE_TYPE;
        }

        @Override
        public JsonSerializer<?> getContentSerializer() {
            // 14-Jan-2012, tatu: We could refer to an actual serializer if absolutely necessary
            return null;
        }
        
        @Override
        public boolean isEmpty(float[] value) {
            return (value == null) || (value.length == 0);
        }

        @Override
        public boolean hasSingleElement(float[] value) {
            return (value.length == 1);
        }
        
        @Override
        public void serializeContents(float[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            if (_valueTypeSerializer != null) {
                for (int i = 0, len = value.length; i < len; ++i) {
                    _valueTypeSerializer.writeTypePrefixForScalar(null, jgen, Float.TYPE);
                    jgen.writeNumber(value[i]);
                    _valueTypeSerializer.writeTypeSuffixForScalar(null, jgen);
                }
                return;
            }
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
            return createSchemaNode("array", true).set("items", createSchemaNode("number"));
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
        {
            if (visitor != null) {
                JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
                if (v2 != null) {
                    v2.itemsFormat(JsonFormatTypes.NUMBER);
                }
            }
        }
    }

    @JacksonStdImpl
    public final static class DoubleArraySerializer extends ArraySerializerBase<double[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Double.TYPE);

        public DoubleArraySerializer() { super(double[].class, null); }

        /**
         * Doubles never add type info; hence, even if type serializer is suggested,
         * we'll ignore it...
         */
        @Override
        public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
            return this;
        }

        @Override
        public JavaType getContentType() {
            return VALUE_TYPE;
        }

        @Override
        public JsonSerializer<?> getContentSerializer() {
            // 14-Jan-2012, tatu: We could refer to an actual serializer if absolutely necessary
            return null;
        }
        
        @Override
        public boolean isEmpty(double[] value) {
            return (value == null) || (value.length == 0);
        }

        @Override
        public boolean hasSingleElement(double[] value) {
            return (value.length == 1);
        }
        
        @Override
        public void serializeContents(double[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
            return createSchemaNode("array", true).set("items", createSchemaNode("number"));
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
                throws JsonMappingException
        {
            if (visitor != null) {
                JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
                if (v2 != null) {
                    v2.itemsFormat(JsonFormatTypes.NUMBER);
                }
            }
        }
    }
}
