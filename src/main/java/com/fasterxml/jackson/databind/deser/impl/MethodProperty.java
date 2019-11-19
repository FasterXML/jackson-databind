package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.*;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.Annotations;

// https://github.com/FasterXML/jackson-databind/issues/2083
// TODO:
// - add LambdaMetafactoryUtils
// - add support of set and return
// - add support for primitives
// - tests of performance
// - create pull request


/**
 * This concrete sub-class implements property that is set
 * using regular "setter" method.
 */
public final class MethodProperty
    extends SettableBeanProperty
{
    private static final long serialVersionUID = 1;

    protected final AnnotatedMethod _annotated;
    
    /**
     * Setter method for modifying property value; used for
     * "regular" method-accessible properties.
     */
    protected final transient Method _setter;
    protected final transient BiConsumer consumer;
    protected final transient Object primitiveConsumer;

    /**
     * @since 2.9
     */
    final protected boolean _skipNulls;
    
    public MethodProperty(BeanPropertyDefinition propDef,
            JavaType type, TypeDeserializer typeDeser,
            Annotations contextAnnotations, AnnotatedMethod method)
    {
        super(propDef, type, typeDeser, contextAnnotations);
        _annotated = method;
        _setter = method.getAnnotated();
        primitiveConsumer = getPrimitiveConsumer();
        consumer = getConsumer();
        _skipNulls = NullsConstantProvider.isSkipper(_nullProvider);
    }

    protected MethodProperty(MethodProperty src, JsonDeserializer<?> deser,
            NullValueProvider nva) {
        super(src, deser, nva);
        _annotated = src._annotated;
        _setter = src._setter;
        primitiveConsumer = getPrimitiveConsumer();
        consumer = getConsumer();

        _skipNulls = NullsConstantProvider.isSkipper(nva);
    }

    protected MethodProperty(MethodProperty src, PropertyName newName) {
        super(src, newName);
        _annotated = src._annotated;
        _setter = src._setter;
        primitiveConsumer = getPrimitiveConsumer();
        consumer = getConsumer();

        _skipNulls = src._skipNulls;
    }

    private BiConsumer getConsumer() {
        Class<?> setterParameterType = _setter.getParameterTypes()[0];
        if (setterParameterType.isPrimitive()) {
//            if (setterParameterType == int.class)
//                return (a, b) -> ((ObjIntConsumer) primitiveConsumer).accept(a, (int) b);
//            if (setterParameterType == long.class)
//                return (a, b) -> ((ObjLongConsumer) primitiveConsumer).accept(a, (long) b);
            return null;
        } else {
            try {
                return getBiConsumerObjectSetter(unreflect(_setter));
            } catch (Throwable throwable) {
                return null;
            }
        }
    }

    private Object getPrimitiveConsumer() {
        Class<?> setterParameterType = _setter.getParameterTypes()[0];
        if (setterParameterType.isPrimitive()) {
            try {
                if (setterParameterType == int.class) {
                        return getIntSetter(unreflect(_setter));
                }
                if (setterParameterType == long.class)
                    return getLongSetter(unreflect(_setter));
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

    private ObjIntConsumer getIntSetter(MethodHandle methodHandle) throws Throwable {
        final Class<?> functionKlaz = ObjIntConsumer.class;
        Object o = getSetter(methodHandle, functionKlaz);
        return (ObjIntConsumer) o;
    }

    private ObjLongConsumer getLongSetter(MethodHandle methodHandle) throws Throwable {
        final Class<?> functionKlaz = ObjLongConsumer.class;
        Object o = getSetter(methodHandle, functionKlaz);
        return (ObjLongConsumer) o;
    }

    private BiConsumer getBiConsumerObjectSetter(MethodHandle methodHandle) throws Throwable {
        final Class<?> functionKlaz = BiConsumer.class;
        Object o = getBiConsumerSetter(methodHandle, functionKlaz);
        return (BiConsumer) o;
    }

    private Object getSetter(MethodHandle methodHandle, Class<?> functionKlaz) throws Throwable {
        final String functionName = "accept";
        final Class<?> functionReturn = void.class;
        Class<?> aClass = !methodHandle.type().parameterType(1).isPrimitive()
                ? Object.class
                : methodHandle.type().parameterType(1);
        final Class<?>[] functionParams = new Class<?>[] { Object.class,
                aClass};

        final MethodType factoryMethodType = MethodType
                .methodType(functionKlaz);
        final MethodType functionMethodType = MethodType.methodType(
                functionReturn, functionParams);

        final CallSite setterFactory = LambdaMetafactory.metafactory( //
                getLookup(_setter), // Represents a lookup context.
                functionName, // The name of the method to implement.
                factoryMethodType, // Signature of the factory method.
                functionMethodType, // Signature of function implementation.
                methodHandle, // Function method implementation.
                methodHandle.type() // Function method type signature.
        );

        final MethodHandle setterInvoker = setterFactory.getTarget();
        return setterInvoker.invoke();
    }


    private Object getBiConsumerSetter(MethodHandle methodHandle, Class<?> functionKlaz) throws Throwable {
        final String functionName = "accept";
        final Class<?> functionReturn = void.class;
        Class<?> aClass = Object.class;
        final Class<?>[] functionParams = new Class<?>[] { Object.class,
                aClass};

        final MethodType factoryMethodType = MethodType
                .methodType(functionKlaz);
        final MethodType functionMethodType = MethodType.methodType(
                functionReturn, functionParams);

        final CallSite setterFactory = LambdaMetafactory.metafactory( //
                getLookup(_setter), // Represents a lookup context.
                functionName, // The name of the method to implement.
                factoryMethodType, // Signature of the factory method.
                functionMethodType, // Signature of function implementation.
                methodHandle, // Function method implementation.
                methodHandle.type() // Function method type signature.
        );

        final MethodHandle setterInvoker = setterFactory.getTarget();
        return setterInvoker.invoke();
    }

    /**
     * Constructor used for JDK Serialization when reading persisted object
     */
    protected MethodProperty(MethodProperty src, Method m) {
        super(src);
        _annotated = src._annotated;
        _setter = m;
        primitiveConsumer = getPrimitiveConsumer();
        consumer = getConsumer();

        _skipNulls = src._skipNulls;
    }

    @Override
    public SettableBeanProperty withName(PropertyName newName) {
        return new MethodProperty(this, newName);
    }
    
    @Override
    public SettableBeanProperty withValueDeserializer(JsonDeserializer<?> deser) {
        if (_valueDeserializer == deser) {
            return this;
        }
        // 07-May-2019, tatu: As per [databind#2303], must keep VD/NVP in-sync if they were
        NullValueProvider nvp = (_valueDeserializer == _nullProvider) ? deser : _nullProvider;
        return new MethodProperty(this, deser, nvp);
    }

    @Override
    public SettableBeanProperty withNullProvider(NullValueProvider nva) {
        return new MethodProperty(this, _valueDeserializer, nva);
    }

    @Override
    public void fixAccess(DeserializationConfig config) {
        _annotated.fixAccess(
                config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
    }

    /*
    /**********************************************************
    /* BeanProperty impl
    /**********************************************************
     */
    
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        return (_annotated == null) ? null : _annotated.getAnnotation(acls);
    }

    @Override public AnnotatedMember getMember() {  return _annotated; }

    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */

    @Override
    public void deserializeAndSet(JsonParser p, DeserializationContext ctxt,
            Object instance) throws IOException
    {
        Object value;
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            if (_skipNulls) {
                return;
            }
            value = _nullProvider.getNullValue(ctxt);
        } else if (_valueTypeDeserializer == null) {
            value = _valueDeserializer.deserialize(p, ctxt);
            // 04-May-2018, tatu: [databind#2023] Coercion from String (mostly) can give null
            if (value == null) {
                if (_skipNulls) {
                    return;
                }
                value = _nullProvider.getNullValue(ctxt);
            }
        } else {
            value = _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        }
        try {
            invokeWithoutResult(ctxt.isEnabled(DeserializationFeature.LAMBDA_METAFACTORY_AS_INVOKER), instance, value);
        } catch (Exception e) {
            _throwAsIOE(p, e, value);
        }
    }

    @Override
    public Object deserializeSetAndReturn(JsonParser p,
    		DeserializationContext ctxt, Object instance) throws IOException
    {
        Object value;
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            if (_skipNulls) {
                return instance;
            }
            value = _nullProvider.getNullValue(ctxt);
        } else if (_valueTypeDeserializer == null) {
            value = _valueDeserializer.deserialize(p, ctxt);
            // 04-May-2018, tatu: [databind#2023] Coercion from String (mostly) can give null
            if (value == null) {
                if (_skipNulls) {
                    return instance;
                }
                value = _nullProvider.getNullValue(ctxt);
            }
        } else {
            value = _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        }
        try {
            Object result = invokeWithResult(instance, value);
            return (result == null) ? instance : result;
        } catch (Exception e) {
            _throwAsIOE(p, e, value);
            return null;
        }
    }

    private void invokeWithoutResult(boolean useConsumer, Object instance, Object value) throws InvocationTargetException, IllegalAccessException {
        if (useConsumer && consumer != null) {
            consumer.accept(instance, value);
        } else {
            _setter.invoke(instance, value);
        }
    }

    private Object invokeWithResult(Object instance, Object value) throws InvocationTargetException, IllegalAccessException {
        return _setter.invoke(instance, value);
    }
    @Override
    public final void set(Object instance, Object value) throws IOException
    {
        try {
            invokeWithoutResult(false, instance, value);
        } catch (Exception e) {
            // 15-Sep-2015, tatu: How could we get a ref to JsonParser?
            _throwAsIOE(e, value);
        }
    }

    @Override
    public Object setAndReturn(Object instance, Object value) throws IOException
    {
        try {
            Object result = invokeWithResult(instance, value);
            return (result == null) ? instance : result;
        } catch (Exception e) {
            // 15-Sep-2015, tatu: How could we get a ref to JsonParser?
            _throwAsIOE(e, value);
            return null;
        }
    }

    /*
    /**********************************************************
    /* JDK serialization handling
    /**********************************************************
     */

    Object readResolve() {
        return new MethodProperty(this, _annotated.getAnnotated());
    }
}
