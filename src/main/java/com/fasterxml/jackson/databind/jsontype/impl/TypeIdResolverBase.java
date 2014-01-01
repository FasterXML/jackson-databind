package com.fasterxml.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Partial base implementation of {@link TypeIdResolver}: all custom implementations
 * are <b>strongly</b> recommended to extend this class, instead of directly
 * implementing {@link TypeIdResolver}.
 *<p>
 * Note that instances created to be constructed from annotations
 * ({@link com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver})
 * are always created using no-arguments constructor; protected constructor
 * is only used sub-classes.
 */
public abstract class TypeIdResolverBase
    implements TypeIdResolver
{
    protected final TypeFactory _typeFactory;

    /**
     * Common base type for all polymorphic instances handled.
     */
    protected final JavaType _baseType;

    protected TypeIdResolverBase() {
        this(null, null);
    }
    
    protected TypeIdResolverBase(JavaType baseType, TypeFactory typeFactory) {
        _baseType = baseType;
        _typeFactory = typeFactory;
    }

    // Standard type id resolvers do not need this: only useful for custom ones.
    @Override
    public void init(JavaType bt) { }

    @Override
    public String idFromBaseType() {
        /* By default we will just defer to regular handling, handing out the
         * base type; and since there is no value, must just pass null here
         * assuming that implementations can deal with it.
         * Alternative would be to pass a bogus Object, but that does not seem right.
         */
        return idFromValueAndType(null, _baseType.getRawClass());
    }

    /**
     * @deprecated Since 2.3, override {@link #typeFromId(DatabindContext, String)} instead
     *    to get access to contextual information
     */
    @Deprecated
    @Override
    public abstract JavaType typeFromId(String id);

    /**
     * New method, replacement for {@link #typeFromId(String)}, which is given
     * context for accessing information, including configuration and
     * {@link TypeFactory}.
     * 
     * @return Type for given id
     * 
     * @since 2.3
     */
    public JavaType typeFromId(DatabindContext context, String id) {
        return typeFromId(id);
    }
}
