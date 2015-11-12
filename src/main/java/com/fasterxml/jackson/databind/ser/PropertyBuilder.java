package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.util.*;

/**
 * Helper class for {@link BeanSerializerFactory} that is used to
 * construct {@link BeanPropertyWriter} instances. Can be sub-classed
 * to change behavior.
 */
public class PropertyBuilder
{
    // @since 2.7
    private final static Object NO_DEFAULT_MARKER = Boolean.FALSE;
    
    final protected SerializationConfig _config;
    final protected BeanDescription _beanDesc;

    /**
     * Default inclusion mode for properties of the POJO for which
     * properties are collected; possibly overridden on
     * per-property basis.
     */
    final protected JsonInclude.Value _defaultInclusion;

    final protected AnnotationIntrospector _annotationIntrospector;

    /**
     * If a property has serialization inclusion value of
     * {@link com.fasterxml.jackson.annotation.JsonInclude.Include#NON_DEFAULT},
     * we may need to know the default value of the bean, to know if property value
     * equals default one.
     *<p>
     * NOTE: only used if enclosing class defines NON_DEFAULT, but NOT if it is the
     * global default OR per-property override.
     */
    protected Object _defaultBean;

    public PropertyBuilder(SerializationConfig config, BeanDescription beanDesc)
    {
        _config = config;
        _beanDesc = beanDesc;
        _defaultInclusion = beanDesc.findPropertyInclusion(config.getDefaultPropertyInclusion());
        _annotationIntrospector = _config.getAnnotationIntrospector();
    }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public Annotations getClassAnnotations() {
        return _beanDesc.getClassAnnotations();
    }

    /**
     * @param contentTypeSer Optional explicit type information serializer
     *    to use for contained values (only used for properties that are
     *    of container type)
     */
    protected BeanPropertyWriter buildWriter(SerializerProvider prov,
            BeanPropertyDefinition propDef, JavaType declaredType, JsonSerializer<?> ser,
            TypeSerializer typeSer, TypeSerializer contentTypeSer,
            AnnotatedMember am, boolean defaultUseStaticTyping)
        throws JsonMappingException
    {
        // do we have annotation that forces type to use (to declared type or its super type)?
        JavaType serializationType = findSerializationType(am, defaultUseStaticTyping, declaredType);

        // Container types can have separate type serializers for content (value / element) type
        if (contentTypeSer != null) {
            /* 04-Feb-2010, tatu: Let's force static typing for collection, if there is
             *    type information for contents. Should work well (for JAXB case); can be
             *    revisited if this causes problems.
             */
            if (serializationType == null) {
//                serializationType = TypeFactory.type(am.getGenericType(), _beanDesc.getType());
                serializationType = declaredType;
            }
            JavaType ct = serializationType.getContentType();
            // Not exactly sure why, but this used to occur; better check explicitly:
            if (ct == null) {
                throw new IllegalStateException("Problem trying to create BeanPropertyWriter for property '"
                        +propDef.getName()+"' (of type "+_beanDesc.getType()+"); serialization type "+serializationType+" has no content");
            }
            serializationType = serializationType.withContentTypeHandler(contentTypeSer);
            ct = serializationType.getContentType();
        }
        
        Object valueToSuppress = null;
        boolean suppressNulls = false;

        JsonInclude.Value inclV = _defaultInclusion.withOverrides(propDef.findInclusion());
        JsonInclude.Include inclusion = inclV.getValueInclusion();
        if (inclusion == JsonInclude.Include.USE_DEFAULTS) { // should not occur but...
            inclusion = JsonInclude.Include.ALWAYS;
        }

        /*
        JsonInclude.Include inclusion = propDef.findInclusion().getValueInclusion();
        if (inclusion == JsonInclude.Include.USE_DEFAULTS) { // since 2.6
            inclusion = _defaultInclusion;
            if (inclusion == null) {
                inclusion = JsonInclude.Include.ALWAYS;
            }
        }
        */

        switch (inclusion) {
        case NON_DEFAULT:
            // 11-Nov-2015, tatu: This is tricky because semantics differ between cases,
            //    so that if enclosing class has this, we may need to values of property,
            //    whereas for global defaults OR per-property overrides, we have more
            //    static definition. Sigh.
            // First: case of class specifying it; try to find POJO property defaults
            JavaType t = (serializationType == null) ? declaredType : serializationType;
            if (_defaultInclusion.getValueInclusion() == JsonInclude.Include.NON_DEFAULT) {
                valueToSuppress = getPropertyDefaultValue(propDef.getName(), am, t);
            } else {
                valueToSuppress = getDefaultValue(t);
            }
            if (valueToSuppress == null) {
                suppressNulls = true;
            } else {
                if (valueToSuppress.getClass().isArray()) {
                    valueToSuppress = ArrayBuilders.getArrayComparator(valueToSuppress);
                }
            }

            break;
        case NON_ABSENT: // new with 2.6, to support Guava/JDK8 Optionals
            // always suppress nulls
            suppressNulls = true;
            // and for referential types, also "empty", which in their case means "absent"
            if (declaredType.isReferenceType()) {
                valueToSuppress = BeanPropertyWriter.MARKER_FOR_EMPTY;
            }
            break;
        case NON_EMPTY:
            // always suppress nulls
            suppressNulls = true;
            // but possibly also 'empty' values:
            valueToSuppress = BeanPropertyWriter.MARKER_FOR_EMPTY;
            break;
        case NON_NULL:
            suppressNulls = true;
            // fall through
        case ALWAYS: // default
        default:
            // we may still want to suppress empty collections, as per [JACKSON-254]:
            if (declaredType.isContainerType()
                    && !_config.isEnabled(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)) {
                valueToSuppress = BeanPropertyWriter.MARKER_FOR_EMPTY;
            }
            break;
        }
        BeanPropertyWriter bpw = new BeanPropertyWriter(propDef,
                am, _beanDesc.getClassAnnotations(), declaredType,
                ser, typeSer, serializationType, suppressNulls, valueToSuppress);

        // How about custom null serializer?
        Object serDef = _annotationIntrospector.findNullSerializer(am);
        if (serDef != null) {
            bpw.assignNullSerializer(prov.serializerInstance(am, serDef));
        }
        // And then, handling of unwrapping
        NameTransformer unwrapper = _annotationIntrospector.findUnwrappingNameTransformer(am);
        if (unwrapper != null) {
            bpw = bpw.unwrappingWriter(unwrapper);
        }
        return bpw;
    }

    /*
    /**********************************************************
    /* Helper methods; annotation access
    /**********************************************************
     */

    /**
     * Method that will try to determine statically defined type of property
     * being serialized, based on annotations (for overrides), and alternatively
     * declared type (if static typing for serialization is enabled).
     * If neither can be used (no annotations, dynamic typing), returns null.
     */
    protected JavaType findSerializationType(Annotated a, boolean useStaticTyping, JavaType declaredType)
        throws JsonMappingException
    {
        JavaType secondary = _annotationIntrospector.refineSerializationType(_config, a, declaredType);
        // 11-Oct-2015, tatu: As of 2.7, not 100% sure following checks are needed. But keeping
        //    for now, just in case
        if (secondary != declaredType) {
            Class<?> serClass = secondary.getRawClass();
            // Must be a super type to be usable
            Class<?> rawDeclared = declaredType.getRawClass();
            if (serClass.isAssignableFrom(rawDeclared)) {
                ; // fine as is
            } else {
                /* 18-Nov-2010, tatu: Related to fixing [JACKSON-416], an issue with such
                 *   check is that for deserialization more specific type makes sense;
                 *   and for serialization more generic. But alas JAXB uses but a single
                 *   annotation to do both... Hence, we must just discard type, as long as
                 *   types are related
                 */
                if (!rawDeclared.isAssignableFrom(serClass)) {
                    throw new IllegalArgumentException("Illegal concrete-type annotation for method '"+a.getName()+"': class "+serClass.getName()+" not a super-type of (declared) class "+rawDeclared.getName());
                }
                /* 03-Dec-2010, tatu: Actually, ugh, we may need to further relax this
                 *   and actually accept subtypes too for serialization. Bit dangerous in theory
                 *   but need to trust user here...
                 */
            }
            useStaticTyping = true;
            declaredType = secondary;
        }
        // If using static typing, declared type is known to be the type...
        JsonSerialize.Typing typing = _annotationIntrospector.findSerializationTyping(a);
        if ((typing != null) && (typing != JsonSerialize.Typing.DEFAULT_TYPING)) {
            useStaticTyping = (typing == JsonSerialize.Typing.STATIC);
        }
        if (useStaticTyping) {
            // 11-Oct-2015, tatu: Make sure JavaType also "knows" static-ness...
            return declaredType.withStaticTyping();
            
        }
        return null;
    }

    /*
    /**********************************************************
    /* Helper methods for default value handling
    /**********************************************************
     */

    protected Object getDefaultBean()
    {
        Object def = _defaultBean;
        if (def == null) {
            /* If we can fix access rights, we should; otherwise non-public
             * classes or default constructor will prevent instantiation
             */
            def = _beanDesc.instantiateBean(_config.canOverrideAccessModifiers());
            if (def == null) {
                // 06-Nov-2015, tatu: As per [databind#998], do not fail.
                /*
                Class<?> cls = _beanDesc.getClassInfo().getAnnotated();
                throw new IllegalArgumentException("Class "+cls.getName()+" has no default constructor; can not instantiate default bean value to support 'properties=JsonSerialize.Inclusion.NON_DEFAULT' annotation");
                 */

                // And use a marker
                def = NO_DEFAULT_MARKER;
            }
            _defaultBean = def;
        }
        return (def == NO_DEFAULT_MARKER) ? null : _defaultBean;
    }

    /**
     * Accessor used to find out "default value" for given property, to use for
     * comparing values to serialize, to determine whether to exclude value from serialization with
     * inclusion type of {@link com.fasterxml.jackson.annotation.JsonInclude.Include#NON_EMPTY}.
     * This method is called when we specifically want to know default value within context
     * of a POJO, when annotation is within containing class, and not for property or
     * defined as global baseline.
     *<p>
     * Note that returning of pseudo-type 
     *
     * @since 2.7
     */
    protected Object getPropertyDefaultValue(String name, AnnotatedMember member,
            JavaType type)
    {
        Object defaultBean = getDefaultBean();
        if (defaultBean == null) {
            return getDefaultValue(type);
        }
        try {
            return member.getValue(defaultBean);
        } catch (Exception e) {
            return _throwWrapped(e, name, defaultBean);
        }
    }

    /**
     * Accessor used to find out "default value" to use for comparing values to
     * serialize, to determine whether to exclude value from serialization with
     * inclusion type of {@link com.fasterxml.jackson.annotation.JsonInclude.Include#NON_DEFAULT}.
     *<p>
     * Default logic is such that for primitives and wrapper types for primitives, expected
     * defaults (0 for `int` and `java.lang.Integer`) are returned; for Strings, empty String,
     * and for structured (Maps, Collections, arrays) and reference types, criteria
     * {@link com.fasterxml.jackson.annotation.JsonInclude.Include#NON_DEFAULT}
     * is used.
     *
     * @since 2.7
     */
    protected Object getDefaultValue(JavaType type)
    {
        // 06-Nov-2015, tatu: Returning null is fine for Object types; but need special
        //   handling for primitives since they are never passed as nulls.
        Class<?> cls = type.getRawClass();

        Class<?> prim = ClassUtil.primitiveType(cls);
        if (prim != null) {
            return ClassUtil.defaultValue(prim);
        }
        if (type.isContainerType() || type.isReferenceType()) {
            return JsonInclude.Include.NON_EMPTY;
        }
        if (cls == String.class) {
            return "";
        }
        return null;
    }
    
    /*
    /**********************************************************
    /* Helper methods for exception handling
    /**********************************************************
     */
    
    protected Object _throwWrapped(Exception e, String propName, Object defaultBean)
    {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof Error) throw (Error) t;
        if (t instanceof RuntimeException) throw (RuntimeException) t;
        throw new IllegalArgumentException("Failed to get property '"+propName+"' of default "+defaultBean.getClass().getName()+" instance");
    }
}
