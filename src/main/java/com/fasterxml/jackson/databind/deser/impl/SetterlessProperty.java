package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.Annotations;

/**
 * This concrete sub-class implements Collection or Map property that is
 * indirectly by getting the property value and directly modifying it.
 */
public final class SetterlessProperty
    extends SettableBeanProperty
{
    private static final long serialVersionUID = 1L;

    protected final AnnotatedMethod _annotated;

    /**
     * Get method for accessing property value used to access property
     * (of Collection or Map type) to modify.
     */
    protected final Method _getter;

    public SetterlessProperty(BeanPropertyDefinition propDef, JavaType type,
            TypeDeserializer typeDeser, Annotations contextAnnotations, AnnotatedMethod method)
    {
        super(propDef, type, typeDeser, contextAnnotations);
        _annotated = method;
        _getter = method.getAnnotated();
    }

    protected SetterlessProperty(SetterlessProperty src, JsonDeserializer<?> deser,
            NullValueProvider nva) {
        super(src, deser, nva);
        _annotated = src._annotated;
        _getter = src._getter;
    }

    protected SetterlessProperty(SetterlessProperty src, PropertyName newName) {
        super(src, newName);
        _annotated = src._annotated;
        _getter = src._getter;
    }

    @Override
    public SettableBeanProperty withName(PropertyName newName) {
        return new SetterlessProperty(this, newName);
    }

    @Override
    public SettableBeanProperty withValueDeserializer(JsonDeserializer<?> deser) {
        if (_valueDeserializer == deser) {
            return this;
        }
        // 07-May-2019, tatu: As per [databind#2303], must keep VD/NVP in-sync if they were
        NullValueProvider nvp = (_valueDeserializer == _nullProvider) ? deser : _nullProvider;
        return new SetterlessProperty(this, deser, nvp);
    }

    @Override
    public SettableBeanProperty withNullProvider(NullValueProvider nva) {
        return new SetterlessProperty(this, _valueDeserializer, nva);
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
        return _annotated.getAnnotation(acls);
    }

    @Override public AnnotatedMember getMember() {  return _annotated; }

    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */

    @Override
    public final void deserializeAndSet(JsonParser p, DeserializationContext ctxt,
            Object instance) throws IOException
    {
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            // Hmmh. Is this a problem? We won't be setting anything, so it's
            // equivalent of empty Collection/Map in this case
            return;
        }
        // For [databind#501] fix we need to implement this but:
        if (_valueTypeDeserializer != null) {
            ctxt.reportBadDefinition(getType(), String.format(
                    "Problem deserializing 'setterless' property (\"%s\"): no way to handle typed deser with setterless yet",
                    getName()));
//            return _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        }
        // Ok: then, need to fetch Collection/Map to modify:
        Object toModify;
        try {
            toModify = _getter.invoke(instance, (Object[]) null);
        } catch (Exception e) {
            _throwAsIOE(p, e);
            return; // never gets here
        }
        // Note: null won't work, since we can't then inject anything in. At least
        // that's not good in common case. However, theoretically the case where
        // we get JSON null might be compatible. If so, implementation could be changed.
        if (toModify == null) {
            ctxt.reportBadDefinition(getType(), String.format(
                    "Problem deserializing 'setterless' property '%s': get method returned null",
                    getName()));
        }
        _valueDeserializer.deserialize(p, ctxt, toModify);
    }

    @Override
    public Object deserializeSetAndReturn(JsonParser p,
    		DeserializationContext ctxt, Object instance) throws IOException
    {
        deserializeAndSet(p, ctxt, instance);
        return instance;
    }

    @Override
    public final void set(Object instance, Object value) throws IOException {
        throw new UnsupportedOperationException("Should never call `set()` on setterless property ('"+getName()+"')");
    }

    @Override
    public Object setAndReturn(Object instance, Object value) throws IOException
    {
        set(instance, value);
        return instance;
    }
}