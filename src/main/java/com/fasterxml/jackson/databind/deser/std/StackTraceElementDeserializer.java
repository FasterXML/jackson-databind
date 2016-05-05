package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;

public class StackTraceElementDeserializer
    extends StdScalarDeserializer<StackTraceElement>
{
    private static final long serialVersionUID = 1L;

    public StackTraceElementDeserializer() { super(StackTraceElement.class); }

    @Override
    public StackTraceElement deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        JsonToken t = p.getCurrentToken();
        // Must get an Object
        if (t == JsonToken.START_OBJECT) {
            String className = "", methodName = "", fileName = "";
            int lineNumber = -1;

            while ((t = p.nextValue()) != JsonToken.END_OBJECT) {
                String propName = p.getCurrentName();
                if ("className".equals(propName)) {
                    className = p.getText();
                } else if ("fileName".equals(propName)) {
                    fileName = p.getText();
                } else if ("lineNumber".equals(propName)) {
                    if (t.isNumeric()) {
                        lineNumber = p.getIntValue();
                    } else {
                        throw JsonMappingException.from(p, "Non-numeric token ("+t+") for property 'lineNumber'");
                    }
                } else if ("methodName".equals(propName)) {
                    methodName = p.getText();
                } else if ("nativeMethod".equals(propName)) {
                    // no setter, not passed via constructor: ignore
                } else {
                    handleUnknownProperty(p, ctxt, _valueClass, propName);
                }
            }
            return new StackTraceElement(className, methodName, fileName, lineNumber);
        } else if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final StackTraceElement value = deserialize(p, ctxt);
            if (p.nextToken() != JsonToken.END_ARRAY) {
                handleMissingEndArrayForSingle(p, ctxt);
            }
            return value;
        }
        throw ctxt.mappingException(_valueClass, t);
    }
}