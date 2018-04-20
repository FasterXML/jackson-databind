package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.core.TokenStreamFactory;
import com.fasterxml.jackson.core.util.Snapshottable;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerCache;
import com.fasterxml.jackson.databind.ser.SerializerFactory;

/**
 * Factory/builder class that replaces Jackson 2.x concept of "blueprint" instance
 * of {@link SerializerProvider}. It will be constructed and configured during
 * {@link ObjectMapper} building phase, and will be called once per {@code writeValue}
 * call to construct actual stateful {@link SerializerProvider} to use during
 * serialization.
 *<p>
 * Note that since this object has to be serializable (to allow JDK serialization of
 * mapper instances), {@link SerializerProvider} need not be serializable any more.
 *
 * @since 3.0
 */
public abstract class SerializationContexts
    implements Snapshottable<SerializationContexts>,
        java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */
    
    /**
     * Low-level {@link TokenStreamFactory} that may be used for constructing
     * embedded generators.
     */
    final protected TokenStreamFactory _streamFactory;

    /*
    /**********************************************************************
    /* Shared helper objects (dynamic)
    /**********************************************************************
     */

    /**
     * Cache for doing type-to-value-serializer lookups.
     */
    final protected SerializerCache _serializerCache;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected SerializationContexts(TokenStreamFactory tsf) {
        _streamFactory = tsf;
        _serializerCache = new SerializerCache();
    }

    public abstract DefaultSerializerProvider createContext(SerializationConfig config,
            GeneratorSettings genSettings, SerializerFactory serFactory);

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
        return _serializerCache.size();
    }

    /**
     * Method that will drop all serializers currently cached by this provider.
     * This can be used to remove memory usage (in case some serializers are
     * only used once or so), or to force re-construction of serializers after
     * configuration changes for mapper than owns the provider.
     */
    public void flushCachedSerializers() {
        _serializerCache.flush();
    }

    /*
    /**********************************************************************
    /* Vanilla implementation
    /**********************************************************************
     */

    public static class DefaultImpl extends SerializationContexts
    {
        private static final long serialVersionUID = 3L;

        public DefaultImpl(TokenStreamFactory tsf) {
            super(tsf);
        }

        @Override
        public SerializationContexts snapshot() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public DefaultSerializerProvider createContext(SerializationConfig config,
                GeneratorSettings genSettings, SerializerFactory serFactory) {
            return new DefaultSerializerProvider.Impl(_streamFactory,
                    _serializerCache,
                    config, genSettings, serFactory);
        }
    }
}
