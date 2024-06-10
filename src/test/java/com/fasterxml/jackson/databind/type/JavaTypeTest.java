package com.fasterxml.jackson.databind.type;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple tests to verify that {@link JavaType} types work to
 * some degree
 */
public class JavaTypeTest
{
    static class BaseType { }

    static class SubType extends BaseType { }

    static enum MyEnum { A, B; }
    static enum MyEnum2 {
        A(1), B(2);

        private MyEnum2(int value) { }
    }

    static enum MyEnumSub {
        A(1) {
            @Override public String toString() {
                return "a";
            }
        },
        B(2) {
            @Override public String toString() {
                return "b";
            }
        }
        ;

        private MyEnumSub(int value) { }
    }

    // [databind#728]
    static class Issue728 {
        public <C extends CharSequence> C method(C input) { return null; }
    }

    public interface Generic1194 {
        public AtomicReference<String> getGeneric();
        public List<String> getList();
        public Map<String,String> getMap();
    }

    @SuppressWarnings("serial")
    static class AtomicStringReference extends AtomicReference<String> { }

    static interface StringStream extends Stream<String> { }
    static interface StringIterator extends Iterator<String> { }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    public void testLocalType728() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        Method m = Issue728.class.getMethod("method", CharSequence.class);
        assertNotNull(m);

        // Start with return type
        // first type-erased
        JavaType t = tf.constructType(m.getReturnType());
        assertEquals(CharSequence.class, t.getRawClass());
        // then generic
        t = tf.constructType(m.getGenericReturnType());
        assertEquals(CharSequence.class, t.getRawClass());

        // then parameter type
        t = tf.constructType(m.getParameterTypes()[0]);
        assertEquals(CharSequence.class, t.getRawClass());
        t = tf.constructType(m.getGenericParameterTypes()[0]);
        assertEquals(CharSequence.class, t.getRawClass());
    }

    @Test
    public void testSimpleClass()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType baseType = tf.constructType(BaseType.class);
        assertSame(BaseType.class, baseType.getRawClass());
        assertTrue(baseType.hasRawClass(BaseType.class));
        assertFalse(baseType.isTypeOrSubTypeOf(SubType.class));

        assertFalse(baseType.isArrayType());
        assertFalse(baseType.isContainerType());
        assertFalse(baseType.isEnumType());
        assertFalse(baseType.isInterface());
        assertFalse(baseType.isIterationType());
        assertFalse(baseType.isPrimitive());
        assertFalse(baseType.isReferenceType());
        assertFalse(baseType.hasContentType());

        assertNull(baseType.getContentType());
        assertNull(baseType.getKeyType());
        assertNull(baseType.getValueHandler());

        assertEquals("Lcom/fasterxml/jackson/databind/type/JavaTypeTest$BaseType;", baseType.getGenericSignature());
        assertEquals("Lcom/fasterxml/jackson/databind/type/JavaTypeTest$BaseType;", baseType.getErasedSignature());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDeprecated()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType baseType = tf.constructType(BaseType.class);
        assertTrue(baseType.hasRawClass(BaseType.class));
        assertNull(baseType.getParameterSource());
        assertNull(baseType.getContentTypeHandler());
        assertNull(baseType.getContentValueHandler());
        assertFalse(baseType.hasValueHandler());
        assertFalse(baseType.hasHandlers());
    }

    @Test
    public void testArrayType()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType arrayT = ArrayType.construct(tf.constructType(String.class), null);
        assertNotNull(arrayT);
        assertTrue(arrayT.isContainerType());
        assertFalse(arrayT.isIterationType());
        assertFalse(arrayT.isReferenceType());
        assertTrue(arrayT.hasContentType());

        assertNotNull(arrayT.toString());
        assertNotNull(arrayT.getContentType());
        assertNull(arrayT.getKeyType());

        assertTrue(arrayT.equals(arrayT));
        assertFalse(arrayT.equals(null));
        final Object bogus = "xyz";
        assertFalse(arrayT.equals(bogus));

        assertTrue(arrayT.equals(ArrayType.construct(tf.constructType(String.class), null)));
        assertFalse(arrayT.equals(ArrayType.construct(tf.constructType(Integer.class), null)));
    }

    @Test
    public void testMapType()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType mapT = tf.constructType(HashMap.class);
        assertTrue(mapT.isContainerType());
        assertFalse(mapT.isIterationType());
        assertFalse(mapT.isReferenceType());
        assertTrue(mapT.hasContentType());

        assertNotNull(mapT.toString());
        assertNotNull(mapT.getContentType());
        assertNotNull(mapT.getKeyType());

        assertEquals("Ljava/util/HashMap<Ljava/lang/Object;Ljava/lang/Object;>;", mapT.getGenericSignature());
        assertEquals("Ljava/util/HashMap;", mapT.getErasedSignature());

        assertTrue(mapT.equals(mapT));
        assertFalse(mapT.equals(null));
        Object bogus = "xyz";
        assertFalse(mapT.equals(bogus));
    }

    @Test
    public void testEnumType()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType enumT = tf.constructType(MyEnum.class);
        // JDK actually works fine with "basic" Enum types...
        assertTrue(enumT.getRawClass().isEnum());
        assertTrue(enumT.isEnumType());
        assertTrue(enumT.isEnumImplType());
        assertFalse(enumT.isIterationType());

        assertFalse(enumT.hasHandlers());
        assertTrue(enumT.isTypeOrSubTypeOf(MyEnum.class));
        assertTrue(enumT.isTypeOrSubTypeOf(Object.class));
        assertNull(enumT.containedType(3));
        assertTrue(enumT.containedTypeOrUnknown(3).isJavaLangObject());

        assertEquals("Lcom/fasterxml/jackson/databind/type/JavaTypeTest$MyEnum;", enumT.getGenericSignature());
        assertEquals("Lcom/fasterxml/jackson/databind/type/JavaTypeTest$MyEnum;", enumT.getErasedSignature());

        assertTrue(tf.constructType(MyEnum2.class).isEnumType());
        assertTrue(tf.constructType(MyEnum.A.getClass()).isEnumType());
        assertTrue(tf.constructType(MyEnum2.A.getClass()).isEnumType());

        // [databind#2480]
        assertFalse(tf.constructType(Enum.class).isEnumImplType());
        JavaType enumSubT = tf.constructType(MyEnumSub.B.getClass());
        assertTrue(enumSubT.isEnumType());
        assertTrue(enumSubT.isEnumImplType());

        // and this is kind of odd twist by JDK: one might except this to return true,
        // but no, sub-classes (when Enum values have overrides, and require sub-class)
        // are NOT considered enums for whatever reason
        assertFalse(enumSubT.getRawClass().isEnum());
    }

    @SuppressWarnings("SelfComparison")
    @Test
    public void testClassKey()
    {
        ClassKey key = new ClassKey(String.class);
        assertEquals(0, key.compareTo(key));
        assertTrue(key.equals(key));
        assertFalse(key.equals(null));
        assertFalse(key.equals("foo"));
        assertFalse(key.equals(new ClassKey(Integer.class)));
        assertEquals(String.class.getName(), key.toString());
    }

    // [databind#116]
    @Test
    public void testJavaTypeAsJLRType()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType t1 = tf.constructType(getClass());
        // should just get it back as-is:
        JavaType t2 = tf.constructType(t1);
        assertSame(t1, t2);
    }

    // [databind#1194]
    @Test
    public void testGenericSignature1194() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        Method m;
        JavaType t;

        m = Generic1194.class.getMethod("getList");
        t  = tf.constructType(m.getGenericReturnType());
        assertEquals("Ljava/util/List<Ljava/lang/String;>;", t.getGenericSignature());
        assertEquals("Ljava/util/List;", t.getErasedSignature());

        m = Generic1194.class.getMethod("getMap");
        t  = tf.constructType(m.getGenericReturnType());
        assertEquals("Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;",
                t.getGenericSignature());

        m = Generic1194.class.getMethod("getGeneric");
        t  = tf.constructType(m.getGenericReturnType());
        assertEquals("Ljava/util/concurrent/atomic/AtomicReference<Ljava/lang/String;>;", t.getGenericSignature());
    }

    @Deprecated
    @Test
    public void testAnchorTypeForRefTypes() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType t  = tf.constructType(AtomicStringReference.class);
        assertTrue(t.isReferenceType());
        assertTrue(t.hasContentType());
        JavaType ct = t.getContentType();
        assertEquals(String.class, ct.getRawClass());
        assertSame(ct, t.containedType(0));
        ReferenceType rt = (ReferenceType) t;
        assertFalse(rt.isAnchorType());
        assertEquals(AtomicReference.class, rt.getAnchorType().getRawClass());
    }

    // for [databind#1290]
    @Test
    public void testObjectToReferenceSpecialization() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType base = tf.constructType(Object.class);
        assertTrue(base.isJavaLangObject());

        JavaType sub = tf.constructSpecializedType(base, AtomicReference.class);
        assertEquals(AtomicReference.class, sub.getRawClass());
        assertTrue(sub.isReferenceType());
    }

    // for [databind#2091]
    @Test
    public void testConstructReferenceType() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        // do AtomicReference<Long>
        final JavaType refdType = tf.constructType(Long.class);
        JavaType t  = tf.constructReferenceType(AtomicReference.class, refdType);
        assertTrue(t.isReferenceType());
        assertTrue(t.hasContentType());
        assertEquals(Long.class, t.getContentType().getRawClass());

        // 26-Mar-2020, tatu: [databind#2019] made this work
        assertEquals(1, t.containedTypeCount());
        TypeBindings bindings = t.getBindings();
        assertEquals(1, bindings.size());
        assertEquals(refdType, bindings.getBoundType(0));
        // Should we even verify this or not?
        assertEquals("V", bindings.getBoundName(0));
    }

    // for [databind#3950]: resolve `Iterator`, `Stream`
    @Test
    public void testIterationTypesDirect() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();

        // First, type-erased types
        _verifyIteratorType(tf.constructType(Iterator.class),
                Iterator.class, Object.class);
        _verifyIteratorType(tf.constructType(Stream.class),
                Stream.class, Object.class);

        // Then generic but direct
        JavaType t = _verifyIteratorType(tf.constructType(new TypeReference<Iterator<String>>() { }),
                Iterator.class, String.class);
        assertEquals("java.util.Iterator<java.lang.String>", t.toCanonical());
        assertEquals("Ljava/util/Iterator;", t.getErasedSignature());
        assertEquals("Ljava/util/Iterator<Ljava/lang/String;>;", t.getGenericSignature());
        _verifyIteratorType(tf.constructType(new TypeReference<Stream<Long>>() { }),
                Stream.class, Long.class);

        // Then primitive stream:
        _verifyIteratorType(tf.constructType(DoubleStream.class),
                DoubleStream.class, Double.TYPE);
        _verifyIteratorType(tf.constructType(IntStream.class),
                IntStream.class, Integer.TYPE);
        _verifyIteratorType(tf.constructType(LongStream.class),
                LongStream.class, Long.TYPE);
    }

    // for [databind#3950]: resolve `Iterator`, `Stream`
    @Test
    public void testIterationTypesFromValues() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        List<String> strings = Arrays.asList("foo", "bar");
        // We will get type-erased, alas, so:
        Iterator<String> stringIT = strings.iterator();
        _verifyIteratorType(tf.constructType(stringIT.getClass()),
                stringIT.getClass(), Object.class);
        Stream<String> stringStream = strings.stream();
        _verifyIteratorType(tf.constructType(stringStream.getClass()),
                stringStream.getClass(), Object.class);
    }

    // for [databind#3950]: resolve `Iterator`, `Stream`
    @Test
    public void testIterationSubTypes() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        _verifyIteratorType(tf.constructType(StringIterator.class),
                StringIterator.class, String.class);
        _verifyIteratorType(tf.constructType(StringStream.class),
                StringStream.class, String.class);
    }

    private JavaType _verifyIteratorType(JavaType type,
            Class<?> expType, Class<?> expContentType) {
        assertTrue(type.isIterationType());
        assertEquals(IterationType.class, type.getClass());
        assertEquals(expType, type.getRawClass());
        assertEquals(expContentType, type.getContentType().getRawClass());
        return type;
    }
}
