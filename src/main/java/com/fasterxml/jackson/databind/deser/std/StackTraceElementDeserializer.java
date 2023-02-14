package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

public class StackTraceElementDeserializer
    extends StdScalarDeserializer<StackTraceElement>
{
    private static final long serialVersionUID = 1L;

    protected final JsonDeserializer<?> _adapterDeserializer;

    @Deprecated // since 2.14
    public StackTraceElementDeserializer() {
        this(null);
    }

    protected StackTraceElementDeserializer(JsonDeserializer<?> ad)
    {
        super(StackTraceElement.class);
        _adapterDeserializer = ad;
    }

    /**
     * @since 2.14
     */
    public static JsonDeserializer<?> construct(DeserializationContext ctxt) throws JsonMappingException {
        // 26-May-2022, tatu: for legacy use, need to do this:
        if (ctxt == null) {
            return new StackTraceElementDeserializer();
        }
        JsonDeserializer<?> adapterDeser = ctxt.findNonContextualValueDeserializer(ctxt.constructType(Adapter.class));
        return new StackTraceElementDeserializer(adapterDeser);
    }

    @Override
    public StackTraceElement deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        JsonToken t = p.currentToken();

        // Must get an Object
        if (t == JsonToken.START_OBJECT || t == JsonToken.FIELD_NAME) {
            Adapter adapted;
            // 26-May-2022, tatu: for legacy use, need to do this:
            if (_adapterDeserializer == null) {
                adapted = ctxt.readValue(p, Adapter.class);
            } else {
                adapted = (Adapter) _adapterDeserializer.deserialize(p, ctxt);
            }
            return constructValue(ctxt, adapted);
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
     * @since 2.14
     */
    protected StackTraceElement constructValue(DeserializationContext ctxt,
            Adapter adapted)
    {
        return constructValue(ctxt, adapted.className, adapted.methodName,
                adapted.fileName, adapted.lineNumber,
                adapted.moduleName, adapted.moduleVersion,
                adapted.classLoaderName);
    }

    @Deprecated // since 2.9
    protected StackTraceElement constructValue(DeserializationContext ctxt,
            String className, String methodName, String fileName, int lineNumber,
            String moduleName, String moduleVersion) {
        return constructValue(ctxt, className, methodName, fileName, lineNumber,
                moduleName, moduleVersion, null);
    }

    /**
     * Overridable factory method used for constructing {@link StackTraceElement}s.
     *
     * @since 2.8
     */
    protected StackTraceElement constructValue(DeserializationContext ctxt,
            String className, String methodName, String fileName, int lineNumber,
            String moduleName, String moduleVersion, String classLoaderName)
    {
        // 21-May-2016, tatu: With Java 9, could use different constructor, probably
        //   via different module, and throw exception here if extra args passed
        return new StackTraceElement(className, methodName, fileName, lineNumber);
    }

    /**
     * Intermediate class used both for convenience of binding and
     * to support {@code PropertyNamingStrategy}.
     *
     * @since 2.14
     */
    public final static class Adapter {
        // NOTE: some String fields must not be nulls
        public String className = "", classLoaderName;
        public String declaringClass, format;
        public String fileName = "", methodName = "";
        public int lineNumber = -1;
        public String moduleName, moduleVersion;
        public boolean nativeMethod;
    }
}
