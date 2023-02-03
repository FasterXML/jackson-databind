package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.JDKValueInstantiators;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId.Referring;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Class that represents a "wildcard" set method which can be used
 * to generically set values of otherwise unmapped (aka "unknown")
 * properties read from JSON content.
 *<p>
 * Note: starting with 2.14, is {@code abstract} class with multiple
 * concrete implementations
 */
public abstract class SettableAnyProperty
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
    protected final AnnotatedMember _setter;

    protected final boolean _setterIsField;

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

    /**
     * @since 2.14
     */
    public static SettableAnyProperty constructForMethod(DeserializationContext ctxt,
            BeanProperty property,
            AnnotatedMember field, JavaType valueType,
            KeyDeserializer keyDeser,
            JsonDeserializer<Object> valueDeser, TypeDeserializer typeDeser) {
        return new MethodAnyProperty(property, field, valueType,
                keyDeser, valueDeser, typeDeser);
    }

    /**
     * @since 2.14
     */
    public static SettableAnyProperty constructForMapField(DeserializationContext ctxt,
            BeanProperty property,
            AnnotatedMember field, JavaType valueType,
            KeyDeserializer keyDeser,
            JsonDeserializer<Object> valueDeser, TypeDeserializer typeDeser)
    {
        Class<?> mapType = field.getRawType();
        // 02-Aug-2022, tatu: Ideally would be resolved to a concrete type by caller but
        //    alas doesn't appear to happen. Nor does `BasicDeserializerFactory` expose method
        //    for finding default or explicit mappings.
        if (mapType == Map.class) {
            mapType = LinkedHashMap.class;
        }
        ValueInstantiator vi = JDKValueInstantiators.findStdValueInstantiator(ctxt.getConfig(), mapType);
        return new MapFieldAnyProperty(property, field, valueType,
                keyDeser, valueDeser, typeDeser,
                vi);
    }

    /**
     * @since 2.14
     */
    public static SettableAnyProperty constructForJsonNodeField(DeserializationContext ctxt,
            BeanProperty property,
            AnnotatedMember field, JavaType valueType, JsonDeserializer<Object> valueDeser) {
        return new JsonNodeFieldAnyProperty(property, field, valueType,
                valueDeser,
                ctxt.getNodeFactory());
    }

    // Abstract @since 2.14
    public abstract SettableAnyProperty withValueDeserializer(JsonDeserializer<Object> deser);

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
            throw new IllegalArgumentException("Missing method/field (broken JDK (de)serialization?)");
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

    /**
     * @since 2.14
     */
    public String getPropertyName() { return _property.getName(); }

    /*
    /**********************************************************
    /* Public API, deserialization
    /**********************************************************
     */

    /**
     * Method called to deserialize appropriate value, given parser (and
     * context), and set it using appropriate method (a setter method).
     */
    public void deserializeAndSet(JsonParser p, DeserializationContext ctxt,
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
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            return _valueDeserializer.getNullValue(ctxt);
        }
        if (_valueTypeDeserializer != null) {
            return _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        }
        return _valueDeserializer.deserialize(p, ctxt);
    }

    // Default implementation since 2.14
    public void set(Object instance, Object propName, Object value) throws IOException
    {
        try {
            _set(instance, propName, value);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            _throwAsIOE(e, propName, value);
        }
    }

    protected abstract void _set(Object instance, Object propName, Object value) throws Exception;

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
            StringBuilder msg = new StringBuilder("Problem deserializing \"any-property\" '").append(propName);
            msg.append("' of class "+getClassName()+" (expected type: ").append(_type);
            msg.append("; actual type: ").append(actType).append(")");
            String origMsg = ClassUtil.exceptionMessage(e);
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
        throw new JsonMappingException(null, ClassUtil.exceptionMessage(t), t);
    }

    private String getClassName() { return ClassUtil.nameOf(_setter.getDeclaringClass()); }

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

    /*
    /**********************************************************************
    /* Concrete implementations
    /**********************************************************************
     */

    /**
     * @since 2.14
     */
    protected static class MethodAnyProperty extends SettableAnyProperty
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        public MethodAnyProperty(BeanProperty property,
                AnnotatedMember field, JavaType valueType,
                KeyDeserializer keyDeser,
                JsonDeserializer<Object> valueDeser, TypeDeserializer typeDeser) {
            super(property, field, valueType,
                    keyDeser, valueDeser, typeDeser);
        }

        @Override
        protected void _set(Object instance, Object propName, Object value) throws Exception
        {
            // note: cannot use 'setValue()' due to taking 2 args
            ((AnnotatedMethod) _setter).callOnWith(instance, propName, value);
        }

        @Override
        public SettableAnyProperty withValueDeserializer(JsonDeserializer<Object> deser) {
            return new MethodAnyProperty(_property, _setter, _type,
                    _keyDeserializer, deser, _valueTypeDeserializer);
        }
    }

    /**
     * @since 2.14
     */
    protected static class MapFieldAnyProperty extends SettableAnyProperty
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        protected final ValueInstantiator _valueInstantiator;

        public MapFieldAnyProperty(BeanProperty property,
                AnnotatedMember field, JavaType valueType,
                KeyDeserializer keyDeser,
                JsonDeserializer<Object> valueDeser, TypeDeserializer typeDeser,
                ValueInstantiator inst) {
            super(property, field, valueType,
                    keyDeser, valueDeser, typeDeser);
            _valueInstantiator = inst;
        }

        @Override
        public SettableAnyProperty withValueDeserializer(JsonDeserializer<Object> deser) {
            return new MapFieldAnyProperty(_property, _setter, _type,
                    _keyDeserializer, deser, _valueTypeDeserializer,
                    _valueInstantiator);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void _set(Object instance, Object propName, Object value) throws Exception
        {
            AnnotatedField field = (AnnotatedField) _setter;
            Map<Object,Object> val = (Map<Object,Object>) field.getValue(instance);
            // 01-Aug-2022, tatu: [databind#3559] Will try to create and assign an
            //    instance.
            if (val == null) {
                val = _createAndSetMap(null, field, instance, propName);
            }
            // add the property key and value
            val.put(propName, value);
        }

        @SuppressWarnings("unchecked")
        protected Map<Object, Object> _createAndSetMap(DeserializationContext ctxt, AnnotatedField field,
                Object instance, Object propName)
            throws IOException
        {
            if (_valueInstantiator == null) {
                throw JsonMappingException.from(ctxt, String.format(
                        "Cannot create an instance of %s for use as \"any-setter\" '%s'",
                        ClassUtil.nameOf(_type.getRawClass()), _property.getName()));
            }
            Map<Object,Object> map = (Map<Object,Object>) _valueInstantiator.createUsingDefault(ctxt);
            field.setValue(instance, map);
            return map;
        }
    }

    /**
     * @since 2.14
     */
    protected static class JsonNodeFieldAnyProperty extends SettableAnyProperty
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        protected final JsonNodeFactory _nodeFactory;

        public JsonNodeFieldAnyProperty(BeanProperty property,
                AnnotatedMember field, JavaType valueType,
                JsonDeserializer<Object> valueDeser,
                JsonNodeFactory nodeFactory) {
            super(property, field, valueType, null, valueDeser, null);
            _nodeFactory = nodeFactory;
        }

        // Let's override since this is much simpler with JsonNodes
        @Override
        public void deserializeAndSet(JsonParser p, DeserializationContext ctxt,
                Object instance, String propName)
            throws IOException
        {
            setProperty(instance, propName, (JsonNode) deserialize(p, ctxt));
        }

        // Let's override since this is much simpler with JsonNodes
        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            return _valueDeserializer.deserialize(p, ctxt);
        }

        @Override
        protected void _set(Object instance, Object propName, Object value) throws Exception {
            setProperty(instance, (String) propName, (JsonNode) value);
        }

        protected void setProperty(Object instance, String propName, JsonNode value)
            throws IOException
        {
            AnnotatedField field = (AnnotatedField) _setter;
            Object val0 = field.getValue(instance);
            ObjectNode objectNode;

            if (val0 == null) {
                objectNode = _nodeFactory.objectNode();
                field.setValue(instance, objectNode);
            } else if (!(val0 instanceof ObjectNode)) {
                throw JsonMappingException.from((DeserializationContext) null, String.format(
                        "Value \"any-setter\" '%s' not `ObjectNode` but %s",
                        getPropertyName(),
                        ClassUtil.nameOf(val0.getClass())));
            } else {
                objectNode = (ObjectNode) val0;
            }
            // add the property key and value
            objectNode.set(propName, value);
        }

        // Should not get called but...
        @Override
        public SettableAnyProperty withValueDeserializer(JsonDeserializer<Object> deser) {
            return this;
        }
    }
}
