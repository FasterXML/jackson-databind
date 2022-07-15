package tools.jackson.databind.deser.std;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.TypeDeserializer;

/**
 * Bogus deserializer that will simply skip all content there is to map
 * and returns Java null reference.
 */
public class NullifyingDeserializer
    extends StdDeserializer<Object>
{
    public final static NullifyingDeserializer instance = new NullifyingDeserializer();
    
    public NullifyingDeserializer() { super(Object.class); }

    /*
    /**********************************************************************
    /* Deserializer API
    /**********************************************************************
     */

    @Override
    public Boolean supportsUpdate(DeserializationConfig config) {
        return Boolean.FALSE;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        // 29-Jan-2016, tatu: Simple skipping for all other tokens, but PROPERTY_NAME bit
        //    special unfortunately
        if (p.hasToken(JsonToken.PROPERTY_NAME)) {
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
            TypeDeserializer typeDeserializer) throws JacksonException
    {
        // Not sure if we need to bother but:

        switch (p.currentTokenId()) {
        case JsonTokenId.ID_START_ARRAY:
        case JsonTokenId.ID_START_OBJECT:
        case JsonTokenId.ID_PROPERTY_NAME:
            return typeDeserializer.deserializeTypedFromAny(p, ctxt);
        default:
            return null;
        }
    }
}
