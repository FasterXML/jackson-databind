package tools.jackson.databind.ext.javatime.ser;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;

import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Base class that indicates that all JSR310 datatypes are serialized as scalar JSON types.
 *
 * @author Nick Williams
 */
abstract class JSR310SerializerBase<T> extends StdSerializer<T>
{
    protected JSR310SerializerBase(Class<?> supportedType) {
        super(supportedType);
    }

    @Override
    public void serializeWithType(T value, JsonGenerator g, SerializationContext ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, serializationShape(ctxt)));
        serialize(value, g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    /**
     * Overridable helper method used from {@link #serializeWithType}, to indicate
     * shape of value during serialization; needed to know how type id is to be
     * serialized.
     */
    protected abstract JsonToken serializationShape(SerializationContext ctxt);
}
