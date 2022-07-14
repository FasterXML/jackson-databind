package com.fasterxml.jackson.databind;

import tools.jackson.core.JacksonException;

/**
 * Abstract class that defines API used for deserializing JSON content
 * field names into Java Map keys. These deserializers are only used
 * if the Map key class is not <code>String</code> or <code>Object</code>.
 */
public abstract class KeyDeserializer
{
    /*
    /**********************************************************************
    /* Initialization, with former `ResolvableDeserializer`
    /**********************************************************************
     */

    /**
     * Method called after deserializer instance has been constructed
     * (and registered as necessary by provider objects),
     * but before it has returned it to the caller.
     * Called object can then resolve its dependencies to other types,
     * including self-references (direct or indirect).
     *
     * @param ctxt Context to use for accessing configuration, resolving
     *    secondary deserializers
     */
    public void resolve(DeserializationContext ctxt) throws JacksonException {
        // Default implementation does nothing
    }

    /*
    /**********************************************************************
    /* Main API
    /**********************************************************************
     */
    
    /**
     * Method called to deserialize a {@link java.util.Map} key from JSON property name.
     */
    public abstract Object deserializeKey(String key, DeserializationContext ctxt)
        throws JacksonException;

    /**
     * This marker class is only to be used with annotations, to
     * indicate that <b>no deserializer is configured</b>.
     *<p>
     * Specifically, this class is to be used as the marker for
     * annotation {@link com.fasterxml.jackson.databind.annotation.JsonDeserialize}.
     */
    public abstract static class None
        extends KeyDeserializer { }
}
