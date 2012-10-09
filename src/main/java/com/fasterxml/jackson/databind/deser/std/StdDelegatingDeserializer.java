package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
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
 * 
 * @param <T> Target type to convert to, from delegate type
 * 
 * @since 2.1
 */
public class StdDelegatingDeserializer<T>
    extends StdDeserializer<T>
    implements ContextualDeserializer
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
    protected StdDelegatingDeserializer(Converter<Object,T> converter,
            JavaType delegateType, JsonDeserializer<?> delegateDeserializer)
    {
        super(delegateType);
        _converter = converter;
        _delegateType = delegateType;
        _delegateDeserializer = (JsonDeserializer<Object>) delegateDeserializer;
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
    

    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
        throws JsonMappingException
    {
        // First: figure out what is the fully generic delegate type:
        TypeFactory tf = ctxt.getTypeFactory();
        JavaType implType = tf.constructType(_converter.getClass());
        JavaType[] params = tf.findTypeParameters(implType, Converter.class);
        if (params == null || params.length != 2) {
            throw new JsonMappingException("Could not determine Converter parameterization for "
                    +implType);
        }
        // and then we can find serializer to delegate to, construct a new instance:
        JavaType delegateType = params[0];
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
     * @return
     */
    protected T convertValue(Object delegateValue) {
        return _converter.convert(delegateValue);
    }
}
