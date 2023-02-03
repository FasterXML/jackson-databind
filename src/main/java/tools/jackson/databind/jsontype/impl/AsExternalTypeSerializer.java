package tools.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import tools.jackson.core.*;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.jsontype.TypeIdResolver;

/**
 * Type serializer that preferably embeds type information as an "external"
 * type property; embedded in enclosing JSON object.
 * Note that this serializer should only be used when value is being output
 * at JSON Object context; otherwise it cannot work reliably, and will have
 * to revert operation similar to {@link AsPropertyTypeSerializer}.
 *<p>
 * Note that implementation of serialization is bit cumbersome as we must
 * serialized external type id AFTER object; this because callback only
 * occurs after field name has been written.
 *<p>
 * Also note that this type of type id inclusion will NOT try to make use
 * of native Type Ids, even if those exist.
 */
public class AsExternalTypeSerializer extends TypeSerializerBase
{
    protected final String _typePropertyName;

    public AsExternalTypeSerializer(TypeIdResolver idRes, BeanProperty property, String propName) {
        super(idRes, property);
        _typePropertyName = propName;
    }

    @Override
    public AsExternalTypeSerializer forProperty(SerializerProvider ctxt,
            BeanProperty prop) {
        return (_property == prop) ? this : new AsExternalTypeSerializer(_idResolver, prop, _typePropertyName);
    }

    @Override
    public String getPropertyName() { return _typePropertyName; }

    @Override
    public As getTypeInclusion() { return As.EXTERNAL_PROPERTY; }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    // nothing to wrap it with:
    protected final void _writeScalarPrefix(Object value, JsonGenerator g) throws JacksonException { }

    protected final void _writeObjectPrefix(Object value, JsonGenerator g) throws JacksonException {
        g.writeStartObject();
    }

    protected final void _writeArrayPrefix(Object value, JsonGenerator g) throws JacksonException {
        g.writeStartArray();
    }

    protected final void _writeScalarSuffix(Object value, JsonGenerator g, String typeId) throws JacksonException {
        if (typeId != null) {
            g.writeStringProperty(_typePropertyName, typeId);
        }
    }

    protected final void _writeObjectSuffix(Object value, JsonGenerator g, String typeId) throws JacksonException {
        g.writeEndObject();
        if (typeId != null) {
            g.writeStringProperty(_typePropertyName, typeId);
        }
    }

    protected final void _writeArraySuffix(Object value, JsonGenerator g, String typeId) throws JacksonException {
        g.writeEndArray();
        if (typeId != null) {
            g.writeStringProperty(_typePropertyName, typeId);
        }
    }
}
