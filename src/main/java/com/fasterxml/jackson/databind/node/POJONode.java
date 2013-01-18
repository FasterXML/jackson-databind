package com.fasterxml.jackson.databind.node;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.SerializerProvider;


/**
 * Value node that contains a wrapped POJO, to be serialized as
 * a JSON constructed through data mapping (usually done by
 * calling {@link com.fasterxml.jackson.databind.ObjectMapper}).
 */
public final class POJONode
    extends ValueNode
{
    protected final Object _value;

    public POJONode(Object v) { _value = v; }

    /*
    /**********************************************************
    /* Base class overrides
    /**********************************************************
     */

    @Override
    public JsonNodeType getNodeType()
    {
        return JsonNodeType.POJO;
    }

    @Override public JsonToken asToken() { return JsonToken.VALUE_EMBEDDED_OBJECT; }

    /**
     * As it is possible that some implementations embed byte[] as POJONode
     * (despite optimal being {@link BinaryNode}), let's add support for exposing
     * binary data here too.
     */
    @Override
    public byte[] binaryValue() throws IOException
    {
        if (_value instanceof byte[]) {
            return (byte[]) _value;
        }
        return super.binaryValue();
    }
    
    /* 
    /**********************************************************
    /* General type coercions
    /**********************************************************
     */

    @Override
    public String asText() {
        return (_value == null) ? "null" : _value.toString();
    }

    @Override
    public boolean asBoolean(boolean defaultValue)
    {
        if (_value != null && _value instanceof Boolean) {
            return ((Boolean) _value).booleanValue();
        }
        return defaultValue;
    }
    
    @Override
    public int asInt(int defaultValue)
    {
        if (_value instanceof Number) {
            return ((Number) _value).intValue();
        }
        return defaultValue;
    }

    @Override
    public long asLong(long defaultValue)
    {
        if (_value instanceof Number) {
            return ((Number) _value).longValue();
        }
        return defaultValue;
    }
    
    @Override
    public double asDouble(double defaultValue)
    {
        if (_value instanceof Number) {
            return ((Number) _value).doubleValue();
        }
        return defaultValue;
    }
    
    /*
    /**********************************************************
    /* Public API, serialization
    /**********************************************************
     */

    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        if (_value == null) {
            jg.writeNull();
        } else {
            jg.writeObject(_value);
        }
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    /**
     * Method that can be used to access the POJO this node wraps.
     */
    public Object getPojo() { return _value; }

    /*
    /**********************************************************
    /* Overridden standard methods
    /**********************************************************
     */

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) { // final class, can do this
            return false;
        }
        POJONode other = (POJONode) o;
        if (_value == null) {
            return other._value == null;
        }
        return _value.equals(other._value);
    }

    @Override
    public int hashCode() { return _value.hashCode(); }

    @Override
    public String toString()
    {
        return String.valueOf(_value);
    }
}
