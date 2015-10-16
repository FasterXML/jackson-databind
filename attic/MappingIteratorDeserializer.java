package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

public class MappingIteratorDeserializer
    extends StdDeserializer<MappingIterator<Object>>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = 1L;

    protected final JavaType _valueType;

    protected final JsonDeserializer<Object> _valueDeserializer;
    
    public MappingIteratorDeserializer(JavaType valueType) {
        this(valueType, null);
    }

    protected MappingIteratorDeserializer(JavaType valueType, JsonDeserializer<Object> vdeser) {
        super(MappingIterator.class);
        _valueType = valueType;
        _valueDeserializer = vdeser;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty prop) throws JsonMappingException
    {
        JsonDeserializer<Object> deser = ctxt.findContextualValueDeserializer(_valueType, prop);
        return (deser == _valueDeserializer) ? this
                : new MappingIteratorDeserializer(_valueType, deser);
    }

    @Override
    public MappingIterator<Object> deserialize(JsonParser p,
            DeserializationContext ctxt) throws IOException,
            JsonProcessingException
    {
        MappingIterator<Object> mit = new MappingIterator<Object>(_valueType, p, ctxt,
                _valueDeserializer, false, null);
        return mit;
    }
}
