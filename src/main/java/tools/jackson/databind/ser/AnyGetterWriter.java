package tools.jackson.databind.ser;

import java.util.Map;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.jdk.MapSerializer;
import tools.jackson.databind.util.NameTransformer;

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
     * Optional name transformer to apply to map keys, used when this any-getter
     * is part of a bean serialized via {@code @JsonUnwrapped} with a prefix/suffix.
     * @since 3.3
     */
    protected final NameTransformer _nameTransformer;

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
        _nameTransformer = null;
    }

    /**
     * Copy constructor used when applying a {@link NameTransformer} via {@link #rename}.
     */
    protected AnyGetterWriter(AnyGetterWriter src, NameTransformer transformer)
    {
        super(src);
        _accessor = src._accessor;
        _property = src._property;
        _serializer = src._serializer;
        _mapSerializer = src._mapSerializer;
        _nameTransformer = (src._nameTransformer == null) ? transformer
                : NameTransformer.chainedTransformer(transformer, src._nameTransformer);
    }

    /**
     * Override to return an {@link AnyGetterWriter} with the given transformer
     * stored so that map keys are prefixed/suffixed during serialization, rather
     * than renaming the property itself (which has no meaning for any-getter).
     */
    @Override
    public BeanPropertyWriter rename(NameTransformer transformer) {
        return new AnyGetterWriter(this, transformer);
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
            ObjectNode objectNode = _verifyObjectNode(value, ctxt);
            if (_nameTransformer != null) {
                _serializeObjectNodeEntriesWithTransformer(objectNode, gen, ctxt, _nameTransformer);
            } else {
                _serializeObjectNodeEntries(objectNode, gen, ctxt);
            }
            return;
        }
        if (!(value instanceof Map<?,?>)) {
            ctxt.reportBadDefinition(_property.getType(), "Value returned by 'any-getter' %s() not java.util.Map but %s".formatted(
                    _accessor.getName(), value.getClass().getName()));
        }
        // 23-Feb-2015, tatu: Nasty, but has to do (for now)
        if (_mapSerializer != null) {
            // If we have a name transformer, wrap the generator so that
            // MapSerializer's full key/value handling (custom key serializers,
            // content inclusion, polymorphic typing) is preserved while keys
            // get the prefix/suffix applied.
            if (_nameTransformer != null) {
                gen = new NameTransformingGenerator(gen, _nameTransformer);
            }
            _mapSerializer.serializeWithoutTypeInfo((Map<?,?>) value, gen, ctxt);
            return;
        }
        if (_nameTransformer != null) {
            gen = new NameTransformingGenerator(gen, _nameTransformer);
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
            // No special filtering support for ObjectNode (yet); just serialize entries
            ObjectNode objectNode = _verifyObjectNode(value, ctxt);
            if (_nameTransformer != null) {
                _serializeObjectNodeEntriesWithTransformer(objectNode, gen, ctxt, _nameTransformer);
            } else {
                _serializeObjectNodeEntries(objectNode, gen, ctxt);
            }
            return;
        }
        if (!(value instanceof Map<?,?>)) {
            ctxt.reportBadDefinition(_property.getType(),
                    "Value returned by 'any-getter' (%s()) not java.util.Map but %s".formatted(
                            _accessor.getName(), value.getClass().getName()));
        }
        // 19-Oct-2014, tatu: Should we try to support @JsonInclude options here?
        if (_mapSerializer != null) {
            if (_nameTransformer != null) {
                gen = new NameTransformingGenerator(gen, _nameTransformer);
            }
            _mapSerializer.serializeFilteredAnyProperties(ctxt, gen, bean,(Map<?,?>) value,
                    filter, null);
            return;
        }
        if (_nameTransformer != null) {
            gen = new NameTransformingGenerator(gen, _nameTransformer);
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
     * Helper method for serializing entries of an {@link ObjectNode}
     * with keys transformed by the given {@link NameTransformer}
     * (e.g. when this any-getter is part of a {@code @JsonUnwrapped} bean with prefix/suffix).
     */
    protected void _serializeObjectNodeEntriesWithTransformer(ObjectNode objectNode,
            JsonGenerator gen, SerializationContext ctxt, NameTransformer transformer)
        throws Exception
    {
        for (Map.Entry<String, JsonNode> entry : objectNode.properties()) {
            gen.writeName(transformer.transform(entry.getKey()));
            entry.getValue().serialize(gen, ctxt);
        }
    }

    @Override
    public void depositSchemaProperty(JsonObjectFormatVisitor v, SerializationContext ctxt)
    {
        // no-op
    }
}
