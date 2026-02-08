package tools.jackson.databind.deser.jdk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.util.TokenBuffer;

public class StackTraceElementDeserializer
    extends StdScalarDeserializer<StackTraceElement>
{
    protected final ValueDeserializer<?> _adapterDeserializer;

    /**
     * Optional mapping from mix-in property names to standard Adapter field names.
     * {@code null} if no mix-in name overrides are present (common case).
     */
    protected final Map<String, String> _propertyAliases;

    protected StackTraceElementDeserializer(ValueDeserializer<?> ad)
    {
        super(StackTraceElement.class);
        _adapterDeserializer = ad;
        _propertyAliases = null;
    }

    protected StackTraceElementDeserializer(ValueDeserializer<?> ad,
            Map<String, String> propertyAliases)
    {
        super(StackTraceElement.class);
        _adapterDeserializer = ad;
        _propertyAliases = propertyAliases;
    }

    public static ValueDeserializer<?> construct(DeserializationContext ctxt) {
        // 27-May-2022, tatu: MUST contextualize, alas, for optimized bean property
        //    matching to work
        ValueDeserializer<?> adapterDeser = ctxt.findRootValueDeserializer(ctxt.constructType(Adapter.class));

        // Check for mix-in @JsonProperty name overrides on StackTraceElement
        Map<String, String> aliases = _findPropertyAliases(ctxt);
        if (aliases != null) {
            return new StackTraceElementDeserializer(adapterDeser, aliases);
        }
        return new StackTraceElementDeserializer(adapterDeser);
    }

    /**
     * Introspect {@code StackTraceElement} properties (including mix-ins) to find
     * any {@code @JsonProperty} name overrides. Returns {@code null} if no overrides found.
     */
    private static Map<String, String> _findPropertyAliases(DeserializationContext ctxt) {
        JavaType steType = ctxt.constructType(StackTraceElement.class);
        BeanDescription beanDesc = ctxt.introspectBeanDescription(steType);
        List<BeanPropertyDefinition> props = beanDesc.findProperties();

        Map<String, String> aliases = null;
        for (BeanPropertyDefinition prop : props) {
            String externalName = prop.getName();
            String internalName = prop.getInternalName();
            if (!externalName.equals(internalName)) {
                if (aliases == null) {
                    aliases = new HashMap<>();
                }
                aliases.put(externalName, internalName);
            }
        }
        return aliases;
    }

    @Override
    public StackTraceElement deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        JsonToken t = p.currentToken();

        // Must get an Object
        if (t == JsonToken.START_OBJECT || t == JsonToken.PROPERTY_NAME) {
            // If we have mix-in aliases, translate property names before
            // feeding to the Adapter deserializer
            JsonParser effectiveParser = p;
            if (_propertyAliases != null) {
                effectiveParser = _translatePropertyNames(p, ctxt);
            }

            Adapter adapted;
            // 26-May-2022, tatu: for legacy use, need to do this:
            if (_adapterDeserializer == null) {
                adapted = ctxt.readValue(effectiveParser, Adapter.class);
            } else {
                adapted = (Adapter) _adapterDeserializer.deserialize(effectiveParser, ctxt);
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

    /**
     * Pre-process JSON token stream, translating mix-in property names
     * to standard Adapter field names.
     */
    private JsonParser _translatePropertyNames(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        TokenBuffer buf = ctxt.bufferForInputBuffering(p);
        // If we're at START_OBJECT, write it through
        if (p.currentToken() == JsonToken.START_OBJECT) {
            buf.writeStartObject();
            p.nextToken();
        }
        // Process property name/value pairs
        while (p.currentToken() == JsonToken.PROPERTY_NAME) {
            String name = p.currentName();
            String translated = _propertyAliases.get(name);
            buf.writeName(translated != null ? translated : name);
            p.nextToken();
            buf.copyCurrentStructure(p);
            p.nextToken();
        }
        // Write END_OBJECT if present
        if (p.currentToken() == JsonToken.END_OBJECT) {
            buf.writeEndObject();
        }
        return buf.asParserOnFirstToken(ctxt);
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
        // 08-Dec-2024, tatu: With Jackson 3.0 can use full Java 9 introduced
        //   constructor, finally
        return new StackTraceElement(classLoaderName, moduleName, moduleVersion,
                className, methodName, fileName, lineNumber);
    }

    /**
     * Intermediate class used both for convenience of binding and
     * to support {@code PropertyNamingStrategy}.
     *<p>
     * NOTE: MUST remain {@code public} for JDK 17 at least to avoid
     * needing opening up access separately.
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
