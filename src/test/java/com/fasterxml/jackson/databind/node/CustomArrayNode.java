package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

public class CustomArrayNode extends BaseJsonNode
{
    private final ArrayNode _delegate;

    public CustomArrayNode(ArrayNode delegate) {
        this._delegate = delegate;
    }
    
    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public int size() {
        return _delegate.size();
    }

    @Override
    public Iterator<JsonNode> elements() {
        return _delegate.elements();
    }

    @Override
    public JsonToken asToken() {
        return _delegate.asToken();
    }

    @Override
    public void serialize(JsonGenerator g, SerializerProvider ctxt) throws IOException {
        _delegate.serialize(g, ctxt);
    }

    @Override
    public void serializeWithType(JsonGenerator g, SerializerProvider ctxt, TypeSerializer typeSer) throws IOException {
        _delegate.serializeWithType(g, ctxt, typeSer);
    }

    @Override
    public <T extends JsonNode> T deepCopy() {
        return (T) _delegate.deepCopy();
    }

    @Override
    public JsonNode get(int index) {
        return _delegate.get(index);
    }

    @Override
    public JsonNode path(String fieldName) {
        return _delegate.path(fieldName);
    }

    @Override
    public JsonNode path(int index) {
        return _delegate.path(index);
    }

    @Override
    protected JsonNode _at(JsonPointer ptr) {
        return _delegate.get(ptr.getMatchingIndex());
    }

    @Override
    public JsonNodeType getNodeType() {
        return _delegate.getNodeType();
    }

    @Override
    public String asText() {
        return _delegate.asText();
    }

    @Override
    public JsonNode findValue(String fieldName) {
        return _delegate.findValue(fieldName);
    }

    @Override
    public JsonNode findParent(String fieldName) {
        return _delegate.findParent(fieldName);
    }

    @Override
    public List<JsonNode> findValues(String fieldName, List<JsonNode> foundSoFar) {
        return _delegate.findValues(fieldName, foundSoFar);
    }

    @Override
    public List<String> findValuesAsText(String fieldName, List<String> foundSoFar) {
        return _delegate.findValuesAsText(fieldName, foundSoFar);
    }

    @Override
    public List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar) {
        return _delegate.findParents(fieldName, foundSoFar);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CustomArrayNode)) {
            return false;
        }
        CustomArrayNode other = (CustomArrayNode) o;
        return this._delegate.equals(other._delegate);
    }

    @Override
    public int hashCode() {
        return _delegate.hashCode();
    }

}
