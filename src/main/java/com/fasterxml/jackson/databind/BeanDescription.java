package com.fasterxml.jackson.databind;

import java.util.*;

import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.util.Annotations;

/**
 * Basic container for information gathered by {@link ClassIntrospector} to
 * help in constructing serializers and deserializers.
 * Note that the main implementation type is
 * {@link com.fasterxml.jackson.databind.introspect.BasicBeanDescription},
 * meaning that it is safe to upcast to this type.
 * 
 * @author tatu
 */
public abstract class BeanDescription
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Bean type information, including raw class and possible
     * * generics information
     */
    protected final JavaType _type;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected BeanDescription(JavaType type)
    {
    	_type = type;
    }

    /*
    /**********************************************************
    /* Simple accesors
    /**********************************************************
     */

    /**
     * Method for accessing declared type of bean being introspected,
     * including full generic type information (from declaration)
     */
    public JavaType getType() { return _type; }

    public Class<?> getBeanClass() { return _type.getRawClass(); }

    public abstract AnnotatedClass getClassInfo();
    
    public abstract boolean hasKnownClassAnnotations();

    /**
     * Accessor for type bindings that may be needed to fully resolve
     * types of member object, such as return and argument types of
     * methods and constructors, and types of fields.
     */
    public abstract TypeBindings bindingsForBeanType();

    /**
     * Method for resolving given JDK type, using this bean as the
     * generic type resolution context.
     */
    public abstract JavaType resolveType(java.lang.reflect.Type jdkType);
    
    /**
     * Method for accessing collection of annotations the bean
     * class has.
     */
    public abstract Annotations getClassAnnotations();
    
    /*
    /**********************************************************
    /* Basic API for finding properties, related
    /**********************************************************
     */
    
    /**
     * @return Ordered Map with logical property name as key, and
     *    matching getter method as value.
     */
    public abstract List<BeanPropertyDefinition> findProperties();

    public abstract Map<Object, AnnotatedMember> findInjectables();
    
    public abstract AnnotatedMethod findAnyGetter();

    public abstract AnnotatedMethod findAnySetter();

    public abstract AnnotatedMethod findJsonValueMethod();

    public abstract AnnotatedConstructor findDefaultConstructor();
    
    public abstract Set<String> getIgnoredPropertyNames();
}
