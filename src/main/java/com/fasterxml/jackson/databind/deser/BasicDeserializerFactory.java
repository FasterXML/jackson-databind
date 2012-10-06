package com.fasterxml.jackson.databind.deser;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.NoClass;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.deser.impl.CreatorCollector;
import com.fasterxml.jackson.databind.deser.std.*;
import com.fasterxml.jackson.databind.ext.OptionalHandlerFactory;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.*;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.EnumResolver;

/**
 * Abstract factory base class that can provide deserializers for standard
 * JDK classes, including collection classes and simple heuristics for
 * "upcasting" commmon collection interface types
 * (such as {@link java.util.Collection}).
 *<p>
 * Since all simple deserializers are eagerly instantiated, and there is
 * no additional introspection or customizability of these types,
 * this factory is stateless.
 */
public abstract class BasicDeserializerFactory
    extends DeserializerFactory
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1;

    /**
     * We will pre-create serializers for common non-structured
     * (that is things other than Collection, Map or array)
     * types. These need not go through factory.
     */
    final protected static HashMap<ClassKey, JsonDeserializer<Object>> _simpleDeserializers
        = new HashMap<ClassKey, JsonDeserializer<Object>>();

    /**
     * Also special array deserializers for primitive array types.
     */
    final protected static HashMap<JavaType,JsonDeserializer<Object>> _arrayDeserializers
        = PrimitiveArrayDeserializers.getAll();

    /**
     * Set of available key deserializers is currently limited
     * to standard types; and all known instances are storing in this map.
     */
    final protected static HashMap<JavaType, KeyDeserializer> _keyDeserializers = StdKeyDeserializers.constructAll();

    static {
        // First, add the fall-back "untyped" deserializer:
        _add(_simpleDeserializers, Object.class, new UntypedObjectDeserializer());
    
        // Then String and String-like converters:
        StdDeserializer<?> strDeser = new StringDeserializer();
        _add(_simpleDeserializers, String.class, strDeser);
        _add(_simpleDeserializers, CharSequence.class, strDeser);
    
        // Primitives/wrappers, other Numbers:
        _add(_simpleDeserializers, NumberDeserializers.all());
        // Date/time types
        _add(_simpleDeserializers, DateDeserializers.all());
        // other JDK types
        _add(_simpleDeserializers, JdkDeserializers.all());
        // and a few Jackson types as well:
        _add(_simpleDeserializers, JacksonDeserializers.all());
    }

    private static void _add(Map<ClassKey, JsonDeserializer<Object>> desers,
            StdDeserializer<?>[] serializers) {
        for (StdDeserializer<?> ser : serializers) {
            _add(desers, ser.getValueClass(), ser);
        }
    }

    private static void _add(Map<ClassKey, JsonDeserializer<Object>> desers,
            Class<?> valueClass, StdDeserializer<?> stdDeser)
    {
        @SuppressWarnings("unchecked")
        JsonDeserializer<Object> deser = (JsonDeserializer<Object>) stdDeser;
        desers.put(new ClassKey(valueClass), deser);
    }
    
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

        /* 11-Jan-2009, tatu: Let's see if we can still add support for
         *    JDK 1.6 interfaces, even if we run on 1.5. Just need to be
         *    more careful with typos, since compiler won't notice any
         *    problems...
         */
        _mapFallbacks.put("java.util.NavigableMap", TreeMap.class);
        try {
            Class<?> key = Class.forName("java.util.concurrent.ConcurrentNavigableMap");
            Class<?> value = Class.forName("java.util.concurrent.ConcurrentSkipListMap");
            @SuppressWarnings("unchecked")
                Class<? extends Map<?,?>> mapValue = (Class<? extends Map<?,?>>) value;
            _mapFallbacks.put(key.getName(), mapValue);
        } catch (ClassNotFoundException cnfe) { // occurs on 1.5
        } catch (SecurityException se) { // might occur in applets, see stackoverflow.com/questions/12345068
        }
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

        /* 11-Jan-2009, tatu: Let's see if we can still add support for
         *    JDK 1.6 interfaces, even if we run on 1.5. Just need to be
         *    more careful with typos, since compiler won't notice any
         *    problems...
         */
        _collectionFallbacks.put("java.util.Deque", LinkedList.class);
        _collectionFallbacks.put("java.util.NavigableSet", TreeSet.class);
    }

    /**
     * To support external/optional deserializers, we'll use a helper class
     */
    protected OptionalHandlerFactory optionalHandlers = OptionalHandlerFactory.instance;
    
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
    /* JsonDeserializerFactory impl (partial): type mappings
    /**********************************************************
     */

    @Override
    public JavaType mapAbstractType(DeserializationConfig config, JavaType type)
        throws JsonMappingException
    {
        // first, general mappings
        while (true) {
            JavaType next = _mapAbstractType2(config, type);
            if (next == null) {
                return type;
            }
            /* Should not have to worry about cycles; but better verify since they will invariably
             * occur... :-)
             * (also: guard against invalid resolution to a non-related type)
             */
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
                if (concrete != null && concrete.getRawClass() != currClass) {
                    return concrete;
                }
            }
        }
        return null;
    }
    
    /*
    /**********************************************************
    /* JsonDeserializerFactory impl (partial): ValueInstantiators
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
        // [JACKSON-633] Check @JsonValueInstantiator before anything else
        AnnotatedClass ac = beanDesc.getClassInfo();
        Object instDef = ctxt.getAnnotationIntrospector().findValueInstantiator(ac);
        if (instDef != null) {
            instantiator = _valueInstantiatorInstance(config, ac, instDef);
        }
        if (instantiator == null) {
            /* Second: see if some of standard Jackson/JDK types might provide value
             * instantiators.
             */
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
                    throw new JsonMappingException("Broken registered ValueInstantiators (of type "
                            +insts.getClass().getName()+"): returned null ValueInstantiator");
                }
            }
        }
        
        return instantiator;
    }

    private ValueInstantiator _findStdValueInstantiator(DeserializationConfig config,
            BeanDescription beanDesc)
        throws JsonMappingException
    {
        return JacksonDeserializers.findValueInstantiator(config, beanDesc);
    }

    /**
     * Method that will construct standard default {@link ValueInstantiator}
     * using annotations (like @JsonCreator) and visibility rules
     */
    protected ValueInstantiator _constructDefaultValueInstantiator(DeserializationContext ctxt,
            BeanDescription beanDesc)
        throws JsonMappingException
    {
        boolean fixAccess = ctxt.canOverrideAccessModifiers();
        CreatorCollector creators =  new CreatorCollector(beanDesc, fixAccess);
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        
        // need to construct suitable visibility checker:
        final DeserializationConfig config = ctxt.getConfig();
        VisibilityChecker<?> vchecker = config.getDefaultVisibilityChecker();
        vchecker = intr.findAutoDetectVisibility(beanDesc.getClassInfo(), vchecker);

        /* Important: first add factory methods; then constructors, so
         * latter can override former!
         */
        _addDeserializerFactoryMethods(ctxt, beanDesc, vchecker, intr, creators);
        // constructors only usable on concrete types:
        if (beanDesc.getType().isConcrete()) {
            _addDeserializerConstructors(ctxt, beanDesc, vchecker, intr, creators);
        }
        return creators.constructValueInstantiator(config);
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
            inst = (ValueInstantiator) instDef;
        } else {
            if (!(instDef instanceof Class)) {
                throw new IllegalStateException("AnnotationIntrospector returned key deserializer definition of type "
                        +instDef.getClass().getName()
                        +"; expected type KeyDeserializer or Class<KeyDeserializer> instead");
            }
            Class<?> instClass = (Class<?>)instDef;
            if (instClass == NoClass.class) {
                return null;
            }
            if (!ValueInstantiator.class.isAssignableFrom(instClass)) {
                throw new IllegalStateException("AnnotationIntrospector returned Class "+instClass.getName()
                        +"; expected Class<ValueInstantiator>");
            }
            HandlerInstantiator hi = config.getHandlerInstantiator();
            if (hi != null) {
                inst = hi.valueInstantiatorInstance(config, annotated, instClass);
            } else {
                inst = (ValueInstantiator) ClassUtil.createInstance(instClass,
                        config.canOverrideAccessModifiers());
            }
        }
        // not resolvable or contextual, just return:
        return inst;
    }
    
    protected void _addDeserializerConstructors
        (DeserializationContext ctxt, BeanDescription beanDesc, VisibilityChecker<?> vchecker,
         AnnotationIntrospector intr, CreatorCollector creators)
        throws JsonMappingException
    {
        /* First things first: the "default constructor" (zero-arg
         * constructor; whether implicit or explicit) is NOT included
         * in list of constructors, so needs to be handled separately.
         */
        AnnotatedConstructor defaultCtor = beanDesc.findDefaultConstructor();
        if (defaultCtor != null) {
            if (!creators.hasDefaultCreator() || intr.hasCreatorAnnotation(defaultCtor)) {
                creators.setDefaultCreator(defaultCtor);
            }
        }
        
        for (AnnotatedConstructor ctor : beanDesc.getConstructors()) {
            int argCount = ctor.getParameterCount();
            boolean isCreator = intr.hasCreatorAnnotation(ctor);
            boolean isVisible =  vchecker.isCreatorVisible(ctor);
            // some single-arg constructors (String, number) are auto-detected
            if (argCount == 1) {
                _handleSingleArgumentConstructor(ctxt, beanDesc, vchecker, intr, creators,
                        ctor, isCreator, isVisible);
                continue;
            }
            if (!isCreator && !isVisible) {
                continue;
            }
            // [JACKSON-541] improved handling a bit so:
            // 2 or more args; all params must have name annotations
            // ... or @JacksonInject (or equivalent)
            /* [JACKSON-711] One more possibility; can have 1 or more injectables, and
             * exactly one non-annotated parameter: if so, it's still delegating.
             */
            AnnotatedParameter nonAnnotatedParam = null;
            int namedCount = 0;
            int injectCount = 0;
            CreatorProperty[] properties = new CreatorProperty[argCount];
            for (int i = 0; i < argCount; ++i) {
                AnnotatedParameter param = ctor.getParameter(i);
                PropertyName pn = (param == null) ? null : intr.findNameForDeserialization(param);
                String name = (pn == null) ? null : pn.getSimpleName();
                Object injectId = intr.findInjectableValueId(param);
                if (name != null && name.length() > 0) {
                    ++namedCount;
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectId);
                } else if (injectId != null) {
                    ++injectCount;
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectId);
                } else if (nonAnnotatedParam == null) {
                    nonAnnotatedParam = param;
                }
            }

            // Ok: if named or injectable, we have more work to do
            if (isCreator || namedCount > 0 || injectCount > 0) {
                // simple case; everything covered:
                if ((namedCount + injectCount) == argCount) {
                    creators.addPropertyCreator(ctor, properties);
                } else if ((namedCount == 0) && ((injectCount + 1) == argCount)) {
                    // [712] secondary: all but one injectable, one un-annotated (un-named)
                    creators.addDelegatingCreator(ctor, properties);
                } else { // otherwise, epic fail
                    throw new IllegalArgumentException("Argument #"+nonAnnotatedParam.getIndex()+" of constructor "+ctor+" has no property name annotation; must have name when multiple-paramater constructor annotated as Creator");
                }
            }
        }
    }

    protected boolean _handleSingleArgumentConstructor(DeserializationContext ctxt,
            BeanDescription beanDesc, VisibilityChecker<?> vchecker,
            AnnotationIntrospector intr, CreatorCollector creators,
            AnnotatedConstructor ctor, boolean isCreator, boolean isVisible)
        throws JsonMappingException
    {
        // note: if we do have parameter name, it'll be "property constructor":
        AnnotatedParameter param = ctor.getParameter(0);
        PropertyName pn = (param == null) ? null : intr.findNameForDeserialization(param);
        String name = (pn == null) ? null : pn.getSimpleName();
        Object injectId = intr.findInjectableValueId(param);
    
        if ((injectId != null) || (name != null && name.length() > 0)) { // property-based
            // We know there's a name and it's only 1 parameter.
            CreatorProperty[] properties = new CreatorProperty[1];
            properties[0] = constructCreatorProperty(ctxt, beanDesc, name, 0, param, injectId);
            creators.addPropertyCreator(ctor, properties);
            return true;
        }
    
        // otherwise either 'simple' number, String, or general delegate:
        Class<?> type = ctor.getRawParameterType(0);
        if (type == String.class) {
            if (isCreator || isVisible) {
                creators.addStringCreator(ctor);
            }
            return true;
        }
        if (type == int.class || type == Integer.class) {
            if (isCreator || isVisible) {
                creators.addIntCreator(ctor);
            }
            return true;
        }
        if (type == long.class || type == Long.class) {
            if (isCreator || isVisible) {
                creators.addLongCreator(ctor);
            }
            return true;
        }
        if (type == double.class || type == Double.class) {
            if (isCreator || isVisible) {
                creators.addDoubleCreator(ctor);
            }
            return true;
        }
    
        // Delegating Creator ok iff it has @JsonCreator (etc)
        if (isCreator) {
            creators.addDelegatingCreator(ctor, null);
            return true;
        }
        return false;
    }
    
    protected void _addDeserializerFactoryMethods
        (DeserializationContext ctxt, BeanDescription beanDesc, VisibilityChecker<?> vchecker,
         AnnotationIntrospector intr, CreatorCollector creators)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        for (AnnotatedMethod factory : beanDesc.getFactoryMethods()) {
            boolean isCreator = intr.hasCreatorAnnotation(factory);
            int argCount = factory.getParameterCount();
            // zero-arg methods must be annotated; if so, are "default creators" [JACKSON-850]
            if (argCount == 0) {
                if (isCreator) {
                    creators.setDefaultCreator(factory);
                }
                continue;
            }
            // some single-arg factory methods (String, number) are auto-detected
            if (argCount == 1) {
                AnnotatedParameter param = factory.getParameter(0);
                PropertyName pn = (param == null) ? null : intr.findNameForDeserialization(param);
                String name = (pn == null) ? null : pn.getSimpleName();
                Object injectId = intr.findInjectableValueId(param);

                if ((injectId == null) && (name == null || name.length() == 0)) { // not property based
                    _handleSingleArgumentFactory(config, beanDesc, vchecker, intr, creators,
                            factory, isCreator);
                    // otherwise just ignored
                    continue;
                }
                // fall through if there's name
            } else {
                // more than 2 args, must be @JsonCreator
                if (!intr.hasCreatorAnnotation(factory)) {
                    continue;
                }
            }
            // 1 or more args; all params must have name annotations
            AnnotatedParameter nonAnnotatedParam = null;            
            CreatorProperty[] properties = new CreatorProperty[argCount];
            int namedCount = 0;
            int injectCount = 0;            
            for (int i = 0; i < argCount; ++i) {
                AnnotatedParameter param = factory.getParameter(i);
                PropertyName pn = (param == null) ? null : intr.findNameForDeserialization(param);
                String name = (pn == null) ? null : pn.getSimpleName();
                Object injectId = intr.findInjectableValueId(param);
                if (name != null && name.length() > 0) {
                    ++namedCount;
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectId);
                } else if (injectId != null) {
                    ++injectCount;
                    properties[i] = constructCreatorProperty(ctxt, beanDesc, name, i, param, injectId);
                } else if (nonAnnotatedParam == null) {
                    nonAnnotatedParam = param;
                }
            }

            // Ok: if named or injectable, we have more work to do
            if (isCreator || namedCount > 0 || injectCount > 0) {
                // simple case; everything covered:
                if ((namedCount + injectCount) == argCount) {
                    creators.addPropertyCreator(factory, properties);
                } else if ((namedCount == 0) && ((injectCount + 1) == argCount)) {
                    // [712] secondary: all but one injectable, one un-annotated (un-named)
                    creators.addDelegatingCreator(factory, properties);
                } else { // otherwise, epic fail
                    throw new IllegalArgumentException("Argument #"+nonAnnotatedParam.getIndex()
                            +" of factory method "+factory+" has no property name annotation; must have name when multiple-paramater constructor annotated as Creator");
                }
            }
        }
    }

    protected boolean _handleSingleArgumentFactory(DeserializationConfig config,
            BeanDescription beanDesc, VisibilityChecker<?> vchecker,
            AnnotationIntrospector intr, CreatorCollector creators,
            AnnotatedMethod factory, boolean isCreator)
        throws JsonMappingException
    {
        Class<?> type = factory.getRawParameterType(0);
        
        if (type == String.class) {
            if (isCreator || vchecker.isCreatorVisible(factory)) {
                creators.addStringCreator(factory);
            }
            return true;
        }
        if (type == int.class || type == Integer.class) {
            if (isCreator || vchecker.isCreatorVisible(factory)) {
                creators.addIntCreator(factory);
            }
            return true;
        }
        if (type == long.class || type == Long.class) {
            if (isCreator || vchecker.isCreatorVisible(factory)) {
                creators.addLongCreator(factory);
            }
            return true;
        }
        if (type == double.class || type == Double.class) {
            if (isCreator || vchecker.isCreatorVisible(factory)) {
                creators.addDoubleCreator(factory);
            }
            return true;
        }
        if (type == boolean.class || type == Boolean.class) {
            if (isCreator || vchecker.isCreatorVisible(factory)) {
                creators.addBooleanCreator(factory);
            }
            return true;
        }
        if (intr.hasCreatorAnnotation(factory)) {
            creators.addDelegatingCreator(factory, null);
            return true;
        }
        return false;
    }

    /**
     * Method that will construct a property object that represents
     * a logical property passed via Creator (constructor or static
     * factory method)
     */
    protected CreatorProperty constructCreatorProperty(DeserializationContext ctxt,
            BeanDescription beanDesc, String name, int index,
            AnnotatedParameter param,
            Object injectableValueId)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        JavaType t0 = config.getTypeFactory().constructType(param.getParameterType(), beanDesc.bindingsForBeanType());
        BeanProperty.Std property = new BeanProperty.Std(name, t0, beanDesc.getClassAnnotations(), param);
        JavaType type = resolveType(ctxt, beanDesc, t0, param);
        if (type != t0) {
            property = property.withType(type);
        }
        // Is there an annotation that specifies exact deserializer?
        JsonDeserializer<Object> deser = findDeserializerFromAnnotation(ctxt, param);
        // If yes, we are mostly done:
        type = modifyTypeByAnnotation(ctxt, param, type);

        // Type deserializer: either comes from property (and already resolved)
        TypeDeserializer typeDeser = (TypeDeserializer) type.getTypeHandler();
        // or if not, based on type being referenced:
        if (typeDeser == null) {
            typeDeser = findTypeDeserializer(config, type);
        }
        CreatorProperty prop = new CreatorProperty(name, type, typeDeser,
                beanDesc.getClassAnnotations(), param, index, injectableValueId);
        if (deser != null) {
            prop = prop.withValueDeserializer(deser);
        }
        return prop;
    }
    
    /*
    /**********************************************************
    /* JsonDeserializerFactory impl: array deserializers
    /**********************************************************
     */
        
    @Override
    public JsonDeserializer<?> createArrayDeserializer(DeserializationContext ctxt,
            ArrayType type, final BeanDescription beanDesc)
        throws JsonMappingException
    {
        JavaType elemType = type.getContentType();
        
        // Very first thing: is deserializer hard-coded for elements?
        JsonDeserializer<Object> contentDeser = elemType.getValueHandler();
        if (contentDeser == null) {
            // Maybe special array type, such as "primitive" arrays (int[] etc)
            JsonDeserializer<?> deser = _arrayDeserializers.get(elemType);
            if (deser != null) {
                /* 23-Nov-2010, tatu: Although not commonly needed, ability to override
                 *   deserializers for all types (including primitive arrays) is useful
                 *   so let's allow this
                 */
                JsonDeserializer<?> custom = _findCustomArrayDeserializer(type,
                        ctxt.getConfig(), beanDesc, null, contentDeser);
                if (custom != null) {
                    return custom;
                }
                return deser;
            }
            // If not, generic one:
            if (elemType.isPrimitive()) { // sanity check
                throw new IllegalArgumentException("Internal error: primitive type ("+type+") passed, no array deserializer found");
            }
        }
        // Then optional type info (1.5): if type has been resolved, we may already know type deserializer:
        TypeDeserializer elemTypeDeser = elemType.getTypeHandler();
        // but if not, may still be possible to find:
        if (elemTypeDeser == null) {
            elemTypeDeser = findTypeDeserializer(ctxt.getConfig(), elemType);
        }
        // 23-Nov-2010, tatu: Custom array deserializer?
        JsonDeserializer<?> custom = _findCustomArrayDeserializer(type,
                ctxt.getConfig(), beanDesc, elemTypeDeser, contentDeser);
        if (custom != null) {
            return custom;
        }
        return new ObjectArrayDeserializer(type, contentDeser, elemTypeDeser);
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
    
    /*
    /**********************************************************
    /* JsonDeserializerFactory impl: Collection(-like) deserializers
    /**********************************************************
     */
    
    @Override
    public JsonDeserializer<?> createCollectionDeserializer(DeserializationContext ctxt,
            CollectionType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        JavaType contentType = type.getContentType();
        // Very first thing: is deserializer hard-coded for elements?
        JsonDeserializer<Object> contentDeser = contentType.getValueHandler();

        // Then optional type info (1.5): if type has been resolved, we may already know type deserializer:
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        // but if not, may still be possible to find:
        if (contentTypeDeser == null) {
            contentTypeDeser = findTypeDeserializer(ctxt.getConfig(), contentType);
        }

        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> custom = _findCustomCollectionDeserializer(type,
                ctxt.getConfig(), beanDesc, contentTypeDeser, contentDeser);
        if (custom != null) {
            return custom;
        }
        
        Class<?> collectionClass = type.getRawClass();
        if (contentDeser == null) { // not defined by annotation
            // One special type: EnumSet:
            if (EnumSet.class.isAssignableFrom(collectionClass)) {
                return new EnumSetDeserializer(contentType, null);
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
        if (type.isInterface() || type.isAbstract()) {
            @SuppressWarnings({ "rawtypes" })
            Class<? extends Collection> fallback = _collectionFallbacks.get(collectionClass.getName());
            if (fallback == null) {
                throw new IllegalArgumentException("Can not find a deserializer for non-concrete Collection type "+type);
            }
            collectionClass = fallback;
            type = (CollectionType) ctxt.getConfig().constructSpecializedType(type, collectionClass);
            // But if so, also need to re-check creators...
            beanDesc = ctxt.getConfig().introspectForCreation(type);
        }
        ValueInstantiator inst = findValueInstantiator(ctxt, beanDesc);
        // 13-Dec-2010, tatu: Can use more optimal deserializer if content type is String, so:
        if (contentType.getRawClass() == String.class) {
            // no value type deserializer because Strings are one of natural/native types:
            return new StringCollectionDeserializer(type, contentDeser, inst);
        }
        return new CollectionDeserializer(type, contentDeser, contentTypeDeser, inst);
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

        // Then optional type info (1.5): if type has been resolved, we may already know type deserializer:
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        // but if not, may still be possible to find:
        if (contentTypeDeser == null) {
            contentTypeDeser = findTypeDeserializer(ctxt.getConfig(), contentType);
        }
        return _findCustomCollectionLikeDeserializer(type, ctxt.getConfig(), beanDesc,
                contentTypeDeser, contentDeser);
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

    /*
    /**********************************************************
    /* JsonDeserializerFactory impl: Map(-like) deserializers
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
        // Then optional type info (1.5); either attached to type, or resolved separately:
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        // but if not, may still be possible to find:
        if (contentTypeDeser == null) {
            contentTypeDeser = findTypeDeserializer(config, contentType);
        }

        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> custom = _findCustomMapDeserializer(type, config, beanDesc,
                keyDes, contentTypeDeser, contentDeser);

        if (custom != null) {
            return custom;
        }
        // Value handling is identical for all, but EnumMap requires special handling for keys
        Class<?> mapClass = type.getRawClass();
        if (EnumMap.class.isAssignableFrom(mapClass)) {
            Class<?> kt = keyType.getRawClass();
            if (kt == null || !kt.isEnum()) {
                throw new IllegalArgumentException("Can not construct EnumMap; generic (key) type not available");
            }
            return new EnumMapDeserializer(type, null, contentDeser);
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
        if (type.isInterface() || type.isAbstract()) {
            @SuppressWarnings("rawtypes")
            Class<? extends Map> fallback = _mapFallbacks.get(mapClass.getName());
            if (fallback == null) {
                throw new IllegalArgumentException("Can not find a deserializer for non-concrete Map type "+type);
            }
            mapClass = fallback;
            type = (MapType) config.constructSpecializedType(type, mapClass);
            // But if so, also need to re-check creators...
            beanDesc = config.introspectForCreation(type);
        }
        ValueInstantiator inst = findValueInstantiator(ctxt, beanDesc);
        MapDeserializer md = new MapDeserializer(type, inst, keyDes, contentDeser, contentTypeDeser);
        md.setIgnorableProperties(config.getAnnotationIntrospector().findPropertiesToIgnore(beanDesc.getClassInfo()));
        return md;
    }

    // Copied almost verbatim from "createMapDeserializer" -- should try to share more code
    @Override
    public JsonDeserializer<?> createMapLikeDeserializer(DeserializationContext ctxt,
            MapLikeType type, final BeanDescription beanDesc)
        throws JsonMappingException
    {
        JavaType keyType = type.getKeyType();
        JavaType contentType = type.getContentType();
        
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
            contentTypeDeser = findTypeDeserializer(ctxt.getConfig(), contentType);
        }
        return _findCustomMapLikeDeserializer(type, ctxt.getConfig(),
                beanDesc, keyDes, contentTypeDeser, contentDeser);
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
    /* JsonDeserializerFactory impl: Enum deserializers
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
        Class<?> enumClass = type.getRawClass();
        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> custom = _findCustomEnumDeserializer(enumClass,
                ctxt.getConfig(), beanDesc);
        if (custom != null) {
            return custom;
        }

        // [JACKSON-193] May have @JsonCreator for static factory method:
        for (AnnotatedMethod factory : beanDesc.getFactoryMethods()) {
            if (ctxt.getAnnotationIntrospector().hasCreatorAnnotation(factory)) {
                int argCount = factory.getParameterCount();
                if (argCount == 1) {
                    Class<?> returnType = factory.getRawReturnType();
                    // usually should be class, but may be just plain Enum<?> (for Enum.valueOf()?)
                    if (returnType.isAssignableFrom(enumClass)) {
                        return EnumDeserializer.deserializerForCreator(ctxt.getConfig(), enumClass, factory);
                    }
                }
                throw new IllegalArgumentException("Unsuitable method ("+factory+") decorated with @JsonCreator (for Enum type "
                        +enumClass.getName()+")");
            }
        }
        // [JACKSON-749] Also, need to consider @JsonValue, if one found
        return new EnumDeserializer(constructEnumResolver(enumClass, ctxt.getConfig(), beanDesc.findJsonValueMethod()));
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
    
    /*
    /**********************************************************
    /* JsonDeserializerFactory impl: Tree deserializers
    /**********************************************************
     */
    
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
    
    /*
    /**********************************************************
    /* JsonDeserializerFactory impl (partial): type deserializers
    /**********************************************************
     */

    @Override
    public TypeDeserializer findTypeDeserializer(DeserializationConfig config,
            JavaType baseType)
        throws JsonMappingException
    {
        Class<?> cls = baseType.getRawClass();
        BeanDescription bean = config.introspectClassAnnotations(cls);
        AnnotatedClass ac = bean.getClassInfo();
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findTypeResolver(config, ac, baseType);

        /* Ok: if there is no explicit type info handler, we may want to
         * use a default. If so, config object knows what to use.
         */
        Collection<NamedType> subtypes = null;
        if (b == null) {
            b = config.getDefaultTyper(baseType);
            if (b == null) {
                return null;
            }
        } else {
            subtypes = config.getSubtypeResolver().collectAndResolveSubtypes(ac, config, ai);
        }
        // [JACKSON-505]: May need to figure out default implementation, if none found yet
        // (note: check for abstract type is not 100% mandatory, more of an optimization)
        if ((b.getDefaultImpl() == null) && baseType.isAbstract()) {
            JavaType defaultType = mapAbstractType(config, baseType);
            if (defaultType != null && defaultType.getRawClass() != baseType.getRawClass()) {
                b = b.defaultImpl(defaultType.getRawClass());
            }
        }
        return b.buildTypeDeserializer(config, baseType, subtypes);
    }

    /*
    /**********************************************************
    /* JsonDeserializerFactory impl (partial): key deserializers
    /**********************************************************
     */
    
    @Override
    public KeyDeserializer createKeyDeserializer(DeserializationContext ctxt,
            JavaType type)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        if (_factoryConfig.hasKeyDeserializers()) {
            BeanDescription beanDesc = config.introspectClassAnnotations(type.getRawClass());
            for (KeyDeserializers d  : _factoryConfig.keyDeserializers()) {
                KeyDeserializer deser = d.findKeyDeserializer(type, config, beanDesc);
                if (deser != null) {
                    return deser;
                }
            }
        }
        // and if none found, standard ones:
        Class<?> raw = type.getRawClass();
        if (raw == String.class || raw == Object.class) {
            return StdKeyDeserializers.constructStringKeyDeserializer(config, type);
        }
        // Most other keys are for limited number of static types
        KeyDeserializer kdes = _keyDeserializers.get(type);
        if (kdes != null) {
            return kdes;
        }
        // And then other one-offs; first, Enum:
        if (type.isEnumType()) {
            return _createEnumKeyDeserializer(ctxt, type);
        }
        // One more thing: can we find ctor(String) or valueOf(String)?
        kdes = StdKeyDeserializers.findStringBasedKeyDeserializer(config, type);
        return kdes;
    }

    private KeyDeserializer _createEnumKeyDeserializer(DeserializationContext ctxt,
            JavaType type)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        BeanDescription beanDesc = config.introspect(type);
        JsonDeserializer<?> des = findDeserializerFromAnnotation(ctxt, beanDesc.getClassInfo());
        if (des != null) {
            return StdKeyDeserializers.constructDelegatingKeyDeserializer(config, type, des);
        }
        Class<?> enumClass = type.getRawClass();
        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> custom = _findCustomEnumDeserializer(enumClass, config, beanDesc);
        if (custom != null) {
            return StdKeyDeserializers.constructDelegatingKeyDeserializer(config, type, des);
        }

        EnumResolver<?> enumRes = constructEnumResolver(enumClass, config, beanDesc.findJsonValueMethod());
        // [JACKSON-193] May have @JsonCreator for static factory method:
        for (AnnotatedMethod factory : beanDesc.getFactoryMethods()) {
            if (config.getAnnotationIntrospector().hasCreatorAnnotation(factory)) {
                int argCount = factory.getParameterCount();
                if (argCount == 1) {
                    Class<?> returnType = factory.getRawReturnType();
                    // usually should be class, but may be just plain Enum<?> (for Enum.valueOf()?)
                    if (returnType.isAssignableFrom(enumClass)) {
                        // note: mostly copied from 'EnumDeserializer.deserializerForCreator(...)'
                        if (factory.getGenericParameterType(0) != String.class) {
                            throw new IllegalArgumentException("Parameter #0 type for factory method ("+factory+") not suitable, must be java.lang.String");
                        }
                        if (config.canOverrideAccessModifiers()) {
                            ClassUtil.checkAndFixAccess(factory.getMember());
                        }
                        return StdKeyDeserializers.constructEnumKeyDeserializer(enumRes, factory);
                    }
                }
                throw new IllegalArgumentException("Unsuitable method ("+factory+") decorated with @JsonCreator (for Enum type "
                        +enumClass.getName()+")");
            }
        }
        // [JACKSON-749] Also, need to consider @JsonValue, if one found
        return StdKeyDeserializers.constructEnumKeyDeserializer(enumRes);
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
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypes(
                annotated, config, ai, baseType);
        return b.buildTypeDeserializer(config, baseType, subtypes);
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
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypes(
                propertyEntity, config, ai, contentType);
        return b.buildTypeDeserializer(config, contentType, subtypes);
    }

    /*
    /**********************************************************
    /* Helper methods, value/content/key type introspection
    /**********************************************************
     */
    
    /**
     * Helper method called to check if a class or method
     * has annotation that tells which class to use for deserialization.
     * Returns null if no such annotation found.
     */
    protected JsonDeserializer<Object> findDeserializerFromAnnotation(DeserializationContext ctxt,
            Annotated ann)
        throws JsonMappingException
    {
        Object deserDef = ctxt.getAnnotationIntrospector().findDeserializer(ann);
        if (deserDef == null) {
            return null;
        }
        return ctxt.deserializerInstance(ann, deserDef);
    }

    /**
     * Method called to see if given method has annotations that indicate
     * a more specific type than what the argument specifies.
     * If annotations are present, they must specify compatible Class;
     * instance of which can be assigned using the method. This means
     * that the Class has to be raw class of type, or its sub-class
     * (or, implementing class if original Class instance is an interface).
     *
     * @param a Method or field that the type is associated with
     * @param type Type of field, or the setter argument
     *
     * @return Original type if no annotations are present; or a more
     *   specific type derived from it if type annotation(s) was found
     *
     * @throws JsonMappingException if invalid annotation is found
     */
    @SuppressWarnings({ "unchecked" })
    protected <T extends JavaType> T modifyTypeByAnnotation(DeserializationContext ctxt,
            Annotated a, T type)
        throws JsonMappingException
    {
        // first: let's check class for the instance itself:
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        Class<?> subclass = intr.findDeserializationType(a, type);
        if (subclass != null) {
            try {
                type = (T) type.narrowBy(subclass);
            } catch (IllegalArgumentException iae) {
                throw new JsonMappingException("Failed to narrow type "+type+" with concrete-type annotation (value "+subclass.getName()+"), method '"+a.getName()+"': "+iae.getMessage(), null, iae);
            }
        }

        // then key class
        if (type.isContainerType()) {
            Class<?> keyClass = intr.findDeserializationKeyType(a, type.getKeyType());
            if (keyClass != null) {
                // illegal to use on non-Maps
                if (!(type instanceof MapLikeType)) {
                    throw new JsonMappingException("Illegal key-type annotation: type "+type+" is not a Map(-like) type");
                }
                try {
                    type = (T) ((MapLikeType) type).narrowKey(keyClass);
                } catch (IllegalArgumentException iae) {
                    throw new JsonMappingException("Failed to narrow key type "+type+" with key-type annotation ("+keyClass.getName()+"): "+iae.getMessage(), null, iae);
                }
            }
            JavaType keyType = type.getKeyType();
            /* 21-Mar-2011, tatu: ... and associated deserializer too (unless already assigned)
             *   (not 100% why or how, but this does seem to get called more than once, which
             *   is not good: for now, let's just avoid errors)
             */
            if (keyType != null && keyType.getValueHandler() == null) {
                Object kdDef = intr.findKeyDeserializer(a);
                KeyDeserializer kd = ctxt.keyDeserializerInstance(a, kdDef);
                if (kd != null) {
                    type = (T) ((MapLikeType) type).withKeyValueHandler(kd);
                    keyType = type.getKeyType(); // just in case it's used below
                }
            }            
           
           // and finally content class; only applicable to structured types
           Class<?> cc = intr.findDeserializationContentType(a, type.getContentType());
           if (cc != null) {
               try {
                   type = (T) type.narrowContentsBy(cc);
               } catch (IllegalArgumentException iae) {
                   throw new JsonMappingException("Failed to narrow content type "+type+" with content-type annotation ("+cc.getName()+"): "+iae.getMessage(), null, iae);
               }
           }
           // ... as well as deserializer for contents:
           JavaType contentType = type.getContentType();
           if (contentType.getValueHandler() == null) { // as with above, avoid resetting (which would trigger exception)
               Object cdDef = intr.findContentDeserializer(a);
                JsonDeserializer<?> cd = ctxt.deserializerInstance(a, cdDef);
                if (cd != null) {
                    type = (T) type.withContentValueHandler(cd);
                }
            }
        }
        return type;
    }
    
    /**
     * Helper method used to resolve method return types and field
     * types. The main trick here is that the containing bean may
     * have type variable binding information (when deserializing
     * using generic type passed as type reference), which is
     * needed in some cases.
     */
    protected JavaType resolveType(DeserializationContext ctxt,
            BeanDescription beanDesc, JavaType type, AnnotatedMember member)
        throws JsonMappingException
    {
        // [JACKSON-154]: Also need to handle keyUsing, contentUsing
        if (type.isContainerType()) {
            AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
            JavaType keyType = type.getKeyType();
            if (keyType != null) {
                Object kdDef = intr.findKeyDeserializer(member);
                KeyDeserializer kd = ctxt.keyDeserializerInstance(member, kdDef);
                if (kd != null) {
                    type = ((MapLikeType) type).withKeyValueHandler(kd);
                    keyType = type.getKeyType(); // just in case it's used below
                }
            }
            // and all container types have content types...
            Object cdDef = intr.findContentDeserializer(member);
            JsonDeserializer<?> cd = ctxt.deserializerInstance(member, cdDef);
            if (cd != null) {
                type = type.withContentValueHandler(cd);
            }
            /* 04-Feb-2010, tatu: Need to figure out JAXB annotations that indicate type
             *    information to use for polymorphic members; and specifically types for
             *    collection values (contents).
             *    ... but only applies to members (fields, methods), not classes
             */
            if (member instanceof AnnotatedMember) {
            	TypeDeserializer contentTypeDeser = findPropertyContentTypeDeserializer(
            	        ctxt.getConfig(), type, (AnnotatedMember) member);            	
            	if (contentTypeDeser != null) {
            	    type = type.withContentTypeHandler(contentTypeDeser);
            	}
            }
        }
    	TypeDeserializer valueTypeDeser;

        if (member instanceof AnnotatedMember) { // JAXB allows per-property annotations
            valueTypeDeser = findPropertyTypeDeserializer(ctxt.getConfig(),
                    type, (AnnotatedMember) member);
        } else { // classes just have Jackson annotations
            // probably only occurs if 'property' is null anyway
            valueTypeDeser = findTypeDeserializer(ctxt.getConfig(), type);
        }
    	if (valueTypeDeser != null) {
            type = type.withTypeHandler(valueTypeDeser);
    	}
    	return type;
    }
    
    protected EnumResolver<?> constructEnumResolver(Class<?> enumClass,
            DeserializationConfig config, AnnotatedMethod jsonValueMethod)
    {
        if (jsonValueMethod != null) {
            Method accessor = jsonValueMethod.getAnnotated();
            if (config.canOverrideAccessModifiers()) {
                ClassUtil.checkAndFixAccess(accessor);
            }
            return EnumResolver.constructUnsafeUsingMethod(enumClass, accessor);
        }
        // [JACKSON-212]: may need to use Enum.toString()
        if (config.isEnabled(DeserializationFeature.READ_ENUMS_USING_TO_STRING)) {
            return EnumResolver.constructUnsafeUsingToString(enumClass);
        }
        return EnumResolver.constructUnsafe(enumClass, config.getAnnotationIntrospector());
    }

    protected AnnotatedMethod _findJsonValueFor(DeserializationConfig config, JavaType enumType)
    {
        if (enumType == null) {
            return null;
        }
        BeanDescription beanDesc = config.introspect(enumType);
        return beanDesc.findJsonValueMethod();
    }
}
