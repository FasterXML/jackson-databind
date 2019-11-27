package com.fasterxml.jackson.databind.util;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;

public final class LambdaMetafactoryUtils {
    public static Object getPrimitiveConsumer(Method _setter) {
        Class<?> setterParameterType = _setter.getParameterTypes()[0];
        if (setterParameterType.isPrimitive()) {
            try {
                if (setterParameterType == int.class) {
                    return getIntSetter(_setter);
                }
                if (setterParameterType == long.class)
                    return getLongSetter(_setter);
            } catch (Throwable throwable) {
                return null;
            }
        }
        return null;
    }

    private static MethodHandle unreflect(Method method) {
        try {
            final MethodHandles.Lookup caller = getLookup(method);

            return caller.unreflect(method);
        } catch (Throwable e) {
            return null;
        }
    }

    private static MethodHandles.Lookup getLookup(Method method) throws NoSuchFieldException, IllegalAccessException {
        // Define black magic.
        final MethodHandles.Lookup original = MethodHandles.lookup();
        final Field internal = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        internal.setAccessible(true);
        final MethodHandles.Lookup trusted = (MethodHandles.Lookup) internal.get(original);

        // Invoke black magic.
        return trusted.in(method.getDeclaringClass());
    }

    private static ObjIntConsumer getIntSetter(Method method) throws Throwable {
        final Class<?> functionKlaz = ObjIntConsumer.class;
        Object o = getSetter(method, functionKlaz);
        return (ObjIntConsumer) o;
    }

    private static ObjLongConsumer getLongSetter(Method method) throws Throwable {
        final Class<?> functionKlaz = ObjLongConsumer.class;
        Object o = getSetter(method, functionKlaz);
        return (ObjLongConsumer) o;
    }

    public static BiConsumer getBiConsumerObjectSetter(Method method) throws Throwable {
        final Class<?> functionKlaz = BiConsumer.class;
        Object o = getBiConsumerSetter(method, functionKlaz);
        return (BiConsumer) o;
    }

    private static Object getSetter(Method method, Class<?> functionKlaz) throws Throwable {
        MethodHandle methodHandle = unreflect(method);

        final String functionName = "accept";
        final Class<?> functionReturn = void.class;
        Class<?> aClass = !methodHandle.type().parameterType(1).isPrimitive()
                ? Object.class
                : methodHandle.type().parameterType(1);
        return createSetter(method, functionKlaz, methodHandle, functionName, functionReturn, aClass);
    }

    private static Object createSetter(Method method, Class<?> functionKlaz, MethodHandle methodHandle,
                                       String functionName, Class<?> functionReturn, Class<?> aClass) throws Throwable {
        final Class<?>[] functionParams = new Class<?>[] { Object.class,
                aClass};

        final MethodType factoryMethodType = MethodType
                .methodType(functionKlaz);
        final MethodType functionMethodType = MethodType.methodType(
                functionReturn, functionParams);

        final CallSite setterFactory = LambdaMetafactory.metafactory( //
                getLookup(method), // Represents a lookup context.
                functionName, // The name of the method to implement.
                factoryMethodType, // Signature of the factory method.
                functionMethodType, // Signature of function implementation.
                methodHandle, // Function method implementation.
                methodHandle.type() // Function method type signature.
        );

        final MethodHandle setterInvoker = setterFactory.getTarget();
        return setterInvoker.invoke();
    }

    private static Object getBiConsumerSetter(Method method, Class<?> functionKlaz) throws Throwable {
        MethodHandle methodHandle = unreflect(method);
        final String functionName = "accept";
        final Class<?> functionReturn = void.class;
        Class<?> aClass = Object.class;
        return createSetter(method, functionKlaz, methodHandle, functionName, functionReturn, aClass);
    }

}
