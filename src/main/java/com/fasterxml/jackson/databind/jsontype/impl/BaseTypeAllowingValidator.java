package com.fasterxml.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Validator used after a configured validator approves all subtypes of one
 * base type. Validation for any other base type is delegated to the original
 * validator so canonical generic arguments do not inherit that approval.
 *<p>
 * Error messages should name the configured validator, available through
 * {@link #getDelegate()}, since this wrapper is not what the user set up.
 *
 * @since 2.18.12
 */
public final class BaseTypeAllowingValidator extends PolymorphicTypeValidator.Base
{
    private static final long serialVersionUID = 1L;

    private final JavaType _allowedBaseType;
    private final PolymorphicTypeValidator _delegate;

    BaseTypeAllowingValidator(JavaType allowedBaseType,
            PolymorphicTypeValidator delegate) {
        _allowedBaseType = allowedBaseType;
        _delegate = delegate;
    }

    /**
     * @return The configured validator this one delegates to for every base
     *    type but the approved one
     */
    public PolymorphicTypeValidator getDelegate() {
        return _delegate;
    }

    @Override
    public Validity validateBaseType(MapperConfig<?> config, JavaType baseType) {
        return _delegate.validateBaseType(config, baseType);
    }

    @Override
    public Validity validateSubClassName(MapperConfig<?> config,
            JavaType baseType, String subClassName) throws JsonMappingException {
        if (_allowedBaseType.equals(baseType)) {
            return Validity.ALLOWED;
        }
        return _delegate.validateSubClassName(config, baseType, subClassName);
    }

    @Override
    public Validity validateSubType(MapperConfig<?> config, JavaType baseType,
            JavaType subType) throws JsonMappingException {
        if (_allowedBaseType.equals(baseType)) {
            return Validity.ALLOWED;
        }
        return _delegate.validateSubType(config, baseType, subType);
    }
}
