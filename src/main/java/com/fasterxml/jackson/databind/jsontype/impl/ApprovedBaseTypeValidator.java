package com.fasterxml.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * {@link PolymorphicTypeValidator} used when the configured validator has approved
 * all subtypes of a polymorphic base type (by returning
 * {@link Validity#ALLOWED} from {@link #validateBaseType}).
 * Like {@link LaissezFaireSubTypeValidator} it allows all subtypes of that base type,
 * but retains the configured validator so that classes named as type parameters
 * of a generic type id (like {@code "java.util.HashMap<a.b.Gadget,java.lang.String>"})
 * can still be validated: approval of the base type does not extend to them.
 *
 * @since 2.18.12
 */
public final class ApprovedBaseTypeValidator
    extends PolymorphicTypeValidator.Base
{
    private static final long serialVersionUID = 1L;

    private final PolymorphicTypeValidator _typeParameterValidator;

    public ApprovedBaseTypeValidator(PolymorphicTypeValidator typeParameterValidator) {
        _typeParameterValidator = typeParameterValidator;
    }

    /**
     * Accessor for the configured validator, to use for validating type parameters
     * of generic type ids.
     */
    public PolymorphicTypeValidator getTypeParameterValidator() {
        return _typeParameterValidator;
    }

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
