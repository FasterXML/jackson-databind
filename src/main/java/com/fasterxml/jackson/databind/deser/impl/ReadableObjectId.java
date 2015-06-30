package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.UnresolvedForwardReference;

/**
 * Simple value container for containing information about single Object Id
 * during deserialization
 */
public class ReadableObjectId
{
    /**
     * @deprecated Change visibility, if possible; prefer using {@link #resolve()}, which is able
     *    to handle external id resolving mechanism.
     */
    @Deprecated // at least since 2.5. Remove from 2.7
    public Object item;

    @Deprecated
    public final Object id;

    protected final ObjectIdGenerator.IdKey _key;

    protected LinkedList<Referring> _referringProperties;

    protected ObjectIdResolver _resolver;

    @Deprecated // at least since 2.5. Remove from 2.7
    public ReadableObjectId(Object id) {
        this.id = id;
        _key = null;
    }

    public ReadableObjectId(ObjectIdGenerator.IdKey key) {
        _key = key;
        id = key.key;
    }

    public void setResolver(ObjectIdResolver resolver) {
        _resolver = resolver;
    }

    public ObjectIdGenerator.IdKey getKey() {
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

    /**
     * Method called by {@link DeserializationContext} at the end of deserialization
     * if this Object Id was not resolved during normal processing. Call is made to
     * allow custom implementations to use alternative resolution strategies; currently
     * the only way to make use of this functionality is by sub-classing
     * {@link ReadableObjectId} and overriding this method.
     *<p>
     * Default implementation simply returns <code>false</code> to indicate that resolution
     * attempt did not succeed. 
     *
     * @return True, if resolution succeeded (and no error needs to be reported); false to
     *   indicate resolution did not succeed.
     *
     * @since 2.6
     */
    public boolean tryToResolveUnresolved(DeserializationContext ctxt)
    {
        return false;
    }
    
    @Override
    public String toString() {
        return String.valueOf(_key);
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    public static abstract class Referring {
        private final UnresolvedForwardReference _reference;
        private final Class<?> _beanType;

        public Referring(UnresolvedForwardReference ref, Class<?> beanType) {
            _reference = ref;
            _beanType = beanType;
        }

        public JsonLocation getLocation() { return _reference.getLocation(); }
        public Class<?> getBeanType() { return _beanType; }

        public abstract void handleResolvedForwardReference(Object id, Object value) throws IOException;
        public boolean hasId(Object id) {
            return id.equals(_reference.getUnresolvedId());
        }
    }
}
