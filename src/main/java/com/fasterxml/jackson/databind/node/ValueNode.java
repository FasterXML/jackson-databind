package com.fasterxml.jackson.databind.node;

import java.io.IOException;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * This intermediate base class is used for all leaf nodes, that is,
 * all non-container (array or object) nodes, except for the
 * "missing node".
 */
public abstract class ValueNode
    extends BaseJsonNode
{
    protected ValueNode() { }

    /**
     * All current value nodes are immutable, so we can just return
     * them as is.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends JsonNode> T deepCopy() { return (T) this; }
    
    @Override public boolean isValueNode() { return true; }

    @Override public abstract JsonToken asToken();

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        typeSer.writeTypePrefixForScalar(this, jg);
        serialize(jg, provider);
        typeSer.writeTypeSuffixForScalar(this, jg);
    }

    /*
    /**********************************************************************
    /* Base impls for standard methods
    /**********************************************************************
     */

    @Override
    public String toString() { return asText(); }

    /*
     **********************************************************************
     * Navigation methods
     **********************************************************************
     */

    @Override
    public final JsonNode get(int index)
    {
        return null;
    }

    @Override
    public final JsonNode path(int index)
    {
        return MissingNode.getInstance();
    }

    @Override
    public final boolean has(int index)
    {
        return false;
    }

    @Override
    public final boolean hasNonNull(int index)
    {
        return false;
    }

    @Override
    public final JsonNode get(String fieldName)
    {
        return null;
    }

    @Override
    public final JsonNode path(String fieldName)
    {
        return MissingNode.getInstance();
    }

    @Override
    public final boolean has(String fieldName)
    {
        return false;
    }

    @Override
    public final boolean hasNonNull(String fieldName)
    {
        return false;
    }
}
