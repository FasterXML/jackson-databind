package com.fasterxml.jackson.databind;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Interface that is to replace {@link JsonSerializable} to
 * allow for dynamic type information embedding.
 * 
 * @author tatu
 */
@SuppressWarnings("deprecation")
public interface JsonSerializableWithType
    extends JsonSerializable
{
    public void serializeWithType(JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException;
}
