package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

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
    implements ResolvableDeserializer
{
    protected final JavaType _enumType;

    protected final BeanProperty _property;

    protected final Class<Enum> _enumClass;

    protected JsonDeserializer<Enum<?>> _enumDeserializer;

    @SuppressWarnings("unchecked" )
    public EnumSetDeserializer(JavaType enumType, BeanProperty prop,
            JsonDeserializer<?> deser)
    {
        super(EnumSet.class);
        _enumType = enumType;
        _property = prop;
        _enumClass = (Class<Enum>) enumType.getRawClass();
        _enumDeserializer = (JsonDeserializer<Enum<?>>) deser;
    }

    /**
     * Because of costs associated with constructing Enum resolvers,
     * let's cache instances by default.
     */
    @Override
    public boolean isCachable() { return true; }
    
    @SuppressWarnings("unchecked")
    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException
    {
        if (_enumDeserializer == null) {
            _enumDeserializer = (JsonDeserializer<Enum<?>>)(JsonDeserializer<?>)
                ctxt.findValueDeserializer(_enumType, _property);
        }
    }
    
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
