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

    public AtomicReferenceDeserializer(JavaType referencedType,
            TypeDeserializer typeDeser, JsonDeserializer<?> deser)
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
        Object contents = (_valueTypeDeserializer == null)
                ? _valueDeserializer.deserialize(p, ctxt)
                : _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        return new AtomicReference<Object>(contents);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeser) throws IOException
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
        // 19-Apr-2016, tatu: Alas, due to there not really being anything for AtomicReference
        //   itself, need to just ignore `typeDeser`, use TypeDeserializer we do have for contents
        //   and it might just work.

        if (_valueTypeDeserializer == null) {
            return deserialize(p, ctxt);
        }
        return new AtomicReference<Object>(_valueTypeDeserializer.deserializeTypedFromAny(p, ctxt));
    }
}
