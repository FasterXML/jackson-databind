package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
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
import com.fasterxml.jackson.databind.util.LambdaMetafactoryUtils;

// https://github.com/FasterXML/jackson-databind/issues/2083
// TODO:
// - tests of performance
// - create pull request
// - add support for Java 9,10,11,12,13


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
    protected final transient BiFunction function;

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
        consumer = getConsumer();
        function = getFunction();
        _skipNulls = NullsConstantProvider.isSkipper(_nullProvider);
    }

    protected MethodProperty(MethodProperty src, JsonDeserializer<?> deser,
            NullValueProvider nva) {
        super(src, deser, nva);
        _annotated = src._annotated;
        _setter = src._setter;
        consumer = getConsumer();
        function = getFunction();

        _skipNulls = NullsConstantProvider.isSkipper(nva);
    }

    protected MethodProperty(MethodProperty src, PropertyName newName) {
        super(src, newName);
        _annotated = src._annotated;
        _setter = src._setter;
        consumer = getConsumer();
        function = getFunction();

        _skipNulls = src._skipNulls;
    }

    /**
     * Constructor used for JDK Serialization when reading persisted object
     */
    protected MethodProperty(MethodProperty src, Method m) {
        super(src);
        _annotated = src._annotated;
        _setter = m;
        consumer = getConsumer();
        function = getFunction();

        _skipNulls = src._skipNulls;
    }

    private BiConsumer getConsumer() {
        return LambdaMetafactoryUtils.getBiConsumerObjectSetter(_setter);
    }

    private BiFunction getFunction() {
        return LambdaMetafactoryUtils.getBiFunctionObjectSetter(_setter);
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
            invokeWithoutResult(isConsumerEnabled(ctxt), instance, value);
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
            Object result = invokeWithResult(isConsumerEnabled(ctxt), instance, value);
            return (result == null) ? instance : result;
        } catch (Exception e) {
            _throwAsIOE(p, e, value);
            return null;
        }
    }

    private boolean isConsumerEnabled(DeserializationContext ctxt) {
        return ctxt.isEnabled(DeserializationFeature.LAMBDA_METAFACTORY_AS_INVOKER);
    }


    private void invokeWithoutResult(boolean useConsumer, Object instance, Object value) throws InvocationTargetException, IllegalAccessException {
        if (useConsumer) {
            if (consumer != null) {
                consumer.accept(instance, value);
            } else {
                _setter.invoke(instance, value);
            }
        } else {
            _setter.invoke(instance, value);
        }
    }

    private Object invokeWithResult(boolean useConsumer, Object instance, Object value) throws InvocationTargetException, IllegalAccessException {
        if (useConsumer) {
            if (function != null) {
                return function.apply(instance, value);
            } else {
                return _setter.invoke(instance, value);
            }
        } else {
            return _setter.invoke(instance, value);
        }
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
            Object result = invokeWithResult(false, instance, value);
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
