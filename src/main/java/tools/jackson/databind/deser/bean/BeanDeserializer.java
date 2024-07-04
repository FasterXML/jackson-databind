package tools.jackson.databind.deser.bean;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.core.sym.PropertyNameMatcher;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.deser.BeanDeserializerBuilder;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.UnresolvedForwardReference;
import tools.jackson.databind.deser.ReadableObjectId.Referring;
import tools.jackson.databind.deser.impl.ExternalTypeHandler;
import tools.jackson.databind.deser.impl.MethodProperty;
import tools.jackson.databind.deser.impl.ObjectIdReader;
import tools.jackson.databind.deser.impl.UnwrappedPropertyHandler;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.IgnorePropertiesUtil;
import tools.jackson.databind.util.NameTransformer;
import tools.jackson.databind.util.TokenBuffer;

/**
 * Deserializer class that can deserialize instances of
 * arbitrary bean objects, usually from JSON Object structs,
 */
public class BeanDeserializer
    extends BeanDeserializerBase
{
    /**
     * Lazily constructed exception used as root cause if reporting problem
     * with creator method that returns <code>null</code> (which is not allowed)
     */
    protected transient Exception _nullFromCreator;

    // @since 3.0
    protected PropertyNameMatcher _propNameMatcher;

    // @since 3.0
    protected SettableBeanProperty[] _propsByIndex;

    /**
     * State marker we need in order to avoid infinite recursion for some cases
     * (not very clean, alas, but has to do for now)
     */
    protected volatile transient NameTransformer _currentlyTransforming;

    /*
    /**********************************************************************
    /* Life-cycle, constructors
    /**********************************************************************
     */

    /**
     * Constructor used by {@link BeanDeserializerBuilder}.
     */
    public BeanDeserializer(BeanDeserializerBuilder builder, BeanDescription beanDesc,
            BeanPropertyMap properties, Map<String, SettableBeanProperty> backRefs,
            HashSet<String> ignorableProps, boolean ignoreAllUnknown, Set<String> includableProps,
            boolean hasViews)
    {
        super(builder, beanDesc, properties, backRefs,
                ignorableProps, ignoreAllUnknown, includableProps, hasViews);
    }

    /**
     * Copy-constructor that can be used by sub-classes to allow
     * copy-on-write style copying of settings of an existing instance.
     */
    protected BeanDeserializer(BeanDeserializer src) {
        super(src, src._ignoreAllUnknown);
        _propNameMatcher = src._propNameMatcher;
        _propsByIndex = src._propsByIndex;
    }

    protected BeanDeserializer(BeanDeserializer src, boolean ignoreAllUnknown) {
        super(src, ignoreAllUnknown);
        _propNameMatcher = src._propNameMatcher;
        _propsByIndex = src._propsByIndex;
    }

    protected BeanDeserializer(BeanDeserializer src,
            UnwrappedPropertyHandler unwrapHandler, BeanPropertyMap renamedProperties,
            boolean ignoreAllUnknown) {
        super(src, unwrapHandler, renamedProperties, ignoreAllUnknown);
        _propNameMatcher = _beanProperties.getNameMatcher();
        _propsByIndex = _beanProperties.getNameMatcherProperties();
    }

    protected BeanDeserializer(BeanDeserializer src, ObjectIdReader oir) {
        super(src, oir);
        _propNameMatcher = src._propNameMatcher;
        _propsByIndex = src._propsByIndex;
    }

    protected BeanDeserializer(BeanDeserializer src,
            Set<String> ignorableProps, Set<String> includableProps) {
        super(src, ignorableProps, includableProps);
        _propNameMatcher = src._propNameMatcher;
        _propsByIndex = src._propsByIndex;
    }

    protected BeanDeserializer(BeanDeserializer src, BeanPropertyMap props) {
        super(src, props);
        _propNameMatcher = _beanProperties.getNameMatcher();
        _propsByIndex = _beanProperties.getNameMatcherProperties();
    }

    /*
    /**********************************************************************
    /* Life-cycle, mutant factories
    /**********************************************************************
     */

    @Override
    public ValueDeserializer<Object> unwrappingDeserializer(DeserializationContext ctxt,
            NameTransformer transformer)
    {
        // bit kludgy but we don't want to accidentally change type; sub-classes
        // MUST override this method to support unwrapped properties...
        if (getClass() != BeanDeserializer.class) {
            return this;
        }
        // 25-Mar-2017, tatu: Not clean at all, but for [databind#383] we do need
        //   to keep track of accidental recursion...
        if (_currentlyTransforming == transformer) {
            return this;
        }
        _currentlyTransforming = transformer;
        try {
            UnwrappedPropertyHandler uwHandler = _unwrappedPropertyHandler;
            if (uwHandler != null) { // delegate further unwraps, if any
                uwHandler = uwHandler.renameAll(ctxt, transformer);
            }
            // and handle direct unwrapping as well:
            return new BeanDeserializer(this, uwHandler,
                    _beanProperties.renameAll(ctxt, transformer), true);
        } finally { _currentlyTransforming = null; }
    }

    @Override
    public BeanDeserializer withObjectIdReader(ObjectIdReader oir) {
        return new BeanDeserializer(this, oir);
    }

    @Override
    public BeanDeserializer withByNameInclusion(Set<String> ignorableProps,
            Set<String> includableProps) {
        return new BeanDeserializer(this, ignorableProps, includableProps);
    }

    @Override
    public BeanDeserializerBase withIgnoreAllUnknown(boolean ignoreUnknown) {
        return new BeanDeserializer(this, ignoreUnknown);
    }

    @Override
    public BeanDeserializerBase withBeanProperties(BeanPropertyMap props) {
        return new BeanDeserializer(this, props);
    }

    @Override
    protected BeanDeserializerBase asArrayDeserializer() {
        return new BeanAsArrayDeserializer(this, _beanProperties.getPrimaryProperties());
    }

    /*
    /**********************************************************************
    /* Life-cycle, initialization
    /**********************************************************************
     */

    @Override
    protected void initNameMatcher(DeserializationContext ctxt) {
        _beanProperties.initMatcher(ctxt.tokenStreamFactory());
        _propNameMatcher = _beanProperties.getNameMatcher();
        _propsByIndex = _beanProperties.getNameMatcherProperties();
    }

    /*
    /**********************************************************************
    /* ValueDeserializer implementation
    /**********************************************************************
     */

    /**
     * Main deserialization method for bean-based objects (POJOs).
     */
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        // common case first
        if (p.isExpectedStartObjectToken()) {
            if (_vanillaProcessing) {
                return _vanillaDeserialize(p, ctxt);
            }
            // 23-Sep-2015, tatu: This is wrong at some many levels, but for now... it is
            //    what it is, including "expected behavior".
            p.nextToken();
            if (_objectIdReader != null) {
                return deserializeWithObjectId(p, ctxt);
            }
            return deserializeFromObject(p, ctxt);
        }
        return _deserializeOther(p, ctxt, p.currentToken());
    }

    protected final Object _deserializeOther(JsonParser p, DeserializationContext ctxt,
            JsonToken t) throws JacksonException
    {
        // and then others, generally requiring use of @JsonCreator
        if (t != null) {
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
                // these only work if there's a (delegating) creator, or UNWRAP_SINGLE_ARRAY
                return _deserializeFromArray(p, ctxt);
            case PROPERTY_NAME:
            case END_OBJECT: // added to resolve [JACKSON-319], possible related issues
                if (_vanillaProcessing) {
                    return _vanillaDeserialize(p, ctxt, t);
                }
                if (_objectIdReader != null) {
                    return deserializeWithObjectId(p, ctxt);
                }
                return deserializeFromObject(p, ctxt);
            default:
            }
        }
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    /**
     * Secondary deserialization method, called in cases where POJO
     * instance is created as part of deserialization, potentially
     * after collecting some or all of the properties to set.
     */
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object bean) throws JacksonException
    {
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.assignCurrentValue(bean);
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
            propName = p.nextName();
            if (propName == null) {
                return bean;
            }
        } else if (p.hasTokenId(JsonTokenId.ID_PROPERTY_NAME)) {
            propName = p.currentName();
        } else {
            return bean;
        }
        if (_needViewProcesing) {
            Class<?> view = ctxt.getActiveView();
            if (view != null) {
                return deserializeWithView(p, ctxt, bean, view);
            }
        }
        // May or may not be interned...
        int ix = _propNameMatcher.matchName(propName);
        while (ix >= 0) {
            p.nextToken();
            SettableBeanProperty prop = _propsByIndex[ix];
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                throw wrapAndThrow(e, bean, prop.getName(), ctxt);
            }
            ix = p.nextNameMatch(_propNameMatcher);
        }
        if (ix != PropertyNameMatcher.MATCH_END_OBJECT) {
            if (ix == PropertyNameMatcher.MATCH_UNKNOWN_NAME) {
                return _vanillaDeserializeWithUnknown(p, ctxt, bean,
                        p.currentName());
            }
            return _handleUnexpectedWithin(p, ctxt, bean);
        }
        return bean;
    }

    /*
    /**********************************************************************
    /* Concrete deserialization methods
    /**********************************************************************
     */

    /**
     * Streamlined version that is only used when no "special"
     * features are enabled, and when current logical token
     * is {@link JsonToken#START_OBJECT} (or equivalent).
     */
    private final Object _vanillaDeserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.assignCurrentValue(bean);

        int ix = p.nextNameMatch(_propNameMatcher);
        while (ix >= 0) {
            p.nextToken();
            SettableBeanProperty prop = _propsByIndex[ix];
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                wrapAndThrow(e, bean, prop.getName(), ctxt);
            }
            // Elem #2
            ix = p.nextNameMatch(_propNameMatcher);
            if (ix < 0) {
                break;
            }
            p.nextToken();
            prop = _propsByIndex[ix];
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                wrapAndThrow(e, bean, prop.getName(), ctxt);
            }
            // Elem #3
            ix = p.nextNameMatch(_propNameMatcher);
            if (ix < 0) {
                break;
            }
            p.nextToken();
            prop = _propsByIndex[ix];
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                wrapAndThrow(e, bean, prop.getName(), ctxt);
            }
            // Elem #4
            ix = p.nextNameMatch(_propNameMatcher);
            if (ix < 0) {
                break;
            }
            p.nextToken();
            prop = _propsByIndex[ix];
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                wrapAndThrow(e, bean, prop.getName(), ctxt);
            }
            ix = p.nextNameMatch(_propNameMatcher);
        }
        if (ix != PropertyNameMatcher.MATCH_END_OBJECT) {
            if (ix == PropertyNameMatcher.MATCH_UNKNOWN_NAME) {
                return _vanillaDeserializeWithUnknown(p, ctxt, bean,
                        p.currentName());
            }
            return _handleUnexpectedWithin(p, ctxt, bean);
        }
        return bean;
    }

    /**
     * Streamlined version that is only used when no "special"
     * features are enabled.
     */
    private final Object _vanillaDeserialize(JsonParser p,
    		DeserializationContext ctxt, JsonToken t)
        throws JacksonException
    {
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        if (t != JsonToken.PROPERTY_NAME) {
            return bean;
        }
        // [databind#631]: Assign current value, to be accessible by custom serializers
        // [databind#4184]: but only if we have at least one property
        p.assignCurrentValue(bean);
        int ix = p.currentNameMatch(_propNameMatcher);
        while (ix >= 0) { // minor unrolling here (by-2), less likely on critical path
            SettableBeanProperty prop = _propsByIndex[ix];
            p.nextToken();
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                throw wrapAndThrow(e, bean, p.currentName(), ctxt);
            }

            // Elem #2
            ix = p.nextNameMatch(_propNameMatcher);
            if (ix < 0) {
                break;
            }
            prop = _propsByIndex[ix];
            p.nextToken();
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                throw wrapAndThrow(e, bean, p.currentName(), ctxt);
            }
            ix = p.nextNameMatch(_propNameMatcher);
        }
        if (ix != PropertyNameMatcher.MATCH_END_OBJECT) {
            if (ix == PropertyNameMatcher.MATCH_UNKNOWN_NAME) {
                return _vanillaDeserializeWithUnknown(p, ctxt, bean,
                        p.currentName());
            }
            return _handleUnexpectedWithin(p, ctxt, bean);
        }
        return bean;
    }

    private final Object _vanillaDeserializeWithUnknown(JsonParser p,
            DeserializationContext ctxt, Object bean, String propName) throws JacksonException
    {
        p.nextToken();
        handleUnknownVanilla(p, ctxt, bean, propName);

        while (true) {
            int ix = p.nextNameMatch(_propNameMatcher);
            if (ix >= 0) { // normal case
                p.nextToken();
                try {
                    _propsByIndex[ix].deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, p.currentName(), ctxt);
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
     * General version used when handling needs more advanced features.
     */
    @Override
    public Object deserializeFromObject(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        /* 09-Dec-2014, tatu: As per [databind#622], we need to allow Object Id references
         *   to come in as JSON Objects as well; but for now assume they will
         *   be simple, single-property references, which means that we can
         *   recognize them without having to buffer anything.
         *   Once again, if we must, we can do more complex handling with buffering,
         *   but let's only do that if and when that becomes necessary.
         */
        if ((_objectIdReader != null) && _objectIdReader.maySerializeAsObject()) {
            if (p.hasTokenId(JsonTokenId.ID_PROPERTY_NAME)
                    && _objectIdReader.isValidReferencePropertyName(p.currentName(), p)) {
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
            // 27-May-2014, tatu: I don't think view processing would work
            //   at this point, so commenting it out; but leaving in place
            //   just in case I forgot something fundamental...
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
        p.assignCurrentValue(bean);
        if (p.canReadObjectId()) {
            Object id = p.getObjectId();
            if (id != null) {
                _handleTypedObjectId(p, ctxt, bean, id);
            }
        }
        // [databind#3838]: since 2.16 Uniform handling of missing objectId
        // only for the specific "empty JSON Object" case
        if (_objectIdReader != null && p.hasTokenId(JsonTokenId.ID_END_OBJECT)) {
            // [databind#4610]: check if we are to skip failure
            if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)) {
                ctxt.reportUnresolvedObjectId(_objectIdReader, bean);
            }
        }
        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        if (!p.hasTokenId(JsonTokenId.ID_PROPERTY_NAME)) {
            // should we check what exactly it is... ?
            return bean;
        }
        if (_needViewProcesing) {
            Class<?> view = ctxt.getActiveView();
            if (view != null) {
                return deserializeWithView(p, ctxt, bean, view);
            }
        }
        for (int ix = p.currentNameMatch(_propNameMatcher); ; ix = p.nextNameMatch(_propNameMatcher)) {
            if (ix >= 0) { // normal case
                p.nextToken();
                try {
                    _propsByIndex[ix].deserializeAndSet(p, ctxt, bean);
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
     */
    @Override
    protected Object _deserializeUsingPropertyBased(final JsonParser p, final DeserializationContext ctxt)
        throws JacksonException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = (_anySetter != null)
            ? creator.startBuildingWithAnySetter(p, ctxt, _objectIdReader, _anySetter)
            : creator.startBuilding(p, ctxt, _objectIdReader);
        TokenBuffer unknown = null;
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;

        JsonToken t = p.currentToken();
        List<BeanReferring> referrings = null;
        for (; t == JsonToken.PROPERTY_NAME; t = p.nextToken()) {
            String propName = p.currentName();
            p.nextToken(); // to point to value
            final SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            // Object Id property?
            if (buffer.readIdProperty(propName) && creatorProp == null) {
                continue;
            }
            // [databind#1891]: possible fix
            /*
            if (_ignorableProps != null && _ignorableProps.contains(propName)){
                continue;
            }
            */

            // Creator property?
            if (creatorProp != null) {
                Object value;
                if ((activeView != null) && !creatorProp.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                value = _deserializeWithErrorWrapping(p, ctxt, creatorProp);
                // Last creator property to set?
                if (buffer.assignParameter(creatorProp, value)) {
                    p.nextToken(); // to move to following PROPERTY_NAME/END_OBJECT
                    Object bean;
                    try {
                        bean = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        bean = wrapInstantiationProblem(e, ctxt);
                    }
                    if (bean == null) {
                        return ctxt.handleInstantiationProblem(handledType(), null,
                                _creatorReturnedNullException());
                    }
                    // [databind#631]: Assign current value, to be accessible by custom serializers
                    p.assignCurrentValue(bean);

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
            int ix = _propNameMatcher.matchName(propName);
            if (ix >= 0) {
                SettableBeanProperty prop = _propsByIndex[ix];
                // [databind#3724]: Special handling because Records' ignored creator props
                // weren't removed (to help in creating constructor-backed PropertyCreator)
                // so they ended up in _beanProperties, unlike POJO (whose ignored
                // props are removed)
                // [databind#3938]: except if it's MethodProperty
                if (!_beanType.isRecordType() || (prop instanceof MethodProperty)) {
                    try {
                        buffer.bufferProperty(prop, _deserializeWithErrorWrapping(p, ctxt, prop));
                    } catch (UnresolvedForwardReference reference) {
                        // 14-Jun-2016, tatu: As per [databind#1261], looks like we need additional
                        //    handling of forward references here. Not exactly sure why existing
                        //    facilities did not cover, but this does appear to solve the problem
                        BeanReferring referring = handleUnresolvedReference(ctxt,
                                prop, buffer, reference);
                        if (referrings == null) {
                            referrings = new ArrayList<BeanReferring>();
                        }
                        referrings.add(referring);
                    }
                    continue;
                }
            }
            // Things marked as ignorable should not be passed to any setter
            if (IgnorePropertiesUtil.shouldIgnore(propName, _ignorableProps, _includableProps)) {
                handleIgnoredProperty(p, ctxt, handledType(), propName);
                continue;
            }
            // "any property"?
            if (_anySetter != null) {
                try {
                    buffer.bufferAnyParameterProperty(_anySetter, propName, _anySetter.deserialize(p, ctxt));
                } catch (Exception e) {
                    throw wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                }
                continue;
            }
            // 29-Mar-2021, tatu: [databind#3082] May skip collection if we know
            //    they'd just get ignored (note: any-setter handled above; unwrapped
            //    properties also separately handled)
            if (_ignoreAllUnknown) {
                // 22-Aug-2021, tatu: [databind#3252] must ensure we do skip the whole value
                p.skipChildren();
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
        Object bean;
        try {
            bean = creator.build(ctxt, buffer);
        } catch (Exception e) {
            return wrapInstantiationProblem(e, ctxt);
        }
        // 13-Apr-2020, tatu: [databind#2678] need to handle injection here
        if (_injectables != null) {
            injectValues(ctxt, bean);
        }

        if (referrings != null) {
            for (BeanReferring referring : referrings) {
               referring.setBean(bean);
            }
        }
        if (unknown != null) {
            // polymorphic?
            if (bean.getClass() != _beanType.getRawClass()) { // lgtm [java/dereferenced-value-may-be-null]
                return handlePolymorphic(null, ctxt, bean, unknown);
            }
            // no, just some extra unknown properties
            return handleUnknownProperties(ctxt, bean, unknown);
        }
        return bean;
    }

    private BeanReferring handleUnresolvedReference(DeserializationContext ctxt,
            SettableBeanProperty prop, PropertyValueBuffer buffer,
            UnresolvedForwardReference reference)
        throws DatabindException
    {
        BeanReferring referring = new BeanReferring(ctxt, reference,
                prop.getType(), buffer, prop);
        reference.getRoid().appendReferring(referring);
        return referring;
    }

    protected final Object _deserializeWithErrorWrapping(JsonParser p,
            DeserializationContext ctxt, SettableBeanProperty prop)
        throws DatabindException
    {
        try {
            return prop.deserialize(p, ctxt);
        } catch (Exception e) {
            throw wrapAndThrow(e, _beanType.getRawClass(), prop.getName(), ctxt);
        }
    }

    /**
     * Helper method called for rare case of pointing to {@link JsonToken#VALUE_NULL}
     * token. While this is most often an erroneous condition, there is one specific
     * case with XML handling where polymorphic type with no properties is exposed
     * as such, and should be handled same as empty Object.
     */
    protected Object deserializeFromNull(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // 17-Dec-2015, tatu: Highly specialized case, mainly to support polymorphic
        //   "empty" POJOs deserialized from XML, where empty XML tag synthesizes a
        //   `VALUE_NULL` tokens
        /*
        if (p.canSynthesizeNulls()) {
            TokenBuffer tb = ctxt.bufferForInputBuffering(p);
            tb.writeEndObject();
            JsonParser p2 = tb.asParser(ctxt, p);
            p2.nextToken(); // to point to END_OBJECT
            // note: don't have ObjectId to consider at this point, so:
            Object ob = _vanillaProcessing ? _vanillaDeserialize(p2, ctxt, JsonToken.END_OBJECT)
                    : deserializeFromObject(p2, ctxt);
            p2.close();
            tb.close();
            return ob;
        }
        */
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    @Override
    protected Object _deserializeFromArray(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        // note: cannot call `_delegateDeserializer()` since order reversed here:
        ValueDeserializer<Object> delegateDeser = _arrayDelegateDeserializer;
        // fallback to non-array delegate
        if ((delegateDeser != null) || ((delegateDeser = _delegateDeserializer) != null)) {
            Object bean = _valueInstantiator.createUsingArrayDelegate(ctxt,
                    delegateDeser.deserialize(p, ctxt));
            if (_injectables != null) {
                injectValues(ctxt, bean);
            }
            return bean;
        }
        final CoercionAction act = _findCoercionFromEmptyArray(ctxt);
        final boolean unwrap = ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);

        if (unwrap || (act != CoercionAction.Fail)) {
            JsonToken unwrappedToken = p.nextToken();
            if (unwrappedToken == JsonToken.END_ARRAY) {
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
                // 23-Aug-2022, tatu: To prevent unbounded nested arrays, we better
                //   check there is NOT another START_ARRAY lurking there..
                if (unwrappedToken == JsonToken.START_ARRAY) {
                    JavaType targetType = getValueType(ctxt);
                    return ctxt.handleUnexpectedToken(targetType, JsonToken.START_ARRAY, p,
"Cannot deserialize value of type %s from deeply-nested Array: only single wrapper allowed with `%s`",
                            ClassUtil.getTypeDescription(targetType),
                                    "DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS");
                }
                final Object value = deserialize(p, ctxt);
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(p, ctxt);
                }
                return value;
            }
            // 15-Nov-2022, tatu: ... we probably should pass original `JsonToken.START_ARRAY`
            //     as unexpected token, since `p` now points to `unwrappedToken` instead...
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
        for (int ix = p.currentNameMatch(_propNameMatcher); ; ix = p.nextNameMatch(_propNameMatcher)) {
            if (ix >= 0) {
                p.nextToken();
                SettableBeanProperty prop = _propsByIndex[ix];
                if (!prop.visibleInView(activeView)) {
                    // [databind#437]: fields in other views to be considered as unknown properties
                    if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)){
                        ctxt.reportInputMismatch(handledType(),
                                String.format("Input mismatch while deserializing %s. Property '%s' is not part of current active view '%s'" +
                                        " (disable 'DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES' to allow)",
                                        ClassUtil.nameOf(handledType()), prop.getName(), activeView.getName()));
                    }
                    p.skipChildren();
                    continue;
                }
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, p.currentName(), ctxt);
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
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);

        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.assignCurrentValue(bean);

        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;

        for (int ix = p.currentNameMatch(_propNameMatcher); ; ix = p.nextNameMatch(_propNameMatcher)) {
            if (ix >= 0) { // common case
                p.nextToken();
                SettableBeanProperty prop = _propsByIndex[ix];
                if ((activeView != null) && !prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
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
            // Things marked as ignorable should not be passed to any setter
            if (IgnorePropertiesUtil.shouldIgnore(propName, _ignorableProps, _includableProps)) {
                handleIgnoredProperty(p, ctxt, bean, propName);
                continue;
            }
            // 29-Nov-2016, tatu: probably should try to avoid sending content
            //    both to any setter AND buffer... but, for now, the only thing
            //    we can do.
            // how about any setter? We'll get copies but...
            if (_anySetter == null) {
                // but... others should be passed to unwrapped property deserializers
                tokens.writeName(propName);
                tokens.copyCurrentStructure(p);
                continue;
            }
            // Need to copy to a separate buffer first
            TokenBuffer b2 = ctxt.bufferAsCopyOfValue(p);
            tokens.writeName(propName);
            tokens.append(b2);
            try {
                _anySetter.deserializeAndSet(b2.asParserOnFirstToken(ctxt), ctxt, bean, propName);
            } catch (Exception e) {
                throw wrapAndThrow(e, bean, propName, ctxt);
            }
        }
        tokens.writeEndObject();
        _unwrappedPropertyHandler.processUnwrapped(p, ctxt, bean, tokens);
        return bean;
    }

    @SuppressWarnings("resource")
    protected Object deserializeWithUnwrapped(JsonParser p, DeserializationContext ctxt,
            Object bean)
        throws JacksonException
    {
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) {
            t = p.nextToken();
        }
        TokenBuffer tokens = ctxt.bufferForInputBuffering(p);
        tokens.writeStartObject();
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        for (int ix = p.currentNameMatch(_propNameMatcher); ; ix = p.nextNameMatch(_propNameMatcher)) {
            if (ix >= 0) { // common case
                p.nextToken();
                SettableBeanProperty prop = _propsByIndex[ix];
                if ((activeView != null) && !prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
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
            if (IgnorePropertiesUtil.shouldIgnore(propName, _ignorableProps, _includableProps)) {
                handleIgnoredProperty(p, ctxt, bean, propName);
                continue;
            }
            // 29-Nov-2016, tatu: probably should try to avoid sending content
            //    both to any setter AND buffer... but, for now, the only thing
            //    we can do.
            // how about any setter? We'll get copies but...
            if (_anySetter == null) {
                // but... others should be passed to unwrapped property deserializers
                tokens.writeName(propName);
                tokens.copyCurrentStructure(p);
            } else {
                // Need to copy to a separate buffer first
                TokenBuffer b2 = ctxt.bufferAsCopyOfValue(p);
                tokens.writeName(propName);
                tokens.append(b2);
                try {
                    _anySetter.deserializeAndSet(b2.asParserOnFirstToken(ctxt), ctxt, bean, propName);
                } catch (Exception e) {
                    throw wrapAndThrow(e, bean, propName, ctxt);
                }
            }
        }
        tokens.writeEndObject();
        _unwrappedPropertyHandler.processUnwrapped(p, ctxt, bean, tokens);
        return bean;
    }

    @SuppressWarnings("resource")
    protected Object deserializeUsingPropertyBasedWithUnwrapped(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // 01-Dec-2016, tatu: Note: This IS legal to call, but only when unwrapped
        //    value itself is NOT passed via `CreatorProperty` (which isn't supported).
        //    Ok however to pass via setter or field.

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
                if (buffer.assignParameter(creatorProp,
                        _deserializeWithErrorWrapping(p, ctxt, creatorProp))) {
                    t = p.nextToken(); // to move to following PROPERTY_NAME/END_OBJECT
                    Object bean;
                    try {
                        bean = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        bean = wrapInstantiationProblem(e, ctxt);
                    }
                    // [databind#631]: Assign current value, to be accessible by custom serializers
                    p.assignCurrentValue(bean);
                    // if so, need to copy all remaining tokens into buffer
                    while (t == JsonToken.PROPERTY_NAME) {
                        // NOTE: do NOT skip name as it needs to be copied; `copyCurrentStructure` does that
                        tokens.copyCurrentStructure(p);
                        t = p.nextToken();
                    }
                    // 28-Aug-2018, tatu: Let's add sanity check here, easier to catch off-by-some
                    //    problems if we maintain invariants
                    if (t != JsonToken.END_OBJECT) {
                        ctxt.reportWrongTokenException(this, JsonToken.END_OBJECT,
                                "Attempted to unwrap '%s' value",
                                handledType().getName());
                    }
                    tokens.writeEndObject();
                    if (bean.getClass() != _beanType.getRawClass()) {
                        // !!! 08-Jul-2011, tatu: Could probably support; but for now
                        //   it's too complicated, so bail out
                        return ctxt.reportInputMismatch(creatorProp,
                                "Cannot create polymorphic instances with unwrapped values");
                    }
                    return _unwrappedPropertyHandler.processUnwrapped(p, ctxt, bean, tokens);
                }
                continue;
            }
            // regular property? needs buffering
            int ix = _propNameMatcher.matchName(propName);
            if (ix >= 0) {
                SettableBeanProperty prop = _propsByIndex[ix];
                buffer.bufferProperty(prop, _deserializeWithErrorWrapping(p, ctxt, prop));
                continue;
            }
            // Things marked as ignorable should not be passed to any setter
            if (IgnorePropertiesUtil.shouldIgnore(propName, _ignorableProps, _includableProps)) {
                handleIgnoredProperty(p, ctxt, handledType(), propName);
                continue;
            }
            // 29-Nov-2016, tatu: probably should try to avoid sending content
            //    both to any setter AND buffer... but, for now, the only thing
            //    we can do.
            // how about any setter? We'll get copies but...
            if (_anySetter == null) {
                // but... others should be passed to unwrapped property deserializers
                tokens.writeName(propName);
                tokens.copyCurrentStructure(p);
            } else {
                // Need to copy to a separate buffer first
                TokenBuffer b2 = ctxt.bufferAsCopyOfValue(p);
                tokens.writeName(propName);
                tokens.append(b2);
                try {
                    buffer.bufferAnyProperty(_anySetter, propName,
                            _anySetter.deserialize(b2.asParserOnFirstToken(ctxt), ctxt));
                } catch (Exception e) {
                    throw wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                }
            }
        }

        // We hit END_OBJECT, so:
        Object bean;
        try {
            bean = creator.build(ctxt, buffer);
        } catch (Exception e) {
            return wrapInstantiationProblem(e, ctxt);
        }
        return _unwrappedPropertyHandler.processUnwrapped(p, ctxt, bean, tokens);
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
        throws JacksonException
    {
        return _deserializeWithExternalTypeId(p, ctxt, bean, _externalTypeIdHandler.start());
    }

    protected Object _deserializeWithExternalTypeId(JsonParser p, DeserializationContext ctxt,
            Object bean, ExternalTypeHandler ext)
        throws JacksonException
    {
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;

        for (int ix = p.currentNameMatch(_propNameMatcher); ; ix = p.nextNameMatch(_propNameMatcher)) {
            if (ix >= 0) { // normal case
                SettableBeanProperty prop = _propsByIndex[ix];
                JsonToken t = p.nextToken();
                // [JACKSON-831]: may have property AND be used as external type id:
                if (t.isScalarValue()) {
                    ext.handleTypePropertyValue(p, ctxt, p.currentName(), bean);
                }
                if (activeView != null && !prop.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
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
            p.nextToken();
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
            }
            // Unknown: let's call handler method
            handleUnknownProperty(p, ctxt, bean, p.currentName());
        }
        // and when we get this far, let's try finalizing the deal:
        return ext.complete(p, ctxt, bean);
    }

    @SuppressWarnings("resource")
    protected Object deserializeUsingPropertyBasedWithExternalTypeId(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        final ExternalTypeHandler ext = _externalTypeIdHandler.start();
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);

        for (JsonToken t = p.currentToken(); t == JsonToken.PROPERTY_NAME; t = p.nextToken()) {
            String propName = p.currentName();
            t = p.nextToken(); // to point to value
            // creator property?
            final SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            // Object Id property?
            if (buffer.readIdProperty(propName) && creatorProp == null) {
                continue;
            }
            if (creatorProp != null) {
                // first: let's check to see if this might be part of value with external type id:
                // 11-Sep-2015, tatu: Important; do NOT pass buffer as last arg, but null,
                //   since it is not the bean
                if (!ext.handlePropertyValue(p, ctxt, propName, null)) {
                    // Last creator property to set?
                    if (buffer.assignParameter(creatorProp, _deserializeWithErrorWrapping(p, ctxt, creatorProp))) {
                        t = p.nextToken(); // to move to following PROPERTY_NAME/END_OBJECT
                        Object bean;
                        try {
                            bean = creator.build(ctxt, buffer);
                        } catch (Exception e) {
                            throw wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                        }
                        if (bean.getClass() != _beanType.getRawClass()) {
                            // !!! 08-Jul-2011, tatu: Could theoretically support; but for now
                            //   it's too complicated, so bail out
                            return ctxt.reportBadDefinition(_beanType, String.format(
                                    "Cannot create polymorphic instances with external type ids (%s -> %s)",
                                    _beanType, bean.getClass()));
                        }
                        // 19-Feb-2021, tatu: [databind#3045] Better delegate
                        return _deserializeWithExternalTypeId(p, ctxt, bean, ext);
                    }
                }
                continue;
            }
            // regular property? needs buffering
            int ix = _propNameMatcher.matchName(propName);
            if (ix >= 0) {
                SettableBeanProperty prop = _propsByIndex[ix];
                // [databind#3045]: may have property AND be used as external type id:
                if (t.isScalarValue()) {
                    ext.handleTypePropertyValue(p, ctxt, propName, null);
                }
                buffer.bufferProperty(prop, prop.deserialize(p, ctxt));
                continue;
            }
            // external type id (or property that depends on it)?
            if (ext.handlePropertyValue(p, ctxt, propName, null)) {
                continue;
            }
            // Things marked as ignorable should not be passed to any setter
            if (IgnorePropertiesUtil.shouldIgnore(propName, _ignorableProps, _includableProps)) {
                handleIgnoredProperty(p, ctxt, handledType(), propName);
                continue;
            }
            // "any property"?
            if (_anySetter != null) {
                buffer.bufferAnyProperty(_anySetter, propName,
                        _anySetter.deserialize(p, ctxt));
                continue;
            }
            // Unknown: let's call handler method
            handleUnknownProperty(p, ctxt, _valueClass, propName);
        }
        // We hit END_OBJECT; resolve the pieces:
        try {
            return ext.complete(p, ctxt, buffer, creator);
        } catch (Exception e) {
            return wrapInstantiationProblem(e, ctxt);
        }
    }

    /**
     * Helper method for getting a lazily construct exception to be reported
     * to {@link DeserializationContext#handleInstantiationProblem(Class, Object, Throwable)}.
     */
    protected Exception _creatorReturnedNullException() {
        if (_nullFromCreator == null) {
            _nullFromCreator = new NullPointerException("JSON Creator returned null");
        }
        return _nullFromCreator;
    }

    /**
     * Method called if an unexpected token (other then {@code JsonToken.PROPERTY_NAME})
     * is found after POJO has been instantiated and partially bound.
     *
     * @since 3.0
     */
    protected Object _handleUnexpectedWithin(JsonParser p,
            DeserializationContext ctxt, Object bean) throws JacksonException
    {
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    static class BeanReferring extends Referring
    {
        private final DeserializationContext _context;
        private final SettableBeanProperty _prop;
        private Object _bean;

        BeanReferring(DeserializationContext ctxt, UnresolvedForwardReference ref,
                JavaType valueType, PropertyValueBuffer buffer, SettableBeanProperty prop)
        {
            super(ref, valueType);
            _context = ctxt;
            _prop = prop;
        }

        public void setBean(Object bean) {
            _bean = bean;
        }

        @Override
        public void handleResolvedForwardReference(Object id, Object value)
        {
            if (_bean == null) {
                _context.reportInputMismatch(_prop,
"Cannot resolve ObjectId forward reference using property '%s' (of type %s): Bean not yet resolved",
_prop.getName(), _prop.getDeclaringClass().getName());
        }
            _prop.set(_bean, value);
        }
    }
}
