package tools.jackson.databind.ext.javatime.deser.key;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;

public class LocalTimeKeyDeserializer extends Jsr310KeyDeserializer {

    public static final LocalTimeKeyDeserializer INSTANCE = new LocalTimeKeyDeserializer();

    private LocalTimeKeyDeserializer() {
        // singleton
    }

    @Override
    protected LocalTime deserialize(String key, DeserializationContext ctxt)
        throws JacksonException
    {
        try {
            return LocalTime.parse(key, DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (DateTimeException e) {
            return _handleDateTimeException(ctxt, LocalTime.class, e, key);
        }
    }

}
