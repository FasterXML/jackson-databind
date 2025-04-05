package tools.jackson.databind.ext.javatime.deser.key;

import java.time.DateTimeException;
import java.time.ZonedDateTime;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;

public class ZonedDateTimeKeyDeserializer extends Jsr310KeyDeserializer {

    public static final ZonedDateTimeKeyDeserializer INSTANCE = new ZonedDateTimeKeyDeserializer();

    protected ZonedDateTimeKeyDeserializer() {
        // singleton
    }

    @Override
    protected ZonedDateTime deserialize(String key, DeserializationContext ctxt)
        throws JacksonException
    {
        try {
            // Not supplying a formatter allows the use of all supported formats
            return ZonedDateTime.parse(key);
        } catch (DateTimeException e) {
            return _handleDateTimeException(ctxt, ZonedDateTime.class, e, key);
        }
    }
}
