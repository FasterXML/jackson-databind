package com.fasterxml.jackson.databind.ser.std;

import java.util.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonschema.JsonFormatVisitorAware;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonFormatVisitor;
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
    public void acceptJsonFormatVisitor(JsonFormatVisitor visitor, JavaType typeHint)
    {
    	acceptContentVisitor(visitor.arrayFormat(typeHint));
    }

    /*
    /**********************************************************
    /* Abstract methods for sub-classes to implement
    /**********************************************************
     */

    protected abstract void acceptContentVisitor(JsonArrayFormatVisitor visitor);    
}
