package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

@JacksonStdImpl
public final class StringDeserializer extends StdScalarDeserializer<String>
{
    private static final long serialVersionUID = 1L;

    /**
     * @since 2.2
     */
    public final static StringDeserializer instance = new StringDeserializer();
    
    public StringDeserializer() { super(String.class); }

    // since 2.6, slightly faster lookups for this very common type
    @Override
    public boolean isCachable() { return true; }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return p.getText();
        }
        JsonToken t = p.getCurrentToken();
        // [databind#381]
        if ((t == JsonToken.START_ARRAY) && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final String parsed = _parseString(p, ctxt);
            if (p.nextToken() != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(p, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single 'String' value but there was more than a single value in the array");
            }            
            return parsed;            
        }
        // need to gracefully handle byte[] data, as base64
        if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
            Object ob = p.getEmbeddedObject();
            if (ob == null) {
                return null;
            }
            if (ob instanceof byte[]) {
                return ctxt.getBase64Variant().encode((byte[]) ob, false);
            }
            // otherwise, try conversion using toString()...
            return ob.toString();
        }
        // allow coercions for other scalar types
        String text = p.getValueAsString();
        // According to [databind#742], StringDeserializer shouldn't throw an exception if the value of the property is null
        if (text != null || JsonToken.VALUE_NULL.equals(p.getCurrentToken())) {
            return text;
        }
        throw ctxt.mappingException(_valueClass, p.getCurrentToken());
    }

    // Since we can never have type info ("natural type"; String, Boolean, Integer, Double):
    // (is it an error to even call this version?)
    @Override
    public String deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        return deserialize(p, ctxt);
    }
}
