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
 */
public class BeanDeserializer
    extends BeanDeserializerBase
    implements java.io.Serializable
{
    /* TODOs for future versions:
     * 
     * For 2.8?
     *
     * - New method in JsonDeserializer (deserializeNext()) to allow use of more
     *   efficient 'nextXxx()' method `JsonParser` provides.
     *
     * Also: need to ensure efficient impl of those methods for Smile, CBOR
     * at least (in addition to JSON)
     */

    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Life-cycle, construction, initialization
    /**********************************************************
     */

    /**
     * Constructor used by {@link BeanDeserializerBuilder}.
     */
    public BeanDeserializer(BeanDeserializerBuilder builder, BeanDescription beanDesc,
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
     *<p>
     * NOTE: was declared 'final' in 2.2; should NOT be to let extensions
     * like Afterburner change definition.
     */
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // common case first
        if (p.isExpectedStartObjectToken()) {
            if (_vanillaProcessing) {
                return vanillaDeserialize(p, ctxt, p.nextToken());
            }
            // 23-Sep-2015, tatu: This is wrong at some many levels, but for now... it is
            //    what it is, including "expected behavior".
            p.nextToken();
            if (_objectIdReader != null) {
                return deserializeWithObjectId(p, ctxt);
            }
            return deserializeFromObject(p, ctxt);
        }
        return _deserializeOther(p, ctxt, p.getCurrentToken());
    }

    protected final Object _deserializeOther(JsonParser p, DeserializationContext ctxt,
            JsonToken t) throws IOException
    {
        // and then others, generally requiring use of @JsonCreator
        switch (t) {
        case VALUE_STRING:
            return deserializeFromString(p, ctxt);
        case VALUE_NUMBER_INT:
            return deserializeFromNumber(p, ctxt);
        case VALUE_NUMBER_FLOAT:
	    return deserializeFromDouble(p, ctxt);
        case VALUE_EMBEDDED_OBJECT:
            return deserializeFromEmbedded(p, ctxt);
        case VALUE_TRUE:
        case VALUE_FALSE:
            return deserializeFromBoolean(p, ctxt);

        case VALUE_NULL:
            return deserializeFromNull(p, ctxt);
        case START_ARRAY:
            // these only work if there's a (delegating) creator...
            return deserializeFromArray(p, ctxt);
        case FIELD_NAME:
        case END_OBJECT: // added to resolve [JACKSON-319], possible related issues
            if (_vanillaProcessing) {
                return vanillaDeserialize(p, ctxt, t);
            }
            if (_objectIdReader != null) {
                return deserializeWithObjectId(p, ctxt);
            }
            return deserializeFromObject(p, ctxt);
        default:
        }
        throw ctxt.mappingException(handledType());
    }

    protected Object _missingToken(JsonParser p, DeserializationContext ctxt) throws IOException {
        throw ctxt.endOfInputException(handledType());
    }

    /**
     * Secondary deserialization method, called in cases where POJO
     * instance is created as part of deserialization, potentially
     * after collecting some or all of the properties to set.
     */
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object bean) throws IOException
    {
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(bean);
        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        if (_unwrappedPropertyHandler != null) {
            return deserializeWithUnwrapped(p, ctxt, bean);
        }
        if (_externalTypeIdHandler != null) {
            return deserializeWithExternalTypeId(p, ctxt, bean);
        }
        String propName;

        // 23-Mar-2010, tatu: In some cases, we start with full JSON object too...
        if (p.isExpectedStartObjectToken()) {
            propName = p.nextFieldName();
            if (propName == null) {
                return bean;
            }
        } else {
            if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)) {
                propName = p.getCurrentName();
            } else {
                return bean;
            }
        }
        if (_needViewProcesing) {
            Class<?> view = ctxt.getActiveView();
            if (view != null) {
                return deserializeWithView(p, ctxt, bean, view);
            }
        }
        do {
            p.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);

            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            handleUnknownVanilla(p, ctxt, bean, propName);
        } while ((propName = p.nextFieldName()) != null);
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
    private final Object vanillaDeserialize(JsonParser p,
    		DeserializationContext ctxt, JsonToken t)
        throws IOException
    {
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(bean);
        if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)) {
            String propName = p.getCurrentName();
            do {
                p.nextToken();
                SettableBeanProperty prop = _beanProperties.find(propName);

                if (prop != null) { // normal case
                    try {
                        prop.deserializeAndSet(p, ctxt, bean);
                    } catch (Exception e) {
                        wrapAndThrow(e, bean, propName, ctxt);
                    }
                    continue;
                }
                handleUnknownVanilla(p, ctxt, bean, propName);
            } while ((propName = p.nextFieldName()) != null);
        }
        return bean;
    }

    /**
     * General version used when handling needs more advanced features.
     */
    @Override
    public Object deserializeFromObject(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        /* 09-Dec-2014, tatu: As per [#622], we need to allow Object Id references
         *   to come in as JSON Objects as well; but for now assume they will
         *   be simple, single-property references, which means that we can
         *   recognize them without having to buffer anything.
         *   Once again, if we must, we can do more complex handling with buffering,
         *   but let's only do that if and when that becomes necessary.
         */
        if (_objectIdReader != null && _objectIdReader.maySerializeAsObject()) {
            if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)
                    && _objectIdReader.isValidReferencePropertyName(p.getCurrentName(), p)) {
                return deserializeFromObjectId(p, ctxt);
            }
        }
        if (_nonStandardCreation) {
            if (_unwrappedPropertyHandler != null) {
                return deserializeWithUnwrapped(p, ctxt);
            }
            if (_externalTypeIdHandler != null) {
                return deserializeWithExternalTypeId(p, ctxt);
            }
            Object bean = deserializeFromObjectUsingNonDefault(p, ctxt);
            if (_injectables != null) {
                injectValues(ctxt, bean);
            }
            /* 27-May-2014, tatu: I don't think view processing would work
             *   at this point, so commenting it out; but leaving in place
             *   just in case I forgot something fundamental...
             */
            /*
            if (_needViewProcesing) {
                Class<?> view = ctxt.getActiveView();
                if (view != null) {
                    return deserializeWithView(p, ctxt, bean, view);
                }
            }
            */
            return bean;
        }
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        // [databind#631]: Assign current value, to be accessible by custom deserializers
        p.setCurrentValue(bean);
        if (p.canReadObjectId()) {
            Object id = p.getObjectId();
            if (id != null) {
                _handleTypedObjectId(p, ctxt, bean, id);
            }
        }
        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        if (_needViewProcesing) {
            Class<?> view = ctxt.getActiveView();
            if (view != null) {
                return deserializeWithView(p, ctxt, bean, view);
            }
        }
        if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)) {
            String propName = p.getCurrentName();
            do {
                p.nextToken();
                SettableBeanProperty prop = _beanProperties.find(propName);
                if (prop != null) { // normal case
                    try {
                        prop.deserializeAndSet(p, ctxt, bean);
                    } catch (Exception e) {
                        wrapAndThrow(e, bean, propName, ctxt);
                    }
                    continue;
                }
                handleUnknownVanilla(p, ctxt, bean, propName);
            } while ((propName = p.nextFieldName()) != null);
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
    @SuppressWarnings("resource")
    protected Object _deserializeUsingPropertyBased(final JsonParser p, final DeserializationContext ctxt)
        throws IOException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);

        // 04-Jan-2010, tatu: May need to collect unknown properties for polymorphic cases
        TokenBuffer unknown = null;

        JsonToken t = p.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.getCurrentName();
            p.nextToken(); // to point to value
            // Object Id property?
            if (buffer.readIdProperty(propName)) {
                continue;
            }
            // creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                // Last creator property to set?
                if (buffer.assignParameter(creatorProp,
                        _deserializeWithErrorWrapping(p, ctxt, creatorProp))) {
                    p.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                    Object bean;
                    try {
                        bean = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        wrapInstantiationProblem(e, ctxt);
                        bean = null; // never gets here
                    }
                    if (bean == null) {
                        throw ctxt.instantiationException(_beanType.getRawClass(), "JSON Creator returned null");
                    }
                    // [databind#631]: Assign current value, to be accessible by custom serializers
                    p.setCurrentValue(bean);

                    //  polymorphic?
                    if (bean.getClass() != _beanType.getRawClass()) {
                        return handlePolymorphic(p, ctxt, bean, unknown);
                    }
                    if (unknown != null) { // nope, just extra unknown stuff...
                        bean = handleUnknownProperties(ctxt, bean, unknown);
                    }
                    // or just clean?
                    return deserialize(p, ctxt, bean);
                }
                continue;
            }
            // regular property? needs buffering
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) {
                buffer.bufferProperty(prop, _deserializeWithErrorWrapping(p, ctxt, prop));
                continue;
            }
            // As per [JACKSON-313], things marked as ignorable should not be
            // passed to any setter
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, handledType(), propName);
                continue;
            }
            // "any property"?
            if (_anySetter != null) {
                try {
                    buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(p, ctxt));
                } catch (Exception e) {
                    wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                }
                continue;
            }
            // Ok then, let's collect the whole field; name and value
            if (unknown == null) {
                unknown = new TokenBuffer(p, ctxt);
            }
            unknown.writeFieldName(propName);
            unknown.copyCurrentStructure(p);
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

    protected final Object _deserializeWithErrorWrapping(JsonParser p,
            DeserializationContext ctxt, SettableBeanProperty prop)
        throws IOException
    {
        try {
            return prop.deserialize(p, ctxt);
        } catch (Exception e) {
            wrapAndThrow(e, _beanType.getRawClass(), prop.getName(), ctxt);
            // never gets here, unless caller declines to throw an exception
            return null;
        }
    }

    /**
     * Helper method called for rare case of pointing to {@link JsonToken#VALUE_NULL}
     * token. While this is most often an erroneous condition, there is one specific
     * case with XML handling where polymorphic type with no properties is exposed
     * as such, and should be handled same as empty Object.
     *
     * @since 2.7
     */
    protected Object deserializeFromNull(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // 17-Dec-2015, tatu: Highly specialized case, mainly to support polymorphic
        //   "empty" POJOs deserialized from XML, where empty XML tag synthesizes a
        //   `VALUE_NULL` token.
        if (p.requiresCustomCodec()) { // not only XML module, but mostly it...
            @SuppressWarnings("resource")
            TokenBuffer tb = new TokenBuffer(p, ctxt);
            tb.writeEndObject();
            JsonParser p2 = tb.asParser(p);
            p2.nextToken(); // to point to END_OBJECT
            // note: don't have ObjectId to consider at this point, so:
            Object ob = _vanillaProcessing ? vanillaDeserialize(p2, ctxt, JsonToken.END_OBJECT)
                    : deserializeFromObject(p2, ctxt);
            p2.close();
            return ob;
        }
        throw ctxt.mappingException(handledType());
    }
    
    /*
    /**********************************************************
    /* Deserializing when we have to consider an active View
    /**********************************************************
     */

    protected final Object deserializeWithView(JsonParser p, DeserializationContext ctxt,
            Object bean, Class<?> activeView)
        throws IOException
    {
        if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)) {
            String propName = p.getCurrentName();
            do {
                p.nextToken();
                // TODO: 06-Jan-2015, tatu: try streamlining call sequences here as well
                SettableBeanProperty prop = _beanProperties.find(propName);
                if (prop != null) {
                    if (!prop.visibleInView(activeView)) {
                        p.skipChildren();
                        continue;
                    }
                    try {
                        prop.deserializeAndSet(p, ctxt, bean);
                    } catch (Exception e) {
                        wrapAndThrow(e, bean, propName, ctxt);
                    }
                    continue;
                }
                handleUnknownVanilla(p, ctxt, bean, propName);
            } while ((propName = p.nextFieldName()) != null);
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
    @SuppressWarnings("resource")
    protected Object deserializeWithUnwrapped(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        if (_delegateDeserializer != null) {
            return _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(p, ctxt));
        }
        if (_propertyBasedCreator != null) {
            return deserializeUsingPropertyBasedWithUnwrapped(p, ctxt);
        }
        TokenBuffer tokens = new TokenBuffer(p, ctxt);
        tokens.writeStartObject();
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);

        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(bean);

        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        String propName = p.hasTokenId(JsonTokenId.ID_FIELD_NAME) ? p.getCurrentName() : null;

        for (; propName != null; propName = p.nextFieldName()) {
            p.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                if (activeView != null && !prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            // Things marked as ignorable should not be passed to any setter
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, bean, propName);
                continue;
            }
            // but... others should be passed to unwrapped property deserializers
            tokens.writeFieldName(propName);
            tokens.copyCurrentStructure(p);
            // how about any setter? We'll get copies but...
            if (_anySetter != null) {
                try {
                    _anySetter.deserializeAndSet(p, ctxt, bean, propName);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
        }
        tokens.writeEndObject();
        _unwrappedPropertyHandler.processUnwrapped(p, ctxt, bean, tokens);
        return bean;
    }

    @SuppressWarnings("resource")
    protected Object deserializeWithUnwrapped(JsonParser p, DeserializationContext ctxt, Object bean)
        throws IOException
    {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            t = p.nextToken();
        }
        TokenBuffer tokens = new TokenBuffer(p, ctxt);
        tokens.writeStartObject();
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.getCurrentName();
            SettableBeanProperty prop = _beanProperties.find(propName);
            p.nextToken();
            if (prop != null) { // normal case
                if (activeView != null && !prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, bean, propName);
                continue;
            }
            // but... others should be passed to unwrapped property deserializers
            tokens.writeFieldName(propName);
            tokens.copyCurrentStructure(p);
            // how about any setter? We'll get copies but...
            if (_anySetter != null) {
                _anySetter.deserializeAndSet(p, ctxt, bean, propName);
            }
        }
        tokens.writeEndObject();
        _unwrappedPropertyHandler.processUnwrapped(p, ctxt, bean, tokens);
        return bean;
    }

    @SuppressWarnings("resource")
    protected Object deserializeUsingPropertyBasedWithUnwrapped(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);

        TokenBuffer tokens = new TokenBuffer(p, ctxt);
        tokens.writeStartObject();

        JsonToken t = p.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.getCurrentName();
            p.nextToken(); // to point to value
            // creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                // Last creator property to set?
                if (buffer.assignParameter(creatorProp, _deserializeWithErrorWrapping(p, ctxt, creatorProp))) {
                    t = p.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                    Object bean;
                    try {
                        bean = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        wrapInstantiationProblem(e, ctxt);
                        continue; // never gets here
                    }
                    // [databind#631]: Assign current value, to be accessible by custom serializers
                    p.setCurrentValue(bean);
                    // if so, need to copy all remaining tokens into buffer
                    while (t == JsonToken.FIELD_NAME) {
                        p.nextToken(); // to skip name
                        tokens.copyCurrentStructure(p);
                        t = p.nextToken();
                    }
                    tokens.writeEndObject();
                    if (bean.getClass() != _beanType.getRawClass()) {
                        // !!! 08-Jul-2011, tatu: Could probably support; but for now
                        //   it's too complicated, so bail out
                        tokens.close();
                        throw ctxt.mappingException("Can not create polymorphic instances with unwrapped values");
                    }
                    return _unwrappedPropertyHandler.processUnwrapped(p, ctxt, bean, tokens);
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
                buffer.bufferProperty(prop, _deserializeWithErrorWrapping(p, ctxt, prop));
                continue;
            }
            // Things marked as ignorable should not be passed to any setter
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, handledType(), propName);
                continue;
            }
            tokens.writeFieldName(propName);
            tokens.copyCurrentStructure(p);
            // "any property"?
            if (_anySetter != null) {
                try {
                    buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(p, ctxt));
                } catch (Exception e) {
                    wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                }
            }
        }

        // We hit END_OBJECT, so:
        Object bean;
        try {
            bean = creator.build(ctxt, buffer);
        } catch (Exception e) {
            wrapInstantiationProblem(e, ctxt);
            return null; // never gets here
        }
        return _unwrappedPropertyHandler.processUnwrapped(p, ctxt, bean, tokens);
    }

    /*
    /**********************************************************
    /* Handling for cases where we have property/-ies with
    /* external type id
    /**********************************************************
     */

    protected Object deserializeWithExternalTypeId(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        if (_propertyBasedCreator != null) {
            return deserializeUsingPropertyBasedWithExternalTypeId(p, ctxt);
        }
        if (_delegateDeserializer != null) {
            /* 24-Nov-2015, tatu: Use of delegating creator needs to have precedence, and basically
             *   external type id handling just has to be ignored, as they would relate to target
             *   type and not delegate type. Whether this works as expected is another story, but
             *   there's no other way to really mix these conflicting features.
             */
            return _valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(p, ctxt));
        }

        return deserializeWithExternalTypeId(p, ctxt, _valueInstantiator.createUsingDefault(ctxt));
    }

    protected Object deserializeWithExternalTypeId(JsonParser p, DeserializationContext ctxt,
            Object bean)
        throws IOException
    {
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        final ExternalTypeHandler ext = _externalTypeIdHandler.start();

        for (JsonToken t = p.getCurrentToken(); t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.getCurrentName();
            t = p.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                // [JACKSON-831]: may have property AND be used as external type id:
                if (t.isScalarValue()) {
                    ext.handleTypePropertyValue(p, ctxt, propName, bean);
                }
                if (activeView != null && !prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            // ignorable things should be ignored
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, bean, propName);
                continue;
            }
            // but others are likely to be part of external type id thingy...
            if (ext.handlePropertyValue(p, ctxt, propName, bean)) {
                continue;
            }
            // if not, the usual fallback handling:
            if (_anySetter != null) {
                try {
                    _anySetter.deserializeAndSet(p, ctxt, bean, propName);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            // Unknown: let's call handler method
            handleUnknownProperty(p, ctxt, bean, propName);
        }
        // and when we get this far, let's try finalizing the deal:
        return ext.complete(p, ctxt, bean);
    }

    @SuppressWarnings("resource")
    protected Object deserializeUsingPropertyBasedWithExternalTypeId(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        final ExternalTypeHandler ext = _externalTypeIdHandler.start();
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);

        TokenBuffer tokens = new TokenBuffer(p, ctxt);
        tokens.writeStartObject();

        JsonToken t = p.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.getCurrentName();
            p.nextToken(); // to point to value
            // creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                // first: let's check to see if this might be part of value with external type id:
                // 11-Sep-2015, tatu: Important; do NOT pass buffer as last arg, but null,
                //   since it is not the bean
                if (ext.handlePropertyValue(p, ctxt, propName, null)) {
                    ;
                } else {
                    // Last creator property to set?
                    if (buffer.assignParameter(creatorProp, _deserializeWithErrorWrapping(p, ctxt, creatorProp))) {
                        t = p.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                        Object bean;
                        try {
                            bean = creator.build(ctxt, buffer);
                        } catch (Exception e) {
                            wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                            continue; // never gets here
                        }
                        // if so, need to copy all remaining tokens into buffer
                        while (t == JsonToken.FIELD_NAME) {
                            p.nextToken(); // to skip name
                            tokens.copyCurrentStructure(p);
                            t = p.nextToken();
                        }
                        if (bean.getClass() != _beanType.getRawClass()) {
                            // !!! 08-Jul-2011, tatu: Could theoretically support; but for now
                            //   it's too complicated, so bail out
                            throw ctxt.mappingException("Can not create polymorphic instances with unwrapped values");
                        }
                        return ext.complete(p, ctxt, bean);
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
                buffer.bufferProperty(prop, prop.deserialize(p, ctxt));
                continue;
            }
            // external type id (or property that depends on it)?
            if (ext.handlePropertyValue(p, ctxt, propName, null)) {
                continue;
            }
            // Things marked as ignorable should not be passed to any setter
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, handledType(), propName);
                continue;
            }
            // "any property"?
            if (_anySetter != null) {
                buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(p, ctxt));
            }
        }

        // We hit END_OBJECT; resolve the pieces:
        try {
            return ext.complete(p, ctxt, buffer, creator);
        } catch (Exception e) {
            wrapInstantiationProblem(e, ctxt);
            return null; // never gets here
        }
    }
}
