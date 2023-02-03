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

    @Override // since 2.9
    public Boolean supportsUpdate(DeserializationConfig config) {
        return Boolean.FALSE;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // 29-Jan-2016, tatu: Simple skipping for all other tokens, but FIELD_NAME bit
        //    special unfortunately
        if (p.hasToken(JsonToken.FIELD_NAME)) {
            while (true) {
                JsonToken t = p.nextToken();
                if ((t == null) || (t == JsonToken.END_OBJECT)) {
                    break;
                }
                p.skipChildren();
            }
        } else {
            p.skipChildren();
        }
        return null;
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException
    {
        // Not sure if we need to bother but:

        switch (p.currentTokenId()) {
        case JsonTokenId.ID_START_ARRAY:
        case JsonTokenId.ID_START_OBJECT:
        case JsonTokenId.ID_FIELD_NAME:
            return typeDeserializer.deserializeTypedFromAny(p, ctxt);
        default:
            return null;
        }
    }
}
