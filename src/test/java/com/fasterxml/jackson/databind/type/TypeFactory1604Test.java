package com.fasterxml.jackson.databind.type;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newTypeFactory;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.verifyException;

// for [databind#1604], [databind#2577]
public class TypeFactory1604Test
{
    static class Data1604<T> { }

    static class DataList1604<T> extends Data1604<List<T>> { }

    static class RefinedDataList1604<T> extends DataList1604<T> { }

    public static class SneakyDataList1604<BOGUS,T> extends Data1604<List<T>> { }

    static class TwoParam1604<KEY,VALUE> { }

    static class SneakyTwoParam1604<V,K> extends TwoParam1604<K,List<V>> { }

    // [databind#2577]

    static class Either<L, R> { }

    static class EitherWrapper<L, R> {
        public Either<L, R> value;
    }

    static class Left<V> extends Either<V, Void> { }
    static class Right<V> extends Either<Void, V> { }

    @Test
    public void testCustomTypesRefinedSimple()
    {
        TypeFactory tf = newTypeFactory();
        JavaType base = tf.constructType(new TypeReference<Data1604<List<Long>>>() { });
        assertEquals(Data1604.class, base.getRawClass());
        assertEquals(1, base.containedTypeCount());
        assertEquals(List.class, base.containedType(0).getRawClass());

        JavaType subtype = tf.constructSpecializedType(base, DataList1604.class);
        assertEquals(DataList1604.class, subtype.getRawClass());
        assertEquals(1, subtype.containedTypeCount());
        JavaType paramType = subtype.containedType(0);
        assertEquals(Long.class, paramType.getRawClass());
    }

    @Test
    public void testCustomTypesRefinedNested()
    {
        TypeFactory tf = newTypeFactory();
        JavaType base = tf.constructType(new TypeReference<Data1604<List<Long>>>() { });
        assertEquals(Data1604.class, base.getRawClass());

        JavaType subtype = tf.constructSpecializedType(base, RefinedDataList1604.class);
        assertEquals(RefinedDataList1604.class, subtype.getRawClass());
        assertEquals(DataList1604.class, subtype.getSuperClass().getRawClass());

        assertEquals(1, subtype.containedTypeCount());
        JavaType paramType = subtype.containedType(0);
        assertEquals(Long.class, paramType.getRawClass());
    }

    @Test
    public void testCustomTypesRefinedSneaky()
    {
        TypeFactory tf = newTypeFactory();
        JavaType base = tf.constructType(new TypeReference<Data1604<List<Long>>>() { });
        assertEquals(Data1604.class, base.getRawClass());

        JavaType subtype = tf.constructSpecializedType(base, SneakyDataList1604.class);
        assertEquals(SneakyDataList1604.class, subtype.getRawClass());
        assertEquals(2, subtype.containedTypeCount());
        assertEquals(Long.class, subtype.containedType(1).getRawClass());
        // first one, "bogus", has to be essentially "unknown"
        assertEquals(Object.class, subtype.containedType(0).getRawClass());

        // and have correct parent too
        assertEquals(Data1604.class, subtype.getSuperClass().getRawClass());
    }

    @Test
    public void testTwoParamSneakyCustom()
    {
        TypeFactory tf = newTypeFactory();
        JavaType type = tf.constructType(new TypeReference<TwoParam1604<String,List<Long>>>() { });
        assertEquals(TwoParam1604.class, type.getRawClass());
        assertEquals(String.class, type.containedType(0).getRawClass());
        JavaType ct = type.containedType(1);
        assertEquals(List.class, ct.getRawClass());
        assertEquals(Long.class, ct.getContentType().getRawClass());

        JavaType subtype = tf.constructSpecializedType(type, SneakyTwoParam1604.class);
        assertEquals(SneakyTwoParam1604.class, subtype.getRawClass());
        assertEquals(TwoParam1604.class, subtype.getSuperClass().getRawClass());
        assertEquals(2, subtype.containedTypeCount());

        // should properly resolve type parameters despite sneaky switching, including "unwounding"
        // `List` wrapper
        JavaType first = subtype.containedType(0);
        assertEquals(Long.class, first.getRawClass());
        JavaType second = subtype.containedType(1);
        assertEquals(String.class, second.getRawClass());
    }

    // Also: let's not allow mismatching binding
    @Test
    public void testErrorForMismatch()
    {
        TypeFactory tf = newTypeFactory();
        // NOTE: plain `String` NOT `List<String>`
        JavaType base = tf.constructType(new TypeReference<Data1604<String>>() { });

        try {
            tf.constructSpecializedType(base, DataList1604.class);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Failed to specialize");
            verifyException(e, "Data1604");
            verifyException(e, "DataList1604");
        }
    }

    // [databind#2577]
    @Test
    public void testResolveGenericPartialSubtypes()
    {
        TypeFactory tf = newTypeFactory();
        JavaType base = tf.constructType(new TypeReference<Either<Object, Object>>() { });

        JavaType lefty = tf.constructSpecializedType(base, Left.class);
        assertEquals(Left.class, lefty.getRawClass());
        JavaType[] params = tf.findTypeParameters(lefty, Either.class);
        assertEquals(2, params.length);
        assertEquals(Object.class, params[0].getRawClass());
        assertEquals(Void.class, params[1].getRawClass());

        JavaType righty = tf.constructSpecializedType(base, Right.class);
        assertEquals(Right.class, righty.getRawClass());

        params = tf.findTypeParameters(righty, Either.class);
        assertEquals(2, params.length);
        assertEquals(Void.class, params[0].getRawClass());
        assertEquals(Object.class, params[1].getRawClass());
    }
}
