package com.fasterxml.jackson.databind.cfg;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.TokenStreamFactory;
import com.fasterxml.jackson.core.util.Snapshottable;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.DeserializerFactory;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.MixInHandler;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverProvider;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.LinkedNode;

/**
 * Interface for State object used for preserving initial state of a
 * {@link MapperBuilder} before modules are configured and resulting
 * {@link com.fasterxml.jackson.databind.ObjectMapper} isn't constructed.
 * It is passed to mapper to allow "re-building" via newly created builder.
 *<p>
 * Note that JDK serialization is supported by switching this object in place
 * of mapper. This requires some acrobatics on return direction.
 */
public abstract class MapperBuilderState
    implements java.io.Serializable // important!
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************************
    /* Basic settings
    /**********************************************************************
     */

    protected final BaseSettings _baseSettings;
    protected final TokenStreamFactory _streamFactory;
    protected final ConfigOverrides _configOverrides;

    /*
    /**********************************************************************
    /* Feature flags
    /**********************************************************************
     */

    protected final int _mapperFeatures, _serFeatures, _deserFeatures;
    protected final int _parserFeatures, _generatorFeatures;
    protected final int _formatParserFeatures, _formatGeneratorFeatures;

    /*
    /**********************************************************************
    /* Modules
    /**********************************************************************
     */

    /**
     * Modules registered in registration order, if any; `null` if none.
     */
    protected final com.fasterxml.jackson.databind.Module[] _modules;

    /*
    /**********************************************************************
    /* Handlers, introspection
    /**********************************************************************
     */

    protected final TypeFactory _typeFactory;
    protected final ClassIntrospector _classIntrospector;
    protected final TypeResolverProvider _typeResolverProvider;
    protected final SubtypeResolver _subtypeResolver;
    protected final MixInHandler _mixInHandler;

    /*
    /**********************************************************************
    /* Factories for serialization
    /**********************************************************************
     */

    protected final SerializationContexts _serializationContexts;
    protected final SerializerFactory _serializerFactory;
    protected final FilterProvider _filterProvider;
    protected final PrettyPrinter _defaultPrettyPrinter;

    /*
    /**********************************************************************
    /* Factories for deserialization
    /**********************************************************************
     */

    protected final DeserializationContexts _deserializationContexts;
    protected final DeserializerFactory _deserializerFactory;
    protected final InjectableValues _injectableValues;

    /**
     * Optional handlers that application may register to try to work-around
     * various problem situations during deserialization
     */
    protected final LinkedNode<DeserializationProblemHandler> _problemHandlers;

    protected final AbstractTypeResolver[] _abstractTypeResolvers;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    /**
     * Constructor called when "saving" state of mapper, to be used as base for
     * {@link ObjectMapper#rebuild()} functionality.
     */
    public MapperBuilderState(MapperBuilder<?,?> src)
    {
        // Basic settings
        _baseSettings = src._baseSettings; // immutable
        _streamFactory = src._streamFactory; // immutable
        _configOverrides = Snapshottable.takeSnapshot(src._configOverrides);

        // Feature flags (simple ints, no copy needed)
        _mapperFeatures = src._mapperFeatures;
        _serFeatures = src._serFeatures;
        _deserFeatures = src._deserFeatures;
        _parserFeatures = src._parserFeatures;
        _generatorFeatures = src._generatorFeatures;
        _formatParserFeatures = src._formatParserFeatures;
        _formatGeneratorFeatures = src._formatGeneratorFeatures;

        // Handlers, introspection
        _typeFactory = Snapshottable.takeSnapshot(src._typeFactory);
        _classIntrospector = src._classIntrospector; // no snapshot needed (uses `forMapper()`)
        _typeResolverProvider = src._typeResolverProvider;
        _subtypeResolver = Snapshottable.takeSnapshot(src._subtypeResolver);
        _mixInHandler = (MixInHandler) Snapshottable.takeSnapshot(src._mixInHandler);

        // Factories for serialization
        _serializerFactory = src._serializerFactory;
        _serializationContexts = src._serializationContexts; // no snapshot needed (uses `forMapper()`)
        _filterProvider = Snapshottable.takeSnapshot(src._filterProvider);
        _defaultPrettyPrinter = src._defaultPrettyPrinter;

        // Factories for deserialization
        _deserializerFactory = src._deserializerFactory;
        _deserializationContexts = src._deserializationContexts;
        _injectableValues = Snapshottable.takeSnapshot(src._injectableValues);
        // assume our usage of LinkedNode-based list is immutable here (should be)
        _problemHandlers = src._problemHandlers;
        _abstractTypeResolvers = src._abstractTypeResolvers;

        // Modules
        if (src._modules == null) {
            _modules = null;
        } else {
            _modules = _toArray(src._modules.values());
        }
    }

    private static com.fasterxml.jackson.databind.Module[] _toArray(Collection<?> coll)
    {
        if (coll == null || coll.isEmpty()) {
            return null;
        }
        return coll.toArray(new com.fasterxml.jackson.databind.Module[coll.size()]);
    }

    /*
    /**********************************************************************
    /* Configuration access by ObjectMapper
    /**********************************************************************
     */

    public Collection<com.fasterxml.jackson.databind.Module> modules() {
        if (_modules == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(_modules));
    }

    /*
    /**********************************************************************
    /* JDK deserialization support
    /**********************************************************************
     */

    /**
     * Method required to support JDK deserialization; made `abstract` here to ensure
     * sub-classes must implement it.
     */
    protected abstract Object readResolve();
}
