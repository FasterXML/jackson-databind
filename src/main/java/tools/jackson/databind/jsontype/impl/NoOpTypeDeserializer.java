package tools.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeIdResolver;

/**
 * Special {@link TypeDeserializer} implementation used to explicitly
 * block type deserialization. This is used when a property or class
 * is annotated with {@code @JsonTypeInfo(use = Id.NONE)}, indicating
 * that type information should not be expected or processed even if
 * the value type has a class-level type info annotation.
 *<p>
 * Unlike returning {@code null} (which means "no special type handling,
 * use defaults"), this actively prevents type information from being read.
 *
 * @since 3.1
 */
public class NoOpTypeDeserializer extends TypeDeserializer
{
    private final JavaType _baseType;
    private final ValueDeserializer<Object> _deserializer;

    public NoOpTypeDeserializer(JavaType baseType, ValueDeserializer<Object> deser) {
        _baseType = baseType;
        _deserializer = deser;
    }

    public NoOpTypeDeserializer withDeserializer(ValueDeserializer<Object> deser) {
        if (_deserializer == deser) {
            return this;
        }
        return new NoOpTypeDeserializer(_baseType, deser);
    }

    @Override
    public TypeDeserializer forProperty(BeanProperty prop) {
        return this;
    }

    @Override
    public JsonTypeInfo.As getTypeInclusion() {
        // No proper value but need to return something
        return JsonTypeInfo.As.EXISTING_PROPERTY;
    }

    @Override
    public String getPropertyName() {
        return null;
    }

    @Override
    public TypeIdResolver getTypeIdResolver() {
        return null;
    }

    @Override
    public Class<?> getDefaultImpl() {
        return null;
    }

    @Override
    public Object deserializeTypedFromObject(JsonParser p,
            DeserializationContext ctxt) throws JacksonException
    {
        // Just deserialize without type info
        return _deserialize(p, ctxt);
    }

    @Override
    public Object deserializeTypedFromArray(JsonParser p,
            DeserializationContext ctxt) throws JacksonException
    {
        // Just deserialize without type info
        return _deserialize(p, ctxt);
    }

    @Override
    public Object deserializeTypedFromScalar(JsonParser p,
            DeserializationContext ctxt) throws JacksonException
    {
        // Just deserialize without type info
        return _deserialize(p, ctxt);
    }

    @Override
    public Object deserializeTypedFromAny(JsonParser p,
            DeserializationContext ctxt) throws JacksonException
    {
        // Just deserialize without type info
        return _deserialize(p, ctxt);
    }

    protected Object _deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        ValueDeserializer<Object> deser = _deserializer;

        // Find deserializer for the base type (this will find custom deserializers
        // registered for this type, including those from @JsonDeserialize annotations)
        if (deser == null) {
            deser = ctxt.findContextualValueDeserializer(_baseType, null);
            if (deser == null) {
                ctxt.reportBadDefinition(_baseType,
                        "Cannot find deserializer for type " + _baseType);
            }
        }
        return deser.deserialize(p, ctxt);
    }
}
