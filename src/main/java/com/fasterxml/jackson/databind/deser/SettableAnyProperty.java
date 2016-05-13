package com.fasterxml.jackson.databind.deser;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId.Referring;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

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
    final protected AnnotatedMethod _setter;

    protected final JavaType _type;

    protected JsonDeserializer<Object> _valueDeserializer;

    protected final TypeDeserializer _valueTypeDeserializer;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public SettableAnyProperty(BeanProperty property, AnnotatedMethod setter, JavaType type,
            JsonDeserializer<Object> valueDeser, TypeDeserializer typeDeser)
    {
        _property = property;
        _setter = setter;
        _type = type;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = typeDeser;
    }

    public SettableAnyProperty withValueDeserializer(JsonDeserializer<Object> deser) {
        return new SettableAnyProperty(_property, _setter, _type,
                deser, _valueTypeDeserializer);
    }

    /**
     * Constructor used for JDK Serialization when reading persisted object
     */
    protected SettableAnyProperty(SettableAnyProperty src)
    {
        _property = src._property;
        _setter = src._setter;
        _type = src._type;
        _valueDeserializer = src._valueDeserializer;
        _valueTypeDeserializer = src._valueTypeDeserializer;
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
            set(instance, propName, deserialize(p, ctxt));
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

    public void set(Object instance, String propName, Object value) throws IOException
    {
        try {
            // note: can not use 'setValue()' due to taking 2 args
            _setter.getAnnotated().invoke(instance, propName, value);
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
    protected void _throwAsIOE(Exception e, String propName, Object value)
        throws IOException
    {
        if (e instanceof IllegalArgumentException) {
            String actType = (value == null) ? "[NULL]" : value.getClass().getName();
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
        if (e instanceof IOException) {
            throw (IOException) e;
        }
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        // let's wrap the innermost problem
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
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
