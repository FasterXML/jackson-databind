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

    /**
     * Type that the builder will produce, target type; as opposed to
     * `handledType()` which refers to Builder class.
     *
     * @since 2.9
     */
    protected final JavaType _targetType;

    /*
    /**********************************************************
    /* Life-cycle, construction, initialization
    /**********************************************************
     */

    /**
     * Constructor used by {@link BeanDeserializerBuilder}.
     */
    public BuilderBasedDeserializer(BeanDeserializerBuilder builder,
            BeanDescription beanDesc, JavaType targetType,
            BeanPropertyMap properties, Map<String, SettableBeanProperty> backRefs,
            Set<String> ignorableProps, boolean ignoreAllUnknown,
            boolean hasViews)
    {
        super(builder, beanDesc, properties, backRefs,
                ignorableProps, ignoreAllUnknown, hasViews);
        _targetType = targetType;
        _buildMethod = builder.getBuildMethod();
        // 05-Mar-2012, tatu: Cannot really make Object Ids work with builders, not yet anyway
        if (_objectIdReader != null) {
            throw new IllegalArgumentException("Cannot use Object Id with Builder-based deserialization (type "
                    +beanDesc.getType()+")");
        }
    }

    /**
     * @deprecated Since 2.9
     */
    @Deprecated
    public BuilderBasedDeserializer(BeanDeserializerBuilder builder,
            BeanDescription beanDesc,
            BeanPropertyMap properties, Map<String, SettableBeanProperty> backRefs,
            Set<String> ignorableProps, boolean ignoreAllUnknown,
            boolean hasViews)
    {
        this(builder, beanDesc,
                beanDesc.getType(), // Wrong! But got no access via `BeanDeserializerBuilder`
                properties, backRefs, ignorableProps, ignoreAllUnknown, hasViews);
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
        _targetType = src._targetType;
    }

    protected BuilderBasedDeserializer(BuilderBasedDeserializer src, NameTransformer unwrapper) {
        super(src, unwrapper);
        _buildMethod = src._buildMethod;
        _targetType = src._targetType;
    }

    public BuilderBasedDeserializer(BuilderBasedDeserializer src, ObjectIdReader oir) {
        super(src, oir);
        _buildMethod = src._buildMethod;
        _targetType = src._targetType;
    }

    public BuilderBasedDeserializer(BuilderBasedDeserializer src, Set<String> ignorableProps) {
        super(src, ignorableProps);
        _buildMethod = src._buildMethod;
        _targetType = src._targetType;
    }

    public BuilderBasedDeserializer(BuilderBasedDeserializer src, BeanPropertyMap props) {
        super(src, props);
        _buildMethod = src._buildMethod;
        _targetType = src._targetType;
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
    public BeanDeserializerBase withObjectIdReader(ObjectIdReader oir) {
        return new BuilderBasedDeserializer(this, oir);
    }

    @Override
    public BeanDeserializerBase withIgnorableProperties(Set<String> ignorableProps) {
        return new BuilderBasedDeserializer(this, ignorableProps);
    }

    @Override
    public BeanDeserializerBase withBeanProperties(BeanPropertyMap props) {
        return new BuilderBasedDeserializer(this, props);
    }

    @Override
    protected BeanDeserializerBase asArrayDeserializer() {
        SettableBeanProperty[] props = _beanProperties.getPropertiesInInsertionOrder();
        return new BeanAsArrayBuilderDeserializer(this, _targetType, props, _buildMethod);
    }

    /*
    /**********************************************************
    /* JsonDeserializer implementation
    /**********************************************************
     */

    @Override // since 2.9
    public Boolean supportsUpdate(DeserializationConfig config) {
        // 26-Oct-2016, tatu: No, we can't merge Builder-based POJOs as of now
        return Boolean.FALSE;
    }
    
    /*
    /**********************************************************
    /* JsonDeserializer implementation
    /**********************************************************
     */

    protected final Object finishBuild(DeserializationContext ctxt, Object builder)
            throws IOException
    {
        // As per [databind#777], allow returning builder itself
        if (null == _buildMethod) {
            return builder;
        }
        try {
            return _buildMethod.getMember().invoke(builder, (Object[]) null);
        } catch (Exception e) {
            return wrapInstantiationProblem(e, ctxt);
        }
    }

    /**
     * Main deserialization method for bean-based objects (POJOs).
     */
    @Override
    public final Object deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // common case first:
        if (p.isExpectedStartObjectToken()) {
            JsonToken t = p.nextToken();
            if (_vanillaProcessing) {
                return finishBuild(ctxt, vanillaDeserialize(p, ctxt, t));
            }
            Object builder = deserializeFromObject(p, ctxt);
            return finishBuild(ctxt, builder);
        }
        // and then others, generally requiring use of @JsonCreator
        switch (p.getCurrentTokenId()) {
        case JsonTokenId.ID_STRING:
            return finishBuild(ctxt, deserializeFromString(p, ctxt));
        case JsonTokenId.ID_NUMBER_INT:
            return finishBuild(ctxt, deserializeFromNumber(p, ctxt));
        case JsonTokenId.ID_NUMBER_FLOAT:
            return finishBuild(ctxt, deserializeFromDouble(p, ctxt));
        case JsonTokenId.ID_EMBEDDED_OBJECT:
            return p.getEmbeddedObject();
        case JsonTokenId.ID_TRUE:
        case JsonTokenId.ID_FALSE:
            return finishBuild(ctxt, deserializeFromBoolean(p, ctxt));
        case JsonTokenId.ID_START_ARRAY:
            // these only work if there's a (delegating) creator...
            return finishBuild(ctxt, deserializeFromArray(p, ctxt));
        case JsonTokenId.ID_FIELD_NAME:
        case JsonTokenId.ID_END_OBJECT:
            return finishBuild(ctxt, deserializeFromObject(p, ctxt));
        default:
        }
        return ctxt.handleUnexpectedToken(handledType(), p);
    }

    /**
     * Secondary deserialization method, called in cases where POJO
     * instance is created as part of deserialization, potentially
     * after collecting some or all of the properties to set.
     */
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt,
    		Object value) throws IOException
    {
        // 26-Oct-2016, tatu: I cannot see any of making this actually
        //    work correctly, so let's indicate problem right away
        JavaType valueType = _targetType;
        // Did they try to give us builder?
        Class<?> builderRawType = handledType();
        Class<?> instRawType = value.getClass();
        if (builderRawType.isAssignableFrom(instRawType)) {
            return ctxt.reportBadDefinition(valueType, String.format(
                    "Deserialization of %s by passing existing Builder (%s) instance not supported",
                    valueType, builderRawType.getName()));
        }
        return ctxt.reportBadDefinition(valueType, String.format(
                "Deserialization of %s by passing existing instance (of %s) not supported",
                valueType, instRawType.getName()));
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
        Object bean = _valueInstantiator.createUsingDefault(ctxt);
        for (; p.getCurrentToken() != JsonToken.END_OBJECT; p.nextToken()) {
            String propName = p.getCurrentName();
            // Skip field name:
            p.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                try {
                    bean = prop.deserializeSetAndReturn(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
            } else {
                handleUnknownVanilla(p, ctxt, bean, propName);
            }
        }
        return bean;
    }

    /**
     * General version used when handling needs more advanced
     * features.
     */
    @Override
    public Object deserializeFromObject(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        if (_nonStandardCreation) {
            if (_unwrappedPropertyHandler != null) {
                return deserializeWithUnwrapped(p, ctxt);
            }
            if (_externalTypeIdHandler != null) {
                return deserializeWithExternalTypeId(p, ctxt);
            }
            return deserializeFromObjectUsingNonDefault(p, ctxt);
        }
        Object bean = _valueInstantiator.createUsingDefault(ctxt);
        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        if (_needViewProcesing) {
            Class<?> view = ctxt.getActiveView();
            if (view != null) {
                return deserializeWithView(p, ctxt, bean, view);
            }
        }
        for (; p.getCurrentToken() != JsonToken.END_OBJECT; p.nextToken()) {
            String propName = p.getCurrentName();
            // Skip field name:
            p.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                try {
                    bean = prop.deserializeSetAndReturn(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            handleUnknownVanilla(p, ctxt, bean, propName);
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
     *
     * @return Builder instance constructed
     */
    @Override
    @SuppressWarnings("resource")
    protected final Object _deserializeUsingPropertyBased(final JsonParser p,
            final DeserializationContext ctxt)
        throws IOException
    { 
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;

        // 04-Jan-2010, tatu: May need to collect unknown properties for polymorphic cases
        TokenBuffer unknown = null;

        JsonToken t = p.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.getCurrentName();
            p.nextToken(); // to point to value
            // creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                if ((activeView != null) && !creatorProp.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                // Last creator property to set?
                if (buffer.assignParameter(creatorProp, creatorProp.deserialize(p, ctxt))) {
                    p.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                    Object builder;
                    try {
                        builder = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                        continue; // never gets here
                    }
                    //  polymorphic?
                    if (builder.getClass() != _beanType.getRawClass()) {
                        return handlePolymorphic(p, ctxt, builder, unknown);
                    }
                    if (unknown != null) { // nope, just extra unknown stuff...
                        builder = handleUnknownProperties(ctxt, builder, unknown);
                    }
                    // or just clean?
                    return _deserialize(p, ctxt, builder);
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
            // As per [JACKSON-313], things marked as ignorable should not be
            // passed to any setter
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, handledType(), propName);
                continue;
            }
            // "any property"?
            if (_anySetter != null) {
                buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(p, ctxt));
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
        Object builder;
        try {
            builder = creator.build(ctxt, buffer);
        } catch (Exception e) {
            builder = wrapInstantiationProblem(e, ctxt);
        }
        if (unknown != null) {
            // polymorphic?
            if (builder.getClass() != _beanType.getRawClass()) {
                return handlePolymorphic(null, ctxt, builder, unknown);
            }
            // no, just some extra unknown properties
            return handleUnknownProperties(ctxt, builder, unknown);
        }
        return builder;
    }

    @SuppressWarnings("resource")
    protected final Object _deserialize(JsonParser p,
            DeserializationContext ctxt, Object builder) throws IOException
    {        
        if (_injectables != null) {
            injectValues(ctxt, builder);
        }
        if (_unwrappedPropertyHandler != null) {
            if (p.hasToken(JsonToken.START_OBJECT)) {
                p.nextToken();
            }
            TokenBuffer tokens = new TokenBuffer(p, ctxt);
            tokens.writeStartObject();
            return deserializeWithUnwrapped(p, ctxt, builder, tokens);
        }
        if (_externalTypeIdHandler != null) {
            return deserializeWithExternalTypeId(p, ctxt, builder);
        }
        if (_needViewProcesing) {
            Class<?> view = ctxt.getActiveView();
            if (view != null) {
                return deserializeWithView(p, ctxt, builder, view);
            }
        }
        JsonToken t = p.getCurrentToken();
        // 23-Mar-2010, tatu: In some cases, we start with full JSON object too...
        if (t == JsonToken.START_OBJECT) {
            t = p.nextToken();
        }
        for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.getCurrentName();
            // Skip field name:
            p.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            
            if (prop != null) { // normal case
                try {
                    builder = prop.deserializeSetAndReturn(p, ctxt, builder);
                } catch (Exception e) {
                    wrapAndThrow(e, builder, propName, ctxt);
                }
                continue;
            }
            handleUnknownVanilla(p, ctxt, handledType(), propName);
        }
        return builder;
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
        JsonToken t = p.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.getCurrentName();
            // Skip field name:
            p.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) {
                if (!prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    bean = prop.deserializeSetAndReturn(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            handleUnknownVanilla(p, ctxt, bean, propName);
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
        Object bean = _valueInstantiator.createUsingDefault(ctxt);

        if (_injectables != null) {
            injectValues(ctxt, bean);
        }

        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;

        for (; p.getCurrentToken() != JsonToken.END_OBJECT; p.nextToken()) {
            String propName = p.getCurrentName();
            p.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                if (activeView != null && !prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    bean = prop.deserializeSetAndReturn(p, ctxt, bean);
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
        return _unwrappedPropertyHandler.processUnwrapped(p, ctxt, bean, tokens);
    }

    @SuppressWarnings("resource")
    protected Object deserializeUsingPropertyBasedWithUnwrapped(JsonParser p,
    		DeserializationContext ctxt)
        throws IOException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);

        TokenBuffer tokens = new TokenBuffer(p, ctxt);
        tokens.writeStartObject();
        Object builder = null;

        JsonToken t = p.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.getCurrentName();
            p.nextToken(); // to point to value
            // creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                // Last creator property to set?
                if (buffer.assignParameter(creatorProp, creatorProp.deserialize(p, ctxt))) {
                    t = p.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                    try {
                        builder = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                        continue; // never gets here
                    }
                    if (builder.getClass() != _beanType.getRawClass()) {
                        return handlePolymorphic(p, ctxt, builder, tokens);
                    }
                    return deserializeWithUnwrapped(p, ctxt, builder, tokens);
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
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, handledType(), propName);
                continue;
            }
            tokens.writeFieldName(propName);
            tokens.copyCurrentStructure(p);
            // "any property"?
            if (_anySetter != null) {
                buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(p, ctxt));
            }
        }
        // We hit END_OBJECT, so:
        if (builder == null) {
            try {
                builder = creator.build(ctxt, buffer);
            } catch (Exception e) {
                return wrapInstantiationProblem(e, ctxt);
            }
        }
        return _unwrappedPropertyHandler.processUnwrapped(p, ctxt, builder, tokens);
    }

    protected Object deserializeWithUnwrapped(JsonParser p,
            DeserializationContext ctxt, Object builder, TokenBuffer tokens)
        throws IOException
    {
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        for (JsonToken t = p.getCurrentToken(); t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.getCurrentName();
            SettableBeanProperty prop = _beanProperties.find(propName);
            p.nextToken();
            if (prop != null) { // normal case
                if (activeView != null && !prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    builder = prop.deserializeSetAndReturn(p, ctxt, builder);
                } catch (Exception e) {
                    wrapAndThrow(e, builder, propName, ctxt);
                }
                continue;
            }
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, builder, propName);
                continue;
            }
            // but... others should be passed to unwrapped property deserializers
            tokens.writeFieldName(propName);
            tokens.copyCurrentStructure(p);
            // how about any setter? We'll get copies but...
            if (_anySetter != null) {
                _anySetter.deserializeAndSet(p, ctxt, builder, propName);
            }
        }
        tokens.writeEndObject();
        return _unwrappedPropertyHandler.processUnwrapped(p, ctxt, builder, tokens);
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
        return deserializeWithExternalTypeId(p, ctxt, _valueInstantiator.createUsingDefault(ctxt));
    }

    protected Object deserializeWithExternalTypeId(JsonParser p,
    		DeserializationContext ctxt, Object bean)
        throws IOException
    {
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        final ExternalTypeHandler ext = _externalTypeIdHandler.start();

        for (JsonToken t = p.getCurrentToken(); t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.getCurrentName();
            t = p.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                // May have property AND be used as external type id:
                if (t.isScalarValue()) {
                    ext.handleTypePropertyValue(p, ctxt, propName, bean);
                }
                if (activeView != null && !prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    bean = prop.deserializeSetAndReturn(p, ctxt, bean);
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
            } else {
                // Unknown: let's call handler method
                handleUnknownProperty(p, ctxt, bean, propName);
            }
        }
        // and when we get this far, let's try finalizing the deal:
        return ext.complete(p, ctxt, bean);
    }

    protected Object deserializeUsingPropertyBasedWithExternalTypeId(JsonParser p,
    		DeserializationContext ctxt)
        throws IOException
    {
        // !!! 04-Mar-2012, TODO: Need to fix -- will not work as is...
        JavaType t = _targetType;
        return ctxt.reportBadDefinition(t, String.format(
                "Deserialization (of %s) with Builder, External type id, @JsonCreator not yet implemented",
                t));
    }
}
