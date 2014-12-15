package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Failing test related to [Issue#76]
 */
public class TestJavaType76 extends BaseMapTest
{
    @SuppressWarnings("serial")
    static class HashTree<K, V> extends HashMap<K, HashTree<K, V>> { }

    public void testRecursiveType()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType type = tf.constructType(HashTree.class);
        assertNotNull(type);
    }
}
