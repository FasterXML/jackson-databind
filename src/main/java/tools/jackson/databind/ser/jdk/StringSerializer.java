package tools.jackson.databind.ser.jdk;

import tools.jackson.core.*;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdScalarSerializer;

/**
 * This is the special serializer for regular {@link java.lang.String}s.
 *<p>
 * Since this is one of "native" types, no type information is ever
 * included on serialization (unlike for most scalar types as of 1.5)
 */
@JacksonStdImpl
public final class StringSerializer
    extends StdScalarSerializer<Object>
{
    public final static StringSerializer instance = new StringSerializer();
    
    public StringSerializer() { super(String.class, false); }

    @Override
    public boolean isEmpty(SerializerProvider prov, Object value) {
        String str = (String) value;
        return str.isEmpty();
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        gen.writeString((String) value);
    }

    @Override
    public final void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws JacksonException
    {
        // no type info, just regular serialization
        gen.writeString((String) value);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) {
        visitStringFormat(visitor, typeHint);
    }
}
