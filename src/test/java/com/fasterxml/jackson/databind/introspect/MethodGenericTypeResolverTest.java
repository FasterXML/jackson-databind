package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MethodGenericTypeResolverTest extends BaseMapTest {

    private static final TypeResolutionContext EMPTY_CONTEXT =
            new TypeResolutionContext.Empty(TypeFactory.defaultInstance());

    public static <T> AtomicReference<T> simple(T input) {
        throw new UnsupportedOperationException();
    }

    public static AtomicReference<?> noGenerics(String input) {
        throw new UnsupportedOperationException();
    }

    public static <T> Map<T, T> mapWithSameKeysAndValues(List<T> input) {
        throw new UnsupportedOperationException();
    }

    public static <T> Map<?, ?> disconnected(List<T> input) {
        throw new UnsupportedOperationException();
    }

    public static <A, B> Map<A, B> multipleTypeVariables(Map<A, B> input) {
        throw new UnsupportedOperationException();
    }

    public static <A, B> Map<? extends A, ? extends B> multipleTypeVariablesWithUpperBound(Map<A, B> input) {
        throw new UnsupportedOperationException();
    }

    public static class StubA {
        private final String value;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        StubA(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class StubB extends StubA {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public StubB(String value) {
            super(value);
        }
    }

    public void testWithoutGenerics() {
        TypeBindings bindings = MethodGenericTypeResolver.bindMethodTypeParameters(
                method("noGenerics"), type(String.class), EMPTY_CONTEXT);
        assertNull(bindings);
    }

    public void testWithoutGenericsInResult() {
        TypeBindings bindings = MethodGenericTypeResolver.bindMethodTypeParameters(
                method("simple"), type(AtomicReference.class), EMPTY_CONTEXT);
        assertNull(bindings);
    }

    public void testResultDoesNotUseTypeVariables() {
        TypeBindings bindings = MethodGenericTypeResolver.bindMethodTypeParameters(
                method("disconnected"), type(new TypeReference<Map<String, String>>() {
                }), EMPTY_CONTEXT);
        assertNull(bindings);
    }

    public void testWithoutGenericsInMethod() {
        TypeBindings bindings = MethodGenericTypeResolver.bindMethodTypeParameters(
                method("noGenerics"), type(new TypeReference<Map<String, String>>() {
                }), EMPTY_CONTEXT);
        assertNull(bindings);
    }

    public void testWithRepeatedGenericInReturn() {
        TypeBindings bindings = MethodGenericTypeResolver.bindMethodTypeParameters(
                method("mapWithSameKeysAndValues"), type(new TypeReference<Map<String, String>>() {
                }), EMPTY_CONTEXT);
        assertEquals(asMap("T", type(String.class)), asMap(bindings));
    }

    public void testWithRepeatedGenericInReturnWithIncreasingSpecificity() {
        Method method = method("mapWithSameKeysAndValues");
        TypeBindings bindingsAb = MethodGenericTypeResolver.bindMethodTypeParameters(
                method, type(new TypeReference<Map<StubA, StubB>>() {
                }), EMPTY_CONTEXT);
        TypeBindings bindingsBa = MethodGenericTypeResolver.bindMethodTypeParameters(
                method, type(new TypeReference<Map<StubB, StubA>>() {
                }), EMPTY_CONTEXT);
        assertEquals(asMap(bindingsBa), asMap(bindingsAb));
        assertEquals(asMap(bindingsBa), asMap("T", type(StubB.class)));
    }

    public void testMultipleTypeVariables() {
        TypeBindings bindings = MethodGenericTypeResolver.bindMethodTypeParameters(
                method("multipleTypeVariables"), type(new TypeReference<Map<Integer, Long>>() {
                }), EMPTY_CONTEXT);
        assertEquals(
                asMap("A", type(Integer.class), "B", type(Long.class)),
                asMap(bindings));
    }

    public void testMultipleTypeVariablesWithUpperBounds() {
        TypeBindings bindings = MethodGenericTypeResolver.bindMethodTypeParameters(
                method("multipleTypeVariablesWithUpperBound"), type(new TypeReference<Map<Integer, Long>>() {
                }), EMPTY_CONTEXT);
        assertEquals(
                asMap("A", type(Integer.class), "B", type(Long.class)),
                asMap(bindings));
    }

    public void testResultTypeDoesNotExactlyMatch() {
        TypeBindings bindings = MethodGenericTypeResolver.bindMethodTypeParameters(
                method("multipleTypeVariables"), type(new TypeReference<HashMap<Integer, Long>>() {
                }), EMPTY_CONTEXT);
        // Mapping the result to a common supertype is not supported.
        assertNull(bindings);
    }

    private static Method method(String name) {
        Method result = null;
        for (Method method : MethodGenericTypeResolverTest.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && name.equals(method.getName())) {
                if (result != null) {
                    throw new AssertionError("Multiple methods discovered with name "
                            + name + ": " + result + " and " + method);
                }
                result = method;
            }
        }
        assertNotNull("Failed to find method", result);
        return result;
    }

    private static JavaType type(TypeReference<?> reference) {
        return type(reference.getType());
    }

    private static JavaType type(Type type) {
        return EMPTY_CONTEXT.resolveType(type);
    }

    private static Map<String, JavaType> asMap(TypeBindings bindings) {
        assertNotNull(bindings);
        Map<String, JavaType> result = new HashMap<>(bindings.size());
        for (int i = 0; i < bindings.size(); i++) {
            result.put(bindings.getBoundName(i), bindings.getBoundType(i));
        }
        assertEquals(bindings.size(), result.size());
        return result;
    }

    private static Map<String, JavaType> asMap(String name, JavaType javaType) {
        return Collections.singletonMap(name, javaType);
    }

    private static Map<String, JavaType> asMap(
            String name0, JavaType javaType0, String name1, JavaType javaType1) {
        Map<String, JavaType> result = new HashMap<>(2);
        result.put(name0, javaType0);
        result.put(name1, javaType1);
        return result;
    }
}