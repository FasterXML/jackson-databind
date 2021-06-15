package com.fasterxml.jackson.databind.util;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.BaseTest;

public class LambdaMetafactoryUtilsTest extends BaseTest {

    public void testSimpleSetter() throws Throwable {
        // given
        ClassWithSetter object = new ClassWithSetter();
        Method method = ClassWithSetter.class.getDeclaredMethod("setString", String.class);
        BiConsumer setterConsumer = LambdaMetafactoryUtils.getBiConsumerObjectSetter(method);

        // when
        setterConsumer.accept(object, "Value");

        // then
        assertEquals("Value", object.getString());
    }

    public void testSetterReturningValue() throws Throwable {
        // given
        ClassWithSetter object = new ClassWithSetter();
        Method method = ClassWithSetter.class.getDeclaredMethod("withString", String.class);
        BiFunction biFunction = LambdaMetafactoryUtils.getBiFunctionObjectSetter(method);

        // when
        Object result = biFunction.apply(object, "Value");

        // then
        assertEquals("Value", result);
        assertEquals("Value", object.getString());
    }

    static class ClassWithSetter {
        private String string;
        private int i;

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public String withString(String string) {
            this.string = string;
            return string;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public int withI(int i) {
            this.i = i;
            return i;
        }
    }
}