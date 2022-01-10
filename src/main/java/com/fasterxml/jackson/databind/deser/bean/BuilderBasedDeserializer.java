package com.fasterxml.jackson.databind.deser.bean;

import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.sym.PropertyNameMatcher;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.ExternalTypeHandler;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdReader;
import com.fasterxml.jackson.databind.deser.impl.UnwrappedPropertyHandler;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.util.IgnorePropertiesUtil;
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
    protected final AnnotatedMethod _buildMethod;

    /**
     * Type that the builder will produce, target type; as opposed to
     * `handledType()` which refers to Builder class.
     */
    protected final JavaType _targetType;

    // @since 3.0
    protected PropertyNameMatcher _propertyNameMatcher;

    // @since 3.0
    protected SettableBeanProperty[] _propertiesByIndex;

    /**
     * State marker we need in order to avoid infinite recursion for some cases
     * (not very clean, alas, but has to do for now)
     */
    private volatile transient NameTransformer _currentlyTransforming;

    /*
    /**********************************************************************
    /* Life-cycle, construction, initialization
    /**********************************************************************
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
        this(builder, beanDesc, targetType, properties, backRefs, ignorableProps, ignoreAllUnknown, null, hasViews);
    }

    /**
     * @since 2.12
     */
    public BuilderBasedDeserializer(BeanDeserializerBuilder builder,
            BeanDescription beanDesc, JavaType targetType,
            BeanPropertyMap properties, Map<String, SettableBeanProperty> backRefs,
            Set<String> ignorableProps, boolean ignoreAllUnknown, Set<String> includableProps,
            boolean hasViews)
    {
        super(builder, beanDesc, properties, backRefs,
                ignorableProps, ignoreAllUnknown, includableProps, hasViews);
        _targetType = targetType;
        _buildMethod = builder.getBuildMethod();
        // 05-Mar-2012, tatu: Cannot really make Object Ids work with builders, not yet anyway
        if (_objectIdReader != null) {
            throw new IllegalArgumentException("Cannot use Object Id with Builder-based deserialization (type "
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
        _targetType = src._targetType;
        _propertyNameMatcher = src._propertyNameMatcher;
        _propertiesByIndex = src._propertiesByIndex;
    }

    protected BuilderBasedDeserializer(BuilderBasedDeserializer src,
            UnwrappedPropertyHandler unwrapHandler, BeanPropertyMap renamedProperties,
            boolean ignoreAllUnknown) {
        super(src, unwrapHandler, renamedProperties, ignoreAllUnknown);
        _buildMethod = src._buildMethod;
        _targetType = src._targetType;
        _propertyNameMatcher = _beanProperties.getNameMatcher();
        _propertiesByIndex = _beanProperties.getNameMatcherProperties();
    }

    public BuilderBasedDeserializer(BuilderBasedDeserializer src, ObjectIdReader oir) {
        super(src, oir);
        _buildMethod = src._buildMethod;
        _targetType = src._targetType;
        _propertyNameMatcher = src._propertyNameMatcher;
        _propertiesByIndex = src._propertiesByIndex;
    }

    public BuilderBasedDeserializer(BuilderBasedDeserializer src, Set<String> ignorableProps) {
        this(src, ignorableProps, src._includableProps);
    }

    public BuilderBasedDeserializer(BuilderBasedDeserializer src, Set<String> ignorableProps, Set<String> includableProps) {
        super(src, ignorableProps, includableProps);
        _buildMethod = src._buildMethod;
        _targetType = src._targetType;
        _propertyNameMatcher = src._propertyNameMatcher;
        _propertiesByIndex = src._propertiesByIndex;
    }

    public BuilderBasedDeserializer(BuilderBasedDeserializer src, BeanPropertyMap props) {
        super(src, props);
        _buildMethod = src._buildMethod;
        _targetType = src._targetType;
        _propertyNameMatcher = _beanProperties.getNameMatcher();
        _propertiesByIndex = _beanProperties.getNameMatcherProperties();
    }

    @Override
    protected void initNameMatcher(DeserializationContext ctxt) {
        _beanProperties.initMatcher(ctxt.getParserFactory());
        _propertyNameMatcher = _beanProperties.getNameMatcher();
        _propertiesByIndex = _beanProperties.getNameMatcherProperties();
    }

    @Override
    public ValueDeserializer<Object> unwrappingDeserializer(DeserializationContext ctxt,
            NameTransformer transformer)
    {
        // main thing really is to just enforce ignoring of unknown properties; since
        // there may be multiple unwrapped values and properties for all may be interleaved...
        if (_currentlyTransforming == transformer) {
            return this;
        }
        _currentlyTransforming = transformer;
        try {
            UnwrappedPropertyHandler uwHandler = _unwrappedPropertyHandler;
            // delegate further unwraps, if any
            if (uwHandler != null) {
                uwHandler = uwHandler.renameAll(ctxt, transformer);
            }
            // and handle direct unwrapping as well:
            BeanPropertyMap props = _beanProperties.renameAll(ctxt, transformer);
            return new BuilderBasedDeserializer(this, uwHandler, props, true);
        } finally { _currentlyTransforming = null; }
    }

    @Override
    public BeanDeserializerBase withObjectIdReader(ObjectIdReader oir) {
        return new BuilderBasedDeserializer(this, oir);
    }

    @Override
    public BeanDeserializerBase withByNameInclusion(Set<String> ignorableProps,
            Set<String> includableProps) {
        return new BuilderBasedDeserializer(this, ignorableProps, includableProps);
    }

    @Override
    public BeanDeserializerBase withIgnoreAllUnknown(boolean ignoreUnknown) {
        return new BuilderBasedDeserializer(this, ignoreUnknown);
    }

    @Override
    public BeanDeserializerBase withBeanProperties(BeanPropertyMap props) {
        return new BuilderBasedDeserializer(this, props);
    }

    @Override
    protected BeanDeserializerBase asArrayDeserializer() {
        return new BeanAsArrayBuilderDeserializer(this, _targetType,
                _beanProperties.getPrimaryProperties(), 
                _buildMethod);
    }

    /*
    /**********************************************************************
    /* ValueDeserializer implementation
    /**********************************************************************
     */

    @Override
    public Boolean supportsUpdate(DeserializationConfig config) {
        // 26-Oct-2016, tatu: No, we can't merge Builder-based POJOs as of now
        return Boolean.FALSE;
    }

    protected Object finishBuild(DeserializationContext ctxt, Object builder)
            throws JacksonException
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
    public Object deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // common case first:
        if (p.isExpectedStartObjectToken()) { 
            if (_vanillaProcessing) {
                return finishBuild(ctxt, _vanillaDeserialize(p, ctxt));
            }
            p.nextToken();
            return finishBuild(ctxt, deserializeFromObject(p, ctxt));
        }
        // and then others, generally requiring use of @JsonCreator
        switch (p.currentTokenId()) {
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
            // these only work if there's a (delegating) creator, or UNWRAP_SINGLE_ARRAY
            // [databind#2608]: Do NOT call `finishBuild()` as method implements it
            return _deserializeFromArray(p, ctxt);
        case JsonTokenId.ID_PROPERTY_NAME:
        case JsonTokenId.ID_END_OBJECT:
            return finishBuild(ctxt, deserializeFromObject(p, ctxt));
        default:
        }
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    /**
     * Secondary deserialization method, called in cases where POJO
     * instance is created as part of deserialization, potentially
     * after collecting some or all of the properties to set.
     */
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt,
    		Object value) throws JacksonException
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
    /**********************************************************************
    /* Concrete deserialization methods
    /**********************************************************************
     */

    /**
     * Streamlined version that is only used when no "special"
     * features are enabled.
     */
    private final Object _vanillaDeserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        Object builder = _valueInstantiator.createUsingDefault(ctxt);
        while (true) {
            int ix = p.nextNameMatch(_propertyNameMatcher);
            if (ix >= 0) {
                p.nextToken();
                SettableBeanProperty prop = _propertiesByIndex[ix];
                try {
                    builder = prop.deserializeSetAndReturn(p, ctxt, builder);
                } catch (Exception e) {
                    throw wrapAndThrow(e, builder, prop.getName(), ctxt);
                }
                continue;
            }
            if (ix == PropertyNameMatcher.MATCH_END_OBJECT) {
                return builder;
            }
            if (ix == PropertyNameMatcher.MATCH_UNKNOWN_NAME) {
                p.nextToken();
                handleUnknownVanilla(p, ctxt, builder, p.currentName());
                continue;
            }
            return _handleUnexpectedWithin(p, ctxt, builder);
        }
    }

    /**
     * General version used when handling needs more advanced
     * features.
     */
    @Override
    public Object deserializeFromObject(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
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
        for (int ix = p.currentNameMatch(_propertyNameMatcher); ; ix = p.nextNameMatch(_propertyNameMatcher)) {
            if (ix >= 0) { // normal case
                p.nextToken();
                try {
                    bean = _propertiesByIndex[ix].deserializeSetAndReturn(p, ctxt, bean);
                } catch (Exception e) {
                    throw wrapAndThrow(e, bean, p.currentName(), ctxt);
                }
                continue;
            }
            if (ix == PropertyNameMatcher.MATCH_END_OBJECT) {
                return bean;
            }
            if (ix != PropertyNameMatcher.MATCH_UNKNOWN_NAME) {
                return _handleUnexpectedWithin(p, ctxt, bean);
            }
            p.nextToken();
            handleUnknownVanilla(p, ctxt, bean, p.currentName());
        }
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
    protected Object _deserializeUsingPropertyBased(final JsonParser p,
            final DeserializationContext ctxt)
        throws JacksonException
    { 
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;

        // 04-Jan-2010, tatu: May need to collect unknown properties for polymorphic cases
        TokenBuffer unknown = null;

        JsonToken t = p.currentToken();
        for (; t == JsonToken.PROPERTY_NAME; t = p.nextToken()) {
            String propName = p.currentName();
            p.nextToken(); // to point to value
            // creator property?
            final SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            // Object Id property?
            if (buffer.readIdProperty(propName) && creatorProp == null) {
                continue;
            }
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
                        throw wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
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
            // regular property? needs buffering
            int ix = _propertyNameMatcher.matchName(propName);
            if (ix >= 0) {
                SettableBeanProperty prop = _propertiesByIndex[ix];
                // !!! 21-Nov-2017, tatu: Regular deserializer handles references here...
                buffer.bufferProperty(prop, prop.deserialize(p, ctxt));
                continue;
            }
            // As per [JACKSON-313], things marked as ignorable should not be
            // passed to any setter
            if (IgnorePropertiesUtil.shouldIgnore(propName, _ignorableProps, _includableProps)) {
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
                unknown = ctxt.bufferForInputBuffering(p);
            }
            unknown.writeName(propName);
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

    protected final Object _deserialize(JsonParser p,
            DeserializationContext ctxt, Object builder) throws JacksonException
    {        
        if (_injectables != null) {
            injectValues(ctxt, builder);
        }
        if (_unwrappedPropertyHandler != null) {
            if (p.hasToken(JsonToken.START_OBJECT)) {
                p.nextToken();
            }
            TokenBuffer tokens = ctxt.bufferForInputBuffering(p);
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
        int ix = p.isExpectedStartObjectToken() ?
                p.nextNameMatch(_propertyNameMatcher) : p.currentNameMatch(_propertyNameMatcher);
        for (; ; ix = p.nextNameMatch(_propertyNameMatcher)) {
            if (ix >= 0) {
                p.nextToken();
                SettableBeanProperty prop = _propertiesByIndex[ix];
                try {
                    builder = prop.deserializeSetAndReturn(p, ctxt, builder);
                } catch (Exception e) {
                    throw wrapAndThrow(e, builder, prop.getName(), ctxt);
                }
                continue;
            }
            if (ix == PropertyNameMatcher.MATCH_END_OBJECT) {
                return builder;
            }
            if (ix != PropertyNameMatcher.MATCH_UNKNOWN_NAME) {
                return _handleUnexpectedWithin(p, ctxt, builder);
            }
            p.nextToken();
            handleUnknownVanilla(p, ctxt, builder, p.currentName());
        }
    }

    @Override
    protected Object _deserializeFromArray(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        // note: cannot call `_delegateDeserializer()` since order reversed here:
        ValueDeserializer<Object> delegateDeser = _arrayDelegateDeserializer;
        // fallback to non-array delegate
        if ((delegateDeser != null) || ((delegateDeser = _delegateDeserializer) != null)) {
            Object builder = _valueInstantiator.createUsingArrayDelegate(ctxt,
                    delegateDeser.deserialize(p, ctxt));
            if (_injectables != null) {
                injectValues(ctxt, builder);
            }
            return finishBuild(ctxt, builder);
        }
        final CoercionAction act = _findCoercionFromEmptyArray(ctxt);
        final boolean unwrap = ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);

        if (unwrap || (act != CoercionAction.Fail)) {
            JsonToken t = p.nextToken();
            if (t == JsonToken.END_ARRAY) {
                switch (act) {
                case AsEmpty:
                    return getEmptyValue(ctxt);
                case AsNull:
                case TryConvert:
                    return getNullValue(ctxt);
                default:
                }
                return ctxt.handleUnexpectedToken(getValueType(ctxt), JsonToken.START_ARRAY, p, null);
            }
            if (unwrap) {
                final Object value = deserialize(p, ctxt);
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(p, ctxt);
                }
                return value;
            }
        }
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    /*
    /**********************************************************************
    /* Deserializing when we have to consider an active View
    /**********************************************************************
     */

    protected final Object deserializeWithView(JsonParser p, DeserializationContext ctxt,
            Object bean, Class<?> activeView)
        throws JacksonException
    {
        for (int ix = p.currentNameMatch(_propertyNameMatcher); ; ix = p.nextNameMatch(_propertyNameMatcher)) {
            if (ix >= 0) {
                p.nextToken();
                SettableBeanProperty prop = _propertiesByIndex[ix];
                if (!prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    bean = prop.deserializeSetAndReturn(p, ctxt, bean);
                } catch (Exception e) {
                    throw wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
                continue;
            }
            if (ix != PropertyNameMatcher.MATCH_END_OBJECT) {
                if (ix != PropertyNameMatcher.MATCH_UNKNOWN_NAME) {
                    return _handleUnexpectedWithin(p, ctxt, bean);
                }
                p.nextToken();
                handleUnknownVanilla(p, ctxt, bean, p.currentName());
                continue;
            }
            return bean;
        }
    }

    /*
    /**********************************************************************
    /* Handling for cases where we have "unwrapped" values
    /**********************************************************************
     */

    /**
     * Method called when there are declared "unwrapped" properties
     * which need special handling
     */
    @SuppressWarnings("resource")
    protected Object deserializeWithUnwrapped(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        if (_delegateDeserializer != null) {
            return _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(p, ctxt));
        }
        if (_propertyBasedCreator != null) {
            return deserializeUsingPropertyBasedWithUnwrapped(p, ctxt);
        }
        TokenBuffer tokens = ctxt.bufferForInputBuffering(p);
        tokens.writeStartObject();
        Object bean = _valueInstantiator.createUsingDefault(ctxt);

        if (_injectables != null) {
            injectValues(ctxt, bean);
        }

        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        for (int ix = p.currentNameMatch(_propertyNameMatcher); ; ix = p.nextNameMatch(_propertyNameMatcher)) {
            if (ix >= 0) { // common case
                p.nextToken();
                SettableBeanProperty prop = _propertiesByIndex[ix];
                if ((activeView != null) && !prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    bean = prop.deserializeSetAndReturn(p, ctxt, bean);
                } catch (Exception e) {
                    throw wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
                continue;
            }
            if (ix == PropertyNameMatcher.MATCH_END_OBJECT) {
                break;
            }
            if (ix == PropertyNameMatcher.MATCH_ODD_TOKEN) {
                return _handleUnexpectedWithin(p, ctxt, bean);
            }
            final String propName = p.currentName();
            p.nextToken();
            // ignorable things should be ignored
            if (IgnorePropertiesUtil.shouldIgnore(propName, _ignorableProps, _includableProps)) {
                handleIgnoredProperty(p, ctxt, bean, propName);
                continue;
            }
            // but... others should be passed to unwrapped property deserializers
            tokens.writeName(propName);
            tokens.copyCurrentStructure(p);
            // how about any setter? We'll get copies but...
            if (_anySetter != null) {
                try {
                    _anySetter.deserializeAndSet(p, ctxt, bean, propName);
                } catch (Exception e) {
                    throw wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
        }
        tokens.writeEndObject();
        return _unwrappedPropertyHandler.processUnwrapped(p, ctxt, bean, tokens);
    }

    protected Object deserializeWithUnwrapped(JsonParser p,
            DeserializationContext ctxt, Object builder, TokenBuffer tokens)
        throws JacksonException
    {
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        for (int ix = p.currentNameMatch(_propertyNameMatcher); ; ix = p.nextNameMatch(_propertyNameMatcher)) {
            if (ix >= 0) { // common case
                p.nextToken();
                SettableBeanProperty prop = _propertiesByIndex[ix];
                if ((activeView != null) && !prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    builder = prop.deserializeSetAndReturn(p, ctxt, builder);
                } catch (Exception e) {
                    wrapAndThrow(e, builder, prop.getName(), ctxt);
                }
                continue;
            }
            if (ix == PropertyNameMatcher.MATCH_END_OBJECT) {
                break;
            }
            if (ix == PropertyNameMatcher.MATCH_ODD_TOKEN) {
                return _handleUnexpectedWithin(p, ctxt, builder);
            }
            final String propName = p.currentName();
            p.nextToken();
            if ((_ignorableProps != null) && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, builder, propName);
                continue;
            }
            // but... others should be passed to unwrapped property deserializers
            tokens.writeName(propName);
            tokens.copyCurrentStructure(p);
            // how about any setter? We'll get copies but...
            if (_anySetter != null) {
                _anySetter.deserializeAndSet(p, ctxt, builder, propName);
            }
        }
        tokens.writeEndObject();
        return _unwrappedPropertyHandler.processUnwrapped(p, ctxt, builder, tokens);
    }

    @SuppressWarnings("resource")
    protected Object deserializeUsingPropertyBasedWithUnwrapped(JsonParser p,
    		DeserializationContext ctxt)
        throws JacksonException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);

        TokenBuffer tokens = ctxt.bufferForInputBuffering(p);
        tokens.writeStartObject();

        JsonToken t = p.currentToken();
        for (; t == JsonToken.PROPERTY_NAME; t = p.nextToken()) {
            String propName = p.currentName();
            p.nextToken(); // to point to value
            // creator property?
            final SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            // Object Id property?
            if (buffer.readIdProperty(propName) && creatorProp == null) {
                continue;
            }
            if (creatorProp != null) {
                // Last creator property to set?
                if (buffer.assignParameter(creatorProp, creatorProp.deserialize(p, ctxt))) {
                    t = p.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                    Object builder = null;
                    try {
                        builder = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        throw wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                    }
                    if (builder.getClass() != _beanType.getRawClass()) {
                        return handlePolymorphic(p, ctxt, builder, tokens);
                    }
                    return deserializeWithUnwrapped(p, ctxt, builder, tokens);
                }
                continue;
            }
            // regular property? needs buffering
            int ix = _propertyNameMatcher.matchName(propName);
            if (ix >= 0) {
                SettableBeanProperty prop = _propertiesByIndex[ix];
                buffer.bufferProperty(prop, prop.deserialize(p, ctxt));
                continue;
            }
            if (IgnorePropertiesUtil.shouldIgnore(propName, _ignorableProps, _includableProps)) {
                handleIgnoredProperty(p, ctxt, handledType(), propName);
                continue;
            }
            tokens.writeName(propName);
            tokens.copyCurrentStructure(p);
            // "any property"?
            if (_anySetter != null) {
                buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(p, ctxt));
            }
        }
        tokens.writeEndObject();

        // We hit END_OBJECT, so:
        Object builder = null;
        try {
            builder = creator.build(ctxt, buffer);
        } catch (Exception e) {
            return wrapInstantiationProblem(e, ctxt);

        }
        return _unwrappedPropertyHandler.processUnwrapped(p, ctxt, builder, tokens);
    }

    /*
    /**********************************************************************
    /* Handling for cases where we have property/-ies with external type id
    /**********************************************************************
     */

    protected Object deserializeWithExternalTypeId(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        if (_propertyBasedCreator != null) {
            return deserializeUsingPropertyBasedWithExternalTypeId(p, ctxt);
        }
        return deserializeWithExternalTypeId(p, ctxt, _valueInstantiator.createUsingDefault(ctxt));
    }

    protected Object deserializeWithExternalTypeId(JsonParser p,
    		DeserializationContext ctxt, Object bean)
        throws JacksonException
    {
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        final ExternalTypeHandler ext = _externalTypeIdHandler.start();

        for (int ix = p.currentNameMatch(_propertyNameMatcher); ; ix = p.nextNameMatch(_propertyNameMatcher)) {
            if (ix >= 0) { // normal case
                SettableBeanProperty prop = _propertiesByIndex[ix];
                JsonToken t = p.nextToken();
                // May have property AND be used as external type id:
                if (t.isScalarValue()) {
                    ext.handleTypePropertyValue(p, ctxt, p.currentName(), bean);
                }
                if (activeView != null && !prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    bean = prop.deserializeSetAndReturn(p, ctxt, bean);
                } catch (Exception e) {
                    throw wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
                continue;
            }
            if (ix == PropertyNameMatcher.MATCH_END_OBJECT) {
                break;
            }
            if (ix != PropertyNameMatcher.MATCH_UNKNOWN_NAME) {
                return _handleUnexpectedWithin(p, ctxt, bean);
            }
            // ignorable things should be ignored
            final String propName = p.currentName();
            if (IgnorePropertiesUtil.shouldIgnore(propName, _ignorableProps, _includableProps)) {
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
                    throw wrapAndThrow(e, bean, propName, ctxt);
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
        throws JacksonException
    {
        // !!! 04-Mar-2012, TODO: Need to fix -- will not work as is...
        JavaType t = _targetType;
        return ctxt.reportBadDefinition(t, String.format(
                "Deserialization (of %s) with Builder, External type id, @JsonCreator not yet implemented",
                t));
    }

    /*
    /**********************************************************************
    /* Error handling
    /**********************************************************************
     */

    /**
     * Method called if an unexpected token (other then <code>FIELD_NAME</code>)
     * is found after POJO has been instantiated and partially bound.
     *
     * @since 3.0
     */
    protected Object _handleUnexpectedWithin(JsonParser p,
            DeserializationContext ctxt, Object beanOrBuilder) throws JacksonException
    {
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }
}
