package com.fasterxml.jackson.databind.type;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JavaType;

/**
 * Simple tests to verify that {@link JavaType} types work to
 * some degree
 */
public class TestJavaType
    extends BaseMapTest
{
    static class BaseType { }

    static class SubType extends BaseType { }
    
    static enum MyEnum { A, B; }
    static enum MyEnum2 {
        A(1), B(2);

        private MyEnum2(int value) { }
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
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
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

    public void testSimpleClass()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType baseType = tf.constructType(BaseType.class);
        assertSame(BaseType.class, baseType.getRawClass());
        assertTrue(baseType.hasRawClass(BaseType.class));

        assertFalse(baseType.isArrayType());
        assertFalse(baseType.isContainerType());
        assertFalse(baseType.isEnumType());
        assertFalse(baseType.isInterface());
        assertFalse(baseType.isPrimitive());
        assertFalse(baseType.isReferenceType());
        assertFalse(baseType.hasContentType());

        assertNull(baseType.getContentType());
        assertNull(baseType.getKeyType());
        assertNull(baseType.getValueHandler());
    }

    public void testArrayType()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType arrayT = ArrayType.construct(tf.constructType(String.class), null);
        assertNotNull(arrayT);
        assertTrue(arrayT.isContainerType());
        assertFalse(arrayT.isReferenceType());
        assertTrue(arrayT.hasContentType());

        assertNotNull(arrayT.toString());
        assertNotNull(arrayT.getContentType());
        assertNull(arrayT.getKeyType());

        assertTrue(arrayT.equals(arrayT));
        assertFalse(arrayT.equals(null));
        assertFalse(arrayT.equals("xyz"));

        assertTrue(arrayT.equals(ArrayType.construct(tf.constructType(String.class), null)));
        assertFalse(arrayT.equals(ArrayType.construct(tf.constructType(Integer.class), null)));
    }

    public void testMapType()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType mapT = tf.constructType(HashMap.class);
        assertTrue(mapT.isContainerType());
        assertFalse(mapT.isReferenceType());
        assertTrue(mapT.hasContentType());

        assertNotNull(mapT.toString());
        assertNotNull(mapT.getContentType());
        assertNotNull(mapT.getKeyType());

        assertTrue(mapT.equals(mapT));
        assertFalse(mapT.equals(null));
        assertFalse(mapT.equals("xyz"));
    }
    
    public void testEnumType()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        assertTrue(tf.constructType(MyEnum.class).isEnumType());
        assertTrue(tf.constructType(MyEnum2.class).isEnumType());
        assertTrue(tf.constructType(MyEnum.A.getClass()).isEnumType());
        assertTrue(tf.constructType(MyEnum2.A.getClass()).isEnumType());
    }

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
    public void testJavaTypeAsJLRType()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType t1 = tf.constructType(getClass());
        // should just get it back as-is:
        JavaType t2 = tf.constructType(t1);
        assertSame(t1, t2);
    }

    // [databind#1194]
    public void testGenericSignature1194() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        Method m;
        JavaType t;

        m = Generic1194.class.getMethod("getList");
        t  = tf.constructType(m.getGenericReturnType());
        assertEquals("Ljava/util/List<Ljava/lang/String;>;", t.getGenericSignature());

        m = Generic1194.class.getMethod("getMap");
        t  = tf.constructType(m.getGenericReturnType());
        assertEquals("Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;",
                t.getGenericSignature());

        m = Generic1194.class.getMethod("getGeneric");
        t  = tf.constructType(m.getGenericReturnType());
        assertEquals("Ljava/util/concurrent/atomic/AtomicReference<Ljava/lang/String;>;", t.getGenericSignature());
    }

    public void testAnchorTypeForRefTypes() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType t  = tf.constructType(AtomicStringReference.class);
        assertTrue(t.isReferenceType());
        assertTrue(t.hasContentType());
        ReferenceType rt = (ReferenceType) t;
        assertFalse(rt.isAnchorType());
        assertEquals(AtomicReference.class, rt.getAnchorType().getRawClass());
    }

    // for [databind#1290]
    public void testObjectToReferenceSpecialization() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType base = tf.constructType(Object.class);
        assertTrue(base.isJavaLangObject());

        JavaType sub = tf.constructSpecializedType(base, AtomicReference.class);
        assertEquals(AtomicReference.class, sub.getRawClass());
        assertTrue(sub.isReferenceType());
    }
}
