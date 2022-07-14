package tools.jackson.databind.jsontype.impl;

import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Simple {@link PolymorphicTypeValidator} implementation used by {@link StdTypeResolverBuilder}
 * in cases where all subtypes for given base type are deemed acceptable; usually because
 * user controls base type in question (and no serialization gadgets should exist).
 *<p>
 * NOTE: unlike in 2.x, this implementation is NOT available to regular users as its
 * use can easily open up security holes. Only used internally in cases where validation
 * results from regular implementation indicate that no further checks are needed.
 */
final class LaissezFaireSubTypeValidator
    extends PolymorphicTypeValidator.Base
{
    private static final long serialVersionUID = 1L;

    public final static LaissezFaireSubTypeValidator instance = new LaissezFaireSubTypeValidator(); 

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
