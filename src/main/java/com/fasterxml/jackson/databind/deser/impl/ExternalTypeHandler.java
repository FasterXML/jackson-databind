package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Helper class that is used to flatten JSON structure when using
 * "external type id" (see {@link com.fasterxml.jackson.annotation.JsonTypeInfo.As#EXTERNAL_PROPERTY}).
 * This is needed to store temporary state and buffer tokens, as the structure is
 * rearranged a bit so that actual type deserializer can resolve type and 
 * finalize deserialization.
 */
public class ExternalTypeHandler
{
    private final JavaType _beanType;

    private final ExtTypedProperty[] _properties;

    /**
     * Mapping from external property ids to one or more indexes;
     * in most cases single index as <code>Integer</code>, but
     * occasionally same name maps to multiple ones: if so,
     * <code>List&lt;Integer&gt;</code>.
     */
    private final Map<String, Object> _nameToPropertyIndex;

    private final String[] _typeIds;
    private final TokenBuffer[] _tokens;

    protected ExternalTypeHandler(JavaType beanType,
            ExtTypedProperty[] properties,
            Map<String, Object> nameToPropertyIndex,
            String[] typeIds, TokenBuffer[] tokens)
    {
        _beanType = beanType;
        _properties = properties;
        _nameToPropertyIndex = nameToPropertyIndex;
        _typeIds = typeIds;
        _tokens = tokens;
    }

    protected ExternalTypeHandler(ExternalTypeHandler h)
    {
        _beanType = h._beanType;
        _properties = h._properties;
        _nameToPropertyIndex = h._nameToPropertyIndex;
        int len = _properties.length;
        _typeIds = new String[len];
        _tokens = new TokenBuffer[len];
    }

    /**
     * @since 2.9
     */
    public static Builder builder(JavaType beanType) {
        return new Builder(beanType);
    }

    /**
     * Method called to start collection process by creating non-blueprint
     * instances.
     */
    public ExternalTypeHandler start() {
        return new ExternalTypeHandler(this);
    }

    /**
     * Method called to see if given property/value pair is an external type
     * id; and if so handle it. This is <b>only</b> to be called in case
     * containing POJO has similarly named property as the external type id AND
     * value is of scalar type:
     * otherwise {@link #handlePropertyValue} should be called instead.
     */
    @SuppressWarnings("unchecked")
    public boolean handleTypePropertyValue(JsonParser p, DeserializationContext ctxt,
            String propName, Object bean)
        throws IOException
    {
        Object ob = _nameToPropertyIndex.get(propName);
        if (ob == null) {
            return false;
        }
        final String typeId = p.getText();
        // 28-Nov-2016, tatu: For [databind#291], need separate handling
        if (ob instanceof List<?>) {
            boolean result = false;
            for (Integer index : (List<Integer>) ob) {
                if (_handleTypePropertyValue(p, ctxt, propName, bean,
                        typeId, index.intValue())) {
                    result = true;
                }
            }
            return result;
        }
        return _handleTypePropertyValue(p, ctxt, propName, bean,
                typeId, ((Integer) ob).intValue());
    }

    private final boolean _handleTypePropertyValue(JsonParser p, DeserializationContext ctxt,
            String propName, Object bean, String typeId, int index)
        throws IOException
    {
        ExtTypedProperty prop = _properties[index];
        if (!prop.hasTypePropertyName(propName)) { // when could/should this ever happen?
            return false;
        }
        // note: can NOT skip child values (should always be String anyway)
        boolean canDeserialize = (bean != null) && (_tokens[index] != null);
        // Minor optimization: deserialize properties as soon as we have all we need:
        if (canDeserialize) {
            _deserializeAndSet(p, ctxt, bean, index, typeId);
            // clear stored data, to avoid deserializing+setting twice:
            _tokens[index] = null;
        } else {
            _typeIds[index] = typeId;
        }
        return true;
    }

    /**
     * Method called to ask handler to handle value of given property,
     * at point where parser points to the first token of the value.
     * Handling can mean either resolving type id it contains (if it matches type
     * property name), or by buffering the value for further use.
     * 
     * @return True, if the given property was properly handled
     */
    @SuppressWarnings("unchecked")
    public boolean handlePropertyValue(JsonParser p, DeserializationContext ctxt,
            String propName, Object bean) throws IOException
    {
        Object ob = _nameToPropertyIndex.get(propName);
        if (ob == null) {
            return false;
        }
        // 28-Nov-2016, tatu: For [databind#291], need separate handling
        if (ob instanceof List<?>) {
            Iterator<Integer> it = ((List<Integer>) ob).iterator();
            Integer index = it.next();

            ExtTypedProperty prop = _properties[index];
            // For now, let's assume it's same type (either type id OR value)
            // for all mappings, so we'll only check first one
            if (prop.hasTypePropertyName(propName)) {
                String typeId = p.getText();
                p.skipChildren();
                _typeIds[index] = typeId;
                while (it.hasNext()) {
                    _typeIds[it.next()] = typeId;
                }
            } else {
                @SuppressWarnings("resource")
                TokenBuffer tokens = new TokenBuffer(p, ctxt);
                tokens.copyCurrentStructure(p);
                _tokens[index] = tokens;
                while (it.hasNext()) {
                    _tokens[it.next()] = tokens;
                }
            }
            return true;
        }

        // Otherwise only maps to a single value, in which case we can
        // handle things in bit more optimal way...
        int index = ((Integer) ob).intValue();
        ExtTypedProperty prop = _properties[index];
        boolean canDeserialize;
        if (prop.hasTypePropertyName(propName)) {
            _typeIds[index] = p.getText();
            p.skipChildren();
            canDeserialize = (bean != null) && (_tokens[index] != null);
        } else {
            @SuppressWarnings("resource")
            TokenBuffer tokens = new TokenBuffer(p, ctxt);
            tokens.copyCurrentStructure(p);
            _tokens[index] = tokens;
            canDeserialize = (bean != null) && (_typeIds[index] != null);
        }
        // Minor optimization: let's deserialize properties as soon as
        // we have all pertinent information:
        if (canDeserialize) {
            String typeId = _typeIds[index];
            // clear stored data, to avoid deserializing+setting twice:
            _typeIds[index] = null;
            _deserializeAndSet(p, ctxt, bean, index, typeId);
            _tokens[index] = null;
        }
        return true;
    }

    /**
     * Method called after JSON Object closes, and has to ensure that all external
     * type ids have been handled.
     */
    @SuppressWarnings("resource")
    public Object complete(JsonParser p, DeserializationContext ctxt, Object bean)
        throws IOException
    {
        for (int i = 0, len = _properties.length; i < len; ++i) {
            String typeId = _typeIds[i];
            if (typeId == null) {
                TokenBuffer tokens = _tokens[i];
                // let's allow missing both type and property (may already have been set, too)
                // but not just one
                if (tokens == null) {
                    continue;
                }
                // [databind#118]: Need to mind natural types, for which no type id
                // will be included.
                JsonToken t = tokens.firstToken();
                if (t.isScalarValue()) { // can't be null as we never store empty buffers
                    JsonParser buffered = tokens.asParser(p);
                    buffered.nextToken();
                    SettableBeanProperty extProp = _properties[i].getProperty();
                    Object result = TypeDeserializer.deserializeIfNatural(buffered, ctxt, extProp.getType());
                    if (result != null) {
                        extProp.set(bean, result);
                        continue;
                    }
                    // 26-Oct-2012, tatu: As per [databind#94], must allow use of 'defaultImpl'
                    if (!_properties[i].hasDefaultType()) {
                        ctxt.reportInputMismatch(bean.getClass(),
                                "Missing external type id property '%s'",
                                _properties[i].getTypePropertyName());                                
                    } else  {
                        typeId = _properties[i].getDefaultTypeId();
                    }
                }
            } else if (_tokens[i] == null) {
                SettableBeanProperty prop = _properties[i].getProperty();

                if(prop.isRequired() ||
                        ctxt.isEnabled(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY)) {
                    ctxt.reportInputMismatch(bean.getClass(),
                            "Missing property '%s' for external type id '%s'",
                            prop.getName(), _properties[i].getTypePropertyName());
                }
                return bean;
            }
            _deserializeAndSet(p, ctxt, bean, i, typeId);
        }
        return bean;
    }

    /**
     * Variant called when creation of the POJO involves buffering of creator properties
     * as well as property-based creator.
     */
    public Object complete(JsonParser p, DeserializationContext ctxt,
            PropertyValueBuffer buffer, PropertyBasedCreator creator)
        throws IOException
    {
        // first things first: deserialize all data buffered:
        final int len = _properties.length;
        Object[] values = new Object[len];
        for (int i = 0; i < len; ++i) {
            String typeId = _typeIds[i];
            final ExtTypedProperty extProp = _properties[i];

            if (typeId == null) {
                // let's allow missing both type and property (may already have been set, too)
                if (_tokens[i] == null) {
                    continue;
                }
                // but not just one
                // 26-Oct-2012, tatu: As per [databind#94], must allow use of 'defaultImpl'
                if (!extProp.hasDefaultType()) {
                    ctxt.reportInputMismatch(_beanType,
                            "Missing external type id property '%s'",
                            extProp.getTypePropertyName());
                } else {
                    typeId = extProp.getDefaultTypeId();
                }
            } else if (_tokens[i] == null) {
                SettableBeanProperty prop = extProp.getProperty();
                ctxt.reportInputMismatch(_beanType,
                        "Missing property '%s' for external type id '%s'",
                        prop.getName(), _properties[i].getTypePropertyName());
            }
            values[i] = _deserialize(p, ctxt, i, typeId);

            final SettableBeanProperty prop = extProp.getProperty();
            // also: if it's creator prop, fill in
            if (prop.getCreatorIndex() >= 0) {
                buffer.assignParameter(prop, values[i]);

                // [databind#999] And maybe there's creator property for type id too?
                SettableBeanProperty typeProp = extProp.getTypeProperty();
                // for now, should only be needed for creator properties, too
                if ((typeProp != null) && (typeProp.getCreatorIndex() >= 0)) {
                    buffer.assignParameter(typeProp, typeId);
                }
            }
        }

        Object bean = creator.build(ctxt, buffer);
        // third: assign non-creator properties
        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = _properties[i].getProperty();
            if (prop.getCreatorIndex() < 0) {
                prop.set(bean, values[i]);
            }
        }
        return bean;
    }

    @SuppressWarnings("resource")
    protected final Object _deserialize(JsonParser p, DeserializationContext ctxt,
            int index, String typeId) throws IOException
    {
        JsonParser p2 = _tokens[index].asParser(p);
        JsonToken t = p2.nextToken();
        // 29-Sep-2015, tatu: As per [databind#942], nulls need special support
        if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        TokenBuffer merged = new TokenBuffer(p, ctxt);
        merged.writeStartArray();
        merged.writeString(typeId);
        merged.copyCurrentStructure(p2);
        merged.writeEndArray();

        // needs to point to START_OBJECT (or whatever first token is)
        JsonParser mp = merged.asParser(p);
        mp.nextToken();
        return _properties[index].getProperty().deserialize(mp, ctxt);
    }

    @SuppressWarnings("resource")
    protected final void _deserializeAndSet(JsonParser p, DeserializationContext ctxt,
            Object bean, int index, String typeId) throws IOException
    {
        /* Ok: time to mix type id, value; and we will actually use "wrapper-array"
         * style to ensure we can handle all kinds of JSON constructs.
         */
        JsonParser p2 = _tokens[index].asParser(p);
        JsonToken t = p2.nextToken();
        // 29-Sep-2015, tatu: As per [databind#942], nulls need special support
        if (t == JsonToken.VALUE_NULL) {
            _properties[index].getProperty().set(bean, null);
            return;
        }
        TokenBuffer merged = new TokenBuffer(p, ctxt);
        merged.writeStartArray();
        merged.writeString(typeId);

        merged.copyCurrentStructure(p2);
        merged.writeEndArray();
        // needs to point to START_OBJECT (or whatever first token is)
        JsonParser mp = merged.asParser(p);
        mp.nextToken();
        _properties[index].getProperty().deserializeAndSet(mp, ctxt, bean);
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */
    
    public static class Builder
    {
        private final JavaType _beanType;

        private final List<ExtTypedProperty> _properties = new ArrayList<>();
        private final Map<String, Object> _nameToPropertyIndex = new HashMap<>();

        protected Builder(JavaType t) {
            _beanType = t;
        }

        public void addExternal(SettableBeanProperty property, TypeDeserializer typeDeser)
        {
            Integer index = _properties.size();
            _properties.add(new ExtTypedProperty(property, typeDeser));
            _addPropertyIndex(property.getName(), index);
            _addPropertyIndex(typeDeser.getPropertyName(), index);
        }

        private void _addPropertyIndex(String name, Integer index) {
            Object ob = _nameToPropertyIndex.get(name);
            if (ob == null) {
                _nameToPropertyIndex.put(name, index);
            } else if (ob instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) ob;
                list.add(index);
            } else {
                List<Object> list = new LinkedList<>();
                list.add(ob);
                list.add(index);
                _nameToPropertyIndex.put(name, list);
            }
        }
        
        /**
         * Method called after all external properties have been assigned, to further
         * link property with polymorphic value with possible property for type id
         * itself. This is needed to support type ids as Creator properties.
         *
         * @since 2.8
         */
        public ExternalTypeHandler build(BeanPropertyMap otherProps) {
            // 21-Jun-2016, tatu: as per [databind#999], may need to link type id property also
            final int len = _properties.size();
            ExtTypedProperty[] extProps = new ExtTypedProperty[len];
            for (int i = 0; i < len; ++i) {
                ExtTypedProperty extProp = _properties.get(i);
                String typePropId = extProp.getTypePropertyName();
                SettableBeanProperty typeProp = otherProps.find(typePropId);
                if (typeProp != null) {
                    extProp.linkTypeProperty(typeProp);
                }
                extProps[i] = extProp;
            }
            return new ExternalTypeHandler(_beanType, extProps, _nameToPropertyIndex,
                    null, null);
        }
    }

    private final static class ExtTypedProperty
    {
        private final SettableBeanProperty _property;
        private final TypeDeserializer _typeDeserializer;
        private final String _typePropertyName;

        /**
         * @since 2.8
         */
        private SettableBeanProperty _typeProperty;

        public ExtTypedProperty(SettableBeanProperty property, TypeDeserializer typeDeser)
        {
            _property = property;
            _typeDeserializer = typeDeser;
            _typePropertyName = typeDeser.getPropertyName();
        }

        /**
         * @since 2.8
         */
        public void linkTypeProperty(SettableBeanProperty p) {
            _typeProperty = p;
        }

        public boolean hasTypePropertyName(String n) {
            return n.equals(_typePropertyName);
        }

        public boolean hasDefaultType() {
            return _typeDeserializer.getDefaultImpl() != null;
        }

        /**
         * Specialized called when we need to expose type id of `defaultImpl` when
         * serializing: we may need to expose it for assignment to a property, or
         * it may be requested as visible for some other reason.
         */
        public String getDefaultTypeId() {
            Class<?> defaultType = _typeDeserializer.getDefaultImpl();
            if (defaultType == null) {
                return null;
            }
            return _typeDeserializer.getTypeIdResolver().idFromValueAndType(null, defaultType);
        }

        public String getTypePropertyName() { return _typePropertyName; }

        public SettableBeanProperty getProperty() {
            return _property;
        }

        /**
         * @since 2.8
         */
        public SettableBeanProperty getTypeProperty() {
            return _typeProperty;
        }
    }
}
