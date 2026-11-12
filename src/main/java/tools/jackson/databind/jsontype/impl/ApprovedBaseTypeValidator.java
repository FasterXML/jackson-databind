package tools.jackson.databind.jsontype.impl;

import java.io.Serial;
import java.util.Objects;

import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * {@link PolymorphicTypeValidator} used when the configured validator has approved
 * all subtypes of a polymorphic base type (by returning
 * {@link Validity#ALLOWED} from {@link #validateBaseType}).
 * Like {@link LaissezFaireSubTypeValidator} it allows all subtypes of that base type,
 * but retains the configured validator so that classes named as type parameters
 * of a generic type id (like {@code "java.util.HashMap<a.b.Gadget,java.lang.String>"})
 * can still be validated: approval of the base type does not extend to them.
 *
 * @since 3.1.8
 */
public final class ApprovedBaseTypeValidator
    extends PolymorphicTypeValidator.Base
{
    @Serial
    private static final long serialVersionUID = 1L;

    private final PolymorphicTypeValidator _typeParameterValidator;

    public ApprovedBaseTypeValidator(PolymorphicTypeValidator typeParameterValidator) {
        _typeParameterValidator = Objects.requireNonNull(typeParameterValidator);
    }

    /**
     * Accessor for the configured validator, to use for validating type parameters
     * of generic type ids.
     */
    public PolymorphicTypeValidator getTypeParameterValidator() {
        return _typeParameterValidator;
    }

    @Override
    public Validity validateBaseType(DatabindContext ctxt, JavaType baseType) {
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubClassName(DatabindContext ctxt,
            JavaType baseType, String subClassName) {
        return Validity.ALLOWED;
    }

    @Override
    public Validity validateSubType(DatabindContext ctxt, JavaType baseType,
            JavaType subType) {
        return Validity.ALLOWED;
    }
}
