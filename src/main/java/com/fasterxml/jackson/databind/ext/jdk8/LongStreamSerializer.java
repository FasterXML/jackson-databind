package com.fasterxml.jackson.databind.ext.jdk8;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.LongStream;

/**
 * {@link LongStream} serializer
 * <p>
 * Unfortunately there to common ancestor between number base stream, so we need to define each in a specific class
 * </p>
 */
public class LongStreamSerializer extends StdSerializer<LongStream>
{
    private static final long serialVersionUID = 1L;

    /**
     * Singleton instance
     */
    public static final LongStreamSerializer INSTANCE = new LongStreamSerializer();

    /**
     * Constructor
     */
    private LongStreamSerializer() {
        super(LongStream.class);
    }

    @Override
    public void serialize(LongStream stream, JsonGenerator g, SerializerProvider provider) throws IOException {
        try (LongStream ls = stream) {
            g.writeStartArray();
            ls.forEach(value -> {
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
