package tools.jackson.databind.ext.javatime.deser.key;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;

public class OffsetDateTimeKeyDeserializer extends Jsr310KeyDeserializer {

    public static final OffsetDateTimeKeyDeserializer INSTANCE = new OffsetDateTimeKeyDeserializer();

    private OffsetDateTimeKeyDeserializer() {
        // singleton
    }

    @Override
    protected OffsetDateTime deserialize(String key, DeserializationContext ctxt)
        throws JacksonException
    {
        try {
            return OffsetDateTime.parse(key, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeException e) {
            return _handleDateTimeException(ctxt, OffsetDateTime.class, e, key);
        }
    }
}
