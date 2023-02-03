package com.fasterxml.jackson.databind.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.lang.reflect.Type;

/**
 * Marker interface for schema-aware serializers.
 *
 * @deprecated Since 2.15, we recommend use of external
 * <a href="https://github.com/FasterXML/jackson-module-jsonSchema">JSON Schema generator module</a>
 */
@Deprecated
public interface SchemaAware
{
    /**
     * Get the representation of the schema to which this serializer will conform.
     *
     * @param provider The serializer provider.
     * @param typeHint A hint about the type.
     * @return <a href="http://json-schema.org/">Json-schema</a> for this serializer.
     */
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException;

    /**
     * Get the representation of the schema to which this serializer will conform.
     *
     * @param provider The serializer provider.
     * @param isOptional Is the type optional
     * @param typeHint A hint about the type.
     * @return <a href="http://json-schema.org/">Json-schema</a> for this serializer.
     */
    public JsonNode getSchema(SerializerProvider provider, Type typeHint, boolean isOptional)
        throws JsonMappingException;
}
