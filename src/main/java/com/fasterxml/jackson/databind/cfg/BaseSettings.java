package com.fasterxml.jackson.databind.cfg;

import java.text.DateFormat;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * Immutable container class used to store simple configuration
 * settings. Since instances are fully immutable, instances can
 * be freely shared and used without synchronization.
 */
public final class BaseSettings
    implements java.io.Serializable
{
    // for 2.6
    private static final long serialVersionUID = 1L;

    /**
     * We will use a default TimeZone as the baseline.
     */
    private static final TimeZone DEFAULT_TIMEZONE = 
            //  TimeZone.getDefault()
            /* [databind#915] 05-Nov-2015, tatu: Changed to UTC, from earlier
             * baseline of GMT (up to 2.6)
             */
            TimeZone.getTimeZone("UTC");
    
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

    /**
     * Default {@link java.util.Locale} used with serialization formats.
     * Default value is {@link Locale#getDefault()}.
     */
    protected final Locale _locale;

    /**
     * Default {@link java.util.TimeZone} used with serialization formats,
     * if (and only if!) explicitly set by use; otherwise `null` to indicate
     * "use default", which means "UTC" (from Jackson 2.7); earlier versions
     * (up to 2.6) used "GMT".
     *<p>
     * Note that if a new value is set, timezone is also assigned to
     * {@link #_dateFormat} of this object.
     */
    protected final TimeZone _timeZone;

    /**
     * Explicitly default {@link Base64Variant} to use for handling
     * binary data (<code>byte[]</code>), used with data formats
     * that use base64 encoding (like JSON, CSV).
     * 
     * @since 2.1
     */
    protected final Base64Variant _defaultBase64;
    
    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    public BaseSettings(ClassIntrospector ci, AnnotationIntrospector ai,
            VisibilityChecker<?> vc, PropertyNamingStrategy pns, TypeFactory tf,
            TypeResolverBuilder<?> typer, DateFormat dateFormat, HandlerInstantiator hi,
            Locale locale, TimeZone tz, Base64Variant defaultBase64)
    {
        _classIntrospector = ci;
        _annotationIntrospector = ai;
        _visibilityChecker = vc;
        _propertyNamingStrategy = pns;
        _typeFactory = tf;
        _typeResolverBuilder = typer;
        _dateFormat = dateFormat;
        _handlerInstantiator = hi;
        _locale = locale;
        _timeZone = tz;
        _defaultBase64 = defaultBase64;
    }

    /*
    /**********************************************************
    /* Factory methods
    /**********************************************************
     */
    
    public BaseSettings withClassIntrospector(ClassIntrospector ci) {
        if (_classIntrospector == ci) {
            return this;
        }
        return new BaseSettings(ci, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64);
    }
    
    public BaseSettings withAnnotationIntrospector(AnnotationIntrospector ai) {
        if (_annotationIntrospector == ai) {
            return this;
        }
        return new BaseSettings(_classIntrospector, ai, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64);
    }

    public BaseSettings withInsertedAnnotationIntrospector(AnnotationIntrospector ai) {
        return withAnnotationIntrospector(AnnotationIntrospectorPair.create(ai, _annotationIntrospector));
    }

    public BaseSettings withAppendedAnnotationIntrospector(AnnotationIntrospector ai) {
        return withAnnotationIntrospector(AnnotationIntrospectorPair.create(_annotationIntrospector, ai));
    }
    
    public BaseSettings withVisibilityChecker(VisibilityChecker<?> vc) {
        if (_visibilityChecker == vc) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, vc, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64);
    }

    public BaseSettings withVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility) {
        return new BaseSettings(_classIntrospector, _annotationIntrospector,
                _visibilityChecker.withVisibility(forMethod, visibility),
                _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64);
    }
    
    public BaseSettings withPropertyNamingStrategy(PropertyNamingStrategy pns) {
        if (_propertyNamingStrategy == pns) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _visibilityChecker, pns, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64);
    }

    public BaseSettings withTypeFactory(TypeFactory tf) {
        if (_typeFactory == tf) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, tf,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64);
    }

    public BaseSettings withTypeResolverBuilder(TypeResolverBuilder<?> typer) {
        if (_typeResolverBuilder == typer) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                typer, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64);
    }
    
    /**
     * Set the {@link DateFormat} instance to use with this settings.
     * <p>
     * Note that the TimeZone value originally set on the DateFormat will be ignored and
     * reset reset to use the value configured on this settings object.
     * The locale will remains unchanged however.
     * 
     * @param df the {@link DateFormat} instance to use
     * @return a new instance of the settings with the DateFormat set to the desired value
     */
    public BaseSettings withDateFormat(DateFormat df) {
        if (_dateFormat == df) {
            return this;
        }
        
        // Configure the DateFormat with the TimeZone mandated by this settings
        if (df != null ) {
            df = _force(df, getTimeZone());
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, df, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64);
    }

    public BaseSettings withHandlerInstantiator(HandlerInstantiator hi) {
        if (_handlerInstantiator == hi) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, hi, _locale,
                _timeZone, _defaultBase64);
    }

    public BaseSettings with(Locale l) {
        if (_locale == l) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, l,
                _timeZone, _defaultBase64);
    }

    /**
     * Fluent factory for constructing a new instance that uses specified TimeZone.
     * Note that timezone used with also be assigned to configured {@link DateFormat},
     * changing time formatting defaults.
     */
    public BaseSettings with(TimeZone tz)
    {
        if (tz == null) {
            throw new IllegalArgumentException();
        }
        if (tz == _timeZone) {
            return this;
        }
        
        // Reconfigure the DateFormat with the new TimeZone
        DateFormat df = _dateFormat;
        if( df != null ) {
        	df = _force(_dateFormat, tz);
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector,
                _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, df, _handlerInstantiator, _locale,
                tz, _defaultBase64);
    }

    /**
     * @since 2.1
     */
    public BaseSettings with(Base64Variant base64) {
        if (base64 == _defaultBase64) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector,
                _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, base64);
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

    public Locale getLocale() {
        return _locale;
    }

    /**
     * Get the {@link TimeZone} explicitly set on this settings object or the 
     * default value otherwise.
     * 
     * @return the {@link TimeZone} to use according to this settings object (not null).
     * 
     * @see #with(TimeZone)
     * @see #hasExplicitTimeZone()
     * @see #DEFAULT_TIMEZONE
     */
    public TimeZone getTimeZone() {
        TimeZone tz = _timeZone;
        return (tz == null) ? DEFAULT_TIMEZONE : tz;
    }

    /**
     * Accessor that may be called to determine whether this settings object
     * has been explicitly configured with a TimeZone {@code true}, or is still
     * relying on the default settings {@code false}.
     *
     * @return {@code true} if a TimeZone has been explicitly configured
     * @since 2.7
     */
    public boolean hasExplicitTimeZone() {
        return (_timeZone != null);
    }
    
    public Base64Variant getBase64Variant() {
        return _defaultBase64;
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * Set the timeZone on the given DateFormat
     * 
     * @param df the DateFormat instance to configure
     * @param tz the TimeZone to set on the DateFormat
     * @return a potentially new instance of the DateFormat with the TimeZone set to the desired value
     */
    private DateFormat _force(DateFormat df, TimeZone tz)
    {
        if (df instanceof StdDateFormat) {
            return ((StdDateFormat) df).withTimeZone(tz);
        }
        // we don't know if original format might be shared; better create a clone:
        df = (DateFormat) df.clone();
        df.setTimeZone(tz);
        return df;
    }
}
