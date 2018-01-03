package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Variant of {@link BeanDeserializer} used for handling deserialization
 * of POJOs when serialized as JSON Arrays, instead of JSON Objects.
 */
public class BeanAsArrayDeserializer
    extends BeanDeserializerBase
{
    private static final long serialVersionUID = 1L;

    /**
     * Deserializer we delegate operations that we cannot handle.
     */
    protected final BeanDeserializerBase _delegate;

    /**
     * Properties in order expected to be found in JSON array.
     */
    protected final SettableBeanProperty[] _orderedProperties;
    
    /*
    /**********************************************************
    /* Life-cycle, construction, initialization
    /**********************************************************
     */

    /**
     * Main constructor used both for creating new instances (by
     * {@link BeanDeserializer#asArrayDeserializer}) and for
     * creating copies with different delegate.
     */
    public BeanAsArrayDeserializer(BeanDeserializerBase delegate,
            SettableBeanProperty[] ordered)
    {
        super(delegate);
        _delegate = delegate;
        _orderedProperties = ordered;
    }
    
    @Override
    public JsonDeserializer<Object> unwrappingDeserializer(DeserializationContext ctxt,
            NameTransformer unwrapper)
    {
        // We can't do much about this; could either replace _delegate with unwrapping
        // instance, or just replace this one. Latter seems more sensible.
        return _delegate.unwrappingDeserializer(ctxt, unwrapper);
    }

    @Override
    public BeanDeserializerBase withObjectIdReader(ObjectIdReader oir) {
        return new BeanAsArrayDeserializer(_delegate.withObjectIdReader(oir),
                _orderedProperties);
    }

    @Override
    public BeanDeserializerBase withIgnorableProperties(Set<String> ignorableProps) {
        return new BeanAsArrayDeserializer(_delegate.withIgnorableProperties(ignorableProps),
                _orderedProperties);
    }

    @Override
    public BeanDeserializerBase withBeanProperties(BeanPropertyMap props) {
        return new BeanAsArrayDeserializer(_delegate.withBeanProperties(props),
                _orderedProperties);
    }
    
    @Override
    protected BeanDeserializerBase asArrayDeserializer() {
        return this;
    }

    @Override
    protected void initFieldMatcher(DeserializationContext ctxt) { }
    
    /*
    /**********************************************************
    /* JsonDeserializer implementation
    /**********************************************************
     */
    
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // Let's delegate just in case we got a JSON Object (could error out, alternatively?)
        if (!p.isExpectedStartArrayToken()) {
            return _deserializeFromNonArray(p, ctxt);
        }
        if (!_vanillaProcessing) {
            return _deserializeNonVanilla(p, ctxt);
        }
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(bean);

        final SettableBeanProperty[] props = _orderedProperties;
        int i = 0;
        final int propCount = props.length;

        for (; (i + 3) < propCount; i += 4) {
            SettableBeanProperty prop;
            if (p.nextToken() == JsonToken.END_ARRAY) return bean;
            if ((prop = props[i]) != null) {
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
            } else {
                p.skipChildren();
            }

            if (p.nextToken() == JsonToken.END_ARRAY) return bean;
            if ((prop = props[i+1]) != null) { // element #2
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
            } else {
                p.skipChildren();
            }

            if (p.nextToken() == JsonToken.END_ARRAY) return bean;
            if ((prop = props[i+2]) != null) { // element #3
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
            } else {
                p.skipChildren();
            }

            if (p.nextToken() == JsonToken.END_ARRAY) return bean;
            if ((prop = props[i+3]) != null) { // element #4
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
            } else {
                p.skipChildren();
            }
        }

        SettableBeanProperty prop;
        switch (propCount - i) {
        case 3:
            if (p.nextToken() == JsonToken.END_ARRAY) {
                return bean;
            }
            if ((prop = props[i++]) != null) {
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
            } else {
                p.skipChildren();
            }
            // fall through
        case 2:
            if (p.nextToken() == JsonToken.END_ARRAY) {
                return bean;
            }
            if ((prop = props[i++]) != null) {
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
            } else {
                p.skipChildren();
            }
            // fall through
        case 1:
            if (p.nextToken() == JsonToken.END_ARRAY) {
                return bean;
            }
            if ((prop = props[i]) != null) {
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
            } else {
                p.skipChildren();
            }
        }
        
        /*
        while (true) {
            if (p.nextToken() == JsonToken.END_ARRAY) {
                return bean;
            }
            if (i == propCount) {
                break;
            }
            SettableBeanProperty prop = props[i];
            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    throw wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
            } else { // just skip?
                p.skipChildren();
            }
            ++i;
        }
        */
        if (p.nextToken() != JsonToken.END_ARRAY) {
            // Ok; extra fields? Let's fail, unless ignoring extra props is fine
            if (!_ignoreAllUnknown && ctxt.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
                ctxt.reportWrongTokenException(this, JsonToken.END_ARRAY,
                        "Unexpected JSON values; expected at most %d properties (in JSON Array)",
                        propCount);
                // never gets here
            }
            // otherwise, skip until end
            do {
                p.skipChildren();
            } while (p.nextToken() != JsonToken.END_ARRAY);
        }
        return bean;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object bean)
        throws IOException
    {
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(bean);

        if (!p.isExpectedStartArrayToken()) {
            return _deserializeFromNonArray(p, ctxt);
        }
        
        /* No good way to verify that we have an array... although could I guess
         * check via JsonParser. So let's assume everything is working fine, for now.
         */
        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        final SettableBeanProperty[] props = _orderedProperties;
        int i = 0;
        final int propCount = props.length;
        while (true) {
            if (p.nextToken() == JsonToken.END_ARRAY) {
                return bean;
            }
            if (i == propCount) {
                break;
            }
            SettableBeanProperty prop = props[i];
            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    throw wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
            } else { // just skip?
                p.skipChildren();
            }
            ++i;
        }
        
        // Ok; extra fields? Let's fail, unless ignoring extra props is fine
        if (!_ignoreAllUnknown && ctxt.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
            ctxt.reportWrongTokenException(this, JsonToken.END_ARRAY,
                    "Unexpected JSON values; expected at most %d properties (in JSON Array)",
                    propCount);
            // never gets here
        }
        // otherwise, skip until end
        do {
            p.skipChildren();
        } while (p.nextToken() != JsonToken.END_ARRAY);
        return bean;
    }

    // needed since 2.1
    @Override
    public Object deserializeFromObject(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        return _deserializeFromNonArray(p, ctxt);
    }

    /*
    /**********************************************************
    /* Helper methods, non-standard creation
    /**********************************************************
     */

    /**
     * Alternate deserialization method that has to check many more configuration
     * aspects than the "vanilla" processing.
     */
    protected Object _deserializeNonVanilla(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        if (_nonStandardCreation) {
            return deserializeFromObjectUsingNonDefault(p, ctxt);
        }
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(bean);
        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        final SettableBeanProperty[] props = _orderedProperties;
        int i = 0;
        final int propCount = props.length;

        while (true) {
            if (p.nextToken() == JsonToken.END_ARRAY) {
                return bean;
            }
            if (i == propCount) {
                break;
            }
            SettableBeanProperty prop = props[i];
            ++i;
            if (prop != null) { // normal case
                if (activeView == null || prop.visibleInView(activeView)) {
                    try {
                        prop.deserializeAndSet(p, ctxt, bean);
                    } catch (Exception e) {
                        throw wrapAndThrow(e, bean, prop.getName(), ctxt);
                    }
                    continue;
                }
            }
            // otherwise, skip it (view-filtered, no prop etc)
            p.skipChildren();
        }
        // Ok; extra fields? Let's fail, unless ignoring extra props is fine
        if (!_ignoreAllUnknown) {
            ctxt.reportWrongTokenException(this, JsonToken.END_ARRAY,
                    "Unexpected JSON values; expected at most %d properties (in JSON Array)",
                    propCount);
            // will never reach here as exception has been thrown
        }
        // otherwise, skip until end
        do {
            p.skipChildren();
        } while (p.nextToken() != JsonToken.END_ARRAY);
        return bean;
    }

    /**
     * Method called to deserialize bean using "property-based creator":
     * this means that a non-default constructor or factory method is
     * called, and then possibly other setters. The trick is that
     * values for creator method need to be buffered, first; and 
     * due to non-guaranteed ordering possibly some other properties
     * as well.
     */
    @Override
    protected final Object _deserializeUsingPropertyBased(final JsonParser p, final DeserializationContext ctxt)
        throws IOException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);

        final SettableBeanProperty[] props = _orderedProperties;
        final int propCount = props.length;
        int i = 0;
        Object bean = null;
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;

        for (; p.nextToken() != JsonToken.END_ARRAY; ++i) {
            SettableBeanProperty prop = (i < propCount) ? props[i] : null;
            if (prop == null) { // we get null if there are extra elements; maybe otherwise too?
                p.skipChildren();
                continue;
            }
            if ((activeView != null) && !prop.visibleInView(activeView)) {
                p.skipChildren();
                continue;
            }

            // if we have already constructed POJO, things are simple:
            if (bean != null) {
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    throw wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
                continue;
            }
            final String propName = prop.getName();
            // if not yet, maybe we got a creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                // Last creator property to set?
                if (buffer.assignParameter(creatorProp, creatorProp.deserialize(p, ctxt))) {
                    try {
                        bean = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        throw wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                    }
                    // [databind#631]: Assign current value, to be accessible by custom serializers
                    p.setCurrentValue(bean);
                    
                    //  polymorphic?
                    if (bean.getClass() != _beanType.getRawClass()) {
                        /* 23-Jul-2012, tatu: Not sure if these could ever be properly
                         *   supported (since ordering of elements may not be guaranteed);
                         *   but make explicitly non-supported for now.
                         */
                        ctxt.reportBadDefinition(_beanType, String.format(
                                "Cannot support implicit polymorphic deserialization for POJOs-as-Arrays style: "
                                +"nominal type %s, actual type %s",
                                _beanType.getRawClass().getName(), bean.getClass().getName()));
                    }
                }
                continue;
            }
            // Object Id property?
            if (buffer.readIdProperty(propName)) {
                continue;
            }
            // regular property? needs buffering
            buffer.bufferProperty(prop, prop.deserialize(p, ctxt));
        }

        // In case we didn't quite get all the creator properties, we may have to do this:
        if (bean == null) {
            try {
                bean = creator.build(ctxt, buffer);
            } catch (Exception e) {
                return wrapInstantiationProblem(e, ctxt);
            }
        }
        return bean;
    }

    /*
    /**********************************************************
    /* Helper methods, error reporting
    /**********************************************************
     */

    protected Object _deserializeFromNonArray(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        return ctxt.handleUnexpectedToken(handledType(), p.currentToken(), p,
                "Cannot deserialize a POJO (of type %s) from non-Array representation (token: %s): "
                +"type/property designed to be serialized as JSON Array",
                _beanType.getRawClass().getName(),
                p.currentToken());
        // in future, may allow use of "standard" POJO serialization as well; if so, do:
        //return _delegate.deserialize(p, ctxt);
    }
}
