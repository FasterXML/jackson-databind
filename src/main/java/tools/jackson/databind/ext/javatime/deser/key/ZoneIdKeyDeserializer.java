package tools.jackson.databind.ext.javatime.deser.key;

import java.time.DateTimeException;
import java.time.ZoneId;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;

public class ZoneIdKeyDeserializer extends Jsr310KeyDeserializer {

    public static final ZoneIdKeyDeserializer INSTANCE = new ZoneIdKeyDeserializer();

    private ZoneIdKeyDeserializer() {
        // singleton
    }

    @Override
    protected Object deserialize(String key, DeserializationContext ctxt)
        throws JacksonException
    {
        try {
            return ZoneId.of(key);
        } catch (DateTimeException e) {
            return _handleDateTimeException(ctxt, ZoneId.class, e, key);
        }
    }
}
