package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.sym.FieldNameMatcher;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.*;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId.Referring;
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
     * For 2.9?
     *
     * - New method in JsonDeserializer (deserializeNext()) to allow use of more
     *   efficient 'nextXxx()' method `JsonParser` provides.
     *
     * Also: need to ensure efficient impl of those methods for Smile, CBOR
     * at least (in addition to JSON)
     */

    private static final long serialVersionUID = 1L;

    /**
     * Lazily constructed exception used as root cause if reporting problem
     * with creator method that returns <code>null</code> (which is not allowed)
     */
    protected transient Exception _nullFromCreator;

    // @since 3.0
    protected FieldNameMatcher _fieldMatcher;

    // @since 3.0
    protected SettableBeanProperty[] _fieldsByIndex;

    /**
     * State marker we need in order to avoid infinite recursion for some cases
     * (not very clean, alas, but has to do for now)
     */
    protected volatile transient NameTransformer _currentlyTransforming;

    /*
    /**********************************************************
    /* Life-cycle, constructors
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
    protected BeanDeserializer(BeanDeserializer src) {
        super(src, src._ignoreAllUnknown);
        _fieldMatcher = src._fieldMatcher;
        _fieldsByIndex = src._fieldsByIndex;
    }

    protected BeanDeserializer(BeanDeserializer src, boolean ignoreAllUnknown) {
        super(src, ignoreAllUnknown);
        _fieldMatcher = src._fieldMatcher;
        _fieldsByIndex = src._fieldsByIndex;
    }

    protected BeanDeserializer(BeanDeserializer src,
            UnwrappedPropertyHandler unwrapHandler, BeanPropertyMap renamedProperties,
            boolean ignoreAllUnknown) {
        super(src, unwrapHandler, renamedProperties, ignoreAllUnknown);
        _fieldMatcher = _beanProperties.getFieldMatcher();
        _fieldsByIndex = _beanProperties.getFieldMatcherProperties();
    }

    public BeanDeserializer(BeanDeserializer src, ObjectIdReader oir) {
        super(src, oir);
        _fieldMatcher = src._fieldMatcher;
        _fieldsByIndex = src._fieldsByIndex;
    }

    public BeanDeserializer(BeanDeserializer src, Set<String> ignorableProps) {
        super(src, ignorableProps);
        _fieldMatcher = src._fieldMatcher;
        _fieldsByIndex = src._fieldsByIndex;
    }

    public BeanDeserializer(BeanDeserializer src, BeanPropertyMap props) {
        super(src, props);
        _fieldMatcher = _beanProperties.getFieldMatcher();
        _fieldsByIndex = _beanProperties.getFieldMatcherProperties();
    }

    /*
    /**********************************************************
    /* Life-cycle, mutant factories
    /**********************************************************
     */

    @Override
    public JsonDeserializer<Object> unwrappingDeserializer(DeserializationContext ctxt,
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
    public BeanDeserializer withIgnorableProperties(Set<String> ignorableProps) {
        return new BeanDeserializer(this, ignorableProps);
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
    /**********************************************************
    /* Life-cycle, initialization
    /**********************************************************
     */

    @Override
    protected void initFieldMatcher(DeserializationContext ctxt) {
        _beanProperties.initMatcher(ctxt.getParserFactory());
        _fieldMatcher = _beanProperties.getFieldMatcher();
        _fieldsByIndex = _beanProperties.getFieldMatcherProperties();
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
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
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
                return _vanillaDeserialize(p, ctxt, t);
            }
            if (_objectIdReader != null) {
                return deserializeWithObjectId(p, ctxt);
            }
            return deserializeFromObject(p, ctxt);
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
        } else if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)) {
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
        int ix = _fieldMatcher.matchName(propName);
        while (ix >= 0) {
            p.nextToken();
            SettableBeanProperty prop = _fieldsByIndex[ix];
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                throw wrapAndThrow(e, bean, prop.getName(), ctxt);
            }
            ix = p.nextFieldName(_fieldMatcher);
        }
        if (ix != FieldNameMatcher.MATCH_END_OBJECT) {
            if (ix == FieldNameMatcher.MATCH_UNKNOWN_NAME) {
                return _vanillaDeserializeWithUnknown(p, ctxt, bean,
                        p.currentName());
            }
            return _handleUnexpectedWithin(p, ctxt, bean);
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
     * features are enabled, and when current logical token
     * is {@link JsonToken#START_OBJECT} (or equivalent).
     */
    private final Object _vanillaDeserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(bean);

        int ix = p.nextFieldName(_fieldMatcher);
        while (ix >= 0) {
            p.nextToken();
            SettableBeanProperty prop = _fieldsByIndex[ix];
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                wrapAndThrow(e, bean, prop.getName(), ctxt);
            }
            // Elem #2
            ix = p.nextFieldName(_fieldMatcher);
            if (ix < 0) {
                break;
            }
            p.nextToken();
            prop = _fieldsByIndex[ix];
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                wrapAndThrow(e, bean, prop.getName(), ctxt);
            }
            // Elem #3
            ix = p.nextFieldName(_fieldMatcher);
            if (ix < 0) {
                break;
            }
            p.nextToken();
            prop = _fieldsByIndex[ix];
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                wrapAndThrow(e, bean, prop.getName(), ctxt);
            }
            // Elem #4
            ix = p.nextFieldName(_fieldMatcher);
            if (ix < 0) {
                break;
            }
            p.nextToken();
            prop = _fieldsByIndex[ix];
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                wrapAndThrow(e, bean, prop.getName(), ctxt);
            }
            ix = p.nextFieldName(_fieldMatcher);
        }
        if (ix != FieldNameMatcher.MATCH_END_OBJECT) {
            if (ix == FieldNameMatcher.MATCH_UNKNOWN_NAME) {
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
        throws IOException
    {
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(bean);

        if (t != JsonToken.FIELD_NAME) {
            return bean;
        }
        int ix = p.currentFieldName(_fieldMatcher);
        while (ix >= 0) { // minor unrolling here (by-2), less likely on critical path
            SettableBeanProperty prop = _fieldsByIndex[ix];
            p.nextToken();
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                throw wrapAndThrow(e, bean, p.currentName(), ctxt);
            }

            // Elem #2
            ix = p.nextFieldName(_fieldMatcher);
            if (ix < 0) {
                break;
            }
            prop = _fieldsByIndex[ix];
            p.nextToken();
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                throw wrapAndThrow(e, bean, p.currentName(), ctxt);
            }
            ix = p.nextFieldName(_fieldMatcher);
        }
        if (ix != FieldNameMatcher.MATCH_END_OBJECT) {
            if (ix == FieldNameMatcher.MATCH_UNKNOWN_NAME) {
                return _vanillaDeserializeWithUnknown(p, ctxt, bean,
                        p.currentName());
            }
            return _handleUnexpectedWithin(p, ctxt, bean);
        }
        return bean;
    }

    private final Object _vanillaDeserializeWithUnknown(JsonParser p,
            DeserializationContext ctxt, Object bean, String propName) throws IOException
    {
        p.nextToken();
        handleUnknownVanilla(p, ctxt, bean, propName);

        while (true) {
            int ix = p.nextFieldName(_fieldMatcher);
            if (ix >= 0) { // normal case
                p.nextToken();
                try {
                    _fieldsByIndex[ix].deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, p.currentName(), ctxt);
                }
                continue;
            }
            if (ix == FieldNameMatcher.MATCH_END_OBJECT) {
                return bean;
            }
            if (ix != FieldNameMatcher.MATCH_UNKNOWN_NAME) {
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
    public Object deserializeFromObject(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        /* 09-Dec-2014, tatu: As per [databind#622], we need to allow Object Id references
         *   to come in as JSON Objects as well; but for now assume they will
         *   be simple, single-property references, which means that we can
         *   recognize them without having to buffer anything.
         *   Once again, if we must, we can do more complex handling with buffering,
         *   but let's only do that if and when that becomes necessary.
         */
        if (_objectIdReader != null && _objectIdReader.maySerializeAsObject()) {
            if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)
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
            if (_injectables != null) {
                injectValues(ctxt, bean);
            }
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
        if (!p.hasTokenId(JsonTokenId.ID_FIELD_NAME)) {
            // should we check what exactly it is... ?
            return bean;
        }
        if (_needViewProcesing) {
            Class<?> view = ctxt.getActiveView();
            if (view != null) {
                return deserializeWithView(p, ctxt, bean, view);
            }
        }
        for (int ix = p.currentFieldName(_fieldMatcher); ; ix = p.nextFieldName(_fieldMatcher)) {
            if (ix >= 0) { // normal case
                p.nextToken();
                try {
                    _fieldsByIndex[ix].deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    throw wrapAndThrow(e, bean, p.currentName(), ctxt);
                }
                continue;
            }
            if (ix == FieldNameMatcher.MATCH_END_OBJECT) {
                return bean;
            }
            if (ix != FieldNameMatcher.MATCH_UNKNOWN_NAME) {
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
    @SuppressWarnings("resource")
    protected Object _deserializeUsingPropertyBased(final JsonParser p, final DeserializationContext ctxt)
        throws IOException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);
        TokenBuffer unknown = null;
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;

        JsonToken t = p.currentToken();
        List<BeanReferring> referrings = null;
        for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.currentName();
            p.nextToken(); // to point to value
            // Object Id property?
            if (buffer.readIdProperty(propName)) {
                continue;
            }
            // creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                // Last creator property to set?
                Object value;
                if ((activeView != null) && !creatorProp.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }
                value = _deserializeWithErrorWrapping(p, ctxt, creatorProp);
                if (buffer.assignParameter(creatorProp, value)) {
                    p.nextToken(); // to move to following FIELD_NAME/END_OBJECT
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
            int ix = _fieldMatcher.matchName(propName);
            if (ix >= 0) {
                SettableBeanProperty prop = _fieldsByIndex[ix];
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
            // Things marked as ignorable should not be passed to any setter
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, handledType(), propName);
                continue;
            }
            // "any property"?
            if (_anySetter != null) {
                try {
                    buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(p, ctxt));
                } catch (Exception e) {
                    throw wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
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
        if (referrings != null) {
            for (BeanReferring referring : referrings) {
               referring.setBean(bean);
            }
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
     * @since 2.8
     */
    private BeanReferring handleUnresolvedReference(DeserializationContext ctxt,
            SettableBeanProperty prop, PropertyValueBuffer buffer,
            UnresolvedForwardReference reference)
        throws JsonMappingException
    {
        BeanReferring referring = new BeanReferring(ctxt, reference,
                prop.getType(), buffer, prop);
        reference.getRoid().appendReferring(referring);
        return referring;
    }

    protected final Object _deserializeWithErrorWrapping(JsonParser p,
            DeserializationContext ctxt, SettableBeanProperty prop)
        throws IOException
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
        throws IOException
    {
        // 17-Dec-2015, tatu: Highly specialized case, mainly to support polymorphic
        //   "empty" POJOs deserialized from XML, where empty XML tag synthesizes a
        //   `VALUE_NULL` tokens
        if (p.canSynthesizeNulls()) {
            @SuppressWarnings("resource")
            TokenBuffer tb = new TokenBuffer(p, ctxt);
            tb.writeEndObject();
            JsonParser p2 = tb.asParser(ctxt, p);
            p2.nextToken(); // to point to END_OBJECT
            // note: don't have ObjectId to consider at this point, so:
            Object ob = _vanillaProcessing ? _vanillaDeserialize(p2, ctxt, JsonToken.END_OBJECT)
                    : deserializeFromObject(p2, ctxt);
            p2.close();
            return ob;
        }
        return ctxt.handleUnexpectedToken(handledType(), p);
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
        for (int ix = p.currentFieldName(_fieldMatcher); ; ix = p.nextFieldName(_fieldMatcher)) {
            if (ix >= 0) {
                p.nextToken();
                SettableBeanProperty prop = _fieldsByIndex[ix];
                if (!prop.visibleInView(activeView)) {
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
            if (ix != FieldNameMatcher.MATCH_END_OBJECT) {
                if (ix != FieldNameMatcher.MATCH_UNKNOWN_NAME) {
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

        for (int ix = p.currentFieldName(_fieldMatcher); ; ix = p.nextFieldName(_fieldMatcher)) {
            if (ix >= 0) { // common case
                p.nextToken();
                SettableBeanProperty prop = _fieldsByIndex[ix];
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
            if (ix == FieldNameMatcher.MATCH_END_OBJECT) {
                break;
            }
            if (ix == FieldNameMatcher.MATCH_ODD_TOKEN) {
                return _handleUnexpectedWithin(p, ctxt, bean);
            }
            final String propName = p.currentName();
            p.nextToken();
            // Things marked as ignorable should not be passed to any setter
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, bean, propName);
                continue;
            }
            // 29-Nov-2016, tatu: probably should try to avoid sending content
            //    both to any setter AND buffer... but, for now, the only thing
            //    we can do.
            // how about any setter? We'll get copies but...
            if (_anySetter == null) {
                // but... others should be passed to unwrapped property deserializers
                tokens.writeFieldName(propName);
                tokens.copyCurrentStructure(p);
                continue;
            }
            // Need to copy to a separate buffer first
            TokenBuffer b2 = TokenBuffer.asCopyOfValue(p);
            tokens.writeFieldName(propName);
            tokens.append(b2);
            try {
                _anySetter.deserializeAndSet(b2.asParserOnFirstToken(), ctxt, bean, propName);
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
        throws IOException
    {
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) {
            t = p.nextToken();
        }
        TokenBuffer tokens = new TokenBuffer(p, ctxt);
        tokens.writeStartObject();
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        for (int ix = p.currentFieldName(_fieldMatcher); ; ix = p.nextFieldName(_fieldMatcher)) {
            if (ix >= 0) { // common case
                p.nextToken();
                SettableBeanProperty prop = _fieldsByIndex[ix];
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
            if (ix == FieldNameMatcher.MATCH_END_OBJECT) {
                break;
            }
            if (ix == FieldNameMatcher.MATCH_ODD_TOKEN) {
                return _handleUnexpectedWithin(p, ctxt, bean);
            }
            final String propName = p.currentName();
            p.nextToken();
            if ((_ignorableProps != null) && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, bean, propName);
                continue;
            }
            // 29-Nov-2016, tatu: probably should try to avoid sending content
            //    both to any setter AND buffer... but, for now, the only thing
            //    we can do.
            // how about any setter? We'll get copies but...
            if (_anySetter == null) {
                // but... others should be passed to unwrapped property deserializers
                tokens.writeFieldName(propName);
                tokens.copyCurrentStructure(p);
            } else {
                // Need to copy to a separate buffer first
                TokenBuffer b2 = TokenBuffer.asCopyOfValue(p);
                tokens.writeFieldName(propName);
                tokens.append(b2);
                try {
                    _anySetter.deserializeAndSet(b2.asParserOnFirstToken(), ctxt, bean, propName);
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
        throws IOException
    {
        // 01-Dec-2016, tatu: Note: This IS legal to call, but only when unwrapped
        //    value itself is NOT passed via `CreatorProperty` (which isn't supported).
        //    Ok however to pass via setter or field.
        
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);

        TokenBuffer tokens = new TokenBuffer(p, ctxt);
        tokens.writeStartObject();

        JsonToken t = p.currentToken();
        for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.currentName();
            p.nextToken(); // to point to value
            // creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                // Last creator property to set?
                if (buffer.assignParameter(creatorProp,
                        _deserializeWithErrorWrapping(p, ctxt, creatorProp))) {
                    t = p.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                    Object bean;
                    try {
                        bean = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        bean = wrapInstantiationProblem(e, ctxt);
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
                        ctxt.reportInputMismatch(creatorProp,
                                "Cannot create polymorphic instances with unwrapped values");
                        return null;
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
            int ix = _fieldMatcher.matchName(propName);
            if (ix >= 0) {
                SettableBeanProperty prop = _fieldsByIndex[ix];
                buffer.bufferProperty(prop, _deserializeWithErrorWrapping(p, ctxt, prop));
                continue;
            }
            // Things marked as ignorable should not be passed to any setter
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, handledType(), propName);
                continue;
            }
            // 29-Nov-2016, tatu: probably should try to avoid sending content
            //    both to any setter AND buffer... but, for now, the only thing
            //    we can do.
            // how about any setter? We'll get copies but...
            if (_anySetter == null) {
                // but... others should be passed to unwrapped property deserializers
                tokens.writeFieldName(propName);
                tokens.copyCurrentStructure(p);
            } else {
                // Need to copy to a separate buffer first
                TokenBuffer b2 = TokenBuffer.asCopyOfValue(p);
                tokens.writeFieldName(propName);
                tokens.append(b2);
                try {
                    buffer.bufferAnyProperty(_anySetter, propName,
                            _anySetter.deserialize(b2.asParserOnFirstToken(), ctxt));
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

        for (int ix = p.currentFieldName(_fieldMatcher); ; ix = p.nextFieldName(_fieldMatcher)) {
            if (ix >= 0) { // normal case
                SettableBeanProperty prop = _fieldsByIndex[ix];
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
            if (ix == FieldNameMatcher.MATCH_END_OBJECT) {
                break;
            }
            if (ix != FieldNameMatcher.MATCH_UNKNOWN_NAME) {
                return _handleUnexpectedWithin(p, ctxt, bean);
            }
            // ignorable things should be ignored
            final String propName = p.currentName();
            p.nextToken();
            if ((_ignorableProps != null) && _ignorableProps.contains(propName)) {
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
        throws IOException
    {
        final ExternalTypeHandler ext = _externalTypeIdHandler.start();
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, _objectIdReader);

        TokenBuffer tokens = new TokenBuffer(p, ctxt);
        tokens.writeStartObject();

        for (JsonToken t = p.currentToken(); t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.currentName();
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
                            throw wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
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
                            return ctxt.reportBadDefinition(_beanType, String.format(
                                    "Cannot create polymorphic instances with external type ids (%s -> %s)",
                                    _beanType, bean.getClass()));
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
            int ix = _fieldMatcher.matchName(propName);
            if (ix >= 0) {
                SettableBeanProperty prop = _fieldsByIndex[ix];
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
                buffer.bufferAnyProperty(_anySetter, propName,
                        _anySetter.deserialize(p, ctxt));
            }
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
     * Method called if an unexpected token (other then <code>FIELD_NAME</code>)
     * is found after POJO has been instantiated and partially bound.
     *
     * @since 3.0
     */
    protected Object _handleUnexpectedWithin(JsonParser p,
            DeserializationContext ctxt, Object bean) throws IOException
    {
        return ctxt.handleUnexpectedToken(handledType(), p);
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
        public void handleResolvedForwardReference(Object id, Object value) throws IOException
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
