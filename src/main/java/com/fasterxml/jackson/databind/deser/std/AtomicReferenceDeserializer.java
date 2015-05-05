package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.*;

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
    
    public AtomicReferenceDeserializer(JavaType referencedType, TypeDeserializer typeDeser, JsonDeserializer<?> deser)
    {
        super(AtomicReference.class);
        _referencedType = referencedType;
        _valueDeserializer = deser;
        _valueTypeDeserializer = typeDeser;
    }

    public AtomicReferenceDeserializer withResolved(TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
        return new AtomicReferenceDeserializer(_referencedType, typeDeser, valueDeser);
    }

    @Override
    public AtomicReference<?> getNullValue(DeserializationContext ctxt) {
        return new AtomicReference<Object>();
    }

    @Deprecated // remove in 2.7
    @Override
    public AtomicReference<?> getNullValue() {
        return new AtomicReference<Object>();
    }
    
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException
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
    public AtomicReference<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        /* 06-Nov-2013, tatu: Looks like the only way to make polymorphic deser to work
         *   correctly is to add support here; problem being that handler is not available
         *   for nominal type of AtomicReference but only "contained" type...
         */
        if (_valueTypeDeserializer != null) {
            return new AtomicReference<Object>(_valueDeserializer.deserializeWithType(jp, ctxt, _valueTypeDeserializer));
        }
        return new AtomicReference<Object>(_valueDeserializer.deserialize(jp, ctxt));
    }

    @Override
    public Object[] deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException {
        return (Object[]) typeDeserializer.deserializeTypedFromAny(jp, ctxt);
    }
}
