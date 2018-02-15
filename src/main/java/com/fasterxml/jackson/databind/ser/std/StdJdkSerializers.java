package com.fasterxml.jackson.databind.ser.std;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.BasicSerializerFactory;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Class that providers access to serializers user for non-structured JDK types that
 * are serializer as scalars; some using basic {@link ToStringSerializer},
 * others explicit serializers.
 */
@SuppressWarnings("serial")
public class StdJdkSerializers
{
    /**
     * Method called by {@link BasicSerializerFactory} to find one of serializers provided here.
     */
    public static final JsonSerializer<?> find(Class<?> raw)
    {
        JsonSerializer<?> ser = StringLikeSerializer.find(raw);
        if (ser != null) {
            return ser;
        }
        if (raw == UUID.class) {
            return new UUIDSerializer();
        }
        if (raw == AtomicBoolean.class) {
            return new AtomicBooleanSerializer();
        }
        if (raw == AtomicInteger.class) {
            return new AtomicIntegerSerializer();
        }
        if (raw == AtomicLong.class) {
            return new AtomicLongSerializer();
        }
        // Jackson-specific type(s)
        // (Q: can this ever be sub-classed?)
        if (raw == TokenBuffer.class) {
            return new TokenBufferSerializer();
        }
        // And then some stranger types... not 100% they are needed but:
        if ((raw == Void.class) || (raw == Void.TYPE)) { 
            return NullSerializer.instance;
        }
        if (raw.getName().startsWith("java.sql."))  {
            return _findSqlType(raw);
        }
        return null;
    }

    private static JsonSerializer<?> _findSqlType(Class<?> raw) {
        try {
            // note: timestamps are very similar to java.util.Date, thus serialized as such
            if (raw == java.sql.Timestamp.class) {
                return DateSerializer.instance;
            }
            if (raw == java.sql.Date.class) {
                return new SqlDateSerializer();
            }
            if (raw == java.sql.Time.class) {
                return new SqlTimeSerializer();
            }
        } catch (NoClassDefFoundError e) {
            // nothing much we can do here; could log, but probably not useful for now.
        }
        return null;
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
        public void serialize(AtomicBoolean value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeBoolean(value.get());
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
        public void serialize(AtomicInteger value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeNumber(value.get());
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
        public void serialize(AtomicLong value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeNumber(value.get());
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
            throws JsonMappingException
        {
            visitIntFormat(visitor, typeHint, JsonParser.NumberType.LONG);
        }
    }
}
