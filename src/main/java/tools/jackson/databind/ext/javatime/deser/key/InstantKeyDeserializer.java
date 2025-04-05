package tools.jackson.databind.ext.javatime.deser.key;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;

public class InstantKeyDeserializer extends Jsr310KeyDeserializer {

    public static final InstantKeyDeserializer INSTANCE = new InstantKeyDeserializer();

    private InstantKeyDeserializer() {
        // singleton
    }

    @Override
    protected Instant deserialize(String key, DeserializationContext ctxt)
        throws JacksonException
    {
        try {
            return DateTimeFormatter.ISO_INSTANT.parse(key, Instant::from);
        } catch (DateTimeException e) {
            return _handleDateTimeException(ctxt, Instant.class, e, key);
        }
    }
}
