package tools.jackson.databind.ser;

import java.util.Map;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.jdk.JDKKeySerializers;
import tools.jackson.databind.ser.jdk.MapProperty;
import tools.jackson.databind.ser.jdk.MapSerializer;
import tools.jackson.databind.util.IgnorePropertiesUtil;

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
    protected final AnnotatedMember _anyGetter;

    protected ValueSerializer<Object> _anySerializer;

    protected MapSerializer _mapSerializer;

    /**
     * For {@code ObjectNode}/{@code JsonNode}-valued any-getters only: property-level
     * {@code @JsonIgnoreProperties} / {@code @JsonIncludeProperties} rules to apply to
     * the emitted entries. Map-valued any-getters get the same treatment through their
     * {@code MapSerializer} during contextualization; the node path has no such
     * serializer, so the check is captured here instead. {@code null} when no rules apply.
     *
     * @since 3.3
     */
    protected IgnorePropertiesUtil.Checker _inclusionChecker;

    /**
     * @since 2.19
     */
    @SuppressWarnings("unchecked")
    public AnyGetterWriter(BeanPropertyWriter parent, BeanProperty property,
            AnnotatedMember accessor, ValueSerializer<?> serializer)
    {
        super(parent);
        _anyGetter = accessor;
        _property = property;
        _anySerializer = (ValueSerializer<Object>) serializer;
        if (serializer instanceof MapSerializer mapSer) {
            _mapSerializer = mapSer;
        }
    }

    @Override
    public void fixAccess(SerializationConfig config) {
        _anyGetter.fixAccess(
                config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
    }

    // Note: NOT part of ResolvableSerializer...
    @SuppressWarnings("unchecked")
    public void resolve(SerializationContext ctxt)
    {
        // Built regardless of `_anySerializer`: a custom `@JsonSerialize` on a node-valued
        // any-getter leaves it non-null, but node entries are still written directly
        _inclusionChecker = _buildInclusionChecker(ctxt);
        // [databind#3604]: _anySerializer may be null for ObjectNode/JsonNode any-getters
        if (_anySerializer == null) {
            return;
        }
        // 05-Sep-2013, tatu: I _think_ this can be considered a primary property...
        ValueSerializer<?> ser = ctxt.handlePrimaryContextualization(_anySerializer, _property);
        _anySerializer = (ValueSerializer<Object>) ser;
        if (ser instanceof MapSerializer mapSer) {
            _mapSerializer = mapSer;
        }
    }

    private IgnorePropertiesUtil.Checker _buildInclusionChecker(SerializationContext ctxt)
    {
        final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        final AnnotatedMember member = (_property == null) ? null : _property.getMember();
        if (member == null) {
            return null;
        }
        final MapperConfig<?> config = ctxt.getConfig();
        // Mirrors annotation handling in `MapSerializer.createContextual()`, minus its merge
        // with pre-set ignored/included names (none exist for any-getters); keep in sync.
        // (empty "ignored" set is handled by `buildCheckerIfNeeded()`)
        return IgnorePropertiesUtil.buildCheckerIfNeeded(
                intr.findPropertyIgnoralByName(config, member).findIgnoredForSerialization(),
                intr.findPropertyInclusionByName(config, member).getIncluded());
    }

    private boolean _isIgnored(String name) {
        return (_inclusionChecker != null) && _inclusionChecker.shouldIgnore(name);
    }

    public void getAndSerialize(Object bean, JsonGenerator gen, SerializationContext ctxt)
        throws Exception
    {
        Object value = _anyGetter.getValue(bean);
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
                    _anyGetter.getName(), value.getClass().getName()));
        }
        // 23-Feb-2015, tatu: Nasty, but has to do (for now)
        if (_mapSerializer != null) {
            _mapSerializer.serializeWithoutTypeInfo((Map<?,?>) value, gen, ctxt);
            return;
        }
        _anySerializer.serialize(value, gen, ctxt);
    }

    @Override
    public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext ctxt) throws Exception {
        getAndSerialize(bean, gen, ctxt);
    }

    /**
     * [databind#6136]: The any-getter accessor name is not a property name in the
     * output -- the entries it emits are -- so the filter decides inclusion per
     * entry, instead of once for the accessor.
     *<p>
     * Note that as a result the accessor name (e.g. {@code "anyProperties"}) is no
     * longer consulted as a filter key: inclusion is decided by the emitted entry
     * names only. In particular {@code filterOutAllExcept(accessorName)} no longer
     * includes all of its entries, and a custom {@link PropertyFilter} that excluded
     * the accessor to drop the whole map no longer does so.
     *
     * @since 3.3
     */
    @Override
    public void serializeFilteredAsProperty(Object bean, JsonGenerator gen,
            SerializationContext ctxt, PropertyFilter filter)
        throws Exception
    {
        getAndFilter(bean, gen, ctxt, filter);
    }

    public void getAndFilter(Object bean, JsonGenerator gen, SerializationContext ctxt,
            PropertyFilter filter)
        throws Exception
    {
        Object value = _anyGetter.getValue(bean);
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
                            _anyGetter.getName(), value.getClass().getName()));
        }
        if (_mapSerializer != null) {
            _mapSerializer.serializeFilteredAnyProperties(ctxt, gen, bean, (Map<?,?>) value,
                    filter);
            return;
        }
        // ... not sure how custom handler would do it
        _anySerializer.serialize(value, gen, ctxt);
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
                _anyGetter.getName(), value.getClass().getName()));
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
            if (_isIgnored(entry.getKey())) {
                continue;
            }
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
        // plain names, same as `_serializeObjectNodeEntries()` (no custom key serializer)
        final ValueSerializer<Object> keySer = JDKKeySerializers.getStdKeySerializer(
                ctxt.getConfig(), String.class, false);
        for (Map.Entry<String, JsonNode> entry : objectNode.properties()) {
            if (_isIgnored(entry.getKey())) {
                continue;
            }
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
