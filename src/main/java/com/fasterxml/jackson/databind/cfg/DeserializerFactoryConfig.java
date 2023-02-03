package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.AbstractTypeResolver;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.std.StdKeyDeserializers;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.ArrayIterator;

/**
 * Configuration settings container class for {@link DeserializerFactory}.
 */
public class DeserializerFactoryConfig
    implements java.io.Serializable // since 2.1
{
    private static final long serialVersionUID = 1L; // since 2.5

    protected final static Deserializers[] NO_DESERIALIZERS = new Deserializers[0];
    protected final static BeanDeserializerModifier[] NO_MODIFIERS = new BeanDeserializerModifier[0];
    protected final static AbstractTypeResolver[] NO_ABSTRACT_TYPE_RESOLVERS = new AbstractTypeResolver[0];
    protected final static ValueInstantiators[] NO_VALUE_INSTANTIATORS = new ValueInstantiators[0];

    /**
     * By default we plug default key deserializers using as "just another" set of
     * of key deserializers.
     *
     * @since 2.2
     */
    protected final static KeyDeserializers[] DEFAULT_KEY_DESERIALIZERS = new KeyDeserializers[] {
        new StdKeyDeserializers()
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
    protected final BeanDeserializerModifier[] _modifiers;

    /**
     * List of objects that may be able to resolve abstract types to
     * concrete types. Used by functionality like "mr Bean" to materialize
     * types as needed.
     */
    protected final AbstractTypeResolver[] _abstractTypeResolvers;

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
        this(null, null, null, null, null);
    }

    /**
     * Copy-constructor that will create an instance that contains defined
     * set of additional deserializer providers.
     */
    protected DeserializerFactoryConfig(Deserializers[] allAdditionalDeserializers,
            KeyDeserializers[] allAdditionalKeyDeserializers,
            BeanDeserializerModifier[] modifiers,
            AbstractTypeResolver[] atr,
            ValueInstantiators[] vi)
    {
        _additionalDeserializers = (allAdditionalDeserializers == null) ?
                NO_DESERIALIZERS : allAdditionalDeserializers;
        _additionalKeyDeserializers = (allAdditionalKeyDeserializers == null) ?
                DEFAULT_KEY_DESERIALIZERS : allAdditionalKeyDeserializers;
        _modifiers = (modifiers == null) ? NO_MODIFIERS : modifiers;
        _abstractTypeResolvers = (atr == null) ? NO_ABSTRACT_TYPE_RESOLVERS : atr;
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
                _abstractTypeResolvers, _valueInstantiators);
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
                _abstractTypeResolvers, _valueInstantiators);
    }

    /**
     * Fluent/factory method used to construct a configuration object that
     * has same configuration as this instance plus one additional
     * deserialiazer modifier. Added modifier has the highest priority (that is, it
     * gets called before any already registered modifier).
     */
    public DeserializerFactoryConfig withDeserializerModifier(BeanDeserializerModifier modifier)
    {
        if (modifier == null) {
            throw new IllegalArgumentException("Cannot pass null modifier");
        }
        BeanDeserializerModifier[] all = ArrayBuilders.insertInListNoDup(_modifiers, modifier);
        return new DeserializerFactoryConfig(_additionalDeserializers, _additionalKeyDeserializers, all,
                _abstractTypeResolvers, _valueInstantiators);
    }

    /**
     * Fluent/factory method used to construct a configuration object that
     * has same configuration as this instance plus one additional
     * abstract type resolver.
     * Added resolver has the highest priority (that is, it
     * gets called before any already registered resolver).
     */
    public DeserializerFactoryConfig withAbstractTypeResolver(AbstractTypeResolver resolver)
    {
        if (resolver == null) {
            throw new IllegalArgumentException("Cannot pass null resolver");
        }
        AbstractTypeResolver[] all = ArrayBuilders.insertInListNoDup(_abstractTypeResolvers, resolver);
        return new DeserializerFactoryConfig(_additionalDeserializers, _additionalKeyDeserializers, _modifiers,
                all, _valueInstantiators);
    }

    /**
     * Fluent/factory method used to construct a configuration object that
     * has same configuration as this instance plus specified additional
     * value instantiator provider object.
     * Added instantiator provider has the highest priority (that is, it
     * gets called before any already registered resolver).
     *
     * @param instantiators Object that can provide {@link com.fasterxml.jackson.databind.deser.ValueInstantiator}s for
     *    constructing POJO values during deserialization
     */
    public DeserializerFactoryConfig withValueInstantiators(ValueInstantiators instantiators)
    {
        if (instantiators == null) {
            throw new IllegalArgumentException("Cannot pass null resolver");
        }
        ValueInstantiators[] all = ArrayBuilders.insertInListNoDup(_valueInstantiators, instantiators);
        return new DeserializerFactoryConfig(_additionalDeserializers, _additionalKeyDeserializers, _modifiers,
                _abstractTypeResolvers, all);
    }

    public boolean hasDeserializers() { return _additionalDeserializers.length > 0; }

    public boolean hasKeyDeserializers() { return _additionalKeyDeserializers.length > 0; }

    public boolean hasDeserializerModifiers() { return _modifiers.length > 0; }

    public boolean hasAbstractTypeResolvers() { return _abstractTypeResolvers.length > 0; }

    public boolean hasValueInstantiators() { return _valueInstantiators.length > 0; }

    public Iterable<Deserializers> deserializers() {
        return new ArrayIterator<Deserializers>(_additionalDeserializers);
    }

    public Iterable<KeyDeserializers> keyDeserializers() {
        return new ArrayIterator<KeyDeserializers>(_additionalKeyDeserializers);
    }

    public Iterable<BeanDeserializerModifier> deserializerModifiers() {
        return new ArrayIterator<BeanDeserializerModifier>(_modifiers);
    }

    public Iterable<AbstractTypeResolver> abstractTypeResolvers() {
        return new ArrayIterator<AbstractTypeResolver>(_abstractTypeResolvers);
    }

    public Iterable<ValueInstantiators> valueInstantiators() {
        return new ArrayIterator<ValueInstantiators>(_valueInstantiators);
    }
}
