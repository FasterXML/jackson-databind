package tools.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.jsontype.TypeIdResolver;

/**
 * Type serializer that will embed type information in an array,
 * as the first element, and actual value as the second element.
 */
public class AsArrayTypeSerializer extends TypeSerializerBase
{
    public AsArrayTypeSerializer(TypeIdResolver idRes, BeanProperty property) {
        super(idRes, property);
    }

    @Override
    public AsArrayTypeSerializer forProperty(SerializerProvider ctxt,
            BeanProperty prop) {
        return (_property == prop) ? this : new AsArrayTypeSerializer(_idResolver, prop);
    }

    @Override
    public As getTypeInclusion() { return As.WRAPPER_ARRAY; }
}
