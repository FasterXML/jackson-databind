package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Bogus deserializer that will simply skip all content there is to map
 * and returns Java null reference.
 * 
 * @since 2.2
 */
public class NullifyingDeserializer
    extends StdDeserializer<Object>
{
    private static final long serialVersionUID = 1L;

    public final static NullifyingDeserializer instance = new NullifyingDeserializer();
    
    public NullifyingDeserializer() { super(Object.class); }

    /*
    /**********************************************************
    /* Deserializer API
    /**********************************************************
     */
    
    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        jp.skipChildren();
        return null;
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        // Not sure if we need to bother but:

        JsonToken t = jp.getCurrentToken();
        switch (t) {
        case START_ARRAY:
        case START_OBJECT:
        case FIELD_NAME:
            return typeDeserializer.deserializeTypedFromAny(jp, ctxt);
        default:
            return null;
        }
    }
}
