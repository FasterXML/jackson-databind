package tools.jackson.databind.ext.javatime.deser.key;

import java.time.DateTimeException;
import java.time.Duration;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;

public class DurationKeyDeserializer extends Jsr310KeyDeserializer {

    public static final DurationKeyDeserializer INSTANCE = new DurationKeyDeserializer();

    private DurationKeyDeserializer() {
        // singleton
    }

    @Override
    protected Duration deserialize(String key, DeserializationContext ctxt)
        throws JacksonException
    {
        try {
            return Duration.parse(key);
        } catch (DateTimeException e) {
            return _handleDateTimeException(ctxt, Duration.class, e, key);
        }
    }
}
