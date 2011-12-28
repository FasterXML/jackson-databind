package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Container class for core Jackson type deserializers.
 */
public class JacksonDeserializers
{
    public static StdDeserializer<?>[] all()
    {
        return  new StdDeserializer[] {
            new TokenBufferDeserializer(),
            new JavaTypeDeserializer()
        };
    }

    /*
    /**********************************************************
    /* Deserializer implementations
    /**********************************************************
     */
    
    /**
     * Deserializer for {@link JavaType} values.
     */
    public static class JavaTypeDeserializer
        extends StdScalarDeserializer<JavaType>
    {
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
    
    /**
     * We also want to directly support deserialization of {@link TokenBuffer}.
     *<p>
     * Note that we use scalar deserializer base just because we claim
     * to be of scalar for type information inclusion purposes; actual
     * underlying content can be of any (Object, Array, scalar) type.
     */
    @JacksonStdImpl
    public static class TokenBufferDeserializer
        extends StdScalarDeserializer<TokenBuffer>
    {
        public TokenBufferDeserializer() { super(TokenBuffer.class); }

        @Override
        public TokenBuffer deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            TokenBuffer tb = new TokenBuffer(jp.getCodec());
            // quite simple, given that TokenBuffer is a JsonGenerator:
            tb.copyCurrentStructure(jp);
            return tb;
        }
    }
}
