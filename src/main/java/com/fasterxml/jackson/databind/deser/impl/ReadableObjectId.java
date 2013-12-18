package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import com.fasterxml.jackson.core.JsonLocation;

/**
 * Simple value container for containing information about single Object Id
 * during deserialization
 */
public class ReadableObjectId
{
    public final Object id;

    public Object item;

    private LinkedList<Referring> _referringProperties;

    public ReadableObjectId(Object id)
    {
        this.id = id;
    }

    public void appendReferring(Referring currentReferring)
    {
        if (_referringProperties == null) {
            _referringProperties = new LinkedList<Referring>();
        }
        _referringProperties.add(currentReferring);
    }

    /**
     * Method called to assign actual POJO to which ObjectId refers to: will
     * also handle referring properties, if any, by assigning POJO.
     */
    public void bindItem(Object ob)
        throws IOException
    {
        if (item != null) {
            throw new IllegalStateException("Already had POJO for id (" + id.getClass().getName() + ") [" + id + "]");
        }
        item = ob;
        if (_referringProperties != null) {
            Iterator<Referring> it = _referringProperties.iterator();
            _referringProperties = null;
            while (it.hasNext()) {
                Referring ref = it.next();
                ref.handleResolvedForwardReference(id, ob);
            }
        }
    }

    public boolean hasReferringProperties()
    {
        return (_referringProperties != null) && !_referringProperties.isEmpty();
    }

    public Iterator<Referring> referringProperties()
    {
        if (_referringProperties == null) {
            return Collections.<Referring> emptyList().iterator();
        }
        return _referringProperties.iterator();
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    public static abstract class Referring {
        private final JsonLocation _location;

        protected Referring(JsonLocation location)
        {
            _location = location;
        }

        public JsonLocation getLocation()
        {
            return _location;
        }

        public abstract void handleResolvedForwardReference(Object id, Object value)
            throws IOException;
    }
}
