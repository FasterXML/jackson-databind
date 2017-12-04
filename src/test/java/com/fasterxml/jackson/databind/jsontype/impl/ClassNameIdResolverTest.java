package com.fasterxml.jackson.databind.jsontype.impl;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import com.fasterxml.jackson.databind.type.TypeFactory;

import com.fasterxml.jackson.databind.JavaType;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TypeFactory.class)
public class ClassNameIdResolverTest
{
    @Mock
    private JavaType javaType;
    
    @Mock
    private TypeFactory typeFactory;

    private ClassNameIdResolver classNameIdResolver;
    
    @Before
    public void setup(){
        this.classNameIdResolver = new ClassNameIdResolver(javaType, typeFactory);
    }
    
    @Test
    public void testIdFromValue_shouldUseJavaUtilHashMapForSingletonMap(){
        Map<String, String> singletonMap = Collections.singletonMap("ANY_KEY", "ANY_VALUE");
        
        String clazz = classNameIdResolver.idFromValue( singletonMap );
        
        assertEquals(clazz, "java.util.HashMap");
    }
    
    @Test
    public void testIdFromValue_shouldUseJavaUtilHashSetForSingletonSet(){
        Set<String> singletonSet = Collections.singleton("ANY_VALUE");
        
        String clazz = classNameIdResolver.idFromValue( singletonSet );
        
        assertEquals(clazz, "java.util.HashSet");
    }
    
    @Test
    public void testIdFromValue_shouldUseJavaUtilArrayListForSingletonList(){
        List<String> singletonList = Collections.singletonList("ANY_VALUE");
        
        String clazz = classNameIdResolver.idFromValue( singletonList );
        
        assertEquals(clazz, "java.util.ArrayList");
    }
    
    @Test
    public void testIdFromValue_shouldUseJavaUtilArrayListForArrays$List(){
        List<String> utilList = Arrays.asList("ANY_VALUE");
        
        String clazz = classNameIdResolver.idFromValue( utilList );
        
        assertEquals(clazz, "java.util.ArrayList");
    }
}
