package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;

/**
 * Serializer implementation where given Java type is first converted
 * to an intermediate "delegate type" (using a configured
 * {@link Converter}, and then this delegate value is serialized by Jackson.
 *<p>
 * Note that although types may be related, they must not be same; trying
 * to do this will result in an exception.
 * 
 * @since 2.1
 */
public class StdDelegatingSerializer
    extends StdSerializer<Object>
    implements ContextualSerializer,
        JsonFormatVisitable, SchemaAware
{
    protected final Converter<Object,?> _converter;
    
    /**
     * Fully resolved delegate type, with generic information if any available.
     */
    protected final JavaType _delegateType;
    
    /**
     * Underlying serializer for type <code>T<.code>.
     */
    protected final JsonSerializer<Object> _delegateSerializer;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public StdDelegatingSerializer(Converter<?,?> converter)
    {
        super(Object.class);
        _converter = (Converter<Object,?>)converter;
        _delegateType = null;
        _delegateSerializer = null;
    }

    @SuppressWarnings("unchecked")
    public <T> StdDelegatingSerializer(Class<T> cls, Converter<T,?> converter)
    {
        super(cls, false);
        _converter = (Converter<Object,?>)converter;
        _delegateType = null;
        _delegateSerializer = null;
    }
    
    @SuppressWarnings("unchecked")
    protected StdDelegatingSerializer(Converter<Object,?> converter,
            JavaType delegateType, JsonSerializer<?> delegateSerializer)
    {
        super(delegateType);
        _converter = converter;
        _delegateType = delegateType;
        _delegateSerializer = (JsonSerializer<Object>) delegateSerializer;
    }

    /**
     * Method used for creating resolved contextual instances. Must be
     * overridden when sub-classing.
     */
    protected StdDelegatingSerializer withDelegate(Converter<Object,?> converter,
            JavaType delegateType, JsonSerializer<?> delegateSerializer)
    {
        if (getClass() != StdDelegatingSerializer.class) {
            throw new IllegalStateException("Sub-class "+getClass().getName()+" must override 'withDelegate'");
        }
        return new StdDelegatingSerializer(converter, delegateType, delegateSerializer);
    }
    
    /*
    /**********************************************************
    /* Contextualization
    /**********************************************************
     */
    
    // @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
        throws JsonMappingException
    {
        // First: figure out what is the fully generic delegate type:
        TypeFactory tf = provider.getTypeFactory();
        JavaType implType = tf.constructType(_converter.getClass());
        JavaType[] params = tf.findTypeParameters(implType, Converter.class);
        if (params == null || params.length != 2) {
            throw new JsonMappingException("Could not determine Converter parameterization for "
                    +implType);
        }
        // and then we can find serializer to delegate to, construct a new instance:
        JavaType delegateType = params[1];
        return withDelegate(_converter, delegateType,
                provider.findValueSerializer(delegateType, property));
    }

    /*
    /**********************************************************
    /* Serialization
    /**********************************************************
     */

    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        Object delegateValue = _converter.convert(value);
        // should we accept nulls?
        if (delegateValue == null) {
            provider.defaultSerializeNull(jgen);
            return;
        }
        _delegateSerializer.serialize(delegateValue, jgen, provider);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        /* 03-Oct-2012, tatu: This is actually unlikely to work ok... but for now,
         *    let's give it a chance?
         */
        Object delegateValue = _converter.convert(value);
        _delegateSerializer.serializeWithType(delegateValue, jgen, provider, typeSer);
    }
    
    /*
    /**********************************************************
    /* Schema functionality
    /**********************************************************
     */

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        if (_delegateSerializer instanceof SchemaAware) {
            return ((SchemaAware) _delegateSerializer).getSchema(provider, typeHint);
        }
        return super.getSchema(provider, typeHint);
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint,
        boolean isOptional) throws JsonMappingException
    {
        if (_delegateSerializer instanceof SchemaAware) {
            return ((SchemaAware) _delegateSerializer).getSchema(provider, typeHint, isOptional);
        }
        return super.getSchema(provider, typeHint);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        if (_delegateSerializer instanceof JsonFormatVisitable) {
            ((JsonFormatVisitable) _delegateSerializer).acceptJsonFormatVisitor(visitor, typeHint);
            return;
        }
        super.acceptJsonFormatVisitor(visitor, typeHint);
    }

    /*
    /**********************************************************
    /* Other
    /**********************************************************
     */

    @Override
    public JsonSerializer<?> getDelegatee() {
        return _delegateSerializer;
    }
}
