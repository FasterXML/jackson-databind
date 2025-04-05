package tools.jackson.databind.ext.javatime.deser.key;

import java.time.DateTimeException;
import java.time.ZoneOffset;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;

public class ZoneOffsetKeyDeserializer extends Jsr310KeyDeserializer {

    public static final ZoneOffsetKeyDeserializer INSTANCE = new ZoneOffsetKeyDeserializer();

    private ZoneOffsetKeyDeserializer() {
        // singleton
    }

    @Override
    protected ZoneOffset deserialize(String key, DeserializationContext ctxt)
        throws JacksonException
    {
        try {
            return ZoneOffset.of(key);
        } catch (DateTimeException e) {
            return _handleDateTimeException(ctxt, ZoneOffset.class, e, key);
        }
    }
}
