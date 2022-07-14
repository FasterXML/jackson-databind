package tools.jackson.databind.ser;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.SerializableString;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.ser.impl.ObjectIdWriter;

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

    public WritableObjectId(ObjectIdGenerator<?> g) {
        this.generator = g;
    }

    /**
     * Method to call to write a reference to object that this id refers. Usually this
     * is done after an earlier call to {@link #writeAsDeclaration}.
     */
    public boolean writeAsReference(JsonGenerator g, SerializerProvider ctxt, ObjectIdWriter w)
        throws JacksonException
    {
        if ((id != null) && (idWritten || w.alwaysAsId)) {
            // 03-Aug-2013, tatu: Prefer Native Object Ids if available
            if (g.canWriteObjectId()) {
                g.writeObjectRef(String.valueOf(id));
            } else {
                w.serializer.serialize(id, g, ctxt);
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
     * Method called to output Object Id declaration, either using native Object Id write
     * method {@link JsonGenerator#writeObjectId(Object)} (if allowed), or, if not,
     * just writing it as an Object property with specified property name and id value.
     */
    public void writeAsDeclaration(JsonGenerator g, SerializerProvider ctxt, ObjectIdWriter w)
        throws JacksonException
    {
        idWritten = true;

        // 03-Aug-2013, tatu: Prefer Native Object Ids if available
        if (g.canWriteObjectId()) {
            // Need to assume String(ified) ids, for now... could add 'long' variant?
            // 05-Feb-2019, tatu: But in special case of `null` we should not coerce -- whether
            //   we should even call is an open question, but for now do pass to let generator
            //   decide what to do, if anything.
            String idStr = (id == null) ? null : String.valueOf(id);
            g.writeObjectId(idStr);
            return;
        }

        SerializableString name = w.propertyName;
        if (name != null) {
            // 05-Feb-2019, tatu: How about `null` id? For now, write
            g.writeName(name);
            w.serializer.serialize(id, g, ctxt);
        }
    }
}
