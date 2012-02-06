package com.fasterxml.jackson.databind.util;

import java.util.IdentityHashMap;

import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

/**
 * Map used during serialization, to keep track of referable Objects
 * along with lazily evaluated ids.
 */
public class ObjectIdMap
{
    public final IdentityHashMap<Object,Entry> _seenObjects;
    
    public ObjectIdMap()
    {
        _seenObjects = new IdentityHashMap<Object,Entry>(16);
    }

    /*
    /**********************************************************
    /* API
    /**********************************************************
     */

    /**
     * Method that is called to figure out whether we have already
     * seen given POJO: if yes, we will return its id (first looking
     * it up as necessary); if not, we will mark down that we have
     * seen it but return null.
     */
    public Object findOrInsertId(Object pojo, AnnotatedMember idAccessor)
    {
        Entry entry = _seenObjects.get(pojo);
        if (entry == null) { // no, first time: insert, return null
            _seenObjects.put(pojo, new Entry());
            return null;
        }
        Object id = entry.id;
        if (id == null) {
            id = idAccessor.getValue(pojo);
            entry.id = id;
        }
        return id;
    }
    
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    protected final static class Entry
    {
        /**
         * Lazily evaluated object id to use to represent object
         */
        public Object id;

        public Entry() { }
    }
}
