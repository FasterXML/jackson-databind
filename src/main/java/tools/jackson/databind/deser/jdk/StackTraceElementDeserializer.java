package tools.jackson.databind.deser.jdk;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.bean.BeanDeserializerBase;
import tools.jackson.databind.deser.bean.BeanPropertyMap;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.introspect.BeanPropertyDefinition;

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

        // [databind#429]: Check for mix-in @JsonProperty name overrides on
        //   StackTraceElement and propagate as aliases to the Adapter deserializer
        if (adapterDeser instanceof BeanDeserializerBase beanDeser) {
            Class<?> mixin = ctxt.getConfig().findMixInClassFor(StackTraceElement.class);
            if (mixin != null) {
                adapterDeser = _applyPropertyAliases(ctxt, beanDeser);
            }
        }
        return new StackTraceElementDeserializer(adapterDeser);
    }

    /**
     * Introspects {@code StackTraceElement} properties (including mix-ins) for
     * any {@code @JsonProperty} name overrides; if found, injects them as aliases
     * into the Adapter's {@link BeanPropertyMap} so that renamed properties are
     * recognized without any token-stream rewriting.
     */
    private static ValueDeserializer<?> _applyPropertyAliases(DeserializationContext ctxt,
            BeanDeserializerBase adapterDeser)
    {
        JavaType steType = ctxt.constructType(StackTraceElement.class);
        List<BeanPropertyDefinition> steDefs = ctxt.introspectBeanDescription(steType).findProperties();

        List<SettableBeanProperty> adapterProps = new ArrayList<>();
        adapterDeser.properties().forEachRemaining(adapterProps::add);

        // For each STE property where mix-in renamed it (external != internal),
        // find the matching Adapter property and register the external name as alias
        PropertyName[][] aliasDefs = null;
        for (BeanPropertyDefinition steProp : steDefs) {
            String externalName = steProp.getName();
            String internalName = steProp.getInternalName();
            if (externalName.equals(internalName)) {
                continue;
            }
            for (int i = 0, end = adapterProps.size(); i < end; ++i) {
                if (internalName.equals(adapterProps.get(i).getName())) {
                    if (aliasDefs == null) {
                        aliasDefs = new PropertyName[end][];
                    }
                    aliasDefs[i] = new PropertyName[] { PropertyName.construct(externalName) };
                    break;
                }
            }
        }
        if (aliasDefs == null) {
            return adapterDeser;
        }
        boolean caseInsensitive = ctxt.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
        BeanPropertyMap newMap = BeanPropertyMap.construct(
                ctxt.getConfig(), adapterProps, aliasDefs, caseInsensitive)
                .initMatcher(ctxt.tokenStreamFactory());
        return adapterDeser.withBeanProperties(newMap);
    }

    @Override
    public StackTraceElement deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        JsonToken t = p.currentToken();

        // Must get an Object
        if (t == JsonToken.START_OBJECT || t == JsonToken.PROPERTY_NAME) {
            return constructValue(ctxt, (Adapter) _adapterDeserializer.deserialize(p, ctxt));
        }
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
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
