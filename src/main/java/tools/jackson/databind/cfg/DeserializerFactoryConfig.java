package tools.jackson.databind.cfg;

import tools.jackson.databind.deser.*;
import tools.jackson.databind.deser.bean.BeanDeserializer;
import tools.jackson.databind.deser.jdk.JDKKeyDeserializers;
import tools.jackson.databind.util.ArrayBuilders;
import tools.jackson.databind.util.ArrayIterator;

/**
 * Configuration settings container class for {@link DeserializerFactory}.
 */
public class DeserializerFactoryConfig
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    protected final static Deserializers[] NO_DESERIALIZERS = new Deserializers[0];
    protected final static ValueDeserializerModifier[] NO_MODIFIERS = new ValueDeserializerModifier[0];
    protected final static ValueInstantiators[] NO_VALUE_INSTANTIATORS = new ValueInstantiators[0];

    /**
     * By default we plug default key deserializers using as "just another" set of
     * of key deserializers.
     */
    protected final static KeyDeserializers[] DEFAULT_KEY_DESERIALIZERS = new KeyDeserializers[] {
        new JDKKeyDeserializers()
    };

    /**
     * List of providers for additional deserializers, checked before considering default
     * basic or bean deserializers.
     */
    protected final Deserializers[] _additionalDeserializers;

    /**
     * List of providers for additional key deserializers, checked before considering
     * standard key deserializers.
     */
    protected final KeyDeserializers[] _additionalKeyDeserializers;

    /**
     * List of modifiers that can change the way {@link BeanDeserializer} instances
     * are configured and constructed.
     */
    protected final ValueDeserializerModifier[] _modifiers;

    /**
     * List of objects that know how to create instances of POJO types;
     * possibly using custom construction (non-annoted constructors; factory
     * methods external to value type etc).
     * Used to support objects that are created using non-standard methods;
     * or to support post-constructor functionality.
     */
    protected final ValueInstantiators[] _valueInstantiators;

    /**
     * Constructor for creating basic configuration with no additional
     * handlers.
     */
    public DeserializerFactoryConfig() {
        this(null, null, null, null);
    }

    /**
     * Copy-constructor that will create an instance that contains defined
     * set of additional deserializer providers.
     */
    protected DeserializerFactoryConfig(Deserializers[] allAdditionalDeserializers,
            KeyDeserializers[] allAdditionalKeyDeserializers,
            ValueDeserializerModifier[] modifiers,
            ValueInstantiators[] vi)
    {
        _additionalDeserializers = (allAdditionalDeserializers == null) ?
                NO_DESERIALIZERS : allAdditionalDeserializers;
        _additionalKeyDeserializers = (allAdditionalKeyDeserializers == null) ?
                DEFAULT_KEY_DESERIALIZERS : allAdditionalKeyDeserializers;
        _modifiers = (modifiers == null) ? NO_MODIFIERS : modifiers;
        _valueInstantiators = (vi == null) ? NO_VALUE_INSTANTIATORS : vi;
    }

    /**
     * Fluent/factory method used to construct a configuration object that
     * has same deserializer providers as this instance, plus one specified
     * as argument. Additional provider will be added before existing ones,
     * meaning it has priority over existing definitions.
     */
    public DeserializerFactoryConfig withAdditionalDeserializers(Deserializers additional)
    {
        if (additional == null) {
            throw new IllegalArgumentException("Cannot pass null Deserializers");
        }
        Deserializers[] all = ArrayBuilders.insertInListNoDup(_additionalDeserializers, additional);
        return new DeserializerFactoryConfig(all, _additionalKeyDeserializers, _modifiers,
                _valueInstantiators);
    }

    /**
     * Fluent/factory method used to construct a configuration object that
     * has same key deserializer providers as this instance, plus one specified
     * as argument. Additional provider will be added before existing ones,
     * meaning it has priority over existing definitions.
     */
    public DeserializerFactoryConfig withAdditionalKeyDeserializers(KeyDeserializers additional)
    {
        if (additional == null) {
            throw new IllegalArgumentException("Cannot pass null KeyDeserializers");
        }
        KeyDeserializers[] all = ArrayBuilders.insertInListNoDup(_additionalKeyDeserializers, additional);
        return new DeserializerFactoryConfig(_additionalDeserializers, all, _modifiers,
                _valueInstantiators);
    }

    /**
     * Fluent/factory method used to construct a configuration object that
     * has same configuration as this instance plus one additional
     * deserialiazer modifier. Added modifier has the highest priority (that is, it
     * gets called before any already registered modifier).
     */
    public DeserializerFactoryConfig withDeserializerModifier(ValueDeserializerModifier modifier)
    {
        if (modifier == null) {
            throw new IllegalArgumentException("Cannot pass null modifier");
        }
        ValueDeserializerModifier[] all = ArrayBuilders.insertInListNoDup(_modifiers, modifier);
        return new DeserializerFactoryConfig(_additionalDeserializers, _additionalKeyDeserializers,
                all, _valueInstantiators);
    }

    /**
     * Fluent/factory method used to construct a configuration object that
     * has same configuration as this instance plus specified additional
     * value instantiator provider object.
     * Added instantiator provider has the highest priority (that is, it
     * gets called before any already registered resolver).
     *
     * @param instantiators Object that can provide {@link tools.jackson.databind.deser.ValueInstantiator}s for
     *    constructing POJO values during deserialization
     */
    public DeserializerFactoryConfig withValueInstantiators(ValueInstantiators instantiators)
    {
        if (instantiators == null) {
            throw new IllegalArgumentException("Cannot pass null resolver");
        }
        ValueInstantiators[] all = ArrayBuilders.insertInListNoDup(_valueInstantiators, instantiators);
        return new DeserializerFactoryConfig(_additionalDeserializers, _additionalKeyDeserializers,
                _modifiers, all);
    }

    public boolean hasDeserializers() { return _additionalDeserializers.length > 0; }

    public boolean hasKeyDeserializers() { return _additionalKeyDeserializers.length > 0; }

    public boolean hasDeserializerModifiers() { return _modifiers.length > 0; }

    public boolean hasValueInstantiators() { return _valueInstantiators.length > 0; }

    public Iterable<Deserializers> deserializers() {
        return new ArrayIterator<Deserializers>(_additionalDeserializers);
    }

    public Iterable<KeyDeserializers> keyDeserializers() {
        return new ArrayIterator<KeyDeserializers>(_additionalKeyDeserializers);
    }

    public Iterable<ValueDeserializerModifier> deserializerModifiers() {
        return new ArrayIterator<ValueDeserializerModifier>(_modifiers);
    }

    public Iterable<ValueInstantiators> valueInstantiators() {
        return new ArrayIterator<ValueInstantiators>(_valueInstantiators);
    }
}
