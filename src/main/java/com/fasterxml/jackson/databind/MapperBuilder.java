package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Since {@link ObjectMapper} instances are immutable in  Jackson 3.x for full thread-safety,
 * we need means to construct configured instances. This is the shared base API for
 * builders for all types of mappers.
 *
 * @since 3.0
 */
public abstract class MapperBuilder<M extends ObjectMapper,
    B extends MapperBuilder<M,B>>
{
    /*
    /**********************************************************
    /* Simple feature bitmasks
    /**********************************************************
     */

    /**
     * Set of {@link MapperFeature}s enabled, as bitmask.
     */
    protected int _mapperFeatures;

    /*
    /**********************************************************
    /* Factories for framework itself, general
    /**********************************************************
     */

    /**
     * Underlying stream factory
     */
    protected final TokenStreamFactory _streamFactory;

    protected TypeFactory _typeFactory;

    protected SubtypeResolver _subtypeResolver;

    /*
    /**********************************************************
    /* Factories for framework itself, serialization
    /**********************************************************
     */
    
    protected SerializerFactory _serializerFactory;

    /**
     * Prototype {@link SerializerProvider} to use for creating per-operation providers.
     */
    protected DefaultSerializerProvider _serializerProvider;

    /*
    /**********************************************************
    /* Factories for framework itself, deserialization
    /**********************************************************
     */

    protected DeserializerFactory _deserializerFactory;
    
    /**
     * Prototype (about same as factory) to use for creating per-operation contexts.
     */
    protected DefaultDeserializationContext _deserializationContext;

    /*
    /**********************************************************
    /* Configuration settings, shared
    /**********************************************************
     */
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected MapperBuilder(TokenStreamFactory streamFactory)
    {
        _streamFactory = streamFactory;

        _typeFactory = TypeFactory.defaultInstance();
        _subtypeResolver = null;

        _serializerFactory = BeanSerializerFactory.instance;
        _serializerProvider = null;

        _deserializerFactory = BeanDeserializerFactory.instance;
        _deserializationContext = null;

        //        _mapperFeatures = MapperFeature;
    }

    protected MapperBuilder(MapperBuilder<?,?> base)
    {
        _streamFactory = base._streamFactory;

        _typeFactory = base._typeFactory;
        _subtypeResolver = base._subtypeResolver;

        _serializerFactory = base._serializerFactory;
        _serializerProvider = base._serializerProvider;

        _deserializerFactory = base._deserializerFactory;
        _deserializationContext = base._deserializationContext;
    }

    /**
     * Method to call to create an initialize actual mapper instance
     */
    public abstract M build();

    /*
    /**********************************************************
    /* Accessors for framework factories
    /**********************************************************
     */

    public TokenStreamFactory streamFactory() {
        return _streamFactory;
    }

    public TypeFactory typeFactory() {
        return _typeFactory;
    }

    public SubtypeResolver subtypeResolver() {
        return (_subtypeResolver != null) ? _subtypeResolver : defaultSubtypeResolver();
    }

    /**
     * Overridable method for changing default {@link SubtypeResolver} prototype
     * to use.
     */
    protected SubtypeResolver defaultSubtypeResolver() {
        return new StdSubtypeResolver();
    }

    public SerializerFactory serializerFactory() {
        return _serializerFactory;
    }

    public DefaultSerializerProvider serializerProvider() {
        return (_serializerProvider != null) ? _serializerProvider : defaultSerializerProvider();
    }

    /**
     * Overridable method for changing default {@link SerializerProvider} prototype
     * to use.
     */
    protected DefaultSerializerProvider defaultSerializerProvider() {
        return new DefaultSerializerProvider.Impl(_streamFactory);
    }

    public DeserializerFactory deserializerFactory() {
        return _deserializerFactory;
    }

    protected DefaultDeserializationContext deserializationContext() {
        return (_deserializationContext != null) ? _deserializationContext
                : defaultDeserializationContext();
    }

    /**
     * Overridable method for changing default {@link SerializerProvider} prototype
     * to use.
     */
    protected DefaultDeserializationContext defaultDeserializationContext() {
        return new DefaultDeserializationContext.Impl(deserializerFactory(),
                _streamFactory);
    }

    /*
    /**********************************************************
    /* Changing simple features
    /**********************************************************
     */

    /*
    /**********************************************************
    /* Changing factories
    /**********************************************************
     */

    public B typeFactory(TypeFactory f) {
        _typeFactory = f;
        return _this();
    }

    public B subtypeResolver(SubtypeResolver r) {
        _subtypeResolver = r;
        return _this();
    }

    public B serializerFactory(SerializerFactory f) {
        _serializerFactory = f;
        return _this();
    }

    public B serializerProvider(DefaultSerializerProvider prov) {
        _serializerProvider = prov;
        return _this();
    }

    public B deserializerFactory(DeserializerFactory f) {
        _deserializerFactory = f;
        return _this();
    }

    protected B deserializationContext(DefaultDeserializationContext ctxt) {
        _deserializationContext = ctxt;
        return _this();
    }

    /*
    /**********************************************************
    /* Other helper methods
    /**********************************************************
     */
    
    // silly convenience cast method we need
    @SuppressWarnings("unchecked")
    protected final B _this() { return (B) this; }
}
