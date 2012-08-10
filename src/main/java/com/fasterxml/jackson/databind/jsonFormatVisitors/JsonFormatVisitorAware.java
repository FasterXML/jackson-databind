package com.fasterxml.jackson.databind.jsonFormatVisitors;


import com.fasterxml.jackson.databind.JavaType;

/**
 * Marker interface for schema-aware serializers.
 */
public interface JsonFormatVisitorAware
{
    /**
     * Get the representation of the schema to which this serializer will conform.
     * @param typeHint TODO
     *
     * @return <a href="http://json-schema.org/">Json-schema</a> for this serializer.
     */
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint);
}
