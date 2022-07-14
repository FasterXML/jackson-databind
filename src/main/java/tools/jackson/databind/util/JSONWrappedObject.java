package tools.jackson.databind.util;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.TypeSerializer;

/**
 * General-purpose wrapper class that can be used to decorate serialized
 * value with arbitrary literal prefix and suffix. This can be used for
 * example to construct arbitrary Javascript values (similar to how basic
 * function name and parenthesis are used with JSONP).
 * 
 * @see tools.jackson.databind.util.JSONPObject
 */
public class JSONWrappedObject implements JacksonSerializable
{
    /**
     * Literal String to output before serialized value.
     * Will not be quoted when serializing value.
     */
    protected final String _prefix;

    /**
     * Literal String to output after serialized value.
     * Will not be quoted when serializing value.
     */
    protected final String _suffix;
    
    /**
     * Value to be serialized as JSONP padded; can be null.
     */
    protected final Object _value;

    /**
     * Optional static type to use for serialization; if null, runtime
     * type is used. Can be used to specify declared type which defines
     * serializer to use, as well as aspects of extra type information
     * to include (if any).
     */
    protected final JavaType _serializationType;

    public JSONWrappedObject(String prefix, String suffix, Object value) {
        this(prefix, suffix, value, (JavaType) null);
    }

    /**
     * Constructor that should be used when specific serialization type to use
     * is important, and needs to be passed instead of just using runtime
     * (type-erased) type of the value.
     */
    public JSONWrappedObject(String prefix, String suffix, Object value, JavaType asType)
    {
        _prefix = prefix;
        _suffix = suffix;
        _value = value;
        _serializationType = asType;
    }
    
    /*
    /**********************************************************************
    /* JacksonSerializable implementation
    /**********************************************************************
     */

    @Override
    public void serializeWithType(JsonGenerator g, SerializerProvider provider, TypeSerializer typeSer)
        throws JacksonException
    {
        // No type for JSONP wrapping: value serializer will handle typing for value:
        serialize(g, provider);
    }

    @Override
    public void serialize(JsonGenerator g, SerializerProvider provider)
        throws JacksonException
    {
        // First, wrapping:
    	if (_prefix != null) g.writeRaw(_prefix);
        if (_value == null) {
            provider.defaultSerializeNullValue(g);
        } else if (_serializationType != null) {
            provider.findTypedValueSerializer(_serializationType, true)
                .serialize(_value, g, provider);
        } else {
            provider.findTypedValueSerializer(_value.getClass(), true)
                .serialize(_value, g, provider);
        }
        if (_suffix != null) g.writeRaw(_suffix);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */
    
    public String getPrefix() { return _prefix; }
    public String getSuffix() { return _suffix; }
    public Object getValue() { return _value; }
    public JavaType getSerializationType() { return _serializationType; }
}
