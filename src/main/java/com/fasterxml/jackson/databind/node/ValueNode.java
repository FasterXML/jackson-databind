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
    /* Public API, path handling
    /**********************************************************************
     */

    @Override
    public JsonNode path(String fieldName) { return MissingNode.getInstance(); }

    @Override
    public JsonNode path(int index) { return MissingNode.getInstance(); }

    /*
    /**********************************************************************
    /* Base impls for standard methods
    /**********************************************************************
     */

    @Override
    public String toString() { return asText(); }
}
