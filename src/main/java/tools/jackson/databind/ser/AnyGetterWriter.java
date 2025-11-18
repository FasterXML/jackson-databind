package tools.jackson.databind.ser;

import java.util.Map;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.ser.jdk.MapSerializer;

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
    public void resolve(SerializationContext provider)
    {
        // 05-Sep-2013, tatu: I _think_ this can be considered a primary property...
        ValueSerializer<?> ser = provider.handlePrimaryContextualization(_serializer, _property);
        _serializer = (ValueSerializer<Object>) ser;
        if (ser instanceof MapSerializer mapSer) {
            _mapSerializer = mapSer;
        }
    }

    public void getAndSerialize(Object bean, JsonGenerator gen, SerializationContext provider)
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
    public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext prov) throws Exception {
        getAndSerialize(bean, gen, prov);
    }

    public void getAndFilter(Object bean, JsonGenerator gen, SerializationContext provider,
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
}
