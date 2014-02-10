package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.core.JsonLocation;

/**
 * Simple value container for containing information about single Object Id
 * during deserialization
 */
public class ReadableObjectId
{
    /**
     * @deprecated Prefer using {@link #resolve()}, which is able to handle
     *             external id resolving mechanism.
     */
    @Deprecated
    public Object item;
    @Deprecated
    public final Object id;

    private final IdKey _key;

    private LinkedList<Referring> _referringProperties;

    private ObjectIdResolver _resolver;

    @Deprecated
    public ReadableObjectId(Object id)
    {
        this.id = id;
        _key = null;
    }

    public ReadableObjectId(IdKey key)
    {
        _key = key;
        id = key.key;
    }

    public void setResolver(ObjectIdResolver resolver)
    {
        _resolver = resolver;
    }

    public IdKey getKey()
    {
        return _key;
    }

    public void appendReferring(Referring currentReferring) {
        if (_referringProperties == null) {
            _referringProperties = new LinkedList<Referring>();
        }
        _referringProperties.add(currentReferring);
    }

    /**
     * Method called to assign actual POJO to which ObjectId refers to: will
     * also handle referring properties, if any, by assigning POJO.
     */
    public void bindItem(Object ob) throws IOException
    {
        _resolver.bindItem(_key, ob);
        item = ob;
        if (_referringProperties != null) {
            Iterator<Referring> it = _referringProperties.iterator();
            _referringProperties = null;
            while (it.hasNext()) {
                it.next().handleResolvedForwardReference(id, ob);
            }
        }
    }

    public Object resolve(){
         return (item = _resolver.resolveId(_key));
    }

    public boolean hasReferringProperties() {
        return (_referringProperties != null) && !_referringProperties.isEmpty();
    }

    public Iterator<Referring> referringProperties() {
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
        private final Class<?> _beanType;

        public Referring(JsonLocation location, Class<?> beanType)
        {
            _location = location;
            _beanType = beanType;
        }

        public JsonLocation getLocation() { return _location; }
        public Class<?> getBeanType() { return _beanType; }

        public abstract void handleResolvedForwardReference(Object id, Object value)
            throws IOException;
    }
}
