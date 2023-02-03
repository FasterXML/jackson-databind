package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Interface {@link com.fasterxml.jackson.databind.JsonSerializer} implements
 * to allow for visiting type hierarchy.
 */
public interface JsonFormatVisitable
{
    /**
     * Get the representation of the schema to which this serializer will conform.
     *
     * @param typeHint Type of element (entity like property) being visited
     */
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException;
}
