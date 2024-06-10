package com.fasterxml.jackson.databind.type;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JavaType;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.verifyException;

/**
 * Simple tests to verify for generic type binding functionality
 * implemented by {@link TypeBindings} class.
 */
public class TypeBindingsTest
{
    static class AbstractType<A,B> { }

    static class LongStringType extends AbstractType<Long,String> { }

    static class InnerGenericTyping<K, V> extends AbstractMap<K, Collection<V>>
    {
        @Override
        public Set<java.util.Map.Entry<K, Collection<V>>> entrySet() {
            return null;
        }
        public class InnerClass extends AbstractMap<K, Collection<V>> {
            @Override
            public Set<java.util.Map.Entry<K, Collection<V>>> entrySet() {
                return null;
            }
        }
    }

    // for [databind#76]
    @SuppressWarnings("serial")
    static class HashTree<K, V> extends HashMap<K, HashTree<K, V>> { }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final TypeFactory DEFAULT_TF = TypeFactory.defaultInstance();

    @Test
    public void testInnerType() throws Exception
    {
        JavaType type = DEFAULT_TF.constructType(InnerGenericTyping.InnerClass.class);
        assertEquals(MapType.class, type.getClass());
        JavaType keyType = type.getKeyType();
        assertEquals(Object.class, keyType.getRawClass());
        JavaType valueType = type.getContentType();
        assertEquals(Collection.class, valueType.getRawClass());
        JavaType vt2 = valueType.getContentType();
        assertEquals(Object.class, vt2.getRawClass());
    }

    // for [databind#76]
    @Test
    public void testRecursiveType()
    {
        JavaType type = DEFAULT_TF.constructType(HashTree.class);
        assertNotNull(type);
    }

    @Test
    public void testBindingsBasics()
    {
        TypeBindings b = TypeBindings.create(Collection.class,
                TypeFactory.unknownType());
        // let's just call it -- should probably try to inspect but...
        assertNotNull(b.toString());
        assertEquals(Object.class, b.getBoundType(0).getRawClass());
        assertNull(b.getBoundName(-1));
        assertNull(b.getBoundType(-1));
        assertNull(b.getBoundName(1));
        assertNull(b.getBoundType(1));

        assertFalse(b.equals("foo"));
    }

    @Test
    public void testInvalidBindings()
    {
        JavaType unknown = TypeFactory.unknownType();
        try {
            TypeBindings.create(AbstractType.class, unknown);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot create TypeBindings");
            verifyException(e, "class expects 2");
        }
    }

    // for [databind#3876]
    @Test
    public void testEqualityAndHashCode()
    {
        JavaType stringType = DEFAULT_TF.constructType(String.class);
        JavaType integerType = DEFAULT_TF.constructType(Integer.class);
        TypeBindings listStringBindings = TypeBindings.create(List.class, stringType);
        TypeBindings listStringBindingsWithUnbound = listStringBindings.withUnboundVariable("X");
        TypeBindings iterableStringBindings = TypeBindings.create(Iterable.class, stringType);
        TypeBindings mapStringInt = TypeBindings.create(Map.class, stringType, integerType);
        TypeBindings mapIntString = TypeBindings.create(Map.class, integerType, stringType);
        // Ensure that type variable names used by List and Iterable do not change in future java versions
        assertEquals("E", listStringBindings.getBoundName(0));
        assertEquals("T", iterableStringBindings.getBoundName(0));
        // These TypeBindings bind the same types in the same order
        assertEquals(listStringBindings, iterableStringBindings);
        assertEquals(listStringBindings.hashCode(), iterableStringBindings.hashCode());
        // Type bindings which differ by an unbound variable still evaluate to equal
        assertEquals(listStringBindingsWithUnbound, listStringBindings);
        assertEquals(listStringBindingsWithUnbound.hashCode(), listStringBindings.hashCode());
        // However type bindings for the same types in different order must differ:
        assertNotEquals(mapStringInt, mapIntString);
        assertNotEquals(mapStringInt.hashCode(), mapIntString.hashCode());

        Object iterableStringBaseList = iterableStringBindings.asKey(List.class);
        Object listStringBaseList = listStringBindings.asKey(List.class);
        Object listStringBindingsWithUnboundBaseList = listStringBindingsWithUnbound.asKey(List.class);
        Object mapStringIntBaseMap = mapStringInt.asKey(Map.class);
        Object mapIntStringBaseMap = mapIntString.asKey(Map.class);
        assertEquals(iterableStringBaseList, listStringBaseList);
        assertEquals(iterableStringBaseList.hashCode(), listStringBaseList.hashCode());
        assertEquals(listStringBindingsWithUnboundBaseList, listStringBaseList);
        assertEquals(listStringBindingsWithUnboundBaseList.hashCode(), listStringBaseList.hashCode());
        assertNotEquals(mapStringIntBaseMap, mapIntStringBaseMap);
        assertNotEquals(mapStringIntBaseMap.hashCode(), mapIntStringBaseMap.hashCode());
    }
}
