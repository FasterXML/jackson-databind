package tools.jackson.databind.ser.jackson;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * This is a simple dummy serializer that will just output raw values by calling
 * toString() on value to serialize.
 */
public class RawSerializer<T>
    extends StdSerializer<T>
{
    /**
     * Constructor takes in expected type of values; but since caller
     * typically cannot really provide actual type parameter, we will
     * just take wild card and coerce type.
     */
    public RawSerializer(Class<?> cls) {
        super(cls);
    }

    @Override
    public void serialize(T value, JsonGenerator g, SerializerProvider ctxt) throws JacksonException {
        g.writeRawValue(value.toString());
    }

    @Override
    public void serializeWithType(T value, JsonGenerator g, SerializerProvider ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, JsonToken.VALUE_EMBEDDED_OBJECT));
        serialize(value, g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        // type not really known, but since it is a JSON string:
        visitStringFormat(visitor, typeHint);
    }
}
