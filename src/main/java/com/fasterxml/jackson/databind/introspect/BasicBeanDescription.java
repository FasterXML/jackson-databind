package com.fasterxml.jackson.databind.introspect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.util.Annotations;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;

/**
 * Default {@link BeanDescription} implementation used by Jackson.
 *<p>
 * Although sub-classing is a theoretical possibility there are no known
 * use cases for that, nor is such usage tested or supported.
 * Separation from API is mostly to isolate some implementation details
 * here and keep API simple.
 */
public class BasicBeanDescription extends BeanDescription
{
    /*
    /**********************************************************
    /* General configuration
    /**********************************************************
     */

    /**
     * We will hold a reference to the collector in cases where
     * information is lazily accessed and constructed; properties
     * are only accessed when they are actually needed.
     */
    final protected POJOPropertiesCollector _propCollector;
    
    final protected MapperConfig<?> _config;

    final protected AnnotationIntrospector _annotationIntrospector;
    
    /**
     * Information collected about the class introspected.
     */
    final protected AnnotatedClass _classInfo;

    /*
    /**********************************************************
    /* Member information
    /**********************************************************
     */

    /**
     * Properties collected for the POJO; initialized as needed.
     */
    protected List<BeanPropertyDefinition> _properties;

    /**
     * Details of Object Id to include, if any
     */
    protected ObjectIdInfo _objectIdInfo;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected BasicBeanDescription(POJOPropertiesCollector coll,
            JavaType type, AnnotatedClass classDef)
    {
        super(type);
        _propCollector = coll;
        _config = coll.getConfig();
        // NOTE: null config only for some pre-constructed types
        if (_config == null) {
            _annotationIntrospector = null;
        } else {
            _annotationIntrospector = _config.getAnnotationIntrospector();
        }
        _classInfo = classDef;
    }

    /**
     * Alternate constructor used in cases where property information is not needed,
     * only class info.
     */
    protected BasicBeanDescription(MapperConfig<?> config,
            JavaType type, AnnotatedClass classDef, List<BeanPropertyDefinition> props)
    {
        super(type);
        _propCollector = null;
        _config = config;
        // NOTE: null config only for some pre-constructed types
        if (_config == null) {
            _annotationIntrospector = null;
        } else {
            _annotationIntrospector = _config.getAnnotationIntrospector();
        }
        _classInfo = classDef;
        _properties = props;
    }
    
    protected BasicBeanDescription(POJOPropertiesCollector coll)
    {
        this(coll, coll.getType(), coll.getClassDef());
        _objectIdInfo = coll.getObjectIdInfo();
    }

    /**
     * Factory method to use for constructing an instance to use for building
     * deserializers.
     */
    public static BasicBeanDescription forDeserialization(POJOPropertiesCollector coll) {
        return new BasicBeanDescription(coll);
    }

    /**
     * Factory method to use for constructing an instance to use for building
     * serializers.
     */
    public static BasicBeanDescription forSerialization(POJOPropertiesCollector coll) {
        return new BasicBeanDescription(coll);
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

    protected List<BeanPropertyDefinition> _properties() {
        if (_properties == null) {
            _properties = _propCollector.getProperties();
        }
        return _properties;
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
        Iterator<BeanPropertyDefinition> it = _properties().iterator();
        while (it.hasNext()) {
            BeanPropertyDefinition prop = it.next();
            if (prop.getName().equals(propName)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public boolean addProperty(BeanPropertyDefinition def)
    {
        // first: ensure we do not have such property
        if (hasProperty(def.getFullName())) {
            return false;
        }
        _properties().add(def);
        return true;
    }
    
    /**
     * @since 2.6
     */
    public boolean hasProperty(PropertyName name) {
        return findProperty(name) != null;
    }
    
    /**
     * @since 2.6
     */
    public BeanPropertyDefinition findProperty(PropertyName name)
    {
        for (BeanPropertyDefinition prop : _properties()) {
            if (prop.hasName(name)) {
                return prop;
            }
        }
        return null;
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
        return _properties();
    }

    @Override
    public AnnotatedMethod findJsonValueMethod() {
        return (_propCollector == null) ? null
                : _propCollector.getJsonValueMethod();
    }

    @Override
    public Set<String> getIgnoredPropertyNames() {
        Set<String> ign = (_propCollector == null) ? null
                : _propCollector.getIgnoredPropertyNames();
        if (ign == null) {
            return Collections.emptySet();
        }
        return ign;
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
    @Deprecated // since 2.7
    public TypeBindings bindingsForBeanType() {
        return _type.getBindings();
    }

    @Override
    @Deprecated // since 2.8
    public JavaType resolveType(java.lang.reflect.Type jdkType) {
        if (jdkType == null) {
            return null;
        }
        return _config.getTypeFactory().constructType(jdkType, _type.getBindings());
    }

    @Override
    public AnnotatedConstructor findDefaultConstructor() {
        return _classInfo.getDefaultConstructor();
    }

    @Override
    public AnnotatedMethod findAnySetter() throws IllegalArgumentException
    {
        AnnotatedMethod anySetter = (_propCollector == null) ? null
                : _propCollector.getAnySetterMethod();
        if (anySetter != null) {
            /* Also, let's be somewhat strict on how field name is to be
             * passed; String, Object make sense, others not
             * so much.
             */
            /* !!! 18-May-2009, tatu: how about enums? Can add support if
             *  requested; easy enough for devs to add support within
             *  method.
             */
            Class<?> type = anySetter.getRawParameterType(0);
            if (type != String.class && type != Object.class) {
                throw new IllegalArgumentException("Invalid 'any-setter' annotation on method "+anySetter.getName()+"(): first argument not of type String or Object, but "+type.getName());
            }
        }
        return anySetter;
    }

    @Override
    public Map<Object, AnnotatedMember> findInjectables() {
        if (_propCollector != null) {
            return _propCollector.getInjectables();
        }
        return Collections.emptyMap();
    }

    @Override
    public List<AnnotatedConstructor> getConstructors() {
        return _classInfo.getConstructors();
    }

    @Override
    public Object instantiateBean(boolean fixAccess) {
        AnnotatedConstructor ac = _classInfo.getDefaultConstructor();
        if (ac == null) {
            return null;
        }
        if (fixAccess) {
            ac.fixAccess(_config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
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
        // 15-Apr-2016, tatu: Let's check both per-type defaults and annotations; per-type
        //   defaults having higher precedence, so start with that
        if (_annotationIntrospector != null) {
            JsonFormat.Value v = _annotationIntrospector.findFormat(_classInfo);
            if (v != null) {
                if (defValue == null) {
                    defValue = v;
                } else {
                    defValue = defValue.withOverrides(v);
                }
            }
        }
        JsonFormat.Value v = _config.getDefaultPropertyFormat(_classInfo.getRawType());
        if (v != null) {
            if (defValue == null) {
                defValue = v;
            } else {
                defValue = defValue.withOverrides(v);
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
    public JsonInclude.Value findPropertyInclusion(JsonInclude.Value defValue) {
        if (_annotationIntrospector != null) {
            JsonInclude.Value incl = _annotationIntrospector.findPropertyInclusion(_classInfo);
            if (incl != null) {
                return (defValue == null) ? incl : defValue.withOverrides(incl);
            }
        }
        return defValue;
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
        AnnotatedMember anyGetter = (_propCollector == null) ? null
                : _propCollector.getAnyGetter();
        if (anyGetter != null) {
            /* For now let's require a Map; in future can add support for other
             * types like perhaps Iterable<Map.Entry>?
             */
            Class<?> type = anyGetter.getRawType();
            if (!Map.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("Invalid 'any-getter' annotation on method "+anyGetter.getName()+"(): return type is not instance of java.util.Map");
            }
        }
        return anyGetter;
    }

    @Override
    public AnnotatedMember findAnySetterField() throws IllegalArgumentException {
        AnnotatedMember anySetter = (_propCollector == null) ? null : _propCollector.getAnySetterField();
		if (anySetter != null) {
			/*
			 * For now let's require a Map; in future can add support for other
			 * types like perhaps Iterable<Map.Entry>?
			 */
			Class<?> type = anySetter.getRawType();
			if (!Map.class.isAssignableFrom(type)) {
				throw new IllegalArgumentException("Invalid 'any-setter' annotation on field " + anySetter.getName()
				        + "(): type is not instance of java.util.Map");
			}
		}
		return anySetter;
	}

    @Override
    public Map<String,AnnotatedMember> findBackReferenceProperties()
    {
        HashMap<String,AnnotatedMember> result = null;
//        boolean hasIgnored = (_ignoredPropertyNames != null);

        for (BeanPropertyDefinition property : _properties()) {
            /* 23-Sep-2014, tatu: As per [databind#426], we _should_ try to avoid
             *   calling accessor, as it triggers exception from seeming conflict.
             *   But the problem is that _ignoredPropertyNames here only contains
             *   ones ignored on per-property annotations, but NOT class annotations...
             *   so commented out part does not work, alas
             */
            /*
            if (hasIgnored && _ignoredPropertyNames.contains(property.getName())) {
                continue;
            }
            */
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
            // 24-Oct-2016, tatu: Better ensure it only takes 1 arg, no matter what
            if (isFactoryMethod(am) && am.getParameterCount() == 1) {
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
         * (i.e. allowed to be sub-class, although usually is the same class)
         */
        Class<?> rt = am.getRawReturnType();
        if (!getBeanClass().isAssignableFrom(rt)) {
            return false;
        }
        /* Also: must be a recognized factory method, meaning:
         * (a) marked with @JsonCreator annotation, or
         * (b) "valueOf" (at this point, need not be public)
         */
        if (_annotationIntrospector.hasCreatorAnnotation(am)) {
            return true;
        }
        final String name = am.getName();
        // 24-Oct-2016, tatu: As per [databind#1429] must ensure takes exactly one arg
        if ("valueOf".equals(name)) {
            if (am.getParameterCount() == 1) {
                return true;
            }
        }
        // [databind#208] Also accept "fromString()", if takes String or CharSequence
        if ("fromString".equals(name)) {
            if (am.getParameterCount() == 1) {
                Class<?> cls = am.getRawParameterType(0);
                if (cls == String.class || CharSequence.class.isAssignableFrom(cls)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @deprecated since 2.8
     */
    @Deprecated // since 2.8, not used at least since 2.7
    protected PropertyName _findCreatorPropertyName(AnnotatedParameter param)
    {
        PropertyName name = _annotationIntrospector.findNameForDeserialization(param);
        if (name == null || name.isEmpty()) {
            String str = _annotationIntrospector.findImplicitPropertyName(param);
            if (str != null && !str.isEmpty()) {
                name = PropertyName.construct(str);
            }
        }
        return name;
    }

    /*
    /**********************************************************
    /* Introspection for deserialization, other
    /**********************************************************
     */

    @Override
    public Class<?> findPOJOBuilder() {
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

    @Override
    public String findClassDescription() {
        return (_annotationIntrospector == null) ?
                null : _annotationIntrospector.findClassDescription(_classInfo);
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
     *
     * @deprecated Since 2.7.2, does not seem to be used?
     */
    @Deprecated
    public LinkedHashMap<String,AnnotatedField> _findPropertyFields(
            Collection<String> ignoredProperties, boolean forSerialization)
    {
        LinkedHashMap<String,AnnotatedField> results = new LinkedHashMap<String,AnnotatedField>();
        for (BeanPropertyDefinition property : _properties()) {
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
        if (converterClass == Converter.None.class || ClassUtil.isBogusClass(converterClass)) {
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
