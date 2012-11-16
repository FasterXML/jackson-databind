package com.fasterxml.jackson.databind.ser.std;

import java.lang.reflect.Type;
import java.util.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Intermediate base class for Lists, Collections and Arrays
 * that contain static (non-dynamic) value types.
 */
public abstract class StaticListSerializerBase<T extends Collection<?>>
    extends StdSerializer<T>
{
    protected StaticListSerializerBase(Class<?> cls) {
        super(cls, false);
    }

    @Override
    public boolean isEmpty(T value) {
        return (value == null) || (value.size() == 0);
    }
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        ObjectNode o = createSchemaNode("array", true);
        o.put("items", contentSchema());
        return o;
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        acceptContentVisitor(visitor.expectArrayFormat(typeHint));
    }

    /*
    /**********************************************************
    /* Abstract methods for sub-classes to implement
    /**********************************************************
     */

    protected abstract JsonNode contentSchema();
    
    protected abstract void acceptContentVisitor(JsonArrayFormatVisitor visitor)
        throws JsonMappingException;
}
