package tools.jackson.databind.cfg;

import tools.jackson.core.FormatSchema;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.DeserializationContextExt;
import tools.jackson.databind.deser.DeserializerCache;
import tools.jackson.databind.deser.DeserializerFactory;

/**
 * Factory/builder class that replaces Jackson 2.x concept of "blueprint" instance
 * of {@link DeserializationContext}. It will be constructed and configured during
 * {@link ObjectMapper} building phase, and will be called once per {@code readValue}
 * call to construct actual stateful {@link DeserializationContext} to use during
 * serialization.
 *<p>
 * Note that since this object has to be serializable (to allow JDK serialization of
 * mapper instances), {@link DeserializationContext} need not be serializable any more.
 *
 * @since 3.0
 */
public abstract class DeserializationContexts
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
    final transient protected DeserializerFactory _deserializerFactory;

    /**
     * Cache for doing type-to-value-serializer lookups.
     */
    final transient protected DeserializerCache _cache;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected DeserializationContexts() { this(null, null, null); }

    protected DeserializationContexts(TokenStreamFactory tsf,
            DeserializerFactory deserializerFactory, DeserializerCache cache) {
        _streamFactory = tsf;
        _deserializerFactory = deserializerFactory;
        _cache = cache;
    }

    /**
     * Mutant factory method called when instance is actually created for use by mapper
     * (as opposed to coming into existence during building, module registration).
     * Necessary usually to initialize non-configuration state, such as caching.
     */
    public DeserializationContexts forMapper(Object mapper,
            TokenStreamFactory tsf, DeserializerFactory deserializerFactory) {
        return forMapper(mapper, tsf, deserializerFactory, _defaultCache());
    }

    protected abstract DeserializationContexts forMapper(Object mapper,
            TokenStreamFactory tsf, DeserializerFactory deserializerFactory,
            DeserializerCache cache);

    /**
     * Factory method for constructing context object for individual {@code writeValue} call.
     */
    public abstract DeserializationContextExt createContext(DeserializationConfig config,
            FormatSchema schema, InjectableValues injectables);

    /*
    /**********************************************************************
    /* Overridable default methods
    /**********************************************************************
     */

    /**
     * Factory method for constructing per-mapper serializer cache to use.
     */
    protected DeserializerCache _defaultCache() {
        return new DeserializerCache();
    }

    /*
    /**********************************************************************
    /* Vanilla implementation
    /**********************************************************************
     */

    public static class DefaultImpl extends DeserializationContexts
    {
        private static final long serialVersionUID = 3L;

        public DefaultImpl() { super(); }
        public DefaultImpl(TokenStreamFactory tsf,
                DeserializerFactory serializerFactory, DeserializerCache cache) {
            super(tsf, serializerFactory, cache);
        }

        @Override
        public DeserializationContexts forMapper(Object mapper,
                TokenStreamFactory tsf, DeserializerFactory serializerFactory,
                DeserializerCache cache) {
            return new DefaultImpl(tsf, serializerFactory, cache);
        }

        @Override
        public DeserializationContextExt createContext(DeserializationConfig config,
                FormatSchema schema, InjectableValues injectables) {
            return new DeserializationContextExt.Impl(_streamFactory,
                    _deserializerFactory, _cache,
                    config, schema, injectables);
        }

        // As per name, just for testing
        public DeserializerCache cacheForTests() { return _cache; }
    }
}
