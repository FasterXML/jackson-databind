package com.fasterxml.jackson.databind.cfg;

import java.text.DateFormat;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Immutable container class used to store simple configuration
 * settings. Since instances are fully immutable, instances can
 * be freely shared and used without synchronization.
 */
public final class BaseSettings
{
    /*
    /**********************************************************
    /* Configuration settings; introspection, related
    /**********************************************************
     */
    
    /**
     * Introspector used to figure out Bean properties needed for bean serialization
     * and deserialization. Overridable so that it is possible to change low-level
     * details of introspection, like adding new annotation types.
     */
    protected final ClassIntrospector _classIntrospector;

    /**
     * Introspector used for accessing annotation value based configuration.
     */
    protected final AnnotationIntrospector _annotationIntrospector;

    /**
     * Object used for determining whether specific property elements
     * (method, constructors, fields) can be auto-detected based on
     * their visibility (access modifiers). Can be changed to allow
     * different minimum visibility levels for auto-detection. Note
     * that this is the global handler; individual types (classes)
     * can further override active checker used (using
     * {@link JsonAutoDetect} annotation)
     */
    protected final VisibilityChecker<?> _visibilityChecker;

    /**
     * Custom property naming strategy in use, if any.
     */
    protected final PropertyNamingStrategy _propertyNamingStrategy;

    /**
     * Specific factory used for creating {@link JavaType} instances;
     * needed to allow modules to add more custom type handling
     * (mostly to support types of non-Java JVM languages)
     */
    protected final TypeFactory _typeFactory;

    /*
    /**********************************************************
    /* Configuration settings; type resolution
    /**********************************************************
     */

    /**
     * Type information handler used for "untyped" values (ones declared
     * to have type <code>Object.class</code>)
     */
    protected final TypeResolverBuilder<?> _typeResolverBuilder;
    
    /*
    /**********************************************************
    /* Configuration settings; other
    /**********************************************************
     */
    
    /**
     * Custom date format to use for de-serialization. If specified, will be
     * used instead of {@link com.fasterxml.jackson.databind.util.StdDateFormat}.
     *<p>
     * Note that the configured format object will be cloned once per
     * deserialization process (first time it is needed)
     */
    protected final DateFormat _dateFormat;

    /**
     * Object used for creating instances of handlers (serializers, deserializers,
     * type and type id resolvers), given class to instantiate. This is typically
     * used to do additional configuration (with dependency injection, for example)
     * beyond simply construction of instances; or to use alternative constructors.
     */
    protected final HandlerInstantiator _handlerInstantiator;
    
    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    public BaseSettings(ClassIntrospector ci, AnnotationIntrospector ai,
            VisibilityChecker<?> vc, PropertyNamingStrategy pns, TypeFactory tf,
            TypeResolverBuilder<?> typer, DateFormat dateFormat, HandlerInstantiator hi)
    {
        _classIntrospector = ci;
        _annotationIntrospector = ai;
        _visibilityChecker = vc;
        _propertyNamingStrategy = pns;
        _typeFactory = tf;
        _typeResolverBuilder = typer;
        _dateFormat = dateFormat;
        _handlerInstantiator = hi;
    }

    /*
    /**********************************************************
    /* Factory methods
    /**********************************************************
     */
    
    public BaseSettings withClassIntrospector(ClassIntrospector ci) {
        return new BaseSettings(ci, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator);
    }
    
    public BaseSettings withAnnotationIntrospector(AnnotationIntrospector ai) {
        return new BaseSettings(_classIntrospector, ai, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator);
    }

    public BaseSettings withInsertedAnnotationIntrospector(AnnotationIntrospector ai) {
        return withAnnotationIntrospector(AnnotationIntrospector.Pair.create(ai, _annotationIntrospector));
    }

    public BaseSettings withAppendedAnnotationIntrospector(AnnotationIntrospector ai) {
        return withAnnotationIntrospector(AnnotationIntrospector.Pair.create(_annotationIntrospector, ai));
    }
    
    public BaseSettings withVisibilityChecker(VisibilityChecker<?> vc) {
        return new BaseSettings(_classIntrospector, _annotationIntrospector, vc, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator);
    }

    public BaseSettings withVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility) {
        return new BaseSettings(_classIntrospector, _annotationIntrospector,
                _visibilityChecker.withVisibility(forMethod, visibility),
                _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator);
    }
    
    public BaseSettings withPropertyNamingStrategy(PropertyNamingStrategy pns) {
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _visibilityChecker, pns, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator);
    }

    public BaseSettings withTypeFactory(TypeFactory tf) {
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, tf,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator);
    }

    public BaseSettings withTypeResolverBuilder(TypeResolverBuilder<?> typer) {
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                typer, _dateFormat, _handlerInstantiator);
    }
    
    public BaseSettings withDateFormat(DateFormat df) {
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, df, _handlerInstantiator);
    }

    public BaseSettings withHandlerInstantiator(HandlerInstantiator hi) {
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, hi);
    }
    
    /*
    /**********************************************************
    /* API
    /**********************************************************
     */

    public ClassIntrospector getClassIntrospector() {
        return _classIntrospector;
    }
    
    public AnnotationIntrospector getAnnotationIntrospector() {
        return _annotationIntrospector;
    }


    public VisibilityChecker<?> getVisibilityChecker() {
        return _visibilityChecker;
    }

    public PropertyNamingStrategy getPropertyNamingStrategy() {
        return _propertyNamingStrategy;
    }

    public TypeFactory getTypeFactory() {
        return _typeFactory;
    }

    public TypeResolverBuilder<?> getTypeResolverBuilder() {
        return _typeResolverBuilder;
    }
    
    public DateFormat getDateFormat() {
        return _dateFormat;
    }

    public HandlerInstantiator getHandlerInstantiator() {
        return _handlerInstantiator;
    }
}
