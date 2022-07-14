package com.fasterxml.jackson.databind.deser;

import java.util.Map;

import tools.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ReadableObjectId.Referring;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Class that represents a "wildcard" set method which can be used
 * to generically set values of otherwise unmapped (aka "unknown")
 * properties read from Json content.
 *<p>
 * !!! Note: might make sense to refactor to share some code
 * with {@link SettableBeanProperty}?
 */
public class SettableAnyProperty
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;
    
    /**
     * Method used for setting "any" properties, along with annotation
     * information. Retained to allow contextualization of any properties.
     */
    protected final BeanProperty _property;

    /**
     * Annotated variant is needed for JDK serialization only
     */
    final protected AnnotatedMember _setter;

    final boolean _setterIsField;
    
    protected final JavaType _type;

    protected ValueDeserializer<Object> _valueDeserializer;

    protected final TypeDeserializer _valueTypeDeserializer;
    protected final KeyDeserializer _keyDeserializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public SettableAnyProperty(BeanProperty property, AnnotatedMember setter, JavaType type,
            KeyDeserializer keyDeser,
            ValueDeserializer<Object> valueDeser, TypeDeserializer typeDeser)
    {
        _property = property;
        _setter = setter;
        _type = type;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = typeDeser;
        _keyDeserializer = keyDeser;
        _setterIsField = setter instanceof AnnotatedField;
    }

    public SettableAnyProperty withValueDeserializer(ValueDeserializer<Object> deser) {
        return new SettableAnyProperty(_property, _setter, _type,
                _keyDeserializer, deser, _valueTypeDeserializer);
    }

    public void fixAccess(DeserializationConfig config) {
        _setter.fixAccess(
                config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
    }

    /*
    /**********************************************************
    /* JDK serialization handling
    /**********************************************************
     */

    /**
     * Need to define this to verify that we retain actual Method reference
     */
    Object readResolve() {
        // sanity check...
        if (_setter == null || _setter.getAnnotated() == null) {
            throw new IllegalArgumentException("Missing method (broken JDK (de)serialization?)");
        }
        return this;
    }
    
    /*
    /**********************************************************
    /* Public API, accessors
    /**********************************************************
     */

    public BeanProperty getProperty() { return _property; }
    
    public boolean hasValueDeserializer() { return (_valueDeserializer != null); }

    public JavaType getType() { return _type; }

    /*
    /**********************************************************
    /* Public API, deserialization
    /**********************************************************
     */
    
    /**
     * Method called to deserialize appropriate value, given parser (and
     * context), and set it using appropriate method (a setter method).
     */
    public final void deserializeAndSet(JsonParser p, DeserializationContext ctxt,
            Object instance, String propName)
        throws JacksonException
    {
        try {
            Object key = (_keyDeserializer == null) ? propName
                    : _keyDeserializer.deserializeKey(propName, ctxt);
            set(instance, key, deserialize(p, ctxt));
        } catch (UnresolvedForwardReference reference) {
            if (_valueDeserializer.getObjectIdReader(ctxt) == null) {
                throw DatabindException.from(p, "Unresolved forward reference but no identity info.", reference);
            }
            AnySetterReferring referring = new AnySetterReferring(this, reference,
                    _type.getRawClass(), instance, propName);
            reference.getRoid().appendReferring(referring);
        }
    }

    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_NULL) {
            return _valueDeserializer.getNullValue(ctxt);
        }
        if (_valueTypeDeserializer != null) {
            return _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        }
        return _valueDeserializer.deserialize(p, ctxt);
    }

    @SuppressWarnings("unchecked")
    public void set(Object instance, Object propName, Object value) throws JacksonException
    {
        try {
            // if annotation in the field (only map is supported now)
            if (_setterIsField) {
                AnnotatedField field = (AnnotatedField) _setter;
                Map<Object,Object> val = (Map<Object,Object>) field.getValue(instance);
                /* 01-Jun-2016, tatu: At this point it is not quite clear what to do if
                 *    field is `null` -- we cannot necessarily count on zero-args
                 *    constructor except for a small set of types, so for now just
                 *    ignore if null. May need to figure out something better in future.
                 */
                if (val != null) {
                    // add the property key and value
                    val.put(propName, value);
                }
            } else {
                // note: cannot use 'setValue()' due to taking 2 args
                ((AnnotatedMethod) _setter).callOnWith(instance, propName, value);
            }
        } catch (Exception e) {
            _throwAsIOE(e, propName, value);
        }
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * @param e Exception to re-throw or wrap
     * @param propName Name of property (from Json input) to set
     * @param value Value of the property
     */
    protected void _throwAsIOE(Exception e, Object propName, Object value)
        throws JacksonException
    {
        if (e instanceof IllegalArgumentException) {
            String actType = ClassUtil.classNameOf(value);
            StringBuilder msg = new StringBuilder("Problem deserializing \"any\" property '").append(propName);
            msg.append("' of class "+getClassName()+" (expected type: ").append(_type);
            msg.append("; actual type: ").append(actType).append(")");
            String origMsg = ClassUtil.exceptionMessage(e);
            if (origMsg != null) {
                msg.append(", problem: ").append(origMsg);
            } else {
                msg.append(" (no error message provided)");
            }
            throw DatabindException.from((JsonParser) null, msg.toString(), e);
        }
        ClassUtil.throwIfJacksonE(e);
        ClassUtil.throwIfRTE(e);
        // let's wrap the innermost problem
        Throwable t = ClassUtil.getRootCause(e);
        throw DatabindException.from((JsonParser) null, ClassUtil.exceptionMessage(t), t);
    }

    private String getClassName() { return _setter.getDeclaringClass().getName(); }

    @Override public String toString() { return "[any property on class "+getClassName()+"]"; }

    private static class AnySetterReferring extends Referring {
        private final SettableAnyProperty _parent;
        private final Object _pojo;
        private final String _propName;

        public AnySetterReferring(SettableAnyProperty parent,
                UnresolvedForwardReference reference, Class<?> type, Object instance, String propName)
        {
            super(reference, type);
            _parent = parent;
            _pojo = instance;
            _propName = propName;
        }

        @Override
        public void handleResolvedForwardReference(Object id, Object value)
            throws JacksonException
        {
            if (!hasId(id)) {
                throw new IllegalArgumentException("Trying to resolve a forward reference with id [" + id.toString()
                        + "] that wasn't previously registered.");
            }
            _parent.set(_pojo, _propName, value);
        }
    }
}
