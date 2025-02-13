package com.fasterxml.jackson.databind.ser;

import java.util.Map;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;

/**
 * Class similar to {@link BeanPropertyWriter}, but that will be used
 * for serializing {@link com.fasterxml.jackson.annotation.JsonAnyGetter} annotated
 * (Map) properties
 */
public class AnyGetterWriter extends BeanPropertyWriter
    implements java.io.Serializable // since 2.19
{
    // As of 2.19
    private static final long serialVersionUID = 1L;

    protected final BeanProperty _property;

    /**
     * Method (or Field) that represents the "any getter"
     */
    protected final AnnotatedMember _accessor;

    protected JsonSerializer<Object> _serializer;

    protected MapSerializer _mapSerializer;

    /**
     * @since 2.19
     */
    @SuppressWarnings("unchecked")
    public AnyGetterWriter(BeanPropertyWriter parent, BeanProperty property,
            AnnotatedMember accessor, JsonSerializer<?> serializer)
    {
        super(parent);
        _accessor = accessor;
        _property = property;
        _serializer = (JsonSerializer<Object>) serializer;
        if (serializer instanceof MapSerializer) {
            _mapSerializer = (MapSerializer) serializer;
        }
    }

    /**
     * @deprecated Since 2.19, use one that takes {@link BeanPropertyWriter} instead.
     */
    @Deprecated
    public AnyGetterWriter(BeanProperty property,
            AnnotatedMember accessor, JsonSerializer<?> serializer)
    {
        this(null, property, accessor, serializer);
    }

    /**
     * @since 2.8.3
     */
    @Override
    public void fixAccess(SerializationConfig config) {
        _accessor.fixAccess(
                config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
    }

    public void getAndSerialize(Object bean, JsonGenerator gen, SerializerProvider provider)
        throws Exception
    {
        Object value = _accessor.getValue(bean);
        if (value == null) {
            return;
        }
        if (!(value instanceof Map<?,?>)) {
            provider.reportBadDefinition(_property.getType(), String.format(
                    "Value returned by 'any-getter' %s() not java.util.Map but %s",
                    _accessor.getName(), value.getClass().getName()));
        }
        // 23-Feb-2015, tatu: Nasty, but has to do (for now)
        if (_mapSerializer != null) {
            _mapSerializer.serializeWithoutTypeInfo((Map<?,?>) value, gen, provider);
            return;
        }
        _serializer.serialize(value, gen, provider);
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        getAndSerialize(bean, gen, prov);
    }

    /**
     * @since 2.3
     */
    public void getAndFilter(Object bean, JsonGenerator gen, SerializerProvider provider,
            PropertyFilter filter)
        throws Exception
    {
        Object value = _accessor.getValue(bean);
        if (value == null) {
            return;
        }
        if (!(value instanceof Map<?,?>)) {
            provider.reportBadDefinition(_property.getType(),
                    String.format("Value returned by 'any-getter' (%s()) not java.util.Map but %s",
                    _accessor.getName(), value.getClass().getName()));
        }
        // 19-Oct-2014, tatu: Should we try to support @JsonInclude options here?
        if (_mapSerializer != null) {
            _mapSerializer.serializeFilteredAnyProperties(provider, gen, bean,(Map<?,?>) value,
                    filter, null);
            return;
        }
        // ... not sure how custom handler would do it
        _serializer.serialize(value, gen, provider);
    }

    // Note: NOT part of ResolvableSerializer...
    @SuppressWarnings("unchecked")
    public void resolve(SerializerProvider provider) throws JsonMappingException
    {
        // 05-Sep-2013, tatu: I _think_ this can be considered a primary property...
        if (_serializer instanceof ContextualSerializer) {
            JsonSerializer<?> ser = provider.handlePrimaryContextualization(_serializer, _property);
            _serializer = (JsonSerializer<Object>) ser;
            if (ser instanceof MapSerializer) {
                _mapSerializer = (MapSerializer) ser;
            }
        }
    }
}
