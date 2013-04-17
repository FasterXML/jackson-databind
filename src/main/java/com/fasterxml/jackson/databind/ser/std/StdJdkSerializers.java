package com.fasterxml.jackson.databind.ser.std;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import com.fasterxml.jackson.databind.ser.BasicSerializerFactory;

/**
 * Class that providers access to serializers user for non-structured JDK types that
 * are serializer as scalars; some using basic {@link ToStringSerializer},
 * others explicit serializers.
 */
public class StdJdkSerializers
{
    /**
     * Method called by {@link BasicSerializerFactory} to access
     * all serializers this class provides.
     */
    public static Collection<Map.Entry<Class<?>, Object>> all()
    {
        HashMap<Class<?>,Object> sers = new HashMap<Class<?>,Object>();

        // First things that 'toString()' can handle
        final ToStringSerializer sls = ToStringSerializer.instance;

        sers.put(java.net.URL.class, sls);
        sers.put(java.net.URI.class, sls);

        sers.put(Currency.class, sls);
        sers.put(UUID.class, sls);
        sers.put(java.util.regex.Pattern.class, sls);
        sers.put(Locale.class, sls);

        // starting with 1.7, use compact String for Locale
        sers.put(Locale.class, sls);
        
        // then atomic types
        sers.put(AtomicReference.class, AtomicReferenceSerializer.class);
        sers.put(AtomicBoolean.class, AtomicBooleanSerializer.class);
        sers.put(AtomicInteger.class, AtomicIntegerSerializer.class);
        sers.put(AtomicLong.class, AtomicLongSerializer.class);
        
        // then types that need specialized serializers
        sers.put(File.class, FileSerializer.class);
        sers.put(Class.class, ClassSerializer.class);

        // And then some stranger types... not 100% they are needed but:
        sers.put(Void.TYPE, NullSerializer.class);
        
        return sers.entrySet();
    }

    /*
    /**********************************************************
    /* Serializers for atomic types
    /**********************************************************
     */

    public final static class AtomicBooleanSerializer
        extends StdScalarSerializer<AtomicBoolean>
    {
        public AtomicBooleanSerializer() { super(AtomicBoolean.class, false); }
    
        @Override
        public void serialize(AtomicBoolean value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeBoolean(value.get());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("boolean", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
                throws JsonMappingException
        {
            visitor.expectBooleanFormat(typeHint);
        }
    }
    
    public final static class AtomicIntegerSerializer
        extends StdScalarSerializer<AtomicInteger>
    {
        public AtomicIntegerSerializer() { super(AtomicInteger.class, false); }
    
        @Override
        public void serialize(AtomicInteger value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.get());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("integer", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
                throws JsonMappingException
        {
            JsonIntegerFormatVisitor v2 = visitor.expectIntegerFormat(typeHint);
            if (v2 != null) {
                v2.numberType(JsonParser.NumberType.INT);
            }
        }
    }

    public final static class AtomicLongSerializer
        extends StdScalarSerializer<AtomicLong>
    {
        public AtomicLongSerializer() { super(AtomicLong.class, false); }
    
        @Override
        public void serialize(AtomicLong value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.get());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("integer", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
                throws JsonMappingException
        {
            JsonIntegerFormatVisitor v2 = visitor.expectIntegerFormat(typeHint);
            if (v2 != null) {
                v2.numberType(JsonParser.NumberType.LONG);
            }
        }
    }
    
    public final static class AtomicReferenceSerializer
        extends StdSerializer<AtomicReference<?>>
    {
        public AtomicReferenceSerializer() { super(AtomicReference.class, false); }

        @Override
        public void serialize(AtomicReference<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            provider.defaultSerializeValue(value.get(), jgen);
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("any", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
                throws JsonMappingException
        {
            visitor.expectAnyFormat(typeHint);
        }
    }
    
    /*
    /**********************************************************
    /* Specialized serializers, referential types
    /**********************************************************
     */

    /**
     * For now, File objects get serialized by just outputting
     * absolute (but not canonical) name as String value
     */
    public final static class FileSerializer
        extends StdScalarSerializer<File>
    {
        public FileSerializer() { super(File.class); }

        @Override
        public void serialize(File value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.getAbsolutePath());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("string", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
                throws JsonMappingException
        {
            visitor.expectStringFormat(typeHint);
        }
    }

    /**
     * Also: default bean access will not do much good with Class.class. But
     * we can just serialize the class name and that should be enough.
     */
    public final static class ClassSerializer
        extends StdScalarSerializer<Class<?>>
    {
        public ClassSerializer() { super(Class.class, false); }

        @Override
        public void serialize(Class<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.getName());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("string", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
                throws JsonMappingException
        {
            visitor.expectStringFormat(typeHint);
        }
    }
}
