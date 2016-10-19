package com.fasterxml.jackson.databind.ser.std;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.BasicSerializerFactory;

/**
 * Class that providers access to serializers user for non-structured JDK types that
 * are serializer as scalars; some using basic {@link ToStringSerializer},
 * others explicit serializers.
 */
@SuppressWarnings("serial")
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
        sers.put(java.net.URL.class, new ToStringSerializer(java.net.URL.class));
        sers.put(java.net.URI.class, new ToStringSerializer(java.net.URI.class));

        sers.put(Currency.class, new ToStringSerializer(Currency.class));
        sers.put(UUID.class, new UUIDSerializer());
        sers.put(java.util.regex.Pattern.class, new ToStringSerializer(java.util.regex.Pattern.class));
        sers.put(Locale.class, new ToStringSerializer(Locale.class));

        // then atomic types (note: AtomicReference defined elsewhere)
        sers.put(AtomicBoolean.class, AtomicBooleanSerializer.class);
        sers.put(AtomicInteger.class, AtomicIntegerSerializer.class);
        sers.put(AtomicLong.class, AtomicLongSerializer.class);

        // then other types that need specialized serializers
        sers.put(File.class, FileSerializer.class);
        sers.put(Class.class, ClassSerializer.class);

        // And then some stranger types... not 100% they are needed but:
        sers.put(Void.class, NullSerializer.instance);
        sers.put(Void.TYPE, NullSerializer.instance);

        // 09-Jan-2015, tatu: As per [databind#1073], let's try to guard against possibility
        //   of some environments missing `java.sql.` types
        try {
            // note: timestamps are very similar to java.util.Date, thus serialized as such
            sers.put(java.sql.Timestamp.class, DateSerializer.instance);
    
            // leave some of less commonly used ones as lazy, no point in proactive construction
            sers.put(java.sql.Date.class, SqlDateSerializer.class);
            sers.put(java.sql.Time.class, SqlTimeSerializer.class);
        } catch (NoClassDefFoundError e) {
            // nothing much we can do here; could log, but probably not useful for now.
        }
        
        return sers.entrySet();
    }

    /*
    /**********************************************************
    /* Serializers for atomic types
    /**********************************************************
     */

    public static class AtomicBooleanSerializer
        extends StdScalarSerializer<AtomicBoolean>
    {
        public AtomicBooleanSerializer() { super(AtomicBoolean.class, false); }
    
        @Override
        public void serialize(AtomicBoolean value, JsonGenerator gen, SerializerProvider provider) throws IOException, JsonGenerationException {
            gen.writeBoolean(value.get());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
            return createSchemaNode("boolean", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
            visitor.expectBooleanFormat(typeHint);
        }
    }
    
    public static class AtomicIntegerSerializer
        extends StdScalarSerializer<AtomicInteger>
    {
        public AtomicIntegerSerializer() { super(AtomicInteger.class, false); }
    
        @Override
        public void serialize(AtomicInteger value, JsonGenerator gen, SerializerProvider provider) throws IOException, JsonGenerationException {
            gen.writeNumber(value.get());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
            return createSchemaNode("integer", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
        {
            visitIntFormat(visitor, typeHint, JsonParser.NumberType.INT);
        }
    }

    public static class AtomicLongSerializer
        extends StdScalarSerializer<AtomicLong>
    {
        public AtomicLongSerializer() { super(AtomicLong.class, false); }
    
        @Override
        public void serialize(AtomicLong value, JsonGenerator gen, SerializerProvider provider) throws IOException, JsonGenerationException {
            gen.writeNumber(value.get());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
            return createSchemaNode("integer", true);
        }
        
        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
            throws JsonMappingException
        {
            visitIntFormat(visitor, typeHint, JsonParser.NumberType.LONG);
        }
    }
}
