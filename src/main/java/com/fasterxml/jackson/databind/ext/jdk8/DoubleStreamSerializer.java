package com.fasterxml.jackson.databind.ext.jdk8;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.DoubleStream;

/**
 * {@link DoubleStream} serializer
 * <p>
 * Unfortunately there to common ancestor between number base stream,
 * so we need to define each in a specific class
 * </p>
 */
public class DoubleStreamSerializer extends StdSerializer<DoubleStream>
{
    private static final long serialVersionUID = 1L;

    /**
     * Singleton instance
     */
    public static final DoubleStreamSerializer INSTANCE = new DoubleStreamSerializer();

    /**
     * Constructor
     */
    private DoubleStreamSerializer() {
        super(DoubleStream.class);
    }

    @Override
    public void serialize(DoubleStream stream, JsonGenerator g, SerializerProvider provider) throws IOException {
        
        try(DoubleStream ds = stream) {
            g.writeStartArray();
            ds.forEach(value -> {
                try {
                    g.writeNumber(value);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            g.writeEndArray();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } 
    }
}
