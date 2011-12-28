package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.EnumResolver;

/**
 * 
 * <p>
 * Note: casting within this class is all messed up -- just could not figure out a way
 * to properly deal with recursive definition of "EnumSet<K extends Enum<K>, V>
 * 
 * @author tsaloranta
 */
@SuppressWarnings("rawtypes")
public class EnumSetDeserializer
    extends StdDeserializer<EnumSet<?>>
{
    protected final Class<Enum> _enumClass;

    protected final EnumDeserializer _enumDeserializer;

    @SuppressWarnings("unchecked" )
    public EnumSetDeserializer(EnumResolver enumRes)
    {
        super(EnumSet.class);
        _enumDeserializer = new EnumDeserializer(enumRes);
        // this is fugly, but not sure of a better way...
        _enumClass = (Class<Enum>) ((Class<?>) enumRes.getEnumClass());
    }

    /**
     * Because of costs associated with constructing Enum resolvers,
     * let's cache instances by default.
     */
    @Override
    public boolean isCachable() { return true; }
    
    @SuppressWarnings("unchecked") 
    @Override
    public EnumSet<?> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (!jp.isExpectedStartArrayToken()) {
            throw ctxt.mappingException(EnumSet.class);
        }
        EnumSet result = constructSet();
        JsonToken t;

        while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
            /* What to do with nulls? Fail or ignore? Fail, for now
             * (note: would fail if we passed it to EnumDeserializer, too,
             * but in general nulls should never be passed to non-container
             * deserializers)
             */
            if (t == JsonToken.VALUE_NULL) {
                throw ctxt.mappingException(_enumClass);
            }
            Enum<?> value = _enumDeserializer.deserialize(jp, ctxt);
            result.add(value);
        }
        return result;
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        return typeDeserializer.deserializeTypedFromArray(jp, ctxt);
    }
    
    @SuppressWarnings("unchecked") 
    private EnumSet constructSet()
    {
    	// superbly ugly... but apparently necessary
    	return EnumSet.noneOf(_enumClass);
    }
}
