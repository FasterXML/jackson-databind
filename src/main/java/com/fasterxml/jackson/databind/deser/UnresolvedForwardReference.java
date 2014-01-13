package com.fasterxml.jackson.databind.deser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId;

/**
 * Exception thrown during deserialization when there are object id that can't
 * be resolved.
 * 
 * @author pgelinas
 */
public final class UnresolvedForwardReference extends JsonMappingException {

    private static final long serialVersionUID = -5097969645059502061L;
    private ReadableObjectId _roid;
    private List<UnresolvedId> _unresolvedIds;

    public UnresolvedForwardReference(String msg, JsonLocation loc, ReadableObjectId roid)
    {
        super(msg, loc);
        _roid = roid;
    }

    public UnresolvedForwardReference(String msg)
    {
        super(msg);
        _unresolvedIds = new ArrayList<UnresolvedId>();
    }

    // ******************************
    // ****** Accessor methods ******
    // ******************************

    public ReadableObjectId getRoid()
    {
        return _roid;
    }

    public Object getUnresolvedId()
    {
        return _roid.id;
    }

    /**
     * Helper class
     * 
     * @author pgelinas
     */
    public static class UnresolvedId {
        private Object _id;
        private JsonLocation _location;
        private Class<?> _type;

        public UnresolvedId(Object id, Class<?> type, JsonLocation where)
        {
            _id = id;
            _type = type;
            _location = where;
        }
        
        /**
         * The id which is unresolved.
         */
        public Object getId() { return _id; }
        /**
         * The type of object which was expected.
         */
        public Class<?> getType() { return _type; }
        public JsonLocation getLocation() { return _location; }

        @Override
        public String toString()
        {
            return String.format("Object id [%s] (for %s) at %s", _id, _type, _location);
        }
    }

    public void addUnresolvedId(Object id, Class<?> type, JsonLocation where)
    {
        _unresolvedIds.add(new UnresolvedId(id, type, where));
    }

    public List<UnresolvedId> getUnresolvedIds(){
        return _unresolvedIds;
    }
    
    @Override
    public String getMessage()
    {
        String msg = super.getMessage();
        if (_unresolvedIds == null) {
            return msg;
        }

        StringBuilder sb = new StringBuilder(msg);
        Iterator<UnresolvedId> iterator = _unresolvedIds.iterator();
        while (iterator.hasNext()) {
            UnresolvedId unresolvedId = iterator.next();
            sb.append(unresolvedId.toString());
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append('.');
        return sb.toString();
    }
}
