package tools.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import tools.jackson.core.*;
import tools.jackson.core.util.JsonParserSequence;
import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.util.TokenBuffer;

/**
 * Type deserializer used with {@link As#WRAPPER_OBJECT}
 * inclusion mechanism. Simple since JSON structure used is always
 * the same, regardless of structure used for actual value: wrapping
 * is done using a single-element JSON Object where type id is the key,
 * and actual object data as the value.
 */
public class AsWrapperTypeDeserializer
    extends TypeDeserializerBase
{
    public AsWrapperTypeDeserializer(JavaType bt, TypeIdResolver idRes,
            String typePropertyName, boolean typeIdVisible, JavaType defaultImpl)
    {
        super(bt, idRes, typePropertyName, typeIdVisible, defaultImpl);
    }

    protected AsWrapperTypeDeserializer(AsWrapperTypeDeserializer src, BeanProperty property) {
        super(src, property);
    }
    
    @Override
    public TypeDeserializer forProperty(BeanProperty prop) {
        return (prop == _property) ? this : new AsWrapperTypeDeserializer(this, prop);
    }
    
    @Override
    public As getTypeInclusion() { return As.WRAPPER_OBJECT; }

    /**
     * Deserializing type id enclosed using WRAPPER_OBJECT style is straightforward
     */
    @Override
    public Object deserializeTypedFromObject(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
        return _deserialize(jp, ctxt);
    }    

    @Override
    public Object deserializeTypedFromArray(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
        return _deserialize(jp, ctxt);
    }

    @Override
    public Object deserializeTypedFromScalar(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
        return _deserialize(jp, ctxt);
    }

    @Override
    public Object deserializeTypedFromAny(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
        return _deserialize(jp, ctxt);
    }
    
    /*
    /***************************************************************
    /* Internal methods
    /***************************************************************
     */

    /**
     * Method that handles type information wrapper, locates actual
     * subtype deserializer to use, and calls it to do actual
     * deserialization.
     */
    @SuppressWarnings("resource")
    protected Object _deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // 02-Aug-2013, tatu: May need to use native type ids
        if (p.canReadTypeId()) {
            Object typeId = p.getTypeId();
            if (typeId != null) {
                return _deserializeWithNativeTypeId(p, ctxt, typeId);
            }
        }
        // first, sanity checks
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) {
            // should always get field name, but just in case...
            if (p.nextToken() != JsonToken.PROPERTY_NAME) {
                ctxt.reportWrongTokenException(baseType(), JsonToken.PROPERTY_NAME,
                        "need JSON String that contains type id (for subtype of "+baseTypeName()+")");
            }
        } else if (t != JsonToken.PROPERTY_NAME) {
            ctxt.reportWrongTokenException(baseType(), JsonToken.START_OBJECT,
                    "need JSON Object to contain As.WRAPPER_OBJECT type information for class "+baseTypeName());
        }
        final String typeId = p.getText();
        ValueDeserializer<Object> deser = _findDeserializer(ctxt, typeId);
        p.nextToken();

        // Minor complication: we may need to merge type id in?
        if (_typeIdVisible && p.isExpectedStartObjectToken()) {
            // but what if there's nowhere to add it in? Error? Or skip? For now, skip.
            TokenBuffer tb = ctxt.bufferForInputBuffering(p);
            tb.writeStartObject(); // recreate START_OBJECT
            tb.writeName(_typePropertyName);
            tb.writeString(typeId);
            // 02-Jul-2016, tatu: Depending on for JsonParserSequence is initialized it may
            //   try to access current token; ensure there isn't one
            p.clearCurrentToken();
            p = JsonParserSequence.createFlattened(false, tb.asParser(ctxt, p), p);
            p.nextToken();
        }

        Object value = deser.deserialize(p, ctxt);
        // And then need the closing END_OBJECT
        if (p.nextToken() != JsonToken.END_OBJECT) {
            ctxt.reportWrongTokenException(baseType(), JsonToken.END_OBJECT,
                    "expected closing END_OBJECT after type information and deserialized value");
        }
        return value;
    }
}
