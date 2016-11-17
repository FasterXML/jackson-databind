package com.fasterxml.jackson.failing;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static org.junit.Assert.assertEquals;

public class GenericParameterTypeFactory1456Test {
    public static class BaseController<Entity extends BaseEntity> {
        public void process(Entity entity) {}
    }

    public static class ImplController extends BaseController<ImplEntity> {}

    public static class BaseEntity {}

    public static class ImplEntity extends BaseEntity {}

    @Test
    public void testGenericParameterHierarchy() throws NoSuchMethodException {
        Method proceed = BaseController.class.getMethod("process", BaseEntity.class);
        Type entityType = proceed.getGenericParameterTypes()[0];

        JavaType resolvedType = new ObjectMapper().getTypeFactory().constructType(entityType, ImplController.class);
        assertEquals(ImplEntity.class, resolvedType.getRawClass());
    }
}
