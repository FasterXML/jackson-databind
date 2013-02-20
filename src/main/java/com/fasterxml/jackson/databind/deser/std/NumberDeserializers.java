package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Container class for deserializers that handle core JDK primitive
 * (and matching wrapper) types, as well as standard "big" numeric types.
 * Note that this includes types such as {@link java.lang.Boolean}
 * and {@link java.lang.Character} which are not strictly numeric,
 * but are part of primitive/wrapper types.
 */
public class NumberDeserializers
{
    private final static HashSet<String> _classNames = new HashSet<String>();
    static {
        // note: can skip primitive types; other ways to check them:
        Class<?>[] numberTypes = new Class<?>[] {
            Boolean.class,
            Byte.class,
            Short.class,
            Character.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            // and more generic ones
            Number.class, BigDecimal.class, BigInteger.class
        };
        for (Class<?> cls : numberTypes) {
            _classNames.add(cls.getName());
        }
    }

    /**
     * @deprecated Since 2.2 -- use {@link #find} instead.
     */
    @Deprecated
    public static StdDeserializer<?>[] all()
    {
        return new StdDeserializer<?>[] {
                // primitive-wrappers (simple):
                new BooleanDeserializer(Boolean.class, null),
                new ByteDeserializer(Byte.class, null),
                new ShortDeserializer(Short.class, null),
                new CharacterDeserializer(Character.class, null),
                new IntegerDeserializer(Integer.class, null),
                new LongDeserializer(Long.class, null),
                new FloatDeserializer(Float.class, null),
                new DoubleDeserializer(Double.class, null),

                /* And actual primitives: difference is the way nulls are to be
                 * handled...
                 */
                new BooleanDeserializer(Boolean.TYPE, Boolean.FALSE),
                new ByteDeserializer(Byte.TYPE, Byte.valueOf((byte)(0))),
                new ShortDeserializer(Short.TYPE, Short.valueOf((short)0)),
                new CharacterDeserializer(Character.TYPE, Character.valueOf('\0')),
                new IntegerDeserializer(Integer.TYPE, Integer.valueOf(0)),
                new LongDeserializer(Long.TYPE, Long.valueOf(0L)),
                new FloatDeserializer(Float.TYPE, Float.valueOf(0.0f)),
                new DoubleDeserializer(Double.TYPE, Double.valueOf(0.0)),
                
                // and related
                new NumberDeserializer(),
                new BigDecimalDeserializer(),
                new BigIntegerDeserializer()
        };
    }
    
    public static JsonDeserializer<?> find(Class<?> rawType, String clsName)
    {
        if (rawType.isPrimitive()) {
            if (rawType == Integer.TYPE) {
                return IntegerDeserializer.primitiveInstance;
            }
            if (rawType == Boolean.TYPE) {
                return BooleanDeserializer.primitiveInstance;
            }
            if (rawType == Long.TYPE) {
                return LongDeserializer.primitiveInstance;
            }
            if (rawType == Double.TYPE) {
                return DoubleDeserializer.primitiveInstance;
            }
            if (rawType == Character.TYPE) {
                return CharacterDeserializer.primitiveInstance;
            }
            if (rawType == Byte.TYPE) {
                return ByteDeserializer.primitiveInstance;
            }
            if (rawType == Short.TYPE) {
                return ShortDeserializer.primitiveInstance;
            }
            if (rawType == Float.TYPE) {
                return FloatDeserializer.primitiveInstance;
            }
        } else if (_classNames.contains(clsName)) {
            // Start with most common types; int, boolean, long, double
            if (rawType == Integer.class) {
                return IntegerDeserializer.wrapperInstance;
            }
            if (rawType == Boolean.class) {
                return BooleanDeserializer.wrapperInstance;
            }
            if (rawType == Long.class) {
                return LongDeserializer.wrapperInstance;
            }
            if (rawType == Double.class) {
                return DoubleDeserializer.wrapperInstance;
            }
            if (rawType == Character.class) {
                return CharacterDeserializer.wrapperInstance;
            }
            if (rawType == Byte.class) {
                return ByteDeserializer.wrapperInstance;
            }
            if (rawType == Short.class) {
                return ShortDeserializer.wrapperInstance;
            }
            if (rawType == Float.class) {
                return FloatDeserializer.wrapperInstance;
            }
            if (rawType == Number.class) {
                return NumberDeserializer.instance;
            }
            if (rawType == BigDecimal.class) {
                return BigDecimalDeserializer.instance;
            }
            if (rawType == BigInteger.class) {
                return BigIntegerDeserializer.instance;
            }
        } else {
            return null;
        }
        // should never occur
        throw new IllegalArgumentException("Internal error: can't find deserializer for "+rawType.getName());
    }
    
    /*
    /**********************************************************
    /* Then one intermediate base class for things that have
    /* both primitive and wrapper types
    /**********************************************************
     */

    protected abstract static class PrimitiveOrWrapperDeserializer<T>
        extends StdScalarDeserializer<T>
    {
        private static final long serialVersionUID = 1L;

        protected final T _nullValue;
        
        protected PrimitiveOrWrapperDeserializer(Class<T> vc, T nvl)
        {
            super(vc);
            _nullValue = nvl;
        }
        
        @Override
        public final T getNullValue() {
            return _nullValue;
        }
    }
    
    /*
    /**********************************************************
    /* Then primitive/wrapper types
    /**********************************************************
     */

    @JacksonStdImpl
    public final static class BooleanDeserializer
        extends PrimitiveOrWrapperDeserializer<Boolean>
    {
        private static final long serialVersionUID = 1L;

        private final static BooleanDeserializer primitiveInstance = new BooleanDeserializer(Boolean.class, Boolean.FALSE);
        private final static BooleanDeserializer wrapperInstance = new BooleanDeserializer(Boolean.TYPE, null);
        
        public BooleanDeserializer(Class<Boolean> cls, Boolean nvl)
        {
            super(cls, nvl);
        }
        
        @Override
        public Boolean deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return _parseBoolean(jp, ctxt);
        }

        // 1.6: since we can never have type info ("natural type"; String, Boolean, Integer, Double):
        // (is it an error to even call this version?)
        @Override
        public Boolean deserializeWithType(JsonParser jp, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer)
            throws IOException, JsonProcessingException
        {
            return _parseBoolean(jp, ctxt);
        }
    }

    @JacksonStdImpl
    public final static class ByteDeserializer
        extends PrimitiveOrWrapperDeserializer<Byte>
    {
        private static final long serialVersionUID = 1L;

        private final static ByteDeserializer primitiveInstance = new ByteDeserializer(Byte.TYPE, (byte) 0);
        private final static ByteDeserializer wrapperInstance = new ByteDeserializer(Byte.class, null);
        
        public ByteDeserializer(Class<Byte> cls, Byte nvl)
        {
            super(cls, nvl);
        }

        @Override
        public Byte deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return _parseByte(jp, ctxt);
        }
    }

    @JacksonStdImpl
    public final static class ShortDeserializer
        extends PrimitiveOrWrapperDeserializer<Short>
    {
        private static final long serialVersionUID = 1L;

        private final static ShortDeserializer primitiveInstance = new ShortDeserializer(Short.class, Short.valueOf((short)0));
        private final static ShortDeserializer wrapperInstance = new ShortDeserializer(Short.TYPE, null);
        
        public ShortDeserializer(Class<Short> cls, Short nvl)
        {
            super(cls, nvl);
        }

        @Override
        public Short deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return _parseShort(jp, ctxt);
        }
    }

    @JacksonStdImpl
    public final static class CharacterDeserializer
        extends PrimitiveOrWrapperDeserializer<Character>
    {
        private static final long serialVersionUID = 1L;

        private final static CharacterDeserializer primitiveInstance = new CharacterDeserializer(Character.class, '\0');
        private final static CharacterDeserializer wrapperInstance = new CharacterDeserializer(Character.TYPE, null);
        
        public CharacterDeserializer(Class<Character> cls, Character nvl)
        {
            super(cls, nvl);
        }

        @Override
        public Character deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            int value;

            if (t == JsonToken.VALUE_NUMBER_INT) { // ok iff ascii value
                value = jp.getIntValue();
                if (value >= 0 && value <= 0xFFFF) {
                    return Character.valueOf((char) value);
                }
            } else if (t == JsonToken.VALUE_STRING) { // this is the usual type
                // But does it have to be exactly one char?
                String text = jp.getText();
                if (text.length() == 1) {
                    return Character.valueOf(text.charAt(0));
                }
                // actually, empty should become null?
                if (text.length() == 0) {
                    return (Character) getEmptyValue();
                }
            }
            throw ctxt.mappingException(_valueClass, t);
        }
    }

    @JacksonStdImpl
    public final static class IntegerDeserializer
        extends PrimitiveOrWrapperDeserializer<Integer>
    {
        private static final long serialVersionUID = 1L;

        private final static IntegerDeserializer primitiveInstance = new IntegerDeserializer(Integer.class, 0);
        private final static IntegerDeserializer wrapperInstance = new IntegerDeserializer(Integer.TYPE, null);
        
        public IntegerDeserializer(Class<Integer> cls, Integer nvl)
        {
            super(cls, nvl);
        }

        @Override
        public Integer deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return _parseInteger(jp, ctxt);
        }

        // 1.6: since we can never have type info ("natural type"; String, Boolean, Integer, Double):
        // (is it an error to even call this version?)
        @Override
        public Integer deserializeWithType(JsonParser jp, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer)
            throws IOException, JsonProcessingException
        {
            return _parseInteger(jp, ctxt);
        }
    }

    @JacksonStdImpl
    public final static class LongDeserializer
        extends PrimitiveOrWrapperDeserializer<Long>
    {
        private static final long serialVersionUID = 1L;

        private final static LongDeserializer primitiveInstance = new LongDeserializer(Long.class, Long.valueOf(0L));
        private final static LongDeserializer wrapperInstance = new LongDeserializer(Long.TYPE, null);
        
        public LongDeserializer(Class<Long> cls, Long nvl)
        {
            super(cls, nvl);
        }

        @Override
        public Long deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return _parseLong(jp, ctxt);
        }
    }

    @JacksonStdImpl
    public final static class FloatDeserializer
        extends PrimitiveOrWrapperDeserializer<Float>
    {
        private static final long serialVersionUID = 1L;

        private final static FloatDeserializer primitiveInstance = new FloatDeserializer(Float.class, 0.f);
        private final static FloatDeserializer wrapperInstance = new FloatDeserializer(Float.TYPE, null);
        
        public FloatDeserializer(Class<Float> cls, Float nvl)
        {
            super(cls, nvl);
        }

        @Override
        public Float deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            /* 22-Jan-2009, tatu: Bounds/range checks would be tricky
             *   here, so let's not bother even trying...
             */
            return _parseFloat(jp, ctxt);
        }
    }

    @JacksonStdImpl
    public final static class DoubleDeserializer
        extends PrimitiveOrWrapperDeserializer<Double>
    {
        private static final long serialVersionUID = 1L;

        private final static DoubleDeserializer primitiveInstance = new DoubleDeserializer(Double.class, 0.d);
        private final static DoubleDeserializer wrapperInstance = new DoubleDeserializer(Double.TYPE, null);
        
        public DoubleDeserializer(Class<Double> cls, Double nvl)
        {
            super(cls, nvl);
        }

        @Override
        public Double deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return _parseDouble(jp, ctxt);
        }

        // 1.6: since we can never have type info ("natural type"; String, Boolean, Integer, Double):
        // (is it an error to even call this version?)
        @Override
        public Double deserializeWithType(JsonParser jp, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer)
            throws IOException, JsonProcessingException
        {
            return _parseDouble(jp, ctxt);
        }
    }

    /**
     * For type <code>Number.class</code>, we can just rely on type
     * mappings that plain {@link JsonParser#getNumberValue} returns.
     *<p>
     * Since 1.5, there is one additional complication: some numeric
     * types (specifically, int/Integer and double/Double) are "non-typed";
     * meaning that they will NEVER be output with type information.
     * But other numeric types may need such type information.
     * This is why {@link #deserializeWithType} must be overridden.
     */
    @SuppressWarnings("serial")
    @JacksonStdImpl
    public final static class NumberDeserializer
        extends StdScalarDeserializer<Number>
    {
        public final static NumberDeserializer instance = new NumberDeserializer();
        
        public NumberDeserializer() { super(Number.class); }

        @Override
        public Number deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.VALUE_NUMBER_INT) {
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
                    return jp.getBigIntegerValue();
                }
                return jp.getNumberValue();
            } else if (t == JsonToken.VALUE_NUMBER_FLOAT) {
                /* [JACKSON-72]: need to allow overriding the behavior
                 * regarding which type to use
                 */
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                    return jp.getDecimalValue();
                }
                return Double.valueOf(jp.getDoubleValue());
            }

            /* Textual values are more difficult... not parsing itself, but figuring
             * out 'minimal' type to use 
             */
            if (t == JsonToken.VALUE_STRING) { // let's do implicit re-parse
                String text = jp.getText().trim();
                try {
                    if (text.indexOf('.') >= 0) { // floating point
                        // as per [JACKSON-72]:
                        if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                            return new BigDecimal(text);
                        }
                        return new Double(text);
                    }
                    // as per [JACKSON-100]:
                    if (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
                        return new BigInteger(text);
                    }
                    long value = Long.parseLong(text);
                    if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                        return Integer.valueOf((int) value);
                    }
                    return Long.valueOf(value);
                } catch (IllegalArgumentException iae) {
                    throw ctxt.weirdStringException(text, _valueClass, "not a valid number");
                }
            }
            // Otherwise, no can do:
            throw ctxt.mappingException(_valueClass, t);
        }

        /**
         * As mentioned in class Javadoc, there is additional complexity in
         * handling potentially mixed type information here. Because of this,
         * we must actually check for "raw" integers and doubles first, before
         * calling type deserializer.
         */
        @SuppressWarnings("incomplete-switch")
        @Override
        public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
                                          TypeDeserializer typeDeserializer)
            throws IOException, JsonProcessingException
        {
            switch (jp.getCurrentToken()) {
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
            case VALUE_STRING:
                // can not point to type information: hence must be non-typed (int/double)
                return deserialize(jp, ctxt);
            }
            return typeDeserializer.deserializeTypedFromScalar(jp, ctxt);
        }
    }

    /*
    /**********************************************************
    /* And then bit more complicated (but non-structured) number
    /* types
    /**********************************************************
     */
    
    /**
     * This is bit trickier to implement efficiently, while avoiding
     * overflow problems.
     */
    @SuppressWarnings("serial")
    @JacksonStdImpl
    public static class BigIntegerDeserializer
        extends StdScalarDeserializer<BigInteger>
    {
        public final static BigIntegerDeserializer instance = new BigIntegerDeserializer();

        public BigIntegerDeserializer() { super(BigInteger.class); }

        @SuppressWarnings("incomplete-switch")
        @Override
        public BigInteger deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            String text;

            if (t == JsonToken.VALUE_NUMBER_INT) {
                switch (jp.getNumberType()) {
                case INT:
                case LONG:
                    return BigInteger.valueOf(jp.getLongValue());
                }
            } else if (t == JsonToken.VALUE_NUMBER_FLOAT) {
                /* Whether to fail if there's non-integer part?
                 * Could do by calling BigDecimal.toBigIntegerExact()
                 */
                return jp.getDecimalValue().toBigInteger();
            } else if (t != JsonToken.VALUE_STRING) { // let's do implicit re-parse
                // String is ok too, can easily convert; otherwise, no can do:
                throw ctxt.mappingException(_valueClass, t);
            }
            text = jp.getText().trim();
            if (text.length() == 0) {
                return null;
            }
            try {
                return new BigInteger(text);
            } catch (IllegalArgumentException iae) {
                throw ctxt.weirdStringException(text, _valueClass, "not a valid representation");
            }
        }
    }
    
    @SuppressWarnings("serial")
    @JacksonStdImpl
    public static class BigDecimalDeserializer
        extends StdScalarDeserializer<BigDecimal>
    {
        public final static BigDecimalDeserializer instance = new BigDecimalDeserializer();
 
        public BigDecimalDeserializer() { super(BigDecimal.class); }

        @Override
        public BigDecimal deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) {
                return jp.getDecimalValue();
            }
            // String is ok too, can easily convert
            if (t == JsonToken.VALUE_STRING) { // let's do implicit re-parse
                String text = jp.getText().trim();
                if (text.length() == 0) {
                    return null;
                }
                try {
                    return new BigDecimal(text);
                } catch (IllegalArgumentException iae) {
                    throw ctxt.weirdStringException(text, _valueClass, "not a valid representation");
                }
            }
            // Otherwise, no can do:
            throw ctxt.mappingException(_valueClass, t);
        }
    }


}
