package com.fasterxml.jackson.databind.cfg;

import java.util.Objects;

import com.fasterxml.jackson.databind.ValueSerializer;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.ser.impl.FailingSerializer;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.ArrayIterator;

/**
 * Configuration settings container class for
 * {@link SerializerFactory} implementations.
 */
public final class SerializerFactoryConfig
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    public final static ValueSerializer<Object> DEFAULT_NULL_KEY_SERIALIZER =
            new FailingSerializer("Null key for a Map not allowed in JSON (use a converting NullKeySerializer?)");

    /**
     * Constant for empty <code>Serializers</code> array (which by definition
     * is stateless and reusable)
     */
    protected final static Serializers[] NO_SERIALIZERS = new Serializers[0];

    protected final static ValueSerializerModifier[] NO_MODIFIERS = new ValueSerializerModifier[0];
    
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
    protected final ValueSerializerModifier[] _modifiers;

    /**
     * Serializer used to output a null value, unless explicitly redefined for property.
     */
    protected final ValueSerializer<Object> _nullValueSerializer;

    /**
     * Serializer used to (try to) output a null key, due to an entry of
     * {@link java.util.Map} having null key.
     */
    protected final ValueSerializer<Object> _nullKeySerializer;
    
    public SerializerFactoryConfig() {
        this(null, null, null,
                DEFAULT_NULL_KEY_SERIALIZER, null);
                
    }

    protected SerializerFactoryConfig(Serializers[] allAdditionalSerializers,
            Serializers[] allAdditionalKeySerializers,
            ValueSerializerModifier[] modifiers,
            ValueSerializer<Object> nullKeySer,
            ValueSerializer<Object> nullValueSer)
    {
        _additionalSerializers = (allAdditionalSerializers == null) ?
                NO_SERIALIZERS : allAdditionalSerializers;
        _additionalKeySerializers = (allAdditionalKeySerializers == null) ?
                NO_SERIALIZERS : allAdditionalKeySerializers;
        _modifiers = (modifiers == null) ? NO_MODIFIERS : modifiers;
        _nullKeySerializer = nullKeySer;
        _nullValueSerializer = nullValueSer;
    }

    public SerializerFactoryConfig withAdditionalSerializers(Serializers additional)
    {
        Objects.requireNonNull(additional, "Cannot pass null Serializers");
        Serializers[] all = ArrayBuilders.insertInListNoDup(_additionalSerializers, additional);
        return new SerializerFactoryConfig(all, _additionalKeySerializers, _modifiers,
                _nullKeySerializer, _nullValueSerializer);
    }

    public SerializerFactoryConfig withAdditionalKeySerializers(Serializers additional)
    {
        Objects.requireNonNull(additional, "Cannot pass null Serializers");
        Serializers[] all = ArrayBuilders.insertInListNoDup(_additionalKeySerializers, additional);
        return new SerializerFactoryConfig(_additionalSerializers, all, _modifiers,
                _nullKeySerializer, _nullValueSerializer);
    }

    public SerializerFactoryConfig withSerializerModifier(ValueSerializerModifier modifier)
    {
        Objects.requireNonNull(modifier, "Cannot pass null ValueSerializerModifier");
        ValueSerializerModifier[] modifiers = ArrayBuilders.insertInListNoDup(_modifiers, modifier);
        return new SerializerFactoryConfig(_additionalSerializers, _additionalKeySerializers, modifiers,
                _nullKeySerializer, _nullValueSerializer);
    }

    @SuppressWarnings("unchecked")
    public SerializerFactoryConfig withNullValueSerializer(ValueSerializer<?> nvs) {
        Objects.requireNonNull(nvs, "Cannot pass null ValueSerializer");
        return new SerializerFactoryConfig(_additionalSerializers, _additionalKeySerializers, _modifiers,
                _nullKeySerializer, (ValueSerializer<Object>) nvs);
    }

    @SuppressWarnings("unchecked")
    public SerializerFactoryConfig withNullKeySerializer(ValueSerializer<?> nks) {
        Objects.requireNonNull(nks, "Cannot pass null ValueSerializer");
        return new SerializerFactoryConfig(_additionalSerializers, _additionalKeySerializers, _modifiers,
                (ValueSerializer<Object>) nks, _nullValueSerializer);
    }

    public boolean hasSerializers() { return _additionalSerializers.length > 0; }
    public boolean hasKeySerializers() { return _additionalKeySerializers.length > 0; }
    public boolean hasSerializerModifiers() { return _modifiers.length > 0; }

    public Iterable<Serializers> serializers() { return new ArrayIterator<Serializers>(_additionalSerializers); }
    public Iterable<Serializers> keySerializers() { return new ArrayIterator<Serializers>(_additionalKeySerializers); }
    public Iterable<ValueSerializerModifier> serializerModifiers() { return new ArrayIterator<ValueSerializerModifier>(_modifiers); }

    public ValueSerializer<Object> getNullKeySerializer() { return _nullKeySerializer; }
    public ValueSerializer<Object> getNullValueSerializer() { return _nullValueSerializer; }
}
