package com.fasterxml.jackson.databind.util;

import java.util.IdentityHashMap;

/**
 * Map used during serialization, to keep track of referable Objects
 * along with lazily evaluated ids.
 */
@SuppressWarnings("serial")
public class ObjectIdMap extends IdentityHashMap<Object,Object>
{
    public ObjectIdMap()
    {
        super(16);
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
    public Object findId(Object pojo)
    {
        return get(pojo);
    }
    
    public void insertId(Object pojo, Object id)
    {
        put(pojo, id);
    }
}
