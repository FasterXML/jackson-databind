package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Container class for core Jackson type deserializers.
 */
@SuppressWarnings("serial")
public class JacksonDeserializers
{
    /**
     * @deprecated Since 2.2 -- use {@link #find} instead.
     */
    @Deprecated
    public static StdDeserializer<?>[] all()
    {
        // note: JsonLocation supported via ValueInstantiator
        return  new StdDeserializer[] {
            JavaTypeDeserializer.instance,
            TokenBufferDeserializer.instance
        };
    }

    public static JsonDeserializer<?> find(Class<?> rawType)
    {
        if (rawType == TokenBuffer.class) {
            return TokenBufferDeserializer.instance;
        }
        if (JavaType.class.isAssignableFrom(rawType)) {
            return JavaTypeDeserializer.instance;
        }
        return null;
    }
    
    public static ValueInstantiator findValueInstantiator(DeserializationConfig config,
            BeanDescription beanDesc)
    {
        if (beanDesc.getBeanClass() == JsonLocation.class) {
            return JsonLocationInstantiator.instance;
        }
        return null;
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

    /**
     * For {@link JsonLocation}, we should be able to just implement
     * {@link ValueInstantiator} (not that explicit one would be very
     * hard but...)
     */
    public static class JsonLocationInstantiator extends ValueInstantiator
    {
        public final static JsonLocationInstantiator instance = new JsonLocationInstantiator();
        
        @Override
        public String getValueTypeDesc() {
            return JsonLocation.class.getName();
        }
        
        @Override
        public boolean canCreateFromObjectWith() { return true; }
        
        @Override
        public CreatorProperty[] getFromObjectArguments(DeserializationConfig config) {
            JavaType intType = config.constructType(Integer.TYPE);
            JavaType longType = config.constructType(Long.TYPE);
            return  new CreatorProperty[] {
                    creatorProp("sourceRef", config.constructType(Object.class), 0),
                    creatorProp("byteOffset", longType, 1),
                    creatorProp("charOffset", longType, 2),
                    creatorProp("lineNr", intType, 3),
                    creatorProp("columnNr", intType, 4)
            };
        }

        private static CreatorProperty creatorProp(String name, JavaType type, int index) {
            return new CreatorProperty(name, type, null,
                    null, null, null, index, null, true);
        }
        
        @Override
        public Object createFromObjectWith(DeserializationContext ctxt, Object[] args) {
            return new JsonLocation(args[0], _long(args[1]), _long(args[2]),
                    _int(args[3]), _int(args[4]));
        }

        private final static long _long(Object o) {
            return (o == null) ? 0L : ((Number) o).longValue();
        }
        private final static int _int(Object o) {
            return (o == null) ? 0 : ((Number) o).intValue();
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
        public final static TokenBufferDeserializer instance = new TokenBufferDeserializer();
        
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
