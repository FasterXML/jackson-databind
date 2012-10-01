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

/**
 * Serializer implementation where given Java type is first converted
 * (by implemenetation sub-class provides) into an intermediate
 * "delegate type", and then serialized by Jackson.
 * Note that although types may be related, they must not be same; trying
 * to do this will result in an exception.
 *
 * @param <T> Java type being serialized and that is first converted into
 *   delegate type <code>DT</code>
 * @param <DT> Delegate type, intermediate into which sub-class converts
 *   Java type <code>T</code>, and that Jackson serializes using standard
 *   serializer of that type
 * 
 * @since 2.1
 */
public abstract class StdDelegatingSerializer<T,DT>
    extends StdSerializer<T>
    implements ContextualSerializer,
        JsonFormatVisitable, SchemaAware
{
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

    public StdDelegatingSerializer() {
        super(Object.class, false);
        _delegateType = null;
        _delegateSerializer = null;
    }

    public StdDelegatingSerializer(Class<T> cls) {
        super(cls);
        _delegateType = null;
        _delegateSerializer = null;
    }
    
    @SuppressWarnings("unchecked")
    protected StdDelegatingSerializer(JavaType delegateType,
            JsonSerializer<?> delegateSerializer)
    {
        super(delegateType);
        _delegateType = delegateType;
        _delegateSerializer = (JsonSerializer<Object>) delegateSerializer;
    }

    /**
     * Method that sub-classes have to implement for creating the actual
     * serializer instance, once all delegating information has been
     * collected: typically simply calls a constructor.
     */
    protected abstract JsonSerializer<?> withDelegate(JavaType delegateType,
            JsonSerializer<?> delegateSerializer);
    
    // @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
        throws JsonMappingException
    {
        // First: figure out what is the fully generic delegate type:
        TypeFactory tf = provider.getTypeFactory();
        JavaType implType = tf.constructType(getClass());
        JavaType[] params = tf.findTypeParameters(implType, StdDelegatingSerializer.class);
        if (params == null || params.length != 2) {
            throw new JsonMappingException("Could not determine StdDelegatingSerializer parameterization for "
                    +implType);
        }
        // and then we can find serializer to delegate to, construct a new instance:
        JavaType delegateType = params[1];
        return withDelegate(delegateType, provider.findValueSerializer(delegateType, property));
    }

    /*
    /**********************************************************
    /* Serialization
    /**********************************************************
     */

    /**
     * Method for sub-class to implement, used for converting given
     * value into delegate value, which is then serialized by standard
     * Jackson serializer.
     * 
     * @param value Property value tyo serializer
     * @param provider Contextual provider to use
     * 
     * @return Delegate value to serialize
     */
    public abstract DT convert(T value, SerializerProvider provider) 
        throws JsonMappingException;

    @Override
    public void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        DT delegateValue = convert(value, provider);
        _delegateSerializer.serialize(delegateValue, jgen, provider);
    }

    @Override
    public void serializeWithType(T value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        DT delegateValue = convert(value, provider);
        _delegateSerializer.serializeWithType(delegateValue, jgen, provider, typeSer);
    }
    
    /*
    /**********************************************************
    /* Schema functionality
    /**********************************************************
     */
    
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        if (_delegateSerializer instanceof SchemaAware) {
            return ((SchemaAware) _delegateSerializer).getSchema(provider, typeHint);
        }
        return super.getSchema(provider, typeHint);
    }

    public JsonNode getSchema(SerializerProvider provider, Type typeHint,
        boolean isOptional) throws JsonMappingException
    {
        if (_delegateSerializer instanceof SchemaAware) {
            return ((SchemaAware) _delegateSerializer).getSchema(provider, typeHint, isOptional);
        }
        return super.getSchema(provider, typeHint);
    }

    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        if (_delegateSerializer instanceof JsonFormatVisitable) {
            ((JsonFormatVisitable) _delegateSerializer).acceptJsonFormatVisitor(visitor, typeHint);
            return;
        }
        super.acceptJsonFormatVisitor(visitor, typeHint);
    }
}
