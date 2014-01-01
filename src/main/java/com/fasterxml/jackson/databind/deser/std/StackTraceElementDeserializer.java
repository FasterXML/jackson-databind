package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;

public class StackTraceElementDeserializer
    extends StdScalarDeserializer<StackTraceElement>
{
    private static final long serialVersionUID = 1L;

    public StackTraceElementDeserializer() { super(StackTraceElement.class); }

    @Override
    public StackTraceElement deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        JsonToken t = jp.getCurrentToken();
        // Must get an Object
        if (t == JsonToken.START_OBJECT) {
            String className = "", methodName = "", fileName = "";
            int lineNumber = -1;

            while ((t = jp.nextValue()) != JsonToken.END_OBJECT) {
                String propName = jp.getCurrentName();
                if ("className".equals(propName)) {
                    className = jp.getText();
                } else if ("fileName".equals(propName)) {
                    fileName = jp.getText();
                } else if ("lineNumber".equals(propName)) {
                    if (t.isNumeric()) {
                        lineNumber = jp.getIntValue();
                    } else {
                        throw JsonMappingException.from(jp, "Non-numeric token ("+t+") for property 'lineNumber'");
                    }
                } else if ("methodName".equals(propName)) {
                    methodName = jp.getText();
                } else if ("nativeMethod".equals(propName)) {
                    // no setter, not passed via constructor: ignore
                } else {
                    handleUnknownProperty(jp, ctxt, _valueClass, propName);
                }
            }
            return new StackTraceElement(className, methodName, fileName, lineNumber);
        }
        throw ctxt.mappingException(_valueClass, t);
    }
}