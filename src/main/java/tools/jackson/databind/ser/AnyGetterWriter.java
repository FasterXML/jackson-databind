package tools.jackson.databind.ser;

import java.util.Map;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.jdk.MapProperty;
import tools.jackson.databind.ser.jdk.MapSerializer;

/**
 * Class similar to {@link BeanPropertyWriter}, but that will be used
 * for serializing {@link com.fasterxml.jackson.annotation.JsonAnyGetter} annotated
 * (Map) properties
 */
public class AnyGetterWriter extends BeanPropertyWriter
{
    protected final BeanProperty _property;

    /**
     * Method (or Field) that represents the "any getter"
     */
    protected final AnnotatedMember _accessor;

    protected ValueSerializer<Object> _serializer;

    protected MapSerializer _mapSerializer;

    /**
     * @since 2.19
     */
    @SuppressWarnings("unchecked")
    public AnyGetterWriter(BeanPropertyWriter parent, BeanProperty property,
            AnnotatedMember accessor, ValueSerializer<?> serializer)
    {
        super(parent);
        _accessor = accessor;
        _property = property;
        _serializer = (ValueSerializer<Object>) serializer;
        if (serializer instanceof MapSerializer mapSer) {
            _mapSerializer = mapSer;
        }
    }

    @Override
    public void fixAccess(SerializationConfig config) {
        _accessor.fixAccess(
                config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
    }

    // Note: NOT part of ResolvableSerializer...
    @SuppressWarnings("unchecked")
    public void resolve(SerializationContext ctxt)
    {
        // [databind#3604]: _serializer may be null for ObjectNode/JsonNode any-getters
        if (_serializer == null) {
            return;
        }
        // 05-Sep-2013, tatu: I _think_ this can be considered a primary property...
        ValueSerializer<?> ser = ctxt.handlePrimaryContextualization(_serializer, _property);
        _serializer = (ValueSerializer<Object>) ser;
        if (ser instanceof MapSerializer mapSer) {
            _mapSerializer = mapSer;
        }
    }

    public void getAndSerialize(Object bean, JsonGenerator gen, SerializationContext ctxt)
        throws Exception
    {
        Object value = _accessor.getValue(bean);
        if (value == null) {
            return;
        }
        // [databind#3604]: Support ObjectNode/JsonNode for @JsonAnyGetter
        if (value instanceof JsonNode) {
            _serializeObjectNodeEntries(_verifyObjectNode(value, ctxt), gen, ctxt);
            return;
        }
        if (!(value instanceof Map<?,?>)) {
            ctxt.reportBadDefinition(_property.getType(), "Value returned by 'any-getter' %s() not java.util.Map but %s".formatted(
                    _accessor.getName(), value.getClass().getName()));
        }
        // 23-Feb-2015, tatu: Nasty, but has to do (for now)
        if (_mapSerializer != null) {
            _mapSerializer.serializeWithoutTypeInfo((Map<?,?>) value, gen, ctxt);
            return;
        }
        _serializer.serialize(value, gen, ctxt);
    }

    @Override
    public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext ctxt) throws Exception {
        getAndSerialize(bean, gen, ctxt);
    }

    public void getAndFilter(Object bean, JsonGenerator gen, SerializationContext ctxt,
            PropertyFilter filter)
        throws Exception
    {
        Object value = _accessor.getValue(bean);
        if (value == null) {
            return;
        }
        // [databind#3604]: Support ObjectNode/JsonNode for @JsonAnyGetter
        if (value instanceof JsonNode) {
            _serializeFilteredObjectNodeEntries(_verifyObjectNode(value, ctxt), gen, ctxt,
                    bean, filter);
            return;
        }
        if (!(value instanceof Map<?,?>)) {
            ctxt.reportBadDefinition(_property.getType(),
                    "Value returned by 'any-getter' (%s()) not java.util.Map but %s".formatted(
                            _accessor.getName(), value.getClass().getName()));
        }
        if (_mapSerializer != null) {
            _mapSerializer.serializeFilteredAnyProperties(ctxt, gen, bean, (Map<?,?>) value,
                    filter);
            return;
        }
        // ... not sure how custom handler would do it
        _serializer.serialize(value, gen, ctxt);
    }

    /**
     * Helper method to verify that a {@link JsonNode} value is an {@link ObjectNode},
     * throwing a clear error if not (e.g. ArrayNode).
     *
     * @since 3.2
     */
    protected ObjectNode _verifyObjectNode(Object value, SerializationContext ctxt)
        throws DatabindException
    {
        if (value instanceof ObjectNode objectNode) {
            return objectNode;
        }
        return ctxt.reportBadDefinition(_property.getType(), String.format(
                "Value returned by 'any-getter' %s not `ObjectNode` but `%s`; only `ObjectNode`s can be used as `@JsonAnyGetter` values",
                _accessor.getName(), value.getClass().getName()));
    }

    /**
     * Helper method for serializing entries of an {@link ObjectNode}
     * as individual properties (for {@code @JsonAnyGetter} support).
     *
     * @since 3.2
     */
    protected void _serializeObjectNodeEntries(ObjectNode objectNode,
            JsonGenerator gen, SerializationContext ctxt)
        throws Exception
    {
        for (Map.Entry<String, JsonNode> entry : objectNode.properties()) {
            gen.writeName(entry.getKey());
            entry.getValue().serialize(gen, ctxt);
        }
    }

    /**
     * Variant of {@link #_serializeObjectNodeEntries} used when a JSON Filter is in
     * effect: entries become properties of the enclosing POJO, so each one is passed
     * to the filter the same way {@code MapSerializer} passes entries of a
     * {@link Map}-valued any-getter.
     *
     * @since 3.3
     */
    protected void _serializeFilteredObjectNodeEntries(ObjectNode objectNode,
            JsonGenerator gen, SerializationContext ctxt,
            Object bean, PropertyFilter filter)
        throws Exception
    {
        final MapProperty prop = new MapProperty(null, _property);
        final ValueSerializer<Object> keySer = ctxt.findKeySerializer(String.class, _property);
        for (Map.Entry<String, JsonNode> entry : objectNode.properties()) {
            final JsonNode v = entry.getValue();
            prop.reset(entry.getKey(), v, keySer, ctxt.findValueSerializer(v.getClass()));
            filter.serializeAsProperty(bean, gen, ctxt, prop);
        }
    }

    @Override
    public void depositSchemaProperty(JsonObjectFormatVisitor v, SerializationContext ctxt)
    {
        // no-op
    }
}
