package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Class that handles deserialization using a separate
 * Builder class, which is used for data binding and
 * produces actual deserialized value at the end
 * of data binding.
 *<p>
 * Note on implementation: much of code has been copied from
 * {@link BeanDeserializer}; there may be opportunities to
 * refactor this in future.
 */
public class BuilderBasedDeserializer
    extends BeanDeserializerBase
{
    private static final long serialVersionUID = 1L;

    protected final AnnotatedMethod _buildMethod;
	
    /*
    /**********************************************************
    /* Life-cycle, construction, initialization
    /**********************************************************
     */

    /**
     * Constructor used by {@link BeanDeserializerBuilder}.
     */
    public BuilderBasedDeserializer(BeanDeserializerBuilder builder,
            BeanDescription beanDesc,
            BeanPropertyMap properties, Map<String, SettableBeanProperty> backRefs,
            HashSet<String> ignorableProps, boolean ignoreAllUnknown,
            boolean hasViews)
    {
        super(builder, beanDesc, properties, backRefs,
                ignorableProps, ignoreAllUnknown, hasViews);
        _buildMethod = builder.getBuildMethod();
        // 05-Mar-2012, tatu: Can not really make Object Ids work with builders, not yet anyway
        if (_objectIdReader != null) {
            throw new IllegalArgumentException("Can not use Object Id with Builder-based deserialization (type "
                    +beanDesc.getType()+")");
        }
    }

    /**
     * Copy-constructor that can be used by sub-classes to allow
     * copy-on-write styling copying of settings of an existing instance.
     */
    protected BuilderBasedDeserializer(BuilderBasedDeserializer src)
    {
        this(src, src._ignoreAllUnknown);
    }

    protected BuilderBasedDeserializer(BuilderBasedDeserializer src, boolean ignoreAllUnknown)
    {
        super(src, ignoreAllUnknown);
        _buildMethod = src._buildMethod;
    }
    
    protected BuilderBasedDeserializer(BuilderBasedDeserializer src, NameTransformer unwrapper) {
        super(src, unwrapper);
        _buildMethod = src._buildMethod;
    }

    public BuilderBasedDeserializer(BuilderBasedDeserializer src, ObjectIdReader oir) {
        super(src, oir);
        _buildMethod = src._buildMethod;
    }

    public BuilderBasedDeserializer(BuilderBasedDeserializer src, HashSet<String> ignorableProps) {
        super(src, ignorableProps);
        _buildMethod = src._buildMethod;
    }
    
    @Override
    public JsonDeserializer<Object> unwrappingDeserializer(NameTransformer unwrapper)
    {
        /* main thing really is to just enforce ignoring of unknown
         * properties; since there may be multiple unwrapped values
         * and properties for all may be interleaved...
         */
        return new BuilderBasedDeserializer(this, unwrapper);
    }

    @Override
    public BuilderBasedDeserializer withObjectIdReader(ObjectIdReader oir) {
        return new BuilderBasedDeserializer(this, oir);
    }

    @Override
    public BuilderBasedDeserializer withIgnorableProperties(HashSet<String> ignorableProps) {
        return new BuilderBasedDeserializer(this, ignorableProps);
    }

    @Override
    protected BeanAsArrayBuilderDeserializer asArrayDeserializer() {
        SettableBeanProperty[] props = _beanProperties.getPropertiesInInsertionOrder();
        return new BeanAsArrayBuilderDeserializer(this, props, _buildMethod);
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
    
    /**
     * Main deserialization method for bean-based objects (POJOs).
     */
    @Override
    public final Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        
        // common case first:
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
            if (_vanillaProcessing) {
            	return finishBuild(ctxt, vanillaDeserialize(jp, ctxt, t));
            }
            Object builder = deserializeFromObject(jp, ctxt);
            return finishBuild(ctxt, builder);
        }
        // and then others, generally requiring use of @JsonCreator
        switch (t) {
        case VALUE_STRING:
            return finishBuild(ctxt, deserializeFromString(jp, ctxt));
        case VALUE_NUMBER_INT:
            return finishBuild(ctxt, deserializeFromNumber(jp, ctxt));
        case VALUE_NUMBER_FLOAT:
        	return finishBuild(ctxt, deserializeFromDouble(jp, ctxt));
        case VALUE_EMBEDDED_OBJECT:
            return jp.getEmbeddedObject();
        case VALUE_TRUE:
        case VALUE_FALSE:
            return finishBuild(ctxt, deserializeFromBoolean(jp, ctxt));
        case START_ARRAY:
            // these only work if there's a (delegating) creator...
            return finishBuild(ctxt, deserializeFromArray(jp, ctxt));
        case FIELD_NAME:
        case END_OBJECT: // added to resolve [JACKSON-319], possible related issues
            return finishBuild(ctxt, deserializeFromObject(jp, ctxt));
        default:
            throw ctxt.mappingException(getBeanClass());
        }
    }

    /**
     * Secondary deserialization method, called in cases where POJO
     * instance is created as part of deserialization, potentially
     * after collecting some or all of the properties to set.
     */
    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt,
    		Object builder)
        throws IOException, JsonProcessingException
    {
        /* Important: we call separate method which does NOT call
         * 'finishBuild()', to avoid problems with recursion
         */
        return finishBuild(ctxt, _deserialize(jp, ctxt, builder));
    }
    
    /*
    /**********************************************************
    /* Concrete deserialization methods
    /**********************************************************
     */

    protected final Object _deserialize(JsonParser jp,
            DeserializationContext ctxt, Object builder)
        throws IOException, JsonProcessingException
    {        
        if (_injectables != null) {
            injectValues(ctxt, builder);
        }
        if (_unwrappedPropertyHandler != null) {
            return deserializeWithUnwrapped(jp, ctxt, builder);
        }
        if (_externalTypeIdHandler != null) {
            return deserializeWithExternalTypeId(jp, ctxt, builder);
        }
        if (_needViewProcesing) {
            Class<?> view = ctxt.getActiveView();
            if (view != null) {
                return deserializeWithView(jp, ctxt, builder, view);
            }
        }
        JsonToken t = jp.getCurrentToken();
        // 23-Mar-2010, tatu: In some cases, we start with full JSON object too...
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            // Skip field name:
            jp.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            
            if (prop != null) { // normal case
                try {
                    builder = prop.deserializeSetAndReturn(jp, ctxt, builder);
                } catch (Exception e) {
                    wrapAndThrow(e, builder, propName, ctxt);
                }
                continue;
            }
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                jp.skipChildren();
            } else if (_anySetter != null) {
                // should we try to get return value of any setter too?
                _anySetter.deserializeAndSet(jp, ctxt, builder, propName);
                continue;
            } else {
                // Unknown: let's call handler method
                handleUnknownProperty(jp, ctxt, builder, propName);
            }
        }
        return builder;
    }
    
    /**
     * Streamlined version that is only used when no "special"
     * features are enabled.
     */
    private final Object vanillaDeserialize(JsonParser jp,
    		DeserializationContext ctxt, JsonToken t)
        throws IOException, JsonProcessingException
    {
        Object bean = _valueInstantiator.createUsingDefault(ctxt);
        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            // Skip field name:
            jp.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                try {
                    bean = prop.deserializeSetAndReturn(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
            } else {
                handleUnknownVanilla(jp, ctxt, bean, propName);
            }
        }
        return bean;
    }

    /**
     * General version used when handling needs more advanced
     * features.
     */
    @Override
    public Object deserializeFromObject(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (_nonStandardCreation) {
            if (_unwrappedPropertyHandler != null) {
                return deserializeWithUnwrapped(jp, ctxt);
            }
            if (_externalTypeIdHandler != null) {
                return deserializeWithExternalTypeId(jp, ctxt);
            }
            return deserializeFromObjectUsingNonDefault(jp, ctxt);
        }
        Object bean = _valueInstantiator.createUsingDefault(ctxt);
        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        if (_needViewProcesing) {
            Class<?> view = ctxt.getActiveView();
            if (view != null) {
                return deserializeWithView(jp, ctxt, bean, view);
            }
        }
        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            // Skip field name:
            jp.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                try {
                    bean = prop.deserializeSetAndReturn(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            /* As per [JACKSON-313], things marked as ignorable should not be
             * passed to any setter
             */
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                jp.skipChildren();
            } else if (_anySetter != null) {
                try {
                    _anySetter.deserializeAndSet(jp, ctxt, bean, propName);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            } else {
                // Unknown: let's call handler method
                handleUnknownProperty(jp, ctxt, bean, propName);         
            }
        }
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
    protected final Object _deserializeUsingPropertyBased(final JsonParser jp,
            final DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    { 
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(jp, ctxt, _objectIdReader);

        // 04-Jan-2010, tatu: May need to collect unknown properties for polymorphic cases
        TokenBuffer unknown = null;

        JsonToken t = jp.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            jp.nextToken(); // to point to value
            // creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                // Last creator property to set?
                Object value = creatorProp.deserialize(jp, ctxt);
                if (buffer.assignParameter(creatorProp.getCreatorIndex(), value)) {
                    jp.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                    Object bean;
                    try {
                        bean = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                        continue; // never gets here
                    }
                    //  polymorphic?
                    if (bean.getClass() != _beanType.getRawClass()) {
                        return handlePolymorphic(jp, ctxt, bean, unknown);
                    }
                    if (unknown != null) { // nope, just extra unknown stuff...
                        bean = handleUnknownProperties(ctxt, bean, unknown);
                    }
                    // or just clean?
                    return _deserialize(jp, ctxt, bean);
                }
                continue;
            }
            // Object Id property?
            if (buffer.readIdProperty(propName)) {
                continue;
            }
            // regular property? needs buffering
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) {
                buffer.bufferProperty(prop, prop.deserialize(jp, ctxt));
                continue;
            }
            // As per [JACKSON-313], things marked as ignorable should not be
            // passed to any setter
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                jp.skipChildren();
                continue;
            }
            // "any property"?
            if (_anySetter != null) {
                buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(jp, ctxt));
                continue;
            }
            // Ok then, let's collect the whole field; name and value
            if (unknown == null) {
                unknown = new TokenBuffer(jp.getCodec());
            }
            unknown.writeFieldName(propName);
            unknown.copyCurrentStructure(jp);
        }

        // We hit END_OBJECT, so:
        Object bean;
        try {
            bean = creator.build(ctxt, buffer);
        } catch (Exception e) {
            wrapInstantiationProblem(e, ctxt);
            return null; // never gets here
        }
        if (unknown != null) {
            // polymorphic?
            if (bean.getClass() != _beanType.getRawClass()) {
                return handlePolymorphic(null, ctxt, bean, unknown);
            }
            // no, just some extra unknown properties
            return handleUnknownProperties(ctxt, bean, unknown);
        }
        return bean;
    }
    
    /*
    /**********************************************************
    /* Deserializing when we have to consider an active View
    /**********************************************************
     */
    
    protected final Object deserializeWithView(JsonParser jp, DeserializationContext ctxt,
            Object bean, Class<?> activeView)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            // Skip field name:
            jp.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) {
                if (!prop.visibleInView(activeView)) {
                    jp.skipChildren();
                    continue;
                }
                try {
                    bean = prop.deserializeSetAndReturn(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            /* As per [JACKSON-313], things marked as ignorable should not be
             * passed to any setter
             */
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                jp.skipChildren();
            } else if (_anySetter != null) {
                _anySetter.deserializeAndSet(jp, ctxt, bean, propName);
                continue;
            } else {
                // Unknown: let's call handler method
                handleUnknownProperty(jp, ctxt, bean, propName);
            }
        }
        return bean;
    }
    
    /*
    /**********************************************************
    /* Handling for cases where we have "unwrapped" values
    /**********************************************************
     */

    /**
     * Method called when there are declared "unwrapped" properties
     * which need special handling
     */
    protected Object deserializeWithUnwrapped(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (_delegateDeserializer != null) {
            return _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(jp, ctxt));
        }
        if (_propertyBasedCreator != null) {
            return deserializeUsingPropertyBasedWithUnwrapped(jp, ctxt);
        }
        TokenBuffer tokens = new TokenBuffer(jp.getCodec());
        tokens.writeStartObject();
        Object bean = _valueInstantiator.createUsingDefault(ctxt);

        if (_injectables != null) {
            injectValues(ctxt, bean);
        }

        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        
        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            jp.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                if (activeView != null && !prop.visibleInView(activeView)) {
                    jp.skipChildren();
                    continue;
                }
                try {
                    bean = prop.deserializeSetAndReturn(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            // ignorable things should be ignored
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                jp.skipChildren();
                continue;
            }
            // but... others should be passed to unwrapped property deserializers
            tokens.writeFieldName(propName);
            tokens.copyCurrentStructure(jp);
            // how about any setter? We'll get copies but...
            if (_anySetter != null) {
                try {
                    _anySetter.deserializeAndSet(jp, ctxt, bean, propName);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
        }
        tokens.writeEndObject();
        _unwrappedPropertyHandler.processUnwrapped(jp, ctxt, bean, tokens);
        return bean;
    }    

    protected Object deserializeWithUnwrapped(JsonParser jp,
    		DeserializationContext ctxt, Object bean)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        TokenBuffer tokens = new TokenBuffer(jp.getCodec());
        tokens.writeStartObject();
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            SettableBeanProperty prop = _beanProperties.find(propName);
            jp.nextToken();
            if (prop != null) { // normal case
                if (activeView != null && !prop.visibleInView(activeView)) {
                    jp.skipChildren();
                    continue;
                }
                try {
                    bean = prop.deserializeSetAndReturn(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                jp.skipChildren();
                continue;
            }
            // but... others should be passed to unwrapped property deserializers
            tokens.writeFieldName(propName);
            tokens.copyCurrentStructure(jp);
            // how about any setter? We'll get copies but...
            if (_anySetter != null) {
                _anySetter.deserializeAndSet(jp, ctxt, bean, propName);
            }
        }
        tokens.writeEndObject();
        _unwrappedPropertyHandler.processUnwrapped(jp, ctxt, bean, tokens);
        return bean;
    }

    @SuppressWarnings("resource")
    protected Object deserializeUsingPropertyBasedWithUnwrapped(JsonParser jp,
    		DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(jp, ctxt, _objectIdReader);

        TokenBuffer tokens = new TokenBuffer(jp.getCodec());
        tokens.writeStartObject();

        JsonToken t = jp.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            jp.nextToken(); // to point to value
            // creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                // Last creator property to set?
                Object value = creatorProp.deserialize(jp, ctxt);
                if (buffer.assignParameter(creatorProp.getCreatorIndex(), value)) {
                    t = jp.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                    Object bean;
                    try {
                        bean = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                        continue; // never gets here
                    }
                    // if so, need to copy all remaining tokens into buffer
                    while (t == JsonToken.FIELD_NAME) {
                        jp.nextToken(); // to skip name
                        tokens.copyCurrentStructure(jp);
                        t = jp.nextToken();
                    }
                    tokens.writeEndObject();
                    if (bean.getClass() != _beanType.getRawClass()) {
                        // !!! 08-Jul-2011, tatu: Could probably support; but for now
                        //   it's too complicated, so bail out
                        throw ctxt.mappingException("Can not create polymorphic instances with unwrapped values");
                    }
                    return _unwrappedPropertyHandler.processUnwrapped(jp, ctxt, bean, tokens);
                }
                continue;
            }
            // Object Id property?
            if (buffer.readIdProperty(propName)) {
                continue;
            }
            // regular property? needs buffering
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) {
                buffer.bufferProperty(prop, prop.deserialize(jp, ctxt));
                continue;
            }
            /* As per [JACKSON-313], things marked as ignorable should not be
             * passed to any setter
             */
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                jp.skipChildren();
                continue;
            }
            tokens.writeFieldName(propName);
            tokens.copyCurrentStructure(jp);
            // "any property"?
            if (_anySetter != null) {
                buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(jp, ctxt));
            }
        }

        // We hit END_OBJECT, so:
        Object bean;
        // !!! 15-Feb-2012, tatu: Need to modify creator to use Builder!
        try {
            bean = creator.build(ctxt, buffer);
        } catch (Exception e) {
            wrapInstantiationProblem(e, ctxt);
            return null; // never gets here
        }
        return _unwrappedPropertyHandler.processUnwrapped(jp, ctxt, bean, tokens);
    }

    /*
    /**********************************************************
    /* Handling for cases where we have property/-ies with
    /* external type id
    /**********************************************************
     */
    
    protected Object deserializeWithExternalTypeId(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (_propertyBasedCreator != null) {
            return deserializeUsingPropertyBasedWithExternalTypeId(jp, ctxt);
        }
        return deserializeWithExternalTypeId(jp, ctxt, _valueInstantiator.createUsingDefault(ctxt));
    }

    protected Object deserializeWithExternalTypeId(JsonParser jp,
    		DeserializationContext ctxt, Object bean)
        throws IOException, JsonProcessingException
    {
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        final ExternalTypeHandler ext = _externalTypeIdHandler.start();
        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            jp.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                if (activeView != null && !prop.visibleInView(activeView)) {
                    jp.skipChildren();
                    continue;
                }
                try {
                    bean = prop.deserializeSetAndReturn(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            // ignorable things should be ignored
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                jp.skipChildren();
                continue;
            }
            // but others are likely to be part of external type id thingy...
            if (ext.handlePropertyValue(jp, ctxt, propName, bean)) {
                continue;
            }
            // if not, the usual fallback handling:
            if (_anySetter != null) {
                try {
                    _anySetter.deserializeAndSet(jp, ctxt, bean, propName);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            } else {
                // Unknown: let's call handler method
                handleUnknownProperty(jp, ctxt, bean, propName);         
            }
        }
        // and when we get this far, let's try finalizing the deal:
        return ext.complete(jp, ctxt, bean);
    }        

    protected Object deserializeUsingPropertyBasedWithExternalTypeId(JsonParser jp,
    		DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // !!! 04-Mar-2012, TODO: Need to fix -- will not work as is...
        throw new IllegalStateException("Deserialization with Builder, External type id, @JsonCreator not yet implemented");
    }
}
