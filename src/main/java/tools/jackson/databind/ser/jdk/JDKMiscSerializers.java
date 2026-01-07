package tools.jackson.databind.ser.jdk;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.atomic.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.ser.BasicSerializerFactory;
import tools.jackson.databind.ser.jackson.TokenBufferSerializer;
import tools.jackson.databind.ser.std.NullSerializer;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.databind.util.TokenBuffer;

/**
 * Class that ctxts access to serializers user for non-structured JDK types that
 * are serializer as scalars; some using basic {@link ToStringSerializer},
 * others explicit serializers.
 */
public class JDKMiscSerializers
{
    /**
     * Method called by {@link BasicSerializerFactory} to find one of serializers provided here.
     */
    public static final ValueSerializer<?> find(Class<?> raw)
    {
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
        if (ByteArrayOutputStream.class.isAssignableFrom(raw)) {
            return new ByteArrayOutputStreamSerializer();
        }
        return null;
    }

    /*
    /**********************************************************************
    /* Serializers for atomic types
    /**********************************************************************
     */

    public static class AtomicBooleanSerializer
        extends StdScalarSerializer<AtomicBoolean>
    {
        public AtomicBooleanSerializer() { super(AtomicBoolean.class, false); }

        @Override
        public void serialize(AtomicBoolean value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeBoolean(value.get());
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) {
            visitor.expectBooleanFormat(typeHint);
        }
    }

    public static class AtomicIntegerSerializer
        extends StdScalarSerializer<AtomicInteger>
    {
        public AtomicIntegerSerializer() { super(AtomicInteger.class, false); }

        @Override
        public void serialize(AtomicInteger value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeNumber(value.get());
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
            visitIntFormat(visitor, typeHint, JsonParser.NumberType.INT);
        }
    }

    public static class AtomicLongSerializer
        extends StdScalarSerializer<AtomicLong>
    {
        public AtomicLongSerializer() { super(AtomicLong.class, false); }

        @Override
        public void serialize(AtomicLong value, JsonGenerator gen, SerializationContext ctxt)
            throws JacksonException
        {
            gen.writeNumber(value.get());
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
            visitIntFormat(visitor, typeHint, JsonParser.NumberType.LONG);
        }
    }

    /*
    /**********************************************************************
    /* Serializers, other
    /**********************************************************************
     */

    /**
     * @since 3.0
     */
    public static class ByteArrayOutputStreamSerializer
        extends StdScalarSerializer<ByteArrayOutputStream>
    {
        public ByteArrayOutputStreamSerializer() { super(ByteArrayOutputStream.class, false); }

        @Override
        public void serialize(ByteArrayOutputStream value, JsonGenerator gen,
                SerializationContext ctxt) throws JacksonException
        {
            gen.writeBinary(value.toByteArray());
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        {
            acceptJsonFormatVisitorForBinary(visitor, typeHint);
        }
    }
}
