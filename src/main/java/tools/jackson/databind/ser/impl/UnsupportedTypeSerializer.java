package tools.jackson.databind.ser.impl;

import tools.jackson.core.*;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Special bogus "serializer" that will throw
 * {@link tools.jackson.databind.exc.InvalidDefinitionException} if its {@link #serialize}
 * gets invoked. Most commonly registered as handler for unknown types,
 * as well as for catching unintended usage (like trying to use null
 * as Map/Object key).
 */
public class UnsupportedTypeSerializer
    extends StdSerializer<Object>
{
    protected final JavaType _type;

    protected final String _message;

    public UnsupportedTypeSerializer(JavaType t, String msg) {
        super(Object.class);
        _type = t;
        _message = msg;
    }

    @Override
    public void serialize(Object value, JsonGenerator g, SerializerProvider ctxt) throws JacksonException {
        ctxt.reportBadDefinition(_type, _message);
    }
}
