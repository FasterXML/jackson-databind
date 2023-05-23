package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Specialized {@link SimpleType} for types that are allow iteration
 * over Collection(-like) types: this includes types like
 * {@link java.util.Iterator}.
 * Referenced type is accessible using {@link #getContentType()}.
 *
 * @since 2.16
 */
public abstract class IterationType extends SimpleType
{
    private static final long serialVersionUID = 1L;

    protected final JavaType _iteratedType;

    /**
     * Constructor used when upgrading into this type (via {@link #upgradeFrom},
     * the usual way for {@link IterationType}s to come into existence.
     * Sets up what is considered the "base" reference type
     */
    protected IterationType(TypeBase base, JavaType iteratedType)
    {
        super(base);
        _iteratedType = iteratedType;
    }
}
