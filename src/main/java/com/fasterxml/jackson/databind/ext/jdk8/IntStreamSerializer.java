package com.fasterxml.jackson.databind.ext.jdk8;

import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * {@link IntStream} serializer
 * <p>
 * Unfortunately there to common ancestor between number base stream, so we need to define each in a specific class
 * </p>
 */
public class IntStreamSerializer extends StdSerializer<IntStream>
{
    /**
     * Singleton instance
     */
    public static final IntStreamSerializer INSTANCE = new IntStreamSerializer();

    /**
     * Constructor
     */
    private IntStreamSerializer() {
        super(IntStream.class);
    }

    @Override
    public void serialize(IntStream stream, JsonGenerator g, SerializerProvider ctxt)
        throws JacksonException
    {
        try (IntStream is = stream) {
            g.writeStartArray(is);
            is.forEach(value -> {
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
