package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.util.ClassUtil;
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
        JsonFormatVisitable
{
    protected final Converter<Object,?> _converter;

    /**
     * Fully resolved delegate type, with generic information if any available.
     */
    protected final JavaType _delegateType;

    /**
     * Underlying serializer for type <code>T</code>.
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
        ClassUtil.verifyMustOverride(StdDelegatingSerializer.class, this, "withDelegate");
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
            // 02-Apr-2015, tatu: For "dynamic case", where type is only specified as
            //    java.lang.Object (or missing generic), [databind#731]
            if (!delegateType.isJavaLangObject()) {
                delSer = provider.findValueSerializer(delegateType);
            }
        }
        if (delSer instanceof ContextualSerializer) {
            delSer = provider.handleSecondaryContextualization(delSer, property);
        }
        if (delSer == _delegateSerializer && delegateType == _delegateType) {
            return this;
        }
        return withDelegate(_converter, delegateType, delSer);
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
        // 02-Apr-2015, tatu: As per [databind#731] may need to do dynamic lookup
        JsonSerializer<Object> ser = _delegateSerializer;
        if (ser == null) {
            ser = _findSerializer(delegateValue, provider);
        }
        ser.serialize(delegateValue, gen, provider);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException
    {
        // 03-Oct-2012, tatu: This is actually unlikely to work ok... but for now,
        //    let's give it a chance?
        Object delegateValue = convertValue(value);
        JsonSerializer<Object> ser = _delegateSerializer;
        if (ser == null) {
            ser = _findSerializer(value, provider);
        }
        ser.serializeWithType(delegateValue, gen, provider, typeSer);
    }

    @Override
    public boolean isEmpty(SerializerProvider prov, Object value)
    {
        Object delegateValue = convertValue(value);
        if (delegateValue == null) {
            return true;
        }
        if (_delegateSerializer == null) { // best we can do for now, too costly to look up
            return (value == null);
        }
        return _delegateSerializer.isEmpty(prov, delegateValue);
    }

    /*
    /**********************************************************
    /* Schema functionality
    /**********************************************************
     */

    /**
     * @deprecated Since 2.15
     */
    @Deprecated
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        if (_delegateSerializer instanceof com.fasterxml.jackson.databind.jsonschema.SchemaAware) {
            return ((com.fasterxml.jackson.databind.jsonschema.SchemaAware) _delegateSerializer)
                .getSchema(provider, typeHint);
        }
        return super.getSchema(provider, typeHint);
    }

    /**
     * @deprecated Since 2.15
     */
    @Deprecated
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint,
        boolean isOptional) throws JsonMappingException
    {
        if (_delegateSerializer instanceof com.fasterxml.jackson.databind.jsonschema.SchemaAware) {
            return ((com.fasterxml.jackson.databind.jsonschema.SchemaAware) _delegateSerializer)
                .getSchema(provider, typeHint, isOptional);
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
        // 02-Apr-2015, tatu: For dynamic case, very little we can do
        if (_delegateSerializer != null) {
            _delegateSerializer.acceptJsonFormatVisitor(visitor, typeHint);
        }
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

    /**
     * Helper method used for locating serializer to use in dynamic use case, where
     * actual type value gets converted to is not specified beyond basic
     * {@link java.lang.Object}, and where serializer needs to be located dynamically
     * based on actual value type.
     *
     * @since 2.6
     */
    protected JsonSerializer<Object> _findSerializer(Object value, SerializerProvider serializers)
        throws JsonMappingException
    {
        // NOTE: will NOT call contextualization
        return serializers.findValueSerializer(value.getClass());
    }
}
