package tools.jackson.databind.deser.impl;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.SettableAnyProperty;
import tools.jackson.databind.deser.SettableBeanProperty;

/**
 * Handler for deserializing properties annotated with {@code @JsonWrapped}.
 * Maintains a mapping from wrapper name to inner properties.
 * When the wrapper field is encountered during deserialization, enters
 * the sub-object and dispatches inner properties to the flat bean setters.
 *
 * @since 3.1
 */
public class WrappedPropertyHandler implements java.io.Serializable
{
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    /**
     * Map from wrapper name to the list of inner properties.
     */
    protected final Map<String, List<SettableBeanProperty>> _wrappedProperties;

    /**
     * Precomputed lookup: wrapper name → (property name → property).
     * Built incrementally by {@link #addProperty} to avoid rebuilding on every
     * {@link #handleWrappedObject} call.
     */
    protected final Map<String, Map<String, SettableBeanProperty>> _innerLookups;

    public WrappedPropertyHandler() {
        _wrappedProperties = new LinkedHashMap<>();
        _innerLookups = new LinkedHashMap<>();
    }

    protected WrappedPropertyHandler(
            Map<String, List<SettableBeanProperty>> wrappedProperties)
    {
        _wrappedProperties = wrappedProperties;
        _innerLookups = new LinkedHashMap<>();
        for (Map.Entry<String, List<SettableBeanProperty>> entry : wrappedProperties.entrySet()) {
            Map<String, SettableBeanProperty> lookup = new HashMap<>();
            for (SettableBeanProperty prop : entry.getValue()) {
                lookup.put(prop.getName(), prop);
            }
            _innerLookups.put(entry.getKey(), lookup);
        }
    }

    public void addProperty(String wrapperName, SettableBeanProperty prop) {
        _wrappedProperties.computeIfAbsent(wrapperName, k -> new ArrayList<>())
                .add(prop);
        _innerLookups.computeIfAbsent(wrapperName, k -> new HashMap<>())
                .put(prop.getName(), prop);
    }

    public boolean hasWrapperName(String name) {
        return _wrappedProperties.containsKey(name);
    }

    /**
     * Deserialize all inner properties from a wrapper object.
     *
     * @param p         Parser positioned at START_OBJECT of the wrapper
     * @param ctxt      Deserialization context
     * @param bean      The target bean being populated
     * @param wrapperName The wrapper field name (for error messages)
     * @param anySetter Optional any-setter from the parent bean (may be null)
     */
    public void handleWrappedObject(JsonParser p, DeserializationContext ctxt,
            Object bean, String wrapperName,
            /* nullable */ SettableAnyProperty anySetter)
        throws JacksonException
    {
        JsonToken t = p.currentToken();

        // Handle null wrapper → treat as absent
        if (t == JsonToken.VALUE_NULL) {
            return;
        }

        // Handle non-object wrapper → error
        if (t != JsonToken.START_OBJECT) {
            ctxt.reportInputMismatch(bean.getClass(),
                "Expected JSON Object for wrapped property group '%s', got %s",
                wrapperName, t);
            return;
        }

        // Retrieve precomputed inner property lookup
        Map<String, SettableBeanProperty> innerLookup = _innerLookups.get(wrapperName);
        if (innerLookup == null) {
            innerLookup = Collections.emptyMap();
        }

        // Iterate inner properties
        while ((t = p.nextToken()) != JsonToken.END_OBJECT) {
            if (t != JsonToken.PROPERTY_NAME) {
                ctxt.reportWrongTokenException(bean.getClass(), JsonToken.PROPERTY_NAME,
                    "Unexpected token inside wrapped object '%s'", wrapperName);
                return;
            }
            String innerName = p.currentName();
            p.nextToken();  // move to value

            SettableBeanProperty innerProp = innerLookup.get(innerName);
            if (innerProp != null) {
                try {
                    innerProp.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    throw DatabindException.wrapWithPath(ctxt, e,
                            new JacksonException.Reference(bean, innerName));
                }
            } else {
                // Unknown inner property
                if (anySetter != null) {
                    try {
                        anySetter.deserializeAndSet(p, ctxt, bean, innerName);
                    } catch (Exception e) {
                        throw DatabindException.wrapWithPath(ctxt, e,
                                new JacksonException.Reference(bean, innerName));
                    }
                } else {
                    handleUnknownInnerProperty(p, ctxt, bean, wrapperName, innerName);
                }
            }
        }
    }

    @SuppressWarnings("unused") // wrapperName available for subclass overrides
    protected void handleUnknownInnerProperty(JsonParser p,
            DeserializationContext ctxt, Object bean,
            String wrapperName, String innerName)
        throws JacksonException
    {
        // Delegate to the standard unknown property handling.
        // This will throw if FAIL_ON_UNKNOWN_PROPERTIES is enabled, skip otherwise.
        // Defensively call p.skipChildren() afterwards to match the pattern in
        // StdDeserializer.handleUnknownProperty(): if a custom
        // DeserializationProblemHandler handles the property (returns true) without
        // consuming the value token, the parser would be left mid-stream. skipChildren()
        // is a no-op for scalars and for values already consumed, so it is always safe.
        ctxt.handleUnknownProperty(p, ctxt.findContextualValueDeserializer(
                ctxt.constructType(bean.getClass()), null),
                bean, innerName);
        p.skipChildren();
    }
}
