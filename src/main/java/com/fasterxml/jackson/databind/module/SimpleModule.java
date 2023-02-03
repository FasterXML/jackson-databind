package com.fasterxml.jackson.databind.module;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

/**
 * Vanilla {@link com.fasterxml.jackson.databind.Module} implementation that allows registration
 * of serializers and deserializers, bean serializer
 * and deserializer modifiers, registration of subtypes and mix-ins
 * as well as some other commonly
 * needed aspects (addition of custom {@link AbstractTypeResolver}s,
 * {@link com.fasterxml.jackson.databind.deser.ValueInstantiator}s).
 *<p>
 * NOTE: although it is not expected that sub-types should need to
 * override {@link #setupModule(SetupContext)} method, if they choose
 * to do so they MUST call {@code super.setupModule(context);}
 * to ensure that registration works as expected.
 *<p>
 * WARNING: when registering {@link JsonSerializer}s and {@link JsonDeserializer}s,
 * only type erased {@code Class} is compared: this means that usually you should
 * NOT use this implementation for registering structured types such as
 * {@link java.util.Collection}s or {@link java.util.Map}s: this because parametric
 * type information will not be considered and you may end up having "wrong" handler
 * for your type.
 * What you need to do, instead, is to implement {@link com.fasterxml.jackson.databind.deser.Deserializers}
 * and/or {@link com.fasterxml.jackson.databind.ser.Serializers} callbacks to match full type
 * signatures (with {@link JavaType}).
 */
public class SimpleModule
    extends com.fasterxml.jackson.databind.Module
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L; // 2.5.0

    // 16-Jun-2021, tatu: For [databind#3110], generate actual unique ids
    //   for SimpleModule instances (System.identityHashCode(...) is close
    //   but not quite it...
    private static final AtomicInteger MODULE_ID_SEQ = new AtomicInteger(1);

    protected final String _name;
    protected final Version _version;

    /**
     * Flag that indicates whether module was given an explicit name
     * or not. Distinction is used to determine whether method
     * {@link #getTypeId()} should return name (yes, if explicit) or
     * {@code null} (if no explicit name was passed).
     *
     * @since 2.13
     */
    protected final boolean _hasExplicitName;

    protected SimpleSerializers _serializers = null;
    protected SimpleDeserializers _deserializers = null;

    protected SimpleSerializers _keySerializers = null;
    protected SimpleKeyDeserializers _keyDeserializers = null;

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

    /**
     * @since 2.2
     */
    protected BeanDeserializerModifier _deserializerModifier = null;

    /**
     * @since 2.2
     */
    protected BeanSerializerModifier _serializerModifier = null;

    /**
     * Lazily-constructed map that contains mix-in definitions, indexed
     * by target class, value being mix-in to apply.
     */
    protected HashMap<Class<?>, Class<?>> _mixins = null;

    /**
     * Set of subtypes to register, if any.
     */
    protected LinkedHashSet<NamedType> _subtypes = null;

    /**
     * @since 2.3
     */
    protected PropertyNamingStrategy _namingStrategy = null;

    /*
    /**********************************************************
    /* Life-cycle: creation
    /**********************************************************
     */

    /**
     * Constructors that should only be used for non-reusable
     * convenience modules used by app code: "real" modules should
     * use actual name and version number information.
     */
    public SimpleModule() {
        // can't chain when making reference to 'this'
        // note: generate different name for direct instantiation, sub-classing;
        // this to avoid collision in former case while still addressing
        // [databind#3110]
        _name = (getClass() == SimpleModule.class)
                ? "SimpleModule-"+MODULE_ID_SEQ.getAndIncrement()
                : getClass().getName();
        _version = Version.unknownVersion();
        // 07-Jun-2021, tatu: [databind#3110] Not passed explicitly so...
        _hasExplicitName = false;
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
     * including name from {@link Version#getArtifactId()}.
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
        _name = name;
        _version = version;
        // 07-Jun-2021, tatu: [databind#3110] Is passed explicitly (may be `null`)
        _hasExplicitName = true;
    }

    /**
     * @since 2.1
     */
    public SimpleModule(String name, Version version,
            Map<Class<?>,JsonDeserializer<?>> deserializers) {
        this(name, version, deserializers, null);
    }

    /**
     * @since 2.1
     */
    public SimpleModule(String name, Version version,
            List<JsonSerializer<?>> serializers) {
        this(name, version, null, serializers);
    }

    /**
     * @since 2.1
     */
    public SimpleModule(String name, Version version,
            Map<Class<?>,JsonDeserializer<?>> deserializers,
            List<JsonSerializer<?>> serializers)
    {
        _name = name;
        // 07-Jun-2021, tatu: [databind#3110] Is passed explicitly (may be `null`)
        _hasExplicitName = true;
        _version = version;
        if (deserializers != null) {
            _deserializers = new SimpleDeserializers(deserializers);
        }
        if (serializers != null) {
            _serializers = new SimpleSerializers(serializers);
        }
    }

    /**
     * Since instances are likely to be custom, implementation returns
     * <code>null</code> if (but only if!) this class is directly instantiated;
     * but class name (default impl) for sub-classes.
     */
    @Override
    public Object getTypeId()
    {
        // 07-Jun-2021, tatu: [databind#3110] Return Type Id if name was
        //    explicitly given
        if (_hasExplicitName) {
            return _name;
        }
        // Otherwise behavior same as with 2.12: no registration id for "throw-away"
        // instances (to avoid bogus conflicts if user just instantiates SimpleModule)

        // Note: actually... always returning `supet.getTypeId()` should be fine since
        // that would return generated id? Let's do that actually.
        if (getClass() == SimpleModule.class) {
            return _name;
        }
        // And for what it is worth, this should usually do the same and we could
        // in fact always just return `_name`. But leaving as-is for now.
        return super.getTypeId();
    }

    /*
    /**********************************************************
    /* Simple setters to allow overriding
    /**********************************************************
     */

    /**
     * Resets all currently configured serializers.
     */
    public void setSerializers(SimpleSerializers s) {
        _serializers = s;
    }

    /**
     * Resets all currently configured deserializers.
     */
    public void setDeserializers(SimpleDeserializers d) {
        _deserializers = d;
    }

    /**
     * Resets all currently configured key serializers.
     */
    public void setKeySerializers(SimpleSerializers ks) {
        _keySerializers = ks;
    }

    /**
     * Resets all currently configured key deserializers.
     */
    public void setKeyDeserializers(SimpleKeyDeserializers kd) {
        _keyDeserializers = kd;
    }

    /**
     * Resets currently configured abstract type mappings
     */
    public void setAbstractTypes(SimpleAbstractTypeResolver atr) {
        _abstractTypes = atr;
    }

    /**
     * Resets all currently configured value instantiators
     */
    public void setValueInstantiators(SimpleValueInstantiators svi) {
        _valueInstantiators = svi;
    }

    /**
     * @since 2.2
     */
    public SimpleModule setDeserializerModifier(BeanDeserializerModifier mod) {
        _deserializerModifier = mod;
        return this;
    }

    /**
     * @since 2.2
     */
    public SimpleModule setSerializerModifier(BeanSerializerModifier mod) {
        _serializerModifier = mod;
        return this;
    }

    /**
     * @since 2.3
     */
    protected SimpleModule setNamingStrategy(PropertyNamingStrategy naming) {
        _namingStrategy = naming;
        return this;
    }

    /*
    /**********************************************************
    /* Configuration methods, adding serializers
    /**********************************************************
     */

    /**
     * Method for adding serializer to handle type that the serializer claims to handle
     * (see {@link JsonSerializer#handledType()}).
     *<p>
     * WARNING! Type matching only uses type-erased {@code Class} and should NOT
     * be used when registering serializers for generic types like
     * {@link java.util.Collection} and {@link java.util.Map}.
     */
    public SimpleModule addSerializer(JsonSerializer<?> ser)
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
     */
    public <T> SimpleModule addSerializer(Class<? extends T> type, JsonSerializer<T> ser)
    {
        _checkNotNull(type, "type to register serializer for");
        _checkNotNull(ser, "serializer");
        if (_serializers == null) {
            _serializers = new SimpleSerializers();
        }
        _serializers.addSerializer(type, ser);
        return this;
    }

    public <T> SimpleModule addKeySerializer(Class<? extends T> type, JsonSerializer<T> ser)
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
    /**********************************************************
    /* Configuration methods, adding deserializers
    /**********************************************************
     */

    /**
     * Method for adding deserializer to handle specified type.
     *<p>
     * WARNING! Type matching only uses type-erased {@code Class} and should NOT
     * be used when registering serializers for generic types like
     * {@link java.util.Collection} and {@link java.util.Map}.
     */
    public <T> SimpleModule addDeserializer(Class<T> type, JsonDeserializer<? extends T> deser)
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
    /**********************************************************
    /* Configuration methods, type mapping
    /**********************************************************
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
     *
     * @since 2.9
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
    /**********************************************************
    /* Configuration methods, add other handlers
    /**********************************************************
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
    /**********************************************************
    /* Module impl
    /**********************************************************
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
            context.addBeanDeserializerModifier(_deserializerModifier);
        }
        if (_serializerModifier != null) {
            context.addBeanSerializerModifier(_serializerModifier);
        }
        if (_subtypes != null && _subtypes.size() > 0) {
            context.registerSubtypes(_subtypes.toArray(new NamedType[_subtypes.size()]));
        }
        if (_namingStrategy != null) {
            context.setNamingStrategy(_namingStrategy);
        }
        if (_mixins != null) {
            for (Map.Entry<Class<?>,Class<?>> entry : _mixins.entrySet()) {
                context.setMixInAnnotations(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public Version version() { return _version; }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * @since 2.9
     */
    protected void _checkNotNull(Object thingy, String type)
    {
        if (thingy == null) {
            throw new IllegalArgumentException(String.format(
                    "Cannot pass `null` as %s", type));
        }
    }
}
