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
            // 15-Jun-2020, tatu: As a consequence of [databind#2796], need to
            //    AVOID passing bindings for raw, type-erased cases, as otherwise
            //    we seem to get odd "generic Long" cases (for Mr Bean module at least)
            if (type instanceof Class<?>) {
                return _typeFactory.constructType(type);
            }
            return _typeFactory.constructType(type, _bindings);
        }

        /*// debugging
        @Override
        public String toString() {
            return "[TRC.Basic, bindings: "+_bindings+"]";
        }
        */
    }

    /**
     * Dummy implementation for case where there are no bindings available
     * (for example, for static methods and fields)
     *
     * @since 2.11.3
     */
    public static class Empty
        implements TypeResolutionContext
    {
        private final TypeFactory _typeFactory;

        public Empty(TypeFactory tf) {
            _typeFactory = tf;
        }

        @Override
        public JavaType resolveType(Type type) {
            return _typeFactory.constructType(type);
        }
    }
}
