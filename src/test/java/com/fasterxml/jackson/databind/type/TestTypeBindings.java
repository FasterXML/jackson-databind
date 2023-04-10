package com.fasterxml.jackson.databind.type;

import java.util.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JavaType;

import static org.junit.Assert.assertNotEquals;

/**
 * Simple tests to verify for generic type binding functionality
 * implemented by {@link TypeBindings} class.
 */
public class TestTypeBindings
    extends BaseMapTest
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
    public void testRecursiveType()
    {
        JavaType type = DEFAULT_TF.constructType(HashTree.class);
        assertNotNull(type);
    }

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

    public void testEqualityAndHashCode()
    {
        JavaType stringType = DEFAULT_TF.constructType(String.class);
        TypeBindings listStringBindings = TypeBindings.create(List.class, stringType);
        TypeBindings listStringBindingsWithUnbound = listStringBindings.withUnboundVariable("X");
        TypeBindings iterableStringBindings = TypeBindings.create(Iterable.class, stringType);
        // Ensure that type variable names used by List and Iterable do not change in future java versions
        assertEquals("E", listStringBindings.getBoundName(0));
        assertEquals("T", iterableStringBindings.getBoundName(0));
        // These TypeBindings should differ:
        assertNotEquals(listStringBindings, iterableStringBindings);
        assertNotEquals(listStringBindings.hashCode(), iterableStringBindings.hashCode());
        // Type bindings which differ by an unbound variable still differ:
        assertNotEquals(listStringBindingsWithUnbound, listStringBindings);
        assertNotEquals(listStringBindingsWithUnbound.hashCode(), listStringBindings.hashCode());

        Object iterableStringBaseList = iterableStringBindings.asKey(List.class);
        Object listStringBaseList = listStringBindings.asKey(List.class);
        Object listStringBindingsWithUnboundBaseList = listStringBindingsWithUnbound.asKey(List.class);
        assertNotEquals(iterableStringBaseList, listStringBaseList);
        assertNotEquals(iterableStringBaseList.hashCode(), listStringBaseList.hashCode());
        assertNotEquals(listStringBindingsWithUnboundBaseList, listStringBaseList);
        assertNotEquals(listStringBindingsWithUnboundBaseList.hashCode(), listStringBaseList.hashCode());
    }
}
