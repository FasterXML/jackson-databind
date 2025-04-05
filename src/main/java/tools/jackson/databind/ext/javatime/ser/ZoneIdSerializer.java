package tools.jackson.databind.ext.javatime.ser;

import java.time.ZoneId;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.ToStringSerializerBase;

public class ZoneIdSerializer extends ToStringSerializerBase
{
    public ZoneIdSerializer() { super(ZoneId.class); }

    @Override
    public void serializeWithType(Object value, JsonGenerator g,
            SerializationContext ctxt, TypeSerializer typeSer)
        throws JacksonException
    {
        // Better ensure we don't use specific sub-classes:
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, ZoneId.class, JsonToken.VALUE_STRING));
        serialize(value, g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    @Override
    public String valueToString(Object value) {
        return value.toString();
    }
}
