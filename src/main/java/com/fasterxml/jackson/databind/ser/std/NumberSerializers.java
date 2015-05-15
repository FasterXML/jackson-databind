package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNumberFormatVisitor;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * Container class for serializers used for handling standard JDK-provided types.
 */
@SuppressWarnings("serial")
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
        allDeserializers.put(Short.class.getName(), ShortSerializer.instance);
        allDeserializers.put(Short.TYPE.getName(), ShortSerializer.instance);

        // Numbers, limited length floating point
        allDeserializers.put(Float.class.getName(), FloatSerializer.instance);
        allDeserializers.put(Float.TYPE.getName(), FloatSerializer.instance);
        allDeserializers.put(Double.class.getName(), DoubleSerializer.instance);
        allDeserializers.put(Double.TYPE.getName(), DoubleSerializer.instance);
    }

    /*
    /**********************************************************
    /* Shared base class
    /**********************************************************
     */

    protected abstract static class Base<T> extends StdScalarSerializer<T>
        implements ContextualSerializer
    {
        protected final JsonParser.NumberType _numberType;
        protected final String _schemaType;
        protected final boolean _isInt;

        protected Base(Class<?> cls, JsonParser.NumberType numberType, String schemaType) {
            super(cls, false);
            _numberType = numberType;
            _schemaType = schemaType;
            _isInt = (numberType == JsonParser.NumberType.INT)
                    || (numberType == JsonParser.NumberType.LONG)
                    || (numberType == JsonParser.NumberType.BIG_INTEGER)
                    ;
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
            return createSchemaNode(_schemaType, true);
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
        {
            if (_isInt) {
                JsonIntegerFormatVisitor v2 = visitor.expectIntegerFormat(typeHint);
                if (v2 != null) {
                    v2.numberType(_numberType);
                }
            } else {
                JsonNumberFormatVisitor v2 = visitor.expectNumberFormat(typeHint);
                if (v2 != null) {
                    v2.numberType(_numberType);
                }
            }
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider prov,
                BeanProperty property) throws JsonMappingException
        {
            if (property != null) {
                AnnotatedMember m = property.getMember();
                if (m != null) {
                    JsonFormat.Value format = prov.getAnnotationIntrospector().findFormat(m);
                    if (format != null) {
                        switch (format.getShape()) {
                        case STRING:
                            return ToStringSerializer.instance;
                        default:
                        }
                    }
                }
            }
            return this;
        }
    }
    
    /*
    /**********************************************************
    /* Concrete serializers, numerics
    /**********************************************************
     */

    @JacksonStdImpl
    public final static class ShortSerializer extends Base<Short>
    {
        final static ShortSerializer instance = new ShortSerializer();
    
        public ShortSerializer() { super(Short.class, JsonParser.NumberType.INT, "number"); }

        @Override
        public void serialize(Short value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeNumber(value.shortValue());
        }
    }
    
    /**
     * This is the special serializer for regular {@link java.lang.Integer}s
     * (and primitive ints)
     *<p>
     * Since this is one of "native" types, no type information is ever
     * included on serialization (unlike for most scalar types)
     *<p>
     * NOTE: as of 2.6, generic signature changed to Object, to avoid generation
     * of bridge methods.
     */
    @JacksonStdImpl
    public final static class IntegerSerializer extends Base<Object>
    {
        public IntegerSerializer() { super(Integer.class, JsonParser.NumberType.INT ,"integer"); }
    
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeNumber(((Integer) value).intValue());
        }
        
        // IMPORTANT: copied from `NonTypedScalarSerializerBase`
        @Override
        public void serializeWithType(Object value, JsonGenerator gen,
                SerializerProvider provider, TypeSerializer typeSer) throws IOException {
            // no type info, just regular serialization
            serialize(value, gen, provider);            
        }
    }

    /**
     * Similar to {@link IntegerSerializer}, but will not cast to Integer:
     * instead, cast is to {@link java.lang.Number}, and conversion is
     * by calling {@link java.lang.Number#intValue}.
     */
    @JacksonStdImpl
    public final static class IntLikeSerializer extends Base<Number>
    {
        final static IntLikeSerializer instance = new IntLikeSerializer();
    
        public IntLikeSerializer() {
            super(Number.class, JsonParser.NumberType.INT, "integer");
        }
        
        @Override
        public void serialize(Number value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeNumber(value.intValue());
        }
    }

    @JacksonStdImpl
    public final static class LongSerializer extends Base<Object>
    {
        final static LongSerializer instance = new LongSerializer();
    
        public LongSerializer() { super(Long.class, JsonParser.NumberType.LONG, "number"); }
        
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeNumber(((Long) value).longValue());
        }
    }
    
    @JacksonStdImpl
    public final static class FloatSerializer extends Base<Float>
    {
        final static FloatSerializer instance = new FloatSerializer();
    
        public FloatSerializer() { super(Float.class, JsonParser.NumberType.FLOAT, "number"); }
        
        @Override
        public void serialize(Float value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeNumber(value.floatValue());
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
    public final static class DoubleSerializer extends Base<Object>
    {
        final static DoubleSerializer instance = new DoubleSerializer();
    
        public DoubleSerializer() { super(Double.class, JsonParser.NumberType.DOUBLE, "number"); }
    
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeNumber(((Double) value).doubleValue());
        }

        // IMPORTANT: copied from `NonTypedScalarSerializerBase`
        @Override
        public void serializeWithType(Object value, JsonGenerator gen,
                SerializerProvider provider, TypeSerializer typeSer) throws IOException {
            // no type info, just regular serialization
            serialize(value, gen, provider);            
        }
    }
}
