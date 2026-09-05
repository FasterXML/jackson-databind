package tools.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class AnnotatedMemberEqualityTest extends DatabindTestUtil
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

    static class NoArgBean {
        public NoArgBean() { }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

 // [databind#3187]
    @Test
    public void testAnnotatedConstructorEquality() {
        DeserializationConfig context = MAPPER.deserializationConfig();
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

    @Test
    public void testAnnotatedConstructorRawParameterTypeIsCached() {
        DeserializationConfig context = MAPPER.deserializationConfig();
        JavaType beanType = MAPPER.constructType(SomeBean.class);

        AnnotatedClass instance = AnnotatedClassResolver.resolve(context, beanType, context);
        AnnotatedConstructor constructor = instance.getConstructors().get(0);

        assertNull(constructor._paramClasses);
        assertEquals(String.class, constructor.getRawParameterType(0));
        Class<?>[] paramTypes = constructor._paramClasses;
        assertNull(constructor.getRawParameterType(1));
        assertSame(paramTypes, constructor._paramClasses);
    }

    // [databind#6187]
    @Test
    public void annotatedConstructorDoesNotEagerlyConstructInvokers() throws Exception {
        DeserializationConfig context = MAPPER.deserializationConfig();
        JavaType beanType = MAPPER.constructType(SomeBean.class);

        AnnotatedClass instance = AnnotatedClassResolver.resolve(context, beanType, context);
        AnnotatedConstructor constructor = instance.getConstructors().get(0);

        assertNull(invokerField(constructor, "_invokerNullary"));
        assertNull(invokerField(constructor, "_invokerUnary"));
        assertNull(invokerField(constructor, "_invokerFixedArity"));
    }

    // [databind#6187]
    @Test
    public void annotatedConstructorBuildsOnlyTheInvokerItUses() throws Exception {
        DeserializationConfig context = MAPPER.deserializationConfig();
        JavaType beanType = MAPPER.constructType(SomeBean.class);

        AnnotatedClass instance = AnnotatedClassResolver.resolve(context, beanType, context);
        AnnotatedConstructor constructor = instance.getConstructors().get(0);

        SomeBean created = (SomeBean) constructor.call(new Object[] { "x" });
        assertEquals("x", created.getValue());
        assertNull(invokerField(constructor, "_invokerNullary"));
        assertNull(invokerField(constructor, "_invokerUnary"));
        assertNotNull(invokerField(constructor, "_invokerFixedArity"));

        SomeBean viaCall1 = (SomeBean) constructor.call1("y");
        assertEquals("y", viaCall1.getValue());
        assertNull(invokerField(constructor, "_invokerNullary"));
        assertNotNull(invokerField(constructor, "_invokerUnary"));
        assertNotNull(invokerField(constructor, "_invokerFixedArity"));
    }

    // [databind#6187]
    @Test
    public void annotatedConstructorNullaryCallBuildsOnlyNullaryInvoker() throws Exception {
        DeserializationConfig context = MAPPER.deserializationConfig();
        JavaType beanType = MAPPER.constructType(NoArgBean.class);

        AnnotatedClass instance = AnnotatedClassResolver.resolve(context, beanType, context);
        AnnotatedConstructor constructor = instance.getDefaultConstructor();
        assertNotNull(constructor);

        assertNull(invokerField(constructor, "_invokerNullary"));
        Object created = constructor.call();
        assertEquals(NoArgBean.class, created.getClass());
        assertNotNull(invokerField(constructor, "_invokerNullary"));
        assertNull(invokerField(constructor, "_invokerUnary"));
        assertNull(invokerField(constructor, "_invokerFixedArity"));
    }

    private static Object invokerField(AnnotatedConstructor constructor, String name) throws Exception {
        java.lang.reflect.Field field = AnnotatedConstructor.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(constructor);
    }

    // [databind#3187]
    @Test
    public void testAnnotatedMethodEquality() {
        DeserializationConfig context = MAPPER.deserializationConfig();
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
    @Test
    public void testAnnotatedFieldEquality() {
        DeserializationConfig context = MAPPER.deserializationConfig();
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
