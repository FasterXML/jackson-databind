package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.Annotations;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * This concrete sub-class implements property that is set
 * directly assigning to a Field.
 */
public final class FieldProperty
    extends SettableBeanProperty
{
    private static final long serialVersionUID = 1L;

    final protected AnnotatedField _annotated;

    /**
     * Actual field to set when deserializing this property.
     * Transient since there is no need to persist; only needed during
     * construction of objects.
     */
    final protected transient Field _field;

    /**
     * @since 2.9
     */
    final protected boolean _skipNulls;

    public FieldProperty(BeanPropertyDefinition propDef, JavaType type,
            TypeDeserializer typeDeser, Annotations contextAnnotations, AnnotatedField field)
    {
        super(propDef, type, typeDeser, contextAnnotations);
        _annotated = field;
        _field = field.getAnnotated();
        _skipNulls = NullsConstantProvider.isSkipper(_nullProvider);
    }

    protected FieldProperty(FieldProperty src, JsonDeserializer<?> deser,
            NullValueProvider nva) {
        super(src, deser, nva);
        _annotated = src._annotated;
        _field = src._field;
        _skipNulls = NullsConstantProvider.isSkipper(nva);
    }

    protected FieldProperty(FieldProperty src, PropertyName newName) {
        super(src, newName);
        _annotated = src._annotated;
        _field = src._field;
        _skipNulls = src._skipNulls;
    }

    /**
     * Constructor used for JDK Serialization when reading persisted object
     */
    protected FieldProperty(FieldProperty src)
    {
        super(src);
        _annotated = src._annotated;
        Field f = _annotated.getAnnotated();
        if (f == null) {
            throw new IllegalArgumentException("Missing field (broken JDK (de)serialization?)");
        }
        _field = f;
        _skipNulls = src._skipNulls;
    }

    @Override
    public SettableBeanProperty withName(PropertyName newName) {
        return new FieldProperty(this, newName);
    }

    @Override
    public SettableBeanProperty withValueDeserializer(JsonDeserializer<?> deser) {
        if (_valueDeserializer == deser) {
            return this;
        }
        // 07-May-2019, tatu: As per [databind#2303], must keep VD/NVP in-sync if they were
        NullValueProvider nvp = (_valueDeserializer == _nullProvider) ? deser : _nullProvider;
        return new FieldProperty(this, deser, nvp);
    }

    @Override
    public SettableBeanProperty withNullProvider(NullValueProvider nva) {
        return new FieldProperty(this, _valueDeserializer, nva);
    }

    @Override
    public void fixAccess(DeserializationConfig config) {
        ClassUtil.checkAndFixAccess(_field,
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
    public void deserializeAndSet(JsonParser p,
    		DeserializationContext ctxt, Object instance) throws IOException
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
            _field.set(instance, value);
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
            _field.set(instance, value);
        } catch (Exception e) {
            _throwAsIOE(p, e, value);
        }
        return instance;
    }

    @Override
    public void set(Object instance, Object value) throws IOException
    {
        try {
            _field.set(instance, value);
        } catch (Exception e) {
            // 15-Sep-2015, tatu: How could we get a ref to JsonParser?
            _throwAsIOE(e, value);
        }
    }

    @Override
    public Object setAndReturn(Object instance, Object value) throws IOException
    {
        try {
            _field.set(instance, value);
        } catch (Exception e) {
            // 15-Sep-2015, tatu: How could we get a ref to JsonParser?
            _throwAsIOE(e, value);
        }
        return instance;
    }

    /*
    /**********************************************************
    /* JDK serialization handling
    /**********************************************************
     */

    Object readResolve() {
        return new FieldProperty(this);
    }
}