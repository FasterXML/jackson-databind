package tools.jackson.databind.deser.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.NullValueProvider;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.introspect.*;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.util.Annotations;
import tools.jackson.databind.util.internal.UnreflectHandleSupplier;

import static java.lang.invoke.MethodType.methodType;

/**
 * This concrete sub-class implements property that is set
 * using a {@link MethodHandle} to the setter, which is either
 * a setter method or a field setter.
 */
public final class MethodProperty
    extends SettableBeanProperty
{
    protected final AnnotatedMember _annotated;

    /**
     * Setter MethodHandle holder for modifying property value.
     */
    protected final SetterHolder _setter = new SetterHolder(methodType(void.class, Object.class, Object.class));
    /**
     * Setter MethodHandle holder for modifying property value and returning the modified bean.
     */
    protected final SetterHolder _setterReturn = new SetterHolder(methodType(Object.class, Object.class, Object.class));

    protected final boolean _skipNulls;

    public MethodProperty(BeanPropertyDefinition propDef,
            JavaType type, TypeDeserializer typeDeser,
            Annotations contextAnnotations, AnnotatedMember annotated)
    {
        super(propDef, type, typeDeser, contextAnnotations);
        _annotated = annotated;
        _skipNulls = NullsConstantProvider.isSkipper(_nullProvider);
    }

    protected MethodProperty(MethodProperty src, ValueDeserializer<?> deser,
            NullValueProvider nva) {
        super(src, deser, nva);
        _annotated = src._annotated;
        _skipNulls = NullsConstantProvider.isSkipper(nva);
    }

    protected MethodProperty(MethodProperty src, PropertyName newName) {
        super(src, newName);
        _annotated = src._annotated;
        _skipNulls = src._skipNulls;
    }

    @Override
    public SettableBeanProperty withName(PropertyName newName) {
        return new MethodProperty(this, newName);
    }

    @Override
    public SettableBeanProperty withValueDeserializer(ValueDeserializer<?> deser) {
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
            Object instance) throws JacksonException
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
            _setter.get().invokeExact(instance, value);
        } catch (Throwable e) {
            _throwAsJacksonE(p, e, value);
        }
    }

    @Override
    public Object deserializeSetAndReturn(JsonParser p,
    		DeserializationContext ctxt, Object instance) throws JacksonException
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
            Object result = _setterReturn.get().invokeExact(instance, value);
            return (result == null) ? instance : result;
        } catch (Throwable e) {
            _throwAsJacksonE(p, e, value);
            return null;
        }
    }

    @Override
    public final void set(DeserializationContext ctxt, Object instance, Object value)
        throws JacksonException
    {
        if (value == null) {
            if (_skipNulls) {
                return;
            }
        }
        try {
            _setter.get().invokeExact(instance, value);
        } catch (Throwable e) {
            _throwAsJacksonE(ctxt.getParser(), e, value);
        }
    }

    @Override
    public Object setAndReturn(DeserializationContext ctxt,
            Object instance, Object value) throws JacksonException
    {
        if (value == null) {
            if (_skipNulls) {
                return instance;
            }
        }
        try {
            Object result = _setterReturn.get().invokeExact(instance, value);
            return (result == null) ? instance : result;
        } catch (Throwable e) {
            _throwAsJacksonE(ctxt.getParser(), e, value);
            return null;
        }
    }

    class SetterHolder extends UnreflectHandleSupplier {
        SetterHolder(MethodType asType) {
            super(asType);
        }

        @Override
        protected MethodHandle unreflect() throws IllegalAccessException {
            if (_annotated instanceof AnnotatedMethod) {
                AnnotatedMethod am = (AnnotatedMethod) _annotated;
                return MethodHandles.lookup().unreflect(am.getAnnotated());
            } else {
                AnnotatedField af = (AnnotatedField) _annotated;
                return MethodHandles.lookup().unreflectSetter(af.getAnnotated());
            }
        }
    }
}
