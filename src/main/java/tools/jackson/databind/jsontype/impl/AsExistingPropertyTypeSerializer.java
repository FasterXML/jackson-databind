package tools.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.jsontype.TypeIdResolver;

/**
 * Type serializer used with {@link As#EXISTING_PROPERTY} inclusion mechanism.
 * Expects type information to be a well-defined property on all sub-classes.
 * Inclusion of type information otherwise follows behavior of {@link As#PROPERTY}.
 */
public class AsExistingPropertyTypeSerializer
    extends TypeSerializerBase
{
    protected final String _typePropertyName;

    public AsExistingPropertyTypeSerializer(TypeIdResolver idRes,
            BeanProperty property, String propName)
    {
        super(idRes, property);
        _typePropertyName = propName;
    }

    @Override
    public AsExistingPropertyTypeSerializer forProperty(SerializerProvider ctxt,
            BeanProperty prop) {
        return (_property == prop) ? this :
            new AsExistingPropertyTypeSerializer(_idResolver, prop, _typePropertyName);
    }

    @Override
    public As getTypeInclusion() { return As.EXISTING_PROPERTY; }
}
