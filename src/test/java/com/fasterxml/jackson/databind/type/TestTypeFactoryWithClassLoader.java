package com.fasterxml.jackson.databind.type;

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TypeFactory.class)

public class TestTypeFactoryWithClassLoader {
  @Mock
  private TypeModifier typeModifier;
  private static ClassLoader classLoader;
  private static ClassLoader threadClassLoader;
  private static String aClassName;
  private ObjectMapper mapper;

  @BeforeClass
  public static void beforeClass() {
	  classLoader = AClass.class.getClassLoader();
	  aClassName = AClass.getStaticClassName();
	  threadClassLoader = Thread.currentThread().getContextClassLoader();
	  Assert.assertNotNull(threadClassLoader);
  }

  @Before
  public void before() {
      mapper = new ObjectMapper();
  }

  @After
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
	Assert.assertNotNull(clazz);
	Assert.assertEquals(classLoader, spySut.getClassLoader());
	Assert.assertEquals(typeModifier,spySut._modifiers[0]);
	Assert.assertEquals(null, Thread.currentThread().getContextClassLoader());
  }

  @Test
public void testUsesCorrectClassLoaderWhenThreadClassLoaderIsNotNull() throws ClassNotFoundException {
	TypeFactory spySut = spy(mapper.getTypeFactory().withModifier(typeModifier).withClassLoader(classLoader));
	Class<?> clazz = spySut.findClass(aClassName);
	verify(spySut).getClassLoader();
	verify(spySut).classForName(any(String.class), any(Boolean.class), eq(classLoader));
	Assert.assertNotNull(clazz);
	Assert.assertEquals(classLoader, spySut.getClassLoader());
	Assert.assertEquals(typeModifier,spySut._modifiers[0]);
}

@Test
public void testCallingOnlyWithModifierGivesExpectedResults(){
	TypeFactory sut = mapper.getTypeFactory().withModifier(typeModifier);
	Assert.assertNull(sut.getClassLoader());
	Assert.assertEquals(typeModifier,sut._modifiers[0]);
}

@Test
public void testCallingOnlyWithClassLoaderGivesExpectedResults(){
	TypeFactory sut = mapper.getTypeFactory().withClassLoader(classLoader);
	Assert.assertNotNull(sut.getClassLoader());
	Assert.assertArrayEquals(null,sut._modifiers);
}

@Test
public void testDefaultTypeFactoryNotAffectedByWithConstructors() {
	TypeFactory sut = mapper.getTypeFactory().withModifier(typeModifier).withClassLoader(classLoader);
	Assert.assertEquals(classLoader, sut.getClassLoader());
	Assert.assertEquals(typeModifier,sut._modifiers[0]);
	Assert.assertNull(mapper.getTypeFactory().getClassLoader());
	Assert.assertArrayEquals(null,mapper.getTypeFactory()._modifiers);
}

@Test
public void testSetsTheCorrectClassLoderIfUsingWithModifierFollowedByWithClassLoader() {
	TypeFactory sut = mapper.getTypeFactory().withModifier(typeModifier).withClassLoader(classLoader);
	Assert.assertNotNull(sut.getClassLoader());
}

@Test
public void testSetsTheCorrectClassLoderIfUsingWithClassLoaderFollowedByWithModifier() {
	TypeFactory sut = mapper.getTypeFactory().withClassLoader(classLoader).withModifier(typeModifier);
	Assert.assertNotNull(sut.getClassLoader());
}

@Test
public void testThreadContextClassLoaderIsUsedIfNotUsingWithClassLoader() throws ClassNotFoundException {
	TypeFactory spySut = spy(mapper.getTypeFactory());
	Assert.assertNull(spySut.getClassLoader());
	Class<?> clazz = spySut.findClass(aClassName);
	Assert.assertNotNull(clazz);
	verify(spySut).classForName(any(String.class), any(Boolean.class), eq(threadClassLoader));
}

@Test
public void testUsesFallBackClassLoaderIfNoThreadClassLoaderAndNoWithClassLoader() throws ClassNotFoundException {
	Thread.currentThread().setContextClassLoader(null);
	TypeFactory spySut = spy(mapper.getTypeFactory());
	Assert.assertNull(spySut.getClassLoader());
	Assert.assertArrayEquals(null,spySut._modifiers);
	Class<?> clazz = spySut.findClass(aClassName);
	Assert.assertNotNull(clazz);
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
