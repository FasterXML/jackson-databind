package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;

/**
 * Simple value container for containing information about single
 * Object Id during deserialization.
 */
public class ReadableObjectId
{
    public final Object id;
    
    public Object item;
    
    public ReadableObjectId(Object id)
    {
        this.id = id;
    }

    /**
     * Method called to assign actual POJO to which ObjectId refers to:
     * will also handle referring properties, if any, by assigning POJO.
     */
    public void bindItem(Object ob) throws IOException
    {
        if (item != null) {
            throw new IllegalStateException("Already had POJO for id ("+id.getClass().getName()+") ["+id+"]");
        }
        item = ob;
    }
}
