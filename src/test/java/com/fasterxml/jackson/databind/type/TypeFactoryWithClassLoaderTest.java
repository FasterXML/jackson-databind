package com.fasterxml.jackson.databind.type;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TypeFactoryWithClassLoaderTest {
  @Mock
  private TypeModifier typeModifier;
  private static ClassLoader classLoader;
  private static ClassLoader threadClassLoader;
  private static String aClassName;
  private ObjectMapper mapper;

  @BeforeAll
  public static void beforeClass() {
	  classLoader = AClass.class.getClassLoader();
	  aClassName = AClass.getStaticClassName();
	  threadClassLoader = Thread.currentThread().getContextClassLoader();
	  assertNotNull(threadClassLoader);
  }

  @BeforeEach
  public void before() {
      mapper = new ObjectMapper();
  }

  @AfterEach
  public void after() {
	Thread.currentThread().setContextClassLoader(threadClassLoader);
	mapper = null;
  }

  @Test
  public void testUsesCorrectClassLoaderWhenThreadClassLoaderIsNull() throws ClassNotFoundException {
	Thread.currentThread().setContextClassLoader(null);
	TypeFactory spySut = spy(mapper.getTypeFactory().withModifier(typeModifier).withClassLoader(classLoader));
	Class<?> clazz = spySut.findClass(aClassName);
	verify(spySut).getClassLoader();
	verify(spySut).classForName(any(String.class), any(Boolean.class), eq(classLoader));
	assertNotNull(clazz);
	assertEquals(classLoader, spySut.getClassLoader());
//	assertEquals(typeModifier,spySut._modifiers[0]);
	assertEquals(null, Thread.currentThread().getContextClassLoader());
  }

  @Test
public void testUsesCorrectClassLoaderWhenThreadClassLoaderIsNotNull() throws ClassNotFoundException {
	TypeFactory spySut = spy(mapper.getTypeFactory().withModifier(typeModifier).withClassLoader(classLoader));
	Class<?> clazz = spySut.findClass(aClassName);
	verify(spySut).getClassLoader();
	verify(spySut).classForName(any(String.class), any(Boolean.class), eq(classLoader));
	assertNotNull(clazz);
	assertEquals(classLoader, spySut.getClassLoader());
//	assertEquals(typeModifier,spySut._modifiers[0]);
}

@Test
public void testCallingOnlyWithModifierGivesExpectedResults(){
	TypeFactory sut = mapper.getTypeFactory().withModifier(typeModifier);
	assertNull(sut.getClassLoader());
//	assertEquals(typeModifier,sut._modifiers[0]);
}

@Test
public void testCallingOnlyWithClassLoaderGivesExpectedResults(){
	TypeFactory sut = mapper.getTypeFactory().withClassLoader(classLoader);
	assertNotNull(sut.getClassLoader());
	assertArrayEquals(null,sut._modifiers);
}

@Test
public void testDefaultTypeFactoryNotAffectedByWithConstructors() {
	TypeFactory sut = mapper.getTypeFactory().withModifier(typeModifier).withClassLoader(classLoader);
	assertEquals(classLoader, sut.getClassLoader());
//	assertEquals(typeModifier,sut._modifiers[0]);
	assertNull(mapper.getTypeFactory().getClassLoader());
	assertArrayEquals(null,mapper.getTypeFactory()._modifiers);
}

@Test
public void testSetsTheCorrectClassLoderIfUsingWithModifierFollowedByWithClassLoader() {
	TypeFactory sut = mapper.getTypeFactory().withModifier(typeModifier).withClassLoader(classLoader);
	assertNotNull(sut.getClassLoader());
}

@Test
public void testSetsTheCorrectClassLoderIfUsingWithClassLoaderFollowedByWithModifier() {
	TypeFactory sut = mapper.getTypeFactory().withClassLoader(classLoader).withModifier(typeModifier);
	assertNotNull(sut.getClassLoader());
}

@Test
public void testThreadContextClassLoaderIsUsedIfNotUsingWithClassLoader() throws ClassNotFoundException {
	TypeFactory spySut = spy(mapper.getTypeFactory());
	assertNull(spySut.getClassLoader());
	Class<?> clazz = spySut.findClass(aClassName);
	assertNotNull(clazz);
	verify(spySut).classForName(any(String.class), any(Boolean.class), eq(threadClassLoader));
}

@Test
public void testUsesFallBackClassLoaderIfNoThreadClassLoaderAndNoWithClassLoader() throws ClassNotFoundException {
	Thread.currentThread().setContextClassLoader(null);
	TypeFactory spySut = spy(mapper.getTypeFactory());
	assertNull(spySut.getClassLoader());
	assertArrayEquals(null,spySut._modifiers);
	Class<?> clazz = spySut.findClass(aClassName);
	assertNotNull(clazz);
	verify(spySut).classForName(any(String.class));
}

    public static class AClass
    {
        private String _foo, _bar;
        protected final static Class<?> thisClass = new Object() {
        }.getClass().getEnclosingClass();

        public AClass() { }
        public AClass(String foo, String bar) {
            _foo = foo;
            _bar = bar;
        }
        public String getFoo() { return _foo; }
        public String getBar() { return _bar; }

        public void setFoo(String foo) { _foo = foo; }
        public void setBar(String bar) { _bar = bar; }
        public static String getStaticClassName() {
            return thisClass.getCanonicalName().replace("."+thisClass.getSimpleName(), "$"+thisClass.getSimpleName());
        }
    }
}
