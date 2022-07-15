package tools.jackson.databind.jsonFormatVisitors;

import tools.jackson.databind.JavaType;

/**
 * Interface {@link tools.jackson.databind.ValueSerializer} implements
 * to allow for visiting type hierarchy.
 */
public interface JsonFormatVisitable
{
    /**
     * Get the representation of the schema to which this serializer will conform.
     * 
     * @param typeHint Type of element (entity like property) being visited
     */
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint);
}
