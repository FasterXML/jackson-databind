package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId.Referring;
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

    protected JsonDeserializer<Object> _valueDeserializer;

    protected final TypeDeserializer _valueTypeDeserializer;

    /**
     * @since 2.9
     */
    protected final KeyDeserializer _keyDeserializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public SettableAnyProperty(BeanProperty property, AnnotatedMember setter, JavaType type,
            KeyDeserializer keyDeser,
            JsonDeserializer<Object> valueDeser, TypeDeserializer typeDeser)
    {
        _property = property;
        _setter = setter;
        _type = type;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = typeDeser;
        _keyDeserializer = keyDeser;
        _setterIsField = setter instanceof AnnotatedField;
    }

    @Deprecated // since 2.9
    public SettableAnyProperty(BeanProperty property, AnnotatedMember setter, JavaType type,
            JsonDeserializer<Object> valueDeser, TypeDeserializer typeDeser)
    {
        this(property, setter, type, null, valueDeser, typeDeser);
    }

    public SettableAnyProperty withValueDeserializer(JsonDeserializer<Object> deser) {
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
        throws IOException
    {
        try {
            Object key = (_keyDeserializer == null) ? propName
                    : _keyDeserializer.deserializeKey(propName, ctxt);
            set(instance, key, deserialize(p, ctxt));
        } catch (UnresolvedForwardReference reference) {
            if (!(_valueDeserializer.getObjectIdReader() != null)) {
                throw JsonMappingException.from(p, "Unresolved forward reference but no identity info.", reference);
            }
            AnySetterReferring referring = new AnySetterReferring(this, reference,
                    _type.getRawClass(), instance, propName);
            reference.getRoid().appendReferring(referring);
        }
    }

    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_NULL) {
            return _valueDeserializer.getNullValue(ctxt);
        }
        if (_valueTypeDeserializer != null) {
            return _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        }
        return _valueDeserializer.deserialize(p, ctxt);
    }

    @SuppressWarnings("unchecked")
    public void set(Object instance, Object propName, Object value) throws IOException
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
        throws IOException
    {
        if (e instanceof IllegalArgumentException) {
            String actType = ClassUtil.classNameOf(value);
            StringBuilder msg = new StringBuilder("Problem deserializing \"any\" property '").append(propName);
            msg.append("' of class "+getClassName()+" (expected type: ").append(_type);
            msg.append("; actual type: ").append(actType).append(")");
            String origMsg = e.getMessage();
            if (origMsg != null) {
                msg.append(", problem: ").append(origMsg);
            } else {
                msg.append(" (no error message provided)");
            }
            throw new JsonMappingException(null, msg.toString(), e);
        }
        ClassUtil.throwIfIOE(e);
        ClassUtil.throwIfRTE(e);
        // let's wrap the innermost problem
        Throwable t = ClassUtil.getRootCause(e);
        throw new JsonMappingException(null, t.getMessage(), t);
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
            throws IOException
        {
            if (!hasId(id)) {
                throw new IllegalArgumentException("Trying to resolve a forward reference with id [" + id.toString()
                        + "] that wasn't previously registered.");
            }
            _parent.set(_pojo, _propName, value);
        }
    }
}
