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
    private static final long serialVersionUID = 1L;

    private final static MissingNode instance = new MissingNode();

    /**
     *<p>
     * NOTE: visibility raised to `protected` in 2.9.3 to allow custom subtypes
     * (which may not be greatest idea ever to have but was requested)
     */
    protected MissingNode() { }

    // To support JDK serialization, recovery of Singleton instance
    protected Object readResolve() {
        return instance;
    }

    @Override
    public boolean isMissingNode() {
        return true;
    }

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

    @Override public String asText() { return ""; }

    @Override public String asText(String defaultValue) { return defaultValue; }

    // // Note: not a numeric node, hence default 'asXxx()' are fine:

    /*
    public int asInt(int defaultValue);
    public long asLong(long defaultValue);
    public double asDouble(double defaultValue);
    public boolean asBoolean(boolean defaultValue);
    */

    /*
    /**********************************************************
    /* Serialization: bit tricky as we don't really have a value
    /**********************************************************
     */

    @Override
    public final void serialize(JsonGenerator g, SerializerProvider provider)
        throws IOException
    {
        /* Nothing to output... should we signal an error tho?
         * Chances are, this is an erroneous call. For now, let's
         * not do that; serialize as explicit null. Why? Because we
         * cannot just omit a value as JSON Object field name may have
         * been written out.
         */
        g.writeNull();
    }

    @Override
    public void serializeWithType(JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException
    {
        g.writeNull();
    }

    /*
    /**********************************************************
    /* Jackson 2.10 improvements for validation
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public JsonNode require() {
        return _reportRequiredViolation("require() called on `MissingNode`");
    }

    @SuppressWarnings("unchecked")
    @Override
    public JsonNode requireNonNull() {
        return _reportRequiredViolation("requireNonNull() called on `MissingNode`");
    }

    @Override
    public int hashCode() {
        return JsonNodeType.MISSING.ordinal();
    }

    /*
    /**********************************************************
    /* Standard method overrides
    /**********************************************************
     */

    // 10-Dec-2019, tatu: Bit tricky case, see [databind#2566], but seems
    //    best NOT to produce legit JSON.
    @Override
    public String toString() {
        return "";
    }

    @Override
    public String toPrettyString() {
        return "";
    }

    @Override
    public boolean equals(Object o)
    {
        /* Hmmh. Since there's just a singleton instance, this fails in all cases but with
         * identity comparison. However: if this placeholder value was to be considered
         * similar to SQL NULL, it shouldn't even equal itself?
         * That might cause problems when dealing with collections like Sets...
         * so for now, let's let identity comparison return true.
         */
        return (o == this);
    }
}
