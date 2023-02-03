package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Partial base implementation of {@link TypeIdResolver}: all custom implementations
 * are <b>strongly</b> recommended to extend this class, instead of directly
 * implementing {@link TypeIdResolver}.
 * Note that ALL sub-class need to re-implement
 * {@link #typeFromId(DatabindContext, String)} method; otherwise implementation
 * will not work.
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

    @Override
    public JavaType typeFromId(DatabindContext context, String id)  throws IOException {
        // 22-Dec-2015, tatu: Must be overridden by sub-classes, so let's throw
        //    an exception if not
        throw new IllegalStateException("Sub-class "+getClass().getName()+" MUST implement "
                +"`typeFromId(DatabindContext,String)");
    }

    /**
     * Helper method used to get a simple description of all known type ids,
     * for use in error messages.
     */
    @Override
    public String getDescForKnownTypeIds() {
        return null;
    }
}
