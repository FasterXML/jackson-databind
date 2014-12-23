package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.util.Converter;

import java.io.IOException;
import java.lang.reflect.Type;

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
@SuppressWarnings("serial")
public class StdDelegatingSerializer
    extends StdSerializer<Object>
    implements ContextualSerializer, ResolvableSerializer,
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
    public StdDelegatingSerializer(Converter<Object,?> converter,
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

    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException
    {
        if ((_delegateSerializer != null)
                && (_delegateSerializer instanceof ResolvableSerializer)) {
            ((ResolvableSerializer) _delegateSerializer).resolve(provider);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
        throws JsonMappingException
    {
        JsonSerializer<?> delSer = _delegateSerializer;
        JavaType delegateType = _delegateType;

        if (delSer == null) {
            // Otherwise, need to locate serializer to delegate to. For that we need type information...
            if (delegateType == null) {
                delegateType = _converter.getOutputType(provider.getTypeFactory());
            }
            delSer = provider.findValueSerializer(delegateType);
        }
        if (delSer instanceof ContextualSerializer) {
            delSer = provider.handleSecondaryContextualization(delSer, property);
        }
        return (delSer == _delegateSerializer) ? this
                : withDelegate(_converter, delegateType, delSer);
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    protected Converter<Object, ?> getConverter() {
        return _converter;
    }

    @Override
    public JsonSerializer<?> getDelegatee() {
        return _delegateSerializer;
    }
    
    /*
    /**********************************************************
    /* Serialization
    /**********************************************************
     */

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException
    {
        Object delegateValue = convertValue(value);
        // should we accept nulls?
        if (delegateValue == null) {
            provider.defaultSerializeNull(gen);
            return;
        }
        _delegateSerializer.serialize(delegateValue, gen, provider);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException
    {
        /* 03-Oct-2012, tatu: This is actually unlikely to work ok... but for now,
         *    let's give it a chance?
         */
        Object delegateValue = convertValue(value);
        _delegateSerializer.serializeWithType(delegateValue, gen, provider, typeSer);
    }

    @Override
    @Deprecated // since 1.5
    public boolean isEmpty(Object value)
    {
        Object delegateValue = convertValue(value);
        return _delegateSerializer.isEmpty(delegateValue);
    }

    @Override
    public boolean isEmpty(SerializerProvider prov, Object value)
    {
        Object delegateValue = convertValue(value);
        return _delegateSerializer.isEmpty(prov, delegateValue);
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
        throws JsonMappingException
    {
        /* 03-Sep-2012, tatu: Not sure if this can be made to really work
         *    properly... but for now, try this:
         */
        _delegateSerializer.acceptJsonFormatVisitor(visitor, typeHint);
    }

    /*
    /**********************************************************
    /* Overridable methods
    /**********************************************************
     */

    /**
     * Method called to convert from source Java value into delegate
     * value (which will be serialized using standard Jackson serializer for delegate type)
     *<P>
     * The default implementation uses configured {@link Converter} to do
     * conversion.
     * 
     * @param value Value to convert
     * 
     * @return Result of conversion
     */
    protected Object convertValue(Object value) {
        return _converter.convert(value);
    }
}
