package com.fasterxml.jackson.databind.ext.jdk8;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.IntStream;

/**
 * {@link IntStream} serializer
 * <p>
 * Unfortunately there to common ancestor between number base stream, so we need to define each in a specific class
 * </p>
 */
public class IntStreamSerializer extends StdSerializer<IntStream>
{
    private static final long serialVersionUID = 1L;

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
    public void serialize(IntStream stream, JsonGenerator g, SerializerProvider provider) throws IOException {

        try(IntStream is = stream) {
            g.writeStartArray();
            is.forEach(value -> {
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
