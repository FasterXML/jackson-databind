package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.core.StreamReadCapability;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;

/**
 * @since 2.14
 */
@JacksonStdImpl
final class UntypedObjectDeserializerNR
    extends StdDeserializer<Object>
{
    private static final long serialVersionUID = 1L;

    protected final static Object[] NO_OBJECTS = new Object[0];

    public final static UntypedObjectDeserializerNR std = new UntypedObjectDeserializerNR();

    // @since 2.9
    protected final boolean _nonMerging;

    public UntypedObjectDeserializerNR() { this(false); }

    protected UntypedObjectDeserializerNR(boolean nonMerging) {
        super(Object.class);
        _nonMerging = nonMerging;
    }

    public static UntypedObjectDeserializerNR instance(boolean nonMerging) {
        if (nonMerging) {
            return new UntypedObjectDeserializerNR(true);
        }
        return std;
    }

    @Override
    public LogicalType logicalType() {
        return LogicalType.Untyped;
    }

    @Override // since 2.9
    public Boolean supportsUpdate(DeserializationConfig config) {
        // 21-Apr-2017, tatu: Bit tricky... some values, yes. So let's say "dunno"
        // 14-Jun-2017, tatu: Well, if merging blocked, can say no, as well.
        return _nonMerging ? Boolean.FALSE : null;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_START_OBJECT:
            return _deserializeNR(p, ctxt,
                    Scope.rootObjectScope(ctxt.isEnabled(StreamReadCapability.DUPLICATE_PROPERTIES)));
        case JsonTokenId.ID_END_OBJECT:
            // 28-Oct-2015, tatu: [databind#989] We may also be given END_OBJECT (similar to FIELD_NAME),
            //    if caller has advanced to the first token of Object, but for empty Object
            return Scope.emptyMap();
        case JsonTokenId.ID_FIELD_NAME:
            return _deserializeObjectAtName(p, ctxt);
        case JsonTokenId.ID_START_ARRAY:
            return _deserializeNR(p, ctxt, Scope.rootArrayScope());

        case JsonTokenId.ID_STRING:
            return p.getText();
        case JsonTokenId.ID_NUMBER_INT:
            if (ctxt.hasSomeOfFeatures(F_MASK_INT_COERCIONS)) {
                return _coerceIntegral(p, ctxt);
            }
            return p.getNumberValue();
        case JsonTokenId.ID_NUMBER_FLOAT:
            if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                return p.getDecimalValue();
            }
            return p.getNumberValue();
        case JsonTokenId.ID_TRUE:
            return Boolean.TRUE;
        case JsonTokenId.ID_FALSE:
            return Boolean.FALSE;
        case JsonTokenId.ID_NULL:
            return null;
        case JsonTokenId.ID_EMBEDDED_OBJECT:
            return p.getEmbeddedObject();
        default:
        }
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException
    {
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_START_ARRAY:
        case JsonTokenId.ID_START_OBJECT:
        case JsonTokenId.ID_FIELD_NAME:
            return typeDeserializer.deserializeTypedFromAny(p, ctxt);
        default:
            return _deserializeAnyScalar(p, ctxt, p.currentTokenId());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object intoValue)
        throws IOException
    {
        if (_nonMerging) {
            return deserialize(p, ctxt);
        }
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_END_OBJECT:
        case JsonTokenId.ID_END_ARRAY:
            return intoValue;
        case JsonTokenId.ID_START_OBJECT:
            {
                JsonToken t = p.nextToken(); // to get to FIELD_NAME or END_OBJECT
                if (t == JsonToken.END_OBJECT) {
                    return intoValue;
                }
            }
            // fall through
        case JsonTokenId.ID_FIELD_NAME:
            if (intoValue instanceof Map<?,?>) {
                Map<Object,Object> m = (Map<Object,Object>) intoValue;
                // NOTE: we are guaranteed to point to FIELD_NAME
                String key = p.currentName();
                do {
                    p.nextToken();
                    // and possibly recursive merge here
                    Object old = m.get(key);
                    Object newV;
                    if (old != null) {
                        newV = deserialize(p, ctxt, old);
                    } else {
                        newV = deserialize(p, ctxt);
                    }
                    if (newV != old) {
                        m.put(key, newV);
                    }
                } while ((key = p.nextFieldName()) != null);
                return intoValue;
            }
            break;
        case JsonTokenId.ID_START_ARRAY:
            {
                JsonToken t = p.nextToken(); // to get to FIELD_NAME or END_OBJECT
                if (t == JsonToken.END_ARRAY) {
                    return intoValue;
                }
            }

            if (intoValue instanceof Collection<?>) {
                Collection<Object> c = (Collection<Object>) intoValue;
                // NOTE: merge for arrays/Collections means append, can't merge contents
                do {
                    c.add(deserialize(p, ctxt));
                } while (p.nextToken() != JsonToken.END_ARRAY);
                return intoValue;
            }
            // 21-Apr-2017, tatu: Should we try to support merging of Object[] values too?
            //    ... maybe future improvement
            break;
        }
        // Easiest handling for the rest, delegate. Only (?) question: how about nulls?
        return deserialize(p, ctxt);
    }

    private Object _deserializeObjectAtName(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        final Scope rootObject = Scope.rootObjectScope(ctxt.isEnabled(StreamReadCapability.DUPLICATE_PROPERTIES));
        String key = p.currentName();
        for (; key != null; key = p.nextFieldName()) {
            Object value;
            JsonToken t = p.nextToken();
            if (t == null) { // can this ever occur?
                t = JsonToken.NOT_AVAILABLE;
            }
            switch (t.id()) {
            case JsonTokenId.ID_START_OBJECT:
                value = _deserializeNR(p, ctxt, rootObject.childObject());
                break;
            case JsonTokenId.ID_END_OBJECT:
                return rootObject.finishRootObject();
            case JsonTokenId.ID_START_ARRAY:
                value = _deserializeNR(p, ctxt, rootObject.childArray());
                break;
            default:
                value = _deserializeAnyScalar(p, ctxt, t.id());
            }
            rootObject.putValue(key, value);
        }
        return rootObject.finishRootObject();
    }

    private Object _deserializeNR(JsonParser p, DeserializationContext ctxt,
            Scope rootScope)
        throws IOException
    {
        final boolean intCoercions = ctxt.hasSomeOfFeatures(F_MASK_INT_COERCIONS);
        final boolean useJavaArray = ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY);

        Scope currScope = rootScope;

        outer_loop:
        while (true) {
            if (currScope.isObject()) {
                String propName = p.nextFieldName();

                objectLoop:
                for (; propName != null; propName = p.nextFieldName()) {
                    Object value;
                    JsonToken t = p.nextToken();
                    if (t == null) { // unexpected end-of-input (or bad buffering?)
                        t = JsonToken.NOT_AVAILABLE; // to trigger an exception
                    }
                    switch (t.id()) {
                    case JsonTokenId.ID_START_OBJECT:
                        currScope = currScope.childObject(propName);
                        // We can actually take a short-cut with nested Objects...
                        continue objectLoop;
                    case JsonTokenId.ID_START_ARRAY:
                        currScope = currScope.childArray(propName);
                        // but for arrays need to go to main loop
                        continue outer_loop;
                    case JsonTokenId.ID_STRING:
                        value = p.getText();
                        break;
                    case JsonTokenId.ID_NUMBER_INT:
                        value = intCoercions ?  _coerceIntegral(p, ctxt) : p.getNumberValue();
                        break;
                    case JsonTokenId.ID_NUMBER_FLOAT:
                        value = ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                            ? p.getDecimalValue() : p.getNumberValue();
                        break;
                    case JsonTokenId.ID_TRUE:
                        value = Boolean.TRUE;
                        break;
                    case JsonTokenId.ID_FALSE:
                        value = Boolean.FALSE;
                        break;
                    case JsonTokenId.ID_NULL:
                        value = null;
                        break;
                    case JsonTokenId.ID_EMBEDDED_OBJECT:
                        value = p.getEmbeddedObject();
                        break;
                    default:
                        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
                    }
                    currScope.putValue(propName, value);
                }
                // reached not-property-name, should be END_OBJECT (verify?)
                if (currScope == rootScope) {
                    return currScope.finishRootObject();
                }
                currScope = currScope.finishBranchObject();
            } else {
                // Otherwise we must have an Array
                arrayLoop:
                while (true) {
                    JsonToken t = p.nextToken();
                    if (t == null) { // unexpected end-of-input (or bad buffering?)
                        t = JsonToken.NOT_AVAILABLE; // to trigger an exception
                    }
                    Object value;
                    switch (t.id()) {
                    case JsonTokenId.ID_START_OBJECT:
                        currScope = currScope.childObject();
                        continue outer_loop;
                    case JsonTokenId.ID_START_ARRAY:
                        currScope = currScope.childArray();
                        continue outer_loop;
                    case JsonTokenId.ID_END_ARRAY:
                        if (currScope == rootScope) {
                            return currScope.finishRootArray(useJavaArray);
                        }
                        currScope = currScope.finishBranchArray(useJavaArray);
                        break arrayLoop;
                    case JsonTokenId.ID_STRING:
                        value = p.getText();
                        break;
                    case JsonTokenId.ID_NUMBER_INT:
                        value = intCoercions ?  _coerceIntegral(p, ctxt) : p.getNumberValue();
                        break;
                    case JsonTokenId.ID_NUMBER_FLOAT:
                        value = ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                            ? p.getDecimalValue() : p.getNumberValue();
                        break;
                    case JsonTokenId.ID_TRUE:
                        value = Boolean.TRUE;
                        break;
                    case JsonTokenId.ID_FALSE:
                        value = Boolean.FALSE;
                        break;
                    case JsonTokenId.ID_NULL:
                        value = null;
                        break;
                    case JsonTokenId.ID_EMBEDDED_OBJECT:
                        value = p.getEmbeddedObject();
                        break;
                    default:
                        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
                    }
                    currScope.addValue(value);
                }
            }
        }
    }

    private Object _deserializeAnyScalar(JsonParser p, DeserializationContext ctxt,
            int tokenType)
        throws IOException
    {
        switch (tokenType) {
        case JsonTokenId.ID_STRING:
            return p.getText();
        case JsonTokenId.ID_NUMBER_INT:
            if (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
                return p.getBigIntegerValue();
            }
            return p.getNumberValue();

        case JsonTokenId.ID_NUMBER_FLOAT:
            if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                return p.getDecimalValue();
            }
            return p.getNumberValue();
        case JsonTokenId.ID_TRUE:
            return Boolean.TRUE;
        case JsonTokenId.ID_FALSE:
            return Boolean.FALSE;
        case JsonTokenId.ID_EMBEDDED_OBJECT:
            return p.getEmbeddedObject();

        case JsonTokenId.ID_NULL: // should not get this far really but...
            return null;
        // Caller should check for anything else
        default:
        }
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    // NOTE: copied from above (alas, no easy way to share/reuse)
    // @since 2.12 (wrt [databind#2733]
    protected Object _mapObjectWithDups(JsonParser p, DeserializationContext ctxt,
            final Map<String, Object> result, String initialKey,
            Object oldValue, Object newValue, String nextKey) throws IOException
    {
        final boolean squashDups = ctxt.isEnabled(StreamReadCapability.DUPLICATE_PROPERTIES);

        if (squashDups) {
            _squashDups(result, initialKey, oldValue, newValue);
        }

        while (nextKey != null) {
            p.nextToken();
            newValue = deserialize(p, ctxt);
            oldValue = result.put(nextKey, newValue);
            if ((oldValue != null) && squashDups) {
                _squashDups(result, nextKey, oldValue, newValue);
            }
            nextKey = p.nextFieldName();
        }

        return result;
    }

    // NOTE: copied from above (alas, no easy way to share/reuse)
    @SuppressWarnings("unchecked")
    private void _squashDups(final Map<String, Object> result, String key,
            Object oldValue, Object newValue)
    {
        if (oldValue instanceof List<?>) {
            ((List<Object>) oldValue).add(newValue);
            result.put(key, oldValue);
        } else {
            ArrayList<Object> l = new ArrayList<>();
            l.add(oldValue);
            l.add(newValue);
            result.put(key, l);
        }
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Helper class used for building Maps and Lists/Arrays.
     */
    private final static class Scope
    {
        private final Scope _parent;
        private Scope _child;

        private boolean _isObject, _squashDups;
        private String  _deferredKey;

        private Map<String, Object> _map;
        private List<Object> _list;

        /*
        /******************************************************************
        /* Life cycle
        /******************************************************************
         */

        // For Arrays:
        private Scope(Scope p) {
            _parent = p;
            _isObject = false;
            _squashDups = false;
        }

        // For Objects:
        private Scope(Scope p, boolean isObject, boolean squashDups) {
            _parent = p;
            _isObject = isObject;
            _squashDups = squashDups;
        }

        public static Scope rootObjectScope(boolean squashDups) {
            return new Scope(null, true, squashDups);
        }

        public static Scope rootArrayScope() {
            return new Scope(null);
        }

        private Scope resetAsArray() {
            _isObject = false;
            return this;
        }

        private Scope resetAsObject(boolean squashDups) {
            _isObject = true;
            _squashDups = squashDups;
            return this;
        }

        public Scope childObject() {
            if (_child == null) {
                return new Scope(this, true, _squashDups);
            }
            return _child.resetAsObject(_squashDups);
        }

        public Scope childObject(String deferredKey) {
            _deferredKey = deferredKey;
            if (_child == null) {
                return new Scope(this, true, _squashDups);
            }
            return _child.resetAsObject(_squashDups);
        }

        public Scope childArray() {
            if (_child == null) {
                return new Scope(this);
            }
            return _child.resetAsArray();
        }

        public Scope childArray(String deferredKey) {
            _deferredKey = deferredKey;
            if (_child == null) {
                return new Scope(this);
            }
            return _child.resetAsArray();
        }

        /*
        /******************************************************************
        /* Accessors
        /******************************************************************
         */

        public boolean isObject() {
            return _isObject;
        }

        /*
        /******************************************************************
        /* Value construction
        /******************************************************************
         */

        public void putValue(String key, Object value) {
            if (_squashDups) {
                _putValueHandleDups(key, value);
                return;
            }
            if (_map == null) {
                _map = new LinkedHashMap<>();
            }
            _map.put(key, value);
        }

        public Scope putDeferredValue(Object value) {
            String key = Objects.requireNonNull(_deferredKey);
            _deferredKey = null;
            if (_squashDups) {
                _putValueHandleDups(key, value);
                return this;
            }
            if (_map == null) {
                _map = new LinkedHashMap<>();
            }
            _map.put(key, value);
            return this;
        }

        public void addValue(Object value) {
            if (_list == null) {
                _list = new ArrayList<>();
            }
            _list.add(value);
        }

        public Object finishRootObject() {
            if (_map == null)  {
                return emptyMap();
            }
            return _map;
        }

        public Scope finishBranchObject() {
            Object value;
            if (_map == null) {
                value = new LinkedHashMap<>();
            } else {
                value = _map;
                _map = null;
            }
            if (_parent.isObject()) {
                return _parent.putDeferredValue(value);
            }
            _parent.addValue(value);
            return _parent;
        }

        public Object finishRootArray(boolean asJavaArray) {
            if (_list == null) {
                if (asJavaArray) {
                    return NO_OBJECTS;
                }
                return emptyList();
            }
            if (asJavaArray) {
                return _list.toArray(NO_OBJECTS);
            }
            return _list;
        }

        public Scope finishBranchArray(boolean asJavaArray) {
            Object value;
            if (_list == null) {
                if (asJavaArray) {
                    value = NO_OBJECTS;
                } else {
                    value = emptyList();
                }
            } else {
                if (asJavaArray) {
                    value = _list.toArray(NO_OBJECTS);
                } else {
                    value = _list;
                }
                _list = null;
            }
            if (_parent.isObject()) {
                return _parent.putDeferredValue(value);
            }
            _parent.addValue(value);
            return _parent;
        }

        /* Helper method that deals with merging of dups, when that is expected.
         * Only used with formats that expose seeming "duplicates" in Object
         * values: most notable this is the case for XML.
         */
        @SuppressWarnings("unchecked")
        private void _putValueHandleDups(String key, Object newValue) {
            if (_map == null) {
                _map = new LinkedHashMap<>();
                _map.put(key, newValue);
                return;
            }
            Object old = _map.put(key, newValue);
            if (old != null) {
                // If value was already a List, append
                if (old instanceof List<?>) {
                    ((List<Object>) old).add(newValue);
                    _map.put(key, old);
                } else { // but if not (Object or, possible, Java array), make it such
                    ArrayList<Object> l = new ArrayList<>();
                    l.add(old);
                    l.add(newValue);
                    _map.put(key, l);
                }

            }
        }

        /*
        /******************************************************************
        /* Helper methods
        /******************************************************************
         */

        public static Map<String, Object> emptyMap() {
            return new LinkedHashMap<>(2);
        }

        public static List<Object> emptyList() {
            return new ArrayList<>(2);
        }
    }
}
