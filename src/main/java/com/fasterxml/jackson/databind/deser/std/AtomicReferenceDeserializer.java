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
        if ((valueDeser == _valueDeserializer) && (typeDeser == _valueTypeDeserializer)) {
            return this;
        }
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
        } else { // otherwise directly assigned, probably not contextual yet:
            deser = ctxt.handleSecondaryContextualization(deser, property, _referencedType);
        }
        if (typeDeser != null) {
            typeDeser = typeDeser.forProperty(property);
        }
        return withResolved(typeDeser, deser);
    }

    @Override
    public AtomicReference<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        /* 06-Nov-2013, tatu: Looks like the only way to make polymorphic deser to work
         *   correctly is to add support here; problem being that handler is not available
         *   for nominal type of AtomicReference but only "contained" type...
         */
        if (_valueTypeDeserializer != null) {
            return new AtomicReference<Object>(_valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer));
        }
        return new AtomicReference<Object>(_valueDeserializer.deserialize(p, ctxt));
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException
    {
        final JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_NULL) { // can this actually happen?
            return getNullValue(ctxt);
        }
        // "Natural" types (String, Long, Boolean, Double) are tricky, so need this:
        if (t != null && t.isScalarValue()) {
            return deserialize(p, ctxt);
        }
        return typeDeserializer.deserializeTypedFromAny(p, ctxt);
    }
}
