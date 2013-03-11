package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;

import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Simple value container used to keep track of Object Ids during
 * serialization.
 */
public final class WritableObjectId
{
    public final ObjectIdGenerator<?> generator;

    public Object id;

    protected boolean idWritten = false;
    
    public WritableObjectId(ObjectIdGenerator<?> generator) {
        this.generator = generator;
    }
    
    public boolean writeAsId(JsonGenerator jgen, SerializerProvider provider, ObjectIdWriter w)
        throws IOException, JsonGenerationException
    {
        if (id != null && (idWritten || w.alwaysAsId)) {
            w.serializer.serialize(id, jgen, provider);
            return true;
        }
        return false;
    }
    
    public Object generateId(Object forPojo) {
        return (id = generator.generateId(forPojo));
    }

    public void writeAsField(JsonGenerator jgen, SerializerProvider provider,
            ObjectIdWriter w)
        throws IOException, JsonGenerationException
    {
        SerializedString name = w.propertyName;
        idWritten = true;
        if (name != null) {
            jgen.writeFieldName(name);
            w.serializer.serialize(id, jgen, provider);
        }
    }
}
