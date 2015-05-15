package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.HashSet;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.util.NameTransformer;

public class BeanAsArrayBuilderDeserializer
    extends BeanDeserializerBase
{
    private static final long serialVersionUID = 1L;

    /**
     * Deserializer we delegate operations that we can not handle.
     */
    final protected BeanDeserializerBase _delegate;

    /**
     * Properties in order expected to be found in JSON array.
     */
    final protected SettableBeanProperty[] _orderedProperties;

    final protected AnnotatedMethod _buildMethod;
        
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
    public BeanAsArrayBuilderDeserializer(BeanDeserializerBase delegate,
            SettableBeanProperty[] ordered,
            AnnotatedMethod buildMethod)
    {
        super(delegate);
        _delegate = delegate;
        _orderedProperties = ordered;
        _buildMethod = buildMethod;
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
    public BeanAsArrayBuilderDeserializer withObjectIdReader(ObjectIdReader oir) {
        return new BeanAsArrayBuilderDeserializer(_delegate.withObjectIdReader(oir),
                _orderedProperties, _buildMethod);
    }

    @Override
    public BeanAsArrayBuilderDeserializer withIgnorableProperties(HashSet<String> ignorableProps) {
        return new BeanAsArrayBuilderDeserializer(_delegate.withIgnorableProperties(ignorableProps),
                _orderedProperties, _buildMethod);
    }

    @Override
    protected BeanAsArrayBuilderDeserializer asArrayDeserializer() {
        return this;
    }

    /*
    /**********************************************************
    /* JsonDeserializer implementation
    /**********************************************************
     */

    protected final Object finishBuild(DeserializationContext ctxt, Object builder)
        throws IOException
    {
        try {
            return _buildMethod.getMember().invoke(builder);
        } catch (Exception e) {
            wrapInstantiationProblem(e, ctxt);
            return null;
        }
    }
    
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // Let's delegate just in case we got a JSON Object (could error out, alternatively?)
        if (!p.isExpectedStartArrayToken()) {
            return finishBuild(ctxt, _deserializeFromNonArray(p, ctxt));
        }
        if (!_vanillaProcessing) {
            return finishBuild(ctxt, _deserializeNonVanilla(p, ctxt));
        }
        Object builder = _valueInstantiator.createUsingDefault(ctxt);
        final SettableBeanProperty[] props = _orderedProperties;
        int i = 0;
        final int propCount = props.length;
        while (true) {
            if (p.nextToken() == JsonToken.END_ARRAY) {
                return finishBuild(ctxt, builder);
            }
            if (i == propCount) {
                break;
            }
            SettableBeanProperty prop = props[i];
            if (prop != null) { // normal case
                try {
                    builder = prop.deserializeSetAndReturn(p, ctxt, builder);
                } catch (Exception e) {
                    wrapAndThrow(e, builder, prop.getName(), ctxt);
                }
            } else { // just skip?
                p.skipChildren();
            }
            ++i;
        }
        // Ok; extra fields? Let's fail, unless ignoring extra props is fine
        if (!_ignoreAllUnknown) {
            throw ctxt.mappingException("Unexpected JSON values; expected at most "+propCount+" properties (in JSON Array)");
        }
        // otherwise, skip until end
        while (p.nextToken() != JsonToken.END_ARRAY) {
            p.skipChildren();
        }
        return finishBuild(ctxt, builder);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object builder)
        throws IOException
    {
        /* No good way to verify that we have an array... although could I guess
         * check via JsonParser. So let's assume everything is working fine, for now.
         */
        if (_injectables != null) {
            injectValues(ctxt, builder);
        }
        final SettableBeanProperty[] props = _orderedProperties;
        int i = 0;
        final int propCount = props.length;
        while (true) {
            if (p.nextToken() == JsonToken.END_ARRAY) {
                return finishBuild(ctxt, builder);
            }
            if (i == propCount) {
                break;
            }
            SettableBeanProperty prop = props[i];
            if (prop != null) { // normal case
                try {
                    builder = prop.deserializeSetAndReturn(p, ctxt, builder);
                } catch (Exception e) {
                    wrapAndThrow(e, builder, prop.getName(), ctxt);
                }
            } else { // just skip?
                p.skipChildren();
            }
            ++i;
        }
        
        // Ok; extra fields? Let's fail, unless ignoring extra props is fine
        if (!_ignoreAllUnknown) {
            throw ctxt.mappingException("Unexpected JSON values; expected at most "+propCount+" properties (in JSON Array)");
        }
        // otherwise, skip until end
        while (p.nextToken() != JsonToken.END_ARRAY) {
            p.skipChildren();
        }
        return finishBuild(ctxt, builder);
    }

    // needed since 2.1
    @Override
    public Object deserializeFromObject(JsonParser p, DeserializationContext ctxt) throws IOException
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
     * Note: should NOT resolve builder; caller will do that
     * 
     * @return Builder object in use.
     */
    protected Object _deserializeNonVanilla(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        if (_nonStandardCreation) {
            return _deserializeWithCreator(p, ctxt);
        }
        Object builder = _valueInstantiator.createUsingDefault(ctxt);
        if (_injectables != null) {
            injectValues(ctxt, builder);
        }
        Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        final SettableBeanProperty[] props = _orderedProperties;
        int i = 0;
        final int propCount = props.length;
        while (true) {
            if (p.nextToken() == JsonToken.END_ARRAY) {
                return builder;
            }
            if (i == propCount) {
                break;
            }
            SettableBeanProperty prop = props[i];
            ++i;
            if (prop != null) { // normal case
                if (activeView == null || prop.visibleInView(activeView)) {
                    try {
                        prop.deserializeSetAndReturn(p, ctxt, builder);
                    } catch (Exception e) {
                        wrapAndThrow(e, builder, prop.getName(), ctxt);
                    }
                    continue;
                }
            }
            // otherwise, skip it (view-filtered, no prop etc)
            p.skipChildren();
        }
        // Ok; extra fields? Let's fail, unless ignoring extra props is fine
        if (!_ignoreAllUnknown) {
            throw ctxt.mappingException("Unexpected JSON values; expected at most "+propCount+" properties (in JSON Array)");
        }
        // otherwise, skip until end
        while (p.nextToken() != JsonToken.END_ARRAY) {
            p.skipChildren();
        }
        return builder;
    }
    
    protected Object _deserializeWithCreator(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {        
        if (_delegateDeserializer != null) {
            return _valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(p, ctxt));
        }
        if (_propertyBasedCreator != null) {
            return _deserializeUsingPropertyBased(p, ctxt);
        }
        // should only occur for abstract types...
        if (_beanType.isAbstract()) {
            throw JsonMappingException.from(p, "Can not instantiate abstract type "+_beanType
                    +" (need to add/enable type information?)");
        }
        throw JsonMappingException.from(p, "No suitable constructor found for type "
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
    protected final Object _deserializeUsingPropertyBased(final JsonParser p,
            final DeserializationContext ctxt)
        throws IOException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);

        final SettableBeanProperty[] props = _orderedProperties;
        final int propCount = props.length;
        int i = 0;
        Object builder = null;
        
        for (; p.nextToken() != JsonToken.END_ARRAY; ++i) {
            SettableBeanProperty prop = (i < propCount) ? props[i] : null;
            if (prop == null) { // we get null if there are extra elements; maybe otherwise too?
                p.skipChildren();
                continue;
            }
            // if we have already constructed POJO, things are simple:
            if (builder != null) {
                try {
                    builder = prop.deserializeSetAndReturn(p, ctxt, builder);
                } catch (Exception e) {
                    wrapAndThrow(e, builder, prop.getName(), ctxt);
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
                        builder = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                        continue; // never gets here
                    }
                    //  polymorphic?
                    if (builder.getClass() != _beanType.getRawClass()) {
                        /* 23-Jul-2012, tatu: Not sure if these could ever be properly
                         *   supported (since ordering of elements may not be guaranteed);
                         *   but make explicitly non-supported for now.
                         */
                        throw ctxt.mappingException("Can not support implicit polymorphic deserialization for POJOs-as-Arrays style: "
                                +"nominal type "+_beanType.getRawClass().getName()+", actual type "+builder.getClass().getName());
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
        if (builder == null) {
            try {
                builder = creator.build(ctxt, buffer);
            } catch (Exception e) {
                wrapInstantiationProblem(e, ctxt);
                return null; // never gets here
            }
        }
        return builder;
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
