package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.ArrayBuilders;

/**
 * Container for deserializers used for instantiating "primitive arrays",
 * arrays that contain non-object java primitive types.
 */
@SuppressWarnings("serial")
public abstract class PrimitiveArrayDeserializers<T> extends StdDeserializer<T>
    implements ContextualDeserializer // since 2.7
{
    /**
     * Specific override for this instance (from proper, or global per-type overrides)
     * to indicate whether single value may be taken to mean an unwrapped one-element array
     * or not. If null, left to global defaults.
     *
     * @since 2.7
     */
    protected final Boolean _unwrapSingle;

    protected PrimitiveArrayDeserializers(Class<T> cls) {
        super(cls);
        _unwrapSingle = null;
    }

    /**
     * @since 2.7
     */
    protected PrimitiveArrayDeserializers(PrimitiveArrayDeserializers<?> base,
            Boolean unwrapSingle) {
        super(base._valueClass);
        _unwrapSingle = unwrapSingle;
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

    /**
     * @since 2.7
     */
    protected abstract PrimitiveArrayDeserializers<?> withResolved(Boolean unwrapSingle);

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        Boolean unwrapSingle = findFormatFeature(ctxt, property, _valueClass,
                JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        if (unwrapSingle == _unwrapSingle) {
            return this;
        }
        return withResolved(unwrapSingle);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException
    {
        /* Should there be separate handling for base64 stuff?
         * for now this should be enough:
         */
        return typeDeserializer.deserializeTypedFromArray(p, ctxt);
    }
    
    protected T handleNonArray(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // [JACKSON-620] Empty String can become null...
        if (p.hasToken(JsonToken.VALUE_STRING)
                && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
            if (p.getText().length() == 0) {
                return null;
            }
        }
        boolean canWrap = (_unwrapSingle == Boolean.TRUE) ||
                ((_unwrapSingle == null) &&
                        ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
        if (canWrap) {
            return handleSingleElementUnwrapped(p, ctxt);
        }
        throw ctxt.mappingException(_valueClass);
    }

    protected abstract T handleSingleElementUnwrapped(JsonParser p,
            DeserializationContext ctxt) throws IOException;
    
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
        protected CharDeser(CharDeser base, Boolean unwrapSingle) {
            super(base, unwrapSingle);
        }

        @Override
        protected PrimitiveArrayDeserializers<?> withResolved(Boolean unwrapSingle) {
            // 11-Dec-2015, tatu: Not sure how re-wrapping would work; omit
            return this;
        }
        
        @Override
        public char[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            /* Won't take arrays, must get a String (could also
             * convert other tokens to Strings... but let's not bother
             * yet, doesn't seem to make sense)
             */
            JsonToken t = p.getCurrentToken();
            if (t == JsonToken.VALUE_STRING) {
                // note: can NOT return shared internal buffer, must copy:
                char[] buffer = p.getTextCharacters();
                int offset = p.getTextOffset();
                int len = p.getTextLength();
    
                char[] result = new char[len];
                System.arraycopy(buffer, offset, result, 0, len);
                return result;
            }
            if (p.isExpectedStartArrayToken()) {
                // Let's actually build as a String, then get chars
                StringBuilder sb = new StringBuilder(64);
                while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
                    if (t != JsonToken.VALUE_STRING) {
                        throw ctxt.mappingException(Character.TYPE);
                    }
                    String str = p.getText();
                    if (str.length() != 1) {
                        throw JsonMappingException.from(p, "Can not convert a JSON String of length "+str.length()+" into a char element of char array");
                    }
                    sb.append(str.charAt(0));
                }
                return sb.toString().toCharArray();
            }
            // or, maybe an embedded object?
            if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
                Object ob = p.getEmbeddedObject();
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

        @Override
        protected char[] handleSingleElementUnwrapped(JsonParser p,
                DeserializationContext ctxt) throws IOException {
            // not sure how this should work so just return `null` so:
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
        protected BooleanDeser(BooleanDeser base, Boolean unwrapSingle) {
            super(base, unwrapSingle);
        }

        @Override
        protected PrimitiveArrayDeserializers<?> withResolved(Boolean unwrapSingle) {
            return new BooleanDeser(this, unwrapSingle);
        }

        @Override
        public boolean[] deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (!p.isExpectedStartArrayToken()) {
                return handleNonArray(p, ctxt);
            }
            ArrayBuilders.BooleanBuilder builder = ctxt.getArrayBuilders().getBooleanBuilder();
            boolean[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    // whether we should allow truncating conversions?
                    boolean value = _parseBooleanPrimitive(p, ctxt);
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

        @Override
        protected boolean[] handleSingleElementUnwrapped(JsonParser p,
                DeserializationContext ctxt) throws IOException {
            return new boolean[] { _parseBooleanPrimitive(p, ctxt) };
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
        protected ByteDeser(ByteDeser base, Boolean unwrapSingle) {
            super(base, unwrapSingle);
        }

        @Override
        protected PrimitiveArrayDeserializers<?> withResolved(Boolean unwrapSingle) {
            return new ByteDeser(this, unwrapSingle);
        }

        @Override
        public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            JsonToken t = p.getCurrentToken();
            
            // Most likely case: base64 encoded String?
            if (t == JsonToken.VALUE_STRING) {
                return p.getBinaryValue(ctxt.getBase64Variant());
            }
            // 31-Dec-2009, tatu: Also may be hidden as embedded Object
            if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
                Object ob = p.getEmbeddedObject();
                if (ob == null) return null;
                if (ob instanceof byte[]) {
                    return (byte[]) ob;
                }
            }
            if (!p.isExpectedStartArrayToken()) {
                return handleNonArray(p, ctxt);
            }
            ArrayBuilders.ByteBuilder builder = ctxt.getArrayBuilders().getByteBuilder();
            byte[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
                    // whether we should allow truncating conversions?
                    byte value;
                    if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) {
                        // should we catch overflow exceptions?
                        value = p.getByteValue();
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

        @Override
        protected byte[] handleSingleElementUnwrapped(JsonParser p,
                DeserializationContext ctxt) throws IOException
        {
            byte value;
            JsonToken t = p.getCurrentToken();
            if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) {
                // should we catch overflow exceptions?
                value = p.getByteValue();
            } else {
                // should probably accept nulls as 'false'
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
        protected ShortDeser(ShortDeser base, Boolean unwrapSingle) {
            super(base, unwrapSingle);
        }

        @Override
        protected PrimitiveArrayDeserializers<?> withResolved(Boolean unwrapSingle) {
            return new ShortDeser(this, unwrapSingle);
        }
        
        @Override
        public short[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            if (!p.isExpectedStartArrayToken()) {
                return handleNonArray(p, ctxt);
            }
            ArrayBuilders.ShortBuilder builder = ctxt.getArrayBuilders().getShortBuilder();
            short[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    short value = _parseShortPrimitive(p, ctxt);
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

        @Override
        protected short[] handleSingleElementUnwrapped(JsonParser p,
                DeserializationContext ctxt) throws IOException {
            return new short[] { _parseShortPrimitive(p, ctxt) };
        }
    }

    @JacksonStdImpl
    final static class IntDeser
        extends PrimitiveArrayDeserializers<int[]>
    {
        private static final long serialVersionUID = 1L;

        public final static IntDeser instance = new IntDeser();
        
        public IntDeser() { super(int[].class); }
        protected IntDeser(IntDeser base, Boolean unwrapSingle) {
            super(base, unwrapSingle);
        }

        @Override
        protected PrimitiveArrayDeserializers<?> withResolved(Boolean unwrapSingle) {
            return new IntDeser(this, unwrapSingle);
        }

        @Override
        public int[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            if (!p.isExpectedStartArrayToken()) {
                return handleNonArray(p, ctxt);
            }
            ArrayBuilders.IntBuilder builder = ctxt.getArrayBuilders().getIntBuilder();
            int[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    // whether we should allow truncating conversions?
                    int value = _parseIntPrimitive(p, ctxt);
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

        @Override
        protected int[] handleSingleElementUnwrapped(JsonParser p,
                DeserializationContext ctxt) throws IOException {
            return new int[] { _parseIntPrimitive(p, ctxt) };
        }
    }

    @JacksonStdImpl
    final static class LongDeser
        extends PrimitiveArrayDeserializers<long[]>
    {
        private static final long serialVersionUID = 1L;

        public final static LongDeser instance = new LongDeser();

        public LongDeser() { super(long[].class); }
        protected LongDeser(LongDeser base, Boolean unwrapSingle) {
            super(base, unwrapSingle);
        }

        @Override
        protected PrimitiveArrayDeserializers<?> withResolved(Boolean unwrapSingle) {
            return new LongDeser(this, unwrapSingle);
        }

        @Override
        public long[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            if (!p.isExpectedStartArrayToken()) {
                return handleNonArray(p, ctxt);
            }
            ArrayBuilders.LongBuilder builder = ctxt.getArrayBuilders().getLongBuilder();
            long[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    long value = _parseLongPrimitive(p, ctxt);
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

        @Override
        protected long[] handleSingleElementUnwrapped(JsonParser p,
                DeserializationContext ctxt) throws IOException {
            return new long[] { _parseLongPrimitive(p, ctxt) };
        }
    }

    @JacksonStdImpl
    final static class FloatDeser
        extends PrimitiveArrayDeserializers<float[]>
    {
        private static final long serialVersionUID = 1L;

        public FloatDeser() { super(float[].class); }
        protected FloatDeser(FloatDeser base, Boolean unwrapSingle) {
            super(base, unwrapSingle);
        }

        @Override
        protected PrimitiveArrayDeserializers<?> withResolved(Boolean unwrapSingle) {
            return new FloatDeser(this, unwrapSingle);
        }

        @Override
        public float[] deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (!p.isExpectedStartArrayToken()) {
                return handleNonArray(p, ctxt);
            }
            ArrayBuilders.FloatBuilder builder = ctxt.getArrayBuilders().getFloatBuilder();
            float[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    // whether we should allow truncating conversions?
                    float value = _parseFloatPrimitive(p, ctxt);
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

        @Override
        protected float[] handleSingleElementUnwrapped(JsonParser p,
                DeserializationContext ctxt) throws IOException {
            return new float[] { _parseFloatPrimitive(p, ctxt) };
        }
    }

    @JacksonStdImpl
    final static class DoubleDeser
        extends PrimitiveArrayDeserializers<double[]>
    {
        private static final long serialVersionUID = 1L;
        
        public DoubleDeser() { super(double[].class); }
        protected DoubleDeser(DoubleDeser base, Boolean unwrapSingle) {
            super(base, unwrapSingle);
        }

        @Override
        protected PrimitiveArrayDeserializers<?> withResolved(Boolean unwrapSingle) {
            return new DoubleDeser(this, unwrapSingle);
        }

        @Override
        public double[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            if (!p.isExpectedStartArrayToken()) {
                return handleNonArray(p, ctxt);
            }
            ArrayBuilders.DoubleBuilder builder = ctxt.getArrayBuilders().getDoubleBuilder();
            double[] chunk = builder.resetAndStart();
            int ix = 0;

            try {
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    double value = _parseDoublePrimitive(p, ctxt);
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

        @Override
        protected double[] handleSingleElementUnwrapped(JsonParser p,
                DeserializationContext ctxt) throws IOException {
            return new double[] { _parseDoublePrimitive(p, ctxt) };
        }
    }
}
