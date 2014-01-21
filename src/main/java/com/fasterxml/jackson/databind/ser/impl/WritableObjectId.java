package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;

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
            // 03-Aug-2013, tatu: Prefer Native Object Ids if available
            if (jgen.canWriteObjectId()) {
                jgen.writeObjectRef(String.valueOf(id));
            } else {
                w.serializer.serialize(id, jgen, provider);
            }
            return true;
        }
        return false;
    }
    
    public Object generateId(Object forPojo) {
        return (id = generator.generateId(forPojo));
    }

    /**
     * Method called to output Object Id as specified.
     */
    public void writeAsField(JsonGenerator jgen, SerializerProvider provider,
            ObjectIdWriter w)
        throws IOException, JsonGenerationException
    {
        idWritten = true;

        // 03-Aug-2013, tatu: Prefer Native Object Ids if available
        if (jgen.canWriteObjectId()) {
            // Need to assume String(ified) ids, for now... could add 'long' variant?
            jgen.writeObjectId(String.valueOf(id));
            return;
        }
        
        SerializableString name = w.propertyName;
        if (name != null) {
            jgen.writeFieldName(name);
            w.serializer.serialize(id, jgen, provider);
        }
    }
}
