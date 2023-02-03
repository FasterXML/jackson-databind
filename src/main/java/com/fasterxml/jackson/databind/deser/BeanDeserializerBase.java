package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.*;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.IgnoredPropertyException;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ClassKey;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.*;

/**
 * Base class for <code>BeanDeserializer</code>.
 */
public abstract class BeanDeserializerBase
    extends StdDeserializer<Object>
    implements ContextualDeserializer, ResolvableDeserializer,
        ValueInstantiator.Gettable, // since 2.9
        java.io.Serializable // since 2.1
{
    private static final long serialVersionUID = 1;

    protected final static PropertyName TEMP_PROPERTY_NAME = new PropertyName("#temporary-name");

    /*
    /**********************************************************
    /* Information regarding type being deserialized
    /**********************************************************
     */

    /**
     * Declared type of the bean this deserializer handles.
     */
    final protected JavaType _beanType;

    /**
     * Requested shape from bean class annotations.
     */
    final protected JsonFormat.Shape _serializationShape;

    /*
    /**********************************************************
    /* Configuration for creating value instance
    /**********************************************************
     */

    /**
     * Object that handles details of constructing initial
     * bean value (to which bind data to), unless instance
     * is passed (via updateValue())
     */
    protected final ValueInstantiator _valueInstantiator;

    /**
     * Deserializer that is used iff delegate-based creator is
     * to be used for deserializing from JSON Object.
     *<p>
     * NOTE: cannot be {@code final} because we need to get it during
     * {@code resolve()} method (and not contextualization).
     */
    protected JsonDeserializer<Object> _delegateDeserializer;

    /**
     * Deserializer that is used iff array-delegate-based creator
     * is to be used for deserializing from JSON Object.
     *<p>
     * NOTE: cannot be {@code final} because we need to get it during
     * {@code resolve()} method (and not contextualization).
     */
    protected JsonDeserializer<Object> _arrayDelegateDeserializer;

    /**
     * If the bean needs to be instantiated using constructor
     * or factory method
     * that takes one or more named properties as argument(s),
     * this creator is used for instantiation.
     * This value gets resolved during general resolution.
     */
    protected PropertyBasedCreator _propertyBasedCreator;

    /**
     * Flag that is set to mark cases where deserialization from Object value
     * using otherwise "standard" property binding will need to use non-default
     * creation method: namely, either "full" delegation (array-delegation does
     * not apply), or properties-based Creator method is used.
     *<p>
     * Note that flag is somewhat mis-named as it is not affected by scalar-delegating
     * creators; it only has effect on Object Value binding.
     */
    protected boolean _nonStandardCreation;

    /**
     * Flag that indicates that no "special features" whatsoever
     * are enabled, so the simplest processing is possible.
     */
    protected boolean _vanillaProcessing;

    /*
    /**********************************************************
    /* Property information, setters
    /**********************************************************
     */

    /**
     * Mapping of property names to properties, built when all properties
     * to use have been successfully resolved.
     */
    final protected BeanPropertyMap _beanProperties;

    /**
     * List of {@link ValueInjector}s, if any injectable values are
     * expected by the bean; otherwise null.
     * This includes injectors used for injecting values via setters
     * and fields, but not ones passed through constructor parameters.
     */
    final protected ValueInjector[] _injectables;

    /**
     * Fallback setter used for handling any properties that are not
     * mapped to regular setters. If setter is not null, it will be
     * called once for each such property.
     */
    protected SettableAnyProperty _anySetter;

    /**
     * In addition to properties that are set, we will also keep
     * track of recognized but ignorable properties: these will
     * be skipped without errors or warnings.
     */
    final protected Set<String> _ignorableProps;

    /**
     * Keep track of the the properties that needs to be specifically included.
     */
    final protected Set<String> _includableProps;

    /**
     * Flag that can be set to ignore and skip unknown properties.
     * If set, will not throw an exception for unknown properties.
     */
    final protected boolean _ignoreAllUnknown;

    /**
     * Flag that indicates that some aspect of deserialization depends
     * on active view used (if any)
     */
    final protected boolean _needViewProcesing;

    /**
     * We may also have one or more back reference fields (usually
     * zero or one).
     */
    final protected Map<String, SettableBeanProperty> _backRefs;

    /*
    /**********************************************************
    /* Related handlers
    /**********************************************************
     */

    /**
     * Lazily constructed map used to contain deserializers needed
     * for polymorphic subtypes.
     * Note that this is <b>only needed</b> for polymorphic types,
     * that is, when the actual type is not statically known.
     * For other types this remains null.
     */
    protected transient HashMap<ClassKey, JsonDeserializer<Object>> _subDeserializers;

    /**
     * If one of properties has "unwrapped" value, we need separate
     * helper object
     */
    protected UnwrappedPropertyHandler _unwrappedPropertyHandler;

    /**
     * Handler that we need iff any of properties uses external
     * type id.
     */
    protected ExternalTypeHandler _externalTypeIdHandler;

    /**
     * If an Object Id is to be used for value handled by this
     * deserializer, this reader is used for handling.
     */
    protected final ObjectIdReader _objectIdReader;

    /*
    /**********************************************************
    /* Life-cycle, construction, initialization
    /**********************************************************
     */

    /**
     * Constructor used when initially building a deserializer
     * instance, given a {@link BeanDeserializerBuilder} that
     * contains configuration.
     */
    protected BeanDeserializerBase(BeanDeserializerBuilder builder,
            BeanDescription beanDesc,
            BeanPropertyMap properties, Map<String, SettableBeanProperty> backRefs,
            Set<String> ignorableProps, boolean ignoreAllUnknown,
            Set<String> includableProps,
            boolean hasViews)
    {
        super(beanDesc.getType());
        _beanType = beanDesc.getType();

        _valueInstantiator = builder.getValueInstantiator();
        _delegateDeserializer = null;
        _arrayDelegateDeserializer = null;
        _propertyBasedCreator = null;

        _beanProperties = properties;
        _backRefs = backRefs;
        _ignorableProps = ignorableProps;
        _ignoreAllUnknown = ignoreAllUnknown;
        _includableProps = includableProps;

        _anySetter = builder.getAnySetter();
        List<ValueInjector> injectables = builder.getInjectables();
        _injectables = (injectables == null || injectables.isEmpty()) ? null
                : injectables.toArray(new ValueInjector[injectables.size()]);
        _objectIdReader = builder.getObjectIdReader();

        // 02-May-2020, tatu: This boolean setting is only used when binding from
        //    Object value, and hence does not consider "array-delegating" or various
        //    scalar-delegation cases. It is set when default (0-argument) constructor
        //    is NOT to be used when binding an Object value (or in case of
        //    POJO-as-array, Array value).
        _nonStandardCreation = (_unwrappedPropertyHandler != null)
            || _valueInstantiator.canCreateUsingDelegate()
        // [databind#2486]: as per above, array-delegating creator should not be considered
        //   as doing so will prevent use of Array-or-standard-Object deserialization
        //            || _valueInstantiator.canCreateUsingArrayDelegate()
            || _valueInstantiator.canCreateFromObjectWith()
            || !_valueInstantiator.canCreateUsingDefault()
            ;

        // Any transformation we may need to apply?
        final JsonFormat.Value format = beanDesc.findExpectedFormat(null);
        _serializationShape = format.getShape();

        _needViewProcesing = hasViews;
        _vanillaProcessing = !_nonStandardCreation
                && (_injectables == null)
                && !_needViewProcesing
                // also, may need to reorder stuff if we expect Object Id:
                && (_objectIdReader == null)
                ;
    }

    protected BeanDeserializerBase(BeanDeserializerBase src) {
        this(src, src._ignoreAllUnknown);
    }

    protected BeanDeserializerBase(BeanDeserializerBase src, boolean ignoreAllUnknown)
    {
        super(src._beanType);

        _beanType = src._beanType;

        _valueInstantiator = src._valueInstantiator;
        _delegateDeserializer = src._delegateDeserializer;
        _arrayDelegateDeserializer = src._arrayDelegateDeserializer;
        _propertyBasedCreator = src._propertyBasedCreator;

        _beanProperties = src._beanProperties;
        _backRefs = src._backRefs;
        _ignorableProps = src._ignorableProps;
        _ignoreAllUnknown = ignoreAllUnknown;
        _includableProps = src._includableProps;
        _anySetter = src._anySetter;
        _injectables = src._injectables;
        _objectIdReader = src._objectIdReader;

        _nonStandardCreation = src._nonStandardCreation;
        _unwrappedPropertyHandler = src._unwrappedPropertyHandler;
        _needViewProcesing = src._needViewProcesing;
        _serializationShape = src._serializationShape;

        _vanillaProcessing = src._vanillaProcessing;
    }

    protected BeanDeserializerBase(BeanDeserializerBase src, NameTransformer unwrapper)
    {
        super(src._beanType);

        _beanType = src._beanType;

        _valueInstantiator = src._valueInstantiator;
        _delegateDeserializer = src._delegateDeserializer;
        _arrayDelegateDeserializer = src._arrayDelegateDeserializer;
        _propertyBasedCreator = src._propertyBasedCreator;

        _backRefs = src._backRefs;
        _ignorableProps = src._ignorableProps;
        _ignoreAllUnknown = (unwrapper != null) || src._ignoreAllUnknown;
        _includableProps = src._includableProps;
        _anySetter = src._anySetter;
        _injectables = src._injectables;
        _objectIdReader = src._objectIdReader;

        _nonStandardCreation = src._nonStandardCreation;
        UnwrappedPropertyHandler uph = src._unwrappedPropertyHandler;

        if (unwrapper != null) {
            // delegate further unwraps, if any
            if (uph != null) { // got handler, delegate
                uph = uph.renameAll(unwrapper);
            }
            // and handle direct unwrapping as well:
            _beanProperties = src._beanProperties.renameAll(unwrapper);
        } else {
            _beanProperties = src._beanProperties;
        }
        _unwrappedPropertyHandler = uph;
        _needViewProcesing = src._needViewProcesing;
        _serializationShape = src._serializationShape;

        // probably adds a twist, so:
        _vanillaProcessing = false;
    }

    public BeanDeserializerBase(BeanDeserializerBase src, ObjectIdReader oir)
    {
        super(src._beanType);
        _beanType = src._beanType;

        _valueInstantiator = src._valueInstantiator;
        _delegateDeserializer = src._delegateDeserializer;
        _arrayDelegateDeserializer = src._arrayDelegateDeserializer;
        _propertyBasedCreator = src._propertyBasedCreator;

        _backRefs = src._backRefs;
        _ignorableProps = src._ignorableProps;
        _ignoreAllUnknown = src._ignoreAllUnknown;
        _includableProps = src._includableProps;
        _anySetter = src._anySetter;
        _injectables = src._injectables;

        _nonStandardCreation = src._nonStandardCreation;
        _unwrappedPropertyHandler = src._unwrappedPropertyHandler;
        _needViewProcesing = src._needViewProcesing;
        _serializationShape = src._serializationShape;

        // then actual changes:
        _objectIdReader = oir;

        if (oir == null) {
            _beanProperties = src._beanProperties;
            _vanillaProcessing = src._vanillaProcessing;
        } else {
            /* 18-Nov-2012, tatu: May or may not have annotations for id property;
             *   but no easy access. But hard to see id property being optional,
             *   so let's consider required at this point.
             */
            ObjectIdValueProperty idProp = new ObjectIdValueProperty(oir, PropertyMetadata.STD_REQUIRED);
            _beanProperties = src._beanProperties.withProperty(idProp);
            _vanillaProcessing = false;
        }
    }

    /**
     * @since 2.12
     */
    public BeanDeserializerBase(BeanDeserializerBase src,
            Set<String> ignorableProps, Set<String> includableProps)
    {
        super(src._beanType);
        _beanType = src._beanType;

        _valueInstantiator = src._valueInstantiator;
        _delegateDeserializer = src._delegateDeserializer;
        _arrayDelegateDeserializer = src._arrayDelegateDeserializer;
        _propertyBasedCreator = src._propertyBasedCreator;

        _backRefs = src._backRefs;
        _ignorableProps = ignorableProps;
        _ignoreAllUnknown = src._ignoreAllUnknown;
        _includableProps = includableProps;
        _anySetter = src._anySetter;
        _injectables = src._injectables;

        _nonStandardCreation = src._nonStandardCreation;
        _unwrappedPropertyHandler = src._unwrappedPropertyHandler;
        _needViewProcesing = src._needViewProcesing;
        _serializationShape = src._serializationShape;

        _vanillaProcessing = src._vanillaProcessing;
        _objectIdReader = src._objectIdReader;

        // 01-May-2016, tatu: [databind#1217]: Remove properties from mapping,
        //    to avoid them being deserialized
        _beanProperties = src._beanProperties.withoutProperties(ignorableProps, includableProps);
    }

    /**
     * @since 2.8
     */
    protected BeanDeserializerBase(BeanDeserializerBase src, BeanPropertyMap beanProps)
    {
        super(src._beanType);
        _beanType = src._beanType;

        _valueInstantiator = src._valueInstantiator;
        _delegateDeserializer = src._delegateDeserializer;
        _arrayDelegateDeserializer = src._arrayDelegateDeserializer;
        _propertyBasedCreator = src._propertyBasedCreator;

        _beanProperties = beanProps;
        _backRefs = src._backRefs;
        _ignorableProps = src._ignorableProps;
        _ignoreAllUnknown = src._ignoreAllUnknown;
        _includableProps = src._includableProps;
        _anySetter = src._anySetter;
        _injectables = src._injectables;
        _objectIdReader = src._objectIdReader;

        _nonStandardCreation = src._nonStandardCreation;
        _unwrappedPropertyHandler = src._unwrappedPropertyHandler;
        _needViewProcesing = src._needViewProcesing;
        _serializationShape = src._serializationShape;

        _vanillaProcessing = src._vanillaProcessing;
    }

    @Deprecated // since 2.12
    protected BeanDeserializerBase(BeanDeserializerBase src, Set<String> ignorableProps)
    {
        this(src, ignorableProps, src._includableProps);
    }

    @Override
    public abstract JsonDeserializer<Object> unwrappingDeserializer(NameTransformer unwrapper);

    public abstract BeanDeserializerBase withObjectIdReader(ObjectIdReader oir);

    /**
     * @since 2.12
     */
    public abstract BeanDeserializerBase withByNameInclusion(Set<String> ignorableProps, Set<String> includableProps);

    /**
     * @since 2.11
     */
    public abstract BeanDeserializerBase withIgnoreAllUnknown(boolean ignoreUnknown);

    /**
     * Mutant factory method that custom sub-classes must override; not left as
     * abstract to prevent more drastic backwards compatibility problems.
     *
     * @since 2.8
     */
    public BeanDeserializerBase withBeanProperties(BeanPropertyMap props) {
        throw new UnsupportedOperationException("Class "+getClass().getName()
                +" does not override `withBeanProperties()`, needs to");
    }

    /**
     * Fluent factory for creating a variant that can handle
     * POJO output as a JSON Array. Implementations may ignore this request
     * if no such input is possible.
     *
     * @since 2.1
     */
    protected abstract BeanDeserializerBase asArrayDeserializer();

    /**
     * @deprecated Since 2.12 use {@link #withByNameInclusion} instead
     */
    @Deprecated
    public BeanDeserializerBase withIgnorableProperties(Set<String> ignorableProps) {
        return withByNameInclusion(ignorableProps, _includableProps);
    }

    /*
    /**********************************************************
    /* Validation, post-processing
    /**********************************************************
     */

    /**
     * Method called to finalize setup of this deserializer,
     * after deserializer itself has been registered.
     * This is needed to handle recursive and transitive dependencies.
     */
    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException
    {
        ExternalTypeHandler.Builder extTypes = null;
        // if ValueInstantiator can use "creator" approach, need to resolve it here...
        SettableBeanProperty[] creatorProps;

        if (_valueInstantiator.canCreateFromObjectWith()) {
            creatorProps = _valueInstantiator.getFromObjectArguments(ctxt.getConfig());

            // 22-Jan-2018, tatu: May need to propagate "ignorable" status (from `Access.READ_ONLY`
            //     or perhaps class-ignorables) into Creator properties too. Can not just delete,
            //     at this point, but is needed for further processing down the line
            if (_ignorableProps != null || _includableProps != null) {
                for (int i = 0, end = creatorProps.length; i < end; ++i) {
                    SettableBeanProperty prop  = creatorProps[i];
                    if (IgnorePropertiesUtil.shouldIgnore(prop.getName(), _ignorableProps, _includableProps)) {
                        creatorProps[i].markAsIgnorable();
                    }
                }
            }
        } else {
            creatorProps = null;
        }
        UnwrappedPropertyHandler unwrapped = null;

        // 24-Mar-2017, tatu: Looks like we may have to iterate over
        //   properties twice, to handle potential issues with recursive
        //   types (see [databind#1575] f.ex).
        // First loop: find deserializer if not yet known, but do not yet
        // contextualize (since that can lead to problems with self-references)
        // 22-Jan-2018, tatu: NOTE! Need not check for `isIgnorable` as that can
        //   only happen for props in `creatorProps`

        for (SettableBeanProperty prop : _beanProperties) {
            if (!prop.hasValueDeserializer()) {
                // [databind#125]: allow use of converters
                JsonDeserializer<?> deser = findConvertingDeserializer(ctxt, prop);
                if (deser == null) {
                    deser = ctxt.findNonContextualValueDeserializer(prop.getType());
                }
                SettableBeanProperty newProp = prop.withValueDeserializer(deser);
                _replaceProperty(_beanProperties, creatorProps, prop, newProp);
            }
        }

        // Second loop: contextualize, find other pieces
        for (SettableBeanProperty origProp : _beanProperties) {
            SettableBeanProperty prop = origProp;
            JsonDeserializer<?> deser = prop.getValueDeserializer();
            deser = ctxt.handlePrimaryContextualization(deser, prop, prop.getType());
            prop = prop.withValueDeserializer(deser);
            // Need to link managed references with matching back references
            prop = _resolveManagedReferenceProperty(ctxt, prop);

            // [databind#351]: need to wrap properties that require object id resolution.
            if (!(prop instanceof ManagedReferenceProperty)) {
                prop = _resolvedObjectIdProperty(ctxt, prop);
            }
            // Support unwrapped values (via @JsonUnwrapped)
            NameTransformer xform = _findPropertyUnwrapper(ctxt, prop);
            if (xform != null) {
                JsonDeserializer<Object> orig = prop.getValueDeserializer();
                JsonDeserializer<Object> unwrapping = orig.unwrappingDeserializer(xform);
                if (unwrapping != orig && unwrapping != null) {
                    prop = prop.withValueDeserializer(unwrapping);
                    if (unwrapped == null) {
                        unwrapped = new UnwrappedPropertyHandler();
                    }
                    unwrapped.addProperty(prop);
                    // 12-Dec-2014, tatu: As per [databind#647], we will have problems if
                    //    the original property is left in place. So let's remove it now.
                    // 25-Mar-2017, tatu: Wonder if this could be problematic wrt creators?
                    //    (that is, should be remove it from creator too)
                    _beanProperties.remove(prop);
                    continue;
                }
            }

            // 26-Oct-2016, tatu: Need to have access to value deserializer to know if
            //   merging needed, and now seems to be reasonable time to do that.
            final PropertyMetadata md = prop.getMetadata();
            prop = _resolveMergeAndNullSettings(ctxt, prop, md);

            // non-static inner classes too:
            prop = _resolveInnerClassValuedProperty(ctxt, prop);
            if (prop != origProp) {
                _replaceProperty(_beanProperties, creatorProps, origProp, prop);
            }

            // one more thing: if this property uses "external property" type inclusion,
            // it needs different handling altogether
            if (prop.hasValueTypeDeserializer()) {
                TypeDeserializer typeDeser = prop.getValueTypeDeserializer();
                if (typeDeser.getTypeInclusion() == JsonTypeInfo.As.EXTERNAL_PROPERTY) {
                    if (extTypes == null) {
                        extTypes = ExternalTypeHandler.builder(_beanType);
                    }
                    extTypes.addExternal(prop, typeDeser);
                    // In fact, remove from list of known properties to simplify later handling
                    _beanProperties.remove(prop);
                    continue;
                }
            }
        }
        // "any setter" may also need to be resolved now
        if ((_anySetter != null) && !_anySetter.hasValueDeserializer()) {
            _anySetter = _anySetter.withValueDeserializer(findDeserializer(ctxt,
                    _anySetter.getType(), _anySetter.getProperty()));
        }
        // as well as delegate-based constructor:
        if (_valueInstantiator.canCreateUsingDelegate()) {
            JavaType delegateType = _valueInstantiator.getDelegateType(ctxt.getConfig());
            if (delegateType == null) {
                ctxt.reportBadDefinition(_beanType, String.format(
"Invalid delegate-creator definition for %s: value instantiator (%s) returned true for 'canCreateUsingDelegate()', but null for 'getDelegateType()'",
ClassUtil.getTypeDescription(_beanType), ClassUtil.classNameOf(_valueInstantiator)));
            }
            _delegateDeserializer = _findDelegateDeserializer(ctxt, delegateType,
                    _valueInstantiator.getDelegateCreator());
        }

        // and array-delegate-based constructor:
        if (_valueInstantiator.canCreateUsingArrayDelegate()) {
            JavaType delegateType = _valueInstantiator.getArrayDelegateType(ctxt.getConfig());
            if (delegateType == null) {
                ctxt.reportBadDefinition(_beanType, String.format(
"Invalid delegate-creator definition for %s: value instantiator (%s) returned true for 'canCreateUsingArrayDelegate()', but null for 'getArrayDelegateType()'",
ClassUtil.getTypeDescription(_beanType), ClassUtil.classNameOf(_valueInstantiator)));
            }
            _arrayDelegateDeserializer = _findDelegateDeserializer(ctxt, delegateType,
                    _valueInstantiator.getArrayDelegateCreator());
        }

        // And now that we know CreatorProperty instances are also resolved can finally create the creator:
        if (creatorProps != null) {
            _propertyBasedCreator = PropertyBasedCreator.construct(ctxt, _valueInstantiator,
                    creatorProps, _beanProperties);
        }

        if (extTypes != null) {
            // 21-Jun-2016, tatu: related to [databind#999], may need to link type ids too,
            //    so need to pass collected properties
            _externalTypeIdHandler = extTypes.build(_beanProperties);
            // we consider this non-standard, to offline handling
            _nonStandardCreation = true;
        }

        _unwrappedPropertyHandler = unwrapped;
        if (unwrapped != null) { // we consider this non-standard, to offline handling
            _nonStandardCreation = true;
        }
        // may need to disable vanilla processing, if unwrapped handling was enabled...
        _vanillaProcessing = _vanillaProcessing && !_nonStandardCreation;
    }

    /**
     * @since 2.8.8
     */
    protected void _replaceProperty(BeanPropertyMap props, SettableBeanProperty[] creatorProps,
            SettableBeanProperty origProp, SettableBeanProperty newProp)
    {
        props.replace(origProp, newProp);
        // [databind#795]: Make sure PropertyBasedCreator's properties stay in sync
        if (creatorProps != null) {
            // 18-May-2015, tatu: _Should_ start with consistent set. But can we really
            //   fully count on this? May need to revisit in future; seems to hold for now.
            for (int i = 0, len = creatorProps.length; i < len; ++i) {
                if (creatorProps[i] == origProp) {
                    creatorProps[i] = newProp;
                    return;
                }
            }
            /*
            // ... as per above, it is possible we'd need to add this as fallback
            // if (but only if) identity check fails?
            for (int i = 0, len = creatorProps.length; i < len; ++i) {
                if (creatorProps[i].getName().equals(origProp.getName())) {
                    creatorProps[i] = newProp;
                    return;
                }
            }
            */
        }
    }

    @SuppressWarnings("unchecked")
    private JsonDeserializer<Object> _findDelegateDeserializer(DeserializationContext ctxt,
            JavaType delegateType, AnnotatedWithParams delegateCreator) throws JsonMappingException
    {
        // Need to create a temporary property to allow contextual deserializers:
        BeanProperty.Std property = new BeanProperty.Std(TEMP_PROPERTY_NAME,
                delegateType, null, delegateCreator,
                PropertyMetadata.STD_OPTIONAL);
        TypeDeserializer td = delegateType.getTypeHandler();
        if (td == null) {
            td = ctxt.getConfig().findTypeDeserializer(delegateType);
        }
        // 04-May-2018, tatu: [databind#2021] check if there's custom deserializer attached
        //    to type (resolved from parameter)
        JsonDeserializer<Object> dd = delegateType.getValueHandler();
        if (dd == null) {
            dd = findDeserializer(ctxt, delegateType, property);
        } else {
            dd = (JsonDeserializer<Object>) ctxt.handleSecondaryContextualization(dd, property, delegateType);
        }
        if (td != null) {
            td = td.forProperty(property);
            return new TypeWrappedDeserializer(td, dd);
        }
        return dd;
    }

    /**
     * Helper method that can be used to see if specified property is annotated
     * to indicate use of a converter for property value (in case of container types,
     * it is container type itself, not key or content type).
     *<p>
     * NOTE: returned deserializer is NOT yet contextualized, caller needs to take
     * care to do that.
     *
     * @since 2.2
     */
    protected JsonDeserializer<Object> findConvertingDeserializer(DeserializationContext ctxt,
            SettableBeanProperty prop)
        throws JsonMappingException
    {
        final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr != null) {
            Object convDef = intr.findDeserializationConverter(prop.getMember());
            if (convDef != null) {
                Converter<Object,Object> conv = ctxt.converterInstance(prop.getMember(), convDef);
                JavaType delegateType = conv.getInputType(ctxt.getTypeFactory());
                // 25-Mar-2017, tatu: should not yet contextualize
//                JsonDeserializer<?> deser = ctxt.findContextualValueDeserializer(delegateType, prop);
                JsonDeserializer<?> deser = ctxt.findNonContextualValueDeserializer(delegateType);
                return new StdDelegatingDeserializer<Object>(conv, delegateType, deser);
            }
        }
        return null;
    }

    /**
     * Although most of post-processing is done in resolve(), we only get
     * access to referring property's annotations here; and this is needed
     * to support per-property ObjectIds.
     * We will also consider Shape transformations (read from Array) at this
     * point, since it may come from either Class definition or property.
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        ObjectIdReader oir = _objectIdReader;

        // First: may have an override for Object Id:
        final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        final AnnotatedMember accessor = _neitherNull(property, intr) ? property.getMember() : null;
        if (accessor != null) {
            ObjectIdInfo objectIdInfo = intr.findObjectIdInfo(accessor);
            if (objectIdInfo != null) { // some code duplication here as well (from BeanDeserializerFactory)
                // 2.1: allow modifications by "id ref" annotations as well:
                objectIdInfo = intr.findObjectReferenceInfo(accessor, objectIdInfo);

                Class<?> implClass = objectIdInfo.getGeneratorType();
                // Property-based generator is trickier
                JavaType idType;
                SettableBeanProperty idProp;
                ObjectIdGenerator<?> idGen;
                ObjectIdResolver resolver = ctxt.objectIdResolverInstance(accessor, objectIdInfo);
                if (implClass == ObjectIdGenerators.PropertyGenerator.class) {
                    PropertyName propName = objectIdInfo.getPropertyName();
                    idProp = findProperty(propName);
                    if (idProp == null) {
                        return ctxt.reportBadDefinition(_beanType, String.format(
"Invalid Object Id definition for %s: cannot find property with name %s",
ClassUtil.nameOf(handledType()), ClassUtil.name(propName)));
                    }
                    idType = idProp.getType();
                    idGen = new PropertyBasedObjectIdGenerator(objectIdInfo.getScope());
                } else { // other types are to be simpler
                    JavaType type = ctxt.constructType(implClass);
                    idType = ctxt.getTypeFactory().findTypeParameters(type, ObjectIdGenerator.class)[0];
                    idProp = null;
                    idGen = ctxt.objectIdGeneratorInstance(accessor, objectIdInfo);
                }
                JsonDeserializer<?> deser = ctxt.findRootValueDeserializer(idType);
                oir = ObjectIdReader.construct(idType, objectIdInfo.getPropertyName(),
                		idGen, deser, idProp, resolver);
            }
        }
        // either way, need to resolve serializer:
        BeanDeserializerBase contextual = this;
        if (oir != null && oir != _objectIdReader) {
            contextual = contextual.withObjectIdReader(oir);
        }
        // And possibly add more properties to ignore
        if (accessor != null) {
            contextual = _handleByNameInclusion(ctxt, intr, contextual, accessor);
        }

        // One more thing: are we asked to serialize POJO as array?
        JsonFormat.Value format = findFormatOverrides(ctxt, property, handledType());
        JsonFormat.Shape shape = null;
        if (format != null) {
            if (format.hasShape()) {
                shape = format.getShape();
            }
            // 16-May-2016, tatu: How about per-property case-insensitivity?
            Boolean B = format.getFeature(JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
            if (B != null) {
                BeanPropertyMap propsOrig = _beanProperties;
                BeanPropertyMap props = propsOrig.withCaseInsensitivity(B.booleanValue());
                if (props != propsOrig) {
                    contextual = contextual.withBeanProperties(props);
                }
            }
        }

        if (shape == null) {
            shape = _serializationShape;
        }
        if (shape == JsonFormat.Shape.ARRAY) {
            contextual = contextual.asArrayDeserializer();
        }
        return contextual;
    }

    // @since 2.12
    protected BeanDeserializerBase _handleByNameInclusion(DeserializationContext ctxt,
            AnnotationIntrospector intr,
            BeanDeserializerBase contextual,
            AnnotatedMember accessor) throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        JsonIgnoreProperties.Value ignorals = intr.findPropertyIgnoralByName(config, accessor);

        // 30-Mar-2020, tatu: As per [databind#2627], need to also allow
        //    per-property override to "ignore all unknown".
        //  NOTE: there is no way to override with `false` because annotation
        //  defaults to `false` (i.e. can not know if `false` is explicit value)
        if (ignorals.getIgnoreUnknown() && !_ignoreAllUnknown) {
            contextual = contextual.withIgnoreAllUnknown(true);
        }

        final Set<String> namesToIgnore = ignorals.findIgnoredForDeserialization();
        final Set<String> prevNamesToIgnore = contextual._ignorableProps;
        final Set<String> newNamesToIgnore;

        if (namesToIgnore.isEmpty()) {
            newNamesToIgnore = prevNamesToIgnore;
        } else if ((prevNamesToIgnore == null) || prevNamesToIgnore.isEmpty()) {
            newNamesToIgnore = namesToIgnore;
        } else {
            newNamesToIgnore = new HashSet<String>(prevNamesToIgnore);
            newNamesToIgnore.addAll(namesToIgnore);
        }

        final Set<String> prevNamesToInclude = contextual._includableProps;
        final Set<String> newNamesToInclude = IgnorePropertiesUtil.combineNamesToInclude(prevNamesToInclude,
                intr.findPropertyInclusionByName(config, accessor).getIncluded());

        if ((newNamesToIgnore != prevNamesToIgnore)
                || (newNamesToInclude != prevNamesToInclude)) {
            contextual = contextual.withByNameInclusion(newNamesToIgnore, newNamesToInclude);
        }
        return contextual;
    }

    /**
     * Helper method called to see if given property is part of 'managed' property
     * pair (managed + back reference), and if so, handle resolution details.
     */
    protected SettableBeanProperty _resolveManagedReferenceProperty(DeserializationContext ctxt,
            SettableBeanProperty prop)
        throws JsonMappingException
    {
        String refName = prop.getManagedReferenceName();
        if (refName == null) {
            return prop;
        }
        JsonDeserializer<?> valueDeser = prop.getValueDeserializer();
        SettableBeanProperty backProp = valueDeser.findBackReference(refName);
        if (backProp == null) {
            return ctxt.reportBadDefinition(_beanType, String.format(
"Cannot handle managed/back reference %s: no back reference property found from type %s",
ClassUtil.name(refName), ClassUtil.getTypeDescription(prop.getType())));
        }
        // also: verify that type is compatible
        JavaType referredType = _beanType;
        JavaType backRefType = backProp.getType();
        boolean isContainer = prop.getType().isContainerType();
        if (!backRefType.getRawClass().isAssignableFrom(referredType.getRawClass())) {
            ctxt.reportBadDefinition(_beanType, String.format(
"Cannot handle managed/back reference %s: back reference type (%s) not compatible with managed type (%s)",
ClassUtil.name(refName), ClassUtil.getTypeDescription(backRefType),
                    referredType.getRawClass().getName()));
        }
        return new ManagedReferenceProperty(prop, refName, backProp, isContainer);
    }

    /**
     * Method that wraps given property with {@link ObjectIdReferenceProperty}
     * in case where object id resolution is required.
     */
    protected SettableBeanProperty _resolvedObjectIdProperty(DeserializationContext ctxt,
            SettableBeanProperty prop) throws JsonMappingException
    {
        ObjectIdInfo objectIdInfo = prop.getObjectIdInfo();
        JsonDeserializer<Object> valueDeser = prop.getValueDeserializer();
        ObjectIdReader objectIdReader = (valueDeser == null) ? null : valueDeser.getObjectIdReader();
        if (objectIdInfo == null && objectIdReader == null) {
            return prop;
        }
        return new ObjectIdReferenceProperty(prop, objectIdInfo);
    }

    /**
     * Helper method called to see if given property might be so-called unwrapped
     * property: these require special handling.
     */
    protected NameTransformer _findPropertyUnwrapper(DeserializationContext ctxt,
            SettableBeanProperty prop)
        throws JsonMappingException
    {
        AnnotatedMember am = prop.getMember();
        if (am != null) {
            NameTransformer unwrapper = ctxt.getAnnotationIntrospector().findUnwrappingNameTransformer(am);
            if (unwrapper != null) {
                // 01-Dec-2016, tatu: As per [databind#265] we cannot yet support passing
                //   of unwrapped values through creator properties, so fail fast
                if (prop instanceof CreatorProperty) {
                    ctxt.reportBadDefinition(getValueType(), String.format(
                            "Cannot define Creator property \"%s\" as `@JsonUnwrapped`: combination not yet supported",
                            prop.getName()));
                }
                return unwrapper;
            }
        }
        return null;
    }

    /**
     * Helper method that will handle gruesome details of dealing with properties
     * that have non-static inner class as value...
     */
    protected SettableBeanProperty _resolveInnerClassValuedProperty(DeserializationContext ctxt,
            SettableBeanProperty prop)
    {
        /* Should we encounter a property that has non-static inner-class
         * as value, we need to add some more magic to find the "hidden" constructor...
         */
        JsonDeserializer<Object> deser = prop.getValueDeserializer();
        // ideally wouldn't rely on it being BeanDeserializerBase; but for now it'll have to do
        if (deser instanceof BeanDeserializerBase) {
            BeanDeserializerBase bd = (BeanDeserializerBase) deser;
            ValueInstantiator vi = bd.getValueInstantiator();
            if (!vi.canCreateUsingDefault()) { // no default constructor
                Class<?> valueClass = prop.getType().getRawClass();
                // NOTE: almost same as `isNonStaticInnerClass()` but need to know enclosing...
                Class<?> enclosing = ClassUtil.getOuterClass(valueClass);
                // and is inner class of the bean class...
                if ((enclosing != null) && (enclosing == _beanType.getRawClass())) {
                    for (Constructor<?> ctor : valueClass.getConstructors()) {
                        if (ctor.getParameterCount() == 1) {
                            Class<?>[] paramTypes = ctor.getParameterTypes();
                            if (enclosing.equals(paramTypes[0])) {
                                if (ctxt.canOverrideAccessModifiers()) {
                                    ClassUtil.checkAndFixAccess(ctor, ctxt.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
                                }
                                return new InnerClassProperty(prop, ctor);
                            }
                        }
                    }
                }
            }
        }
        return prop;
    }

    // @since 2.9
    protected SettableBeanProperty _resolveMergeAndNullSettings(DeserializationContext ctxt,
            SettableBeanProperty prop, PropertyMetadata propMetadata)
        throws JsonMappingException
    {
        PropertyMetadata.MergeInfo merge = propMetadata.getMergeInfo();
        // First mergeability
        if (merge != null) {
            JsonDeserializer<?> valueDeser = prop.getValueDeserializer();
            Boolean mayMerge = valueDeser.supportsUpdate(ctxt.getConfig());

            if (mayMerge == null) {
                // we don't really know if it's ok; so only use if explicitly specified
                if (merge.fromDefaults) {
                    return prop;
                }
            } else if (!mayMerge.booleanValue()) { // prevented
                if (!merge.fromDefaults) {
                    // If attempt was made via explicit annotation/per-type config override,
                    // should be reported; may or may not result in exception
                    ctxt.handleBadMerge(valueDeser);
                }
                return prop;
            }
            // Anyway; if we get this far, do enable merging
            AnnotatedMember accessor = merge.getter;
            accessor.fixAccess(ctxt.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            if (!(prop instanceof SetterlessProperty)) {
                prop = MergingSettableBeanProperty.construct(prop, accessor);
            }
        }

        // And after this, see if we require non-standard null handling
        NullValueProvider nuller = findValueNullProvider(ctxt, prop, propMetadata);
        if (nuller != null) {
            prop = prop.withNullProvider(nuller);
        }
        return prop;
    }

    /*
    /**********************************************************
    /* Public accessors; null/empty value providers
    /**********************************************************
     */

    @Override
    public AccessPattern getNullAccessPattern() {
        // POJO types do not have custom `null` values
        return AccessPattern.ALWAYS_NULL;
    }

    @Override
    public AccessPattern getEmptyAccessPattern() {
        // Empty values cannot be shared
        return AccessPattern.DYNAMIC;
    }

    @Override // since 2.9
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        // alas, need to promote exception, if any:
        try {
            // 20-Nov-2022, tatu: Ok one more complication; may want to consider
            //     EITHER default Creator OR properties-one with no args.
            //     But that is encapsulated by `ValueInstantiator` now
            // return _valueInstantiator.createUsingDefault(ctxt);
            return _valueInstantiator.createUsingDefaultOrWithoutArguments(ctxt);
        } catch (IOException e) {
            return ClassUtil.throwAsMappingException(ctxt, e);
        }
    }

    /*
    /**********************************************************
    /* Public accessors; other
    /**********************************************************
     */

    @Override
    public boolean isCachable() { return true; }

    /**
     * Accessor for checking whether this deserializer is operating
     * in case-insensitive manner.
     *
     * @return True if this deserializer should match property names without
     *    considering casing; false if case has to match exactly.
     *
     * @since 2.12
     */
    public boolean isCaseInsensitive() {
        return _beanProperties.isCaseInsensitive();
    }

    @Override // since 2.9
    public Boolean supportsUpdate(DeserializationConfig config) {
        // although with possible caveats, yes, values can be updated
        // 23-Oct-2016, tatu: Perhaps in future could and should verify from
        //   bean settings...
        return Boolean.TRUE;
    }

    @Override
    public Class<?> handledType() {
        return _beanType.getRawClass();
    }

    /**
     * Overridden to return true for those instances that are
     * handling value for which Object Identity handling is enabled
     * (either via value type or referring property).
     */
    @Override
    public ObjectIdReader getObjectIdReader() {
        return _objectIdReader;
    }

    public boolean hasProperty(String propertyName) {
        return _beanProperties.find(propertyName) != null;
    }

    public boolean hasViews() {
        return _needViewProcesing;
    }

    /**
     * Accessor for checking number of deserialized properties.
     */
    public int getPropertyCount() {
        return _beanProperties.size();
    }

    @Override
    public Collection<Object> getKnownPropertyNames() {
        ArrayList<Object> names = new ArrayList<Object>();
        for (SettableBeanProperty prop : _beanProperties) {
            names.add(prop.getName());
        }
        return names;
    }

    /**
     * @deprecated Since 2.3, use {@link #handledType()} instead
     */
    @Deprecated
    public final Class<?> getBeanClass() { return _beanType.getRawClass(); }

    @Override
    public JavaType getValueType() { return _beanType; }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.POJO;
    }

    /**
     * Accessor for iterating over properties this deserializer uses; with
     * the exception that properties passed via Creator methods
     * (specifically, "property-based constructor") are not included,
     * but can be accessed separate by calling
     * {@link #creatorProperties}
     */
    public Iterator<SettableBeanProperty> properties()
    {
        if (_beanProperties == null) {
            throw new IllegalStateException("Can only call after BeanDeserializer has been resolved");
        }
        return _beanProperties.iterator();
    }

    /**
     * Accessor for finding properties that represents values to pass
     * through property-based creator method (constructor or
     * factory method)
     *
     * @since 2.0
     */
    public Iterator<SettableBeanProperty> creatorProperties()
    {
        if (_propertyBasedCreator == null) {
            return Collections.<SettableBeanProperty>emptyList().iterator();
        }
        return _propertyBasedCreator.properties().iterator();
    }

    public SettableBeanProperty findProperty(PropertyName propertyName)
    {
        // TODO: start matching full name?
        return findProperty(propertyName.getSimpleName());
    }

    /**
     * Accessor for finding the property with given name, if POJO
     * has one. Name used is the external name, i.e. name used
     * in external data representation (JSON).
     *
     * @since 2.0
     */
    public SettableBeanProperty findProperty(String propertyName)
    {
        SettableBeanProperty prop = (_beanProperties == null) ?
                null : _beanProperties.find(propertyName);
        if (prop == null && _propertyBasedCreator != null) {
            prop = _propertyBasedCreator.findCreatorProperty(propertyName);
        }
        return prop;
    }

    /**
     * Alternate find method that tries to locate a property with given
     * <code>property index</code>.
     * Note that access by index is not necessarily faster than by name,
     * since properties are not directly indexable; however, for most
     * instances difference is not significant as number of properties
     * is low.
     *
     * @since 2.3
     */
    public SettableBeanProperty findProperty(int propertyIndex)
    {
        SettableBeanProperty prop = (_beanProperties == null) ?
                null : _beanProperties.find(propertyIndex);
        if (prop == null && _propertyBasedCreator != null) {
            prop = _propertyBasedCreator.findCreatorProperty(propertyIndex);
        }
        return prop;
    }

    /**
     * Method needed by {@link BeanDeserializerFactory} to properly link
     * managed- and back-reference pairs.
     */
    @Override
    public SettableBeanProperty findBackReference(String logicalName)
    {
        if (_backRefs == null) {
            return null;
        }
        return _backRefs.get(logicalName);
    }

    @Override // ValueInstantiator.Gettable
    public ValueInstantiator getValueInstantiator() {
        return _valueInstantiator;
    }

    /*
    /**********************************************************
    /* Mutators
    /**********************************************************
     */

    /**
     * Method that can be used to replace an existing property with
     * a modified one.
     *<p>
     * NOTE: only ever use this method if you know what you are doing;
     * incorrect usage can break deserializer.
     *
     * @param original Property to replace
     * @param replacement Property to replace it with
     *
     * @since 2.1
     */
    public void replaceProperty(SettableBeanProperty original,
            SettableBeanProperty replacement)
    {
        _beanProperties.replace(original, replacement);
    }

    /*
    /**********************************************************
    /* Partial deserializer implementation
    /**********************************************************
     */

    /**
     * General version used when handling needs more advanced
     * features.
     */
    public abstract Object deserializeFromObject(JsonParser p, DeserializationContext ctxt)
        throws IOException;

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException
    {
        // 16-Feb-2012, tatu: ObjectId may be used as well... need to check that first
        if (_objectIdReader != null) {
            // 05-Aug-2013, tatu: May use native Object Id
            if (p.canReadObjectId()) {
                Object id = p.getObjectId();
                if (id != null) {
                    Object ob = typeDeserializer.deserializeTypedFromObject(p, ctxt);
                    return _handleTypedObjectId(p, ctxt, ob, id);
                }
            }
            // or, Object Ids Jackson explicitly sets
            JsonToken t = p.currentToken();
            if (t != null) {
                // Most commonly, a scalar (int id, uuid String, ...)
                if (t.isScalarValue()) {
                    return deserializeFromObjectId(p, ctxt);
                }
                // but, with 2.5+, a simple Object-wrapped value also legal:
                if (t == JsonToken.START_OBJECT) {
                    t = p.nextToken();
                }
                if ((t == JsonToken.FIELD_NAME) && _objectIdReader.maySerializeAsObject()
                        && _objectIdReader.isValidReferencePropertyName(p.currentName(), p)) {
                    return deserializeFromObjectId(p, ctxt);
                }
            }
        }
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromObject(p, ctxt);
    }

    /**
     * Offlined method called to handle "native" Object Id that has been read
     * and known to be associated with given deserialized POJO.
     *
     * @since 2.3
     */
    protected Object _handleTypedObjectId(JsonParser p, DeserializationContext ctxt,
            Object pojo, Object rawId)
        throws IOException
    {
        // One more challenge: type of id may not be type of property we are expecting
        // later on; specifically, numeric ids vs Strings.
        JsonDeserializer<Object> idDeser = _objectIdReader.getDeserializer();
        final Object id;

        // Ok, this is bit ridiculous; let's see if conversion is needed:
        if (idDeser.handledType() == rawId.getClass()) {
            // nope: already same type
            id = rawId;
        } else {
            id = _convertObjectId(p, ctxt, rawId, idDeser);
        }

        ReadableObjectId roid = ctxt.findObjectId(id, _objectIdReader.generator, _objectIdReader.resolver);
        roid.bindItem(pojo);
        // also: may need to set a property value as well
        SettableBeanProperty idProp = _objectIdReader.idProperty;
        if (idProp != null) {
            return idProp.setAndReturn(pojo, id);
        }
        return pojo;
    }

    /**
     * Helper method we need to do necessary conversion from whatever native object id
     * type is, into declared type that Jackson internals expect. This may be
     * simple cast (for String ids), or something more complicated; in latter
     * case we may need to create bogus content buffer to allow use of
     * id deserializer.
     *
     * @since 2.3
     */
    @SuppressWarnings("resource") // TokenBuffers don't need close, nor parser thereof
    protected Object _convertObjectId(JsonParser p, DeserializationContext ctxt,
            Object rawId, JsonDeserializer<Object> idDeser) throws IOException
    {
        TokenBuffer buf = ctxt.bufferForInputBuffering(p);
        if (rawId instanceof String) {
            buf.writeString((String) rawId);
        } else if (rawId instanceof Long) {
            buf.writeNumber(((Long) rawId).longValue());
        } else if (rawId instanceof Integer) {
            buf.writeNumber(((Integer) rawId).intValue());
        } else {
            // should we worry about UUIDs? They should be fine, right?
            // 07-Aug-2014, tatu: Maybe, but not necessarily; had issues with
            //   Smile format; [dataformat-smile#19], possibly related.
            // 01-Sep-2016, tatu: For non-JSON, might want to consider `writeEmbeddedObject`
            //   but that won't work for default impl (JSON and most dataformats)
            buf.writeObject(rawId);
        }
        JsonParser bufParser = buf.asParser(p.streamReadConstraints());
        bufParser.nextToken();
        return idDeser.deserialize(bufParser, ctxt);
    }

    // NOTE: currently only used by standard BeanDeserializer (not Builder-based)
    /**
     * Alternative deserialization method used when we expect to see Object Id;
     * if so, we will need to ensure that the Id is seen before anything
     * else, to ensure that it is available for solving references,
     * even if JSON itself is not ordered that way. This may require
     * buffering in some cases, but usually just a simple lookup to ensure
     * that ordering is correct.
     */
    protected Object deserializeWithObjectId(JsonParser p, DeserializationContext ctxt) throws IOException {
        return deserializeFromObject(p, ctxt);
    }

    /**
     * Method called in cases where it looks like we got an Object Id
     * to parse and use as a reference.
     */
    protected Object deserializeFromObjectId(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        Object id = _objectIdReader.readObjectReference(p, ctxt);
        ReadableObjectId roid = ctxt.findObjectId(id, _objectIdReader.generator, _objectIdReader.resolver);
        // do we have it resolved?
        Object pojo = roid.resolve();
        if (pojo == null) { // not yet; should wait...
            throw new UnresolvedForwardReference(p,
                    "Could not resolve Object Id ["+id+"] (for "+_beanType+").",
                    p.getCurrentLocation(), roid);
        }
        return pojo;
    }

    protected Object deserializeFromObjectUsingNonDefault(JsonParser p,
            DeserializationContext ctxt) throws IOException
    {
        final JsonDeserializer<Object> delegateDeser = _delegateDeserializer();
        if (delegateDeser != null) {
            final Object bean = _valueInstantiator.createUsingDelegate(ctxt,
                    delegateDeser.deserialize(p, ctxt));
            if (_injectables != null) {
                injectValues(ctxt, bean);
            }
            return bean;
        }
        if (_propertyBasedCreator != null) {
            return _deserializeUsingPropertyBased(p, ctxt);
        }
        // 25-Jan-2017, tatu: We do not actually support use of Creators for non-static
        //   inner classes -- with one and only one exception; that of default constructor!
        //   -- so let's indicate it
        Class<?> raw = _beanType.getRawClass();
        if (ClassUtil.isNonStaticInnerClass(raw)) {
            return ctxt.handleMissingInstantiator(raw, null, p,
"non-static inner classes like this can only by instantiated using default, no-argument constructor");
        }
        // 01-May-2022, tatu: [databind#3417] special handling for (Graal) native images
        if (NativeImageUtil.needsReflectionConfiguration(raw)) {
            return ctxt.handleMissingInstantiator(raw, null, p,
                    "cannot deserialize from Object value (no delegate- or property-based Creator): this appears to be a native image, in which case you may need to configure reflection for the class that is to be deserialized");
        }
        return ctxt.handleMissingInstantiator(raw, getValueInstantiator(), p,
"cannot deserialize from Object value (no delegate- or property-based Creator)");
    }

    protected abstract Object _deserializeUsingPropertyBased(final JsonParser p,
            final DeserializationContext ctxt) throws IOException;

    public Object deserializeFromNumber(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // First things first: id Object Id is used, most likely that's it
        if (_objectIdReader != null) {
            return deserializeFromObjectId(p, ctxt);
        }
        final JsonDeserializer<Object> delegateDeser = _delegateDeserializer();
        NumberType nt = p.getNumberType();
        if (nt == NumberType.INT) {
            if (delegateDeser != null) {
                if (!_valueInstantiator.canCreateFromInt()) {
                    Object bean = _valueInstantiator.createUsingDelegate(ctxt,
                            delegateDeser.deserialize(p, ctxt));
                    if (_injectables != null) {
                        injectValues(ctxt, bean);
                    }
                    return bean;
                }
            }
            return _valueInstantiator.createFromInt(ctxt, p.getIntValue());
        }
        if (nt == NumberType.LONG) {
            if (delegateDeser != null) {
                if (!_valueInstantiator.canCreateFromInt()) {
                    Object bean = _valueInstantiator.createUsingDelegate(ctxt,
                            delegateDeser.deserialize(p, ctxt));
                    if (_injectables != null) {
                        injectValues(ctxt, bean);
                    }
                    return bean;
                }
            }
            return _valueInstantiator.createFromLong(ctxt, p.getLongValue());
        }
        if (nt == NumberType.BIG_INTEGER) {
            if (delegateDeser != null) {
                if (!_valueInstantiator.canCreateFromBigInteger()) {
                    Object bean = _valueInstantiator.createUsingDelegate(ctxt, delegateDeser.deserialize(p, ctxt));
                    if (_injectables != null) {
                        injectValues(ctxt, bean);
                    }
                    return bean;
                }
            }
            return _valueInstantiator.createFromBigInteger(ctxt, p.getBigIntegerValue());
        }

        return ctxt.handleMissingInstantiator(handledType(), getValueInstantiator(), p,
                "no suitable creator method found to deserialize from Number value (%s)",
                p.getNumberValue());
    }

    public Object deserializeFromString(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // First things first: id Object Id is used, most likely that's it
        if (_objectIdReader != null) {
            return deserializeFromObjectId(p, ctxt);
        }
        // Bit complicated if we have delegating creator; may need to use it,
        // or might not...
        JsonDeserializer<Object> delegateDeser = _delegateDeserializer();
        if (delegateDeser != null) {
            if (!_valueInstantiator.canCreateFromString()) {
                Object bean = _valueInstantiator.createUsingDelegate(ctxt,
                        delegateDeser.deserialize(p, ctxt));
                if (_injectables != null) {
                    injectValues(ctxt, bean);
                }
                return bean;
            }
        }
        return _deserializeFromString(p, ctxt);
    }

    /**
     * Method called to deserialize POJO value from a JSON floating-point
     * number.
     */
    public Object deserializeFromDouble(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        NumberType t = p.getNumberType();
        // no separate methods for taking float...
        if ((t == NumberType.DOUBLE) || (t == NumberType.FLOAT)) {
            JsonDeserializer<Object> delegateDeser = _delegateDeserializer();
            if (delegateDeser != null) {
                if (!_valueInstantiator.canCreateFromDouble()) {
                    Object bean = _valueInstantiator.createUsingDelegate(ctxt,
                            delegateDeser.deserialize(p, ctxt));
                    if (_injectables != null) {
                        injectValues(ctxt, bean);
                    }
                    return bean;
                }
            }
            return _valueInstantiator.createFromDouble(ctxt, p.getDoubleValue());
        }

        if (t == NumberType.BIG_DECIMAL) {
            JsonDeserializer<Object> delegateDeser = _delegateDeserializer();
            if (delegateDeser != null) {
                if (!_valueInstantiator.canCreateFromBigDecimal()) {
                    Object bean = _valueInstantiator.createUsingDelegate(ctxt, delegateDeser.deserialize(p, ctxt));
                    if (_injectables != null) {
                        injectValues(ctxt, bean);
                    }
                    return bean;
                }
            }

            return _valueInstantiator.createFromBigDecimal(ctxt, p.getDecimalValue());
        }

        return ctxt.handleMissingInstantiator(handledType(), getValueInstantiator(), p,
                "no suitable creator method found to deserialize from Number value (%s)",
                p.getNumberValue());
    }

    /**
     * Method called to deserialize POJO value from a JSON boolean value (true, false)
     */
    public Object deserializeFromBoolean(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        JsonDeserializer<Object> delegateDeser = _delegateDeserializer();
        if (delegateDeser != null) {
            if (!_valueInstantiator.canCreateFromBoolean()) {
                Object bean = _valueInstantiator.createUsingDelegate(ctxt,
                        delegateDeser.deserialize(p, ctxt));
                if (_injectables != null) {
                    injectValues(ctxt, bean);
                }
                return bean;
            }
        }
        boolean value = (p.currentToken() == JsonToken.VALUE_TRUE);
        return _valueInstantiator.createFromBoolean(ctxt, value);
    }

    /**
     * @deprecated Since 2.11 Should not be used: was never meant to be called by
     *    code other than sub-classes (implementations), and implementations details
     *    differ
     */
    @Deprecated
    public Object deserializeFromArray(JsonParser p, DeserializationContext ctxt) throws IOException {
        // should work as subtypes ought to override this method:
        return _deserializeFromArray(p, ctxt);
    }

    public Object deserializeFromEmbedded(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // First things first: id Object Id is used, most likely that's it; specifically,
        // true for UUIDs when written as binary (with Smile, other binary formats)
        if (_objectIdReader != null) {
            return deserializeFromObjectId(p, ctxt);
        }
        // 26-Jul-2017, tatu: as per [databind#1711] need to support delegating case too
        JsonDeserializer<Object> delegateDeser = _delegateDeserializer();
        if (delegateDeser != null) {
            if (!_valueInstantiator.canCreateFromString()) {
                Object bean = _valueInstantiator.createUsingDelegate(ctxt,
                        delegateDeser.deserialize(p, ctxt));
                if (_injectables != null) {
                    injectValues(ctxt, bean);
                }
                return bean;
            }
        }
        // TODO: maybe add support for ValueInstantiator, embedded?

        // 26-Jul-2017, tatu: related to [databind#1711], let's actually verify assignment
        //    compatibility before returning. Bound to catch misconfigured cases and produce
        //    more meaningful exceptions.
        Object value = p.getEmbeddedObject();
        if (value != null) {
            if (!_beanType.isTypeOrSuperTypeOf(value.getClass())) {
                // allow this to be handled...
                value = ctxt.handleWeirdNativeValue(_beanType, value, p);
            }
        }
        return value;
    }

    /**
     * @since 2.9
     */
    protected final JsonDeserializer<Object> _delegateDeserializer() {
        JsonDeserializer<Object> deser = _delegateDeserializer;
        if (deser == null) {
            deser = _arrayDelegateDeserializer;
        }
        return deser;
    }

    /*
    /**********************************************************
    /* Overridable helper methods
    /**********************************************************
     */

    protected void injectValues(DeserializationContext ctxt, Object bean)
        throws IOException
    {
        for (ValueInjector injector : _injectables) {
            injector.inject(ctxt, bean);
        }
    }

    /**
     * Method called to handle set of one or more unknown properties,
     * stored in their entirety in given {@link TokenBuffer}
     * (as field entries, name and value).
     */
    protected Object handleUnknownProperties(DeserializationContext ctxt,
            Object bean, TokenBuffer unknownTokens)
        throws IOException
    {
        // First: add closing END_OBJECT as marker
        unknownTokens.writeEndObject();

        // note: buffer does NOT have starting START_OBJECT
        JsonParser bufferParser = unknownTokens.asParser();
        while (bufferParser.nextToken() != JsonToken.END_OBJECT) {
            String propName = bufferParser.currentName();
            // Unknown: let's call handler method
            bufferParser.nextToken();
            handleUnknownProperty(bufferParser, ctxt, bean, propName);
        }
        return bean;
    }

    /**
     * Helper method called for an unknown property, when using "vanilla"
     * processing.
     *
     * @param beanOrBuilder Either POJO instance (if constructed), or builder
     *   (in case of builder-based approach), that has property we haven't been
     *   able to handle yet.
     */
    protected void handleUnknownVanilla(JsonParser p, DeserializationContext ctxt,
            Object beanOrBuilder, String propName)
        throws IOException
    {
        if (IgnorePropertiesUtil.shouldIgnore(propName, _ignorableProps, _includableProps)) {
            handleIgnoredProperty(p, ctxt, beanOrBuilder, propName);
        } else if (_anySetter != null) {
            try {
               // should we consider return type of any setter?
                _anySetter.deserializeAndSet(p, ctxt, beanOrBuilder, propName);
            } catch (Exception e) {
                wrapAndThrow(e, beanOrBuilder, propName, ctxt);
            }
        } else {
            // Unknown: let's call handler method
            handleUnknownProperty(p, ctxt, beanOrBuilder, propName);
        }
    }

    /**
     * Method called when a JSON property is encountered that has not matching
     * setter, any-setter or field, and thus cannot be assigned.
     */
    @Override
    protected void handleUnknownProperty(JsonParser p, DeserializationContext ctxt,
            Object beanOrClass, String propName)
        throws IOException
    {
        if (_ignoreAllUnknown) {
            p.skipChildren();
            return;
        }
        if (IgnorePropertiesUtil.shouldIgnore(propName, _ignorableProps, _includableProps)) {
            handleIgnoredProperty(p, ctxt, beanOrClass, propName);
        }
        // Otherwise use default handling (call handler(s); if not
        // handled, throw exception or skip depending on settings)
        super.handleUnknownProperty(p, ctxt, beanOrClass, propName);
    }

    /**
     * Method called when an explicitly ignored property (one specified with a
     * name to match, either by property annotation or class annotation) is encountered.
     *
     * @since 2.3
     */
    protected void handleIgnoredProperty(JsonParser p, DeserializationContext ctxt,
            Object beanOrClass, String propName)
        throws IOException
    {
        if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)) {
            throw IgnoredPropertyException.from(p, beanOrClass, propName, getKnownPropertyNames());
        }
        p.skipChildren();
    }

    /**
     * Method called in cases where we may have polymorphic deserialization
     * case: that is, type of Creator-constructed bean is not the type
     * of deserializer itself. It should be a sub-class or implementation
     * class; either way, we may have more specific deserializer to use
     * for handling it.
     *
     * @param p (optional) If not null, parser that has more properties to handle
     *   (in addition to buffered properties); if null, all properties are passed
     *   in buffer
     */
    protected Object handlePolymorphic(JsonParser p, DeserializationContext ctxt,
            Object bean, TokenBuffer unknownTokens)
        throws IOException
    {
        // First things first: maybe there is a more specific deserializer available?
        JsonDeserializer<Object> subDeser = _findSubclassDeserializer(ctxt, bean, unknownTokens);
        if (subDeser != null) {
            if (unknownTokens != null) {
                // need to add END_OBJECT marker first
                unknownTokens.writeEndObject();
                JsonParser p2 = unknownTokens.asParser(p.streamReadConstraints());
                p2.nextToken(); // to get to first data field
                bean = subDeser.deserialize(p2, ctxt, bean);
            }
            // Original parser may also have some leftovers
            if (p != null) {
                bean = subDeser.deserialize(p, ctxt, bean);
            }
            return bean;
        }
        // nope; need to use this deserializer. Unknowns we've seen so far?
        if (unknownTokens != null) {
            bean = handleUnknownProperties(ctxt, bean, unknownTokens);
        }
        // and/or things left to process via main parser?
        if (p != null) {
            bean = deserialize(p, ctxt, bean);
        }
        return bean;
    }

    /**
     * Helper method called to (try to) locate deserializer for given sub-type of
     * type that this deserializer handles.
     */
    protected JsonDeserializer<Object> _findSubclassDeserializer(DeserializationContext ctxt,
            Object bean, TokenBuffer unknownTokens)
        throws IOException
    {
        JsonDeserializer<Object> subDeser;

        // First: maybe we have already created sub-type deserializer?
        synchronized (this) {
            subDeser = (_subDeserializers == null) ? null : _subDeserializers.get(new ClassKey(bean.getClass()));
        }
        if (subDeser != null) {
            return subDeser;
        }
        // If not, maybe we can locate one. First, need provider
        JavaType type = ctxt.constructType(bean.getClass());
        /* 30-Jan-2012, tatu: Ideally we would be passing referring
         *   property; which in theory we could keep track of via
         *   ResolvableDeserializer (if we absolutely must...).
         *   But for now, let's not bother.
         */
//        subDeser = ctxt.findValueDeserializer(type, _property);
        subDeser = ctxt.findRootValueDeserializer(type);
        // Also, need to cache it
        if (subDeser != null) {
            synchronized (this) {
                if (_subDeserializers == null) {
                    _subDeserializers = new HashMap<ClassKey,JsonDeserializer<Object>>();;
                }
                _subDeserializers.put(new ClassKey(bean.getClass()), subDeser);
            }
        }
        return subDeser;
    }

    /*
    /**********************************************************
    /* Helper methods for error reporting
    /**********************************************************
     */

    /**
     * Method that will modify caught exception (passed in as argument)
     * as necessary to include reference information, and to ensure it
     * is a subtype of {@link IOException}, or an unchecked exception.
     *<p>
     * Rules for wrapping and unwrapping are bit complicated; essentially:
     *<ul>
     * <li>Errors are to be passed as is (if uncovered via unwrapping)
     * <li>"Plain" IOExceptions (ones that are not of type
     *   {@link JsonMappingException} are to be passed as is
     *</ul>
     */
    public void wrapAndThrow(Throwable t, Object bean, String fieldName, DeserializationContext ctxt)
        throws IOException
    {
        // Need to add reference information
        throw JsonMappingException.wrapWithPath(throwOrReturnThrowable(t, ctxt), bean, fieldName);
    }

    private Throwable throwOrReturnThrowable(Throwable t, DeserializationContext ctxt)
        throws IOException
    {
        /* 05-Mar-2009, tatu: But one nasty edge is when we get
         *   StackOverflow: usually due to infinite loop. But that
         *   often gets hidden within an InvocationTargetException...
         */
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors to be passed as is
        ClassUtil.throwIfError(t);
        boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
        // Ditto for IOExceptions; except we may want to wrap JSON exceptions
        if (t instanceof IOException) {
            if (!wrap || !(t instanceof JacksonException)) {
                throw (IOException) t;
            }
        } else if (!wrap) { // [JACKSON-407] -- allow disabling wrapping for unchecked exceptions
            ClassUtil.throwIfRTE(t);
        }
        return t;
    }

    protected Object wrapInstantiationProblem(Throwable t, DeserializationContext ctxt)
        throws IOException
    {
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as is
        ClassUtil.throwIfError(t);
        if (t instanceof IOException) {
            // Since we have no more information to add, let's not actually wrap..
            throw (IOException) t;
        }
        if (ctxt == null) { // only to please LGTM...
            throw new IllegalArgumentException(t.getMessage(), t);
        }
        if (!ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS)) {
            ClassUtil.throwIfRTE(t);
        }
        return ctxt.handleInstantiationProblem(_beanType.getRawClass(), null, t);
    }
}
