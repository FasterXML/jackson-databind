package com.fasterxml.jackson.databind.node;

import java.io.IOException;

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
    extends ValueNode
{
    private final static MissingNode instance = new MissingNode();

    private MissingNode() { }

    // Immutable: no need to copy
    @SuppressWarnings("unchecked")
    @Override
    public <T extends JsonNode> T deepCopy() { return (T) this; }
    
    public static MissingNode getInstance() { return instance; }

    @Override
    public JsonNodeType getNodeType()
    {
        return JsonNodeType.MISSING;
    }

    @Override public JsonToken asToken() { return JsonToken.NOT_AVAILABLE; }

    @Override
    public String asText() { return ""; }

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
         * can not just omit a value as JSON Object field name may have
         * been written out.
         */
        jg.writeNull();
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        jg.writeNull();
    }
    
    @Override
    public boolean equals(Object o)
    {
        /* Hmmh. Since there's just a singleton instance, this
         * fails in all cases but with identity comparison.
         * However: if this placeholder value was to be considered
         * similar to Sql NULL, it shouldn't even equal itself?
         * That might cause problems when dealing with collections
         * like Sets... so for now, let's let identity comparison
         * return true.
         */
        return (o == this);
    }

    @Override
    public String toString()
    {
        // toString() should never return null
        return "";
    }
}
