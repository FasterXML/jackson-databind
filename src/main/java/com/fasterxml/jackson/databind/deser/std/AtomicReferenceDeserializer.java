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
     * Full type of property (or root value) for which this deserializer
     * has been constructed and contextualized.
     */
    protected final JavaType _fullType;

    protected final TypeDeserializer _valueTypeDeserializer;

    protected final JsonDeserializer<?> _valueDeserializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * @param fullType Type matching this deserializer, including outer
     *   <code>AtomicReference</code> (and not just referred type)
     */
    public AtomicReferenceDeserializer(JavaType fullType) {
        this(fullType, null, null);
    }

    public AtomicReferenceDeserializer(JavaType fullType,
            TypeDeserializer typeDeser, JsonDeserializer<?> deser)
    {
        super(AtomicReference.class);
        _fullType = fullType;
        _valueDeserializer = deser;
        _valueTypeDeserializer = typeDeser;
    }

    public AtomicReferenceDeserializer withResolved(TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
        if ((valueDeser == _valueDeserializer) && (typeDeser == _valueTypeDeserializer)) {
            return this;
        }
        return new AtomicReferenceDeserializer(_fullType, typeDeser, valueDeser);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException
    {
        JsonDeserializer<?> deser = _valueDeserializer;
        if (deser == null) {
            deser = ctxt.findContextualValueDeserializer(_fullType.getReferencedType(), property);
        } else { // otherwise directly assigned, probably not contextual yet:
            deser = ctxt.handleSecondaryContextualization(deser, property, _fullType.getReferencedType());            
        }
        TypeDeserializer typeDeser = _valueTypeDeserializer;
        if (typeDeser != null) {
            typeDeser = typeDeser.forProperty(property);
        }
        return withResolved(typeDeser, deser);
    }

    /*
    /**********************************************************
    /* Overridden accessors
    /**********************************************************
     */

    @Override
    public JavaType getValueType() { return _fullType; }
    
    @Override
    public AtomicReference<?> getNullValue(DeserializationContext ctxt) {
        return new AtomicReference<Object>();
    }

    /*
    /**********************************************************
    /* Deserialization
    /**********************************************************
     */
    
    @Override
    public AtomicReference<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Object contents = (_valueTypeDeserializer == null)
                ? _valueDeserializer.deserialize(p, ctxt)
                : _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        return new AtomicReference<Object>(contents);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException
    {
        final JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_NULL) { // can this actually happen?
            return getNullValue(ctxt);
        }
        // 22-Oct-2015, tatu: This handling is probably not needed (or is wrong), but
        //   could be result of older (pre-2.7) Jackson trying to serialize natural types.
        //  Because of this, let's allow for now, unless proven problematic
        if ((t != null) && t.isScalarValue()) {
            return deserialize(p, ctxt);
        }
        // andn this is what should really happen
        return typeDeserializer.deserializeTypedFromAny(p, ctxt);
    }
}
