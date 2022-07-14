package com.fasterxml.jackson.databind.cfg;

import tools.jackson.core.TokenStreamFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.SerializationContextExt;
import com.fasterxml.jackson.databind.ser.SerializerCache;
import com.fasterxml.jackson.databind.ser.SerializerFactory;

/**
 * Factory/builder class that replaces Jackson 2.x concept of "blueprint" instance
 * of {@link com.fasterxml.jackson.databind.SerializerProvider}. It will be constructed and configured during
 * {@link ObjectMapper} building phase, and will be called once per {@code writeValue}
 * call to construct actual stateful {@link com.fasterxml.jackson.databind.SerializerProvider} to use during
 * serialization.
 *<p>
 * Note that since this object has to be serializable (to allow JDK serialization of
 * mapper instances), {@link com.fasterxml.jackson.databind.SerializerProvider} need not be serializable any more.
 *
 * @since 3.0
 */
public abstract class SerializationContexts
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    // NOTE! We do not need (or want) to serialize any of these because they
    // get passed via `forMapper(...)` call; all we want to serialize is identity
    // of this class (and possibly whatever sub-classes may want to retain).
    // Hence `transient` modifiers
    
    /**
     * Low-level {@link TokenStreamFactory} that may be used for constructing
     * embedded generators.
     */
    final transient protected TokenStreamFactory _streamFactory;

    /**
     * Factory responsible for constructing standard serializers.
     */
    final transient protected SerializerFactory _serializerFactory;

    /**
     * Cache for doing type-to-value-serializer lookups.
     */
    final transient protected SerializerCache _cache;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected SerializationContexts() { this(null, null, null); }

    protected SerializationContexts(TokenStreamFactory tsf,
            SerializerFactory serializerFactory, SerializerCache cache) {
        _streamFactory = tsf;
        _serializerFactory = serializerFactory;
        _cache = cache;
    }

    /**
     * Mutant factory method called when instance is actually created for use by mapper
     * (as opposed to coming into existence during building, module registration).
     * Necessary usually to initialize non-configuration state, such as caching.
     */
    public SerializationContexts forMapper(Object mapper,
            TokenStreamFactory tsf, SerializerFactory serializerFactory) {
        return forMapper(mapper, tsf, serializerFactory, _defaultCache());
    }

    protected abstract SerializationContexts forMapper(Object mapper,
            TokenStreamFactory tsf, SerializerFactory serializerFactory,
            SerializerCache cache);
    
    /**
     * Factory method for constructing context object for individual {@code writeValue()}
     * calls.
     */
    public abstract SerializationContextExt createContext(SerializationConfig config,
            GeneratorSettings genSettings);

    /*
    /**********************************************************************
    /* Overridable default methods
    /**********************************************************************
     */

    /**
     * Factory method for constructing per-mapper serializer cache to use.
     */
    protected SerializerCache _defaultCache() {
        return new SerializerCache();
    }

    /*
    /**********************************************************************
    /* Access to caching details
    /**********************************************************************
     */

    /**
     * Method that can be used to determine how many serializers this
     * provider is caching currently
     * (if it does caching: default implementation does)
     * Exact count depends on what kind of serializers get cached;
     * default implementation caches all serializers, including ones that
     * are eagerly constructed (for optimal access speed)
     *<p> 
     * The main use case for this method is to allow conditional flushing of
     * serializer cache, if certain number of entries is reached.
     */
    public int cachedSerializersCount() {
        return _cache.size();
    }

    /**
     * Method that will drop all serializers currently cached by this provider.
     * This can be used to remove memory usage (in case some serializers are
     * only used once or so), or to force re-construction of serializers after
     * configuration changes for mapper than owns the provider.
     */
    public void flushCachedSerializers() {
        _cache.flush();
    }

    /*
    /**********************************************************************
    /* Vanilla implementation
    /**********************************************************************
     */

    public static class DefaultImpl extends SerializationContexts
    {
        private static final long serialVersionUID = 3L;

        public DefaultImpl() { super(null, null, null); }
        public DefaultImpl(TokenStreamFactory tsf,
                SerializerFactory serializerFactory, SerializerCache cache) {
            super(tsf, serializerFactory, cache);
        }

        @Override
        public SerializationContexts forMapper(Object mapper,
                TokenStreamFactory tsf, SerializerFactory serializerFactory,
                SerializerCache cache) {
            return new DefaultImpl(tsf, serializerFactory, cache);
        }

        @Override
        public SerializationContextExt createContext(SerializationConfig config,
                GeneratorSettings genSettings) {
            return new SerializationContextExt.Impl(_streamFactory,
                    config, genSettings, _serializerFactory, _cache);
        }
    }
}
