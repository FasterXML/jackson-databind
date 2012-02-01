package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
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
    protected StdArraySerializers() { }

    /*
     ****************************************************************
    /* Concrete serializers, arrays
     ****************************************************************
     */


    @JacksonStdImpl
    public final static class BooleanArraySerializer
        extends ArraySerializerBase<boolean[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Boolean.class);

        public BooleanArraySerializer() { super(boolean[].class, null, null); }

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
            o.put("items", createSchemaNode("boolean"));
            return o;
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
    public final static class ByteArraySerializer
        extends StdSerializer<byte[]>
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
            jgen.writeBinary(value);
        }

        @Override
        public void serializeWithType(byte[] value, JsonGenerator jgen, SerializerProvider provider,
                TypeSerializer typeSer)
            throws IOException, JsonGenerationException
        {
            typeSer.writeTypePrefixForScalar(value, jgen);
            jgen.writeBinary(value);
            typeSer.writeTypeSuffixForScalar(value, jgen);
        }
        
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            ObjectNode itemSchema = createSchemaNode("string"); //binary values written as strings?
            o.put("items", itemSchema);
            return o;
        }
    }

    @JacksonStdImpl
    public final static class ShortArraySerializer
        extends ArraySerializerBase<short[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Short.TYPE);

        public ShortArraySerializer() { this(null); }
        public ShortArraySerializer(TypeSerializer vts) { super(short[].class, vts, null); }

        @Override
        public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
            return new ShortArraySerializer(vts);
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

        @SuppressWarnings("cast")
        @Override
        public void serializeContents(short[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber((int)value[i]);
            }
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            //no "short" type defined by json
            ObjectNode o = createSchemaNode("array", true);
            o.put("items", createSchemaNode("integer"));
            return o;
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
    public final static class CharArraySerializer
        extends StdSerializer<char[]>
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
            if (provider.isEnabled(SerializationConfig.Feature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS)) {
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
            if (provider.isEnabled(SerializationConfig.Feature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS)) {
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
            o.put("items", itemSchema);
            return o;
        }
    }

    @JacksonStdImpl
    public final static class IntArraySerializer
        extends ArraySerializerBase<int[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Integer.TYPE);

        public IntArraySerializer() { super(int[].class, null, null); }

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
        public void serializeContents(int[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            o.put("items", createSchemaNode("integer"));
            return o;
        }
    }

    @JacksonStdImpl
    public final static class LongArraySerializer
        extends ArraySerializerBase<long[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Long.TYPE);

        public LongArraySerializer() { this(null); }
        public LongArraySerializer(TypeSerializer vts) { super(long[].class, vts, null); }

        @Override
        public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
            return new LongArraySerializer(vts);
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
        public void serializeContents(long[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            o.put("items", createSchemaNode("number", true));
            return o;
        }
    }

    @JacksonStdImpl
    public final static class FloatArraySerializer
        extends ArraySerializerBase<float[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Float.TYPE);
        
        public FloatArraySerializer() { this(null); }
        public FloatArraySerializer(TypeSerializer vts) { super(float[].class, vts, null); }

        @Override
        public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
            return new FloatArraySerializer(vts);
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
        public void serializeContents(float[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            o.put("items", createSchemaNode("number"));
            return o;
        }
    }

    @JacksonStdImpl
    public final static class DoubleArraySerializer
        extends ArraySerializerBase<double[]>
    {
        // as above, assuming no one re-defines primitive/wrapper types
        private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(Double.TYPE);

        public DoubleArraySerializer() { super(double[].class, null, null); }

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
        public void serializeContents(double[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            o.put("items", createSchemaNode("number"));
            return o;
        }
    }
}
