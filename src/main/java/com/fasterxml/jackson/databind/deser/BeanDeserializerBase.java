package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.*;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.IgnoredPropertyException;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ClassKey;
import com.fasterxml.jackson.databind.util.*;

/**
 * Base class for <code>BeanDeserializer</code>.
 */
public abstract class BeanDeserializerBase
    extends StdDeserializer<Object>
    implements ContextualDeserializer, ResolvableDeserializer,
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
     * Annotations from the bean class: used for accessing
     * annotations during resolution
     * (see {@link #resolve}) and
     * contextualization (see {@link #createContextual})
     *<p> 
     * Transient since annotations only used during construction.
     */
    final private transient Annotations _classAnnotations;

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
     */
    protected JsonDeserializer<Object> _delegateDeserializer;
    
    /**
     * If the bean needs to be instantiated using constructor
     * or factory method
     * that takes one or more named properties as argument(s),
     * this creator is used for instantiation.
     * This value gets resolved during general resolution.
     */
    protected PropertyBasedCreator _propertyBasedCreator;

    /**
     * Flag that is set to mark "non-standard" cases; where either
     * we use one of non-default creators, or there are unwrapped
     * values to consider.
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
    final protected HashSet<String> _ignorableProps;

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
            HashSet<String> ignorableProps, boolean ignoreAllUnknown,
            boolean hasViews)
    {
        super(beanDesc.getType());

        AnnotatedClass ac = beanDesc.getClassInfo();
        _classAnnotations = ac.getAnnotations();       
        _beanType = beanDesc.getType();
        _valueInstantiator = builder.getValueInstantiator();
        
        _beanProperties = properties;
        _backRefs = backRefs;
        _ignorableProps = ignorableProps;
        _ignoreAllUnknown = ignoreAllUnknown;

        _anySetter = builder.getAnySetter();
        List<ValueInjector> injectables = builder.getInjectables();
        _injectables = (injectables == null || injectables.isEmpty()) ? null
                : injectables.toArray(new ValueInjector[injectables.size()]);
        _objectIdReader = builder.getObjectIdReader();
        _nonStandardCreation = (_unwrappedPropertyHandler != null)
            || _valueInstantiator.canCreateUsingDelegate()
            || _valueInstantiator.canCreateFromObjectWith()
            || !_valueInstantiator.canCreateUsingDefault()
            ;

        // Any transformation we may need to apply?
        JsonFormat.Value format = beanDesc.findExpectedFormat(null);
        _serializationShape = (format == null) ? null : format.getShape();

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
        
        _classAnnotations = src._classAnnotations;
        _beanType = src._beanType;
        
        _valueInstantiator = src._valueInstantiator;
        _delegateDeserializer = src._delegateDeserializer;
        _propertyBasedCreator = src._propertyBasedCreator;
        
        _beanProperties = src._beanProperties;
        _backRefs = src._backRefs;
        _ignorableProps = src._ignorableProps;
        _ignoreAllUnknown = ignoreAllUnknown;
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

        _classAnnotations = src._classAnnotations;
        _beanType = src._beanType;
        
        _valueInstantiator = src._valueInstantiator;
        _delegateDeserializer = src._delegateDeserializer;
        _propertyBasedCreator = src._propertyBasedCreator;

        _backRefs = src._backRefs;
        _ignorableProps = src._ignorableProps;
        _ignoreAllUnknown = (unwrapper != null) || src._ignoreAllUnknown;
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
        
        _classAnnotations = src._classAnnotations;
        _beanType = src._beanType;
        
        _valueInstantiator = src._valueInstantiator;
        _delegateDeserializer = src._delegateDeserializer;
        _propertyBasedCreator = src._propertyBasedCreator;
        
        _backRefs = src._backRefs;
        _ignorableProps = src._ignorableProps;
        _ignoreAllUnknown = src._ignoreAllUnknown;
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

    public BeanDeserializerBase(BeanDeserializerBase src, HashSet<String> ignorableProps)
    {
        super(src._beanType);
        
        _classAnnotations = src._classAnnotations;
        _beanType = src._beanType;
        
        _valueInstantiator = src._valueInstantiator;
        _delegateDeserializer = src._delegateDeserializer;
        _propertyBasedCreator = src._propertyBasedCreator;
        
        _backRefs = src._backRefs;
        _ignorableProps = ignorableProps;
        _ignoreAllUnknown = src._ignoreAllUnknown;
        _anySetter = src._anySetter;
        _injectables = src._injectables;
        
        _nonStandardCreation = src._nonStandardCreation;
        _unwrappedPropertyHandler = src._unwrappedPropertyHandler;
        _needViewProcesing = src._needViewProcesing;
        _serializationShape = src._serializationShape;

        _vanillaProcessing = src._vanillaProcessing;
        _objectIdReader = src._objectIdReader;
        _beanProperties = src._beanProperties;
    }
    
    @Override
    public abstract JsonDeserializer<Object> unwrappingDeserializer(NameTransformer unwrapper);

    public abstract BeanDeserializerBase withObjectIdReader(ObjectIdReader oir);

    public abstract BeanDeserializerBase withIgnorableProperties(HashSet<String> ignorableProps);

    /**
     * Fluent factory for creating a variant that can handle
     * POJO output as a JSON Array. Implementations may ignore this request
     * if no such input is possible.
     * 
     * @since 2.1
     */
    protected abstract BeanDeserializerBase asArrayDeserializer();

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
    public void resolve(DeserializationContext ctxt)
        throws JsonMappingException
    {
        ExternalTypeHandler.Builder extTypes = null;
        // if ValueInstantiator can use "creator" approach, need to resolve it here...
        if (_valueInstantiator.canCreateFromObjectWith()) {
            SettableBeanProperty[] creatorProps = _valueInstantiator.getFromObjectArguments(ctxt.getConfig());
            _propertyBasedCreator = PropertyBasedCreator.construct(ctxt, _valueInstantiator, creatorProps);
            // also: need to try to resolve 'external' type ids...
            for (SettableBeanProperty prop : _propertyBasedCreator.properties()) {
                if (prop.hasValueTypeDeserializer()) {
                    TypeDeserializer typeDeser = prop.getValueTypeDeserializer();
                    if (typeDeser.getTypeInclusion() == JsonTypeInfo.As.EXTERNAL_PROPERTY) {
                        if (extTypes == null) {
                            extTypes = new ExternalTypeHandler.Builder();
                        }
                        extTypes.addExternal(prop, typeDeser);
                    }
                }
            }
        }

        UnwrappedPropertyHandler unwrapped = null;

        for (SettableBeanProperty origProp : _beanProperties) {
            SettableBeanProperty prop = origProp;
            // May already have deserializer from annotations, if so, skip:
            if (!prop.hasValueDeserializer()) {
                // [Issue#125]: allow use of converters
                JsonDeserializer<?> deser = findConvertingDeserializer(ctxt, prop);
                if (deser == null) {
                    deser = findDeserializer(ctxt, prop.getType(), prop);
                }
                prop = prop.withValueDeserializer(deser);
            } else { // may need contextual version
                JsonDeserializer<Object> deser = prop.getValueDeserializer();
                /* Important! This is the only place where actually handle "primary"
                 * property deserializers -- call is different from other places.
                 */
                JsonDeserializer<?> cd = ctxt.handlePrimaryContextualization(deser, prop,
                        prop.getType());
                if (cd != deser) {
                    prop = prop.withValueDeserializer(cd);
                }
            }
            // [JACKSON-235]: need to link managed references with matching back references
            prop = _resolveManagedReferenceProperty(ctxt, prop);

            // issue #351: need to wrap properties that require object id resolution.
            if (!(prop instanceof ManagedReferenceProperty)) {
                prop = _resolvedObjectIdProperty(ctxt, prop);
            }
            // [JACKSON-132]: support unwrapped values (via @JsonUnwrapped)
            SettableBeanProperty u = _resolveUnwrappedProperty(ctxt, prop);
            if (u != null) {
                prop = u;
                if (unwrapped == null) {
                    unwrapped = new UnwrappedPropertyHandler();
                }
                unwrapped.addProperty(prop);
                /* 12-Dec-2014, tatu: As per [databind#647], we will have problems if
                 *    the original property is left in place. So let's remove it now.
                 */
                _beanProperties.remove(prop);
                continue;
            }
            // [JACKSON-594]: non-static inner classes too:
            prop = _resolveInnerClassValuedProperty(ctxt, prop);
            if (prop != origProp) {
                _beanProperties.replace(prop);
            }
            
            /* one more thing: if this property uses "external property" type inclusion
             * (see [JACKSON-453]), it needs different handling altogether
             */
            if (prop.hasValueTypeDeserializer()) {
                TypeDeserializer typeDeser = prop.getValueTypeDeserializer();
                if (typeDeser.getTypeInclusion() == JsonTypeInfo.As.EXTERNAL_PROPERTY) {
                    if (extTypes == null) {
                        extTypes = new ExternalTypeHandler.Builder();
                    }
                    extTypes.addExternal(prop, typeDeser);
                    // In fact, remove from list of known properties to simplify later handling
                    _beanProperties.remove(prop);
                    continue;
                }
            }
        }

        // "any setter" may also need to be resolved now
        if (_anySetter != null && !_anySetter.hasValueDeserializer()) {
            _anySetter = _anySetter.withValueDeserializer(findDeserializer(ctxt,
                    _anySetter.getType(), _anySetter.getProperty()));
        }

        // as well as delegate-based constructor:
        if (_valueInstantiator.canCreateUsingDelegate()) {
            JavaType delegateType = _valueInstantiator.getDelegateType(ctxt.getConfig());
            if (delegateType == null) {
                throw new IllegalArgumentException("Invalid delegate-creator definition for "+_beanType
                        +": value instantiator ("+_valueInstantiator.getClass().getName()
                        +") returned true for 'canCreateUsingDelegate()', but null for 'getDelegateType()'");
            }
            AnnotatedWithParams delegateCreator = _valueInstantiator.getDelegateCreator();
            // Need to create a temporary property to allow contextual deserializers:
            BeanProperty.Std property = new BeanProperty.Std(TEMP_PROPERTY_NAME,
                    delegateType, null, _classAnnotations, delegateCreator,
                    PropertyMetadata.STD_OPTIONAL);

            TypeDeserializer td = delegateType.getTypeHandler();
            if (td == null) {
                td = ctxt.getConfig().findTypeDeserializer(delegateType);
            }
            JsonDeserializer<Object> dd = findDeserializer(ctxt, delegateType, property);
            if (td != null) {
                td = td.forProperty(property);
                dd = new TypeWrappedDeserializer(td, dd);
            }
            _delegateDeserializer = dd;
        }
        
        if (extTypes != null) {
            _externalTypeIdHandler = extTypes.build();
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
     * Helper method that can be used to see if specified property is annotated
     * to indicate use of a converter for property value (in case of container types,
     * it is container type itself, not key or content type).
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
                JsonDeserializer<?> ser = ctxt.findContextualValueDeserializer(delegateType, prop);
                return new StdDelegatingDeserializer<Object>(conv, delegateType, ser);
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
        final AnnotatedMember accessor = (property == null || intr == null)
                ? null : property.getMember();
        if (accessor != null && intr != null) {
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
                        throw new IllegalArgumentException("Invalid Object Id definition for "
                                +handledType().getName()+": can not find property with name '"+propName+"'");
                    }
                    idType = idProp.getType();
                    idGen = new PropertyBasedObjectIdGenerator(objectIdInfo.getScope());
                } else { // other types need to be simpler
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
            String[] ignorals = intr.findPropertiesToIgnore(accessor, false);
            if (ignorals != null && ignorals.length != 0) {
                HashSet<String> newIgnored = ArrayBuilders.setAndArray(contextual._ignorableProps, ignorals);
                contextual = contextual.withIgnorableProperties(newIgnored);
            }
        }

        // One more thing: are we asked to serialize POJO as array?
        JsonFormat.Shape shape = null;
        if (accessor != null) {
            JsonFormat.Value format = intr.findFormat((Annotated) accessor);

            if (format != null) {
                shape = format.getShape();
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

    /**
     * Helper method called to see if given property is part of 'managed' property
     * pair (managed + back reference), and if so, handle resolution details.
     */
    protected SettableBeanProperty _resolveManagedReferenceProperty(DeserializationContext ctxt,
            SettableBeanProperty prop)
    {
        String refName = prop.getManagedReferenceName();
        if (refName == null) {
            return prop;
        }
        JsonDeserializer<?> valueDeser = prop.getValueDeserializer();
        SettableBeanProperty backProp = valueDeser.findBackReference(refName);
        if (backProp == null) {
            throw new IllegalArgumentException("Can not handle managed/back reference '"+refName+"': no back reference property found from type "
                    +prop.getType());
        }
        // also: verify that type is compatible
        JavaType referredType = _beanType;
        JavaType backRefType = backProp.getType();
        boolean isContainer = prop.getType().isContainerType();
        if (!backRefType.getRawClass().isAssignableFrom(referredType.getRawClass())) {
            throw new IllegalArgumentException("Can not handle managed/back reference '"+refName+"': back reference type ("
                    +backRefType.getRawClass().getName()+") not compatible with managed type ("
                    +referredType.getRawClass().getName()+")");
        }
        return new ManagedReferenceProperty(prop, refName, backProp,
                _classAnnotations, isContainer);
    }

    /**
     * Method that wraps given property with {@link ObjectIdReferenceProperty}
     * in case where object id resolution is required.
     */
    protected SettableBeanProperty _resolvedObjectIdProperty(DeserializationContext ctxt, SettableBeanProperty prop)
    {
        ObjectIdInfo objectIdInfo = prop.getObjectIdInfo();
        JsonDeserializer<Object> valueDeser = prop.getValueDeserializer();
        ObjectIdReader objectIdReader = valueDeser.getObjectIdReader();
        if (objectIdInfo == null && objectIdReader == null) {
            return prop;
        }

        return new ObjectIdReferenceProperty(prop, objectIdInfo);
    }

    /**
     * Helper method called to see if given property might be so-called unwrapped
     * property: these require special handling.
     */
    protected SettableBeanProperty _resolveUnwrappedProperty(DeserializationContext ctxt,
            SettableBeanProperty prop)
    {
        AnnotatedMember am = prop.getMember();
        if (am != null) {
            NameTransformer unwrapper = ctxt.getAnnotationIntrospector().findUnwrappingNameTransformer(am);
            if (unwrapper != null) {
                JsonDeserializer<Object> orig = prop.getValueDeserializer();
                JsonDeserializer<Object> unwrapping = orig.unwrappingDeserializer(unwrapper);
                if (unwrapping != orig && unwrapping != null) {
                    // might be cleaner to create new instance; but difficult to do reliably, so:
                    return prop.withValueDeserializer(unwrapping);
                }
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
                Class<?> enclosing = ClassUtil.getOuterClass(valueClass);
                // and is inner class of the bean class...
                if (enclosing != null && enclosing == _beanType.getRawClass()) {
                    for (Constructor<?> ctor : valueClass.getConstructors()) {
                        Class<?>[] paramTypes = ctor.getParameterTypes();
                        if (paramTypes.length == 1 && paramTypes[0] == enclosing) {
                            if (ctxt.getConfig().canOverrideAccessModifiers()) {
                                ClassUtil.checkAndFixAccess(ctor);
                            }
                            return new InnerClassProperty(prop, ctor);
                        }
                    }
                }
            }
        }
        return prop;
    }

    /*
    /**********************************************************
    /* Public accessors
    /**********************************************************
     */

    @Override
    public boolean isCachable() { return true; }

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
        _beanProperties.replace(replacement);
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
            JsonToken t = p.getCurrentToken();
            if (t != null) {
                // Most commonly, a scalar (int id, uuid String, ...)
                if (t.isScalarValue()) {
                    return deserializeFromObjectId(p, ctxt);
                }
                // but, with 2.5+, a simple Object-wrapped value also legal:
                if (t == JsonToken.START_OBJECT) {
                    t = p.nextToken();
                }
                if (t == JsonToken.FIELD_NAME && _objectIdReader.maySerializeAsObject()
                        && _objectIdReader.isValidReferencePropertyName(p.getCurrentName(), p)) {
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
    protected Object _handleTypedObjectId(JsonParser jp, DeserializationContext ctxt,
            Object pojo, Object rawId)
        throws IOException
    {
        /* 07-Aug-2013, tatu: One more challenge: type of id may not be type
         *   of property we are expecting later on; specifically, numeric ids
         *   vs Strings.
         */
        JsonDeserializer<Object> idDeser = _objectIdReader.getDeserializer();
        final Object id;

        // Ok, this is bit ridiculous; let's see if conversion is needed:
        if (idDeser.handledType() == rawId.getClass()) {
            // nope: already same type
            id = rawId;
        } else {
            id = _convertObjectId(jp, ctxt, rawId, idDeser);
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
    protected Object _convertObjectId(JsonParser jp, DeserializationContext ctxt,
            Object rawId, JsonDeserializer<Object> idDeser) throws IOException
    {
        TokenBuffer buf = new TokenBuffer(jp);
        if (rawId instanceof String) {
            buf.writeString((String) rawId);
        } else if (rawId instanceof Long) {
            buf.writeNumber(((Long) rawId).longValue());
        } else if (rawId instanceof Integer) {
            buf.writeNumber(((Integer) rawId).intValue());
        } else {
            // should we worry about UUIDs? They should be fine, right?
            // 07-Aug-2014, tatu: Maybe, but not necessarily; had issues with
            //   Smile format; [Smile#19], possibly related.
            buf.writeObject(rawId);
        }
        JsonParser bufParser = buf.asParser();
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
    protected Object deserializeWithObjectId(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return deserializeFromObject(jp, ctxt);
    }

    /**
     * Method called in cases where it looks like we got an Object Id
     * to parse and use as a reference.
     */
    protected Object deserializeFromObjectId(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        Object id = _objectIdReader.readObjectReference(jp, ctxt);
        ReadableObjectId roid = ctxt.findObjectId(id, _objectIdReader.generator, _objectIdReader.resolver);
        // do we have it resolved?
        Object pojo = roid.resolve();
        if (pojo == null) { // not yet; should wait...
            throw new UnresolvedForwardReference("Could not resolve Object Id ["+id+"] (for "
                    +_beanType+").", jp.getCurrentLocation(), roid);
        }
        return pojo;
    }

    protected Object deserializeFromObjectUsingNonDefault(JsonParser jp,
            DeserializationContext ctxt) throws IOException
    {
        if (_delegateDeserializer != null) {
            return _valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(jp, ctxt));
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
                +_beanType+": can not instantiate from JSON object (missing default constructor or creator, or perhaps need to add/enable type information?)");
    }

    protected abstract Object _deserializeUsingPropertyBased(final JsonParser jp,
            final DeserializationContext ctxt)
        throws IOException, JsonProcessingException;

    @SuppressWarnings("incomplete-switch")
    public Object deserializeFromNumber(JsonParser jp, DeserializationContext ctxt) throws IOException
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
        throw ctxt.instantiationException(handledType(), "no suitable creator method found to deserialize from JSON integer number");
    }

    public Object deserializeFromString(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // First things first: id Object Id is used, most likely that's it
        if (_objectIdReader != null) {
            return deserializeFromObjectId(p, ctxt);
        }
        
        /* Bit complicated if we have delegating creator; may need to use it,
         * or might not...
         */
        if (_delegateDeserializer != null) {
            if (!_valueInstantiator.canCreateFromString()) {
                Object bean = _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(p, ctxt));
                if (_injectables != null) {
                    injectValues(ctxt, bean);
                }
                return bean;
            }
        }
        return _valueInstantiator.createFromString(ctxt, p.getText());
    }

    /**
     * Method called to deserialize POJO value from a JSON floating-point
     * number.
     */
    @SuppressWarnings("incomplete-switch")
    public Object deserializeFromDouble(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        switch (p.getNumberType()) {
        case FLOAT: // no separate methods for taking float...
        case DOUBLE:
            if (_delegateDeserializer != null) {
                if (!_valueInstantiator.canCreateFromDouble()) {
                    Object bean = _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(p, ctxt));
                    if (_injectables != null) {
                        injectValues(ctxt, bean);
                    }
                    return bean;
                }
            }
            return _valueInstantiator.createFromDouble(ctxt, p.getDoubleValue());
        }
        // actually, could also be BigDecimal, so:
        if (_delegateDeserializer != null) {
            return _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(p, ctxt));
        }
        throw ctxt.instantiationException(handledType(), "no suitable creator method found to deserialize from JSON floating-point number");
    }

    /**
     * Method called to deserialize POJO value from a JSON boolean value (true, false)
     */
    public Object deserializeFromBoolean(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        if (_delegateDeserializer != null) {
            if (!_valueInstantiator.canCreateFromBoolean()) {
                Object bean = _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(p, ctxt));
                if (_injectables != null) {
                    injectValues(ctxt, bean);
                }
                return bean;
            }
        }
        boolean value = (p.getCurrentToken() == JsonToken.VALUE_TRUE);
        return _valueInstantiator.createFromBoolean(ctxt, value);
    }

    public Object deserializeFromArray(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        if (_delegateDeserializer != null) {
            try {
                Object bean = _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(p, ctxt));
                if (_injectables != null) {
                    injectValues(ctxt, bean);
                }
                return bean;
            } catch (Exception e) {
                wrapInstantiationProblem(e, ctxt);
            }
        }
        if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            JsonToken t = p.nextToken();
            if (t == JsonToken.END_ARRAY && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)) {
                return null;
            }
            final Object value = deserialize(p, ctxt);
            if (p.nextToken() != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(p, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single '" + _valueClass.getName() + "' value but there was more than a single value in the array");
            }
            return value;
        }
        if (ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)) {
            JsonToken t = p.nextToken();
            if (t == JsonToken.END_ARRAY) {
                return null;
            }
            throw ctxt.mappingException(handledType(), JsonToken.START_ARRAY);
        }
        throw ctxt.mappingException(handledType());
    }

    public Object deserializeFromEmbedded(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        // First things first: id Object Id is used, most likely that's it; specifically,
        // true for UUIDs when written as binary (with Smile, other binary formats)
        if (_objectIdReader != null) {
            return deserializeFromObjectId(jp, ctxt);
        }

        // TODO: maybe add support for ValueInstantiator, embedded?
        
        return jp.getEmbeddedObject();
    }
    
    /*
    /**********************************************************
    /* Overridable helper methods
    /**********************************************************
     */

    protected void injectValues(DeserializationContext ctxt, Object bean)
        throws IOException, JsonProcessingException
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
    @SuppressWarnings("resource")
    protected Object handleUnknownProperties(DeserializationContext ctxt,
            Object bean, TokenBuffer unknownTokens)
        throws IOException, JsonProcessingException
    {
        // First: add closing END_OBJECT as marker
        unknownTokens.writeEndObject();
        
        // note: buffer does NOT have starting START_OBJECT
        JsonParser bufferParser = unknownTokens.asParser();
        while (bufferParser.nextToken() != JsonToken.END_OBJECT) {
            String propName = bufferParser.getCurrentName();
            // Unknown: let's call handler method
            bufferParser.nextToken();
            handleUnknownProperty(bufferParser, ctxt, bean, propName);
        }
        return bean;
    }

    /**
     * Helper method called for an unknown property, when using "vanilla"
     * processing.
     */
    protected void handleUnknownVanilla(JsonParser jp, DeserializationContext ctxt,
            Object bean, String propName)
        throws IOException, JsonProcessingException
    {
        if (_ignorableProps != null && _ignorableProps.contains(propName)) {
            handleIgnoredProperty(jp, ctxt, bean, propName);
        } else if (_anySetter != null) {
            try {
               // should we consider return type of any setter?
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
     * Method called when a JSON property is encountered that has not matching
     * setter, any-setter or field, and thus can not be assigned.
     */
    @Override
    protected void handleUnknownProperty(JsonParser jp, DeserializationContext ctxt,
            Object beanOrClass, String propName)
        throws IOException, JsonProcessingException
    {
        if (_ignoreAllUnknown) {
            jp.skipChildren();
            return;
        }
        if (_ignorableProps != null && _ignorableProps.contains(propName)) {
            handleIgnoredProperty(jp, ctxt, beanOrClass, propName);
        }
        // Otherwise use default handling (call handler(s); if not
        // handled, throw exception or skip depending on settings)
        super.handleUnknownProperty(jp, ctxt, beanOrClass, propName);
    }

    /**
     * Method called when an explicitly ignored property (one specified with a
     * name to match, either by property annotation or class annotation) is encountered.
     * 
     * @since 2.3
     */
    protected void handleIgnoredProperty(JsonParser jp, DeserializationContext ctxt,
            Object beanOrClass, String propName)
        throws IOException, JsonProcessingException
    {
        if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)) {
            throw IgnoredPropertyException.from(jp, beanOrClass, propName, getKnownPropertyNames());
        }
        jp.skipChildren();
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
    @SuppressWarnings("resource")
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
    
    /**
     * Helper method called to (try to) locate deserializer for given sub-type of
     * type that this deserializer handles.
     */
    protected JsonDeserializer<Object> _findSubclassDeserializer(DeserializationContext ctxt,
            Object bean, TokenBuffer unknownTokens)
        throws IOException, JsonProcessingException
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
        // [JACKSON-55] Need to add reference information
        throw JsonMappingException.wrapWithPath(throwOrReturnThrowable(t, ctxt), bean, fieldName);
    }

    @Deprecated // since 2.4, not used by core Jackson; only relevant for arrays/Collections
    public void wrapAndThrow(Throwable t, Object bean, int index, DeserializationContext ctxt) throws IOException {
        // [JACKSON-55] Need to add reference information
        throw JsonMappingException.wrapWithPath(throwOrReturnThrowable(t, ctxt), bean, index);
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
        if (t instanceof Error) {
            throw (Error) t;
        }
        boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
        // Ditto for IOExceptions; except we may want to wrap JSON exceptions
        if (t instanceof IOException) {
            if (!wrap || !(t instanceof JsonProcessingException)) {
                throw (IOException) t;
            }
        } else if (!wrap) { // [JACKSON-407] -- allow disabling wrapping for unchecked exceptions
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
        return t;
    }

    protected void wrapInstantiationProblem(Throwable t, DeserializationContext ctxt)
        throws IOException
    {
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as is
        if (t instanceof Error) {
            throw (Error) t;
        }
        boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
        if (t instanceof IOException) {
            // Since we have no more information to add, let's not actually wrap..
            throw (IOException) t;
        } else if (!wrap) { // [JACKSON-407] -- allow disabling wrapping for unchecked exceptions
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
        throw ctxt.instantiationException(_beanType.getRawClass(), t);
    }
}
