package tools.jackson.databind.cfg;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.util.Snapshottable;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.deser.DeserializerFactory;
import tools.jackson.databind.introspect.ClassIntrospector;
import tools.jackson.databind.introspect.MixInHandler;
import tools.jackson.databind.jsontype.SubtypeResolver;
import tools.jackson.databind.jsontype.TypeResolverProvider;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.SerializerFactory;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.util.LinkedNode;

/**
 * Interface for State object used for preserving initial state of a
 * {@link MapperBuilder} before modules are configured and resulting
 * {@link tools.jackson.databind.ObjectMapper} isn't constructed.
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
    protected final CoercionConfigs _coercionConfigs;

    /*
    /**********************************************************************
    /* Feature flags
    /**********************************************************************
     */

    protected final long _mapperFeatures;
    protected final int _serFeatures, _deserFeatures;
    protected final int _streamReadFeatures, _streamWriteFeatures;
    protected final int _formatReadFeatures, _formatWriteFeatures;

    protected final DatatypeFeatures _datatypeFeatures;

    /*
    /**********************************************************************
    /* Modules
    /**********************************************************************
     */

    /**
     * Modules registered in registration order, if any; `null` if none.
     */
    protected final JacksonModule[] _modules;

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

    protected final ConstructorDetector _ctorDetector;

    /*
    /**********************************************************************
    /* Handlers, other
    /**********************************************************************
     */

    protected final ContextAttributes _defaultAttributes;

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
        _coercionConfigs = Snapshottable.takeSnapshot(src._coercionConfigs);

        // Feature flags (simple ints, no copy needed)
        _mapperFeatures = src._mapperFeatures;
        _serFeatures = src._serFeatures;
        _deserFeatures = src._deserFeatures;
        _datatypeFeatures = src._datatypeFeatures;
        _streamReadFeatures = src._streamReadFeatures;
        _streamWriteFeatures = src._streamWriteFeatures;
        _formatReadFeatures = src._formatReadFeatures;
        _formatWriteFeatures = src._formatWriteFeatures;

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
        _ctorDetector = src._ctorDetector;

        // Other handlers
        _defaultAttributes = Snapshottable.takeSnapshot(src._defaultAttributes);

        // Modules
        if (src._modules == null) {
            _modules = null;
        } else {
            _modules = _toArray(src._modules.values());
        }
    }

    private static JacksonModule[] _toArray(Collection<?> coll)
    {
        if (coll == null || coll.isEmpty()) {
            return null;
        }
        return coll.toArray(new JacksonModule[0]);
    }

    /*
    /**********************************************************************
    /* Configuration access by ObjectMapper
    /**********************************************************************
     */

    public Collection<JacksonModule> modules() {
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
