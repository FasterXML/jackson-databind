package com.fasterxml.jackson.databind.ser.std;

import java.lang.reflect.Type;
import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * This is a simple dummy serializer that will just output raw values by calling
 * toString() on value to serialize.
 */
@SuppressWarnings("serial")
public class RawSerializer<T>
    extends StdSerializer<T>
{
    /**
     * Constructor takes in expected type of values; but since caller
     * typically cannot really provide actual type parameter, we will
     * just take wild card and coerce type.
     */
    public RawSerializer(Class<?> cls) {
        super(cls, false);
    }

    @Override
    public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeRawValue(value.toString());
    }

    @Override
    public void serializeWithType(T value, JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                typeSer.typeId(value, JsonToken.VALUE_EMBEDDED_OBJECT));
        serialize(value, g, provider);
        typeSer.writeTypeSuffix(g, typeIdDef);
    }

    /**
     * @deprecated Since 2.15
     */
    @Deprecated
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        // type not really known, but since it is a JSON string:
        return createSchemaNode("string", true);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
    {
        // type not really known, but since it is a JSON string:
        visitStringFormat(visitor, typeHint);
    }
}
