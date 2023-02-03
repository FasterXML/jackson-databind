package com.fasterxml.jackson.databind.node;

import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Helper class used by {@link TreeTraversingParser} to keep track
 * of current location within traversed JSON tree.
 */
abstract class NodeCursor
    extends JsonStreamContext
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
    public final String getCurrentName() {
        return _currentName;
    }

    /**
     * @since 2.0
     */
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
    public abstract JsonNode currentNode();

    public abstract NodeCursor startObject();
    public abstract NodeCursor startArray();

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
     * Context for all root-level value nodes (including Arrays and Objects):
     * only context for scalar values.
     */
    protected final static class RootCursor
        extends NodeCursor
    {
        protected JsonNode _node;

        protected boolean _done = false;

        public RootCursor(JsonNode n, NodeCursor p) {
            super(JsonStreamContext.TYPE_ROOT, p);
            _node = n;
        }

        @Override
        public void overrideCurrentName(String name) {

        }

        @Override
        public JsonToken nextToken() {
            if (!_done) {
                ++_index;
                _done = true;
                return _node.asToken();
            }
            _node = null;
            return null;
        }

        @Override
        public JsonNode currentNode() {
            // May look weird, but is necessary so as not to expose current node
            // before it has been traversed
            return _done ? _node : null;
        }

        @Override
        public NodeCursor startArray() { return new ArrayCursor(_node, this); }

        @Override
        public NodeCursor startObject() { return new ObjectCursor(_node, this); }
    }

    // Cursor used for traversing JSON Array nodes
    protected final static class ArrayCursor
        extends NodeCursor
    {
        protected Iterator<JsonNode> _contents;

        protected JsonNode _currentElement;

        public ArrayCursor(JsonNode n, NodeCursor p) {
            super(JsonStreamContext.TYPE_ARRAY, p);
            _contents = n.elements();
        }

        @Override
        public JsonToken nextToken()
        {
            if (!_contents.hasNext()) {
                _currentElement = null;
                return JsonToken.END_ARRAY;
            }
            ++_index;
            _currentElement = _contents.next();
            return _currentElement.asToken();
        }

        @Override
        public JsonNode currentNode() { return _currentElement; }

        @Override
        public NodeCursor startArray() { return new ArrayCursor(_currentElement, this); }

        @Override
        public NodeCursor startObject() { return new ObjectCursor(_currentElement, this); }
    }

    // Cursor used for traversing JSON Object nodes
    protected final static class ObjectCursor
        extends NodeCursor
    {
        protected Iterator<Map.Entry<String, JsonNode>> _contents;
        protected Map.Entry<String, JsonNode> _current;

        protected boolean _needEntry;

        public ObjectCursor(JsonNode n, NodeCursor p)
        {
            super(JsonStreamContext.TYPE_OBJECT, p);
            _contents = n.fields();
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
                    return JsonToken.END_OBJECT;
                }
                ++_index;
                _needEntry = false;
                _current = _contents.next();
                _currentName = (_current == null) ? null : _current.getKey();
                return JsonToken.FIELD_NAME;
            }
            _needEntry = true;
            return _current.getValue().asToken();
        }

        @Override
        public JsonNode currentNode() {
            return (_current == null) ? null : _current.getValue();
        }

        @Override
        public NodeCursor startArray() { return new ArrayCursor(currentNode(), this); }

        @Override
        public NodeCursor startObject() { return new ObjectCursor(currentNode(), this); }
    }
}
