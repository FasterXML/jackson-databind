package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.util.ClassUtil;

@JacksonStdImpl
public class ClassDeserializer
    extends StdScalarDeserializer<Class<?>>
{
    private static final long serialVersionUID = 1L;

    public final static ClassDeserializer instance = new ClassDeserializer();
    
    public ClassDeserializer() { super(Class.class); }

    @Override
    public Class<?> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken curr = jp.getCurrentToken();
        // Currently will only accept if given simple class name
        if (curr == JsonToken.VALUE_STRING) {
            String className = jp.getText().trim();
            try {
                return ctxt.findClass(className);
            } catch (Exception e) {
                throw ctxt.instantiationException(_valueClass, ClassUtil.getRootCause(e));
            }
        }
        throw ctxt.mappingException(_valueClass, curr);
    }
}
