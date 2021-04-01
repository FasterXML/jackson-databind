package com.fasterxml.jackson.databind.introspect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
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
    private final static Class<?>[] NO_VIEWS = new Class<?>[0];

    /*
    /**********************************************************************
    /* General configuration
    /**********************************************************************
     */

    /**
     * We will hold a reference to the collector in cases where
     * information is lazily accessed and constructed; properties
     * are only accessed when they are actually needed.
     */
    final protected POJOPropertiesCollector _propCollector;

    final protected MapperConfig<?> _config;

    final protected AnnotationIntrospector _intr;

    /*
    /**********************************************************************
    /* Information about type itself
    /**********************************************************************
     */

    /**
     * Information collected about the class introspected.
     */
    final protected AnnotatedClass _classInfo;

    protected Class<?>[] _defaultViews;

    protected boolean _defaultViewsResolved;

    /*
    /**********************************************************************
    /* Member information
    /**********************************************************************
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
    /**********************************************************************
    /* Lazily accessed results of introspection, cached for reuse
    /**********************************************************************
     */

    /**
     * Results of introspecting `@JsonFormat` configuration for class, if any.
     *
     * @since 3.0
     */
    protected transient JsonFormat.Value _classFormat;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected BasicBeanDescription(POJOPropertiesCollector coll,
            JavaType type, AnnotatedClass classDef)
    {
        super(type);
        _propCollector = coll;
        _config = Objects.requireNonNull(coll.getConfig());
        // NOTE: null config only for some pre-constructed types
        _intr = (_config == null) ? NopAnnotationIntrospector.nopInstance()
                : _config.getAnnotationIntrospector();
        _classInfo = classDef;
    }

    /**
     * Alternate constructor used in cases where property information is not needed,
     * only class info.
     */
    protected BasicBeanDescription(MapperConfig<?> config, JavaType type, AnnotatedClass classDef)
    {
        super(type);
        _propCollector = null;
        _config = Objects.requireNonNull(config);
        _intr = _config.getAnnotationIntrospector();
        _classInfo = classDef;
        _properties = Collections.<BeanPropertyDefinition>emptyList();
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
        return new BasicBeanDescription(config, type, ac);
    }

    protected List<BeanPropertyDefinition> _properties() {
        if (_properties == null) {
            _properties = _propCollector.getProperties();
        }
        return _properties;
    }

    /*
    /**********************************************************************
    /* Limited modifications by core databind functionality
    /**********************************************************************
     */

    /**
     * Method that can be used to prune unwanted properties, during
     * construction of serializers and deserializers.
     * Use with utmost care, if at all...
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

    public boolean hasProperty(PropertyName name) {
        return findProperty(name) != null;
    }

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
    /**********************************************************************
    /* Simple accessors from BeanDescription
    /**********************************************************************
     */

    @Override
    public AnnotatedClass getClassInfo() { return _classInfo; }

    @Override
    public ObjectIdInfo getObjectIdInfo() { return  _objectIdInfo; }

    @Override
    public List<BeanPropertyDefinition> findProperties() {
        return _properties();
    }

    @Override // since 2.12
    public AnnotatedMember findJsonKeyAccessor() {
        return (_propCollector == null) ? null
                : _propCollector.getJsonKeyAccessor();
    }

    @Override // since 2.9
    public AnnotatedMember findJsonValueAccessor() {
        return (_propCollector == null) ? null
                : _propCollector.getJsonValueAccessor();
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
    public AnnotatedConstructor findDefaultConstructor() {
        return _classInfo.getDefaultConstructor();
    }

    @Override
    public AnnotatedMember findAnySetterAccessor() throws IllegalArgumentException
    {
        if (_propCollector != null) {
            AnnotatedMethod anyMethod = _propCollector.getAnySetterMethod();
            if (anyMethod != null) {
                // Also, let's be somewhat strict on how field name is to be
                // passed; String, Object make sense, others not so much.
    
                /* !!! 18-May-2009, tatu: how about enums? Can add support if
                 *  requested; easy enough for devs to add support within method.
                 */
                Class<?> type = anyMethod.getRawParameterType(0);
                if ((type != String.class) && (type != Object.class)) {
                    throw new IllegalArgumentException(String.format(
"Invalid 'any-setter' annotation on method '%s()': first argument not of type String or Object, but %s",
anyMethod.getName(), type.getName()));
                }
                return anyMethod;
            }
            AnnotatedMember anyField = _propCollector.getAnySetterField();
            if (anyField != null) {
                // For now let's require a Map; in future can add support for other
                // types like perhaps Iterable<Map.Entry>?
                Class<?> type = anyField.getRawType();
                if (!Map.class.isAssignableFrom(type)) {
                    throw new IllegalArgumentException(String.format(
"Invalid 'any-setter' annotation on field '%s': type is not instance of java.util.Map",
anyField.getName()));
                }
                return anyField;
            }
        }
        return null;
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
            return ac.call();
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            ClassUtil.throwIfError(t);
            ClassUtil.throwIfRTE(t);
            throw new IllegalArgumentException("Failed to instantiate bean of type "+ClassUtil.nameOf(_classInfo.getAnnotated())
                    +": ("+t.getClass().getName()+") "+ClassUtil.exceptionMessage(t), t);
        }
    }

    /*
    /**********************************************************************
    /* Simple accessors, extended
    /**********************************************************************
     */

    @Override
    public AnnotatedMethod findMethod(String name, Class<?>[] paramTypes) {
        return _classInfo.findMethod(name, paramTypes);
    }

    /*
    /**********************************************************************
    /* General per-class annotation introspection
    /**********************************************************************
     */

    @Deprecated // since 3.0
    @Override
    public JsonFormat.Value findExpectedFormat()
    {
        JsonFormat.Value v = _classFormat;
        if (v == null) {
            // 18-Apr-2018, tatu: Bit unclean but apparently `_config` is `null` for
            //   a small set of pre-discovered simple types that `BasicClassIntrospector`
            //   may expose. If so, nothing we can do
            v = (_config == null) ? null
                    : _intr.findFormat(_config, _classInfo);
            if (v == null) {
                v = JsonFormat.Value.empty();
            }
            _classFormat = v;
        }
        return v;
    }

    @Override
    public JsonFormat.Value findExpectedFormat(Class<?> baseType)
    {
        JsonFormat.Value v0 = _classFormat;
        if (v0 == null) { // copied from above
            v0 = (_config == null) ? null
                    : _intr.findFormat(_config, _classInfo);
            if (v0 == null) {
                v0 = JsonFormat.Value.empty();
            }
            _classFormat = v0;
        }
        JsonFormat.Value v1 = _config.getDefaultPropertyFormat(baseType);
        if (v1 == null) {
            return v0;
        }
        return JsonFormat.Value.merge(v0, v1);
    }

    @Override
    public Class<?>[] findDefaultViews()
    {
        if (!_defaultViewsResolved) {
            _defaultViewsResolved = true;
            Class<?>[] def = _intr.findViews(_config, _classInfo);
            // one more twist: if default inclusion disabled, need to force empty set of views
            if (def == null) {
                if (!_config.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)) {
                    def = NO_VIEWS;
                }
            }
            _defaultViews = def;
        }
        return _defaultViews;
    }

    /*
    /**********************************************************************
    /* Introspection for serialization
    /**********************************************************************
     */

    @Override
    public Converter<Object,Object> findSerializationConverter()
    {
        return _createConverter(_intr.findSerializationConverter(_config, _classInfo));
    }

    /**
     * Method for determining whether null properties should be written
     * out for a Bean of introspected type. This is based on global
     * feature (lowest priority, passed as argument)
     * and per-class annotation (highest priority).
     */
    @Override
    public JsonInclude.Value findPropertyInclusion(JsonInclude.Value defValue) {
        JsonInclude.Value incl = _intr.findPropertyInclusion(_config, _classInfo);
        if (incl != null) {
            return (defValue == null) ? incl : defValue.withOverrides(incl);
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
        if (_propCollector != null) {
            AnnotatedMember anyGetter = _propCollector.getAnyGetterMethod();
            if (anyGetter != null) {
                // For now let's require a Map; in future can add support for other
                // types like perhaps Iterable<Map.Entry>?
                Class<?> type = anyGetter.getRawType();
                if (!Map.class.isAssignableFrom(type)) {
                    throw new IllegalArgumentException(String.format(
                            "Invalid 'any-getter' annotation on method %s(): return type is not instance of java.util.Map",
                            anyGetter.getName()));
                }
                return anyGetter;
            }

            AnnotatedMember anyField = _propCollector.getAnyGetterField();
            if (anyField != null) {
                // For now let's require a Map; in future can add support for other
                // types like perhaps Iterable<Map.Entry>?
                Class<?> type = anyField.getRawType();
                if (!Map.class.isAssignableFrom(type)) {
                    throw new IllegalArgumentException(String.format(
                            "Invalid 'any-getter' annotation on field '%s': type is not instance of java.util.Map",
                            anyField.getName()));
                }
                return anyField;
            }
        }
        return null;
    }

    @Override
    public List<BeanPropertyDefinition> findBackReferences()
    {
        List<BeanPropertyDefinition> result = null;
        HashSet<String> names = null;
        for (BeanPropertyDefinition property : _properties()) {
            AnnotationIntrospector.ReferenceProperty refDef = property.findReferenceType();
            if ((refDef == null) || !refDef.isBackReference()) {
                continue;
            }
            final String refName = refDef.getName();
            if (result == null) {
                result = new ArrayList<BeanPropertyDefinition>();
                names = new HashSet<>();
                names.add(refName);
            } else {
                if (!names.add(refName)) {
                    throw new IllegalArgumentException("Multiple back-reference properties with name "+ClassUtil.name(refName));
                }
            }
            result.add(property);
        }
        return result;
    }

    /*
    /**********************************************************************
    /* Introspection for deserialization, factories
    /**********************************************************************
     */

    @Override
    public List<AnnotatedMethod> getFactoryMethods()
    {
        // must filter out anything that clearly is not a factory method
        List<AnnotatedMethod> candidates = _classInfo.getFactoryMethods();
        if (candidates.isEmpty()) {
            return candidates;
        }
        List<AnnotatedMethod> result = null;
        for (AnnotatedMethod am : candidates) {
            if (isFactoryMethod(am)) {
                if (result == null) {
                    result = new ArrayList<AnnotatedMethod>();
                }
                result.add(am);
            }
        }
        if (result == null) {
            return Collections.emptyList();
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
        for (AnnotatedMethod am : _classInfo.getFactoryMethods()) {
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
        // First: return type must be compatible with the introspected class
        // (i.e. allowed to be sub-class, although usually is the same class)
        Class<?> rt = am.getRawReturnType();
        if (!getBeanClass().isAssignableFrom(rt)) {
            return false;
        }
        /* Also: must be a recognized factory method, meaning:
         * (a) marked with @JsonCreator annotation, or
         * (b) "valueOf" (at this point, need not be public)
         */
        JsonCreator.Mode mode = _intr.findCreatorAnnotation(_config, am);
        if ((mode != null) && (mode != JsonCreator.Mode.DISABLED)) {
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

    /*
    /**********************************************************************
    /* Introspection for deserialization, other
    /**********************************************************************
     */

    @Override
    public Class<?> findPOJOBuilder() {
        return _intr.findPOJOBuilder(_config, _classInfo);
    }

    @Override
    public JsonPOJOBuilder.Value findPOJOBuilderConfig()
    {
        return _intr.findPOJOBuilderConfig(_config, _classInfo);
    }

    @Override
    public Converter<Object,Object> findDeserializationConverter()
    {
        return _createConverter(_intr
                        .findDeserializationConverter(_config, _classInfo));
    }

    @Override
    public String findClassDescription() {
        return _intr.findClassDescription(_config, _classInfo);
    }

    /*
    /**********************************************************************
    /* Helper methods, other
    /**********************************************************************
     */
    
    @SuppressWarnings("unchecked")
    protected Converter<Object,Object> _createConverter(Object converterDef)
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
