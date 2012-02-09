package com.fasterxml.jackson.databind.ser.impl;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;

/**
 * Simple value container used to keep track of Object Ids during
 * serialization.
 */
public final class WritableObjectId
{
    public final ObjectIdGenerator<?> generator;
    
    public JsonSerializer<Object> serializer;

    public Object id;
    
    public WritableObjectId(ObjectIdGenerator<?> generator) {
        this.generator = generator;
    }
}
