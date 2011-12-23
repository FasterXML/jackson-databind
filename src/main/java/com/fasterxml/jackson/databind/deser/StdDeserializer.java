package com.fasterxml.jackson.databind.deser;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.JavaType;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.TypeDeserializer;
import com.fasterxml.jackson.databind.annotate.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;


/**
 * @deprecated Since 1.9, use {@link com.fasterxml.jackson.databind.deser.std.StdDeserializer} instead.
 */
@Deprecated
public abstract class StdDeserializer<T>
    extends com.fasterxml.jackson.databind.deser.std.StdDeserializer<T>
{
    protected StdDeserializer(Class<?> vc) {
        super(vc);
    }

    protected StdDeserializer(JavaType valueType) {
        super(valueType);
    }

    /*
    /**********************************************************
    /* Deprecated inner classes
    /**********************************************************
     */

    /**
     * @deprecated Since 1.9 use {@link com.fasterxml.jackson.databind.deser.std.ClassDeserializer} instead.
     */
    @Deprecated
    @JacksonStdImpl
    public class ClassDeserializer extends com.fasterxml.jackson.databind.deser.std.ClassDeserializer { }

    /**
     * @deprecated Since 1.9 use {@link com.fasterxml.jackson.databind.deser.std.CalendarDeserializer} instead.
     */
    @Deprecated
    @JacksonStdImpl
    public class CalendarDeserializer extends com.fasterxml.jackson.databind.deser.std.CalendarDeserializer { }
    
    /**
     * @deprecated Since 1.9 use {@link com.fasterxml.jackson.databind.deser.std.StringDeserializer} instead.
     */
    @Deprecated
    @JacksonStdImpl
    public final static class StringDeserializer
        extends StdScalarDeserializer<String>
    {
        public StringDeserializer() { super(String.class); }

        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken curr = jp.getCurrentToken();
            if (curr == JsonToken.VALUE_STRING) {
                return jp.getText();
            }
            if (curr == JsonToken.VALUE_EMBEDDED_OBJECT) {
                Object ob = jp.getEmbeddedObject();
                if (ob == null) {
                    return null;
                }
                if (ob instanceof byte[]) {
                    return Base64Variants.getDefaultVariant().encode((byte[]) ob, false);
                }
                return ob.toString();
            }
            if (curr.isScalarValue()) {
                return jp.getText();
            }
            throw ctxt.mappingException(_valueClass, curr);
        }

        @SuppressWarnings("deprecation")
        @Override
        public String deserializeWithType(JsonParser jp, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer)
            throws IOException, JsonProcessingException
        {
            return deserialize(jp, ctxt);
        }
    }

}
