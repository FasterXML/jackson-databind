package tools.jackson.databind.module;

import java.util.*;

import tools.jackson.core.Version;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.util.UniqueId;

/**
 * Vanilla {@link JacksonModule} implementation that allows registration
 * of serializers and deserializers, bean serializer
 * and deserializer modifiers, registration of subtypes and mix-ins
 * as well as some other commonly
 * needed aspects (addition of custom {@link AbstractTypeResolver}s,
 * {@link tools.jackson.databind.deser.ValueInstantiator}s).
 *<p>
 * NOTE: although it is not expected that sub-types should need to
 * override {@link #setupModule(SetupContext)} method, if they choose
 * to do so they MUST call {@code super.setupModule(context);}
 * to ensure that registration works as expected.
 *<p>
 * WARNING: when registering {@link ValueSerializer}s and {@link ValueDeserializer}s,
 * only type erased {@code Class} is compared: this means that usually you should
 * NOT use this implementation for registering structured types such as
 * {@link java.util.Collection}s or {@link java.util.Map}s: this because parametric
 * type information will not be considered and you may end up having "wrong" handler
 * for your type.
 * What you need to do, instead, is to implement {@link tools.jackson.databind.deser.Deserializers}
 * and/or {@link tools.jackson.databind.ser.Serializers} callbacks to match full type
 * signatures (with {@link JavaType}).
 */
public class SimpleModule
    extends JacksonModule
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    protected final String _name;
    protected final Version _version;

    /**
     * Unique id generated to avoid instances from ever matching so all
     * registrations succeed.
     *<p>
     * NOTE! If serialization of SimpleModule instance needed, should be
     * {@link java.io.Serializable}.
     *
     * @since 3.0
     */
    protected final Object _id;

    protected SimpleSerializers _serializers = null;
    protected SimpleDeserializers _deserializers = null;

    protected SimpleSerializers _keySerializers = null;
    protected SimpleKeyDeserializers _keyDeserializers = null;

    protected ValueSerializer<?> _defaultNullKeySerializer = null;
    protected ValueSerializer<?> _defaultNullValueSerializer = null;

    /**
     * Lazily-constructed resolver used for storing mappings from
     * abstract classes to more specific implementing classes
     * (which may be abstract or concrete)
     */
    protected SimpleAbstractTypeResolver _abstractTypes = null;

    /**
     * Lazily-constructed resolver used for storing mappings from
     * abstract classes to more specific implementing classes
     * (which may be abstract or concrete)
     */
    protected SimpleValueInstantiators _valueInstantiators = null;

    protected ValueDeserializerModifier _deserializerModifier = null;

    protected ValueSerializerModifier _serializerModifier = null;

    /**
     * Lazily-constructed map that contains mix-in definitions, indexed
     * by target class, value being mix-in to apply.
     */
    protected HashMap<Class<?>, Class<?>> _mixins = null;

    /**
     * Set of subtypes to register, if any.
     */
    protected LinkedHashSet<NamedType> _subtypes = null;

    protected PropertyNamingStrategy _namingStrategy = null;

    /*
    /**********************************************************************
    /* Life-cycle: creation
    /**********************************************************************
     */

    /**
     * Constructors that should only be used for non-reusable
     * convenience modules used by app code: "real" modules should
     * use actual name and version number information.
     */
    public SimpleModule()
    {
        this(null, Version.unknownVersion());
    }

    /**
     * Convenience constructor that will default version to
     * {@link Version#unknownVersion()}.
     */
    public SimpleModule(String name) {
        this(name, Version.unknownVersion());
    }

    /**
     * Convenience constructor that will use specified Version,
     * including name from {@link Version#getArtifactId()}
     */
    public SimpleModule(Version version) {
        this(version.getArtifactId(), version);
    }

    /**
     * Constructor to use for actual reusable modules.
     * ObjectMapper may use name as identifier to notice attempts
     * for multiple registrations of the same module (although it
     * does not have to).
     *
     * @param name Unique name of the module
     * @param version Version of the module
     */
    public SimpleModule(String name, Version version) {
        this(name, version, null);
    }

    public SimpleModule(String name, Version version, Object registrationId) {
        if (name == null) {
            // So: if constructing plain `SimpleModule`, instances assumed to be
            // distinct, not same, so generate unique id. But if sub-class,
            // class name assumed.
            if (getClass() == SimpleModule.class) {
                if (registrationId == null) {
                    registrationId = UniqueId.create("SimpleModule-");
                }
                name = "SimpleModule-"+registrationId;
            } else {
                name = getClass().getName();
                if (registrationId == null) {
                    registrationId = name;
                }
            }
        } else if (registrationId == null) {
            registrationId = name;
        }
        _name = name;
        _version = version;
        _id = registrationId;
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    @Override
    public Version version() { return _version; }

    /**
     * Since instances are likely to be custom, implementation returns
     * <code>null</code> if (but only if!) this class is directly instantiated;
     * but class name (default impl) for sub-classes.
     */
    @Override
    public Object getRegistrationId() {
        return _id;
    }

    /*
    /**********************************************************************
    /* Simple setters to allow overriding
    /**********************************************************************
     */

    /**
     * Resets all currently configured serializers.
     */
    public SimpleModule setSerializers(SimpleSerializers s) {
        _serializers = s;
        return this;
    }

    /**
     * Resets all currently configured deserializers.
     */
    public SimpleModule setDeserializers(SimpleDeserializers d) {
        _deserializers = d;
        return this;
    }

    /**
     * Resets all currently configured key serializers.
     */
    public SimpleModule setKeySerializers(SimpleSerializers ks) {
        _keySerializers = ks;
        return this;
    }

    /**
     * Resets all currently configured key deserializers.
     */
    public SimpleModule setKeyDeserializers(SimpleKeyDeserializers kd) {
        _keyDeserializers = kd;
        return this;
    }

    public SimpleModule setDefaultNullKeySerializer(ValueSerializer<?> ser) {
        _defaultNullKeySerializer = ser;
        return this;
    }

    public SimpleModule setDefaultNullValueSerializer(ValueSerializer<?> ser) {
        _defaultNullValueSerializer = ser;
        return this;
    }

    /**
     * Resets currently configured abstract type mappings
     */
    public SimpleModule setAbstractTypes(SimpleAbstractTypeResolver atr) {
        _abstractTypes = atr;
        return this;
    }

    /**
     * Resets all currently configured value instantiators
     */
    public SimpleModule setValueInstantiators(SimpleValueInstantiators svi) {
        _valueInstantiators = svi;
        return this;
    }

    public SimpleModule setDeserializerModifier(ValueDeserializerModifier mod) {
        _deserializerModifier = mod;
        return this;
    }

    public SimpleModule setSerializerModifier(ValueSerializerModifier mod) {
        _serializerModifier = mod;
        return this;
    }

    protected SimpleModule setNamingStrategy(PropertyNamingStrategy naming) {
        _namingStrategy = naming;
        return this;
    }

    /*
    /**********************************************************************
    /* Configuration methods, adding serializers
    /**********************************************************************
     */

    /**
     * Method for adding serializer to handle type that the serializer claims to handle
     * (see {@link ValueSerializer#handledType()}).
     *<p>
     * WARNING! Type matching only uses type-erased {@code Class} and should NOT
     * be used when registering serializers for generic types like
     * {@link java.util.Collection} and {@link java.util.Map}.
     *<p>
     * WARNING! "Last one wins" rule is applied.
     * Possible earlier addition of a serializer for a given Class will be replaced.
     */
    public SimpleModule addSerializer(ValueSerializer<?> ser)
    {
        _checkNotNull(ser, "serializer");
        if (_serializers == null) {
            _serializers = new SimpleSerializers();
        }
        _serializers.addSerializer(ser);
        return this;
    }

    /**
     * Method for adding serializer to handle values of specific type.
     *<p>
     * WARNING! Type matching only uses type-erased {@code Class} and should NOT
     * be used when registering serializers for generic types like
     * {@link java.util.Collection} and {@link java.util.Map}.
     *<p>
     * WARNING! "Last one wins" rule is applied.
     * Possible earlier addition of a serializer for a given Class will be replaced.
     */
    public <T> SimpleModule addSerializer(Class<? extends T> type, ValueSerializer<T> ser)
    {
        _checkNotNull(type, "type to register serializer for");
        _checkNotNull(ser, "serializer");
        if (_serializers == null) {
            _serializers = new SimpleSerializers();
        }
        _serializers.addSerializer(type, ser);
        return this;
    }

    public <T> SimpleModule addKeySerializer(Class<? extends T> type, ValueSerializer<T> ser)
    {
        _checkNotNull(type, "type to register key serializer for");
        _checkNotNull(ser, "key serializer");
        if (_keySerializers == null) {
            _keySerializers = new SimpleSerializers();
        }
        _keySerializers.addSerializer(type, ser);
        return this;
    }

    /*
    /**********************************************************************
    /* Configuration methods, adding deserializers
    /**********************************************************************
     */

    /**
     * Method for adding deserializer to handle specified type.
     *<p>
     * WARNING! Type matching only uses type-erased {@code Class} and should NOT
     * be used when registering serializers for generic types like
     * {@link java.util.Collection} and {@link java.util.Map}.
     *<p>
     * WARNING! "Last one wins" rule is applied.
     * Possible earlier addition of a serializer for a given Class will be replaced.
     */
    public <T> SimpleModule addDeserializer(Class<T> type, ValueDeserializer<? extends T> deser)
    {
        _checkNotNull(type, "type to register deserializer for");
        _checkNotNull(deser, "deserializer");
        if (_deserializers == null) {
            _deserializers = new SimpleDeserializers();
        }
        _deserializers.addDeserializer(type, deser);
        return this;
    }

    public SimpleModule addKeyDeserializer(Class<?> type, KeyDeserializer deser)
    {
        _checkNotNull(type, "type to register key deserializer for");
        _checkNotNull(deser, "key deserializer");
        if (_keyDeserializers == null) {
            _keyDeserializers = new SimpleKeyDeserializers();
        }
        _keyDeserializers.addDeserializer(type, deser);
        return this;
    }

    /*
    /**********************************************************************
    /* Configuration methods, type mapping
    /**********************************************************************
     */

    /**
     * Lazily-constructed resolver used for storing mappings from
     * abstract classes to more specific implementing classes
     * (which may be abstract or concrete)
     */
    public <T> SimpleModule addAbstractTypeMapping(Class<T> superType,
            Class<? extends T> subType)
    {
        _checkNotNull(superType, "abstract type to map");
        _checkNotNull(subType, "concrete type to map to");
        if (_abstractTypes == null) {
            _abstractTypes = new SimpleAbstractTypeResolver();
        }
        // note: addMapping() will verify arguments
        _abstractTypes = _abstractTypes.addMapping(superType, subType);
        return this;
    }

    /**
     * Method for adding set of subtypes to be registered with
     * {@link ObjectMapper}
     * this is an alternative to using annotations in super type to indicate subtypes.
     */
    public SimpleModule registerSubtypes(Class<?> ... subtypes)
    {
        if (_subtypes == null) {
            _subtypes = new LinkedHashSet<>();
        }
        for (Class<?> subtype : subtypes) {
            _checkNotNull(subtype, "subtype to register");
            _subtypes.add(new NamedType(subtype));
        }
        return this;
    }

    /**
     * Method for adding set of subtypes (along with type name to use) to be registered with
     * {@link ObjectMapper}
     * this is an alternative to using annotations in super type to indicate subtypes.
     */
    public SimpleModule registerSubtypes(NamedType ... subtypes)
    {
        if (_subtypes == null) {
            _subtypes = new LinkedHashSet<>();
        }
        for (NamedType subtype : subtypes) {
            _checkNotNull(subtype, "subtype to register");
            _subtypes.add(subtype);
        }
        return this;
    }

    /**
     * Method for adding set of subtypes (along with type name to use) to be registered with
     * {@link ObjectMapper}
     * this is an alternative to using annotations in super type to indicate subtypes.
     */
    public SimpleModule registerSubtypes(Collection<Class<?>> subtypes)
    {
        if (_subtypes == null) {
            _subtypes = new LinkedHashSet<>();
        }
        for (Class<?> subtype : subtypes) {
            _checkNotNull(subtype, "subtype to register");
            _subtypes.add(new NamedType(subtype));
        }
        return this;
    }

    /*
    /**********************************************************************
    /* Configuration methods, add other handlers
    /**********************************************************************
     */

    /**
     * Method for registering {@link ValueInstantiator} to use when deserializing
     * instances of type <code>beanType</code>.
     *<p>
     * Instantiator is
     * registered when module is registered for <code>ObjectMapper</code>.
     */
    public SimpleModule addValueInstantiator(Class<?> beanType, ValueInstantiator inst)
    {
        _checkNotNull(beanType, "class to register value instantiator for");
        _checkNotNull(inst, "value instantiator");
        if (_valueInstantiators == null) {
            _valueInstantiators = new SimpleValueInstantiators();
        }
        _valueInstantiators = _valueInstantiators.addValueInstantiator(beanType, inst);
        return this;
    }

    /**
     * Method for specifying that annotations define by <code>mixinClass</code>
     * should be "mixed in" with annotations that <code>targetType</code>
     * has (as if they were directly included on it!).
     *<p>
     * Mix-in annotations are
     * registered when module is registered for <code>ObjectMapper</code>.
     */
    public SimpleModule setMixInAnnotation(Class<?> targetType, Class<?> mixinClass)
    {
        _checkNotNull(targetType, "target type");
        _checkNotNull(mixinClass, "mixin class");
        if (_mixins == null) {
            _mixins = new HashMap<Class<?>, Class<?>>();
        }
        _mixins.put(targetType, mixinClass);
        return this;
    }

    /*
    /**********************************************************************
    /* Module impl
    /**********************************************************************
     */

    @Override
    public String getModuleName() {
        return _name;
    }

    /**
     * Standard implementation handles registration of all configured
     * customizations: it is important that sub-classes call this
     * implementation (usually before additional custom logic)
     * if they choose to override it; otherwise customizations
     * will not be registered.
     */
    @Override
    public void setupModule(SetupContext context)
    {
        if (_serializers != null) {
            context.addSerializers(_serializers);
        }
        if (_deserializers != null) {
            context.addDeserializers(_deserializers);
        }
        if (_keySerializers != null) {
            context.addKeySerializers(_keySerializers);
        }
        if (_keyDeserializers != null) {
            context.addKeyDeserializers(_keyDeserializers);
        }
        if (_abstractTypes != null) {
            context.addAbstractTypeResolver(_abstractTypes);
        }
        if (_valueInstantiators != null) {
            context.addValueInstantiators(_valueInstantiators);
        }
        if (_deserializerModifier != null) {
            context.addDeserializerModifier(_deserializerModifier);
        }
        if (_serializerModifier != null) {
            context.addSerializerModifier(_serializerModifier);
        }
        if (_defaultNullKeySerializer != null) {
            context.overrideDefaultNullKeySerializer(_defaultNullKeySerializer);
        }
        if (_defaultNullValueSerializer != null) {
            context.overrideDefaultNullValueSerializer(_defaultNullValueSerializer);
        }
        if (_subtypes != null && _subtypes.size() > 0) {
            context.registerSubtypes(_subtypes.toArray(new NamedType[0]));
        }
        if (_mixins != null) {
            for (Map.Entry<Class<?>,Class<?>> entry : _mixins.entrySet()) {
                context.setMixIn(entry.getKey(), entry.getValue());
            }
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected void _checkNotNull(Object thingy, String type)
    {
        if (thingy == null) {
            throw new IllegalArgumentException(String.format(
                    "Cannot pass `null` as %s", type));
        }
    }
}
