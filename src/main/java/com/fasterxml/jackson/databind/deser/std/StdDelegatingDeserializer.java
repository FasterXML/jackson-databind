package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.AccessPattern;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Deserializer implementation where given Java type is first deserialized
 * by a standard Jackson deserializer into a delegate type; and then
 * this delegate type is converted using a configured
 * {@link Converter} into desired target type.
 * Common delegate types to use are {@link java.util.Map}
 * and {@link com.fasterxml.jackson.databind.JsonNode}.
 *<p>
 * Note that although types (delegate, target) may be related, they must not be same; trying
 * to do this will result in an exception.
 *<p>
 * Since 2.5 There is {@link StdNodeBasedDeserializer} that is a simplified version
 * for cases where intermediate type is {@link JsonNode}
 *<p>
 * NOTE: in Jackson 3.0 this class will be renamed as {@code StdConvertingDeserializer}.
 *
 * @param <T> Target type to convert to, from delegate type
 *
 * @since 2.1
 *
 * @see StdNodeBasedDeserializer
 * @see Converter
 */
public class StdDelegatingDeserializer<T>
    extends StdDeserializer<T>
    implements ContextualDeserializer, ResolvableDeserializer
{
    private static final long serialVersionUID = 1L;

    /**
     * Converter that was used for creating {@link #_delegateDeserializer}.
     */
    protected final Converter<Object,T> _converter;

    /**
     * Fully resolved delegate type, with generic information if any available.
     */
    protected final JavaType _delegateType;

    /**
     * Underlying serializer for type <code>T</code>.
     */
    protected final JsonDeserializer<Object> _delegateDeserializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public StdDelegatingDeserializer(Converter<?,T> converter)
    {
        super(Object.class);
        _converter = (Converter<Object,T>)converter;
        _delegateType = null;
        _delegateDeserializer = null;
    }

    @SuppressWarnings("unchecked")
    public StdDelegatingDeserializer(Converter<Object,T> converter,
            JavaType delegateType, JsonDeserializer<?> delegateDeserializer)
    {
        super(delegateType);
        _converter = converter;
        _delegateType = delegateType;
        _delegateDeserializer = (JsonDeserializer<Object>) delegateDeserializer;
    }

    /**
     * @since 2.5
     */
    protected StdDelegatingDeserializer(StdDelegatingDeserializer<T> src)
    {
        super(src);
        _converter = src._converter;
        _delegateType = src._delegateType;
        _delegateDeserializer = src._delegateDeserializer;
    }

    /**
     * Method used for creating resolved contextual instances. Must be
     * overridden when sub-classing.
     */
    protected StdDelegatingDeserializer<T> withDelegate(Converter<Object,T> converter,
            JavaType delegateType, JsonDeserializer<?> delegateDeserializer)
    {
        ClassUtil.verifyMustOverride(StdDelegatingDeserializer.class, this, "withDelegate");
        return new StdDelegatingDeserializer<T>(converter, delegateType, delegateDeserializer);
    }

    @Override
    public JsonDeserializer<T> unwrappingDeserializer(NameTransformer unwrapper) {
        ClassUtil.verifyMustOverride(StdDelegatingDeserializer.class, this, "unwrappingDeserializer");
        return replaceDelegatee(_delegateDeserializer.unwrappingDeserializer(unwrapper));
    }

    @Override
    public JsonDeserializer<T> replaceDelegatee(JsonDeserializer<?> delegatee) {
        ClassUtil.verifyMustOverride(StdDelegatingDeserializer.class, this, "replaceDelegatee");
        if (delegatee == _delegateDeserializer) {
            return this;
        }
        return new StdDelegatingDeserializer<T>(_converter, _delegateType, delegatee);
    }

    /*
    /**********************************************************
    /* Contextualization
    /**********************************************************
     */

    // Note: unlikely to get called since most likely instances explicitly constructed;
    // if so, caller must ensure delegating deserializer is properly resolve()d.
    @Override
    public void resolve(DeserializationContext ctxt)
        throws JsonMappingException
    {
        if (_delegateDeserializer != null && _delegateDeserializer instanceof ResolvableDeserializer) {
            ((ResolvableDeserializer) _delegateDeserializer).resolve(ctxt);
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
        throws JsonMappingException
    {
        // First: if already got deserializer to delegate to, contextualize it:
        if (_delegateDeserializer != null) {
            JsonDeserializer<?> deser = ctxt.handleSecondaryContextualization(_delegateDeserializer,
                    property, _delegateType);
            if (deser != _delegateDeserializer) {
                return withDelegate(_converter, _delegateType, deser);
            }
            return this;
        }
        // Otherwise: figure out what is the fully generic delegate type, then find deserializer
        JavaType delegateType = _converter.getInputType(ctxt.getTypeFactory());
        return withDelegate(_converter, delegateType,
                ctxt.findContextualValueDeserializer(delegateType, property));
    }

    /*
    /**********************************************************
    /* Deserialization
    /**********************************************************
     */

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        Object delegateValue = _delegateDeserializer.deserialize(p, ctxt);
        if (delegateValue == null) {
            return null;
        }
        return convertValue(delegateValue);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException
    {
        /* 12-Apr-2016, tatu: As predicted, earlier handling does not work
         *   (see [databind#1189] for details). There does not seem to be any compelling
         *   way to combine polymorphic types, Converters, but the least sucky way
         *   is probably to use Converter and ignore polymorphic type. Alternative
         *   would be to try to change `TypeDeserializer` to accept `Converter` to
         *   invoke... but that is more intrusive, yet not guaranteeing success.
         */
        // method called up to 2.7.3:
//        Object delegateValue = _delegateDeserializer.deserializeWithType(p, ctxt, typeDeserializer);

        // method called since 2.7.4
        Object delegateValue = _delegateDeserializer.deserialize(p, ctxt);
        if (delegateValue == null) {
            return null;
        }
        return convertValue(delegateValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt, Object intoValue)
        throws IOException
    {
        if (_delegateType.getRawClass().isAssignableFrom(intoValue.getClass())){
            return (T) _delegateDeserializer.deserialize(p, ctxt, intoValue);
        }
        return (T) _handleIncompatibleUpdateValue(p, ctxt, intoValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer, T intoValue)
        throws IOException, JacksonException
    {
        // 21-Jan-2023, tatu: Override was missing from 2.15. Tricky to
        //    support but follow example of the other "deserializeWithType()"
        //   It seems unlikely to actually work (isn't type check just... wrong?)
        //   but for now has to do I guess.
        if (!_delegateType.getRawClass().isAssignableFrom(intoValue.getClass())){
            return (T) _handleIncompatibleUpdateValue(p, ctxt, intoValue);
        }
        return (T) _delegateDeserializer.deserialize(p, ctxt, intoValue);
    }

    /**
     * Overridable handler method called when {@link #deserialize(JsonParser, DeserializationContext, Object)}
     * has been called with a value that is not compatible with delegate value.
     * Since no conversion are expected for such "updateValue" case, this is normally not
     * an operation that can be permitted, and the default behavior is to throw exception.
     * Sub-classes may choose to try alternative approach if they have more information on
     * exact usage and constraints.
     *
     * @since 2.6
     */
    protected Object _handleIncompatibleUpdateValue(JsonParser p, DeserializationContext ctxt, Object intoValue)
        throws IOException
    {
        throw new UnsupportedOperationException(String.format
                ("Cannot update object of type %s (using deserializer for type %s)"
                        +intoValue.getClass().getName(), _delegateType));
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override
    public Class<?> handledType() {
        return _delegateDeserializer.handledType();
    }

    @Override // since 2.12
    public LogicalType logicalType() {
        return _delegateDeserializer.logicalType();
    }

    // Let's assume we should be cachable if delegate is
    @Override
    public boolean isCachable() {
        return (_delegateDeserializer != null) && _delegateDeserializer.isCachable();
    }

    @Override
    public JsonDeserializer<?> getDelegatee() {
        return _delegateDeserializer;
    }

    @Override
    public Collection<Object> getKnownPropertyNames() {
        return _delegateDeserializer.getKnownPropertyNames();
    }

    /*
    /**********************************************************
    /* Null/empty/absent accessors
    /**********************************************************
     */

    @Override
    public T getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        return _convertIfNonNull(_delegateDeserializer.getNullValue(ctxt));
    }

    @Override
    public AccessPattern getNullAccessPattern() {
        return _delegateDeserializer.getNullAccessPattern();
    }

    @Override
    public Object getAbsentValue(DeserializationContext ctxt) throws JsonMappingException {
        return _convertIfNonNull(_delegateDeserializer.getAbsentValue(ctxt));
    }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        return _convertIfNonNull(_delegateDeserializer.getEmptyValue(ctxt));
    }

    @Override
    public AccessPattern getEmptyAccessPattern() {
        return _delegateDeserializer.getEmptyAccessPattern();
    }

    /*
    /**********************************************************
    /* Other accessors
    /**********************************************************
     */

    @Override // since 2.9
    public Boolean supportsUpdate(DeserializationConfig config) {
        return _delegateDeserializer.supportsUpdate(config);
    }

    /*
    /**********************************************************
    /* Overridable methods
    /**********************************************************
     */

    /**
     * Method called to convert from "delegate value" (which was deserialized
     * from JSON using standard Jackson deserializer for delegate type)
     * into desired target type.
     *<P>
     * The default implementation uses configured {@link Converter} to do
     * conversion.
     *
     * @param delegateValue
     *
     * @return Result of conversion
     */
    protected T convertValue(Object delegateValue) {
        return _converter.convert(delegateValue);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    // @since 2.14.2
    protected T _convertIfNonNull(Object delegateValue) {
        return (delegateValue == null) ? null
                : _converter.convert(delegateValue);
    }
}
