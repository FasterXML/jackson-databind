package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * This singleton node class is generated to denote "missing nodes"
 * along paths that do not exist. For example, if a path via
 * element of an array is requested for an element outside range
 * of elements in the array; or for a non-array value, result
 * will be reference to this node.
 *<p>
 * In most respects this placeholder node will act as {@link NullNode};
 * for example, for purposes of value conversions, value is considered
 * to be null and represented as value zero when used for numeric
 * conversions. 
 */
public final class MissingNode
    extends BaseJsonNode // NOTE! Does NOT extend `ValueNode` unlike in 2.x
{
    private static final long serialVersionUID = 3L;

    private final static MissingNode instance = new MissingNode();

    protected MissingNode() { }

    // Immutable: no need to copy
    @SuppressWarnings("unchecked")
    @Override
    public <T extends JsonNode> T deepCopy() { return (T) this; }

    public static MissingNode getInstance() { return instance; }

    // To support JDK serialization, recovery of Singleton instance
    protected Object readResolve() {
        return instance;
    }
    
    @Override
    public JsonNodeType getNodeType() {
        return JsonNodeType.MISSING;
    }

    @Override
    public final boolean isMissingNode() {
        return true;
    }

    @Override public JsonToken asToken() { return JsonToken.NOT_AVAILABLE; }

    @Override public String asText() { return ""; }

    @Override public String asText(String defaultValue) { return defaultValue; }
    
    // // Note: not a numeric node, hence default 'asXxx()' are fine:
    
    /*
    public int asInt(int defaultValue);
    public long asLong(long defaultValue);
    public double asDouble(double defaultValue);
    public boolean asBoolean(boolean defaultValue);
    */
    
    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        /* Nothing to output... should we signal an error tho?
         * Chances are, this is an erroneous call. For now, let's
         * not do that; serialize as explicit null. Why? Because we
         * cannot just omit a value as JSON Object field name may have
         * been written out.
         */
        jg.writeNull();
    }

    @Override
    public void serializeWithType(JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        g.writeNull();
    }
    
    @Override
    public boolean equals(Object o)
    {
        /* Hmmh. Since there's just a singleton instance, this
         * fails in all cases but with identity comparison.
         * However: if this placeholder value was to be considered
         * similar to SQL NULL, it shouldn't even equal itself?
         * That might cause problems when dealing with collections
         * like Sets... so for now, let's let identity comparison
         * return true.
         */
        return (o == this);
    }

    @Override
    public int hashCode() {
        return JsonNodeType.MISSING.ordinal();
    }

    @Override
    public JsonNode get(int index) {
        return null;
    }

    @Override
    public JsonNode path(String fieldName) { return this; }

    @Override
    public JsonNode path(int index) { return this; }

    @Override
    protected JsonNode _at(JsonPointer ptr) {
        return this;
    }

    @Override
    public JsonNode findValue(String fieldName) {
        return null;
    }

    @Override
    public JsonNode findParent(String fieldName) {
        return null;
    }

    @Override
    public List<JsonNode> findValues(String fieldName, List<JsonNode> foundSoFar) {
        return foundSoFar;
    }

    @Override
    public List<String> findValuesAsText(String fieldName, List<String> foundSoFar) {
        return foundSoFar;
    }

    @Override
    public List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar) {
        return foundSoFar;
    }
}
