package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Default {@link PolymorphicTypeValidator} used unless explicit one is constructed.
 * Denies use of most types so isn't very useful as base, as it only allows:
 * <ul>
 *  <li>Enums
 *   </li>
 *  </ul>
 */
public class NoneShallPassValidator
    extends PolymorphicTypeValidator
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    @Override
    public Validity validateBaseType(DatabindContext ctxt, JavaType baseType)
            throws JsonMappingException
    {
        // Actually let's not block any particular base type, just mark as 
        // indeterminate to only base on actual subtype
        
        return Validity.INDETERMINATE;
    }
    
    @Override
    public Validity validateSubClassName(DatabindContext ctxt,
            JavaType baseType, String subClassName) {
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubType(DatabindContext ctxt, JavaType baseType,
            JavaType subType)
    {
        // Very small set of allowed types:
        //
        // 1. Enums
        // 2. Primitives, their wrappers
        
        if (subType.isEnumType()) {
            return Validity.ALLOWED;
        }
        if (ClassUtil.primitiveType(subType.getRawClass()) != null) {
            return Validity.ALLOWED;
        }

        // But aside from that actually only allow if base type not too generic
        final Class<?> rawBase = baseType.getRawClass();
        if ((rawBase == Object.class)) {
            return Validity.DENIED;
        }

        return Validity.ALLOWED;
    }
}
