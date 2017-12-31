package com.fasterxml.jackson.databind.node;

import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Helper class used by {@link TreeTraversingParser} to keep track
 * of current location within traversed JSON tree.
 */
abstract class NodeCursor
    extends TokenStreamContext
{
    /**
     * Parent cursor of this cursor, if any; null for root
     * cursors.
     */
    protected final NodeCursor _parent;

    /**
     * Current field name
     */
    protected String _currentName;

    /**
     * @since 2.5
     */
    protected java.lang.Object _currentValue;
    
    public NodeCursor(int contextType, NodeCursor p)
    {
        super();
        _type = contextType;
        _index = -1;
        _parent = p;
    }

    /*
    /**********************************************************
    /* JsonStreamContext impl
    /**********************************************************
     */

    // note: co-variant return type
    @Override
    public final NodeCursor getParent() { return _parent; }

    @Override
    public final String currentName() {
        return _currentName;
    }

    public void overrideCurrentName(String name) {
        _currentName = name;
    }

    @Override
    public java.lang.Object getCurrentValue() {
        return _currentValue;
    }

    @Override
    public void setCurrentValue(java.lang.Object v) {
        _currentValue = v;
    }
    
    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    public abstract JsonToken nextToken();
    public abstract JsonToken nextValue();
    public abstract JsonToken endToken();

    public abstract JsonNode currentNode();
    public abstract boolean currentHasChildren();
    
    /**
     * Method called to create a new context for iterating all
     * contents of the current structured value (JSON array or object)
     */
    public final NodeCursor iterateChildren() {
        JsonNode n = currentNode();
        if (n == null) throw new IllegalStateException("No current node");
        if (n.isArray()) { // false since we have already returned START_ARRAY
            return new ArrayCursor(n, this);
        }
        if (n.isObject()) {
            return new ObjectCursor(n, this);
        }
        throw new IllegalStateException("Current node of type "+n.getClass().getName());
    }

    /*
    /**********************************************************
    /* Concrete implementations
    /**********************************************************
     */

    /**
     * Context matching root-level value nodes (i.e. anything other
     * than JSON Object and Array).
     * Note that context is NOT created for leaf values.
     */
    protected final static class RootCursor
        extends NodeCursor
    {
        protected JsonNode _node;

        protected boolean _done = false;

        public RootCursor(JsonNode n, NodeCursor p) {
            super(TokenStreamContext.TYPE_ROOT, p);
            _node = n;
        }

        @Override
        public void overrideCurrentName(String name) {
            
        }
        
        @Override
        public JsonToken nextToken() {
            if (!_done) {
                _done = true;
                return _node.asToken();
            }
            _node = null;
            return null;
        }
        
        @Override
        public JsonToken nextValue() { return nextToken(); }
        @Override
        public JsonToken endToken() { return null; }
        @Override
        public JsonNode currentNode() { return _node; }
        @Override
        public boolean currentHasChildren() { return false; }
    }

    /**
     * Cursor used for traversing non-empty JSON Array nodes
     */
    protected final static class ArrayCursor
        extends NodeCursor
    {
        protected Iterator<JsonNode> _contents;

        protected JsonNode _currentNode;

        public ArrayCursor(JsonNode n, NodeCursor p) {
            super(TokenStreamContext.TYPE_ARRAY, p);
            _contents = n.elements();
        }

        @Override
        public JsonToken nextToken()
        {
            if (!_contents.hasNext()) {
                _currentNode = null;
                return null;
            }
            _currentNode = _contents.next();
            return _currentNode.asToken();
        }

        @Override
        public JsonToken nextValue() { return nextToken(); }
        @Override
        public JsonToken endToken() { return JsonToken.END_ARRAY; }

        @Override
        public JsonNode currentNode() { return _currentNode; }
        @Override
        public boolean currentHasChildren() {
            // note: ONLY to be called for container nodes
            return ((ContainerNode<?>) currentNode()).size() > 0;
        }
    }

    /**
     * Cursor used for traversing non-empty JSON Object nodes
     */
    protected final static class ObjectCursor
        extends NodeCursor
    {
        protected Iterator<Map.Entry<String, JsonNode>> _contents;
        protected Map.Entry<String, JsonNode> _current;

        protected boolean _needEntry;
        
        public ObjectCursor(JsonNode n, NodeCursor p)
        {
            super(TokenStreamContext.TYPE_OBJECT, p);
            _contents = ((ObjectNode) n).fields();
            _needEntry = true;
        }

        @Override
        public JsonToken nextToken()
        {
            // Need a new entry?
            if (_needEntry) {
                if (!_contents.hasNext()) {
                    _currentName = null;
                    _current = null;
                    return null;
                }
                _needEntry = false;
                _current = _contents.next();
                _currentName = (_current == null) ? null : _current.getKey();
                return JsonToken.FIELD_NAME;
            }
            _needEntry = true;
            return _current.getValue().asToken();
        }

        @Override
        public JsonToken nextValue()
        {
            JsonToken t = nextToken();
            if (t == JsonToken.FIELD_NAME) {
                t = nextToken();
            }
            return t;
        }

        @Override
        public JsonToken endToken() { return JsonToken.END_OBJECT; }

        @Override
        public JsonNode currentNode() {
            return (_current == null) ? null : _current.getValue();
        }
        @Override
        public boolean currentHasChildren() {
            // note: ONLY to be called for container nodes
            return ((ContainerNode<?>) currentNode()).size() > 0;
        }
    }
}
