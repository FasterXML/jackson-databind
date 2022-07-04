package com.fasterxml.jackson.databind.exc;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;

public class JsonNodeException
    extends DatabindException
{
    private static final long serialVersionUID = 3L;

    protected final JsonNode _node;
    
    protected JsonNodeException(JsonNode node, String message) {
        super(message);
        _node = node;
    }

    public static JsonNodeException from(JsonNode node, String message) {
        return new JsonNodeException(node, message);
    }

    public JsonNode getNode() {
        return _node;
    }
}
