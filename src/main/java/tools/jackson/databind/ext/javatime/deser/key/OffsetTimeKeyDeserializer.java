package tools.jackson.databind.ext.javatime.deser.key;

import java.time.DateTimeException;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;

public class OffsetTimeKeyDeserializer extends Jsr310KeyDeserializer {

    public static final OffsetTimeKeyDeserializer INSTANCE = new OffsetTimeKeyDeserializer();

    private OffsetTimeKeyDeserializer() {
        // singleton
    }

    @Override
    protected OffsetTime deserialize(String key, DeserializationContext ctxt)
        throws JacksonException
    {
        try {
            return OffsetTime.parse(key, DateTimeFormatter.ISO_OFFSET_TIME);
        } catch (DateTimeException e) {
            return _handleDateTimeException(ctxt, OffsetTime.class, e, key);
        }
    }

}
