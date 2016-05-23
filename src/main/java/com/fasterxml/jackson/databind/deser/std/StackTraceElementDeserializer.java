package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;

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
            // Java 9 adds couple more things
            String moduleName = null, moduleVersion = null;
            int lineNumber = -1;

            while ((t = p.nextValue()) != JsonToken.END_OBJECT) {
                String propName = p.getCurrentName();
                // TODO: with Java 8, convert to switch
                if ("className".equals(propName)) {
                    className = p.getText();
                } else if ("fileName".equals(propName)) {
                    fileName = p.getText();
                } else if ("lineNumber".equals(propName)) {
                    if (t.isNumeric()) {
                        lineNumber = p.getIntValue();
                    } else {
                        return (StackTraceElement) ctxt.handleUnexpectedToken(handledType(), t, p,
"Non-numeric token (%s) for property 'lineNumber'", t);
                    }
                } else if ("methodName".equals(propName)) {
                    methodName = p.getText();
                } else if ("nativeMethod".equals(propName)) {
                    // no setter, not passed via constructor: ignore
                } else if ("moduleName".equals(propName)) {
                    moduleName = p.getText();
                } else if ("moduleVersion".equals(propName)) {
                    moduleVersion = p.getText();
                } else {
                    handleUnknownProperty(p, ctxt, _valueClass, propName);
                }
            }
            return constructValue(ctxt, className, methodName, fileName, lineNumber,
                    moduleName, moduleVersion);
        } else if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final StackTraceElement value = deserialize(p, ctxt);
            if (p.nextToken() != JsonToken.END_ARRAY) {
                handleMissingEndArrayForSingle(p, ctxt);
            }
            return value;
        }
        return (StackTraceElement) ctxt.handleUnexpectedToken(_valueClass, p);
    }

    /**
     * Overridable factory method used for constructing {@link StackTraceElement}s.
     *
     * @since 2.8
     */
    protected StackTraceElement constructValue(DeserializationContext ctxt,
            String className, String methodName, String fileName, int lineNumber,
            String moduleName, String moduleVersion)
    {
        // 21-May-2016, tatu: With Java 9, need to use different constructor, probably
        //   via different module, and throw exception here if extra args passed
        return new StackTraceElement(className, methodName, fileName, lineNumber);
    }
}
