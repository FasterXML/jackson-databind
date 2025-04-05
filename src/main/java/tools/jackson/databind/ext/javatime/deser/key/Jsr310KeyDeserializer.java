package tools.jackson.databind.ext.javatime.deser.key;

import java.time.DateTimeException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.util.ClassUtil;

abstract class Jsr310KeyDeserializer extends KeyDeserializer
{
    @Override
    public final Object deserializeKey(String key, DeserializationContext ctxt)
        throws JacksonException
    {
        // 17-Aug-2019, tatu: Jackson 2.x had special handling for "null" key marker, which
        //    is why we have this unnecessary dispatching, for now
        return deserialize(key, ctxt);
    }

    protected abstract Object deserialize(String key, DeserializationContext ctxt)
        throws JacksonException;

    @SuppressWarnings("unchecked")
    protected <T> T _handleDateTimeException(DeserializationContext ctxt,
              Class<?> type, DateTimeException e0, String value)
          throws JacksonException
    {
        try {
            return (T) ctxt.handleWeirdKey(type, value,
                    "Failed to deserialize %s: (%s) %s",
                    ClassUtil.nameOf(type),
                    e0.getClass().getName(),
                    e0.getMessage());

        } catch (JacksonException e) {
            e.initCause(e0);
            throw e;
        }
    }
}
