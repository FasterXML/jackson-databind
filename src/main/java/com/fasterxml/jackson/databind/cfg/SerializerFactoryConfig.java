package com.fasterxml.jackson.databind.cfg;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonSerializer;
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

    public final static JsonSerializer<Object> DEFAULT_NULL_KEY_SERIALIZER =
            new FailingSerializer("Null key for a Map not allowed in JSON (use a converting NullKeySerializer?)");

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

    /**
     * Serializer used to output a null value, unless explicitly redefined for property.
     */
    protected final JsonSerializer<Object> _nullValueSerializer;

    /**
     * Serializer used to (try to) output a null key, due to an entry of
     * {@link java.util.Map} having null key.
     */
    protected final JsonSerializer<Object> _nullKeySerializer;
    
    public SerializerFactoryConfig() {
        this(null, null, null,
                DEFAULT_NULL_KEY_SERIALIZER, null);
                
    }

    protected SerializerFactoryConfig(Serializers[] allAdditionalSerializers,
            Serializers[] allAdditionalKeySerializers,
            BeanSerializerModifier[] modifiers,
            JsonSerializer<Object> nullKeySer,
            JsonSerializer<Object> nullValueSer)
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

    public SerializerFactoryConfig withSerializerModifier(BeanSerializerModifier modifier)
    {
        Objects.requireNonNull(modifier, "Cannot pass null BeanSerializerModifier");
        BeanSerializerModifier[] modifiers = ArrayBuilders.insertInListNoDup(_modifiers, modifier);
        return new SerializerFactoryConfig(_additionalSerializers, _additionalKeySerializers, modifiers,
                _nullKeySerializer, _nullValueSerializer);
    }

    @SuppressWarnings("unchecked")
    public SerializerFactoryConfig withNullValueSerializer(JsonSerializer<?> nvs) {
        Objects.requireNonNull(nvs, "Cannot pass null JsonSerializer");
        return new SerializerFactoryConfig(_additionalSerializers, _additionalKeySerializers, _modifiers,
                _nullKeySerializer, (JsonSerializer<Object>) nvs);
    }

    @SuppressWarnings("unchecked")
    public SerializerFactoryConfig withNullKeySerializer(JsonSerializer<?> nks) {
        Objects.requireNonNull(nks, "Cannot pass null JsonSerializer");
        return new SerializerFactoryConfig(_additionalSerializers, _additionalKeySerializers, _modifiers,
                (JsonSerializer<Object>) nks, _nullValueSerializer);
    }

    public boolean hasSerializers() { return _additionalSerializers.length > 0; }
    public boolean hasKeySerializers() { return _additionalKeySerializers.length > 0; }
    public boolean hasSerializerModifiers() { return _modifiers.length > 0; }

    public Iterable<Serializers> serializers() { return new ArrayIterator<Serializers>(_additionalSerializers); }
    public Iterable<Serializers> keySerializers() { return new ArrayIterator<Serializers>(_additionalKeySerializers); }
    public Iterable<BeanSerializerModifier> serializerModifiers() { return new ArrayIterator<BeanSerializerModifier>(_modifiers); }

    public JsonSerializer<Object> getNullKeySerializer() { return _nullKeySerializer; }
    public JsonSerializer<Object> getNullValueSerializer() { return _nullValueSerializer; }
}
