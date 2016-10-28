package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ReferenceType;

/**
 * Base deserializer implementation for properties {@link ReferenceType} values.
 * Implements most of functionality, only leaving couple of abstract
 * methods for sub-classes to implement
 *
 * @since 2.8
 */
public abstract class ReferenceTypeDeserializer<T>
    extends StdDeserializer<T>
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

    public ReferenceTypeDeserializer(JavaType fullType,
            TypeDeserializer typeDeser, JsonDeserializer<?> deser)
    {
        super(fullType);
        _fullType = fullType;
        _valueDeserializer = deser;
        _valueTypeDeserializer = typeDeser;
    }

    // NOTE: for forwards-compatibility; added in 2.8.5 since 2.9.0 has it
    public ReferenceTypeDeserializer(JavaType fullType, ValueInstantiator inst,
            TypeDeserializer typeDeser, JsonDeserializer<?> deser) {
        this(fullType, typeDeser, deser);
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
        if ((deser == _valueDeserializer) && (typeDeser == _valueTypeDeserializer)) {
            return this;
        }
        return withResolved(typeDeser, deser);
    }

    /*
    /**********************************************************
    /* Abstract methods for sub-classes to implement
    /**********************************************************
     */

    protected abstract ReferenceTypeDeserializer<T> withResolved(TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser);

    @Override
    public abstract T getNullValue(DeserializationContext ctxt);

    public abstract T referenceValue(Object contents);
    
    /*
    /**********************************************************
    /* Overridden accessors
    /**********************************************************
     */

    @Override
    public JavaType getValueType() { return _fullType; }

    /*
    /**********************************************************
    /* Deserialization
    /**********************************************************
     */
    
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Object contents = (_valueTypeDeserializer == null)
                ? _valueDeserializer.deserialize(p, ctxt)
                : _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        return referenceValue(contents);
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
        /*
        if ((t != null) && t.isScalarValue()) {
            return deserialize(p, ctxt);
        }
        */
        // 19-Apr-2016, tatu: Alas, due to there typically really being anything for Reference wrapper
        //   itself, need to just ignore `typeDeser`, use TypeDeserializer we do have for contents
        //   and it might just work.

        if (_valueTypeDeserializer == null) {
            return deserialize(p, ctxt);
        }
        return referenceValue(_valueTypeDeserializer.deserializeTypedFromAny(p, ctxt));
    }
}
