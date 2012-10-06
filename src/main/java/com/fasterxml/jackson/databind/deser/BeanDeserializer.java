package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.*;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Deserializer class that can deserialize instances of
 * arbitrary bean objects, usually from JSON Object structs,
 * but possibly also from simple types like String values.
 */
public class BeanDeserializer
    extends BeanDeserializerBase
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Life-cycle, construction, initialization
    /**********************************************************
     */

    /**
     * Constructor used by {@link BeanDeserializerBuilder}.
     */
    public BeanDeserializer(BeanDeserializerBuilder builder,
            BeanDescription beanDesc,
            BeanPropertyMap properties, Map<String, SettableBeanProperty> backRefs,
            HashSet<String> ignorableProps, boolean ignoreAllUnknown,
            boolean hasViews)
    {
        super(builder, beanDesc, properties, backRefs,
                ignorableProps, ignoreAllUnknown, hasViews);
    }

    /**
     * Copy-constructor that can be used by sub-classes to allow
     * copy-on-write style copying of settings of an existing instance.
     */
    protected BeanDeserializer(BeanDeserializerBase src) {
    	super(src, src._ignoreAllUnknown);
    }

    protected BeanDeserializer(BeanDeserializerBase src, boolean ignoreAllUnknown) {
        super(src, ignoreAllUnknown);
    }
    
    protected BeanDeserializer(BeanDeserializerBase src, NameTransformer unwrapper) {
    	super(src, unwrapper);
    }

    public BeanDeserializer(BeanDeserializerBase src, ObjectIdReader oir) {
    	super(src, oir);
    }

    public BeanDeserializer(BeanDeserializerBase src, HashSet<String> ignorableProps) {
        super(src, ignorableProps);
    }
    
    @Override
    public JsonDeserializer<Object> unwrappingDeserializer(NameTransformer unwrapper)
    {
        /* bit kludgy but we don't want to accidentally change type; sub-classes
         * MUST override this method to support unwrapped properties...
         */
        if (getClass() != BeanDeserializer.class) {
            return this;
        }
        /* main thing really is to just enforce ignoring of unknown
         * properties; since there may be multiple unwrapped values
         * and properties for all may be interleaved...
         */
        return new BeanDeserializer(this, unwrapper);
    }

    @Override
    public BeanDeserializer withObjectIdReader(ObjectIdReader oir) {
        return new BeanDeserializer(this, oir);
    }

    @Override
    public BeanDeserializer withIgnorableProperties(HashSet<String> ignorableProps) {
        return new BeanDeserializer(this, ignorableProps);
    }

    @Override
    protected BeanDeserializerBase asArrayDeserializer() {
        SettableBeanProperty[] props = _beanProperties.getPropertiesInInsertionOrder();
        return new BeanAsArrayDeserializer(this, props);
    }
    
    /*
    /**********************************************************
    /* JsonDeserializer implementation
    /**********************************************************
     */

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
                return vanillaDeserialize(jp, ctxt, t);
            }
            if (_objectIdReader != null) {
                return deserializeWithObjectId(jp, ctxt);
            }
            return deserializeFromObject(jp, ctxt);
        }
        if (t == null) {
            return _missingToken(jp, ctxt);
        }
        // and then others, generally requiring use of @JsonCreator
        switch (t) {
        case VALUE_STRING:
            return deserializeFromString(jp, ctxt);
        case VALUE_NUMBER_INT:
            return deserializeFromNumber(jp, ctxt);
        case VALUE_NUMBER_FLOAT:
	    return deserializeFromDouble(jp, ctxt);
        case VALUE_EMBEDDED_OBJECT:
            return jp.getEmbeddedObject();
        case VALUE_TRUE:
        case VALUE_FALSE:
            return deserializeFromBoolean(jp, ctxt);
        case START_ARRAY:
            // these only work if there's a (delegating) creator...
            return deserializeFromArray(jp, ctxt);
        case FIELD_NAME:
        case END_OBJECT: // added to resolve [JACKSON-319], possible related issues
            if (_vanillaProcessing) {
                return vanillaDeserialize(jp, ctxt, t);
            }
            if (_objectIdReader != null) {
                return deserializeWithObjectId(jp, ctxt);
            }
            return deserializeFromObject(jp, ctxt);
	}
        throw ctxt.mappingException(getBeanClass());
    }

    private Object _missingToken(JsonParser jp, DeserializationContext ctxt)
        throws JsonProcessingException
    {
        throw ctxt.endOfInputException(getBeanClass());
    }
    
    /**
     * Secondary deserialization method, called in cases where POJO
     * instance is created as part of deserialization, potentially
     * after collecting some or all of the properties to set.
     */
    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt, Object bean)
        throws IOException, JsonProcessingException
    {
        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        if (_unwrappedPropertyHandler != null) {
            return deserializeWithUnwrapped(jp, ctxt, bean);
        }
        if (_externalTypeIdHandler != null) {
            return deserializeWithExternalTypeId(jp, ctxt, bean);
        }
        JsonToken t = jp.getCurrentToken();
        // 23-Mar-2010, tatu: In some cases, we start with full JSON object too...
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        if (_needViewProcesing) {
            Class<?> view = ctxt.getActiveView();
            if (view != null) {
                return deserializeWithView(jp, ctxt, bean, view);
            }
        }
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            // Skip field name:
            jp.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            
            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
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
    /* Concrete deserialization methods
    /**********************************************************
     */

    /**
     * Streamlined version that is only used when no "special"
     * features are enabled.
     */
    private final Object vanillaDeserialize(JsonParser jp,
    		DeserializationContext ctxt, JsonToken t)
        throws IOException, JsonProcessingException
    {
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            // Skip field name:
            jp.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
            } else {
                _vanillaDeserializeHandleUnknown(jp, ctxt, bean, propName);
            }
        }
        return bean;
    }

    /**
     * Helper method called for an unknown property, when using "vanilla"
     * processing.
     */
    private final void _vanillaDeserializeHandleUnknown(JsonParser jp, DeserializationContext ctxt,
            Object bean, String propName)
        throws IOException, JsonProcessingException
    {
        if (_ignorableProps != null && _ignorableProps.contains(propName)) {
            jp.skipChildren();
        } else if (_anySetter != null) {
            try {
                _anySetter.deserializeAndSet(jp, ctxt, bean, propName);
            } catch (Exception e) {
                wrapAndThrow(e, bean, propName, ctxt);
            }
        } else {
            // Unknown: let's call handler method
            handleUnknownProperty(jp, ctxt, bean, propName);         
        }
    }

    /**
     * General version used when handling needs more advanced
     * features.
     */
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
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
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
                    prop.deserializeAndSet(jp, ctxt, bean);
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
     * Alternative deserialization method used when we expect to see Object Id;
     * if so, we will need to ensure that the Id is seen before anything
     * else, to ensure that it is available for solving references,
     * even if JSON itself is not ordered that way. This may require
     * buffering in some cases, but usually just a simple lookup to ensure
     * that ordering is correct.
     */
    @SuppressWarnings("resource")
    protected Object deserializeWithObjectId(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        final String idPropName = _objectIdReader.propertyName;
        // First, the simple case: we point to the Object Id property
        if (idPropName.equals(jp.getCurrentName())) {
            return deserializeFromObject(jp, ctxt);
        }
        // otherwise need to reorder things
        TokenBuffer tmpBuffer = new TokenBuffer(jp.getCodec());
        TokenBuffer mergedBuffer = null;
        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            // when we match the id property, can start merging
            if (mergedBuffer == null) {
                if (idPropName.equals(propName)) {
                    mergedBuffer = new TokenBuffer(jp.getCodec());
                    mergedBuffer.writeFieldName(propName);
                    jp.nextToken();
                    mergedBuffer.copyCurrentStructure(jp);
                    mergedBuffer.append(tmpBuffer);
                    tmpBuffer = null;
                } else {
                    tmpBuffer.writeFieldName(propName);
                    jp.nextToken();
                    tmpBuffer.copyCurrentStructure(jp);
                }
            } else {
                mergedBuffer.writeFieldName(propName);
                jp.nextToken();
                mergedBuffer.copyCurrentStructure(jp);
            }
        }
        // note: we really should get merged buffer (and if not, that is likely error), but
        // for now let's allow missing case as well. Will be caught be a later stage...
        TokenBuffer buffer = (mergedBuffer == null) ? tmpBuffer : mergedBuffer;
        buffer.writeEndObject();
        // important: need to advance to point to first FIELD_NAME:
        JsonParser mergedParser = buffer.asParser();
        mergedParser.nextToken();
        return deserializeFromObject(mergedParser, ctxt);
    }

    protected Object deserializeFromObjectUsingNonDefault(JsonParser jp, DeserializationContext ctxt)
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
    
    public Object deserializeFromString(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // First things first: id Object Id is used, most likely that's it
        if (_objectIdReader != null) {
            return deserializeFromObjectId(jp, ctxt);
        }
        
        /* Bit complicated if we have delegating creator; may need to use it,
         * or might not...
         */
        if (_delegateDeserializer != null) {
            if (!_valueInstantiator.canCreateFromString()) {
                Object bean = _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(jp, ctxt));
                if (_injectables != null) {
                    injectValues(ctxt, bean);
                }
                return bean;
            }
        }
        return _valueInstantiator.createFromString(ctxt, jp.getText());
    }

    public Object deserializeFromNumber(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // First things first: id Object Id is used, most likely that's it
        if (_objectIdReader != null) {
            return deserializeFromObjectId(jp, ctxt);
        }

        switch (jp.getNumberType()) {
        case INT:
            if (_delegateDeserializer != null) {
                if (!_valueInstantiator.canCreateFromInt()) {
                    Object bean = _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(jp, ctxt));
                    if (_injectables != null) {
                        injectValues(ctxt, bean);
                    }
                    return bean;
                }
            }
            return _valueInstantiator.createFromInt(ctxt, jp.getIntValue());
        case LONG:
            if (_delegateDeserializer != null) {
                if (!_valueInstantiator.canCreateFromInt()) {
                    Object bean = _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(jp, ctxt));
                    if (_injectables != null) {
                        injectValues(ctxt, bean);
                    }
                    return bean;
                }
            }
            return _valueInstantiator.createFromLong(ctxt, jp.getLongValue());
    	}
        // actually, could also be BigInteger, so:
        if (_delegateDeserializer != null) {
            Object bean = _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(jp, ctxt));
            if (_injectables != null) {
                injectValues(ctxt, bean);
            }
            return bean;
        }
        throw ctxt.instantiationException(getBeanClass(), "no suitable creator method found to deserialize from JSON integer number");
    }

    /**
     * Method called to deserialize POJO value from a JSON floating-point
     * number.
     */
    public Object deserializeFromDouble(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        switch (jp.getNumberType()) {
        case FLOAT: // no separate methods for taking float...
        case DOUBLE:
            if (_delegateDeserializer != null) {
                if (!_valueInstantiator.canCreateFromDouble()) {
                    Object bean = _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(jp, ctxt));
                    if (_injectables != null) {
                        injectValues(ctxt, bean);
                    }
                    return bean;
                }
            }
            return _valueInstantiator.createFromDouble(ctxt, jp.getDoubleValue());
        }
        // actually, could also be BigDecimal, so:
        if (_delegateDeserializer != null) {
            return _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(jp, ctxt));
        }
        throw ctxt.instantiationException(getBeanClass(), "no suitable creator method found to deserialize from JSON floating-point number");
    }

    /**
     * Method called to deserialize POJO value from a JSON boolean value (true, false)
     */
    public Object deserializeFromBoolean(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (_delegateDeserializer != null) {
            if (!_valueInstantiator.canCreateFromBoolean()) {
                Object bean = _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(jp, ctxt));
                if (_injectables != null) {
                    injectValues(ctxt, bean);
                }
                return bean;
            }
        }
        boolean value = (jp.getCurrentToken() == JsonToken.VALUE_TRUE);
        return _valueInstantiator.createFromBoolean(ctxt, value);
    }

    public Object deserializeFromArray(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
    	if (_delegateDeserializer != null) {
    	    try {
    	        Object bean = _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(jp, ctxt));
    	        if (_injectables != null) {
    	            injectValues(ctxt, bean);
    	        }
    	        return bean;
            } catch (Exception e) {
                wrapInstantiationProblem(e, ctxt);
            }
    	}
    	throw ctxt.mappingException(getBeanClass());
    }

    /**
     * Method called to deserialize bean using "property-based creator":
     * this means that a non-default constructor or factory method is
     * called, and then possibly other setters. The trick is that
     * values for creator method need to be buffered, first; and 
     * due to non-guaranteed ordering possibly some other properties
     * as well.
     */
    protected final Object _deserializeUsingPropertyBased(final JsonParser jp, final DeserializationContext ctxt)
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
                        bean = null; // never gets here
                    }
                    //  polymorphic?
		    if (bean.getClass() != _beanType.getRawClass()) {
			return handlePolymorphic(jp, ctxt, bean, unknown);
		    }
                    if (unknown != null) { // nope, just extra unknown stuff...
                        bean = handleUnknownProperties(ctxt, bean, unknown);
		    }
		    // or just clean?
                    return deserialize(jp, ctxt, bean);
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
            bean =  creator.build(ctxt, buffer);
        } catch (Exception e) {
            wrapInstantiationProblem(e, ctxt);
            bean = null; // never gets here
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
    
    /**
     * Method called in cases where we may have polymorphic deserialization
     * case: that is, type of Creator-constructed bean is not the type
     * of deserializer itself. It should be a sub-class or implementation
     * class; either way, we may have more specific deserializer to use
     * for handling it.
     *
     * @param jp (optional) If not null, parser that has more properties to handle
     *   (in addition to buffered properties); if null, all properties are passed
     *   in buffer
     */
    protected Object handlePolymorphic(JsonParser jp, DeserializationContext ctxt,                                          
            Object bean, TokenBuffer unknownTokens)
        throws IOException, JsonProcessingException
    {  
        // First things first: maybe there is a more specific deserializer available?
        JsonDeserializer<Object> subDeser = _findSubclassDeserializer(ctxt, bean, unknownTokens);
        if (subDeser != null) {
            if (unknownTokens != null) {
                // need to add END_OBJECT marker first
                unknownTokens.writeEndObject();
                JsonParser p2 = unknownTokens.asParser();
                p2.nextToken(); // to get to first data field
                bean = subDeser.deserialize(p2, ctxt, bean);
            }
            // Original parser may also have some leftovers
            if (jp != null) {
                bean = subDeser.deserialize(jp, ctxt, bean);
            }
            return bean;
        }
        // nope; need to use this deserializer. Unknowns we've seen so far?
        if (unknownTokens != null) {
            bean = handleUnknownProperties(ctxt, bean, unknownTokens);
        }
        // and/or things left to process via main parser?
        if (jp != null) {
            bean = deserialize(jp, ctxt, bean);
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
                    prop.deserializeAndSet(jp, ctxt, bean);
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
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);

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
                    prop.deserializeAndSet(jp, ctxt, bean);
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

    protected Object deserializeWithUnwrapped(JsonParser jp, DeserializationContext ctxt, Object bean)
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
                    prop.deserializeAndSet(jp, ctxt, bean);
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
    protected Object deserializeUsingPropertyBasedWithUnwrapped(JsonParser jp, DeserializationContext ctxt)
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
                        tokens.close();
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
        try {
            bean =  creator.build(ctxt, buffer);
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
    
    protected Object deserializeWithExternalTypeId(JsonParser jp, DeserializationContext ctxt,
            Object bean)
        throws IOException, JsonProcessingException
    {
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        final ExternalTypeHandler ext = _externalTypeIdHandler.start();
        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            jp.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                // [JACKSON-831]: may have property AND be used as external type id:
                if (jp.getCurrentToken().isScalarValue()) {
                    ext.handleTypePropertyValue(jp, ctxt, propName, bean);
                }
                if (activeView != null && !prop.visibleInView(activeView)) {
                    jp.skipChildren();
                    continue;
                }
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
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
            }
            // Unknown: let's call handler method
            handleUnknownProperty(jp, ctxt, bean, propName);         
        }
        // and when we get this far, let's try finalizing the deal:
        return ext.complete(jp, ctxt, bean);
    }        

    @SuppressWarnings("resource")
    protected Object deserializeUsingPropertyBasedWithExternalTypeId(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        final ExternalTypeHandler ext = _externalTypeIdHandler.start();
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
                // first: let's check to see if this might be part of value with external type id:
                if (ext.handlePropertyValue(jp, ctxt, propName, buffer)) {
                    ;
                } else {
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
                        if (bean.getClass() != _beanType.getRawClass()) {
                            // !!! 08-Jul-2011, tatu: Could probably support; but for now
                            //   it's too complicated, so bail out
                            throw ctxt.mappingException("Can not create polymorphic instances with unwrapped values");
                        }
                        return ext.complete(jp, ctxt, bean);
                    }
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
            // external type id (or property that depends on it)?
            if (ext.handlePropertyValue(jp, ctxt, propName, null)) {
                continue;
            }
            /* As per [JACKSON-313], things marked as ignorable should not be
             * passed to any setter
             */
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                jp.skipChildren();
                continue;
            }
            // "any property"?
            if (_anySetter != null) {
                buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(jp, ctxt));
            }
        }

        // We hit END_OBJECT; resolve the pieces:
        try {
            return ext.complete(jp, ctxt, buffer, creator);
        } catch (Exception e) {
            wrapInstantiationProblem(e, ctxt);
            return null; // never gets here
        }
    }
}
