package tools.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.jsontype.TypeSerializer;

/**
 * Special {@link TypeSerializer} implementation used to explicitly
 * block type serialization. This is used when a property or class
 * is annotated with {@code @JsonTypeInfo(use = Id.NONE)}, indicating
 * that type information should not be included even if the value type
 * has a class-level type info annotation.
 *<p>
 * Unlike returning {@code null} (which means "no special type handling,
 * use defaults"), this actively prevents type information from being written.
 *
 * @since 3.1
 */
public class NoOpTypeSerializer extends TypeSerializer
{
    private static final NoOpTypeSerializer INSTANCE = new NoOpTypeSerializer();

    private NoOpTypeSerializer() { }

    public static NoOpTypeSerializer instance() {
        return INSTANCE;
    }

    @Override
    public TypeSerializer forProperty(SerializationContext ctxt, BeanProperty prop) {
        return this;
    }

    @Override
    public JsonTypeInfo.As getTypeInclusion() {
        return JsonTypeInfo.As.NOTHING;
    }

    @Override
    public String getPropertyName() {
        return null;
    }

    @Override
    public TypeIdResolver getTypeIdResolver() {
        return null;
    }

    @Override
    public WritableTypeId writeTypePrefix(JsonGenerator g,
            SerializationContext ctxt, WritableTypeId typeId)
        throws JacksonException
    {
        // Write the value start token if needed, but NO type information
        if (typeId.valueShape == JsonToken.START_OBJECT) {
            g.writeStartObject(typeId.forValue);
        } else if (typeId.valueShape == JsonToken.START_ARRAY) {
            g.writeStartArray();
        }
        // 1. Start marker (part of value) was written but
        // 2. No value wrapper was written.
        typeId.wrapperWritten = false;
        return typeId;
    }

    @Override
    public WritableTypeId writeTypeSuffix(JsonGenerator g,
            SerializationContext ctxt, WritableTypeId typeId)
        throws JacksonException
    {
        // Write the value end token if needed, but no wrapper to close
        if (typeId.valueShape == JsonToken.START_OBJECT) {
            g.writeEndObject();
        } else if (typeId.valueShape == JsonToken.START_ARRAY) {
            g.writeEndArray();
        }
        return typeId;
    }
}
