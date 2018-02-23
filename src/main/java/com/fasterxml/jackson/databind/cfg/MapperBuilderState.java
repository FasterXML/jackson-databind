package com.fasterxml.jackson.databind.cfg;

import java.lang.reflect.Array;
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

    protected BaseSettings _baseSettings;
    protected TokenStreamFactory _streamFactory;
    protected ConfigOverrides _configOverrides;

    /*
    /**********************************************************************
    /* Modules
    /**********************************************************************
     */

    /**
     * Modules registered in registration order
     */
    protected Object[] _modules;

    /*
    /**********************************************************************
    /* Handlers, introspection
    /**********************************************************************
     */

    protected ClassIntrospector _classIntrospector;
    protected SubtypeResolver _subtypeResolver;
    protected MixInHandler _mixInHandler;

    /*
    /**********************************************************************
    /* Factories for serialization
    /**********************************************************************
     */

    protected SerializerFactory _serializerFactory;
    protected DefaultSerializerProvider _serializerProvider;
    protected FilterProvider _filterProvider;
    protected PrettyPrinter _defaultPrettyPrinter;

    /*
    /**********************************************************************
    /* Factories for deserialization
    /**********************************************************************
     */

    protected DeserializerFactory _deserializerFactory;
    protected DefaultDeserializationContext _deserializationContext;
    protected InjectableValues _injectableValues;

    /*
    /**********************************************************************
    /* Feature flags:
    /**********************************************************************
     */

    protected int _mapperFeatures, _serFeatures, _deserFeatures;
    protected int _parserFeatures, _generatorFeatures;
    protected int _formatParserFeatures, _formatGeneratorFeatures;

    /*
    /**********************************************************************
    /* Misc other configuration
    /**********************************************************************
     */

    /**
     * Optional handlers that application may register to try to work-around
     * various problem situations during deserialization
     */
    protected LinkedNode<DeserializationProblemHandler> _problemHandlers;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public MapperBuilderState(MapperBuilder<?,?> src)
    {
        // Basic settings

        this._baseSettings = src._baseSettings; // immutable
        this._streamFactory = src._streamFactory; // immutable
        this._configOverrides = Snapshottable.takeSnapshot(src._configOverrides);

        // Modules

        _modules = _toArray(src._modules.values());

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
        
        // Feature flags
        this._mapperFeatures = src._mapperFeatures;
        this._serFeatures = src._serFeatures;
        this._deserFeatures = src._deserFeatures;
        this._parserFeatures = src._parserFeatures;
        this._generatorFeatures = src._generatorFeatures;
        this._formatParserFeatures = src._formatParserFeatures;
        this._formatGeneratorFeatures = src._formatGeneratorFeatures;

        // Misc other

        // assume our usage of LinkedNode-based list is immutable here (should be)
        this._problemHandlers = src._problemHandlers;
    }

    private static Object[] _toArray(Collection<?> coll)
    {
        if (coll == null || coll.isEmpty()) {
            return null;
        }
        Class<?> raw = coll.iterator().next().getClass();
        int len = coll.size();
        Object[] result = (Object[]) Array.newInstance(raw, len);
        return coll.toArray(result);
    }
}
