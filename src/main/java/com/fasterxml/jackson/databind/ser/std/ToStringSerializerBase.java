package com.fasterxml.jackson.databind.ser.std;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Intermediate base class that serves as base for standard {@link ToStringSerializer}
 * as well as for custom subtypes that want to add processing for converting from
 * value to output into its {@code String} representation (whereas standard version
 * simply calls value object's {@code toString()} method).
 */
public abstract class ToStringSerializerBase
    extends StdSerializer<Object>
{
    public ToStringSerializerBase(Class<?> handledType) {
        super(handledType);
    }

    @Override
    public boolean isEmpty(SerializerProvider prov, Object value) {
        return valueToString(value).isEmpty();
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        gen.writeString(valueToString(value));
    }

    /**
     * Default implementation will write type prefix, call regular serialization
     * method (since assumption is that value itself does not need JSON
     * Array or Object start/end markers), and then write type suffix.
     * This should work for most cases; some sub-classes may want to
     * change this behavior.
     */
    @Override
    public void serializeWithType(Object value, JsonGenerator g, SerializerProvider ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, JsonToken.VALUE_STRING));
        serialize(value, g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        visitStringFormat(visitor, typeHint);
    }

    public abstract String valueToString(Object value);
}
