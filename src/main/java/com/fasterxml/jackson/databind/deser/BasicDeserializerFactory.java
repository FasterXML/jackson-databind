package com.fasterxml.jackson.databind.deser;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.ConfigOverride;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.deser.impl.CreatorCandidate;
import com.fasterxml.jackson.databind.deser.impl.CreatorCollector;
import com.fasterxml.jackson.databind.deser.impl.JDKValueInstantiators;
import com.fasterxml.jackson.databind.deser.impl.JavaUtilCollectionsDeserializers;
import com.fasterxml.jackson.databind.deser.std.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.ext.OptionalHandlerFactory;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.*;
import com.fasterxml.jackson.databind.util.*;

/**
 * Abstract factory base class that can provide deserializers for standard
 * JDK classes, including collection classes and simple heuristics for
 * "upcasting" common collection interface types
 * (such as {@link java.util.Collection}).
 *<p>
 * Since all simple deserializers are eagerly instantiated, and there is
 * no additional introspection or customizability of these types,
 * this factory is stateless.
 */
@SuppressWarnings("serial")
public abstract class BasicDeserializerFactory
    extends DeserializerFactory
    implements java.io.Serializable
{
    private final static Class<?> CLASS_OBJECT = Object.class;
    private final static Class<?> CLASS_STRING = String.class;
    private final static Class<?> CLASS_CHAR_SEQUENCE = CharSequence.class;
    private final static Class<?> CLASS_ITERABLE = Iterable.class;
    private final static Class<?> CLASS_MAP_ENTRY = Map.Entry.class;
    private final static Class<?> CLASS_SERIALIZABLE = Serializable.class;

    /**
     * We need a placeholder for creator properties that don't have name
     * but are marked with `@JsonWrapped` annotation.
     */
    protected final static PropertyName UNWRAPPED_CREATOR_PARAM_NAME = new PropertyName("@JsonUnwrapped");

    /*
    /**********************************************************
    /* Config
    /**********************************************************
     */

    /**
     * Configuration settings for this factory; immutable instance (just like this
     * factory), new version created via copy-constructor (fluent-style)
     */
    protected final DeserializerFactoryConfig _factoryConfig;

    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */

    protected BasicDeserializerFactory(DeserializerFactoryConfig config) {
        _factoryConfig = config;
    }

    /**
     * Method for getting current {@link DeserializerFactoryConfig}.
      *<p>
     * Note that since instances are immutable, you can NOT change settings
     * by accessing an instance and calling methods: this will simply create
     * new instance of config object.
     */
    public DeserializerFactoryConfig getFactoryConfig() {
        return _factoryConfig;
    }

    protected abstract DeserializerFactory withConfig(DeserializerFactoryConfig config);

    /*
    /********************************************************
    /* Configuration handling: fluent factories
    /********************************************************
     */

    /**
     * Convenience method for creating a new factory instance with additional deserializer
     * provider.
     */
    @Override
    public final DeserializerFactory withAdditionalDeserializers(Deserializers additional) {
        return withConfig(_factoryConfig.withAdditionalDeserializers(additional));
    }

    /**
     * Convenience method for creating a new factory instance with additional
     * {@link KeyDeserializers}.
     */
    @Override
    public final DeserializerFactory withAdditionalKeyDeserializers(KeyDeserializers additional) {
        return withConfig(_factoryConfig.withAdditionalKeyDeserializers(additional));
    }

    /**
     * Convenience method for creating a new factory instance with additional
     * {@link BeanDeserializerModifier}.
     */
    @Override
    public final DeserializerFactory withDeserializerModifier(BeanDeserializerModifier modifier) {
        return withConfig(_factoryConfig.withDeserializerModifier(modifier));
    }

    /**
     * Convenience method for creating a new factory instance with additional
     * {@link AbstractTypeResolver}.
     */
    @Override
    public final DeserializerFactory withAbstractTypeResolver(AbstractTypeResolver resolver) {
        return withConfig(_factoryConfig.withAbstractTypeResolver(resolver));
    }

    /**
     * Convenience method for creating a new factory instance with additional
     * {@link ValueInstantiators}.
     */
    @Override
    public final DeserializerFactory withValueInstantiators(ValueInstantiators instantiators) {
        return withConfig(_factoryConfig.withValueInstantiators(instantiators));
    }

    /*
    /**********************************************************
    /* DeserializerFactory impl (partial): type mappings
    /**********************************************************
     */

    @Override
    public JavaType mapAbstractType(DeserializationConfig config, JavaType type) throws JsonMappingException
    {
        // first, general mappings
        while (true) {
            JavaType next = _mapAbstractType2(config, type);
            if (next == null) {
                return type;
            }
            // Should not have to worry about cycles; but better verify since they will invariably occur... :-)
            // (also: guard against invalid resolution to a non-related type)
            Class<?> prevCls = type.getRawClass();
            Class<?> nextCls = next.getRawClass();
            if ((prevCls == nextCls) || !prevCls.isAssignableFrom(nextCls)) {
                throw new IllegalArgumentException("Invalid abstract type resolution from "+type+" to "+next+": latter is not a subtype of former");
            }
            type = next;
        }
    }

    /**
     * Method that will find abstract type mapping for specified type, doing a single
     * lookup through registered abstract type resolvers; will not do recursive lookups.
     */
    private JavaType _mapAbstractType2(DeserializationConfig config, JavaType type)
        throws JsonMappingException
    {
        Class<?> currClass = type.getRawClass();
        if (_factoryConfig.hasAbstractTypeResolvers()) {
            for (AbstractTypeResolver resolver : _factoryConfig.abstractTypeResolvers()) {
                JavaType concrete = resolver.findTypeMapping(config, type);
                if ((concrete != null) && !concrete.hasRawClass(currClass)) {
                    return concrete;
                }
            }
        }
        return null;
    }

    /*
    /**********************************************************
    /* DeserializerFactory impl (partial): ValueInstantiators
    /**********************************************************
     */

    /**
     * Value instantiator is created both based on creator annotations,
     * and on optional externally provided instantiators (registered through
     * module interface).
     */
    @Override
    public ValueInstantiator findValueInstantiator(DeserializationContext ctxt,
            BeanDescription beanDesc)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();

        ValueInstantiator instantiator = null;
        // Check @JsonValueInstantiator before anything else
        AnnotatedClass ac = beanDesc.getClassInfo();
        Object instDef = ctxt.getAnnotationIntrospector().findValueInstantiator(ac);
        if (instDef != null) {
            instantiator = _valueInstantiatorInstance(config, ac, instDef);
        }
        if (instantiator == null) {
            // Second: see if some of standard Jackson/JDK types might provide value
            // instantiators.
            instantiator = JDKValueInstantiators.findStdValueInstantiator(config, beanDesc.getBeanClass());
            if (instantiator == null) {
                instantiator = _constructDefaultValueInstantiator(ctxt, beanDesc);
            }
        }

        // finally: anyone want to modify ValueInstantiator?
        if (_factoryConfig.hasValueInstantiators()) {
            for (ValueInstantiators insts : _factoryConfig.valueInstantiators()) {
                instantiator = insts.findValueInstantiator(config, beanDesc, instantiator);
                // let's do sanity check; easier to spot buggy handlers
                if (instantiator == null) {
                    ctxt.reportBadTypeDefinition(beanDesc,
						"Broken registered ValueInstantiators (of type %s): returned null ValueInstantiator",
						insts.getClass().getName());
                }
            }
        }
        if (instantiator != null) {
            instantiator = instantiator.createContextual(ctxt, beanDesc);
        }

        return instantiator;
    }

    /**
     * Method that will construct standard default {@link ValueInstantiator}
     * using annotations (like @JsonCreator) and visibility rules
     */
    protected ValueInstantiator _constructDefaultValueInstantiator(DeserializationContext ctxt,
            BeanDescription beanDesc)
        throws JsonMappingException
    {
        final CreatorCollectionState ccState;
        final ConstructorDetector ctorDetector;

        {
            final DeserializationConfig config = ctxt.getConfig();
            // need to construct suitable visibility checker:
            final VisibilityChecker<?> vchecker = config.getDefaultVisibilityChecker(beanDesc.getBeanClass(),
                    beanDesc.getClassInfo());
            ctorDetector = config.getConstructorDetector();

            // 24-Sep-2014, tatu: Tricky part first; need to merge resolved property information
            //  (which has creator parameters sprinkled around) with actual creator
            //  declarations (which are needed to access creator annotation, amongst other things).
            //  Easiest to combine that info first, then pass it to remaining processing.

            // 15-Mar-2015, tatu: Alas, this won't help with constructors that only have implicit
            //   names. Those will need to be resolved later on.
            final CreatorCollector creators = new CreatorCollector(beanDesc, config);
            Map<AnnotatedWithParams,BeanPropertyDefinition[]> creatorDefs = _findCreatorsFromProperties(ctxt,
                    beanDesc);
            ccState = new CreatorCollectionState(ctxt, beanDesc, vchecker,
                    creators, creatorDefs);
        }

        // Start with explicitly annotated factory methods
        _addExplicitFactoryCreators(ctxt, ccState, !ctorDetector.requireCtorAnnotation());

        // constructors only usable on concrete types:
        if (beanDesc.getType().isConcrete()) {
            // 25-Jan-2017, tatu: As per [databind#1501], [databind#1502], [databind#1503], best
            //     for now to skip attempts at using anything but no-args constructor (see
            //     `InnerClassProperty` construction for that)
            final boolean isNonStaticInnerClass = beanDesc.isNonStaticInnerClass();
            if (isNonStaticInnerClass) {
                // TODO: look for `@JsonCreator` annotated ones, throw explicit exception?
                ;
            } else {
                // 18-Sep-2020, tatu: Although by default implicit introspection is allowed, 2.12
                //   has settings to prevent that either generally, or at least for JDK types
                final boolean findImplicit = ctorDetector.shouldIntrospectorImplicitConstructors(beanDesc.getBeanClass());
                _addExplicitConstructorCreators(ctxt, ccState, findImplicit);
                if (ccState.hasImplicitConstructorCandidates()
                        // 05-Dec-2020, tatu: [databind#2962] explicit annotation of
                        //   a factory should not block implicit constructor, for backwards
                        //   compatibility (minor regression in 2.12.0)
                        //&& !ccState.hasExplicitFactories()
                        //  ... explicit constructor should prevent, however
                        && !ccState.hasExplicitConstructors()) {
                    _addImplicitConstructorCreators(ctxt, ccState, ccState.implicitConstructorCandidates());
                }
            }
        }
        // and finally, implicitly found factory methods if nothing explicit found
        if (ccState.hasImplicitFactoryCandidates()
                && !ccState.hasExplicitFactories() && !ccState.hasExplicitConstructors()) {
            _addImplicitFactoryCreators(ctxt, ccState, ccState.implicitFactoryCandidates());
        }
        return ccState.creators.constructValueInstantiator(ctxt);
    }

    protected Map<AnnotatedWithParams,BeanPropertyDefinition[]> _findCreatorsFromProperties(DeserializationContext ctxt,
            BeanDescription beanDesc) throws JsonMappingException
    {
        Map<AnnotatedWithParams,BeanPropertyDefinition[]> result = Collections.emptyMap();
        for (BeanPropertyDefinition propDef : beanDesc.findProperties()) {
            Iterator<AnnotatedParameter> it = propDef.getConstructorParameters();
            while (it.hasNext()) {
                AnnotatedParameter param = it.next();
                AnnotatedWithParams owner = param.getOwner();
                BeanPropertyDefinition[] defs = result.get(owner);
                final int index = param.getIndex();

                if (defs == null) {
                    if (result.isEmpty()) { // since emptyMap is immutable need to create a 'real' one
                        result = new LinkedHashMap<AnnotatedWithParams,BeanPropertyDefinition[]>();
                    }
                    defs = new BeanPropertyDefinition[owner.getParameterCount()];
                    result.put(owner, defs);
                } else {
                    if (defs[index] != null) {
                        ctxt.reportBadTypeDefinition(beanDesc,
"Conflict: parameter #%d of %s bound to more than one property; %s vs %s",
index, owner, defs[index], propDef);
                    }
                }
                defs[index] = propDef;
            }
        }
        return result;
    }

    public ValueInstantiator _valueInstantiatorInstance(DeserializationConfig config,
            Annotated annotated, Object instDef)
        throws JsonMappingException
    {
        if (instDef == null) {
            return null;
        }

        ValueInstantiator inst;

        if (instDef instanceof ValueInstantiator) {
            return (ValueInstantiator) instDef;
        }
        if (!(instDef instanceof Class)) {
            throw new IllegalStateException("AnnotationIntrospector returned key deserializer definition of type "
                    +instDef.getClass().getName()
                    +"; expected type KeyDeserializer or Class<KeyDeserializer> instead");
        }
        Class<?> instClass = (Class<?>)instDef;
        if (ClassUtil.isBogusClass(instClass)) {
            return null;
        }
        if (!ValueInstantiator.class.isAssignableFrom(instClass)) {
            throw new IllegalStateException("AnnotationIntrospector returned Class "+instClass.getName()
                    +"; expected Class<ValueInstantiator>");
        }
        HandlerInstantiator hi = config.getHandlerInstantiator();
        if (hi != null) {
            inst = hi.valueInstantiatorInstance(config, annotated, instClass);
            if (inst != null) {
                return inst;
            }
        }
        return (ValueInstantiator) ClassUtil.createInstance(instClass,
                config.canOverrideAccessModifiers());
    }

    /*
    /**********************************************************************
    /* Creator introspection: Record introspection (Jackson 2.12+, Java 14+)
    /**********************************************************************
     */

    /**
     * Helper method called when a {@code java.lang.Record} definition's "canonical"
     * constructor is to be used: if so, we have implicit names to consider.
     *
     * @since 2.12
     * @deprecated since 2.15 - no longer used, but kept because this protected method might have been overridden/used
     * elsewhere
     */
    @Deprecated
    protected void _addRecordConstructor(DeserializationContext ctxt, CreatorCollectionState ccState,
            AnnotatedConstructor canonical, List<String> implicitNames)
                    throws JsonMappingException
    {
        final int argCount = canonical.getParameterCount();
        final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        final SettableBeanProperty[] properties = new SettableBeanProperty[argCount];

        for (int i = 0; i < argCount; ++i) {
            final AnnotatedParameter param = canonical.getParameter(i);
            JacksonInject.Value injectable = intr.findInjectableValue(param);
            PropertyName name = intr.findNameForDeserialization(param);
            if (name == null || name.isEmpty()) {
                name = PropertyName.construct(implicitNames.get(i));
            }
            properties[i] = constructCreatorProperty(ctxt, ccState.beanDesc, name, i, param, injectable);
        }
        ccState.creators.addPropertyCreator(canonical, false, properties);
    }

    /*
    /**********************************************************************
    /* Creator introspection: constructor creator introspection
    /**********************************************************************
     */

    protected void _addExplicitConstructorCreators(DeserializationContext ctxt,
            CreatorCollectionState ccState, boolean findImplicit)
                    throws JsonMappingException
    {
        final BeanDescription beanDesc = ccState.beanDesc;
        final CreatorCollector creators = ccState.creators;
        final AnnotationIntrospector intr = ccState.annotationIntrospector();
        final VisibilityChecker<?> vchecker = ccState.vchecker;
        final Map<AnnotatedWithParams, BeanPropertyDefinition[]> creatorParams = ccState.creatorParams;

        // First things first: the "default constructor" (zero-arg
        // constructor; whether implicit or explicit) is NOT included
        // in list of constructors, so needs to be handled separately.
        AnnotatedConstructor defaultCtor = beanDesc.findDefaultConstructor();
        if (defaultCtor != null) {
            if (!creators.hasDefaultCreator() || _hasCreatorAnnotation(ctxt, defaultCtor)) {
                creators.setDefaultCreator(defaultCtor);
            }
        }
        // 21-Sep-2017, tatu: First let's handle explicitly annotated ones
        for (AnnotatedConstructor ctor : beanDesc.getConstructors()) {
            JsonCreator.Mode creatorMode = intr.findCreatorAnnotation(ctxt.getConfig(), ctor);
            if (JsonCreator.Mode.DISABLED == creatorMode) {
                continue;
            }
            if (creatorMode == null) {
                // let's check Visibility here, to avoid further processing for non-visible?
                if (findImplicit && vchecker.isCreatorVisible(ctor)) {
                    ccState.addImplicitConstructorCandidate(CreatorCandidate.construct(intr,
                            ctor, creatorParams.get(ctor)));
                }
                continue;
            }

            switch (creatorMode) {
            case DELEGATING:
                _addExplicitDelegatingCreator(ctxt, beanDesc, creators,
                        CreatorCandidate.construct(intr, ctor, null));
                break;
            case PROPERTIES:
                _addExplicitPropertyCreator(ctxt, beanDesc, creators,
                        CreatorCandidate.construct(intr, ctor, creatorParams.get(ctor)));
                break;
            default:
                _addExplicitAnyCreator(ctxt, beanDesc, creators,
                        CreatorCandidate.construct(intr, ctor, creatorParams.get(ctor)),
                        ctxt.getConfig().getConstructorDetector());
                break;
            }
            ccState.increaseExplicitConstructorCount();
        }
    }

    protected void _addImplicitConstructorCreators(DeserializationContext ctxt,
            CreatorCollectionState ccState, List<CreatorCandidate> ctorCandidates)
                    throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        final BeanDescription beanDesc = ccState.beanDesc;
        final CreatorCollector creators = ccState.creators;
        final AnnotationIntrospector intr = ccState.annotationIntrospector();
        final VisibilityChecker<?> vchecker = ccState.vchecker;
        List<AnnotatedWithParams> implicitCtors = null;
        final boolean preferPropsBased = config.getConstructorDetector().singleArgCreatorDefaultsToProperties();

        for (CreatorCandidate candidate : ctorCandidates) {
            final int argCount = candidate.paramCount();
            final AnnotatedWithParams ctor = candidate.creator();
            // some single-arg factory methods (String, number) are auto-detected
            if (argCount == 1) {
                final BeanPropertyDefinition propDef = candidate.propertyDef(0);
                final boolean useProps = preferPropsBased
                        || _checkIfCreatorPropertyBased(beanDesc, intr, ctor, propDef);

                if (useProps) {
                    SettableBeanProperty[] properties = new SettableBeanProperty[1];
                    final JacksonInject.Value injection = candidate.injection(0);

                    // 18-Sep-2020, tatu: [databind#1498] looks like implicit name not linked
                    //    unless annotation found, so try again from here
                    PropertyName name = candidate.paramName(0);
                    if (name == null) {
                        name = candidate.findImplicitParamName(0);
                        if ((name == null) && (injection == null)) {
                            continue;
                        }
                    }
                    properties[0] = constructCreatorProperty(ctxt, beanDesc, name, 0,
                            candidate.parameter(0), injection);
                    creators.addPropertyCreator(ctor, false, properties);
                } else {
                    /*boolean added = */ _handleSingleArgumentCreator(creators,
                            ctor, false,
                            vchecker.isCreatorVisible(ctor));
                    // one more thing: sever link to creator property, to avoid possible later
                    // problems with "unresolved" constructor property
                    if (propDef != null) {
                        ((POJOPropertyBuilder) propDef).removeConstructors();
                    }
                }
                // regardless, fully handled
                continue;
            }

            // 2 or more args; all params must have names or be injectable
            // 14-Mar-2015, tatu (2.6): Or, as per [#725], implicit names will also
            //   do, with some constraints. But that will require bit post processing...

            int nonAnnotatedParamIndex = -1;
            SettableBeanProperty[] properties = new SettableBeanProperty[argCount];
            int explicitNameCount = 0;
            int implicitWithCreatorCount = 0;
            int injectCount = 0;

            for (int i = 0; i < argCount; ++i) {
                final AnnotatedParameter param = ctor.getParameter(i);
                BeanPropertyDefinition propDef = candidate.propertyDef(i);
                JacksonInject.Value injectable = intr.findInjectableValue(param);
                final PropertyName name = (propDef == null) ? null : propDef.getFullName();

                if ((propDef != null)
                        // [databind#3724]: Record canonical constructor will have implicitly named propDef
                        && (propDef.isExplicitlyNamed() || beanDesc.isRecordType())) {
                    ++explicitNameCount;
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectable);
                    continue;
                }
                if (injectable != null) {
                    ++injectCount;
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectable);
                    continue;
                }
                NameTransformer unwrapper = intr.findUnwrappingNameTransformer(param);
                if (unwrapper != null) {
                    _reportUnwrappedCreatorProperty(ctxt, beanDesc, param);
                    /*
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, UNWRAPPED_CREATOR_PARAM_NAME, i, param, null);
                    ++explicitNameCount;
                    */
                    continue;
                }
                // One more thing: implicit names are ok iff ctor has creator annotation
                /*
                if (isCreator && (name != null && !name.isEmpty())) {
                    ++implicitWithCreatorCount;
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectId);
                    continue;
                }
                */
                if (nonAnnotatedParamIndex < 0) {
                    nonAnnotatedParamIndex = i;
                }
            }

            final int namedCount = explicitNameCount + implicitWithCreatorCount;
            // Ok: if named or injectable, we have more work to do
            if ((explicitNameCount > 0) || (injectCount > 0)) {
                // simple case; everything covered:
                if ((namedCount + injectCount) == argCount) {
                    creators.addPropertyCreator(ctor, false, properties);
                    continue;
                }
                if ((explicitNameCount == 0) && ((injectCount + 1) == argCount)) {
                    // Secondary: all but one injectable, one un-annotated (un-named)
                    creators.addDelegatingCreator(ctor, false, properties, 0);
                    continue;
                }
                // otherwise, epic fail?
                // 16-Mar-2015, tatu: due to [#725], need to be more permissive. For now let's
                //    only report problem if there's no implicit name
                PropertyName impl = candidate.findImplicitParamName(nonAnnotatedParamIndex);
                if (impl == null || impl.isEmpty()) {
                    // Let's consider non-static inner class as a special case...
                    // 25-Jan-2017, tatu: Non-static inner classes skipped altogether, now
                    /*
                    if ((nonAnnotatedParamIndex == 0) && isNonStaticInnerClass) {
                        throw new IllegalArgumentException("Non-static inner classes like "
                                +ctor.getDeclaringClass().getName()+" cannot use @JsonCreator for constructors");
                    }
                    */
                    ctxt.reportBadTypeDefinition(beanDesc,
"Argument #%d of constructor %s has no property name annotation; must have name when multiple-parameter constructor annotated as Creator",
nonAnnotatedParamIndex, ctor);
                }
            }
            // [#725]: as a fallback, all-implicit names may work as well
            if (!creators.hasDefaultCreator()) {
                if (implicitCtors == null) {
                    implicitCtors = new LinkedList<>();
                }
                implicitCtors.add(ctor);
            }
        }
        // last option, as per [#725]: consider implicit-names-only, visible constructor,
        // if just one found
        if ((implicitCtors != null) && !creators.hasDelegatingCreator()
                && !creators.hasPropertyBasedCreator()) {
            _checkImplicitlyNamedConstructors(ctxt, beanDesc, vchecker, intr,
                    creators, implicitCtors);
        }
    }

    /*
    /**********************************************************************
    /* Creator introspection: factory creator introspection
    /**********************************************************************
     */

    protected void _addExplicitFactoryCreators(DeserializationContext ctxt,
            CreatorCollectionState ccState, boolean findImplicit)
        throws JsonMappingException
    {
        final BeanDescription beanDesc = ccState.beanDesc;
        final CreatorCollector creators = ccState.creators;
        final AnnotationIntrospector intr = ccState.annotationIntrospector();
        final VisibilityChecker<?> vchecker = ccState.vchecker;
        final Map<AnnotatedWithParams, BeanPropertyDefinition[]> creatorParams = ccState.creatorParams;

        // 21-Sep-2017, tatu: First let's handle explicitly annotated ones
        for (AnnotatedMethod factory : beanDesc.getFactoryMethods()) {
            JsonCreator.Mode creatorMode = intr.findCreatorAnnotation(ctxt.getConfig(), factory);
            final int argCount = factory.getParameterCount();
            if (creatorMode == null) {
                // Only potentially accept 1-argument factory methods
                if (findImplicit && (argCount == 1) && vchecker.isCreatorVisible(factory)) {
                    ccState.addImplicitFactoryCandidate(CreatorCandidate.construct(intr, factory, null));
                }
                continue;
            }
            if (creatorMode == JsonCreator.Mode.DISABLED) {
                continue;
            }

            // zero-arg method factory methods fine, as long as explicit
            if (argCount == 0) {
                creators.setDefaultCreator(factory);
                continue;
            }

            switch (creatorMode) {
            case DELEGATING:
                _addExplicitDelegatingCreator(ctxt, beanDesc, creators,
                        CreatorCandidate.construct(intr, factory, null));
                break;
            case PROPERTIES:
                _addExplicitPropertyCreator(ctxt, beanDesc, creators,
                        CreatorCandidate.construct(intr, factory, creatorParams.get(factory)));
                break;
            case DEFAULT:
            default:
                _addExplicitAnyCreator(ctxt, beanDesc, creators,
                        CreatorCandidate.construct(intr, factory, creatorParams.get(factory)),
                        // 13-Sep-2020, tatu: Factory methods do not follow config settings
                        //    (as of Jackson 2.12)
                        ConstructorDetector.DEFAULT);
                break;
            }
            ccState.increaseExplicitFactoryCount();
        }
    }

    protected void _addImplicitFactoryCreators(DeserializationContext ctxt,
            CreatorCollectionState ccState, List<CreatorCandidate> factoryCandidates)
        throws JsonMappingException
    {
        final BeanDescription beanDesc = ccState.beanDesc;
        final CreatorCollector creators = ccState.creators;
        final AnnotationIntrospector intr = ccState.annotationIntrospector();
        final VisibilityChecker<?> vchecker = ccState.vchecker;
        final Map<AnnotatedWithParams, BeanPropertyDefinition[]> creatorParams = ccState.creatorParams;

        // And then implicitly found
        for (CreatorCandidate candidate : factoryCandidates) {
            final int argCount = candidate.paramCount();
            AnnotatedWithParams factory = candidate.creator();
            final BeanPropertyDefinition[] propDefs = creatorParams.get(factory);
            // some single-arg factory methods (String, number) are auto-detected
            if (argCount != 1) {
                continue; // 2 and more args? Must be explicit, handled earlier
            }
            BeanPropertyDefinition argDef = candidate.propertyDef(0);
            boolean useProps = _checkIfCreatorPropertyBased(beanDesc, intr, factory, argDef);
            if (!useProps) { // not property based but delegating
                /*boolean added=*/ _handleSingleArgumentCreator(creators,
                        factory, false, vchecker.isCreatorVisible(factory));
                // 23-Sep-2016, tatu: [databind#1383]: Need to also sever link to avoid possible
                //    later problems with "unresolved" constructor property
                if (argDef != null) {
                    ((POJOPropertyBuilder) argDef).removeConstructors();
                }
                continue;
            }
            AnnotatedParameter nonAnnotatedParam = null;
            SettableBeanProperty[] properties = new SettableBeanProperty[argCount];
            int implicitNameCount = 0;
            int explicitNameCount = 0;
            int injectCount = 0;

            for (int i = 0; i < argCount; ++i) {
                final AnnotatedParameter param = factory.getParameter(i);
                BeanPropertyDefinition propDef = (propDefs == null) ? null : propDefs[i];
                JacksonInject.Value injectable = intr.findInjectableValue(param);
                final PropertyName name = (propDef == null) ? null : propDef.getFullName();

                if (propDef != null && propDef.isExplicitlyNamed()) {
                    ++explicitNameCount;
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectable);
                    continue;
                }
                if (injectable != null) {
                    ++injectCount;
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectable);
                    continue;
                }
                NameTransformer unwrapper = intr.findUnwrappingNameTransformer(param);
                if (unwrapper != null) {
                    _reportUnwrappedCreatorProperty(ctxt, beanDesc, param);
                    /*
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, UNWRAPPED_CREATOR_PARAM_NAME, i, param, null);
                    ++implicitNameCount;
                    */
                    continue;
                }
                // One more thing: implicit names are ok iff ctor has creator annotation
                /*
                if (isCreator) {
                    if (name != null && !name.isEmpty()) {
                        ++implicitNameCount;
                        properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectable);
                        continue;
                    }
                }
                */
                /* 25-Sep-2014, tatu: Actually, we may end up "losing" naming due to higher-priority constructor
                 *  (see TestCreators#testConstructorCreator() test). And just to avoid running into that problem,
                 *  let's add one more work around
                 */
                /*
                PropertyName name2 = _findExplicitParamName(param, intr);
                if (name2 != null && !name2.isEmpty()) {
                    // Hmmh. Ok, fine. So what are we to do with it... ?
                    // For now... skip. May need to revisit this, should this become problematic
                    continue main_loop;
                }
                */
                if (nonAnnotatedParam == null) {
                    nonAnnotatedParam = param;
                }
            }
            final int namedCount = explicitNameCount + implicitNameCount;

            // Ok: if named or injectable, we have more work to do
            if (explicitNameCount > 0 || injectCount > 0) {
                // simple case; everything covered:
                if ((namedCount + injectCount) == argCount) {
                    creators.addPropertyCreator(factory, false, properties);
                } else if ((explicitNameCount == 0) && ((injectCount + 1) == argCount)) {
                    // secondary: all but one injectable, one un-annotated (un-named)
                    creators.addDelegatingCreator(factory, false, properties, 0);
                } else { // otherwise, epic fail
                    ctxt.reportBadTypeDefinition(beanDesc,
"Argument #%d of factory method %s has no property name annotation; must have name when multiple-parameter constructor annotated as Creator",
                    (nonAnnotatedParam == null) ? -1 : nonAnnotatedParam.getIndex(),
                    factory);
                }
            }
        }
    }

    /*
    /**********************************************************************
    /* Creator introspection: helper methods
    /**********************************************************************
     */

    /**
     * Helper method called when there is the explicit "is-creator" with mode of "delegating"
     *
     * @since 2.9.2
     */
    protected void _addExplicitDelegatingCreator(DeserializationContext ctxt,
            BeanDescription beanDesc, CreatorCollector creators,
            CreatorCandidate candidate)
        throws JsonMappingException
    {
        // Somewhat simple: find injectable values, if any, ensure there is one
        // and just one delegated argument; report violations if any

        int ix = -1;
        final int argCount = candidate.paramCount();
        SettableBeanProperty[] properties = new SettableBeanProperty[argCount];
        for (int i = 0; i < argCount; ++i) {
            AnnotatedParameter param = candidate.parameter(i);
            JacksonInject.Value injectId = candidate.injection(i);
            if (injectId != null) {
                properties[i] = constructCreatorProperty(ctxt, beanDesc, null, i, param, injectId);
                continue;
            }
            if (ix < 0) {
                ix = i;
                continue;
            }
            // Illegal to have more than one value to delegate to
            ctxt.reportBadTypeDefinition(beanDesc,
                    "More than one argument (#%d and #%d) left as delegating for Creator %s: only one allowed",
                    ix, i, candidate);
        }
        // Also, let's require that one Delegating argument does eixt
        if (ix < 0) {
            ctxt.reportBadTypeDefinition(beanDesc,
                    "No argument left as delegating for Creator %s: exactly one required", candidate);
        }
        // 17-Jan-2018, tatu: as per [databind#1853] need to ensure we will distinguish
        //   "well-known" single-arg variants (String, int/long, boolean) from "generic" delegating...
        if (argCount == 1) {
            _handleSingleArgumentCreator(creators, candidate.creator(), true, true);
            // one more thing: sever link to creator property, to avoid possible later
            // problems with "unresolved" constructor property
            BeanPropertyDefinition paramDef = candidate.propertyDef(0);
            if (paramDef != null) {
                ((POJOPropertyBuilder) paramDef).removeConstructors();
            }
            return;
        }
        creators.addDelegatingCreator(candidate.creator(), true, properties, ix);
    }

    /**
     * Helper method called when there is the explicit "is-creator" annotation with mode
     * of "properties-based"
     *
     * @since 2.9.2
     */
    protected void _addExplicitPropertyCreator(DeserializationContext ctxt,
            BeanDescription beanDesc, CreatorCollector creators,
            CreatorCandidate candidate)
        throws JsonMappingException
    {
        final int paramCount = candidate.paramCount();
        SettableBeanProperty[] properties = new SettableBeanProperty[paramCount];

        for (int i = 0; i < paramCount; ++i) {
            JacksonInject.Value injectId = candidate.injection(i);
            AnnotatedParameter param = candidate.parameter(i);
            PropertyName name = candidate.paramName(i);
            if (name == null) {
                // 21-Sep-2017, tatu: Looks like we want to block accidental use of Unwrapped,
                //   as that will not work with Creators well at all
                NameTransformer unwrapper = ctxt.getAnnotationIntrospector().findUnwrappingNameTransformer(param);
                if (unwrapper != null) {
                    _reportUnwrappedCreatorProperty(ctxt, beanDesc, param);
                    /*
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, UNWRAPPED_CREATOR_PARAM_NAME, i, param, null);
                    ++explicitNameCount;
                    */
                }
                name = candidate.findImplicitParamName(i);
                _validateNamedPropertyParameter(ctxt, beanDesc, candidate, i,
                        name, injectId);
            }
            properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectId);
        }
        creators.addPropertyCreator(candidate.creator(), true, properties);
    }

    @Deprecated // since 2.12, remove from 2.13 or later
    protected void _addExplicitAnyCreator(DeserializationContext ctxt,
            BeanDescription beanDesc, CreatorCollector creators,
            CreatorCandidate candidate)
        throws JsonMappingException
    {
        _addExplicitAnyCreator(ctxt, beanDesc, creators, candidate,
                ctxt.getConfig().getConstructorDetector());
    }

    /**
     * Helper method called when there is explicit "is-creator" marker,
     * but no mode declaration.
     *
     * @since 2.12
     */
    protected void _addExplicitAnyCreator(DeserializationContext ctxt,
            BeanDescription beanDesc, CreatorCollector creators,
            CreatorCandidate candidate, ConstructorDetector ctorDetector)
        throws JsonMappingException
    {
        // Looks like there's bit of magic regarding 1-parameter creators; others simpler:
        if (1 != candidate.paramCount()) {
            // Ok: for delegates, we want one and exactly one parameter without
            // injection AND without name

            // 13-Sep-2020, tatu: Can only be delegating if not forced to be properties-based
            if (!ctorDetector.singleArgCreatorDefaultsToProperties()) {
                int oneNotInjected = candidate.findOnlyParamWithoutInjection();
                if (oneNotInjected >= 0) {
                    // getting close; but most not have name (or be explicitly specified
                    // as default-to-delegate)
                    if (ctorDetector.singleArgCreatorDefaultsToDelegating()
                            || candidate.paramName(oneNotInjected) == null) {
                        _addExplicitDelegatingCreator(ctxt, beanDesc, creators, candidate);
                        return;
                    }
                }
            }
            _addExplicitPropertyCreator(ctxt, beanDesc, creators, candidate);
            return;
        }

        // And here be the "simple" single-argument construct
        final AnnotatedParameter param = candidate.parameter(0);
        final JacksonInject.Value injectId = candidate.injection(0);
        PropertyName paramName = null;

        boolean useProps;
        switch (ctorDetector.singleArgMode()) {
        case DELEGATING:
            useProps = false;
            break;
        case PROPERTIES:
            useProps = true;
            // 13-Sep-2020, tatu: since we are configured to prefer Properties-style,
            //    any name (explicit OR implicit does):
            paramName = candidate.paramName(0);
            // [databind#2977]: Need better exception if name missing
            if (paramName == null) {
                _validateNamedPropertyParameter(ctxt, beanDesc, candidate, 0,
                        paramName, injectId);
            }
            break;

        case REQUIRE_MODE:
            ctxt.reportBadTypeDefinition(beanDesc,
"Single-argument constructor (%s) is annotated but no 'mode' defined; `CreatorDetector`"
+ "configured with `SingleArgConstructor.REQUIRE_MODE`",
candidate.creator());
            return;
        case HEURISTIC:
        default:
            { // Note: behavior pre-Jackson-2.12
                final BeanPropertyDefinition paramDef = candidate.propertyDef(0);
                // with heuristic, need to start with just explicit name
                paramName = candidate.explicitParamName(0);

                // If there's injection or explicit name, should be properties-based
                useProps = (paramName != null);
                if (!useProps) {
                    // Otherwise, `@JsonValue` suggests delegation
                    if (beanDesc.findJsonValueAccessor() != null) {
                        ;
                    } else if (injectId != null) {
                        // But Injection suggests property-based (for legacy reasons?)
                        useProps = true;
                    } else if (paramDef != null) {
                        // One more thing: if implicit name matches property with a getter
                        // or field, we'll consider it property-based as well

                        // 25-May-2018, tatu: as per [databind#2051], looks like we have to get
                        //    not implicit name, but name with possible strategy-based-rename
        //            paramName = candidate.findImplicitParamName(0);
                        paramName = candidate.paramName(0);
                        useProps = (paramName != null) && paramDef.couldSerialize();
                    }
                }
            }
        }

        if (useProps) {
            SettableBeanProperty[] properties = new SettableBeanProperty[] {
                    constructCreatorProperty(ctxt, beanDesc, paramName, 0, param, injectId)
            };
            creators.addPropertyCreator(candidate.creator(), true, properties);
            return;
        }

        _handleSingleArgumentCreator(creators, candidate.creator(), true, true);

        // one more thing: sever link to creator property, to avoid possible later
        // problems with "unresolved" constructor property
        final BeanPropertyDefinition paramDef = candidate.propertyDef(0);
        if (paramDef != null) {
            ((POJOPropertyBuilder) paramDef).removeConstructors();
        }
    }

    private boolean _checkIfCreatorPropertyBased(BeanDescription beanDesc,
            AnnotationIntrospector intr,
            AnnotatedWithParams creator, BeanPropertyDefinition propDef)
    {
        // If explicit name, property-based
        if ((propDef != null) && propDef.isExplicitlyNamed()) {
            return true;
        }
        // 01-Dec-2022, tatu: [databind#3654] Consider `@JsonValue` to strongly
        //    hint at delegation-based
        if (beanDesc.findJsonValueAccessor() != null) {
            return false;
        }

        // Inject id considered property-based
        // 01-Dec-2022, tatu: ... but why?
        if (intr.findInjectableValue(creator.getParameter(0)) != null) {
            return true;
        }
        if (propDef != null) {
            // One more thing: if implicit name matches property with a getter
            // or field, we'll consider it property-based as well
            String implName = propDef.getName();
            if (implName != null && !implName.isEmpty()) {
                if (propDef.couldSerialize()) {
                    return true;
                }
            }
            // [databind#3897]: Record canonical constructor will have implicitly named propDef
            if (!propDef.isExplicitlyNamed() && beanDesc.isRecordType()) {
                return true;
            }
        }
        // in absence of everything else, default to delegating
        return false;
    }

    private void _checkImplicitlyNamedConstructors(DeserializationContext ctxt,
            BeanDescription beanDesc, VisibilityChecker<?> vchecker,
            AnnotationIntrospector intr, CreatorCollector creators,
            List<AnnotatedWithParams> implicitCtors) throws JsonMappingException
    {
        AnnotatedWithParams found = null;
        SettableBeanProperty[] foundProps = null;

        // Further checks: (a) must have names for all parameters, (b) only one visible
        // Also, since earlier matching of properties and creators relied on existence of
        // `@JsonCreator` (or equivalent) annotation, we need to do bit more re-inspection...

        main_loop:
        for (AnnotatedWithParams ctor : implicitCtors) {
            if (!vchecker.isCreatorVisible(ctor)) {
                continue;
            }
            // as per earlier notes, only end up here if no properties associated with creator
            final int argCount = ctor.getParameterCount();
            SettableBeanProperty[] properties = new SettableBeanProperty[argCount];
            for (int i = 0; i < argCount; ++i) {
                final AnnotatedParameter param = ctor.getParameter(i);
                final PropertyName name = _findParamName(param, intr);

                // must have name (implicit fine)
                if (name == null || name.isEmpty()) {
                    continue main_loop;
                }
                properties[i] = constructCreatorProperty(ctxt, beanDesc, name, param.getIndex(),
                        param, /*injectId*/ null);
            }
            if (found != null) { // only one allowed; but multiple not an error
                found = null;
                break;
            }
            found = ctor;
            foundProps = properties;
        }
        // found one and only one visible? Ship it!
        if (found != null) {
            creators.addPropertyCreator(found, /*isCreator*/ false, foundProps);
            BasicBeanDescription bbd = (BasicBeanDescription) beanDesc;
            // Also: add properties, to keep error messages complete wrt known properties...
            for (SettableBeanProperty prop : foundProps) {
                PropertyName pn = prop.getFullName();
                if (!bbd.hasProperty(pn)) {
                    BeanPropertyDefinition newDef = SimpleBeanPropertyDefinition.construct(
                            ctxt.getConfig(), prop.getMember(), pn);
                    bbd.addProperty(newDef);
                }
            }
        }
    }

    protected boolean _handleSingleArgumentCreator(CreatorCollector creators,
            AnnotatedWithParams ctor, boolean isCreator, boolean isVisible)
    {
        // otherwise either 'simple' number, String, or general delegate:
        Class<?> type = ctor.getRawParameterType(0);
        if (type == String.class || type == CLASS_CHAR_SEQUENCE) {
            if (isCreator || isVisible) {
                creators.addStringCreator(ctor, isCreator);
            }
            return true;
        }
        if (type == int.class || type == Integer.class) {
            if (isCreator || isVisible) {
                creators.addIntCreator(ctor, isCreator);
            }
            return true;
        }
        if (type == long.class || type == Long.class) {
            if (isCreator || isVisible) {
                creators.addLongCreator(ctor, isCreator);
            }
            return true;
        }
        if (type == double.class || type == Double.class) {
            if (isCreator || isVisible) {
                creators.addDoubleCreator(ctor, isCreator);
            }
            return true;
        }
        if (type == boolean.class || type == Boolean.class) {
            if (isCreator || isVisible) {
                creators.addBooleanCreator(ctor, isCreator);
            }
            return true;
        }
        if (type == BigInteger.class) {
            if (isCreator || isVisible) {
                creators.addBigIntegerCreator(ctor, isCreator);
            }
        }
        if (type == BigDecimal.class) {
            if (isCreator || isVisible) {
                creators.addBigDecimalCreator(ctor, isCreator);
            }
        }
        // Delegating Creator ok iff it has @JsonCreator (etc)
        if (isCreator) {
            creators.addDelegatingCreator(ctor, isCreator, null, 0);
            return true;
        }
        return false;
    }

    // Helper method to check that parameter of Property-based creator either
    // has name or is marked as Injectable
    //
    // @since 2.12.1
    protected void _validateNamedPropertyParameter(DeserializationContext ctxt,
            BeanDescription beanDesc,
            CreatorCandidate candidate, int paramIndex,
            PropertyName name, JacksonInject.Value injectId)
        throws JsonMappingException
    {
        // Must be injectable or have name; without either won't work
        if ((name == null) && (injectId == null)) {
            ctxt.reportBadTypeDefinition(beanDesc,
"Argument #%d of constructor %s has no property name (and is not Injectable): can not use as property-based Creator",
paramIndex, candidate);
        }
    }

    // 01-Dec-2016, tatu: As per [databind#265] we cannot yet support passing
    //   of unwrapped values through creator properties, so fail fast
    protected void _reportUnwrappedCreatorProperty(DeserializationContext ctxt,
            BeanDescription beanDesc, AnnotatedParameter param)
        throws JsonMappingException
    {
        ctxt.reportBadTypeDefinition(beanDesc,
"Cannot define Creator parameter %d as `@JsonUnwrapped`: combination not yet supported",
                param.getIndex());
    }

    /**
     * Method that will construct a property object that represents
     * a logical property passed via Creator (constructor or static
     * factory method)
     */
    protected SettableBeanProperty constructCreatorProperty(DeserializationContext ctxt,
            BeanDescription beanDesc, PropertyName name, int index,
            AnnotatedParameter param,
            JacksonInject.Value injectable)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        PropertyMetadata metadata;
        final PropertyName wrapperName;
        {
            if (intr == null) {
                metadata = PropertyMetadata.STD_REQUIRED_OR_OPTIONAL;
                wrapperName = null;
            } else {
                Boolean b = intr.hasRequiredMarker(param);
                String desc = intr.findPropertyDescription(param);
                Integer idx = intr.findPropertyIndex(param);
                String def = intr.findPropertyDefaultValue(param);
                metadata = PropertyMetadata.construct(b, desc, idx, def);
                wrapperName = intr.findWrapperName(param);
            }
        }
        JavaType type = resolveMemberAndTypeAnnotations(ctxt, param, param.getType());
        BeanProperty.Std property = new BeanProperty.Std(name, type,
                wrapperName, param, metadata);
        // Type deserializer: either comes from property (and already resolved)
        TypeDeserializer typeDeser = (TypeDeserializer) type.getTypeHandler();
        // or if not, based on type being referenced:
        if (typeDeser == null) {
            typeDeser = findTypeDeserializer(config, type);
        }

        // 22-Sep-2019, tatu: for [databind#2458] need more work on getting metadata
        //   about SetterInfo, mergeability
        metadata = _getSetterInfo(ctxt, property, metadata);

        // Note: contextualization of typeDeser _should_ occur in constructor of CreatorProperty
        // so it is not called directly here
        SettableBeanProperty prop = CreatorProperty.construct(name, type, property.getWrapperName(),
                typeDeser, beanDesc.getClassAnnotations(), param, index, injectable,
                metadata);
        JsonDeserializer<?> deser = findDeserializerFromAnnotation(ctxt, param);
        if (deser == null) {
            deser = type.getValueHandler();
        }
        if (deser != null) {
            // As per [databind#462] need to ensure we contextualize deserializer before passing it on
            deser = ctxt.handlePrimaryContextualization(deser, prop, type);
            prop = prop.withValueDeserializer(deser);
        }
        return prop;
    }

    private PropertyName _findParamName(AnnotatedParameter param, AnnotationIntrospector intr)
    {
        if (intr != null) {
            PropertyName name = intr.findNameForDeserialization(param);
            if (name != null) {
                // 16-Nov-2020, tatu: One quirk, wrt [databind#2932]; may get "use implicit"
                //    marker; should not return that
                if (!name.isEmpty()) {
                    return name;
                }
            }
            // 14-Apr-2014, tatu: Need to also consider possible implicit name
            //  (for JDK8, or via paranamer)

            String str = intr.findImplicitPropertyName(param);
            if (str != null && !str.isEmpty()) {
                return PropertyName.construct(str);
            }
        }
        return null;
    }

    /**
     * Helper method copied from {@code POJOPropertyBuilder} since that won't be
     * applied to creator parameters
     *
     * @since 2.10
     */
    protected PropertyMetadata _getSetterInfo(DeserializationContext ctxt,
            BeanProperty prop, PropertyMetadata metadata)
    {
        final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        final DeserializationConfig config = ctxt.getConfig();

        boolean needMerge = true;
        Nulls valueNulls = null;
        Nulls contentNulls = null;

        // NOTE: compared to `POJOPropertyBuilder`, we only have access to creator
        // parameter, not other accessors, so code bit simpler
        AnnotatedMember prim = prop.getMember();

        if (prim != null) {
            // Ok, first: does property itself have something to say?
            if (intr != null) {
                JsonSetter.Value setterInfo = intr.findSetterInfo(prim);
                if (setterInfo != null) {
                    valueNulls = setterInfo.nonDefaultValueNulls();
                    contentNulls = setterInfo.nonDefaultContentNulls();
                }
            }
            // If not, config override?
            // 25-Oct-2016, tatu: Either this, or type of accessor...
            if (needMerge || (valueNulls == null) || (contentNulls == null)) {
                ConfigOverride co = config.getConfigOverride(prop.getType().getRawClass());
                JsonSetter.Value setterInfo = co.getSetterInfo();
                if (setterInfo != null) {
                    if (valueNulls == null) {
                        valueNulls = setterInfo.nonDefaultValueNulls();
                    }
                    if (contentNulls == null) {
                        contentNulls = setterInfo.nonDefaultContentNulls();
                    }
                }
            }
        }
        if (needMerge || (valueNulls == null) || (contentNulls == null)) {
            JsonSetter.Value setterInfo = config.getDefaultSetterInfo();
            if (valueNulls == null) {
                valueNulls = setterInfo.nonDefaultValueNulls();
            }
            if (contentNulls == null) {
                contentNulls = setterInfo.nonDefaultContentNulls();
            }
        }
        if ((valueNulls != null) || (contentNulls != null)) {
            metadata = metadata.withNulls(valueNulls, contentNulls);
        }
        return metadata;
    }

    /*
    /**********************************************************
    /* DeserializerFactory impl: array deserializers
    /**********************************************************
     */

    @Override
    public JsonDeserializer<?> createArrayDeserializer(DeserializationContext ctxt,
            ArrayType type, final BeanDescription beanDesc)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        JavaType elemType = type.getContentType();

        // Very first thing: is deserializer hard-coded for elements?
        JsonDeserializer<Object> contentDeser = elemType.getValueHandler();
        // Then optional type info: if type has been resolved, we may already know type deserializer:
        TypeDeserializer elemTypeDeser = elemType.getTypeHandler();
        // but if not, may still be possible to find:
        if (elemTypeDeser == null) {
            elemTypeDeser = findTypeDeserializer(config, elemType);
        }
        // 23-Nov-2010, tatu: Custom array deserializer?
        JsonDeserializer<?>  deser = _findCustomArrayDeserializer(type,
                config, beanDesc, elemTypeDeser, contentDeser);
        if (deser == null) {
            if (contentDeser == null) {
                Class<?> raw = elemType.getRawClass();
                if (elemType.isPrimitive()) {
                    return PrimitiveArrayDeserializers.forType(raw);
                }
                if (raw == String.class) {
                    return StringArrayDeserializer.instance;
                }
            }
            deser = new ObjectArrayDeserializer(type, contentDeser, elemTypeDeser);
        }
        // and then new with 2.2: ability to post-process it too (databind#120)
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deser = mod.modifyArrayDeserializer(config, type, beanDesc, deser);
            }
        }
        return deser;
    }

    /*
    /**********************************************************************
    /* DeserializerFactory impl: Collection(-like) deserializers
    /**********************************************************************
     */

    @Override
    public JsonDeserializer<?> createCollectionDeserializer(DeserializationContext ctxt,
            CollectionType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        JavaType contentType = type.getContentType();
        // Very first thing: is deserializer hard-coded for elements?
        JsonDeserializer<Object> contentDeser = contentType.getValueHandler();
        final DeserializationConfig config = ctxt.getConfig();

        // Then optional type info: if type has been resolved, we may already know type deserializer:
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        // but if not, may still be possible to find:
        if (contentTypeDeser == null) {
            contentTypeDeser = findTypeDeserializer(config, contentType);
        }
        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> deser = _findCustomCollectionDeserializer(type,
                config, beanDesc, contentTypeDeser, contentDeser);
        if (deser == null) {
            Class<?> collectionClass = type.getRawClass();
            if (contentDeser == null) { // not defined by annotation
                // One special type: EnumSet:
                if (EnumSet.class.isAssignableFrom(collectionClass)) {
                    deser = new EnumSetDeserializer(contentType, null);
                }
            }
        }

        /* One twist: if we are being asked to instantiate an interface or
         * abstract Collection, we need to either find something that implements
         * the thing, or give up.
         *
         * Note that we do NOT try to guess based on secondary interfaces
         * here; that would probably not work correctly since casts would
         * fail later on (as the primary type is not the interface we'd
         * be implementing)
         */
        if (deser == null) {
            if (type.isInterface() || type.isAbstract()) {
                CollectionType implType = _mapAbstractCollectionType(type, config);
                if (implType == null) {
                    // [databind#292]: Actually, may be fine, but only if polymorphich deser enabled
                    if (type.getTypeHandler() == null) {
                        throw new IllegalArgumentException("Cannot find a deserializer for non-concrete Collection type "+type);
                    }
                    deser = AbstractDeserializer.constructForNonPOJO(beanDesc);
                } else {
                    type = implType;
                    // But if so, also need to re-check creators...
                    beanDesc = config.introspectForCreation(type);
                }
            }
            if (deser == null) {
                ValueInstantiator inst = findValueInstantiator(ctxt, beanDesc);
                if (!inst.canCreateUsingDefault()) {
                    // [databind#161]: No default constructor for ArrayBlockingQueue...
                    if (type.hasRawClass(ArrayBlockingQueue.class)) {
                        return new ArrayBlockingQueueDeserializer(type, contentDeser, contentTypeDeser, inst);
                    }
                    // 10-Jan-2017, tatu: `java.util.Collections` types need help:
                    deser = JavaUtilCollectionsDeserializers.findForCollection(ctxt, type);
                    if (deser != null) {
                        return deser;
                    }
                }
                // Can use more optimal deserializer if content type is String, so:
                if (contentType.hasRawClass(String.class)) {
                    // no value type deserializer because Strings are one of natural/native types:
                    deser = new StringCollectionDeserializer(type, contentDeser, inst);
                } else {
                    deser = new CollectionDeserializer(type, contentDeser, contentTypeDeser, inst);
                }
            }
        }
        // allow post-processing it too
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deser = mod.modifyCollectionDeserializer(config, type, beanDesc, deser);
            }
        }
        return deser;
    }

    protected CollectionType _mapAbstractCollectionType(JavaType type, DeserializationConfig config)
    {
        final Class<?> collectionClass = ContainerDefaultMappings.findCollectionFallback(type);
        if (collectionClass != null) {
            return (CollectionType) config.getTypeFactory()
                    .constructSpecializedType(type, collectionClass, true);
        }
        return null;
    }

    // Copied almost verbatim from "createCollectionDeserializer" -- should try to share more code
    @Override
    public JsonDeserializer<?> createCollectionLikeDeserializer(DeserializationContext ctxt,
            CollectionLikeType type, final BeanDescription beanDesc)
        throws JsonMappingException
    {
        JavaType contentType = type.getContentType();
        // Very first thing: is deserializer hard-coded for elements?
        JsonDeserializer<Object> contentDeser = contentType.getValueHandler();
        final DeserializationConfig config = ctxt.getConfig();

        // Then optional type info (1.5): if type has been resolved, we may already know type deserializer:
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        // but if not, may still be possible to find:
        if (contentTypeDeser == null) {
            contentTypeDeser = findTypeDeserializer(config, contentType);
        }
        JsonDeserializer<?> deser = _findCustomCollectionLikeDeserializer(type, config, beanDesc,
                contentTypeDeser, contentDeser);
        if (deser != null) {
            // and then new with 2.2: ability to post-process it too (Issue#120)
            if (_factoryConfig.hasDeserializerModifiers()) {
                for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                    deser = mod.modifyCollectionLikeDeserializer(config, type, beanDesc, deser);
                }
            }
        }
        return deser;
    }

    /*
    /**********************************************************
    /* DeserializerFactory impl: Map(-like) deserializers
    /**********************************************************
     */

    @Override
    public JsonDeserializer<?> createMapDeserializer(DeserializationContext ctxt,
            MapType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        JavaType keyType = type.getKeyType();
        JavaType contentType = type.getContentType();

        // First: is there annotation-specified deserializer for values?
        @SuppressWarnings("unchecked")
        JsonDeserializer<Object> contentDeser = (JsonDeserializer<Object>) contentType.getValueHandler();

        // Ok: need a key deserializer (null indicates 'default' here)
        KeyDeserializer keyDes = (KeyDeserializer) keyType.getValueHandler();
        // Then optional type info; either attached to type, or resolved separately:
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        // but if not, may still be possible to find:
        if (contentTypeDeser == null) {
            contentTypeDeser = findTypeDeserializer(config, contentType);
        }

        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> deser = _findCustomMapDeserializer(type, config, beanDesc,
                keyDes, contentTypeDeser, contentDeser);

        if (deser == null) {
            // Value handling is identical for all, but EnumMap requires special handling for keys
            Class<?> mapClass = type.getRawClass();
            if (EnumMap.class.isAssignableFrom(mapClass)) {
                ValueInstantiator inst;

                // 06-Mar-2017, tatu: Should only need to check ValueInstantiator for
                //    custom sub-classes, see [databind#1544]
                if (mapClass == EnumMap.class) {
                    inst = null;
                } else {
                    inst = findValueInstantiator(ctxt, beanDesc);
                }
                if (!keyType.isEnumImplType()) {
                    throw new IllegalArgumentException("Cannot construct EnumMap; generic (key) type not available");
                }
                deser = new EnumMapDeserializer(type, inst, null,
                        contentDeser, contentTypeDeser, null);
            }

            // Otherwise, generic handler works ok.

            /* But there is one more twist: if we are being asked to instantiate
             * an interface or abstract Map, we need to either find something
             * that implements the thing, or give up.
             *
             * Note that we do NOT try to guess based on secondary interfaces
             * here; that would probably not work correctly since casts would
             * fail later on (as the primary type is not the interface we'd
             * be implementing)
             */
            if (deser == null) {
                if (type.isInterface() || type.isAbstract()) {
                    MapType fallback = _mapAbstractMapType(type, config);
                    if (fallback != null) {
                        type = (MapType) fallback;
                        mapClass = type.getRawClass();
                        // But if so, also need to re-check creators...
                        beanDesc = config.introspectForCreation(type);
                    } else {
                        // [databind#292]: Actually, may be fine, but only if polymorphic deser enabled
                        if (type.getTypeHandler() == null) {
                            throw new IllegalArgumentException("Cannot find a deserializer for non-concrete Map type "+type);
                        }
                        deser = AbstractDeserializer.constructForNonPOJO(beanDesc);
                    }
                } else {
                    // 10-Jan-2017, tatu: `java.util.Collections` types need help:
                    deser = JavaUtilCollectionsDeserializers.findForMap(ctxt, type);
                    if (deser != null) {
                        return deser;
                    }
                }
                if (deser == null) {
                    ValueInstantiator inst = findValueInstantiator(ctxt, beanDesc);
                    // 01-May-2016, tatu: Which base type to use here gets tricky, since
                    //   most often it ought to be `Map` or `EnumMap`, but due to abstract
                    //   mapping it will more likely be concrete type like `HashMap`.
                    //   So, for time being, just pass `Map.class`
                    MapDeserializer md = new MapDeserializer(type, inst, keyDes, contentDeser, contentTypeDeser);
                    JsonIgnoreProperties.Value ignorals = config.getDefaultPropertyIgnorals(Map.class,
                            beanDesc.getClassInfo());
                    Set<String> ignored = (ignorals == null) ? null
                            : ignorals.findIgnoredForDeserialization();
                    md.setIgnorableProperties(ignored);
                    JsonIncludeProperties.Value inclusions = config.getDefaultPropertyInclusions(Map.class,
                            beanDesc.getClassInfo());
                    Set<String> included = inclusions == null ? null : inclusions.getIncluded();
                    md.setIncludableProperties(included);
                    deser = md;
                }
            }
        }
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deser = mod.modifyMapDeserializer(config, type, beanDesc, deser);
            }
        }
        return deser;
    }

    protected MapType _mapAbstractMapType(JavaType type, DeserializationConfig config)
    {
        final Class<?> mapClass = ContainerDefaultMappings.findMapFallback(type);
        if (mapClass != null) {
            return (MapType) config.getTypeFactory()
                    .constructSpecializedType(type, mapClass, true);
        }
        return null;
    }

    // Copied almost verbatim from "createMapDeserializer" -- should try to share more code
    @Override
    public JsonDeserializer<?> createMapLikeDeserializer(DeserializationContext ctxt,
            MapLikeType type, final BeanDescription beanDesc)
        throws JsonMappingException
    {
        JavaType keyType = type.getKeyType();
        JavaType contentType = type.getContentType();
        final DeserializationConfig config = ctxt.getConfig();

        // First: is there annotation-specified deserializer for values?
        @SuppressWarnings("unchecked")
        JsonDeserializer<Object> contentDeser = (JsonDeserializer<Object>) contentType.getValueHandler();

        // Ok: need a key deserializer (null indicates 'default' here)
        KeyDeserializer keyDes = (KeyDeserializer) keyType.getValueHandler();
        /* !!! 24-Jan-2012, tatu: NOTE: impls MUST use resolve() to find key deserializer!
        if (keyDes == null) {
            keyDes = p.findKeyDeserializer(config, keyType, property);
        }
        */
        // Then optional type info (1.5); either attached to type, or resolve separately:
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        // but if not, may still be possible to find:
        if (contentTypeDeser == null) {
            contentTypeDeser = findTypeDeserializer(config, contentType);
        }
        JsonDeserializer<?> deser = _findCustomMapLikeDeserializer(type, config,
                beanDesc, keyDes, contentTypeDeser, contentDeser);
        if (deser != null) {
            // and then new with 2.2: ability to post-process it too (Issue#120)
            if (_factoryConfig.hasDeserializerModifiers()) {
                for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                    deser = mod.modifyMapLikeDeserializer(config, type, beanDesc, deser);
                }
            }
        }
        return deser;
    }

    /*
    /**********************************************************
    /* DeserializerFactory impl: other types
    /**********************************************************
     */

    /**
     * Factory method for constructing serializers of {@link Enum} types.
     */
    @Override
    public JsonDeserializer<?> createEnumDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        final Class<?> enumClass = type.getRawClass();
        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> deser = _findCustomEnumDeserializer(enumClass, config, beanDesc);

        if (deser == null) {
            // 12-Feb-2020, tatu: while we can't really create real deserializer for `Enum.class`,
            //    it is necessary to allow it in one specific case: see [databind#2605] for details
            //    but basically it can be used as polymorphic base.
            //    We could check `type.getTypeHandler()` to look for that case but seems like we
            //    may as well simply create placeholder (AbstractDeserializer) regardless
            if (enumClass == Enum.class) {
                return AbstractDeserializer.constructForNonPOJO(beanDesc);
            }

            ValueInstantiator valueInstantiator = _constructDefaultValueInstantiator(ctxt, beanDesc);
            SettableBeanProperty[] creatorProps = (valueInstantiator == null) ? null
                    : valueInstantiator.getFromObjectArguments(ctxt.getConfig());
            // May have @JsonCreator for static factory method:
            for (AnnotatedMethod factory : beanDesc.getFactoryMethods()) {
                if (_hasCreatorAnnotation(ctxt, factory)) {
                    if (factory.getParameterCount() == 0) { // [databind#960]
                        deser = EnumDeserializer.deserializerForNoArgsCreator(config, enumClass, factory);
                        break;
                    }
                    Class<?> returnType = factory.getRawReturnType();
                    // usually should be class, but may be just plain Enum<?> (for Enum.valueOf()?)
                    if (!returnType.isAssignableFrom(enumClass)) {
                        ctxt.reportBadDefinition(type, String.format(
"Invalid `@JsonCreator` annotated Enum factory method [%s]: needs to return compatible type",
factory.toString()));
                    }
                    deser = EnumDeserializer.deserializerForCreator(config, enumClass, factory, valueInstantiator, creatorProps);
                    break;
                }
            }

            // Need to consider @JsonValue if one found
            if (deser == null) {
                deser = new EnumDeserializer(constructEnumResolver(enumClass, config, beanDesc),
                        config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS),
                        constructEnumNamingStrategyResolver(config, enumClass, beanDesc.getClassInfo())
                );
            }
        }

        // and then post-process it too
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deser = mod.modifyEnumDeserializer(config, type, beanDesc, deser);
            }
        }
        return deser;
    }

    @Override
    public JsonDeserializer<?> createTreeDeserializer(DeserializationConfig config,
            JavaType nodeType, BeanDescription beanDesc)
        throws JsonMappingException
    {
        @SuppressWarnings("unchecked")
        Class<? extends JsonNode> nodeClass = (Class<? extends JsonNode>) nodeType.getRawClass();
        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> custom = _findCustomTreeNodeDeserializer(nodeClass, config,
                beanDesc);
        if (custom != null) {
            return custom;
        }
        return JsonNodeDeserializer.getDeserializer(nodeClass);
    }

    @Override
    public JsonDeserializer<?> createReferenceDeserializer(DeserializationContext ctxt,
            ReferenceType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        JavaType contentType = type.getContentType();
        // Very first thing: is deserializer hard-coded for elements?
        JsonDeserializer<Object> contentDeser = contentType.getValueHandler();
        final DeserializationConfig config = ctxt.getConfig();
        // Then optional type info: if type has been resolved, we may already know type deserializer:
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        if (contentTypeDeser == null) { // or if not, may be able to find:
            contentTypeDeser = findTypeDeserializer(config, contentType);
        }
        JsonDeserializer<?> deser = _findCustomReferenceDeserializer(type, config, beanDesc,
                contentTypeDeser, contentDeser);

        if (deser == null) {
            // Just one referential type as of JDK 1.7 / Java 7: AtomicReference (Java 8 adds Optional)
            if (type.isTypeOrSubTypeOf(AtomicReference.class)) {
                Class<?> rawType = type.getRawClass();
                ValueInstantiator inst;
                if (rawType == AtomicReference.class) {
                    inst = null;
                } else {
                    /* 23-Oct-2016, tatu: Note that subtypes are probably not supportable
                     *    without either forcing merging (to avoid having to create instance)
                     *    or something else...
                     */
                    inst = findValueInstantiator(ctxt, beanDesc);
                }
                return new AtomicReferenceDeserializer(type, inst, contentTypeDeser, contentDeser);
            }
        }
        if (deser != null) {
            // and then post-process
            if (_factoryConfig.hasDeserializerModifiers()) {
                for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                    deser = mod.modifyReferenceDeserializer(config, type, beanDesc, deser);
                }
            }
        }
        return deser;
    }

    /*
    /**********************************************************
    /* DeserializerFactory impl (partial): type deserializers
    /**********************************************************
     */

    @Override
    public TypeDeserializer findTypeDeserializer(DeserializationConfig config,
            JavaType baseType)
        throws JsonMappingException
    {
        BeanDescription bean = config.introspectClassAnnotations(baseType.getRawClass());
        AnnotatedClass ac = bean.getClassInfo();
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findTypeResolver(config, ac, baseType);

        // Ok: if there is no explicit type info handler, we may want to
        // use a default. If so, config object knows what to use.
        if (b == null) {
            b = config.getDefaultTyper(baseType);
            if (b == null) {
                return null;
            }
        }
        final Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByTypeId(config, ac);

        // May need to figure out default implementation, if none found yet
        // (note: check for abstract type is not 100% mandatory, more of an optimization)
        if ((b.getDefaultImpl() == null) && baseType.isAbstract()) {
            JavaType defaultType = mapAbstractType(config, baseType);
            // 18-Sep-2021, tatu: We have a shared instance, MUST NOT call mutating method
            //    but the new "mutant factory":
            if ((defaultType != null) && !defaultType.hasRawClass(baseType.getRawClass())) {
                b = b.withDefaultImpl(defaultType.getRawClass());
            }
        }
        // 05-Apt-2018, tatu: Since we get non-mapping exception due to various limitations,
        //    map to better type here
        try {
            return b.buildTypeDeserializer(config, baseType, subtypes);
        } catch (IllegalArgumentException | IllegalStateException e0) {
            throw InvalidDefinitionException.from((JsonParser) null,
                    ClassUtil.exceptionMessage(e0), baseType)
                .withCause(e0);
        }
    }

    /**
     * Overridable method called after checking all other types.
     *
     * @since 2.2
     */
    protected JsonDeserializer<?> findOptionalStdDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        return OptionalHandlerFactory.instance.findDeserializer(type, ctxt.getConfig(), beanDesc);
    }

    /*
    /**********************************************************
    /* DeserializerFactory impl (partial): key deserializers
    /**********************************************************
     */

    @Override
    public KeyDeserializer createKeyDeserializer(DeserializationContext ctxt,
            JavaType type)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        BeanDescription beanDesc = null;
        KeyDeserializer deser = null;
        if (_factoryConfig.hasKeyDeserializers()) {
            beanDesc = config.introspectClassAnnotations(type);
            for (KeyDeserializers d  : _factoryConfig.keyDeserializers()) {
                deser = d.findKeyDeserializer(type, config, beanDesc);
                if (deser != null) {
                    break;
                }
            }
        }

        // the only non-standard thing is this:
        if (deser == null) {
            // [databind#2452]: Support `@JsonDeserialize(keyUsing = ...)`
            if (beanDesc == null) {
                beanDesc = config.introspectClassAnnotations(type.getRawClass());
            }
            deser = findKeyDeserializerFromAnnotation(ctxt, beanDesc.getClassInfo());
            if (deser == null) {
                if (type.isEnumType()) {
                    deser = _createEnumKeyDeserializer(ctxt, type);
                } else {
                    deser = StdKeyDeserializers.findStringBasedKeyDeserializer(config, type);
                }
            }
        }
        // and then post-processing
        if (deser != null) {
            if (_factoryConfig.hasDeserializerModifiers()) {
                for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                    deser = mod.modifyKeyDeserializer(config, type, deser);
                }
            }
        }
        return deser;
    }

    private KeyDeserializer _createEnumKeyDeserializer(DeserializationContext ctxt,
            JavaType type)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        Class<?> enumClass = type.getRawClass();

        BeanDescription beanDesc = config.introspect(type);
        // 24-Sep-2015, bim: a key deserializer is the preferred thing.
        KeyDeserializer des = findKeyDeserializerFromAnnotation(ctxt, beanDesc.getClassInfo());
        if (des != null) {
            return des;
        } else {
            // 24-Sep-2015, bim: if no key deser, look for enum deserializer first, then a plain deser.
            JsonDeserializer<?> custom = _findCustomEnumDeserializer(enumClass, config, beanDesc);
            if (custom != null) {
                return StdKeyDeserializers.constructDelegatingKeyDeserializer(config, type, custom);
            }
            JsonDeserializer<?> valueDesForKey = findDeserializerFromAnnotation(ctxt, beanDesc.getClassInfo());
            if (valueDesForKey != null) {
                return StdKeyDeserializers.constructDelegatingKeyDeserializer(config, type, valueDesForKey);
            }
        }
        EnumResolver enumRes = constructEnumResolver(enumClass, config, beanDesc);
        EnumResolver byEnumNamingResolver = constructEnumNamingStrategyResolver(config, enumClass, beanDesc.getClassInfo());

        // May have @JsonCreator for static factory method
        for (AnnotatedMethod factory : beanDesc.getFactoryMethods()) {
            if (_hasCreatorAnnotation(ctxt, factory)) {
                int argCount = factory.getParameterCount();
                if (argCount == 1) {
                    Class<?> returnType = factory.getRawReturnType();
                    // usually should be class, but may be just plain Enum<?> (for Enum.valueOf()?)
                    if (returnType.isAssignableFrom(enumClass)) {
                        // note: mostly copied from 'EnumDeserializer.deserializerForCreator(...)'
                        if (factory.getRawParameterType(0) != String.class) {
                            // [databind#2725]: Should not error out because (1) there may be good creator
                            //   method and (2) this method may be valid for "regular" enum value deserialization
                            // (leaving aside potential for multiple conflicting creators)
//                            throw new IllegalArgumentException("Parameter #0 type for factory method ("+factory+") not suitable, must be java.lang.String");
                            continue;
                        }
                        if (config.canOverrideAccessModifiers()) {
                            ClassUtil.checkAndFixAccess(factory.getMember(),
                                    ctxt.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
                        }
                        return StdKeyDeserializers.constructEnumKeyDeserializer(enumRes, factory, byEnumNamingResolver);
                    }
                }
                throw new IllegalArgumentException("Unsuitable method ("+factory+") decorated with @JsonCreator (for Enum type "
                        +enumClass.getName()+")");
            }
        }
        // Also, need to consider @JsonValue, if one found
        return StdKeyDeserializers.constructEnumKeyDeserializer(enumRes, byEnumNamingResolver);
    }

    /*
    /**********************************************************
    /* DeserializerFactory impl: checking explicitly registered desers
    /**********************************************************
     */

    @Override
    public boolean hasExplicitDeserializerFor(DeserializationConfig config,
            Class<?> valueType)
    {
        // First things first: unpeel Array types as the element type is
        // what we are interested in -- this because we will always support array
        // types via composition, and since array types are JDK provided (and hence
        // can not be custom or customized).
        while (valueType.isArray()) {
            valueType = valueType.getComponentType();
        }

        // Yes, we handle all Enum types
        if (Enum.class.isAssignableFrom(valueType)) {
            return true;
        }
        // Numbers?
        final String clsName = valueType.getName();
        if (clsName.startsWith("java.")) {
            if (Collection.class.isAssignableFrom(valueType)) {
                return true;
            }
            if (Map.class.isAssignableFrom(valueType)) {
                return true;
            }
            if (Number.class.isAssignableFrom(valueType)) {
                return NumberDeserializers.find(valueType, clsName) != null;
            }
            if (JdkDeserializers.hasDeserializerFor(valueType)
                    || (valueType == CLASS_STRING)
                    || (valueType == Boolean.class)
                    || (valueType == EnumMap.class)
                    || (valueType == AtomicReference.class)
                    ) {
                return true;
            }
            if (DateDeserializers.hasDeserializerFor(valueType)) {
                return true;
            }
        } else if (clsName.startsWith("com.fasterxml.")) {
            return JsonNode.class.isAssignableFrom(valueType)
                   || (valueType == TokenBuffer.class);
        } else {
            return OptionalHandlerFactory.instance.hasDeserializerFor(valueType);
        }
        return false;
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    /**
     * Method called to create a type information deserializer for values of
     * given non-container property, if one is needed.
     * If not needed (no polymorphic handling configured for property), should return null.
     *<p>
     * Note that this method is only called for non-container bean properties,
     * and not for values in container types or root values (or container properties)
     *
     * @param baseType Declared base type of the value to deserializer (actual
     *    deserializer type will be this type or its subtype)
     *
     * @return Type deserializer to use for given base type, if one is needed; null if not.
     */
    public TypeDeserializer findPropertyTypeDeserializer(DeserializationConfig config,
            JavaType baseType, AnnotatedMember annotated)
        throws JsonMappingException
    {
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findPropertyTypeResolver(config, annotated, baseType);
        // Defaulting: if no annotations on member, check value class
        if (b == null) {
            return findTypeDeserializer(config, baseType);
        }
        // but if annotations found, may need to resolve subtypes:
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByTypeId(
                config, annotated, baseType);
        try {
            return b.buildTypeDeserializer(config, baseType, subtypes);
        } catch (IllegalArgumentException | IllegalStateException e0) {
           throw InvalidDefinitionException.from((JsonParser) null,
                    ClassUtil.exceptionMessage(e0), baseType)
               .withCause(e0);
        }
    }

    /**
     * Method called to find and create a type information deserializer for values of
     * given container (list, array, map) property, if one is needed.
     * If not needed (no polymorphic handling configured for property), should return null.
     *<p>
     * Note that this method is only called for container bean properties,
     * and not for values in container types or root values (or non-container properties)
     *
     * @param containerType Type of property; must be a container type
     * @param propertyEntity Field or method that contains container property
     */
    public TypeDeserializer findPropertyContentTypeDeserializer(DeserializationConfig config,
            JavaType containerType, AnnotatedMember propertyEntity)
        throws JsonMappingException
    {
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findPropertyContentTypeResolver(config, propertyEntity, containerType);
        JavaType contentType = containerType.getContentType();
        // Defaulting: if no annotations on member, check class
        if (b == null) {
            return findTypeDeserializer(config, contentType);
        }
        // but if annotations found, may need to resolve subtypes:
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByTypeId(
                config, propertyEntity, contentType);
        return b.buildTypeDeserializer(config, contentType, subtypes);
    }

    /**
     * Helper method called to find one of default serializers for "well-known"
     * platform types: JDK-provided types, and small number of public Jackson
     * API types.
     *
     * @since 2.2
     */
    public JsonDeserializer<?> findDefaultDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        Class<?> rawType = type.getRawClass();
        // Object ("untyped"), and as of 2.10 (see [databind#2115]), `java.io.Serializable`
        if ((rawType == CLASS_OBJECT) || (rawType == CLASS_SERIALIZABLE)) {
            // 11-Feb-2015, tatu: As per [databind#700] need to be careful wrt non-default Map, List.
            DeserializationConfig config = ctxt.getConfig();
            JavaType lt, mt;

            if (_factoryConfig.hasAbstractTypeResolvers()) {
                lt = _findRemappedType(config, List.class);
                mt = _findRemappedType(config, Map.class);
            } else {
                lt = mt = null;
            }
            return new UntypedObjectDeserializer(lt, mt);
        }
        // String and equivalents
        if (rawType == CLASS_STRING || rawType == CLASS_CHAR_SEQUENCE) {
            return StringDeserializer.instance;
        }
        if (rawType == CLASS_ITERABLE) {
            // [databind#199]: Can and should 'upgrade' to a Collection type:
            TypeFactory tf = ctxt.getTypeFactory();
            JavaType[] tps = tf.findTypeParameters(type, CLASS_ITERABLE);
            JavaType elemType = (tps == null || tps.length != 1) ? TypeFactory.unknownType() : tps[0];
            CollectionType ct = tf.constructCollectionType(Collection.class, elemType);
            // Should we re-introspect beanDesc? For now let's not...
            return createCollectionDeserializer(ctxt, ct, beanDesc);
        }
        if (rawType == CLASS_MAP_ENTRY) {
            // 28-Apr-2015, tatu: TypeFactory does it all for us already so
            JavaType kt = type.containedTypeOrUnknown(0);
            JavaType vt = type.containedTypeOrUnknown(1);
            TypeDeserializer vts = (TypeDeserializer) vt.getTypeHandler();
            if (vts == null) {
                vts = findTypeDeserializer(ctxt.getConfig(), vt);
            }
            JsonDeserializer<Object> valueDeser = vt.getValueHandler();
            KeyDeserializer keyDes = (KeyDeserializer) kt.getValueHandler();
            return new MapEntryDeserializer(type, keyDes, valueDeser, vts);
        }
        String clsName = rawType.getName();
        if (rawType.isPrimitive() || clsName.startsWith("java.")) {
            // Primitives/wrappers, other Numbers:
            JsonDeserializer<?> deser = NumberDeserializers.find(rawType, clsName);
            if (deser == null) {
                deser = DateDeserializers.find(rawType, clsName);
            }
            if (deser != null) {
                return deser;
            }
        }
        // and a few Jackson types as well:
        if (rawType == TokenBuffer.class) {
            return new TokenBufferDeserializer();
        }
        JsonDeserializer<?> deser = findOptionalStdDeserializer(ctxt, type, beanDesc);
        if (deser != null) {
            return deser;
        }
        return JdkDeserializers.find(ctxt, rawType, clsName);
    }

    protected JavaType _findRemappedType(DeserializationConfig config, Class<?> rawType) throws JsonMappingException {
        JavaType type = mapAbstractType(config, config.constructType(rawType));
        return (type == null || type.hasRawClass(rawType)) ? null : type;
    }

    /*
    /**********************************************************
    /* Helper methods, finding custom deserializers
    /**********************************************************
     */

    protected JsonDeserializer<?> _findCustomTreeNodeDeserializer(Class<? extends JsonNode> type,
            DeserializationConfig config, BeanDescription beanDesc)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findTreeNodeDeserializer(type, config, beanDesc);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected JsonDeserializer<?> _findCustomReferenceDeserializer(ReferenceType type,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findReferenceDeserializer(type, config, beanDesc,
                    contentTypeDeserializer, contentDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected JsonDeserializer<Object> _findCustomBeanDeserializer(JavaType type,
            DeserializationConfig config, BeanDescription beanDesc)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findBeanDeserializer(type, config, beanDesc);
            if (deser != null) {
                return (JsonDeserializer<Object>) deser;
            }
        }
        return null;
    }

    protected JsonDeserializer<?> _findCustomArrayDeserializer(ArrayType type,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findArrayDeserializer(type, config,
                    beanDesc, elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected JsonDeserializer<?> _findCustomCollectionDeserializer(CollectionType type,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findCollectionDeserializer(type, config, beanDesc,
                    elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected JsonDeserializer<?> _findCustomCollectionLikeDeserializer(CollectionLikeType type,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findCollectionLikeDeserializer(type, config, beanDesc,
                    elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected JsonDeserializer<?> _findCustomEnumDeserializer(Class<?> type,
            DeserializationConfig config, BeanDescription beanDesc)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findEnumDeserializer(type, config, beanDesc);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected JsonDeserializer<?> _findCustomMapDeserializer(MapType type,
            DeserializationConfig config, BeanDescription beanDesc,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findMapDeserializer(type, config, beanDesc,
                    keyDeserializer, elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected JsonDeserializer<?> _findCustomMapLikeDeserializer(MapLikeType type,
            DeserializationConfig config, BeanDescription beanDesc,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findMapLikeDeserializer(type, config, beanDesc,
                    keyDeserializer, elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    /*
    /**********************************************************
    /* Helper methods, value/content/key type introspection
    /**********************************************************
     */

    /**
     * Helper method called to check if a class or method
     * has annotation that tells which class to use for deserialization; and if
     * so, to instantiate, that deserializer to use.
     * Note that deserializer will NOT yet be contextualized so caller needs to
     * take care to call contextualization appropriately.
     * Returns null if no such annotation found.
     */
    protected JsonDeserializer<Object> findDeserializerFromAnnotation(DeserializationContext ctxt,
            Annotated ann)
        throws JsonMappingException
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr != null) {
            Object deserDef = intr.findDeserializer(ann);
            if (deserDef != null) {
                return ctxt.deserializerInstance(ann, deserDef);
            }
        }
        return null;
    }

    /**
     * Helper method called to check if a class or method
     * has annotation that tells which class to use for deserialization of {@link java.util.Map} keys.
     * Returns null if no such annotation found.
     */
    protected KeyDeserializer findKeyDeserializerFromAnnotation(DeserializationContext ctxt,
            Annotated ann)
            throws JsonMappingException
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr != null) {
            Object deserDef = intr.findKeyDeserializer(ann);
            if (deserDef != null) {
                return ctxt.keyDeserializerInstance(ann, deserDef);
            }
        }
        return null;
    }

    /**
     * @since 2.9
     */
    protected JsonDeserializer<Object> findContentDeserializerFromAnnotation(DeserializationContext ctxt,
            Annotated ann)
        throws JsonMappingException
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr != null) {
            Object deserDef = intr.findContentDeserializer(ann);
            if (deserDef != null) {
                return ctxt.deserializerInstance(ann, deserDef);
            }
        }
        return null;
    }

    /**
     * Helper method used to resolve additional type-related annotation information
     * like type overrides, or handler (serializer, deserializer) overrides,
     * so that from declared field, property or constructor parameter type
     * is used as the base and modified based on annotations, if any.
     *
     * @since 2.8 Combines functionality of <code>modifyTypeByAnnotation</code>
     *     and <code>resolveType</code>
     */
    protected JavaType resolveMemberAndTypeAnnotations(DeserializationContext ctxt,
            AnnotatedMember member, JavaType type)
        throws JsonMappingException
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr == null) {
            return type;
        }

        // First things first: see if we can find annotations on declared
        // type

        if (type.isMapLikeType()) {
            JavaType keyType = type.getKeyType();
            if (keyType != null) {
                Object kdDef = intr.findKeyDeserializer(member);
                KeyDeserializer kd = ctxt.keyDeserializerInstance(member, kdDef);
                if (kd != null) {
                    type = ((MapLikeType) type).withKeyValueHandler(kd);
                    keyType = type.getKeyType(); // just in case it's used below
                }
            }
        }

        if (type.hasContentType()) { // that is, is either container- or reference-type
            Object cdDef = intr.findContentDeserializer(member);
            JsonDeserializer<?> cd = ctxt.deserializerInstance(member, cdDef);
            if (cd != null) {
                type = type.withContentValueHandler(cd);
            }
            TypeDeserializer contentTypeDeser = findPropertyContentTypeDeserializer(
                    ctxt.getConfig(), type, (AnnotatedMember) member);
            if (contentTypeDeser != null) {
                type = type.withContentTypeHandler(contentTypeDeser);
            }
        }
        TypeDeserializer valueTypeDeser = findPropertyTypeDeserializer(ctxt.getConfig(),
                    type, (AnnotatedMember) member);
        if (valueTypeDeser != null) {
            type = type.withTypeHandler(valueTypeDeser);
        }

        // Second part: find actual type-override annotations on member, if any

        // 18-Jun-2016, tatu: Should we re-do checks for annotations on refined
        //   subtypes as well? Code pre-2.8 did not do this, but if we get bug
        //   reports may need to consider
        type = intr.refineDeserializationType(ctxt.getConfig(), member, type);
        return type;
    }

    protected EnumResolver constructEnumResolver(Class<?> enumClass,
            DeserializationConfig config, BeanDescription beanDesc)
    {
        AnnotatedMember jvAcc = beanDesc.findJsonValueAccessor();
        if (jvAcc != null) {
            if (config.canOverrideAccessModifiers()) {
                ClassUtil.checkAndFixAccess(jvAcc.getMember(),
                        config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            }
            return EnumResolver.constructUsingMethod(config, enumClass, jvAcc);
        }
        // 14-Mar-2016, tatu: We used to check `DeserializationFeature.READ_ENUMS_USING_TO_STRING`
        //   here, but that won't do: it must be dynamically changeable...
        return EnumResolver.constructFor(config, enumClass);
    }

    /**
     * Factory method used to resolve an instance of {@link CompactStringObjectMap}
     * with {@link EnumNamingStrategy} applied for the target class.
     *
     * @since 2.15
     */
    protected EnumResolver constructEnumNamingStrategyResolver(DeserializationConfig config, Class<?> enumClass,
            AnnotatedClass annotatedClass) {
        Object namingDef = config.getAnnotationIntrospector().findEnumNamingStrategy(config, annotatedClass);
        EnumNamingStrategy enumNamingStrategy = EnumNamingStrategyFactory.createEnumNamingStrategyInstance(
            namingDef, config.canOverrideAccessModifiers());
        return enumNamingStrategy == null ? null
            : EnumResolver.constructUsingEnumNamingStrategy(config, enumClass, enumNamingStrategy);
    }

    /**
     * @since 2.9
     */
    protected boolean _hasCreatorAnnotation(DeserializationContext ctxt,
            Annotated ann) {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr != null) {
            JsonCreator.Mode mode = intr.findCreatorAnnotation(ctxt.getConfig(), ann);
            return (mode != null) && (mode != JsonCreator.Mode.DISABLED);
        }
        return false;
    }

    /*
    /**********************************************************
    /* Deprecated helper methods
    /**********************************************************
     */

    /**
     * Method called to see if given method has annotations that indicate
     * a more specific type than what the argument specifies.
     *
     * @deprecated Since 2.8; call {@link #resolveMemberAndTypeAnnotations} instead
     */
    @Deprecated
    protected JavaType modifyTypeByAnnotation(DeserializationContext ctxt,
            Annotated a, JavaType type)
        throws JsonMappingException
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr == null) {
            return type;
        }
        return intr.refineDeserializationType(ctxt.getConfig(), a, type);
    }

    /**
     * @deprecated since 2.8 call {@link #resolveMemberAndTypeAnnotations} instead.
     */
    @Deprecated // since 2.8
    protected JavaType resolveType(DeserializationContext ctxt,
            BeanDescription beanDesc, JavaType type, AnnotatedMember member)
        throws JsonMappingException
    {
        return resolveMemberAndTypeAnnotations(ctxt, member, type);
    }

    /**
     * @deprecated since 2.8 call <code>findJsonValueMethod</code> on {@link BeanDescription} instead
     */
    @Deprecated // not used, possibly remove as early as 2.9
    protected AnnotatedMethod _findJsonValueFor(DeserializationConfig config, JavaType enumType)
    {
        if (enumType == null) {
            return null;
        }
        BeanDescription beanDesc = config.introspect(enumType);
        return beanDesc.findJsonValueMethod();
    }

    /**
     * Helper class to contain default mappings for abstract JDK {@link java.util.Collection}
     * and {@link java.util.Map} types. Separated out here to defer cost of creating lookups
     * until mappings are actually needed.
     *
     * @since 2.10
     */
    @SuppressWarnings("rawtypes")
    protected static class ContainerDefaultMappings {
        // We do some defaulting for abstract Collection classes and
        // interfaces, to avoid having to use exact types or annotations in
        // cases where the most common concrete Collection will do.
        final static HashMap<String, Class<? extends Collection>> _collectionFallbacks;
        static {
            HashMap<String, Class<? extends Collection>> fallbacks = new HashMap<>();

            final Class<? extends Collection> DEFAULT_LIST = ArrayList.class;
            final Class<? extends Collection> DEFAULT_SET = HashSet.class;

            fallbacks.put(Collection.class.getName(), DEFAULT_LIST);
            fallbacks.put(List.class.getName(), DEFAULT_LIST);
            fallbacks.put(Set.class.getName(), DEFAULT_SET);
            fallbacks.put(SortedSet.class.getName(), TreeSet.class);
            fallbacks.put(Queue.class.getName(), LinkedList.class);

            // 09-Feb-2019, tatu: How did we miss these? Related in [databind#2251] problem
            fallbacks.put(AbstractList.class.getName(), DEFAULT_LIST);
            fallbacks.put(AbstractSet.class.getName(), DEFAULT_SET);

            // 09-Feb-2019, tatu: And more esoteric types added in JDK6
            fallbacks.put(Deque.class.getName(), LinkedList.class);
            fallbacks.put(NavigableSet.class.getName(), TreeSet.class);

            _collectionFallbacks = fallbacks;
        }

        // We do some defaulting for abstract Map classes and
        // interfaces, to avoid having to use exact types or annotations in
        // cases where the most common concrete Maps will do.
        final static HashMap<String, Class<? extends Map>> _mapFallbacks;
        static {
            HashMap<String, Class<? extends Map>> fallbacks = new HashMap<>();

            final Class<? extends Map> DEFAULT_MAP = LinkedHashMap.class;
            fallbacks.put(Map.class.getName(), DEFAULT_MAP);
            fallbacks.put(AbstractMap.class.getName(), DEFAULT_MAP);
            fallbacks.put(ConcurrentMap.class.getName(), ConcurrentHashMap.class);
            fallbacks.put(SortedMap.class.getName(), TreeMap.class);

            fallbacks.put(java.util.NavigableMap.class.getName(), TreeMap.class);
            fallbacks.put(java.util.concurrent.ConcurrentNavigableMap.class.getName(),
                    java.util.concurrent.ConcurrentSkipListMap.class);

            _mapFallbacks = fallbacks;
        }

        public static Class<?> findCollectionFallback(JavaType type) {
            return _collectionFallbacks.get(type.getRawClass().getName());
        }

        public static Class<?> findMapFallback(JavaType type) {
            return _mapFallbacks.get(type.getRawClass().getName());
        }
    }

    /**
     * Helper class to contain largish number of parameters that need to be passed
     * during Creator introspection.
     *
     * @since 2.12
     */
    protected static class CreatorCollectionState {
        public final DeserializationContext context;
        public final BeanDescription beanDesc;
        public final VisibilityChecker<?> vchecker;
        public final CreatorCollector creators;
        public final Map<AnnotatedWithParams,BeanPropertyDefinition[]> creatorParams;

        private List<CreatorCandidate> _implicitFactoryCandidates;
        private int _explicitFactoryCount;

        private List<CreatorCandidate> _implicitConstructorCandidates;
        private int _explicitConstructorCount;

        public CreatorCollectionState(DeserializationContext ctxt, BeanDescription bd,
                VisibilityChecker<?> vc,
                CreatorCollector cc,
                Map<AnnotatedWithParams,BeanPropertyDefinition[]> cp)
        {
            context = ctxt;
            beanDesc = bd;
            vchecker = vc;
            creators = cc;
            creatorParams = cp;
        }

        public AnnotationIntrospector annotationIntrospector() {
            return context.getAnnotationIntrospector();
        }

        // // // Factory creator candidate info

        public void addImplicitFactoryCandidate(CreatorCandidate cc) {
            if (_implicitFactoryCandidates == null) {
                _implicitFactoryCandidates = new LinkedList<>();
            }
            _implicitFactoryCandidates.add(cc);
        }

        public void increaseExplicitFactoryCount() {
            ++_explicitFactoryCount;
        }

        public boolean hasExplicitFactories() {
            return _explicitFactoryCount > 0;
        }

        public boolean hasImplicitFactoryCandidates() {
            return _implicitFactoryCandidates != null;
        }

        public List<CreatorCandidate> implicitFactoryCandidates() {
            return _implicitFactoryCandidates;
        }

        // // // Constructor creator candidate info

        public void addImplicitConstructorCandidate(CreatorCandidate cc) {
            if (_implicitConstructorCandidates == null) {
                _implicitConstructorCandidates = new LinkedList<>();
            }
            _implicitConstructorCandidates.add(cc);
        }

        public void increaseExplicitConstructorCount() {
            ++_explicitConstructorCount;
        }

        public boolean hasExplicitConstructors() {
            return _explicitConstructorCount > 0;
        }

        public boolean hasImplicitConstructorCandidates() {
            return _implicitConstructorCandidates != null;
        }

        public List<CreatorCandidate> implicitConstructorCandidates() {
            return _implicitConstructorCandidates;
        }
    }
}
