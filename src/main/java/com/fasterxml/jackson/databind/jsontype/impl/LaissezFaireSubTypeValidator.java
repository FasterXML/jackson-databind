package com.fasterxml.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Default {@link PolymorphicTypeValidator} used unless explicit one is constructed
 * (and, in 2.11, {@link com.fasterxml.jackson.databind.MapperFeature#BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES}
 * not enabled).
 * Does not do any validation, allows all subtypes. Only used for backwards-compatibility
 * reasons: users should usually NOT use such a permissive implementation but use
 * allow-list/criteria - based implementation.
 *
 * @since 2.10
 */
public final class LaissezFaireSubTypeValidator
    extends PolymorphicTypeValidator.Base
{
    private static final long serialVersionUID = 1L;

    public final static LaissezFaireSubTypeValidator instance = new LaissezFaireSubTypeValidator();

    @Override
    public Validity validateBaseType(MapperConfig<?> ctxt, JavaType baseType) {
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubClassName(MapperConfig<?> ctxt,
            JavaType baseType, String subClassName) {
        return Validity.ALLOWED;
    }

    @Override
    public Validity validateSubType(MapperConfig<?> ctxt, JavaType baseType,
            JavaType subType) {
        return Validity.ALLOWED;
    }
}
