package tools.jackson.databind.ser.impl;

import tools.jackson.core.*;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Special bogus "serializer" that will throw
 * {@link DatabindException} if its {@link #serialize}
 * gets invoked. Most commonly registered as handler for unknown types,
 * as well as for catching unintended usage (like trying to use null
 * as Map/Object key).
 */
public class FailingSerializer
    extends StdSerializer<Object>
{
    protected final String _msg;
    
    public FailingSerializer(String msg) {
        super(Object.class);
        _msg = msg;
    }
    
    @Override
    public void serialize(Object value, JsonGenerator g, SerializerProvider provider)
        throws JacksonException
    {
        provider.reportMappingProblem(_msg);
    }
}
