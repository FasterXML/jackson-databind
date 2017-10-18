package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

// for [databind#1604]
public class TestTypeFactory1604 extends BaseMapTest
{
    static class Data1604<T> { }

    static class DataList1604<T> extends Data1604<List<T>> {
    }

    static class RefinedDataList1604<T> extends DataList1604<List<T>> {
    }

    public static class SneakyDataList1604<BOGUS,T> extends Data1604<List<T>> {
        
    }

    static class TwoParam1604<KEY,VALUE> { }

    static class SneakyTwoParam1604<V,K> extends TwoParam1604<K,List<V>> { }

    public void testCustomTypesRefinedSimple()
    {
        TypeFactory tf = newTypeFactory();
        JavaType type = tf.constructType(new TypeReference<Data1604<Long>>() { });
        assertEquals(Data1604.class, type.getRawClass());

        JavaType subtype = tf.constructSpecializedType(type, DataList1604.class);
        assertEquals(DataList1604.class, subtype.getRawClass());
        assertEquals(1, subtype.containedTypeCount());

        // should properly resolve type parameter:
        JavaType paramType = subtype.containedType(0);
        assertEquals(Long.class, paramType.getRawClass());
    }

    public void testCustomTypesRefinedNested()
    {
        TypeFactory tf = newTypeFactory();
        JavaType type = tf.constructType(new TypeReference<Data1604<Long>>() { });
        assertEquals(Data1604.class, type.getRawClass());

        JavaType subtype = tf.constructSpecializedType(type, RefinedDataList1604.class);
        assertEquals(RefinedDataList1604.class, subtype.getRawClass());
        assertEquals(1, subtype.containedTypeCount());

        // should properly resolve type parameter:
        JavaType paramType = subtype.containedType(0);
        assertEquals(Long.class, paramType.getRawClass());

        // and have correct parent too
        assertEquals(DataList1604.class, subtype.getSuperClass().getRawClass());
    }

    public void testCustomTypesRefinedSneaky()
    {
        TypeFactory tf = newTypeFactory();
        JavaType type = tf.constructType(new TypeReference<Data1604<Long>>() { });
        assertEquals(Data1604.class, type.getRawClass());

        JavaType subtype = tf.constructSpecializedType(type, SneakyDataList1604.class);
        assertEquals(SneakyDataList1604.class, subtype.getRawClass());
        assertEquals(1, subtype.containedTypeCount());

        // should properly resolve type parameter:
        JavaType paramType = subtype.containedType(0);
        assertEquals(Long.class, paramType.getRawClass());

        // and have correct parent too
        assertEquals(Data1604.class, subtype.getSuperClass().getRawClass());
    }

    public void testTwoParamSneakyCustom()
    {
        TypeFactory tf = newTypeFactory();
        JavaType type = tf.constructType(new TypeReference<TwoParam1604<String,Long>>() { });
        assertEquals(TwoParam1604.class, type.getRawClass());
        assertEquals(String.class, type.containedType(0).getRawClass());
        assertEquals(Long.class, type.containedType(1).getRawClass());

        JavaType subtype = tf.constructSpecializedType(type, SneakyTwoParam1604.class);
        assertEquals(SneakyTwoParam1604.class, subtype.getRawClass());
        assertEquals(TwoParam1604.class, subtype.getSuperClass().getRawClass());
        assertEquals(2, subtype.containedTypeCount());

        // should properly resolve type parameters despite sneaky switching
        JavaType first = subtype.containedType(0);
        assertEquals(List.class, first.getRawClass());
        assertEquals(1, first.containedTypeCount());
        assertEquals(Long.class, first.containedType(0).getRawClass());

        JavaType second = subtype.containedType(1);
        assertEquals(String.class, second.getRawClass());
    }
}
