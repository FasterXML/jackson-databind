package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

public class AtomicReferenceDeserializer
    extends StdDeserializer<AtomicReference<?>>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = 1L;

    /**
     * Type of value that we reference
     */
    protected final JavaType _referencedType;
    
    protected final TypeDeserializer _valueTypeDeserializer;

    protected final JsonDeserializer<?> _valueDeserializer;
    
    /**
     * @param referencedType Parameterization of this reference
     */
    public AtomicReferenceDeserializer(JavaType referencedType) {
        this(referencedType, null, null);
    }
    
    public AtomicReferenceDeserializer(JavaType referencedType,
            TypeDeserializer typeDeser, JsonDeserializer<?> deser)
    {
        super(AtomicReference.class);
        _referencedType = referencedType;
        _valueDeserializer = deser;
        _valueTypeDeserializer = typeDeser;
    }

    public AtomicReferenceDeserializer withResolved(TypeDeserializer typeDeser,
            JsonDeserializer<?> valueDeser)
    {
        return new AtomicReferenceDeserializer(_referencedType,
                typeDeser, valueDeser);
    }
    
    // Added in 2.3
    @Override
    public AtomicReference<?> getNullValue() {
        return new AtomicReference<Object>();
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        JsonDeserializer<?> deser = _valueDeserializer;
        TypeDeserializer typeDeser = _valueTypeDeserializer;
        if (deser == null) {
            deser = ctxt.findContextualValueDeserializer(_referencedType, property);
        }
        if (typeDeser != null) {
            typeDeser = typeDeser.forProperty(property);
        }
        if (deser == _valueDeserializer && typeDeser == _valueTypeDeserializer) {
            return this;
        }
        return withResolved(typeDeser, deser);
    }

    @Override
    public AtomicReference<?> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        return new AtomicReference<Object>(_valueDeserializer.deserialize(jp, ctxt));
    }
    
    @Override
    public AtomicReference<?> deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        final JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.VALUE_NULL) {
            return getNullValue();
        }
        /* 03-Nov-2013, tatu: This gets rather tricky with "natural" types
         *   (String, Integer, Boolean), which do NOT include type information.
         *   These might actually be handled ok except that nominal type here
         *   is `Optional`, so special handling is not invoked; instead, need
         *   to do a work-around here.
         */
        if (t != null && t.isScalarValue()) {
            return deserialize(jp, ctxt);
        }
        Object refd = _valueTypeDeserializer.deserializeTypedFromAny(jp, ctxt);
        return new AtomicReference<Object>(refd);
    }
}
