package tools.jackson.databind.deser.jdk;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

public class StackTraceElementDeserializer
    extends StdScalarDeserializer<StackTraceElement>
{
    protected final ValueDeserializer<?> _adapterDeserializer;

    protected StackTraceElementDeserializer(ValueDeserializer<?> ad)
    {
        super(StackTraceElement.class);
        _adapterDeserializer = ad;
    }

    public static ValueDeserializer<?> construct(DeserializationContext ctxt) {
        // 27-May-2022, tatu: MUST contextualize, alas, for optimized bean property
        //    matching to work
        ValueDeserializer<?> adapterDeser = ctxt.findRootValueDeserializer(ctxt.constructType(Adapter.class));
        return new StackTraceElementDeserializer(adapterDeser);
    }

    @Override
    public StackTraceElement deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        JsonToken t = p.currentToken();

        // Must get an Object
        if (t == JsonToken.START_OBJECT || t == JsonToken.PROPERTY_NAME) {
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
        return (StackTraceElement) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    protected StackTraceElement constructValue(DeserializationContext ctxt,
            Adapter adapted)
    {
        return constructValue(ctxt, adapted.className, adapted.methodName,
                adapted.fileName, adapted.lineNumber,
                adapted.moduleName, adapted.moduleVersion,
                adapted.classLoaderName);
    }

    /**
     * Overridable factory method used for constructing {@link StackTraceElement}s.
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
