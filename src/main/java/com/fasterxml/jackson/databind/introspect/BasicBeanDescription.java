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
    // since 2.9
    private final static Class<?>[] NO_VIEWS = new Class<?>[0];

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

    /*
    /**********************************************************
    /* Information about type itself
    /**********************************************************
     */

    /**
     * Information collected about the class introspected.
     */
    final protected AnnotatedClass _classInfo;

    /**
     * @since 2.9
     */
    protected Class<?>[] _defaultViews;

    /**
     * @since 2.9
     */
    protected boolean _defaultViewsResolved;

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

    @Override // since 2.12
    public AnnotatedMember findJsonKeyAccessor() {
        return (_propCollector == null) ? null
                : _propCollector.getJsonKeyAccessor();
    }

    @Override
    @Deprecated // since 2.9
    public AnnotatedMethod findJsonValueMethod() {
        return (_propCollector == null) ? null
                : _propCollector.getJsonValueMethod();
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
    @Deprecated // since 2.7
    public TypeBindings bindingsForBeanType() {
        return _type.getBindings();
    }

    @Override
    @Deprecated // since 2.8
    public JavaType resolveType(java.lang.reflect.Type jdkType) {
        // 06-Sep-2020, tatu: Careful wrt [databind#2846][databind#2821],
        //     call new method added in 2.12
        return _config.getTypeFactory().resolveMemberType(jdkType, _type.getBindings());
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
                if (!Map.class.isAssignableFrom(type)
                        && !JsonNode.class.isAssignableFrom(type)) {
                    throw new IllegalArgumentException(String.format(
"Invalid 'any-setter' annotation on field '%s': type is not instance of `java.util.Map` or `JsonNode`",
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
    public List<AnnotatedAndMetadata<AnnotatedConstructor, JsonCreator.Mode>> getConstructorsWithMode() {
        List<AnnotatedConstructor> allCtors = _classInfo.getConstructors();
        if (allCtors.isEmpty()) {
            return Collections.emptyList();
        }
        List<AnnotatedAndMetadata<AnnotatedConstructor, JsonCreator.Mode>> result = new ArrayList<>();
        for (AnnotatedConstructor ctor : allCtors) {
            JsonCreator.Mode mode = _annotationIntrospector.findCreatorAnnotation(_config, ctor);
            if (mode == JsonCreator.Mode.DISABLED) {
                continue;
            }
            result.add(AnnotatedAndMetadata.of(ctor, mode));
        }
        return result;
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
            throw new IllegalArgumentException("Failed to instantiate bean of type "
                    +_classInfo.getAnnotated().getName()+": ("+t.getClass().getName()+") "
                    +ClassUtil.exceptionMessage(t), t);
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

    @Override // since 2.9
    public Class<?>[] findDefaultViews()
    {
        if (!_defaultViewsResolved) {
            _defaultViewsResolved = true;
            Class<?>[] def = (_annotationIntrospector == null) ? null
                    : _annotationIntrospector.findViews(_classInfo);
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

    @Deprecated // since 2.9
    @Override
    public Map<String,AnnotatedMember> findBackReferenceProperties()
    {
        List<BeanPropertyDefinition> props = findBackReferences();
        if (props == null) {
            return null;
        }
        Map<String,AnnotatedMember> result = new HashMap<>();
        for (BeanPropertyDefinition prop : props) {
            result.put(prop.getName(), prop.getMutator());
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

    @Override // since 2.13
    public List<AnnotatedAndMetadata<AnnotatedMethod, JsonCreator.Mode>> getFactoryMethodsWithMode()
    {
        List<AnnotatedMethod> candidates = _classInfo.getFactoryMethods();
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        List<AnnotatedAndMetadata<AnnotatedMethod, JsonCreator.Mode>> result = null;
        for (AnnotatedMethod am : candidates) {
            AnnotatedAndMetadata<AnnotatedMethod, JsonCreator.Mode> match
                = findFactoryMethodMetadata(am);
            if (match != null) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(match);
            }
        }
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    @Override
    @Deprecated // since 2.13
    public Constructor<?> findSingleArgConstructor(Class<?>... argTypes)
    {
        for (AnnotatedConstructor ac : _classInfo.getConstructors()) {
            // This list is already filtered to only include accessible
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
    @Deprecated // since 2.13
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
         * (b) 1-argument "valueOf" (at this point, need not be public), or
         * (c) 1-argument "fromString()" AND takes {@code String} as the argument
         */
        JsonCreator.Mode mode = _annotationIntrospector.findCreatorAnnotation(_config, am);
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

    // @since 2.13
    protected AnnotatedAndMetadata<AnnotatedMethod, JsonCreator.Mode> findFactoryMethodMetadata(AnnotatedMethod am)
    {
        // First: return type must be compatible with the introspected class
        // (i.e. allowed to be sub-class, although usually is the same class)
        Class<?> rt = am.getRawReturnType();
        if (!getBeanClass().isAssignableFrom(rt)) {
            return null;
        }
        // Also: must be a recognized factory method, meaning:
        // (a) marked with @JsonCreator annotation, or
        // (b) 1-argument "valueOf" (at this point, need not be public), or
        // (c) 1-argument "fromString()" AND takes {@code String} as the argument
        JsonCreator.Mode mode = _annotationIntrospector.findCreatorAnnotation(_config, am);
        if (mode != null) {
            if (mode == JsonCreator.Mode.DISABLED) {
                return null;
            }
            return AnnotatedAndMetadata.of(am, mode);
        }
        final String name = am.getName();
        // 24-Oct-2016, tatu: As per [databind#1429] must ensure takes exactly one arg
        if ("valueOf".equals(name)) {
            if (am.getParameterCount() == 1) {
                return AnnotatedAndMetadata.of(am, mode);
            }
        }
        // [databind#208] Also accept "fromString()", if takes String or CharSequence
        if ("fromString".equals(name)) {
            if (am.getParameterCount() == 1) {
                Class<?> cls = am.getRawParameterType(0);
                if (cls == String.class || CharSequence.class.isAssignableFrom(cls)) {
                    return AnnotatedAndMetadata.of(am, mode);
                }
            }
        }
        return null;
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
