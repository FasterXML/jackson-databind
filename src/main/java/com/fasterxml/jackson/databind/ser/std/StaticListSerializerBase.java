package com.fasterxml.jackson.databind.ser.std;

import java.lang.reflect.Type;
import java.util.*;

import com.fasterxml.jackson.core.JsonNode;


import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Intermediate base class for Lists, Collections and Arrays
 * that contain static (non-dynamic) value types.
 * 
 * @since 1.7
 */
public abstract class StaticListSerializerBase<T extends Collection<?>>
    extends SerializerBase<T>
{
    /**
     * Property that contains String List to serialize, if known.
     */
    protected final BeanProperty _property;

    protected StaticListSerializerBase(Class<?> cls, BeanProperty property)
    {
        super(cls, false);
        _property = property;
    }
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        ObjectNode o = createSchemaNode("array", true);
        o.put("items", contentSchema());
        return o;
    }

    /*
    /**********************************************************
    /* Abstract methods for sub-classes to implement
    /**********************************************************
     */

    protected abstract JsonNode contentSchema();    
}
