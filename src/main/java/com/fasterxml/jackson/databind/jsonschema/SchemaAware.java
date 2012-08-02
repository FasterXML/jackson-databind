package com.fasterxml.jackson.databind.jsonschema;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonFormatVisitor;

/**
 * Marker interface for schema-aware serializers.
 */
public interface SchemaAware
{
    /**
     * Get the representation of the schema to which this serializer will conform.
     * @param typeHint TODO
     *
     * @return <a href="http://json-schema.org/">Json-schema</a> for this serializer.
     */
    public void acceptJsonFormatVisitor(JsonFormatVisitor visitor, JavaType typeHint);
}
