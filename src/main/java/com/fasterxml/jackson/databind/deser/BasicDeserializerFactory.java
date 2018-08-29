package com.fasterxml.jackson.databind.deser;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;

import com.fasterxml.jackson.core.JsonLocation;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.deser.impl.CreatorCandidate;
import com.fasterxml.jackson.databind.deser.impl.CreatorCollector;
import com.fasterxml.jackson.databind.deser.impl.JavaUtilCollectionsDeserializers;
import com.fasterxml.jackson.databind.deser.std.*;
import com.fasterxml.jackson.databind.ext.OptionalHandlerFactory;
import com.fasterxml.jackson.databind.ext.jdk8.Jdk8OptionalDeserializer;
import com.fasterxml.jackson.databind.ext.jdk8.OptionalDoubleDeserializer;
import com.fasterxml.jackson.databind.ext.jdk8.OptionalIntDeserializer;
import com.fasterxml.jackson.databind.ext.jdk8.OptionalLongDeserializer;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
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
    
    /* We do some defaulting for abstract Map classes and
     * interfaces, to avoid having to use exact types or annotations in
     * cases where the most common concrete Maps will do.
     */
    @SuppressWarnings("rawtypes")
    final static HashMap<String, Class<? extends Map>> _mapFallbacks =
        new HashMap<String, Class<? extends Map>>();
    static {
        _mapFallbacks.put(Map.class.getName(), LinkedHashMap.class);
        _mapFallbacks.put(ConcurrentMap.class.getName(), ConcurrentHashMap.class);
        _mapFallbacks.put(SortedMap.class.getName(), TreeMap.class);

        _mapFallbacks.put(java.util.NavigableMap.class.getName(), TreeMap.class);
        _mapFallbacks.put(java.util.concurrent.ConcurrentNavigableMap.class.getName(),
                java.util.concurrent.ConcurrentSkipListMap.class);
    }

    /* We do some defaulting for abstract Collection classes and
     * interfaces, to avoid having to use exact types or annotations in
     * cases where the most common concrete Collection will do.
     */
    @SuppressWarnings("rawtypes")
    final static HashMap<String, Class<? extends Collection>> _collectionFallbacks =
        new HashMap<String, Class<? extends Collection>>();
    static {
        _collectionFallbacks.put(Collection.class.getName(), ArrayList.class);
        _collectionFallbacks.put(List.class.getName(), ArrayList.class);
        _collectionFallbacks.put(Set.class.getName(), HashSet.class);
        _collectionFallbacks.put(SortedSet.class.getName(), TreeSet.class);
        _collectionFallbacks.put(Queue.class.getName(), LinkedList.class);

        // then JDK 1.6 types:
        // 17-May-2013, tatu: [databind#216] Should be fine to use straight Class references EXCEPT
        //   that some god-forsaken platforms (... looking at you, Android) do not
        //   include these. So, use "soft" references...
        _collectionFallbacks.put("java.util.Deque", LinkedList.class);
        _collectionFallbacks.put("java.util.NavigableSet", TreeSet.class);
    }

    /*
    /**********************************************************************
    /* Config
    /**********************************************************************
     */

    /**
     * Configuration settings for this factory; immutable instance (just like this
     * factory), new version created via copy-constructor (fluent-style)
     */
    protected final DeserializerFactoryConfig _factoryConfig;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
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
    /**********************************************************************
    /* Configuration handling: fluent factories
    /**********************************************************************
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
     * {@link ValueInstantiators}.
     */
    @Override
    public final DeserializerFactory withValueInstantiators(ValueInstantiators instantiators) {
        return withConfig(_factoryConfig.withValueInstantiators(instantiators));
    }

    /*
    /**********************************************************************
    /* JsonDeserializerFactory impl (partial): ValueInstantiators
    /**********************************************************************
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
        Object instDef = ctxt.getAnnotationIntrospector().findValueInstantiator(ctxt.getConfig(), ac);
        if (instDef != null) {
            instantiator = _valueInstantiatorInstance(config, ac, instDef);
        }
        if (instantiator == null) {
            // Second: see if some of standard Jackson/JDK types might provide value
            // instantiators.
            instantiator = _findStdValueInstantiator(config, beanDesc);
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
        return instantiator;
    }

    private ValueInstantiator _findStdValueInstantiator(DeserializationConfig config,
            BeanDescription beanDesc)
        throws JsonMappingException
    {
        Class<?> raw = beanDesc.getBeanClass();
        if (raw == JsonLocation.class) {
            return new JsonLocationInstantiator();
        }
        // [databind#1868]: empty List/Set/Map
        if (Collection.class.isAssignableFrom(raw)) {
            if (Collections.EMPTY_SET.getClass() == raw) {
                return new ConstantValueInstantiator(Collections.EMPTY_SET);
            }
            if (Collections.EMPTY_LIST.getClass() == raw) {
                return new ConstantValueInstantiator(Collections.EMPTY_LIST);
            }
        } else if (Map.class.isAssignableFrom(raw)) {
            if (Collections.EMPTY_MAP.getClass() == raw) {
                return new ConstantValueInstantiator(Collections.EMPTY_MAP);
            }
        }
        return null;
    }

    /**
     * Method that will construct standard default {@link ValueInstantiator}
     * using annotations (like @JsonCreator) and visibility rules
     */
    protected ValueInstantiator _constructDefaultValueInstantiator(DeserializationContext ctxt,
            BeanDescription beanDesc)
        throws JsonMappingException
    {
        CreatorCollector creators = new CreatorCollector(beanDesc, ctxt.getConfig());
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        
        // need to construct suitable visibility checker:
        final DeserializationConfig config = ctxt.getConfig();
        VisibilityChecker vchecker = config.getDefaultVisibilityChecker(beanDesc.getBeanClass(),
                beanDesc.getClassInfo());

        /* 24-Sep-2014, tatu: Tricky part first; need to merge resolved property information
         *  (which has creator parameters sprinkled around) with actual creator
         *  declarations (which are needed to access creator annotation, amongst other things).
         *  Easiest to combine that info first, then pass it to remaining processing.
         */
        /* 15-Mar-2015, tatu: Alas, this won't help with constructors that only have implicit
         *   names. Those will need to be resolved later on.
         */
        Map<AnnotatedWithParams,BeanPropertyDefinition[]> creatorDefs = _findCreatorsFromProperties(ctxt,
                beanDesc);
        // Important: first add factory methods; then constructors, so
        // latter can override former!
        _addFactoryCreators(ctxt, beanDesc, vchecker, intr, creators, creatorDefs);
        // constructors only usable on concrete types:
        if (beanDesc.getType().isConcrete()) {
            _addConstructorCreators(ctxt, beanDesc, vchecker, intr, creators, creatorDefs);
        }
        return creators.constructValueInstantiator(ctxt);
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
    /* Creator introspection, main methods
    /**********************************************************************
     */

    protected void _addConstructorCreators(DeserializationContext ctxt,
            BeanDescription beanDesc, VisibilityChecker vchecker,
            AnnotationIntrospector intr, CreatorCollector creators,
            Map<AnnotatedWithParams,BeanPropertyDefinition[]> creatorParams)
        throws JsonMappingException
    {
        // 25-Jan-2017, tatu: As per [databind#1501], [databind#1502], [databind#1503], best
        //     for now to skip attempts at using anything but no-args constructor (see
        //     `InnerClassProperty` construction for that)
        final boolean isNonStaticInnerClass = beanDesc.isNonStaticInnerClass();
        if (isNonStaticInnerClass) {
            // TODO: look for `@JsonCreator` annotated ones, throw explicit exception?
            return;
        }

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
        List<CreatorCandidate> nonAnnotated = new LinkedList<>();
        int explCount = 0;
        for (AnnotatedConstructor ctor : beanDesc.getConstructors()) {
            JsonCreator.Mode creatorMode = intr.findCreatorAnnotation(ctxt.getConfig(), ctor);
            if (Mode.DISABLED == creatorMode) {
                continue;
            }
            if (creatorMode == null) {
                // let's check Visibility here, to avoid further processing for non-visible?
                boolean visible = (ctor.getParameterCount() == 1)
                        ? vchecker.isScalarConstructorVisible(ctor)
                        : vchecker.isCreatorVisible(ctor);
                if (visible) {
                    nonAnnotated.add(CreatorCandidate.construct(intr, ctor, creatorParams.get(ctor)));
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
                        CreatorCandidate.construct(intr, ctor, creatorParams.get(ctor)));
                break;
            }
            ++explCount;
        }
        // And only if and when those handled, consider potentially visible ones
        if (explCount > 0) { // TODO: split method into two since we could have expl factories
            return;
        }
        List<AnnotatedWithParams> implicitCtors = null;

        for (CreatorCandidate candidate : nonAnnotated) {
            final int argCount = candidate.paramCount();
            final AnnotatedWithParams ctor = candidate.creator();
            // some single-arg factory methods (String, number) are auto-detected
            if (argCount == 1) {
                BeanPropertyDefinition propDef = candidate.propertyDef(0);
                boolean useProps = _checkIfCreatorPropertyBased(intr, ctor, propDef);
                if (useProps) {
                    SettableBeanProperty[] properties = new SettableBeanProperty[1];
                    PropertyName name = candidate.paramName(0);
                    properties[0] = constructCreatorProperty(ctxt, beanDesc, name, 0,
                            candidate.parameter(0), candidate.injection(0));
                    creators.addPropertyCreator(ctor, false, properties);
                } else {
                    /*boolean added = */ _handleSingleArgumentCreator(creators,
                            ctor, false, true); // not-annotated, yes, visible
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
                JacksonInject.Value injectId = intr.findInjectableValue(param);
                final PropertyName name = (propDef == null) ? null : propDef.getFullName();

                if (propDef != null && propDef.isExplicitlyNamed()) {
                    ++explicitNameCount;
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectId);
                    continue;
                }
                if (injectId != null) {
                    ++injectCount;
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectId);
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

    protected void _addFactoryCreators(DeserializationContext ctxt,
            BeanDescription beanDesc, VisibilityChecker vchecker,
            AnnotationIntrospector intr, CreatorCollector creators,
            Map<AnnotatedWithParams,BeanPropertyDefinition[]> creatorParams)
        throws JsonMappingException
    {
        List<CreatorCandidate> nonAnnotated = new LinkedList<>();
        int explCount = 0;

        // 21-Sep-2017, tatu: First let's handle explicitly annotated ones
        for (AnnotatedMethod factory : beanDesc.getFactoryMethods()) {
            JsonCreator.Mode creatorMode = intr.findCreatorAnnotation(ctxt.getConfig(), factory);
            final int argCount = factory.getParameterCount();
            if (creatorMode == null) {
                // Only potentially accept 1-argument factory methods
                if ((argCount == 1) && vchecker.isCreatorVisible(factory)) {
                    nonAnnotated.add(CreatorCandidate.construct(intr, factory, null));
                }
                continue;
            }
            if (creatorMode == Mode.DISABLED) {
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
                        CreatorCandidate.construct(intr, factory, creatorParams.get(factory)));
                break;
            }
            ++explCount;
        }
        // And only if and when those handled, consider potentially visible ones
        if (explCount > 0) { // TODO: split method into two since we could have expl factories
            return;
        }
        // And then implicitly found
        for (CreatorCandidate candidate : nonAnnotated) {
            final int argCount = candidate.paramCount();
            AnnotatedWithParams factory = candidate.creator();
            final BeanPropertyDefinition[] propDefs = creatorParams.get(factory);
            // some single-arg factory methods (String, number) are auto-detected
            if (argCount != 1) {
                continue; // 2 and more args? Must be explicit, handled earlier
            }
            BeanPropertyDefinition argDef = candidate.propertyDef(0);
            boolean useProps = _checkIfCreatorPropertyBased(intr, factory, argDef);
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
                    // [712] secondary: all but one injectable, one un-annotated (un-named)
                    creators.addDelegatingCreator(factory, false, properties, 0);
                } else { // otherwise, epic fail
                    ctxt.reportBadTypeDefinition(beanDesc,
"Argument #%d of factory method %s has no property name annotation; must have name when multiple-parameter constructor annotated as Creator",
                    nonAnnotatedParam.getIndex(), factory);
                }
            }
        }
    }

    /*
    /**********************************************************************
    /* Creator introspection, explicitly annotated creators
    /**********************************************************************
     */

    /**
     * Helper method called when there is the explicit "is-creator" with mode of "delegating"
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
     * Helper method called when there is the explicit "is-creator" with mode of "properties-based"
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
                // Must be injectable or have name; without either won't work
                if ((name == null) && (injectId == null)) {
                    ctxt.reportBadTypeDefinition(beanDesc,
"Argument #%d has no property name, is not Injectable: can not use as Creator %s", i, candidate);
                }
            }
            properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectId);
        }
        creators.addPropertyCreator(candidate.creator(), true, properties);
    }

    /**
     * Helper method called when there is the explicit "is-creator", but no mode declaration.
     */
    protected void _addExplicitAnyCreator(DeserializationContext ctxt,
            BeanDescription beanDesc, CreatorCollector creators,
            CreatorCandidate candidate)
        throws JsonMappingException
    {
        // Looks like there's bit of magic regarding 1-parameter creators; others simpler:
        if (1 != candidate.paramCount()) {
            // Ok: for delegates, we want one and exactly one parameter without
            // injection AND without name
            int oneNotInjected = candidate.findOnlyParamWithoutInjection();
            if (oneNotInjected >= 0) {
                // getting close; but most not have name
                if (candidate.paramName(oneNotInjected) == null) {
                    _addExplicitDelegatingCreator(ctxt, beanDesc, creators, candidate);
                    return;
                }
            }
            _addExplicitPropertyCreator(ctxt, beanDesc, creators, candidate);
            return;
        }
        AnnotatedParameter param = candidate.parameter(0);
        JacksonInject.Value injectId = candidate.injection(0);
        PropertyName paramName = candidate.explicitParamName(0);
        BeanPropertyDefinition paramDef = candidate.propertyDef(0);

        // If there's injection or explicit name, should be properties-based
        boolean useProps = (paramName != null) || (injectId != null);
        if (!useProps && (paramDef != null)) {
            // One more thing: if implicit name matches property with a getter
            // or field, we'll consider it property-based as well

            // 25-May-2018, tatu: as per [databind#2051], looks like we have to get
            //    not implicit name, but name with possible strategy-based-rename
//            paramName = candidate.findImplicitParamName(0);
            paramName = candidate.paramName(0);
            useProps = (paramName != null) && paramDef.couldSerialize();
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
        if (paramDef != null) {
            ((POJOPropertyBuilder) paramDef).removeConstructors();
        }
    }

    /*
    /**********************************************************************
    /* Creator introspection, helper methods
    /**********************************************************************
     */

    private boolean _checkIfCreatorPropertyBased(AnnotationIntrospector intr,
            AnnotatedWithParams creator, BeanPropertyDefinition propDef)
    {
        // If explicit name, or inject id, property-based
        if (((propDef != null) && propDef.isExplicitlyNamed())
                || (intr.findInjectableValue(creator.getParameter(0)) != null)) {
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
        }
        // in absence of everything else, default to delegating
        return false;
    }

    private void _checkImplicitlyNamedConstructors(DeserializationContext ctxt,
            BeanDescription beanDesc, VisibilityChecker vchecker,
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
            // 21-Sep-2017, tatu: Note that "scalar constructors" are always delegating,
            //    so use regular creator visibility here.
//            if (!_constructorVisible(vchecker, ctor)) {
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
        // Delegating Creator ok iff it has @JsonCreator (etc)
        if (isCreator) {
            creators.addDelegatingCreator(ctor, isCreator, null, 0);
            return true;
        }
        return false;
    }

    // 01-Dec-2016, tatu: As per [databind#265] we cannot yet support passing
    //   of unwrapped values through creator properties, so fail fast
    protected void _reportUnwrappedCreatorProperty(DeserializationContext ctxt,
            BeanDescription beanDesc, AnnotatedParameter param)
        throws JsonMappingException
    {
        ctxt.reportBadDefinition(beanDesc.getType(), String.format(
                "Cannot define Creator parameter %d as `@JsonUnwrapped`: combination not yet supported",
                param.getIndex()));
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
        final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        PropertyMetadata metadata;
        {
            if (intr == null) {
                metadata = PropertyMetadata.STD_REQUIRED_OR_OPTIONAL;
            } else {
                Boolean b = intr.hasRequiredMarker(param);
                String desc = intr.findPropertyDescription(param);
                Integer idx = intr.findPropertyIndex(param);
                String def = intr.findPropertyDefaultValue(param);
                metadata = PropertyMetadata.construct(b, desc, idx, def);
            }
        }
        JavaType type = resolveMemberAndTypeAnnotations(ctxt, param, param.getType());
        BeanProperty.Std property = new BeanProperty.Std(name, type,
                intr.findWrapperName(param), param, metadata);
        // Type deserializer: either comes from property (and already resolved)
        TypeDeserializer typeDeser = (TypeDeserializer) type.getTypeHandler();
        // or if not, based on type being referenced:
        if (typeDeser == null) {
            typeDeser = ctxt.findTypeDeserializer(type);
        }
        // Note: contextualization of typeDeser _should_ occur in constructor of CreatorProperty
        // so it is not called directly here

        Object injectableValueId = (injectable == null) ? null : injectable.getId();
        
        SettableBeanProperty prop = new CreatorProperty(name, type, property.getWrapperName(),
                typeDeser, beanDesc.getClassAnnotations(), param, index, injectableValueId,
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
        if (param != null && intr != null) {
            PropertyName name = intr.findNameForDeserialization(param);
            if (name != null) {
                return name;
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

    /*
    protected PropertyName _findImplicitParamName(AnnotatedParameter param, AnnotationIntrospector intr)
    {
        String str = intr.findImplicitPropertyName(param);
        if (str != null && !str.isEmpty()) {
            return PropertyName.construct(str);
        }
        return null;
    }

    protected boolean _checkIfCreatorPropertyBased(AnnotationIntrospector intr,
            AnnotatedWithParams creator, BeanPropertyDefinition propDef)
    {
        // If explicit name, or inject id, property-based
        if (((propDef != null) && propDef.isExplicitlyNamed())
                || (intr.findInjectableValue(creator.getParameter(0)) != null)) {
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
        }
        // in absence of everything else, default to delegating
        return false;
    }
*/

    /*
    /**********************************************************************
    /* JsonDeserializerFactory impl: array deserializers
    /**********************************************************************
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
            elemTypeDeser = ctxt.findTypeDeserializer(elemType);
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
        // and then new with 2.2: ability to post-process it too (Issue#120)
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deser = mod.modifyArrayDeserializer(config, type, beanDesc, deser);
            }
        }
        return deser;
    }

    /*
    /**********************************************************************
    /* JsonDeserializerFactory impl: Collection(-like) deserializers
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
            contentTypeDeser = ctxt.findTypeDeserializer(contentType);
        }
        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> deser = _findCustomCollectionDeserializer(type,
                config, beanDesc, contentTypeDeser, contentDeser);
        if (deser == null) {
            Class<?> collectionClass = type.getRawClass();
            if (contentDeser == null) { // not defined by annotation
                // [databind#1853]: Map `Set<ENUM>` to `EnumSet<ENUM>`
                if (contentType.isEnumType() && (collectionClass == Set.class)) {
                    collectionClass = EnumSet.class;
                    type = (CollectionType) config.getTypeFactory().constructSpecializedType(type, collectionClass);
                }
                // One special type: EnumSet:
                if (EnumSet.class.isAssignableFrom(collectionClass)) {
                    // 25-Jan-2018, tatu: shouldn't we pass `contentDeser`?
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
                    beanDesc = ctxt.introspectForCreation(type);
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
        Class<?> collectionClass = type.getRawClass();
        collectionClass = _collectionFallbacks.get(collectionClass.getName());
        if (collectionClass == null) {
            return null;
        }
        return (CollectionType) config.constructSpecializedType(type, collectionClass);
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
            contentTypeDeser = ctxt.findTypeDeserializer(contentType);
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
    /**********************************************************************
    /* JsonDeserializerFactory impl: Map(-like) deserializers
    /**********************************************************************
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
            contentTypeDeser = ctxt.findTypeDeserializer(contentType);
        }

        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> deser = _findCustomMapDeserializer(type, config, beanDesc,
                keyDes, contentTypeDeser, contentDeser);

        if (deser == null) {
            // Value handling is identical for all, but EnumMap requires special handling for keys
            Class<?> mapClass = type.getRawClass();
            // [databind#1853]: Map `Map<ENUM,x>` to `EnumMap<ENUM,x>`
            if ((mapClass == Map.class) && keyType.isEnumType()) {
                mapClass = EnumMap.class;
                type = (MapType) config.getTypeFactory().constructSpecializedType(type, mapClass);
//                type = (MapType) config.getTypeFactory().constructMapType(mapClass, keyType, contentType);
            }
            if (EnumMap.class.isAssignableFrom(mapClass)) {
                ValueInstantiator inst;

                // 06-Mar-2017, tatu: Should only need to check ValueInstantiator for
                //    custom sub-classes, see [databind#1544]
                if (mapClass == EnumMap.class) {
                    inst = null;
                } else {
                    inst = findValueInstantiator(ctxt, beanDesc);
                }
                Class<?> kt = keyType.getRawClass();
                if (kt == null || !kt.isEnum()) {
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
                    @SuppressWarnings("rawtypes")
                    Class<? extends Map> fallback = _mapFallbacks.get(mapClass.getName());
                    if (fallback != null) {
                        mapClass = fallback;
                        type = (MapType) config.constructSpecializedType(type, mapClass);
                        // But if so, also need to re-check creators...
                        beanDesc = ctxt.introspectForCreation(type);
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
            contentTypeDeser = ctxt.findTypeDeserializer(contentType);
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
    /**********************************************************************
    /* JsonDeserializerFactory impl: other types
    /**********************************************************************
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
                    if (returnType.isAssignableFrom(enumClass)) {
                        deser = EnumDeserializer.deserializerForCreator(config, enumClass, factory, valueInstantiator, creatorProps);
                        break;
                    }
                }
            }
           
            // Need to consider @JsonValue if one found
            if (deser == null) {
                deser = new EnumDeserializer(constructEnumResolver(enumClass,
                        config, beanDesc.findJsonValueAccessor()),
                        config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
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
            contentTypeDeser = ctxt.findTypeDeserializer(contentType);
        }
        JsonDeserializer<?> deser = _findCustomReferenceDeserializer(type, config, beanDesc,
                contentTypeDeser, contentDeser);

        if (deser == null) {
            // 19-Sep-2017, tatu: Java 8 Optional directly supported in 3.x:
            if (type.isTypeOrSubTypeOf(Optional.class)) {
                // Not sure this can really work but let's try:
                ValueInstantiator inst = type.hasRawClass(Optional.class) ? null
                        : findValueInstantiator(ctxt, beanDesc);
                return new Jdk8OptionalDeserializer(type, inst, contentTypeDeser, contentDeser);
            }
            if (type.isTypeOrSubTypeOf(AtomicReference.class)) {
                // 23-Oct-2016, tatu: Note that subtypes are probably not supportable
                //    without either forcing merging (to avoid having to create instance)
                //    or something else...
                ValueInstantiator inst = type.hasRawClass(AtomicReference.class) ? null
                        : findValueInstantiator(ctxt, beanDesc);
                return new AtomicReferenceDeserializer(type, inst, contentTypeDeser, contentDeser);
            }
            if (type.hasRawClass(OptionalInt.class)) {
                return new OptionalIntDeserializer();
            }
            if (type.hasRawClass(OptionalLong.class)) {
                return new OptionalLongDeserializer();
            }
            if (type.hasRawClass(OptionalDouble.class)) {
                return new OptionalDoubleDeserializer();
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
    /**********************************************************************
    /* JsonDeserializerFactory impl (partial): type deserializers
    /**********************************************************************
     */

    /**
     * Overridable method called after checking all other types.
     */
    protected JsonDeserializer<?> findOptionalStdDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        return OptionalHandlerFactory.instance.findDeserializer(type, ctxt.getConfig(), beanDesc);
    }
    
    /*
    /**********************************************************************
    /* JsonDeserializerFactory impl (partial): key deserializers
    /**********************************************************************
     */
    
    @Override
    public KeyDeserializer createKeyDeserializer(DeserializationContext ctxt,
            JavaType type)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        KeyDeserializer deser = null;
        if (_factoryConfig.hasKeyDeserializers()) {
            BeanDescription beanDesc = config.introspectClassAnnotations(type.getRawClass());
            for (KeyDeserializers d  : _factoryConfig.keyDeserializers()) {
                deser = d.findKeyDeserializer(type, config, beanDesc);
                if (deser != null) {
                    break;
                }
            }
        }
        // the only non-standard thing is this:
        if (deser == null) {
            if (type.isEnumType()) {
                deser = _createEnumKeyDeserializer(ctxt, type);
            } else {
                deser = StdKeyDeserializers.findStringBasedKeyDeserializer(ctxt, type);
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

        BeanDescription beanDesc = ctxt.introspect(type);
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
        EnumResolver enumRes = constructEnumResolver(enumClass, config, beanDesc.findJsonValueAccessor());
        // May have @JsonCreator for static factory method:
        for (AnnotatedMethod factory : beanDesc.getFactoryMethods()) {
            if (_hasCreatorAnnotation(ctxt, factory)) {
                int argCount = factory.getParameterCount();
                if (argCount == 1) {
                    Class<?> returnType = factory.getRawReturnType();
                    // usually should be class, but may be just plain Enum<?> (for Enum.valueOf()?)
                    if (returnType.isAssignableFrom(enumClass)) {
                        // note: mostly copied from 'EnumDeserializer.deserializerForCreator(...)'
                        if (factory.getRawParameterType(0) != String.class) {
                            throw new IllegalArgumentException("Parameter #0 type for factory method ("+factory+") not suitable, must be java.lang.String");
                        }
                        if (config.canOverrideAccessModifiers()) {
                            ClassUtil.checkAndFixAccess(factory.getMember(),
                                    ctxt.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
                        }
                        return StdKeyDeserializers.constructEnumKeyDeserializer(enumRes, factory);
                    }
                }
                throw new IllegalArgumentException("Unsuitable method ("+factory+") decorated with @JsonCreator (for Enum type "
                        +enumClass.getName()+")");
            }
        }
        // Also, need to consider @JsonValue, if one found
        return StdKeyDeserializers.constructEnumKeyDeserializer(enumRes);
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    /**
     * Helper method called to find one of default serializers for "well-known"
     * platform types: JDK-provided types, and small number of public Jackson
     * API types.
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
            
            if (ctxt.getConfig().hasAbstractTypeResolvers()) {
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
                vts = ctxt.findTypeDeserializer(vt);
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
        return StdJdkDeserializers.find(rawType, clsName);
    }

    private JavaType _findRemappedType(DeserializationConfig config, Class<?> rawType)
            throws JsonMappingException
    {
        JavaType type = config.mapAbstractType(config.constructType(rawType));
        return (type == null || type.hasRawClass(rawType)) ? null : type;
    }

    /*
    /**********************************************************************
    /* Helper methods, finding custom deserializers
    /**********************************************************************
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
    /**********************************************************************
    /* Helper methods, value/content/key type introspection
    /**********************************************************************
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
            Object deserDef = intr.findDeserializer(ctxt.getConfig(), ann);
            if (deserDef != null) {
                return ctxt.deserializerInstance(ann, deserDef);
            }
        }
        return null;
    }

    /**
     * Helper method called to check if a class or method
     * has annotation that tells which class to use for deserialization.
     * Returns null if no such annotation found.
     */
    protected KeyDeserializer findKeyDeserializerFromAnnotation(DeserializationContext ctxt,
            Annotated ann)
            throws JsonMappingException
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr != null) {
            Object deserDef = intr.findKeyDeserializer(ctxt.getConfig(), ann);
            if (deserDef != null) {
                return ctxt.keyDeserializerInstance(ann, deserDef);
            }
        }
        return null;
    }

    protected JsonDeserializer<Object> findContentDeserializerFromAnnotation(DeserializationContext ctxt,
            Annotated ann)
        throws JsonMappingException
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr != null) {
            Object deserDef = intr.findContentDeserializer(ctxt.getConfig(), ann);
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
                Object kdDef = intr.findKeyDeserializer(ctxt.getConfig(), member);
                KeyDeserializer kd = ctxt.keyDeserializerInstance(member, kdDef);
                if (kd != null) {
                    type = ((MapLikeType) type).withKeyValueHandler(kd);
                    keyType = type.getKeyType(); // just in case it's used below
                }
            }
        }

        if (type.hasContentType()) { // that is, is either container- or reference-type
            Object cdDef = intr.findContentDeserializer(ctxt.getConfig(), member);
            JsonDeserializer<?> cd = ctxt.deserializerInstance(member, cdDef);
            if (cd != null) {
                type = type.withContentValueHandler(cd);
            }
            TypeDeserializer contentTypeDeser = ctxt.findPropertyContentTypeDeserializer(type,
                    (AnnotatedMember) member);            	
            if (contentTypeDeser != null) {
                type = type.withContentTypeHandler(contentTypeDeser);
            }
        }
        TypeDeserializer valueTypeDeser = ctxt.findPropertyTypeDeserializer(type, (AnnotatedMember) member);
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
            DeserializationConfig config, AnnotatedMember jsonValueAccessor)
    {
        if (jsonValueAccessor != null) {
            if (config.canOverrideAccessModifiers()) {
                ClassUtil.checkAndFixAccess(jsonValueAccessor.getMember(),
                        config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            }
            return EnumResolver.constructUnsafeUsingMethod(enumClass,
                    jsonValueAccessor, config.getAnnotationIntrospector());
        }
        // 14-Mar-2016, tatu: We used to check `DeserializationFeature.READ_ENUMS_USING_TO_STRING`
        //   here, but that won't do: it must be dynamically changeable...
        return EnumResolver.constructUnsafe(enumClass, config.getAnnotationIntrospector());
    }

    protected boolean _hasCreatorAnnotation(DeserializationContext ctxt,
            Annotated ann) {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr != null) {
            JsonCreator.Mode mode = intr.findCreatorAnnotation(ctxt.getConfig(), ann);
            return (mode != null) && (mode != JsonCreator.Mode.DISABLED); 
        }
        return false;
    }
}
