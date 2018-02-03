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
        JsonToken t = p.currentToken();
        // Must get an Object
        if (t == JsonToken.START_OBJECT) {
            String className = "", methodName = "", fileName = "";
            // Java 9 adds couple more things
            String moduleName = null, moduleVersion = null;
            String classLoaderName = null;
            int lineNumber = -1;

            while ((t = p.nextValue()) != JsonToken.END_OBJECT) {
                String propName = p.currentName();

                switch (propName) {
                case "className":
                    className = p.getText();
                    break;
                case "classLoaderName":
                    classLoaderName = p.getText();
                    break;
                case "fileName":
                    fileName = p.getText();
                    break;
                case "lineNumber":
                    if (t.isNumeric()) {
                        lineNumber = p.getIntValue();
                    } else {
                        lineNumber = _parseIntPrimitive(p, ctxt);
                    }
                    break;
                case "methodName":
                    methodName = p.getText();
                    break;
                case "moduleName":
                    moduleName = p.getText();
                    break;
                case "moduleVersion":
                    moduleVersion = p.getText();
                    break;

                    // and then fluff we can't use:
                
                case "nativeMethod":
                    // no setter, not passed via constructor: ignore
                case "declaringClass":
                    // 01-Nov-2017: [databind#1794] Not sure if we should but... let's prune it for now
                case "format":
                    // 02-Feb-2018, tatu: Java 9 apparently adds "format" somehow...
                    break;
                
                default:
                    handleUnknownProperty(p, ctxt, _valueClass, propName);
                }
                p.skipChildren(); // just in case we might get structured values
            }
            return constructValue(ctxt, className, methodName, fileName, lineNumber,
                    moduleName, moduleVersion, classLoaderName);
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
     */
    protected StackTraceElement constructValue(DeserializationContext ctxt,
            String className, String methodName, String fileName, int lineNumber,
            String moduleName, String moduleVersion, String classLoaderName)
    {
        // 21-May-2016, tatu: With Java 9, need to use different constructor, probably
        //   via different module, and throw exception here if extra args passed
        return new StackTraceElement(className, methodName, fileName, lineNumber);
    }
}
