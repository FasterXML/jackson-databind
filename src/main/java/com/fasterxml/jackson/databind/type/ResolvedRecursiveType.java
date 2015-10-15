package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Internal placeholder type used for self-references.
 *
 * @since 2.7
 */
public class ResolvedRecursiveType extends SimpleType
{
    private static final long serialVersionUID = 1L;

    protected JavaType _referencedType;

    public ResolvedRecursiveType(Class<?> erasedType) {
        super(erasedType);
    }

    public void setReference(JavaType ref)
    {
        // sanity check; should not be called multiple times
        if (_referencedType != null) {
            throw new IllegalStateException("Trying to re-set self reference; old value = "+_referencedType+", new = "+ref);
        }
        _referencedType = ref;
    }

    public JavaType getSelfReferencedType() { return _referencedType; }
}
