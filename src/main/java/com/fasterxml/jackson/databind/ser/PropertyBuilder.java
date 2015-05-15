package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;
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
    final protected SerializationConfig _config;
    final protected BeanDescription _beanDesc;

    /**
     * Default inclusion mode for properties of the POJO for which
     * properties are collected; possibly overridden on
     * per-property basis.
     */
    final protected JsonInclude.Include _defaultInclusion;

    final protected AnnotationIntrospector _annotationIntrospector;

    /**
     * If a property has serialization inclusion value of
     * {@link Inclusion#ALWAYS}, we need to know the default
     * value of the bean, to know if property value equals default
     * one.
     */
    protected Object _defaultBean;

    public PropertyBuilder(SerializationConfig config, BeanDescription beanDesc)
    {
        _config = config;
        _beanDesc = beanDesc;
        _defaultInclusion = beanDesc.findSerializationInclusion(config.getSerializationInclusion());
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
            /* 03-Sep-2010, tatu: This is somehow related to [JACKSON-356], but I don't completely
             *   yet understand how pieces fit together. Still, better to be explicit than rely on
             *   NPE to indicate an issue...
             */
            if (ct == null) {
                throw new IllegalStateException("Problem trying to create BeanPropertyWriter for property '"
                        +propDef.getName()+"' (of type "+_beanDesc.getType()+"); serialization type "+serializationType+" has no content");
            }
            serializationType = serializationType.withContentTypeHandler(contentTypeSer);
            ct = serializationType.getContentType();
        }
        
        Object valueToSuppress = null;
        boolean suppressNulls = false;

        JsonInclude.Include inclusion = propDef.findInclusion();
        if (inclusion == null) {
            inclusion = _defaultInclusion;
        }
        if (inclusion != null) {
            switch (inclusion) {
            case NON_DEFAULT:
                valueToSuppress = getDefaultValue(propDef.getName(), am);
                if (valueToSuppress == null) {
                    suppressNulls = true;
                } else {
                    // [JACKSON-531]: Allow comparison of arrays too...
                    if (valueToSuppress.getClass().isArray()) {
                        valueToSuppress = ArrayBuilders.getArrayComparator(valueToSuppress);
                    }
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
                // we may still want to suppress empty collections, as per [JACKSON-254]:
                if (declaredType.isContainerType()
                        && !_config.isEnabled(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)) {
                    valueToSuppress = BeanPropertyWriter.MARKER_FOR_EMPTY;
                }
                break;
            }
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
    {
        // [JACKSON-120]: Check to see if serialization type is fixed
        Class<?> serClass = _annotationIntrospector.findSerializationType(a);
        if (serClass != null) {
            // Must be a super type to be usable
            Class<?> rawDeclared = declaredType.getRawClass();
            if (serClass.isAssignableFrom(rawDeclared)) {
                declaredType = declaredType.widenBy(serClass);
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
                /* 03-Dec-2010, tatu: Actually, ugh, to resolve [JACKSON-415] may further relax this
                 *   and actually accept subtypes too for serialization. Bit dangerous in theory
                 *   but need to trust user here...
                 */
                declaredType = _config.constructSpecializedType(declaredType, serClass);
            }
            useStaticTyping = true;
        }

        // Should not have to do static method but...
        JavaType secondary = BasicSerializerFactory.modifySecondaryTypesByAnnotation(_config, a, declaredType);
        if (secondary != declaredType) {
            useStaticTyping = true;
            declaredType = secondary;
        }
        
        /* [JACKSON-114]: if using static typing, declared type is known
         * to be the type...
         */
        JsonSerialize.Typing typing = _annotationIntrospector.findSerializationTyping(a);
        if (typing != null && typing != JsonSerialize.Typing.DEFAULT_TYPING) {
            useStaticTyping = (typing == JsonSerialize.Typing.STATIC);
        }
        return useStaticTyping ? declaredType : null;
    }

    /*
    /**********************************************************
    /* Helper methods for default value handling
    /**********************************************************
     */

    protected Object getDefaultBean()
    {
        if (_defaultBean == null) {
            /* If we can fix access rights, we should; otherwise non-public
             * classes or default constructor will prevent instantiation
             */
            _defaultBean = _beanDesc.instantiateBean(_config.canOverrideAccessModifiers());
            if (_defaultBean == null) {
                Class<?> cls = _beanDesc.getClassInfo().getAnnotated();
                throw new IllegalArgumentException("Class "+cls.getName()+" has no default constructor; can not instantiate default bean value to support 'properties=JsonSerialize.Inclusion.NON_DEFAULT' annotation");
            }
        }
        return _defaultBean;
    }

    protected Object getDefaultValue(String name, AnnotatedMember member)
    {
        Object defaultBean = getDefaultBean();
        try {
            return member.getValue(defaultBean);
        } catch (Exception e) {
            return _throwWrapped(e, name, defaultBean);
        }
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
