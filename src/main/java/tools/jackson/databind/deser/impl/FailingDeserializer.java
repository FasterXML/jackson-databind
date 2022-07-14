package tools.jackson.databind.deser.impl;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Special bogus "serializer" that will throw
 * {@link tools.jackson.databind.exc.MismatchedInputException} if an attempt is made to deserialize
 * a value. This is used as placeholder to avoid NPEs for uninitialized
 * structured serializers or handlers.
 */
public class FailingDeserializer extends StdDeserializer<Object>
{
    protected final String _message;

    public FailingDeserializer(String m) {
        this(Object.class, m);
    }

    public FailingDeserializer(Class<?> rawType, String m) {
        super(rawType);
        _message = m;
    }
    
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        ctxt.reportInputMismatch(this, _message);
        return null;
    }
}
