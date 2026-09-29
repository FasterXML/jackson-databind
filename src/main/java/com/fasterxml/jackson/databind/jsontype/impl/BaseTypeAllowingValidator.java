package com.fasterxml.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Validator used after a configured validator approves all subtypes of one
 * base type. Validation for any other base type is delegated to the original
 * validator so canonical generic arguments do not inherit that approval.
 */
final class BaseTypeAllowingValidator extends PolymorphicTypeValidator.Base
{
    private static final long serialVersionUID = 1L;

    private final JavaType _allowedBaseType;
    private final PolymorphicTypeValidator _delegate;

    BaseTypeAllowingValidator(JavaType allowedBaseType,
            PolymorphicTypeValidator delegate) {
        _allowedBaseType = allowedBaseType;
        _delegate = delegate;
    }

    @Override
    public Validity validateBaseType(MapperConfig<?> config, JavaType baseType) {
        return _delegate.validateBaseType(config, baseType);
    }

    @Override
    public Validity validateSubClassName(MapperConfig<?> config,
            JavaType baseType, String subClassName) throws JsonMappingException {
        if (baseType == _allowedBaseType) {
            return Validity.ALLOWED;
        }
        return _delegate.validateSubClassName(config, baseType, subClassName);
    }

    @Override
    public Validity validateSubType(MapperConfig<?> config, JavaType baseType,
            JavaType subType) throws JsonMappingException {
        if (baseType == _allowedBaseType) {
            return Validity.ALLOWED;
        }
        return _delegate.validateSubType(config, baseType, subType);
    }
}
