package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;

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
    public Validity validateSubClassName(MapperConfig<?> ctxt,
            JavaType baseType, String subClassName) {
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubType(MapperConfig<?> ctxt, JavaType baseType,
            JavaType subType) {
        return Validity.DENIED;
    }
}
