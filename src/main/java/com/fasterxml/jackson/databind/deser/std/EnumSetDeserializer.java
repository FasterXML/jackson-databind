package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Standard deserializer for {@link EnumSet}s.
 * <p>
 * Note: casting within this class is all messed up -- just could not figure out a way
 * to properly deal with recursive definition of "EnumSet<K extends Enum<K>, V>
 */
@SuppressWarnings("rawtypes")
public class EnumSetDeserializer
    extends StdDeserializer<EnumSet<?>>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = 3479455075597887177L;

    protected final JavaType _enumType;

    protected final Class<Enum> _enumClass;

    protected JsonDeserializer<Enum<?>> _enumDeserializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    @SuppressWarnings("unchecked" )
    public EnumSetDeserializer(JavaType enumType, JsonDeserializer<?> deser)
    {
        super(EnumSet.class);
        _enumType = enumType;
        _enumClass = (Class<Enum>) enumType.getRawClass();
        _enumDeserializer = (JsonDeserializer<Enum<?>>) deser;
    }

    public EnumSetDeserializer withDeserializer(JsonDeserializer<?> deser) {
        if (_enumDeserializer == deser) {
            return this;
        }
        return new EnumSetDeserializer(_enumType, deser);
    }
    
    /**
     * Because of costs associated with constructing Enum resolvers,
     * let's cache instances by default.
     */
    @Override
    public boolean isCachable() { return true; }
    
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        JsonDeserializer<?> deser = _enumDeserializer;
        if (deser == null) {
            deser = ctxt.findContextualValueDeserializer(_enumType, property);
        } else { // if directly assigned, probably not yet contextual, so:
            deser = ctxt.handleSecondaryContextualization(deser, property);
        }
        return withDeserializer(deser);
    }

    /*
    /**********************************************************
    /* JsonDeserializer API
    /**********************************************************
     */
    
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

        try {
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
                /* 24-Mar-2012, tatu: As per [JACKSON-810], may actually get nulls;
                 *    but EnumSets don't allow nulls so need to skip.
                 */
                if (value != null) { 
                    result.add(value);
                }
            }
        } catch (Exception e) {
            throw JsonMappingException.wrapWithPath(e, result, result.size());
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
