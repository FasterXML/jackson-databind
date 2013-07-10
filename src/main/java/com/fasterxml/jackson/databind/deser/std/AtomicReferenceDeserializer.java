package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

public class AtomicReferenceDeserializer
    extends StdScalarDeserializer<AtomicReference<?>>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = 1L;

    /**
     * Type of value that we reference
     */
    protected final JavaType _referencedType;
    
    protected final JsonDeserializer<?> _valueDeserializer;
    
    /**
     * @param referencedType Parameterization of this reference
     */
    public AtomicReferenceDeserializer(JavaType referencedType) {
        this(referencedType, null);
    }
    
    public AtomicReferenceDeserializer(JavaType referencedType,
            JsonDeserializer<?> deser)
    {
        super(AtomicReference.class);
        _referencedType = referencedType;
        _valueDeserializer = deser;
    }
    
    @Override
    public AtomicReference<?> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        return new AtomicReference<Object>(_valueDeserializer.deserialize(jp, ctxt));
    }
    
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        JsonDeserializer<?> deser = _valueDeserializer;
        if (deser != null) {
            return this;
        }
        return new AtomicReferenceDeserializer(_referencedType,
                ctxt.findContextualValueDeserializer(_referencedType, property));
    }
}