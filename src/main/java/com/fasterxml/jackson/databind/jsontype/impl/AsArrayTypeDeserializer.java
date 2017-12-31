package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.JsonParserSequence;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Type deserializer used with {@link As#WRAPPER_ARRAY}
 * inclusion mechanism. Simple since JSON structure used is always
 * the same, regardless of structure used for actual value: wrapping
 * is done using a 2-element JSON Array where type id is the first
 * element, and actual object data as second element.
 */
public class AsArrayTypeDeserializer
    extends TypeDeserializerBase
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public AsArrayTypeDeserializer(JavaType bt, TypeIdResolver idRes,
            String typePropertyName, boolean typeIdVisible, JavaType defaultImpl)
    {
        super(bt, idRes, typePropertyName, typeIdVisible, defaultImpl);
    }

    public AsArrayTypeDeserializer(AsArrayTypeDeserializer src, BeanProperty property) {
        super(src, property);
    }
    
    @Override
    public TypeDeserializer forProperty(BeanProperty prop) {
        // usually if it's null:
        return (prop == _property) ? this : new AsArrayTypeDeserializer(this, prop);
    }
    
    @Override
    public As getTypeInclusion() { return As.WRAPPER_ARRAY; }

    /**
     * Method called when actual object is serialized as JSON Array.
     */
    @Override
    public Object deserializeTypedFromArray(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return _deserialize(jp, ctxt);
    }

    /**
     * Method called when actual object is serialized as JSON Object
     */
    @Override
    public Object deserializeTypedFromObject(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return _deserialize(jp, ctxt);
    }
    
    @Override
    public Object deserializeTypedFromScalar(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return _deserialize(jp, ctxt);
    }    

    @Override
    public Object deserializeTypedFromAny(JsonParser jp, DeserializationContext ctxt) throws IOException {
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
    protected Object _deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // 02-Aug-2013, tatu: May need to use native type ids
        if (p.canReadTypeId()) {
            Object typeId = p.getTypeId();
            if (typeId != null) {
                return _deserializeWithNativeTypeId(p, ctxt, typeId);
            }
        }
        boolean hadStartArray = p.isExpectedStartArrayToken();
        String typeId = _locateTypeId(p, ctxt);
        JsonDeserializer<Object> deser = _findDeserializer(ctxt, typeId);
        // Minor complication: we may need to merge type id in?
        if (_typeIdVisible
                // 06-Oct-2014, tatu: To fix [databind#408], must distinguish between
                //   internal and external properties
                //  TODO: but does it need to be injected in external case? Why not?
                && !_usesExternalId()
                && p.isExpectedStartObjectToken()) {
            // but what if there's nowhere to add it in? Error? Or skip? For now, skip.
            TokenBuffer tb = TokenBuffer.forInputBuffering(p, ctxt);
            tb.writeStartObject(); // recreate START_OBJECT
            tb.writeFieldName(_typePropertyName);
            tb.writeString(typeId);
            // 02-Jul-2016, tatu: Depending on how JsonParserSequence is initialized it may
            //   try to access current token; ensure there isn't one
            p.clearCurrentToken();
            p = JsonParserSequence.createFlattened(false, tb.asParser(ctxt, p), p);
            p.nextToken();
        }
        Object value = deser.deserialize(p, ctxt);
        // And then need the closing END_ARRAY
        if (hadStartArray && p.nextToken() != JsonToken.END_ARRAY) {
            ctxt.reportWrongTokenException(baseType(), JsonToken.END_ARRAY,
                    "expected closing END_ARRAY after type information and deserialized value");
            // 05-May-2016, tatu: Not 100% what to do if exception is stored for
            //     future, and not thrown immediately: should probably skip until END_ARRAY

            // ... but for now, fall through
        }
        return value;
    }    
    
    protected String _locateTypeId(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        if (!p.isExpectedStartArrayToken()) {
            // Need to allow even more customized handling, if something unexpected seen...
            // but should there be a way to limit this to likely success cases?
            if (_defaultImpl != null) {
                return _idResolver.idFromBaseType();
            }
             ctxt.reportWrongTokenException(baseType(), JsonToken.START_ARRAY,
                     "need JSON Array to contain As.WRAPPER_ARRAY type information for class "+baseTypeName());
             return null;
        }
        // And then type id as a String
        JsonToken t = p.nextToken();
        if (t == JsonToken.VALUE_STRING) {
            String result = p.getText();
            p.nextToken();
            return result;
        }
        if (_defaultImpl != null) {
            return _idResolver.idFromBaseType();
        }
        ctxt.reportWrongTokenException(baseType(), JsonToken.VALUE_STRING,
                "need JSON String that contains type id (for subtype of %s)", baseTypeName());
        return null;
    }

    /**
     * @since 2.5
     */
    protected boolean _usesExternalId() {
        return false;
    }
}
