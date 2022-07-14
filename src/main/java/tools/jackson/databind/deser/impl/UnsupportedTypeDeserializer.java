package tools.jackson.databind.deser.impl;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Special bogus "serializer" that will throw
 * {@link tools.jackson.databind.exc.MismatchedInputException}
 * if an attempt is made to deserialize a value.
 * This is used for "known unknown" types: types that we can recognize
 * but can not support easily (or support known to be added via extension
 * module).
 *<p>
 * NOTE:does allow deserialization from
 * {@code JsonToken.VALUE_EMBEDDED_OBJECT} if type matches (or is {@code null}).
 */
public class UnsupportedTypeDeserializer extends StdDeserializer<Object>
{
    protected final JavaType _type;

    protected final String _message;

    public UnsupportedTypeDeserializer(JavaType t, String m) {
        super(t);
        _type = t;
        _message = m;
    }
    
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        // 26-May-2021, tatu: For [databind#3091], do allow reads from embedded values
        if (p.currentToken() == JsonToken.VALUE_EMBEDDED_OBJECT) {
            Object value = p.getEmbeddedObject();
            if ((value == null) || _type.getRawClass().isAssignableFrom(value.getClass())) {
                return value;
            }
        }
        ctxt.reportBadDefinition(_type, _message);
        return null;
    }
}
