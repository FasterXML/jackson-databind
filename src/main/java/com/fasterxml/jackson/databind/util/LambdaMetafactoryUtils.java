package com.fasterxml.jackson.databind.util;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.util.interfaces.consumer.BooleanSetterConsumer;
import com.fasterxml.jackson.databind.util.interfaces.consumer.ByteSetterConsumer;
import com.fasterxml.jackson.databind.util.interfaces.consumer.CharSetterConsumer;
import com.fasterxml.jackson.databind.util.interfaces.consumer.DoubleSetterConsumer;
import com.fasterxml.jackson.databind.util.interfaces.consumer.FloatSetterConsumer;
import com.fasterxml.jackson.databind.util.interfaces.consumer.IntSetterConsumer;
import com.fasterxml.jackson.databind.util.interfaces.consumer.LongSetterConsumer;
import com.fasterxml.jackson.databind.util.interfaces.consumer.ShortSetterConsumer;
import com.fasterxml.jackson.databind.util.interfaces.function.BooleanSetterFunction;
import com.fasterxml.jackson.databind.util.interfaces.function.ByteSetterFunction;
import com.fasterxml.jackson.databind.util.interfaces.function.CharSetterFunction;
import com.fasterxml.jackson.databind.util.interfaces.function.DoubleSetterFunction;
import com.fasterxml.jackson.databind.util.interfaces.function.FloatSetterFunction;
import com.fasterxml.jackson.databind.util.interfaces.function.IntSetterFunction;
import com.fasterxml.jackson.databind.util.interfaces.function.LongSetterFunction;
import com.fasterxml.jackson.databind.util.interfaces.function.ShortSetterFunction;

public final class LambdaMetafactoryUtils {
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

    public static BiConsumer getBiConsumerObjectSetter(Method method) {
        Class<?> setterParameterType = method.getParameterTypes()[0];
        if (setterParameterType.isPrimitive()) {
            try {
                if (setterParameterType == boolean.class) {
                    final BooleanSetterConsumer primitiveConsumer = getPrimitiveBooleanSetterConsumer(method);
                    return (a, b) -> primitiveConsumer.accept(a, (boolean) b);
                }
                if (setterParameterType == int.class) {
                    final IntSetterConsumer primitiveConsumer = getPrimitiveIntSetterConsumer(method);
                    return (a, b) -> primitiveConsumer.accept(a, (int) b);
                }
                if (setterParameterType == long.class) {
                    final LongSetterConsumer primitiveConsumer = getPrimitiveLongSetterConsumer(method);
                    return (a, b) -> primitiveConsumer.accept(a, (long) b);
                }
                if (setterParameterType == byte.class) {
                    final ByteSetterConsumer primitiveConsumer = getPrimitiveByteSetterConsumer(method);
                    return (a, b) -> primitiveConsumer.accept(a, (byte) b);
                }
                if (setterParameterType == double.class) {
                    final DoubleSetterConsumer primitiveConsumer = getPrimitiveDoubleSetterConsumer(method);
                    return (a, b) -> primitiveConsumer.accept(a, (double) b);
                }
                if (setterParameterType == char.class) {
                    final CharSetterConsumer primitiveConsumer = getPrimitiveCharSetterConsumer(method);
                    return (a, b) -> primitiveConsumer.accept(a, (char) b);
                }
                if (setterParameterType == float.class) {
                    final FloatSetterConsumer primitiveConsumer = getPrimitiveFloatSetterConsumer(method);
                    return (a, b) -> primitiveConsumer.accept(a, (float) b);
                }
                if (setterParameterType == short.class) {
                    final ShortSetterConsumer primitiveConsumer = getPrimitiveShortSetterConsumer(method);
                    return (a, b) -> primitiveConsumer.accept(a, (short) b);
                }
                return null;
            } catch (Throwable throwable) {
                return null;
            }

        } else {
            try {
                return (BiConsumer) getBiConsumerSetter(method, BiConsumer.class);
            } catch (Throwable throwable) {
                return null;
            }
        }
    }

    private static BooleanSetterConsumer getPrimitiveBooleanSetterConsumer(Method _setter) throws Throwable {
        return (BooleanSetterConsumer) getSetter(_setter, BooleanSetterConsumer.class);
    }

    private static IntSetterConsumer getPrimitiveIntSetterConsumer(Method _setter) throws Throwable {
        return (IntSetterConsumer) getSetter(_setter, IntSetterConsumer.class);
    }

    private static LongSetterConsumer getPrimitiveLongSetterConsumer(Method _setter) throws Throwable {
        return (LongSetterConsumer) getSetter(_setter, LongSetterConsumer.class);
    }

    private static ByteSetterConsumer getPrimitiveByteSetterConsumer(Method _setter) throws Throwable {
        return (ByteSetterConsumer) getSetter(_setter, ByteSetterConsumer.class);
    }

    private static DoubleSetterConsumer getPrimitiveDoubleSetterConsumer(Method _setter) throws Throwable {
        return (DoubleSetterConsumer) getSetter(_setter, DoubleSetterConsumer.class);
    }

    private static CharSetterConsumer getPrimitiveCharSetterConsumer(Method _setter) throws Throwable {
        return (CharSetterConsumer) getSetter(_setter, CharSetterConsumer.class);
    }

    private static FloatSetterConsumer getPrimitiveFloatSetterConsumer(Method _setter) throws Throwable {
        return (FloatSetterConsumer) getSetter(_setter, FloatSetterConsumer.class);
    }

    private static ShortSetterConsumer getPrimitiveShortSetterConsumer(Method _setter) throws Throwable {
        return (ShortSetterConsumer) getSetter(_setter, ShortSetterConsumer.class);
    }

    public static BiFunction getBiFunctionObjectSetter(Method method) {
        Class<?> setterParameterType = method.getParameterTypes()[0];
        if (setterParameterType.isPrimitive() && method.getReturnType().isPrimitive()) {
            try {
                if (setterParameterType == boolean.class) {
                    final BooleanSetterFunction primitiveFunction = getPrimitiveBooleanSetterFunction(method);
                    return (a, b) ->primitiveFunction.accept(a, (boolean) b);
                }
                if (setterParameterType == int.class) {
                    final IntSetterFunction primitiveFunction = getPrimitiveIntSetterFunction(method);
                    return (a, b) -> primitiveFunction.accept(a, (int) b);
                }
                if (setterParameterType == long.class) {
                    final LongSetterFunction primitiveFunction = getPrimitiveLongSetterFunction(method);
                    return (a, b) -> primitiveFunction.accept(a, (long) b);
                }
                if (setterParameterType == byte.class) {
                    final ByteSetterFunction primitiveFunction = getPrimitiveByteSetterFunction(method);
                    return (a, b) -> primitiveFunction.accept(a, (byte) b);
                }
                if (setterParameterType == double.class) {
                    final DoubleSetterFunction primitiveFunction = getPrimitiveDoubleSetterFunction(method);
                    return (a, b) -> primitiveFunction.accept(a, (double) b);
                }
                if (setterParameterType == char.class) {
                    final CharSetterFunction primitiveFunction = getPrimitiveCharSetterFunction(method);
                    return (a, b) -> primitiveFunction.accept(a, (char) b);
                }
                if (setterParameterType == float.class) {
                    final FloatSetterFunction primitiveFunction = getPrimitiveFloatSetterFunction(method);
                    return (a, b) -> primitiveFunction.accept(a, (float) b);
                }
                if (setterParameterType == short.class) {
                    final ShortSetterFunction primitiveFunction = getPrimitiveShortSetterFunction(method);
                    return (a, b) -> primitiveFunction.accept(a, (short) b);
                }
                return null;
            } catch (Throwable throwable) {
                return null;
            }
        } else {
            try {
                return (BiFunction) getBiFunctionSetter(method, BiFunction.class);
            } catch (Throwable throwable) {
                return null;
            }
        }
    }

    private static BooleanSetterFunction getPrimitiveBooleanSetterFunction(Method _setter) throws Throwable {
        return (BooleanSetterFunction) getFunction(_setter, BooleanSetterFunction.class);
    }

    private static IntSetterFunction getPrimitiveIntSetterFunction(Method _setter) throws Throwable {
        return (IntSetterFunction) getFunction(_setter, IntSetterFunction.class);
    }

    private static LongSetterFunction getPrimitiveLongSetterFunction(Method _setter) throws Throwable {
        return (LongSetterFunction) getFunction(_setter, LongSetterFunction.class);
    }

    private static ByteSetterFunction getPrimitiveByteSetterFunction(Method _setter) throws Throwable {
        return (ByteSetterFunction) getFunction(_setter, ByteSetterFunction.class);
    }

    private static DoubleSetterFunction getPrimitiveDoubleSetterFunction(Method _setter) throws Throwable {
        return (DoubleSetterFunction) getFunction(_setter, DoubleSetterFunction.class);
    }

    private static CharSetterFunction getPrimitiveCharSetterFunction(Method _setter) throws Throwable {
        return (CharSetterFunction) getFunction(_setter, CharSetterFunction.class);
    }

    private static FloatSetterFunction getPrimitiveFloatSetterFunction(Method _setter) throws Throwable {
        return (FloatSetterFunction) getFunction(_setter, FloatSetterFunction.class);
    }

    private static ShortSetterFunction getPrimitiveShortSetterFunction(Method _setter) throws Throwable {
        return (ShortSetterFunction) getFunction(_setter, ShortSetterFunction.class);
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

    private static Object getFunction(Method method, Class<?> functionKlaz) throws Throwable {
        MethodHandle methodHandle = unreflect(method);

        final String functionName = "apply";
        final Class<?> functionReturn = methodHandle.type().parameterType(1);
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

    private static Object getBiFunctionSetter(Method method, Class<?> functionKlaz) throws Throwable {
        MethodHandle methodHandle = unreflect(method);
        final String functionName = "apply";
        final Class<?> functionReturn = Object.class;
        Class<?> aClass = Object.class;
        return createSetter(method, functionKlaz, methodHandle, functionName, functionReturn, aClass);
    }

}
