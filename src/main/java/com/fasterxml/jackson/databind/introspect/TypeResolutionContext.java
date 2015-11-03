package com.fasterxml.jackson.databind.introspect;

import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Interface that defines API used by members (like {@link AnnotatedMethod})
 * to dynamically resolve types they have.
 *
 * @since 2.7
 */
public interface TypeResolutionContext {
    public JavaType resolveType(Type t);

    public static class Basic
        implements TypeResolutionContext
    {
        private final TypeFactory _typeFactory;
        private final TypeBindings _bindings;

        public Basic(TypeFactory tf, TypeBindings b) {
            _typeFactory = tf;
            _bindings = b;
        }

        @Override
        public JavaType resolveType(Type type) {
            return _typeFactory.constructType(type, _bindings);
        }
    }
}
