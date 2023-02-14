package com.fasterxml.jackson.databind.type;

import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JavaType;

// [databind#3108]: canonical type description for non-generic subtypes
@SuppressWarnings("serial")
public class TestTypeFactory3108
    extends BaseMapTest
{
    static class StringList3108 extends ArrayList<String> {}

    static class StringStringMap3108 extends HashMap<String, String> {}

    static class ParamType3108<T> {}

    static class ConcreteType3108 extends ParamType3108<Integer> {}

    // [databind#3108] with custom Collection
    public void testCanonicalWithCustomCollection()
    {
        final TypeFactory tf = TypeFactory.defaultInstance();
        JavaType stringListType = tf.constructType(StringList3108.class);
        String canonical = stringListType.toCanonical();
        JavaType type = tf.constructFromCanonical(canonical);
        assertEquals(StringList3108.class, type.getRawClass());
        assertTrue(type.isCollectionLikeType());
    }

    // [databind#3108] with custom Map
    public void testCanonicalWithCustomMap()
    {
        final TypeFactory tf = TypeFactory.defaultInstance();
        JavaType stringListType = tf.constructType(StringStringMap3108.class);
        String canonical = stringListType.toCanonical();
        JavaType type = tf.constructFromCanonical(canonical);
        assertEquals(StringStringMap3108.class, type.getRawClass());
        assertTrue(type.isMapLikeType());
    }

    // [databind#3108] with custom generic type
    public void testCanonicalWithCustomGenericType()
    {
        final TypeFactory tf = TypeFactory.defaultInstance();
        JavaType stringListType = tf.constructType(ConcreteType3108.class);
        String canonical = stringListType.toCanonical();
        JavaType type = tf.constructFromCanonical(canonical);
        assertEquals(ConcreteType3108.class, type.getRawClass());
    }
}
