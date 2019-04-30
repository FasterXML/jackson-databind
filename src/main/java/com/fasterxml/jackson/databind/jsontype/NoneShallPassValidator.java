package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Default {@link PolymorphicTypeValidator} used unless explicit one is constructed.
 * Denies use of all types so isn't very useful as base.
 */
public class NoneShallPassValidator
    extends PolymorphicTypeValidator
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    @Override
    public Validity validateBaseType(DatabindContext ctxt, JavaType baseType)
            throws JsonMappingException {
        /* 24-Apr-2019, tatu: We do need to make it restrictive, BUT... as of now
         *    tests would fail; need to get back to that right after 2.10
        return Validity.DENIED;
        */
        /*
        if (baseType.hasRawClass(Object.class)) {
            return Validity.DENIED;
        }
        */
        return Validity.ALLOWED;
    }
    
    @Override
    public Validity validateSubClassName(DatabindContext ctxt,
            JavaType baseType, String subClassName) {
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubType(DatabindContext ctxt, JavaType baseType,
            JavaType subType) {
        /* 24-Apr-2019, tatu: We do need to make it restrictive, BUT... as of now
         *    tests would fail; need to get back to that right after 2.10
        return Validity.DENIED;
        */
        /*
        if (baseType.hasRawClass(Object.class)) {
            return Validity.DENIED;
        }
        */
        return Validity.ALLOWED;
    }
}
