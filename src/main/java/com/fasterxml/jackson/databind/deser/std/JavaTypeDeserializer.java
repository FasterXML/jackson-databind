package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;

/**
 * Deserializer for {@link JavaType} values.
 */
public class JavaTypeDeserializer
    extends StdScalarDeserializer<JavaType>
{
    private static final long serialVersionUID = 1;

    public final static JavaTypeDeserializer instance = new JavaTypeDeserializer();
    
    public JavaTypeDeserializer() { super(JavaType.class); }
    
    @Override
    public JavaType deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken curr = jp.getCurrentToken();
        // Usually should just get string value:
        if (curr == JsonToken.VALUE_STRING) {
            String str = jp.getText().trim();
            if (str.length() == 0) {
                return getEmptyValue();
            }
            return ctxt.getTypeFactory().constructFromCanonical(str);
        }
        // or occasionally just embedded object maybe
        if (curr == JsonToken.VALUE_EMBEDDED_OBJECT) {
            return (JavaType) jp.getEmbeddedObject();
        }
        throw ctxt.mappingException(_valueClass);
    }
}