package tools.jackson.databind.cfg;

import java.text.DateFormat;
import java.util.Locale;
import java.util.TimeZone;

import tools.jackson.core.Base64Variant;
import tools.jackson.databind.*;
import tools.jackson.databind.introspect.AccessorNamingStrategy;
import tools.jackson.databind.introspect.AnnotationIntrospectorPair;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.jsontype.TypeResolverBuilder;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.util.StdDateFormat;

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
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getTimeZone("UTC");

    /*
    /**********************************************************************
    /* Configuration settings; introspection, related
    /**********************************************************************
     */

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
     */
    protected final AccessorNamingStrategy.Provider _accessorNaming;

    /*
    /**********************************************************************
    /* Configuration settings; polymorphic type resolution
    /**********************************************************************
     */

    /**
     * Type information handler used for "default typing".
     */
    protected final TypeResolverBuilder<?> _defaultTyper;

    /**
     * Validator that is used to limit allowed subtypes during polymorphic
     * deserialization,
     * mostly for security reasons when dealing with untrusted content.
     */
    protected final PolymorphicTypeValidator _typeValidator;

    /*
    /**********************************************************************
    /* Configuration settings; other
    /**********************************************************************
     */

    /**
     * Custom date format to use for deserialization. If specified, will be
     * used instead of {@link tools.jackson.databind.util.StdDateFormat}.
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
     */
    protected final Base64Variant _defaultBase64;

    /**
     * Used to provide custom cache implementation in downstream components.
     *
     * @since 2.16
     */
    protected final CacheProvider _cacheProvider;

    /**
     * Factory used for constructing {@link tools.jackson.databind.JsonNode} instances.
     */
    protected final JsonNodeFactory _nodeFactory;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    public BaseSettings(AnnotationIntrospector ai,
            PropertyNamingStrategy pns, AccessorNamingStrategy.Provider accNaming,
            TypeResolverBuilder<?> defaultTyper, PolymorphicTypeValidator ptv,
            DateFormat dateFormat, HandlerInstantiator hi,
            Locale locale, TimeZone tz, Base64Variant defaultBase64,
            CacheProvider cacheProvider, JsonNodeFactory nodeFactory)
    {
        _annotationIntrospector = ai;
        _propertyNamingStrategy = pns;
        _accessorNaming = accNaming;
        _defaultTyper = defaultTyper;
        _typeValidator = ptv;
        _dateFormat = dateFormat;
        _handlerInstantiator = hi;
        _locale = locale;
        _timeZone = tz;
        _defaultBase64 = defaultBase64;
        _cacheProvider = cacheProvider;
        _nodeFactory = nodeFactory;
    }

    /*
    /**********************************************************************
    /* Factory methods
    /**********************************************************************
     */

    public BaseSettings withAnnotationIntrospector(AnnotationIntrospector ai) {
        if (_annotationIntrospector == ai) {
            return this;
        }
        return new BaseSettings(ai, _propertyNamingStrategy, _accessorNaming,
                _defaultTyper, _typeValidator, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _cacheProvider, _nodeFactory);
    }

    public BaseSettings withInsertedAnnotationIntrospector(AnnotationIntrospector ai) {
        return withAnnotationIntrospector(AnnotationIntrospectorPair.create(ai, _annotationIntrospector));
    }

    public BaseSettings withAppendedAnnotationIntrospector(AnnotationIntrospector ai) {
        return withAnnotationIntrospector(AnnotationIntrospectorPair.create(_annotationIntrospector, ai));
    }

    public BaseSettings with(PropertyNamingStrategy pns) {
        if (_propertyNamingStrategy == pns) {
            return this;
        }
        return new BaseSettings(_annotationIntrospector, pns, _accessorNaming,
                _defaultTyper, _typeValidator, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _cacheProvider, _nodeFactory);
    }

    public BaseSettings with(AccessorNamingStrategy.Provider p) {
        if (_accessorNaming == p) {
            return this;
        }
        return new BaseSettings(_annotationIntrospector, _propertyNamingStrategy, p,
                _defaultTyper, _typeValidator, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _cacheProvider, _nodeFactory);
    }

    public BaseSettings with(TypeResolverBuilder<?> typer) {
        if (_defaultTyper == typer) {
            return this;
        }
        return new BaseSettings(_annotationIntrospector, _propertyNamingStrategy, _accessorNaming,
                typer, _typeValidator, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _cacheProvider, _nodeFactory);
    }

    public BaseSettings with(PolymorphicTypeValidator ptv) {
        if (_typeValidator == ptv) {
            return this;
        }
        return new BaseSettings(_annotationIntrospector, _propertyNamingStrategy, _accessorNaming,
                _defaultTyper, ptv, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _cacheProvider, _nodeFactory);
    }

    public BaseSettings with(DateFormat df) {
        if (_dateFormat == df) {
            return this;
        }
        // 26-Sep-2015, tatu: Related to [databind#939], let's try to force TimeZone if
        //   (but only if!) it has been set explicitly.
        if ((df != null) && hasExplicitTimeZone()) {
            df = _force(df, _timeZone);
        }
        return new BaseSettings(_annotationIntrospector, _propertyNamingStrategy, _accessorNaming,
                _defaultTyper, _typeValidator, df, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _cacheProvider, _nodeFactory);
    }

    public BaseSettings with(HandlerInstantiator hi) {
        if (_handlerInstantiator == hi) {
            return this;
        }
        return new BaseSettings(_annotationIntrospector, _propertyNamingStrategy, _accessorNaming,
                _defaultTyper, _typeValidator, _dateFormat, hi, _locale,
                _timeZone, _defaultBase64, _cacheProvider, _nodeFactory);
    }

    public BaseSettings with(Locale l) {
        if (_locale == l) {
            return this;
        }
        return new BaseSettings(_annotationIntrospector, _propertyNamingStrategy, _accessorNaming,
                _defaultTyper, _typeValidator, _dateFormat, _handlerInstantiator, l,
                _timeZone, _defaultBase64, _cacheProvider, _nodeFactory);
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
        DateFormat df = _force(_dateFormat, (tz == null) ? DEFAULT_TIMEZONE : tz);
        return new BaseSettings(_annotationIntrospector, _propertyNamingStrategy, _accessorNaming,
                _defaultTyper, _typeValidator, df, _handlerInstantiator, _locale,
                tz, _defaultBase64, _cacheProvider, _nodeFactory);
    }

    public BaseSettings with(Base64Variant base64) {
        if (base64 == _defaultBase64) {
            return this;
        }
        return new BaseSettings(_annotationIntrospector, _propertyNamingStrategy, _accessorNaming,
                _defaultTyper, _typeValidator, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, base64, _cacheProvider, _nodeFactory);
    }

    /**
     * Fluent factory for constructing a new instance with provided {@link CacheProvider}.
     *
     * @return a new instance with provided {@link CacheProvider}.
     */
    public BaseSettings with(CacheProvider cacheProvider) {
        if (cacheProvider == _cacheProvider) {
            return this;
        }
        return new BaseSettings(_annotationIntrospector, _propertyNamingStrategy, _accessorNaming,
                _defaultTyper, _typeValidator, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, cacheProvider, _nodeFactory);
    }

    public BaseSettings with(JsonNodeFactory nodeFactory) {
        if (nodeFactory == _nodeFactory) {
            return this;
        }
        return new BaseSettings(_annotationIntrospector, _propertyNamingStrategy, _accessorNaming,
                _defaultTyper, _typeValidator, _dateFormat, _handlerInstantiator, _locale,
                _timeZone, _defaultBase64, _cacheProvider, nodeFactory);
    }

    /*
    /**********************************************************************
    /* API
    /**********************************************************************
     */

    public AnnotationIntrospector getAnnotationIntrospector() {
        return _annotationIntrospector;
    }

    public PropertyNamingStrategy getPropertyNamingStrategy() {
        return _propertyNamingStrategy;
    }

    public AccessorNamingStrategy.Provider getAccessorNaming() {
        return _accessorNaming;
    }

    public TypeResolverBuilder<?> getDefaultTyper() {
        return _defaultTyper;
    }

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
     */
    public boolean hasExplicitTimeZone() {
        return (_timeZone != null);
    }

    public Base64Variant getBase64Variant() {
        return _defaultBase64;
    }

    public CacheProvider getCacheProvider() {
        return _cacheProvider;
    }

    public JsonNodeFactory getNodeFactory() {
        return _nodeFactory;
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
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
