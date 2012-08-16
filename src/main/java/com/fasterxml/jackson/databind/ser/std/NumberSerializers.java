package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;

/**
 * Container class for serializers used for handling standard JDK-provided types.
 */
public class NumberSerializers
{
    protected NumberSerializers() { }
    
    public static void addAll(Map<String, JsonSerializer<?>> allDeserializers)
    {
        final JsonSerializer<?> intS = new IntegerSerializer();
        allDeserializers.put(Integer.class.getName(), intS);
        allDeserializers.put(Integer.TYPE.getName(), intS);
        allDeserializers.put(Long.class.getName(), LongSerializer.instance);
        allDeserializers.put(Long.TYPE.getName(), LongSerializer.instance);
        allDeserializers.put(Byte.class.getName(), IntLikeSerializer.instance);
        allDeserializers.put(Byte.TYPE.getName(), IntLikeSerializer.instance);
        allDeserializers.put(Short.class.getName(), IntLikeSerializer.instance);
        allDeserializers.put(Short.TYPE.getName(), IntLikeSerializer.instance);

        // Numbers, limited length floating point
        allDeserializers.put(Float.class.getName(), FloatSerializer.instance);
        allDeserializers.put(Float.TYPE.getName(), FloatSerializer.instance);
        allDeserializers.put(Double.class.getName(), DoubleSerializer.instance);
        allDeserializers.put(Double.TYPE.getName(), DoubleSerializer.instance);
    }
    
    /*
    /**********************************************************
    /* Concrete serializers
    /**********************************************************
     */
    


    /*
    /**********************************************************
    /* Concrete serializers, numerics
    /**********************************************************
     */

    /**
     * This is the special serializer for regular {@link java.lang.Integer}s
     * (and primitive ints)
     *<p>
     * Since this is one of "native" types, no type information is ever
     * included on serialization (unlike for most scalar types as of 1.5)
     */
    @JacksonStdImpl
    public final static class IntegerSerializer
        extends NonTypedScalarSerializerBase<Integer>
    {
        public IntegerSerializer() { super(Integer.class); }
    
        @Override
        public void serialize(Integer value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.intValue());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("integer", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
        	visitor.expectIntegerFormat(typeHint);
        }
    }

    /**
     * Similar to {@link IntegerSerializer}, but will not cast to Integer:
     * instead, cast is to {@link java.lang.Number}, and conversion is
     * by calling {@link java.lang.Number#intValue}.
     */
    @JacksonStdImpl
    public final static class IntLikeSerializer
        extends StdScalarSerializer<Number>
    {
        final static IntLikeSerializer instance = new IntLikeSerializer();
    
        public IntLikeSerializer() { super(Number.class); }
        
        @Override
        public void serialize(Number value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.intValue());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("integer", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
        	visitor.expectIntegerFormat(typeHint);
        }
    }

    @JacksonStdImpl
    public final static class LongSerializer
        extends StdScalarSerializer<Long>
    {
        final static LongSerializer instance = new LongSerializer();
    
        public LongSerializer() { super(Long.class); }
        
        @Override
        public void serialize(Long value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.longValue());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("number", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
        	visitor.expectNumberFormat(typeHint);
        }
    }
    
    @JacksonStdImpl
    public final static class FloatSerializer
        extends StdScalarSerializer<Float>
    {
        final static FloatSerializer instance = new FloatSerializer();
    
        public FloatSerializer() { super(Float.class); }
        
        @Override
        public void serialize(Float value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.floatValue());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("number", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
        	visitor.expectNumberFormat(typeHint);
        }
    }

    /**
     * This is the special serializer for regular {@link java.lang.Double}s
     * (and primitive doubles)
     *<p>
     * Since this is one of "native" types, no type information is ever
     * included on serialization (unlike for most scalar types as of 1.5)
     */
    @JacksonStdImpl
    public final static class DoubleSerializer
        extends NonTypedScalarSerializerBase<Double>
    {
        final static DoubleSerializer instance = new DoubleSerializer();
    
        public DoubleSerializer() { super(Double.class); }
    
        @Override
        public void serialize(Double value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.doubleValue());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("number", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
        	visitor.expectNumberFormat(typeHint);
        }
    }
    
    /**
     * As a fallback, we may need to use this serializer for other
     * types of {@link Number}s (custom types).
     */
    @JacksonStdImpl
    public final static class NumberSerializer
        extends StdScalarSerializer<Number>
    {
        public final static NumberSerializer instance = new NumberSerializer();
    
        public NumberSerializer() { super(Number.class); }
    
        @Override
        public void serialize(Number value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            // As per [JACKSON-423], handling for BigInteger and BigDecimal was missing!
            if (value instanceof BigDecimal) {
                jgen.writeNumber((BigDecimal) value);
            } else if (value instanceof BigInteger) {
                jgen.writeNumber((BigInteger) value);
                
            /* These shouldn't match (as there are more specific ones),
             * but just to be sure:
             */
            } else if (value instanceof Integer) {
                jgen.writeNumber(value.intValue());
            } else if (value instanceof Long) {
                jgen.writeNumber(value.longValue());
            } else if (value instanceof Double) {
                jgen.writeNumber(value.doubleValue());
            } else if (value instanceof Float) {
                jgen.writeNumber(value.floatValue());
            } else if ((value instanceof Byte) || (value instanceof Short)) {
                jgen.writeNumber(value.intValue()); // doesn't need to be cast to smaller numbers
            } else {
                // We'll have to use fallback "untyped" number write method
                jgen.writeNumber(value.toString());
            }
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("number", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
        	visitor.expectNumberFormat(typeHint);
        }
    }
}
