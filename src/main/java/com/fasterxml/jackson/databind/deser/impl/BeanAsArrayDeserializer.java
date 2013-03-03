package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.HashSet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Variant of {@link BeanDeserializer} used for handling deserialization
 * of POJOs when serialized as JSON Arrays, instead of JSON Objects.
 * 
 * @since 2.1
 */
public class BeanAsArrayDeserializer
    extends BeanDeserializerBase
{
    private static final long serialVersionUID = 1L;

    /**
     * Deserializer we delegate operations that we can not handle.
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
    public JsonDeserializer<Object> unwrappingDeserializer(NameTransformer unwrapper)
    {
        /* We can't do much about this; could either replace _delegate
         * with unwrapping instance, or just replace this one. Latter seems
         * more sensible.
         */
        return _delegate.unwrappingDeserializer(unwrapper);
    }

    @Override
    public BeanAsArrayDeserializer withObjectIdReader(ObjectIdReader oir) {
        return new BeanAsArrayDeserializer(_delegate.withObjectIdReader(oir),
                _orderedProperties);
    }

    @Override
    public BeanAsArrayDeserializer withIgnorableProperties(HashSet<String> ignorableProps) {
        return new BeanAsArrayDeserializer(_delegate.withIgnorableProperties(ignorableProps),
                _orderedProperties);
    }

    @Override
    protected BeanDeserializerBase asArrayDeserializer() {
        return this;
    }

    /*
    /**********************************************************
    /* JsonDeserializer implementation
    /**********************************************************
     */
    
    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Let's delegate just in case we got a JSON Object (could error out, alternatively?)
        if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
            return _deserializeFromNonArray(jp, ctxt);
        }
        if (!_vanillaProcessing) {
            return _deserializeNonVanilla(jp, ctxt);
        }
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        final SettableBeanProperty[] props = _orderedProperties;
        int i = 0;
        final int propCount = props.length;
        while (true) {
            if (jp.nextToken() == JsonToken.END_ARRAY) {
                return bean;
            }
            if (i == propCount) {
                break;
            }
            SettableBeanProperty prop = props[i];
            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
            } else { // just skip?
                jp.skipChildren();
            }
            ++i;
        }
        // Ok; extra fields? Let's fail, unless ignoring extra props is fine
        if (!_ignoreAllUnknown) {
            throw ctxt.mappingException("Unexpected JSON values; expected at most "+propCount+" properties (in JSON Array)");
        }
        // otherwise, skip until end
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            jp.skipChildren();
        }
        return bean;
    }

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt, Object bean)
        throws IOException, JsonProcessingException
    {
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
            if (jp.nextToken() == JsonToken.END_ARRAY) {
                return bean;
            }
            if (i == propCount) {
                break;
            }
            SettableBeanProperty prop = props[i];
            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
            } else { // just skip?
                jp.skipChildren();
            }
            ++i;
        }
        
        // Ok; extra fields? Let's fail, unless ignoring extra props is fine
        if (!_ignoreAllUnknown) {
            throw ctxt.mappingException("Unexpected JSON values; expected at most "+propCount+" properties (in JSON Array)");
        }
        // otherwise, skip until end
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            jp.skipChildren();
        }
        return bean;
    }


    // needed since 2.1
    @Override
    public Object deserializeFromObject(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        return _deserializeFromNonArray(jp, ctxt);
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
    protected Object _deserializeNonVanilla(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (_nonStandardCreation) {
            return _deserializeWithCreator(jp, ctxt);
        }
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        final SettableBeanProperty[] props = _orderedProperties;
        int i = 0;
        final int propCount = props.length;
        while (true) {
            if (jp.nextToken() == JsonToken.END_ARRAY) {
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
                        prop.deserializeAndSet(jp, ctxt, bean);
                    } catch (Exception e) {
                        wrapAndThrow(e, bean, prop.getName(), ctxt);
                    }
                    continue;
                }
            }
            // otherwise, skip it (view-filtered, no prop etc)
            jp.skipChildren();
        }
        // Ok; extra fields? Let's fail, unless ignoring extra props is fine
        if (!_ignoreAllUnknown) {
            throw ctxt.mappingException("Unexpected JSON values; expected at most "+propCount+" properties (in JSON Array)");
        }
        // otherwise, skip until end
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            jp.skipChildren();
        }
        return bean;
    }
    
    protected Object _deserializeWithCreator(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {        
        if (_delegateDeserializer != null) {
            return _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(jp, ctxt));
        }
        if (_propertyBasedCreator != null) {
            return _deserializeUsingPropertyBased(jp, ctxt);
        }
        // should only occur for abstract types...
        if (_beanType.isAbstract()) {
            throw JsonMappingException.from(jp, "Can not instantiate abstract type "+_beanType
                    +" (need to add/enable type information?)");
        }
        throw JsonMappingException.from(jp, "No suitable constructor found for type "
                +_beanType+": can not instantiate from JSON object (need to add/enable type information?)");
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
    protected final Object _deserializeUsingPropertyBased(final JsonParser jp, final DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(jp, ctxt, _objectIdReader);

        final SettableBeanProperty[] props = _orderedProperties;
        final int propCount = props.length;
        int i = 0;
        Object bean = null;
        
        for (; jp.nextToken() != JsonToken.END_ARRAY; ++i) {
            SettableBeanProperty prop = (i < propCount) ? props[i] : null;
            if (prop == null) { // we get null if there are extra elements; maybe otherwise too?
                jp.skipChildren();
                continue;
            }
            // if we have already constructed POJO, things are simple:
            if (bean != null) {
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
                continue;
            }
            final String propName = prop.getName();
            // if not yet, maybe we got a creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                // Last creator property to set?
                Object value = creatorProp.deserialize(jp, ctxt);
                if (buffer.assignParameter(creatorProp.getCreatorIndex(), value)) {
                    try {
                        bean = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                        continue; // never gets here
                    }
                    //  polymorphic?
                    if (bean.getClass() != _beanType.getRawClass()) {
                        /* 23-Jul-2012, tatu: Not sure if these could ever be properly
                         *   supported (since ordering of elements may not be guaranteed);
                         *   but make explicitly non-supported for now.
                         */
                        throw ctxt.mappingException("Can not support implicit polymorphic deserialization for POJOs-as-Arrays style: "
                                +"nominal type "+_beanType.getRawClass().getName()+", actual type "+bean.getClass().getName());
                    }
                }
                continue;
            }
            // Object Id property?
            if (buffer.readIdProperty(propName)) {
                continue;
            }
            // regular property? needs buffering
            buffer.bufferProperty(prop, prop.deserialize(jp, ctxt));
        }

        // In case we didn't quite get all the creator properties, we may have to do this:
        if (bean == null) {
            try {
                bean = creator.build(ctxt, buffer);
            } catch (Exception e) {
                wrapInstantiationProblem(e, ctxt);
                return null; // never gets here
            }
        }
        return bean;
    }

    /*
    /**********************************************************
    /* Helper methods, error reporting
    /**********************************************************
     */

    protected Object _deserializeFromNonArray(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        // Let's start with failure
        throw ctxt.mappingException("Can not deserialize a POJO (of type "+_beanType.getRawClass().getName()
                +") from non-Array representation (token: "+jp.getCurrentToken()
                +"): type/property designed to be serialized as JSON Array");
        // in future, may allow use of "standard" POJO serialization as well; if so, do:
        //return _delegate.deserialize(jp, ctxt);
    }
}
