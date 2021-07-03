package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AnnotatedMemberEqualityTest extends BaseMapTest
{
    static class SomeBean {
        private String value;

        public SomeBean(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

 // [databind#3187]
    public void testAnnotatedConstructorEquality() {
        DeserializationConfig context = MAPPER.getDeserializationConfig();
        JavaType beanType = MAPPER.constructType(SomeBean.class);

        AnnotatedClass instance1 = AnnotatedClassResolver.resolve(context, beanType, context);
        AnnotatedClass instance2 = AnnotatedClassResolver.resolve(context, beanType, context);

        AnnotatedConstructor constructor1 = instance1.getConstructors().get(0);
        AnnotatedConstructor constructor2 = instance2.getConstructors().get(0);

        assertEquals(instance1, instance2);
        assertEquals(constructor1.getAnnotated(), constructor2.getAnnotated());
        assertEquals(constructor1, constructor2);
        assertEquals(constructor1.getParameter(0), constructor2.getParameter(0));
    }

    // [databind#3187]
    public void testAnnotatedMethodEquality() {
        DeserializationConfig context = MAPPER.getDeserializationConfig();
        JavaType beanType = MAPPER.constructType(SomeBean.class);

        AnnotatedClass instance1 = AnnotatedClassResolver.resolve(context, beanType, context);
        AnnotatedClass instance2 = AnnotatedClassResolver.resolve(context, beanType, context);

        String methodName = "setValue";
        Class<?>[] paramTypes = {String.class};
        AnnotatedMethod method1 = instance1.findMethod(methodName, paramTypes);
        AnnotatedMethod method2 = instance2.findMethod(methodName, paramTypes);

        assertEquals(instance1, instance2);
        assertEquals(method1.getAnnotated(), method2.getAnnotated());
        assertEquals(method1, method2);
        assertEquals(method1.getParameter(0), method2.getParameter(0));
    }

    // [databind#3187]
    public void testAnnotatedFieldEquality() {
        DeserializationConfig context = MAPPER.getDeserializationConfig();
        JavaType beanType = MAPPER.constructType(SomeBean.class);

        AnnotatedClass instance1 = AnnotatedClassResolver.resolve(context, beanType, context);
        AnnotatedClass instance2 = AnnotatedClassResolver.resolve(context, beanType, context);

        AnnotatedField field1 = instance1.fields().iterator().next();
        AnnotatedField field2 = instance2.fields().iterator().next();

        assertEquals(instance1, instance2);
        assertEquals(field1.getAnnotated(), field2.getAnnotated());
        assertEquals(field1, field2);
    }
}
