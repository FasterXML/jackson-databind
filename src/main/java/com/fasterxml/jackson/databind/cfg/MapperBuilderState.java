package com.fasterxml.jackson.databind.cfg;

import java.util.Collection;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.TokenStreamFactory;
import com.fasterxml.jackson.core.util.Snapshottable;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.DeserializerFactory;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.MixInHandler;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
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

    protected final ClassIntrospector _classIntrospector;
    protected final SubtypeResolver _subtypeResolver;
    protected final MixInHandler _mixInHandler;

    /*
    /**********************************************************************
    /* Factories for serialization
    /**********************************************************************
     */

    protected final SerializerFactory _serializerFactory;
    protected final DefaultSerializerProvider _serializerProvider;
    protected final FilterProvider _filterProvider;
    protected final PrettyPrinter _defaultPrettyPrinter;

    /*
    /**********************************************************************
    /* Factories for deserialization
    /**********************************************************************
     */

    protected final DeserializerFactory _deserializerFactory;
    protected final DefaultDeserializationContext _deserializationContext;
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

    public MapperBuilderState(MapperBuilder<?,?> src)
    {
        // Basic settings

        this._baseSettings = src._baseSettings; // immutable
        this._streamFactory = src._streamFactory; // immutable
        this._configOverrides = Snapshottable.takeSnapshot(src._configOverrides);

        // Feature flags
        this._mapperFeatures = src._mapperFeatures;
        this._serFeatures = src._serFeatures;
        this._deserFeatures = src._deserFeatures;
        this._parserFeatures = src._parserFeatures;
        this._generatorFeatures = src._generatorFeatures;
        this._formatParserFeatures = src._formatParserFeatures;
        this._formatGeneratorFeatures = src._formatGeneratorFeatures;

        // Handlers, introspection
        this._classIntrospector = src._classIntrospector;
        this._subtypeResolver = Snapshottable.takeSnapshot(src._subtypeResolver);
        this._mixInHandler = (MixInHandler) Snapshottable.takeSnapshot(src._mixInHandler);

        // Factories for serialization
        this._serializerFactory = src._serializerFactory;
        this._serializerProvider = src._serializerProvider;
        this._filterProvider = src._filterProvider;
        this._defaultPrettyPrinter = src._defaultPrettyPrinter;
        
        // Factories for deserialization
        this._deserializerFactory = src._deserializerFactory;
        this._deserializationContext = src._deserializationContext;
        this._injectableValues = Snapshottable.takeSnapshot(src._injectableValues);
        // assume our usage of LinkedNode-based list is immutable here (should be)
        this._problemHandlers = src._problemHandlers;
        this._abstractTypeResolvers = src._abstractTypeResolvers;

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
    /* JDK deserialization support
    /**********************************************************************
     */

    /**
     * Method required to support JDK deserialization; made `abstract` here to ensure
     * sub-classes must implement it.
     */
    protected abstract Object readResolve();
}
