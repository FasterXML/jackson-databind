package com.fasterxml.jackson.databind.type;

import java.io.Serializable;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@SuppressWarnings("serial")
class TypeResolutionTest extends DatabindTestUtil
{
    public static class LongValuedMap<K> extends HashMap<K, Long> { }

    static class GenericList<X> extends ArrayList<X> { }

    static class GenericList2<Y> extends GenericList<Y> { }

    static class LongList extends GenericList2<Long> { }

    static class MyLongList<T> extends LongList { }

    static class Range<E extends Comparable<E>> implements Serializable
    {
         public Range(E start, E end) { }
    }

    static class DoubleRange extends Range<Double> {
        public DoubleRange() { super(null, null); }
        public DoubleRange(Double s, Double e) { super(s, e); }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    void maps()
    {
        TypeFactory tf = defaultTypeFactory();
        JavaType t = tf.constructType(new TypeReference<LongValuedMap<String>>() { });
        MapType type = (MapType) t;
        assertSame(LongValuedMap.class, type.getRawClass());
        assertEquals(tf.constructType(String.class), type.getKeyType());
        assertEquals(tf.constructType(Long.class), type.getContentType());
    }

    @Test
    void listViaTypeRef()
    {
        TypeFactory tf = defaultTypeFactory();
        JavaType t = tf.constructType(new TypeReference<MyLongList<Integer>>() {});
        CollectionType type = (CollectionType) t;
        assertSame(MyLongList.class, type.getRawClass());
        assertEquals(tf.constructType(Long.class), type.getContentType());
    }

    @Test
    void listViaClass()
    {
        TypeFactory tf = defaultTypeFactory();
        JavaType t = tf.constructType(LongList.class);
        JavaType type = (CollectionType) t;
        assertSame(LongList.class, type.getRawClass());
        assertEquals(tf.constructType(Long.class), type.getContentType());
    }

    @Test
    void generic()
    {
        TypeFactory tf = defaultTypeFactory();

        // First, via simple sub-class
        JavaType t = tf.constructType(DoubleRange.class);
        JavaType rangeParams = t.findSuperType(Range.class);
        assertEquals(1, rangeParams.containedTypeCount());
        assertEquals(Double.class, rangeParams.containedType(0).getRawClass());

        // then using TypeRef
        t = tf.constructType(new TypeReference<DoubleRange>() { });
        rangeParams = t.findSuperType(Range.class);
        assertEquals(1, rangeParams.containedTypeCount());
        assertEquals(Double.class, rangeParams.containedType(0).getRawClass());
    }
}
