package com.fasterxml.jackson.databind.type;

import java.util.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JavaType;

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

    public void testInnerType() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType type = tf.constructType(InnerGenericTyping.InnerClass.class);
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
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType type = tf.constructType(HashTree.class);
        assertNotNull(type);
    }
}
