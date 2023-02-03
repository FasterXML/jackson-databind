package com.fasterxml.jackson.databind.cfg;

import java.text.DateFormat;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.core.Base64Variant;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AccessorNamingStrategy;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.DefaultAccessorNamingStrategy;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * Immutable container class used to store simple configuration
 * settings for both serialization and deserialization.
 * Since instances are fully immutable, instances can
 * be freely shared and used without synchronization.
 */
public final class BaseSettings
    implements java.io.Serializable
{
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
     * Specific factory used for creating {@link JavaType} instances;
     * needed to allow modules to add more custom type handling
     * (mostly to support types of non-Java JVM languages)
     */
    protected final TypeFactory _typeFactory;

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
     * Custom property naming strategy in use, if any.
     */
    protected final PropertyNamingStrategy _propertyNamingStrategy;

    /**
     * Provider for creating {@link AccessorNamingStrategy} instances to use
     *
     * @since 2.12
     */
    protected final AccessorNamingStrategy.Provider _accessorNaming;

    /*
    /**********************************************************
    /* Configuration settings; polymorphic type resolution
    /**********************************************************
     */

    /**
     * Builder used to create type resolver for serializing and deserializing
     * values for which polymorphic type handling is needed.
     */
    protected final TypeResolverBuilder<?> _typeResolverBuilder;

    /**
     * Validator that is used to limit allowed polymorphic subtypes, mostly
     * for security reasons when dealing with untrusted content.
     *
     * @since 2.10
     */
    protected final PolymorphicTypeValidator _typeValidator;

    /*
    /**********************************************************
    /* Configuration settings; other
    /**********************************************************
     */

    /**
     * Custom date format to use for deserialization. If specified, will be
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

    /**
     * @since 2.12
     */
    public BaseSettings(ClassIntrospector ci, AnnotationIntrospector ai,
            PropertyNamingStrategy pns, TypeFactory tf,
            TypeResolverBuilder<?> typer, DateFormat dateFormat, HandlerInstantiator hi,
            Locale locale, TimeZone tz, Base64Variant defaultBase64,
            PolymorphicTypeValidator ptv, AccessorNamingStrategy.Provider accNaming)
    {
        _classIntrospector = ci;
        _annotationIntrospector = ai;
        _propertyNamingStrategy = pns;
        _typeFactory = tf;
        _typeResolverBuilder = typer;
        _dateFormat = dateFormat;
        _handlerInstantiator = hi;
        _locale = locale;
        _timeZone = tz;
        _defaultBase64 = defaultBase64;
        _typeValidator = ptv;
        _accessorNaming = accNaming;
    }

    @Deprecated // since 2.12
    public BaseSettings(ClassIntrospector ci, AnnotationIntrospector ai,
            PropertyNamingStrategy pns, TypeFactory tf,
            TypeResolverBuilder<?> typer, DateFormat dateFormat, HandlerInstantiator hi,
            Locale locale, TimeZone tz, Base64Variant defaultBase64,
            PolymorphicTypeValidator ptv)
    {
        this(ci, ai, pns, tf, typer, dateFormat, hi, locale, tz, defaultBase64, ptv,
                new DefaultAccessorNamingStrategy.Provider());
    }

    /**
     * Turns out we are not necessarily 100% stateless, alas, since {@link ClassIntrospector}
     * typically has a cache. So this method is needed for deep copy() of Mapper.
     *
     * @since 2.9.6
     */
    public BaseSettings copy() {
        return new BaseSettings(_classIntrospector.copy(),
            _annotationIntrospector,
            _propertyNamingStrategy,
            _typeFactory,
            _typeResolverBuilder,
            _dateFormat,
            _handlerInstantiator,
            _locale,
            _timeZone,
            _defaultBase64,
            _typeValidator,
            _accessorNaming);
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
        return new BaseSettings(ci, _annotationIntrospector, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _typeValidator, _accessorNaming);
    }

    public BaseSettings withAnnotationIntrospector(AnnotationIntrospector ai) {
        if (_annotationIntrospector == ai) {
            return this;
        }
        return new BaseSettings(_classIntrospector, ai, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _typeValidator, _accessorNaming);
    }

    public BaseSettings withInsertedAnnotationIntrospector(AnnotationIntrospector ai) {
        return withAnnotationIntrospector(AnnotationIntrospectorPair.create(ai, _annotationIntrospector));
    }

    public BaseSettings withAppendedAnnotationIntrospector(AnnotationIntrospector ai) {
        return withAnnotationIntrospector(AnnotationIntrospectorPair.create(_annotationIntrospector, ai));
    }

    /*
    public BaseSettings withVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility) {
        return new BaseSettings(_classIntrospector, _annotationIntrospector,
                _visibilityChecker.withVisibility(forMethod, visibility),
                _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _typeValidator);
    }
    */

    public BaseSettings withPropertyNamingStrategy(PropertyNamingStrategy pns) {
        if (_propertyNamingStrategy == pns) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, pns, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _typeValidator, _accessorNaming);
    }

    // @since 2.12
    public BaseSettings withAccessorNaming(AccessorNamingStrategy.Provider p) {
        if (_accessorNaming == p) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _typeValidator, p);
    }

    public BaseSettings withTypeFactory(TypeFactory tf) {
        if (_typeFactory == tf) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _propertyNamingStrategy, tf,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _typeValidator, _accessorNaming);
    }

    public BaseSettings withTypeResolverBuilder(TypeResolverBuilder<?> typer) {
        if (_typeResolverBuilder == typer) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _propertyNamingStrategy, _typeFactory,
                typer, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _typeValidator, _accessorNaming);
    }

    public BaseSettings withDateFormat(DateFormat df) {
        if (_dateFormat == df) {
            return this;
        }
        // 26-Sep-2015, tatu: Related to [databind#939], let's try to force TimeZone if
        //   (but only if!) it has been set explicitly.
        if ((df != null) && hasExplicitTimeZone()) {
            df = _force(df, _timeZone);
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, df, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _typeValidator, _accessorNaming);
    }

    public BaseSettings withHandlerInstantiator(HandlerInstantiator hi) {
        if (_handlerInstantiator == hi) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, hi, _locale,
                _timeZone, _defaultBase64, _typeValidator, _accessorNaming);
    }

    public BaseSettings with(Locale l) {
        if (_locale == l) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector, _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, l,
                _timeZone, _defaultBase64, _typeValidator, _accessorNaming);
    }

    /**
     * Fluent factory for constructing a new instance that uses specified TimeZone.
     * Note that timezone used with also be assigned to configured {@link DateFormat},
     * changing time formatting defaults.
     */
    public BaseSettings with(TimeZone tz)
    {
        if (tz == _timeZone) {
            return this;
        }
        // 18-Oct-2020, tatu: Should allow use of `null` to revert back to "Default",
        //    commented out handling used before 2.12
//        if (tz == null) {
//            throw new IllegalArgumentException();
//        }

        DateFormat df = _force(_dateFormat, (tz == null) ? DEFAULT_TIMEZONE : tz);
        return new BaseSettings(_classIntrospector, _annotationIntrospector,
                _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, df, _handlerInstantiator, _locale,
                tz, _defaultBase64, _typeValidator, _accessorNaming);
    }

    /**
     * @since 2.1
     */
    public BaseSettings with(Base64Variant base64) {
        if (base64 == _defaultBase64) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector,
                _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, base64, _typeValidator, _accessorNaming);
    }

    /**
     * @since 2.10
     */
    public BaseSettings with(PolymorphicTypeValidator v) {
        if (v == _typeValidator) {
            return this;
        }
        return new BaseSettings(_classIntrospector, _annotationIntrospector,
                _propertyNamingStrategy, _typeFactory,
                _typeResolverBuilder, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, v, _accessorNaming);
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

    public PropertyNamingStrategy getPropertyNamingStrategy() {
        return _propertyNamingStrategy;
    }

    public AccessorNamingStrategy.Provider getAccessorNaming() {
        return _accessorNaming;
    }

    public TypeFactory getTypeFactory() {
        return _typeFactory;
    }

    public TypeResolverBuilder<?> getTypeResolverBuilder() {
        return _typeResolverBuilder;
    }

    /**
     * @since 2.10
     */
    public PolymorphicTypeValidator getPolymorphicTypeValidator() {
        return _typeValidator;
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

    public TimeZone getTimeZone() {
        TimeZone tz = _timeZone;
        return (tz == null) ? DEFAULT_TIMEZONE : tz;
    }

    /**
     * Accessor that may be called to determine whether this settings object
     * has been explicitly configured with a TimeZone (true), or is still
     * relying on the default settings (false).
     *
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
