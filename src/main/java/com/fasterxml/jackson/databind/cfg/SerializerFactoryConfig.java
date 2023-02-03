package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.ArrayIterator;

/**
 * Configuration settings container class for
 * {@link SerializerFactory} implementations.
 */
public final class SerializerFactoryConfig
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;
    /**
     * Constant for empty <code>Serializers</code> array (which by definition
     * is stateless and reusable)
     */
    protected final static Serializers[] NO_SERIALIZERS = new Serializers[0];

    protected final static BeanSerializerModifier[] NO_MODIFIERS = new BeanSerializerModifier[0];

    /**
     * List of providers for additional serializers, checked before considering default
     * basic or bean serialializers.
     */
    protected final Serializers[] _additionalSerializers;

    /**
     * List of providers for additional key serializers, checked before considering default
     * key serialializers.
     */
    protected final Serializers[] _additionalKeySerializers;

    /**
     * List of modifiers that can change the way {@link BeanSerializer} instances
     * are configured and constructed.
     */
    protected final BeanSerializerModifier[] _modifiers;

    public SerializerFactoryConfig() {
        this(null, null, null);
    }

    protected SerializerFactoryConfig(Serializers[] allAdditionalSerializers,
            Serializers[] allAdditionalKeySerializers,
            BeanSerializerModifier[] modifiers)
    {
        _additionalSerializers = (allAdditionalSerializers == null) ?
                NO_SERIALIZERS : allAdditionalSerializers;
        _additionalKeySerializers = (allAdditionalKeySerializers == null) ?
                NO_SERIALIZERS : allAdditionalKeySerializers;
        _modifiers = (modifiers == null) ? NO_MODIFIERS : modifiers;
    }

    public SerializerFactoryConfig withAdditionalSerializers(Serializers additional)
    {
        if (additional == null) {
            throw new IllegalArgumentException("Cannot pass null Serializers");
        }
        Serializers[] all = ArrayBuilders.insertInListNoDup(_additionalSerializers, additional);
        return new SerializerFactoryConfig(all, _additionalKeySerializers, _modifiers);
    }

    public SerializerFactoryConfig withAdditionalKeySerializers(Serializers additional)
    {
        if (additional == null) {
            throw new IllegalArgumentException("Cannot pass null Serializers");
        }
        Serializers[] all = ArrayBuilders.insertInListNoDup(_additionalKeySerializers, additional);
        return new SerializerFactoryConfig(_additionalSerializers, all, _modifiers);
    }

    public SerializerFactoryConfig withSerializerModifier(BeanSerializerModifier modifier)
    {
        if (modifier == null) {
            throw new IllegalArgumentException("Cannot pass null modifier");
        }
        BeanSerializerModifier[] modifiers = ArrayBuilders.insertInListNoDup(_modifiers, modifier);
        return new SerializerFactoryConfig(_additionalSerializers, _additionalKeySerializers, modifiers);
    }

    public boolean hasSerializers() { return _additionalSerializers.length > 0; }
    public boolean hasKeySerializers() { return _additionalKeySerializers.length > 0; }
    public boolean hasSerializerModifiers() { return _modifiers.length > 0; }
    public Iterable<Serializers> serializers() { return new ArrayIterator<Serializers>(_additionalSerializers); }
    public Iterable<Serializers> keySerializers() { return new ArrayIterator<Serializers>(_additionalKeySerializers); }
    public Iterable<BeanSerializerModifier> serializerModifiers() { return new ArrayIterator<BeanSerializerModifier>(_modifiers); }
}
