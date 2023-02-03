package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;

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

    /**
     * Marker to denote whether Object Id value has been written as part of an Object,
     * to be referencible. Remains false when forward-reference is written.
     */
    protected boolean idWritten = false;

    public WritableObjectId(ObjectIdGenerator<?> generator) {
        this.generator = generator;
    }

    public boolean writeAsId(JsonGenerator gen, SerializerProvider provider, ObjectIdWriter w) throws IOException
    {
        if ((id != null) && (idWritten || w.alwaysAsId)) {
            // 03-Aug-2013, tatu: Prefer Native Object Ids if available
            if (gen.canWriteObjectId()) {
                gen.writeObjectRef(String.valueOf(id));
            } else {
                w.serializer.serialize(id, gen, provider);
            }
            return true;
        }
        return false;
    }

    public Object generateId(Object forPojo) {
        // 04-Jun-2016, tatu: As per [databind#1255], need to consider possibility of
        //    id being generated for "alwaysAsId", but not being written as POJO; regardless,
        //    need to use existing id if there is one:
        if (id == null) {
            id = generator.generateId(forPojo);
        }
        return id;
    }

    /**
     * Method called to output Object Id as specified.
     */
    public void writeAsField(JsonGenerator gen, SerializerProvider provider,
            ObjectIdWriter w) throws IOException
    {
        idWritten = true;

        // 03-Aug-2013, tatu: Prefer Native Object Ids if available
        if (gen.canWriteObjectId()) {
            // Need to assume String(ified) ids, for now... could add 'long' variant?
            // 05-Feb-2019, tatu: But in special case of `null` we should not coerce -- whether
            //   we should even call is an open question, but for now do pass to let generator
            //   decide what to do, if anything.
            String idStr = (id == null) ? null : String.valueOf(id);
            gen.writeObjectId(idStr);
            return;
        }

        SerializableString name = w.propertyName;
        if (name != null) {
            // 05-Feb-2019, tatu: How about `null` id? For now, write
            gen.writeFieldName(name);
            w.serializer.serialize(id, gen, provider);
        }
    }
}
