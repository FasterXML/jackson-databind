package com.fasterxml.jackson.databind.introspect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.NoClass;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.util.Annotations;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;

/**
 * Default {@link BeanDescription} implementation.
 * Can theoretically be subclassed to customize
 * some aspects of property introspection.
 */
public class BasicBeanDescription extends BeanDescription
{
    /*
    /**********************************************************
    /* General configuration
    /**********************************************************
     */

    final protected MapperConfig<?> _config;

    final protected AnnotationIntrospector _annotationIntrospector;
    
    /**
     * Information collected about the class introspected.
     */
    final protected AnnotatedClass _classInfo;
    
    /**
     * We may need type bindings for the bean type. If so, we'll
     * construct it lazily
     */
    protected TypeBindings _bindings;

    /*
    /**********************************************************
    /* Member information
    /**********************************************************
     */

    /**
     * Properties collected for the POJO.
     */
    protected final List<BeanPropertyDefinition> _properties;

    /**
     * Details of Object Id to include, if any
     */
    protected ObjectIdInfo _objectIdInfo;
    
    // // for deserialization
    
    protected AnnotatedMethod _anySetterMethod;

    protected Map<Object, AnnotatedMember> _injectables;
    
    /**
     * Set of properties that can be ignored during deserialization, due
     * to being marked as ignored.
     */
    protected Set<String> _ignoredPropertyNames;

    // // for serialization
    
    protected AnnotatedMethod _jsonValueMethod;

    protected AnnotatedMember _anyGetter;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected BasicBeanDescription(MapperConfig<?> config,
            JavaType type, AnnotatedClass classDef,
            List<BeanPropertyDefinition> props)
    {
        super(type);
        _config = config;
        _annotationIntrospector = (config == null) ? null : config.getAnnotationIntrospector();
        _classInfo = classDef;
        _properties = props;
    }
    
    protected BasicBeanDescription(POJOPropertiesCollector coll)
    {
        this(coll.getConfig(), coll.getType(), coll.getClassDef(), coll.getProperties());
        _objectIdInfo = coll.getObjectIdInfo();
    }

    /**
     * Factory method to use for constructing an instance to use for building
     * deserializers.
     */
    public static BasicBeanDescription forDeserialization(POJOPropertiesCollector coll)
    {
        BasicBeanDescription desc = new BasicBeanDescription(coll);
        desc._anySetterMethod = coll.getAnySetterMethod();
        desc._ignoredPropertyNames = coll.getIgnoredPropertyNames();
        desc._injectables = coll.getInjectables();
        desc._jsonValueMethod = coll.getJsonValueMethod();
        return desc;
    }

    /**
     * Factory method to use for constructing an instance to use for building
     * serializers.
     */
    public static BasicBeanDescription forSerialization(POJOPropertiesCollector coll)
    {
        BasicBeanDescription desc = new BasicBeanDescription(coll);
        desc._jsonValueMethod = coll.getJsonValueMethod();
        desc._anyGetter = coll.getAnyGetter();
        return desc;
    }

    /**
     * Factory method to use for constructing an instance to use for purposes
     * other than building serializers or deserializers; will only have information
     * on class, not on properties.
     */
    public static BasicBeanDescription forOtherUse(MapperConfig<?> config,
            JavaType type, AnnotatedClass ac)
    {
        return new BasicBeanDescription(config, type,
                ac, Collections.<BeanPropertyDefinition>emptyList());
    }

    /*
    /**********************************************************
    /* Limited modifications by core databind functionality
    /**********************************************************
     */

    /**
     * Method that can be used to prune unwanted properties, during
     * construction of serializers and deserializers.
     * Use with utmost care, if at all...
     * 
     * @since 2.1
     */
    public boolean removeProperty(String propName)
    {
        Iterator<BeanPropertyDefinition> it = _properties.iterator();
        while (it.hasNext()) {
            BeanPropertyDefinition prop = it.next();
            if (prop.getName().equals(propName)) {
                it.remove();
                return true;
            }
        }
        return false;
    }
    
    /*
    /**********************************************************
    /* Simple accessors from BeanDescription
    /**********************************************************
     */

    @Override
    public AnnotatedClass getClassInfo() { return _classInfo; }

    @Override
    public ObjectIdInfo getObjectIdInfo() { return  _objectIdInfo; }
    
    @Override
    public List<BeanPropertyDefinition> findProperties() {
        return _properties;
    }

    @Override
    public AnnotatedMethod findJsonValueMethod() {
        return _jsonValueMethod;
    }

    @Override
    public Set<String> getIgnoredPropertyNames() {
        if (_ignoredPropertyNames == null) {
            return Collections.emptySet();
        }
        return _ignoredPropertyNames;
    }
    
    @Override
    public boolean hasKnownClassAnnotations() {
        return _classInfo.hasAnnotations();
    }

    @Override
    public Annotations getClassAnnotations() {
        return _classInfo.getAnnotations();
    }

    @Override
    public TypeBindings bindingsForBeanType()
    {
        if (_bindings == null) {
            _bindings = new TypeBindings(_config.getTypeFactory(), _type);
        }
        return _bindings;
    }

    @Override
    public JavaType resolveType(java.lang.reflect.Type jdkType) {
        if (jdkType == null) {
            return null;
        }
        return bindingsForBeanType().resolveType(jdkType);
    }

    @Override
    public AnnotatedConstructor findDefaultConstructor() {
        return _classInfo.getDefaultConstructor();
    }

    @Override
    public AnnotatedMethod findAnySetter() throws IllegalArgumentException
    {
        if (_anySetterMethod != null) {
            /* Also, let's be somewhat strict on how field name is to be
             * passed; String, Object make sense, others not
             * so much.
             */
            /* !!! 18-May-2009, tatu: how about enums? Can add support if
             *  requested; easy enough for devs to add support within
             *  method.
             */
            Class<?> type = _anySetterMethod.getRawParameterType(0);
            if (type != String.class && type != Object.class) {
                throw new IllegalArgumentException("Invalid 'any-setter' annotation on method "+_anySetterMethod.getName()+"(): first argument not of type String or Object, but "+type.getName());
            }
        }
        return _anySetterMethod;
    }

    @Override
    public Map<Object, AnnotatedMember> findInjectables() {
        return _injectables;
    }

    @Override
    public List<AnnotatedConstructor> getConstructors() {
        return _classInfo.getConstructors();
    }

    @Override
    public Object instantiateBean(boolean fixAccess)
    {
        AnnotatedConstructor ac = _classInfo.getDefaultConstructor();
        if (ac == null) {
            return null;
        }
        if (fixAccess) {
            ac.fixAccess();
        }
        try {
            return ac.getAnnotated().newInstance();
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            if (t instanceof Error) throw (Error) t;
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            throw new IllegalArgumentException("Failed to instantiate bean of type "+_classInfo.getAnnotated().getName()+": ("+t.getClass().getName()+") "+t.getMessage(), t);
        }
    }
    
    /*
    /**********************************************************
    /* Simple accessors, extended
    /**********************************************************
     */

    @Override
    public AnnotatedMethod findMethod(String name, Class<?>[] paramTypes) {
        return _classInfo.findMethod(name, paramTypes);
    }

    /*
    /**********************************************************
    /* General per-class annotation introspection
    /**********************************************************
     */

    @Override
    public JsonFormat.Value findExpectedFormat(JsonFormat.Value defValue)
    {
        if (_annotationIntrospector != null) {
            JsonFormat.Value v = _annotationIntrospector.findFormat(_classInfo);
            if (v != null) {
                return v;
            }
        }
        return defValue;
    }
    
    /*
    /**********************************************************
    /* Introspection for serialization
    /**********************************************************
     */

    @Override
    public Converter<Object,Object> findSerializationConverter()
    {
        if (_annotationIntrospector == null) {
            return null;
        }
        return _createConverter(_annotationIntrospector.findSerializationConverter(_classInfo));
    }

    /**
     * Method for determining whether null properties should be written
     * out for a Bean of introspected type. This is based on global
     * feature (lowest priority, passed as argument)
     * and per-class annotation (highest priority).
     */
    @Override
    public JsonInclude.Include findSerializationInclusion(JsonInclude.Include defValue)
    {
        if (_annotationIntrospector == null) {
            return defValue;
        }
        return _annotationIntrospector.findSerializationInclusion(_classInfo, defValue);
    }
    
    /**
     * Method used to locate the method of introspected class that
     * implements {@link com.fasterxml.jackson.annotation.JsonAnyGetter}.
     * If no such method exists null is returned.
     * If more than one are found, an exception is thrown.
     */
    @Override
    public AnnotatedMember findAnyGetter() throws IllegalArgumentException
    {
        if (_anyGetter != null) {
            /* For now let's require a Map; in future can add support for other
             * types like perhaps Iterable<Map.Entry>?
             */
            Class<?> type = _anyGetter.getRawType();
            if (!Map.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("Invalid 'any-getter' annotation on method "+_anyGetter.getName()+"(): return type is not instance of java.util.Map");
            }
        }
        return _anyGetter;
    }
    
    @Override
    public Map<String,AnnotatedMember> findBackReferenceProperties()
    {
        HashMap<String,AnnotatedMember> result = null;
        for (BeanPropertyDefinition property : _properties) {
            AnnotatedMember am = property.getMutator();
            if (am == null) {
                continue;
            }
            AnnotationIntrospector.ReferenceProperty refDef = _annotationIntrospector.findReferenceType(am);
            if (refDef != null && refDef.isBackReference()) {
                if (result == null) {
                    result = new HashMap<String,AnnotatedMember>();
                }
                String refName = refDef.getName();
                if (result.put(refName, am) != null) {
                    throw new IllegalArgumentException("Multiple back-reference properties with name '"+refName+"'");
                }
            }
        }
        return result;
    }

    /*
    /**********************************************************
    /* Introspection for deserialization, factories
    /**********************************************************
     */

    @Override
    public List<AnnotatedMethod> getFactoryMethods()
    {
        // must filter out anything that clearly is not a factory method
        List<AnnotatedMethod> candidates = _classInfo.getStaticMethods();
        if (candidates.isEmpty()) {
            return candidates;
        }
        ArrayList<AnnotatedMethod> result = new ArrayList<AnnotatedMethod>();
        for (AnnotatedMethod am : candidates) {
            if (isFactoryMethod(am)) {
                result.add(am);
            }
        }
        return result;
    }

    @Override
    public Constructor<?> findSingleArgConstructor(Class<?>... argTypes)
    {
        for (AnnotatedConstructor ac : _classInfo.getConstructors()) {
            // This list is already filtered to only include accessible
            /* (note: for now this is a redundant check; but in future
             * that may change; thus leaving here for now)
             */
            if (ac.getParameterCount() == 1) {
                Class<?> actArg = ac.getRawParameterType(0);
                for (Class<?> expArg : argTypes) {
                    if (expArg == actArg) {
                        return ac.getAnnotated();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Method findFactoryMethod(Class<?>... expArgTypes)
    {
        // So, of all single-arg static methods:
        for (AnnotatedMethod am : _classInfo.getStaticMethods()) {
            if (isFactoryMethod(am)) {
                // And must take one of expected arg types (or supertype)
                Class<?> actualArgType = am.getRawParameterType(0);
                for (Class<?> expArgType : expArgTypes) {
                    // And one that matches what we would pass in
                    if (actualArgType.isAssignableFrom(expArgType)) {
                        return am.getAnnotated();
                    }
                }
            }
        }
        return null;
    }

    protected boolean isFactoryMethod(AnnotatedMethod am)
    {
        /* First: return type must be compatible with the introspected class
         * (i.e. allowed to be sub-class, although usually is the same
         * class)
         */
        Class<?> rt = am.getRawReturnType();
        if (!getBeanClass().isAssignableFrom(rt)) {
            return false;
        }

        /* Also: must be a recognized factory method, meaning:
         * (a) marked with @JsonCreator annotation, or
         * (a) "valueOf" (at this point, need not be public)
         */
        if (_annotationIntrospector.hasCreatorAnnotation(am)) {
            return true;
        }
        if ("valueOf".equals(am.getName())) {
            return true;
        }
        return false;
    }

    /**
     * Method for getting ordered list of named Creator properties.
     * Returns an empty list is none found. If multiple Creator
     * methods are defined, order between properties from different
     * methods is undefined; however, properties for each such
     * Creator are ordered properly relative to each other. For the
     * usual case of just a single Creator, named properties are
     * thus properly ordered.
     */
    public List<String> findCreatorPropertyNames()
    {
        List<String> names = null;

        for (int i = 0; i < 2; ++i) {
            List<? extends AnnotatedWithParams> l = (i == 0)
                ? getConstructors() : getFactoryMethods();
            for (AnnotatedWithParams creator : l) {
                int argCount = creator.getParameterCount();
                if (argCount < 1) continue;
                PropertyName name = _annotationIntrospector.findNameForDeserialization(creator.getParameter(0));
                if (name == null) {
                    continue;
                }
                if (names == null) {
                    names = new ArrayList<String>();
                }
                names.add(name.getSimpleName());
                for (int p = 1; p < argCount; ++p) {
                    name = _annotationIntrospector.findNameForDeserialization(creator.getParameter(p));
                    names.add((name == null) ? null : name.getSimpleName());
                }
            }
        }
        if (names == null) {
            return Collections.emptyList();
        }
        return names;
    }

    /*
    /**********************************************************
    /* Introspection for deserialization, other
    /**********************************************************
     */
    
    @Override
    public Class<?> findPOJOBuilder()
    {
    	return (_annotationIntrospector == null) ?
    			null : _annotationIntrospector.findPOJOBuilder(_classInfo);
    }

    @Override
    public JsonPOJOBuilder.Value findPOJOBuilderConfig()
    {
        return (_annotationIntrospector == null) ?
                null : _annotationIntrospector.findPOJOBuilderConfig(_classInfo);
    }

    @Override
    public Converter<Object,Object> findDeserializationConverter()
    {
        if (_annotationIntrospector == null) {
            return null;
        }
        return _createConverter(_annotationIntrospector.findDeserializationConverter(_classInfo));
    }

    /*
    /**********************************************************
    /* Helper methods for field introspection
    /**********************************************************
     */

    /**
     * @param ignoredProperties (optional) names of properties to ignore;
     *   any fields that would be recognized as one of these properties
     *   is ignored.
     * @param forSerialization If true, will collect serializable property
     *    fields; if false, deserializable
     *
     * @return Ordered Map with logical property name as key, and
     *    matching field as value.
     */
    public LinkedHashMap<String,AnnotatedField> _findPropertyFields(
            Collection<String> ignoredProperties, boolean forSerialization)
    {
        LinkedHashMap<String,AnnotatedField> results = new LinkedHashMap<String,AnnotatedField>();
        for (BeanPropertyDefinition property : _properties) {
            AnnotatedField f = property.getField();
            if (f != null) {
                String name = property.getName();
                if (ignoredProperties != null) {
                    if (ignoredProperties.contains(name)) {
                        continue;
                    }
                }
                results.put(name, f);
            }
        }
        return results;
    }

    /*
    /**********************************************************
    /* Helper methods, other
    /**********************************************************
     */
    
    @SuppressWarnings("unchecked")
    public Converter<Object,Object> _createConverter(Object converterDef)
    {
        if (converterDef == null) {
            return null;
        }
        if (converterDef instanceof Converter<?,?>) {
            return (Converter<Object,Object>) converterDef;
        }
        if (!(converterDef instanceof Class)) {
            throw new IllegalStateException("AnnotationIntrospector returned Converter definition of type "
                    +converterDef.getClass().getName()+"; expected type Converter or Class<Converter> instead");
        }
        Class<?> converterClass = (Class<?>)converterDef;
        // there are some known "no class" markers to consider too:
        if (converterClass == Converter.None.class || converterClass == NoClass.class) {
            return null;
        }
        if (!Converter.class.isAssignableFrom(converterClass)) {
            throw new IllegalStateException("AnnotationIntrospector returned Class "
                    +converterClass.getName()+"; expected Class<Converter>");
        }
        HandlerInstantiator hi = _config.getHandlerInstantiator();
        Converter<?,?> conv = (hi == null) ? null : hi.converterInstance(_config, _classInfo, converterClass);
        if (conv == null) {
            conv = (Converter<?,?>) ClassUtil.createInstance(converterClass,
                    _config.canOverrideAccessModifiers());
        }
        return (Converter<Object,Object>) conv;
    }
}
