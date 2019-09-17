package com.fasterxml.jackson.databind.util;

import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

public class ClassUtilTest extends BaseMapTest
{
    /*
    /**********************************************************
    /* Test classes, enums
    /**********************************************************
     */

    /* Test classes and interfaces needed for testing class util
     * methods
     */
    static abstract class BaseClass implements Comparable<BaseClass>,
        BaseInt
    {
        BaseClass(String str) { }
    }

    interface BaseInt { }

    interface SubInt extends BaseInt { }

    enum TestEnum { A; }

    abstract class InnerNonStatic { }

    static class Inner {
        protected Inner() {
            throw new IllegalStateException("test");
        }
    }

    static abstract class SubClass
        extends BaseClass
        implements SubInt {
        SubClass() { super("x"); }
    }

    static abstract class ConcreteAndAbstract {
        public abstract void a();
        
        public void c() { }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testIsConcrete() throws Exception
    {
        assertTrue(ClassUtil.isConcrete(getClass()));
        assertFalse(ClassUtil.isConcrete(BaseClass.class));
        assertFalse(ClassUtil.isConcrete(BaseInt.class));

        assertFalse(ClassUtil.isConcrete(ConcreteAndAbstract.class.getDeclaredMethod("a")));
        assertTrue(ClassUtil.isConcrete(ConcreteAndAbstract.class.getDeclaredMethod("c")));
    }

    public void testCanBeABeanType()
    {
        assertEquals("annotation", ClassUtil.canBeABeanType(java.lang.annotation.Retention.class));
        assertEquals("array", ClassUtil.canBeABeanType(String[].class));
        assertEquals("enum", ClassUtil.canBeABeanType(TestEnum.class));
        assertEquals("primitive", ClassUtil.canBeABeanType(Integer.TYPE));
        assertNull(ClassUtil.canBeABeanType(Integer.class));

        assertEquals("non-static member class", ClassUtil.isLocalType(InnerNonStatic.class, false));
        assertNull(ClassUtil.isLocalType(Integer.class, false));
    }

    public void testExceptionHelpers()
    {
        RuntimeException e = new RuntimeException("test");
        RuntimeException wrapper = new RuntimeException(e);

        assertSame(e, ClassUtil.getRootCause(wrapper));

        try {
            ClassUtil.throwAsIAE(e);
            fail("Shouldn't get this far");
        } catch (RuntimeException e2) {
            assertSame(e, e2);
        }

        Error err = new Error();
        try {
            ClassUtil.throwAsIAE(err);
            fail("Shouldn't get this far");
        } catch (Error errAct) {
            assertSame(err, errAct);
        }
        
        try {
            ClassUtil.unwrapAndThrowAsIAE(wrapper);
            fail("Shouldn't get this far");
        } catch (RuntimeException e2) {
            assertSame(e, e2);
        }
    }

    public void testFailedCreateInstance()
    {
        try {
            ClassUtil.createInstance(BaseClass.class, true);
        } catch (IllegalArgumentException e) {
            verifyException(e, "has no default");
        }

        try {
            // false means ctor would need to be public
            ClassUtil.createInstance(Inner.class, false);
        } catch (IllegalArgumentException e) {
            verifyException(e, "is not accessible");
        }

        // and finally, check that we'll get expected exception...
        try {
            ClassUtil.createInstance(Inner.class, true);
        } catch (IllegalStateException e) {
            verifyException(e, "test");
        }
    }

    public void testPrimitiveDefaultValue()
    {
        assertEquals(Integer.valueOf(0), ClassUtil.defaultValue(Integer.TYPE));
        assertEquals(Long.valueOf(0L), ClassUtil.defaultValue(Long.TYPE));
        assertEquals(Character.valueOf('\0'), ClassUtil.defaultValue(Character.TYPE));
        assertEquals(Short.valueOf((short) 0), ClassUtil.defaultValue(Short.TYPE));
        assertEquals(Byte.valueOf((byte) 0), ClassUtil.defaultValue(Byte.TYPE));

        assertEquals(Double.valueOf(0.0), ClassUtil.defaultValue(Double.TYPE));
        assertEquals(Float.valueOf(0.0f), ClassUtil.defaultValue(Float.TYPE));

        assertEquals(Boolean.FALSE, ClassUtil.defaultValue(Boolean.TYPE));
        
        try {
            ClassUtil.defaultValue(String.class);
        } catch (IllegalArgumentException e) {
            verifyException(e, "String is not a primitive type");
        }
    }

    public void testPrimitiveWrapperType()
    {
        assertEquals(Byte.class, ClassUtil.wrapperType(Byte.TYPE));
        assertEquals(Short.class, ClassUtil.wrapperType(Short.TYPE));
        assertEquals(Character.class, ClassUtil.wrapperType(Character.TYPE));
        assertEquals(Integer.class, ClassUtil.wrapperType(Integer.TYPE));
        assertEquals(Long.class, ClassUtil.wrapperType(Long.TYPE));

        assertEquals(Double.class, ClassUtil.wrapperType(Double.TYPE));
        assertEquals(Float.class, ClassUtil.wrapperType(Float.TYPE));

        assertEquals(Boolean.class, ClassUtil.wrapperType(Boolean.TYPE));
        
        try {
            ClassUtil.wrapperType(String.class);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "String is not a primitive type");
        }
    }

    public void testWrapperToPrimitiveType()
    {
        assertEquals(Integer.TYPE, ClassUtil.primitiveType(Integer.class));
        assertEquals(Long.TYPE, ClassUtil.primitiveType(Long.class));
        assertEquals(Character.TYPE, ClassUtil.primitiveType(Character.class));
        assertEquals(Short.TYPE, ClassUtil.primitiveType(Short.class));
        assertEquals(Byte.TYPE, ClassUtil.primitiveType(Byte.class));
        assertEquals(Float.TYPE, ClassUtil.primitiveType(Float.class));
        assertEquals(Double.TYPE, ClassUtil.primitiveType(Double.class));
        assertEquals(Boolean.TYPE, ClassUtil.primitiveType(Boolean.class));
        
        assertNull(ClassUtil.primitiveType(String.class));
    }

    public void testFindEnumType()
    {
        assertEquals(TestEnum.class, ClassUtil.findEnumType(TestEnum.A));
        // different codepaths for empty and non-empty EnumSets...
        assertEquals(TestEnum.class, ClassUtil.findEnumType(EnumSet.allOf(TestEnum.class)));
        assertEquals(TestEnum.class, ClassUtil.findEnumType(EnumSet.noneOf(TestEnum.class)));

        assertEquals(TestEnum.class, ClassUtil.findEnumType(new EnumMap<TestEnum,Integer>(TestEnum.class)));
    }

    public void testDescs()
    {
        final String stringExp = "`java.lang.String`";
        assertEquals(stringExp, ClassUtil.getClassDescription("foo"));
        assertEquals(stringExp, ClassUtil.getClassDescription(String.class));
        final JavaType stringType = TypeFactory.defaultInstance().constructType(String.class);
        assertEquals(stringExp, ClassUtil.getTypeDescription(stringType));
        final JavaType mapType = TypeFactory.defaultInstance().constructType(
                new TypeReference<Map<String, Integer>>() { });
        assertEquals("`java.util.Map<java.lang.String,java.lang.Integer>`",
                ClassUtil.getTypeDescription(mapType));
    }
}
