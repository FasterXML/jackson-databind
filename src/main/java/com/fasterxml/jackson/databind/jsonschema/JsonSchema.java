package com.fasterxml.jackson.databind.jsonschema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Container for a logical JSON Schema instance.
 * Internally schema data is stored as a JSON Tree
 * (instance of {@link JsonNode} is the root
 * of schema document)
 *
 * @author Ryan Heaton
 * @see <a href="http://json-schema.org/">JSON Schema</a>
 *
 * @deprecated Since 2.2, we recommend use of external
 *   <a href="https://github.com/FasterXML/jackson-module-jsonSchema">JSON Schema generator module</a>
 */
@Deprecated
public class JsonSchema
{
    private final ObjectNode schema;

    /**
     * Main constructor for schema instances.
     *<p>
     * This is the creator constructor used by Jackson itself when
     * deserializing instances. It is so-called delegating creator,
     * meaning that its argument will be bound by Jackson before
     * constructor gets called.
     */
    @JsonCreator
    public JsonSchema(ObjectNode schema)
    {
        this.schema = schema;
    }

    /**
     * Method for accessing root JSON object of the contained schema.
     *<p>
     * Note: this method is specified with {@link JsonValue} annotation
     * to represent serialization to use; same as if explicitly
     * serializing returned object.
     *
     * @return Root node of the schema tree
     */
    @JsonValue
    public ObjectNode getSchemaNode()
    {
        return schema;
    }

    @Override
    public String toString()
    {
        return this.schema.toString();
    }

    @Override
    public int hashCode()
    {
        return schema.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof JsonSchema)) return false;

        JsonSchema other = (JsonSchema) o;
        if (schema == null) {
            return other.schema == null;
        }
        return schema.equals(other.schema);
    }

    /**
     * Get the default schema node.
     *
     * @return The default schema node.
     */
    public static JsonNode getDefaultSchemaNode()
    {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("type", "any");
        // "required" is false by default, no need to include
        //objectNode.put("required", false);
        return objectNode;
    }

}
