package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.ArrayBuilders;

/**
 * Container for deserializers used for instantiating "primitive arrays",
 * arrays that contain non-object java primitive types.
 */
@SuppressWarnings("serial")
public abstract class PrimitiveArrayDeserializers<T> extends StdDeserializer<T>
{
    protected PrimitiveArrayDeserializers(Class<T> cls) {
        super(cls);
    }

    public static JsonDeserializer<?> forType(Class<?> rawType)
    {
        // Start with more common types...
        if (rawType == Integer.TYPE) {
            return IntDeser.instance;
        }
        if (rawType == Long.TYPE) {
            return LongDeser.instance;
        }
        
        if (rawType == Byte.TYPE) {
            return new ByteDeser();
        }
        if (rawType == Short.TYPE) {
            return new ShortDeser();
        }
        if (rawType == Float.TYPE) {
            return new FloatDeser();
        }
        if (rawType == Double.TYPE) {
            return new DoubleDeser();
        }
        if (rawType == Boolean.TYPE) {
            return new BooleanDeser();
        }
        if (rawType == Character.TYPE) {
            return new CharDeser();
        }
        throw new IllegalStateException();
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException
    {
        /* Should there be separate handling for base64 stuff?
         * for now this should be enough:
         */
        return typeDeserializer.deserializeTypedFromArray(jp, ctxt);
    }
    
    /*
    /********************************************************
    /* Actual deserializers: efficient String[], char[] deserializers
    /********************************************************
    */

    @JacksonStdImpl
    final static class CharDeser
        extends PrimitiveArrayDeserializers<char[]>
    {
        private static final long serialVersionUID = 1L;

        public CharDeser() { super(char[].class); }

        @Override
        public char[] deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            /* Won't take arrays, must get a String (could also
             * convert other tokens to Strings... but let's not bother
             * yet, doesn't seem to make sense)
             */
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.VALUE_STRING) {
                // note: can NOT return shared internal buffer, must copy:
                char[] buffer = jp.getTextCharacters();
                int offset = jp.getTextOffset();
                int len = jp.getTextLength();
    
                char[] result = new char[len];
                System.arraycopy(buffer, offset, result, 0, len);
                return result;
            }
            if (jp.isExpectedStartArrayToken()) {
                // Let's actually build as a String, then get chars
                StringBuilder sb = new StringBuilder(64);
                while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
                    if (t != JsonToken.VALUE_STRING) {
                        throw ctxt.mappingException(Character.TYPE);
                    }
                    String str = jp.getText();
                    if (str.length() != 1) {
                        throw JsonMappingException.from(jp, "Can not convert a JSON String of length "+str.length()+" into a char element of char array");
                    }
                    sb.append(str.charAt(0));
                }
                return sb.toString().toCharArray();
            }
            // or, maybe an embedded object?
            if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
                Object ob = jp.getEmbeddedObject();
                if (ob == null) return null;
                if (ob instanceof char[]) {
                    return (char[]) ob;
                }
                if (ob instanceof String) {
                    return ((String) ob).toCharArray();
                }
                // 04-Feb-2011, tatu: byte[] can be converted; assuming base64 is wanted
                if (ob instanceof byte[]) {
                    return Base64Variants.getDefaultVariant().encode((byte[]) ob, false).toCharArray();
                }
                // not recognized, just fall through
            }
            throw ctxt.mappingException(_valueClass);
        }
    }

    /*
    /**********************************************************
    /* Actual deserializers: primivate array desers
    /**********************************************************
     */

    @JacksonStdImpl
    final static class BooleanDeser
        extends PrimitiveArrayDeserializers<boolean[]>
    {
        private static final long serialVersionUID = 1L;

        public BooleanDeser() { super(boolean[].class); }

        @Override
        public boolean[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (!jp.isExpectedStartArrayToken()) {
                return handleNonArray(jp, ctxt);
            }
            ArrayBuilders.BooleanBuilder builder = ctxt.getArrayBuilders().getBooleanBuilder();
            boolean[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    // whether we should allow truncating conversions?
                    boolean value = _parseBooleanPrimitive(jp, ctxt);
                    if (ix >= chunk.length) {
                        chunk = builder.appendCompletedChunk(chunk, ix);
                        ix = 0;
                    }
                    chunk[ix++] = value;
                }
            } catch (Exception e) {
                throw JsonMappingException.wrapWithPath(e, chunk, builder.bufferedSize() + ix);
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }

        private final boolean[] handleNonArray(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            // [JACKSON-620] Empty String can become null...
            if ((jp.getCurrentToken() == JsonToken.VALUE_STRING)
                    && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                if (jp.getText().length() == 0) {
                    return null;
                }
            }
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
                throw ctxt.mappingException(_valueClass);
            }
            return new boolean[] { _parseBooleanPrimitive(jp, ctxt) };
        }
    }

    /**
     * When dealing with byte arrays we have one more alternative (compared
     * to int/long/shorts): base64 encoded data.
     */
    @JacksonStdImpl
    final static class ByteDeser
        extends PrimitiveArrayDeserializers<byte[]>
    {
        private static final long serialVersionUID = 1L;

        public ByteDeser() { super(byte[].class); }

        @Override
        public byte[] deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            JsonToken t = jp.getCurrentToken();
            
            // Most likely case: base64 encoded String?
            if (t == JsonToken.VALUE_STRING) {
                return jp.getBinaryValue(ctxt.getBase64Variant());
            }
            // 31-Dec-2009, tatu: Also may be hidden as embedded Object
            if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
                Object ob = jp.getEmbeddedObject();
                if (ob == null) return null;
                if (ob instanceof byte[]) {
                    return (byte[]) ob;
                }
            }
            if (!jp.isExpectedStartArrayToken()) {
                return handleNonArray(jp, ctxt);
            }
            ArrayBuilders.ByteBuilder builder = ctxt.getArrayBuilders().getByteBuilder();
            byte[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
                    // whether we should allow truncating conversions?
                    byte value;
                    if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) {
                        // should we catch overflow exceptions?
                        value = jp.getByteValue();
                    } else {
                        // [JACKSON-79]: should probably accept nulls as 0
                        if (t != JsonToken.VALUE_NULL) {
                            throw ctxt.mappingException(_valueClass.getComponentType());
                        }
                        value = (byte) 0;
                    }
                    if (ix >= chunk.length) {
                        chunk = builder.appendCompletedChunk(chunk, ix);
                        ix = 0;
                    }
                    chunk[ix++] = value;
                }
            } catch (Exception e) {
                throw JsonMappingException.wrapWithPath(e, chunk, builder.bufferedSize() + ix);
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }

        private final byte[] handleNonArray(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            // [JACKSON-620] Empty String can become null...
            if ((jp.getCurrentToken() == JsonToken.VALUE_STRING)
                    && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                if (jp.getText().length() == 0) {
                    return null;
                }
            }
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
                throw ctxt.mappingException(_valueClass);
            }
            byte value;
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) {
                // should we catch overflow exceptions?
                value = jp.getByteValue();
            } else {
                // [JACKSON-79]: should probably accept nulls as 'false'
                if (t != JsonToken.VALUE_NULL) {
                    throw ctxt.mappingException(_valueClass.getComponentType());
                }
                value = (byte) 0;
            }
            return new byte[] { value };
        }
    }

    @JacksonStdImpl
    final static class ShortDeser
        extends PrimitiveArrayDeserializers<short[]>
    {
        private static final long serialVersionUID = 1L;

        public ShortDeser() { super(short[].class); }

        @Override
        public short[] deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            if (!jp.isExpectedStartArrayToken()) {
                return handleNonArray(jp, ctxt);
            }
            ArrayBuilders.ShortBuilder builder = ctxt.getArrayBuilders().getShortBuilder();
            short[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    short value = _parseShortPrimitive(jp, ctxt);
                    if (ix >= chunk.length) {
                        chunk = builder.appendCompletedChunk(chunk, ix);
                        ix = 0;
                    }
                    chunk[ix++] = value;
                }
            } catch (Exception e) {
                throw JsonMappingException.wrapWithPath(e, chunk, builder.bufferedSize() + ix);
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }

        private final short[] handleNonArray(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            // [JACKSON-620] Empty String can become null...
            if ((jp.getCurrentToken() == JsonToken.VALUE_STRING)
                    && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                if (jp.getText().length() == 0) {
                    return null;
                }
            }
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
                throw ctxt.mappingException(_valueClass);
            }
            return new short[] { _parseShortPrimitive(jp, ctxt) };
        }
    }

    @JacksonStdImpl
    final static class IntDeser
        extends PrimitiveArrayDeserializers<int[]>
    {
        private static final long serialVersionUID = 1L;

        public final static IntDeser instance = new IntDeser();
        
        public IntDeser() { super(int[].class); }

        @Override
        public int[] deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            if (!jp.isExpectedStartArrayToken()) {
                return handleNonArray(jp, ctxt);
            }
            ArrayBuilders.IntBuilder builder = ctxt.getArrayBuilders().getIntBuilder();
            int[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    // whether we should allow truncating conversions?
                    int value = _parseIntPrimitive(jp, ctxt);
                    if (ix >= chunk.length) {
                        chunk = builder.appendCompletedChunk(chunk, ix);
                        ix = 0;
                    }
                    chunk[ix++] = value;
                }
            } catch (Exception e) {
                throw JsonMappingException.wrapWithPath(e, chunk, builder.bufferedSize() + ix);
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }

        private final int[] handleNonArray(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            // [JACKSON-620] Empty String can become null...
            if ((jp.getCurrentToken() == JsonToken.VALUE_STRING)
                    && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                if (jp.getText().length() == 0) {
                    return null;
                }
            }
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
                throw ctxt.mappingException(_valueClass);
            }
            return new int[] { _parseIntPrimitive(jp, ctxt) };
        }
    }

    @JacksonStdImpl
    final static class LongDeser
        extends PrimitiveArrayDeserializers<long[]>
    {
        private static final long serialVersionUID = 1L;

        public final static LongDeser instance = new LongDeser();
        
        public LongDeser() { super(long[].class); }

        @Override
        public long[] deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            if (!jp.isExpectedStartArrayToken()) {
                return handleNonArray(jp, ctxt);
            }
            ArrayBuilders.LongBuilder builder = ctxt.getArrayBuilders().getLongBuilder();
            long[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    long value = _parseLongPrimitive(jp, ctxt);
                    if (ix >= chunk.length) {
                        chunk = builder.appendCompletedChunk(chunk, ix);
                        ix = 0;
                    }
                    chunk[ix++] = value;
                }
            } catch (Exception e) {
                throw JsonMappingException.wrapWithPath(e, chunk, builder.bufferedSize() + ix);
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }

        private final long[] handleNonArray(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            // [JACKSON-620] Empty String can become null...
            if ((jp.getCurrentToken() == JsonToken.VALUE_STRING)
                    && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                if (jp.getText().length() == 0) {
                    return null;
                }
            }
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
                throw ctxt.mappingException(_valueClass);
            }
            return new long[] { _parseLongPrimitive(jp, ctxt) };
        }
    }

    @JacksonStdImpl
    final static class FloatDeser
        extends PrimitiveArrayDeserializers<float[]>
    {
        private static final long serialVersionUID = 1L;

        public FloatDeser() { super(float[].class); }

        @Override
        public float[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (!jp.isExpectedStartArrayToken()) {
                return handleNonArray(jp, ctxt);
            }
            ArrayBuilders.FloatBuilder builder = ctxt.getArrayBuilders().getFloatBuilder();
            float[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    // whether we should allow truncating conversions?
                    float value = _parseFloatPrimitive(jp, ctxt);
                    if (ix >= chunk.length) {
                        chunk = builder.appendCompletedChunk(chunk, ix);
                        ix = 0;
                    }
                    chunk[ix++] = value;
                }
            } catch (Exception e) {
                throw JsonMappingException.wrapWithPath(e, chunk, builder.bufferedSize() + ix);
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }

        private final float[] handleNonArray(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // [JACKSON-620] Empty String can become null...
            if ((jp.getCurrentToken() == JsonToken.VALUE_STRING)
                    && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                if (jp.getText().length() == 0) {
                    return null;
                }
            }
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
                throw ctxt.mappingException(_valueClass);
            }
            return new float[] { _parseFloatPrimitive(jp, ctxt) };
        }
    }

    @JacksonStdImpl
    final static class DoubleDeser
        extends PrimitiveArrayDeserializers<double[]>
    {
        private static final long serialVersionUID = 1L;
        
        public DoubleDeser() { super(double[].class); }

        @Override
        public double[] deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            if (!jp.isExpectedStartArrayToken()) {
                return handleNonArray(jp, ctxt);
            }
            ArrayBuilders.DoubleBuilder builder = ctxt.getArrayBuilders().getDoubleBuilder();
            double[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    double value = _parseDoublePrimitive(jp, ctxt);
                    if (ix >= chunk.length) {
                        chunk = builder.appendCompletedChunk(chunk, ix);
                        ix = 0;
                    }
                    chunk[ix++] = value;
                }
            } catch (Exception e) {
                throw JsonMappingException.wrapWithPath(e, chunk, builder.bufferedSize() + ix);
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }

        private final double[] handleNonArray(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            // [JACKSON-620] Empty String can become null...
            if ((jp.getCurrentToken() == JsonToken.VALUE_STRING)
                    && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                if (jp.getText().length() == 0) {
                    return null;
                }
            }
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
                throw ctxt.mappingException(_valueClass);
            }
            return new double[] { _parseDoublePrimitive(jp, ctxt) };
        }
    }
}
