package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by zoliszel on 22/06/2015.
 */
public class TestTypeIdResolver {

    private ObjectMapper mapper;

    @JsonTypeName(TestClass.NAME)
    public static class TestClass{
            public static final String NAME = "TEST_CLASS";
    }

    @Before
    public void setup(){
        mapper = new ObjectMapper();

        TypeResolverBuilder<?> typerBuilder = new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL);
        typerBuilder = typerBuilder.init(JsonTypeInfo.Id.NAME, null);
        typerBuilder = typerBuilder.inclusion(JsonTypeInfo.As.PROPERTY);
        mapper.setDefaultTyping(typerBuilder);
    }

    @Test
    public void testTypeIdResolutionForClasses(){
        SerializationConfig config= mapper.getSerializationConfig();
        BeanDescription beanDescription = config.introspectClassAnnotations(TestClass.class);
        Assert.assertNotNull("BeanDescription should not be null",beanDescription);

        TypeResolverBuilder<?> typer = config.getDefaultTyper(mapper.constructType(TestClass.class));
        Assert.assertNotNull("Typer should not be null",typer);

        TypeSerializer typeSerializer = typer.buildTypeSerializer(config, mapper.constructType(TestClass.class), Collections.EMPTY_LIST);

        Assert.assertNotNull("TypeSerializer should not be null",typeSerializer);
        Assert.assertNotNull("TypeId resolver should not be null",typeSerializer.getTypeIdResolver() );

        String typeName = typeSerializer.getTypeIdResolver().idFromValueAndType(null, TestClass.class);
        Assert.assertEquals("TypeName was not resolved correctly",TestClass.NAME,typeName);
    }
}
