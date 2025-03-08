package tools.jackson.databind.ser.jdk;

import java.util.Map;
import java.util.Map.Entry;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer used to serialize Map.Entry as POJOs: that is, as if
 * introspected as POJOs so that there's intermediate "key" and "value"
 * properties.
 *<p>
 * TODO: does not fully handle contextualization, type resolution and so on.
 */
public class MapEntryAsPOJOSerializer extends StdSerializer<Map.Entry<?,?>>
{
    protected MapEntryAsPOJOSerializer(JavaType type) {
        super(type);
    }

    public static MapEntryAsPOJOSerializer create(SerializationContext ctxt,
            JavaType type)
    {
        return new MapEntryAsPOJOSerializer(type);
    }

    @Override
    public void serialize(Entry<?, ?> value, JsonGenerator gen, SerializationContext ctxt)
    {
        gen.writeStartObject(value);
        ctxt.defaultSerializeProperty("key", value.getKey(), gen);
        ctxt.defaultSerializeProperty("value", value.getValue(), gen);
        gen.writeEndObject();
    }
}
