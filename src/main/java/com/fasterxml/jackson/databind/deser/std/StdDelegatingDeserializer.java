package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.Converter;

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

    protected final Converter<Object,T> _converter;

    /**
     * Fully resolved delegate type, with generic information if any available.
     */
    protected final JavaType _delegateType;

    /**
     * Underlying serializer for type <code>T<.code>.
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
        if (getClass() != StdDelegatingDeserializer.class) {
            throw new IllegalStateException("Sub-class "+getClass().getName()+" must override 'withDelegate'");
        }
        return new StdDelegatingDeserializer<T>(converter, delegateType, delegateDeserializer);
    }

    /*
    /**********************************************************
    /* Contextualization
    /**********************************************************
     */

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
        // First: if already got serializer to delegate to, contextualize it:
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
    /* Accessors
    /**********************************************************
     */

    @Override
    public JsonDeserializer<?> getDelegatee() {
        return _delegateDeserializer;
    }

    @Override
    public Class<?> handledType() {
        return _delegateDeserializer.handledType();
    }

    /*
    /**********************************************************
    /* Serialization
    /**********************************************************
     */
    
    @Override
    public T deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        Object delegateValue = _delegateDeserializer.deserialize(jp, ctxt);
        if (delegateValue == null) {
            return null;
        }
        return convertValue(delegateValue);
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        /* 03-Oct-2012, tatu: This is actually unlikely to work ok... but for now,
         *    let's give it a chance?
         */
        Object delegateValue = _delegateDeserializer.deserializeWithType(jp, ctxt,
                typeDeserializer);
        if (delegateValue == null) {
            return null;
        }
        return convertValue(delegateValue);
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
}
