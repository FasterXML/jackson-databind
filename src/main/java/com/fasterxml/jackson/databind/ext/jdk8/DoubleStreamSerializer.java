package com.fasterxml.jackson.databind.ext.jdk8;

import java.util.stream.DoubleStream;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * {@link DoubleStream} serializer
 * <p>
 * Unfortunately there to common ancestor between number base stream,
 * so we need to define each in a specific class
 * </p>
 */
public class DoubleStreamSerializer extends StdSerializer<DoubleStream>
{
    /**
     * Singleton instance
     */
    public static final DoubleStreamSerializer INSTANCE = new DoubleStreamSerializer();

    private DoubleStreamSerializer() {
        super(DoubleStream.class);
    }

    @Override
    public void serialize(DoubleStream stream, JsonGenerator g, SerializerProvider ctxt)
        throws JacksonException
    {
        try (final DoubleStream ds = stream) {
            g.writeStartArray(ds);
            ds.forEach(value -> {
                g.writeNumber(value);
            });
            g.writeEndArray();
        } catch (Exception e) {
            // For most regular serializers we won't both handling but streams are typically
            // root values so 
            wrapAndThrow(ctxt, e, stream, g.streamWriteContext().getCurrentIndex());
        }
    }
}
