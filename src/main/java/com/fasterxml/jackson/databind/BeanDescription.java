package com.fasterxml.jackson.databind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
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
    
    /**
     * Method for checking whether class being described has any
     * annotations recognized by registered annotation introspector.
     */
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
    /* Basic API for finding properties
    /**********************************************************
     */
    
    /**
     * @return Ordered Map with logical property name as key, and
     *    matching getter method as value.
     */
    public abstract List<BeanPropertyDefinition> findProperties();
    
    /**
     * Method for locating all back-reference properties (setters, fields) bean has
     */
    public abstract Map<String,AnnotatedMember> findBackReferenceProperties();

    public abstract Set<String> getIgnoredPropertyNames();
    
    /*
    /**********************************************************
    /* Basic API for finding creator members
    /**********************************************************
     */
    
    public abstract List<AnnotatedConstructor> getConstructors();
    
    public abstract List<AnnotatedMethod> getFactoryMethods();
    
    /**
     * Method that will locate the no-arg constructor for this class,
     * if it has one, and that constructor has not been marked as
     * ignorable.
     */
    public abstract AnnotatedConstructor findDefaultConstructor();

    /**
     * Method that can be called to locate a single-arg constructor that
     * takes specified exact type (will not accept supertype constructors)
     *
     * @param argTypes Type(s) of the argument that we are looking for
     */
    public abstract Constructor<?> findSingleArgConstructor(Class<?>... argTypes);

    /**
     * Method that can be called to find if introspected class declares
     * a static "valueOf" factory method that returns an instance of
     * introspected type, given one of acceptable types.
     *
     * @param expArgTypes Types that the matching single argument factory
     *   method can take: will also accept super types of these types
     *   (ie. arg just has to be assignable from expArgType)
     */
    public abstract Method findFactoryMethod(Class<?>... expArgTypes);

    /*
    /**********************************************************
    /* Basic API for finding property accessors
    /**********************************************************
     */
    
    public abstract AnnotatedMethod findAnyGetter();

    /**
     * Method used to locate the method of introspected class that
     * implements {@link com.fasterxml.jackson.annotation.JsonAnySetter}. If no such method exists
     * null is returned. If more than one are found, an exception
     * is thrown.
     * Additional checks are also made to see that method signature
     * is acceptable: needs to take 2 arguments, first one String or
     * Object; second any can be any type.
     */
    public abstract AnnotatedMethod findAnySetter();

    /**
     * Method for locating the getter method that is annotated with
     * {@link com.fasterxml.jackson.annotation.JsonValue} annotation,
     * if any. If multiple ones are found,
     * an error is reported by throwing {@link IllegalArgumentException}
     */
    public abstract AnnotatedMethod findJsonValueMethod();

    public abstract AnnotatedMethod findMethod(String name, Class<?>[] paramTypes);
    
    /*
    /**********************************************************
    /* Basic API, other
    /**********************************************************
     */

    public abstract Map<Object, AnnotatedMember> findInjectables();

    public abstract JsonInclude.Include findSerializationInclusion(JsonInclude.Include defValue);
    
    /**
     * Method called to create a "default instance" of the bean, currently
     * only needed for obtaining default field values which may be used for
     * suppressing serialization of fields that have "not changed".
     * 
     * @param fixAccess If true, method is allowed to fix access to the
     *   default constructor (to be able to call non-public constructor);
     *   if false, has to use constructor as is.
     *
     * @return Instance of class represented by this descriptor, if
     *   suitable default constructor was found; null otherwise.
     */
    public abstract Object instantiateBean(boolean fixAccess);
}
