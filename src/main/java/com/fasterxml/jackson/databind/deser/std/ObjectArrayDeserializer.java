package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.Array;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.util.ObjectBuffer;

/**
 * Basic serializer that can serialize non-primitive arrays.
 */
@JacksonStdImpl
public class ObjectArrayDeserializer
    extends ContainerDeserializerBase<Object[]>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = 1L;

    // // Configuration

    /**
     * Full generic type of the array being deserialized
     */
    protected final ArrayType _arrayType;
    
    /**
     * Flag that indicates whether the component type is Object or not.
     * Used for minor optimization when constructing result.
     */
    protected final boolean _untyped;

    /**
     * Type of contained elements: needed for constructing actual
     * result array
     */
    protected final Class<?> _elementClass;

    /**
     * Element deserializer
     */
    protected JsonDeserializer<Object> _elementDeserializer;

    /**
     * If element instances have polymorphic type information, this
     * is the type deserializer that can handle it
     */
    protected final TypeDeserializer _elementTypeDeserializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public ObjectArrayDeserializer(ArrayType arrayType,
            JsonDeserializer<Object> elemDeser, TypeDeserializer elemTypeDeser)
    {
        super(arrayType);
        _arrayType = arrayType;
        _elementClass = arrayType.getContentType().getRawClass();
        _untyped = (_elementClass == Object.class);
        _elementDeserializer = elemDeser;
        _elementTypeDeserializer = elemTypeDeser;
    }

    /**
     * Overridable fluent-factory method used to create contextual instances
     */
    @SuppressWarnings("unchecked")
    public ObjectArrayDeserializer withDeserializer(TypeDeserializer elemTypeDeser,
            JsonDeserializer<?> elemDeser)
    {
        if ((elemDeser == _elementDeserializer) && (elemTypeDeser == _elementTypeDeserializer)) {
            return this;
        }
        return new ObjectArrayDeserializer(_arrayType,
                (JsonDeserializer<Object>) elemDeser, elemTypeDeser);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        JsonDeserializer<?> deser = _elementDeserializer;
        // #125: May have a content converter
        deser = findConvertingContentDeserializer(ctxt, property, deser);
        final JavaType vt = _arrayType.getContentType();
        if (deser == null) {
            deser = ctxt.findContextualValueDeserializer(vt, property);
        } else { // if directly assigned, probably not yet contextual, so:
            deser = ctxt.handleSecondaryContextualization(deser, property, vt);
        }
        TypeDeserializer elemTypeDeser = _elementTypeDeserializer;
        if (elemTypeDeser != null) {
            elemTypeDeser = elemTypeDeser.forProperty(property);
        }
        return withDeserializer(elemTypeDeser, deser);
    }

    @Override // since 2.5
    public boolean isCachable() {
        // Important: do NOT cache if polymorphic values, or ones with custom deserializer
        return (_elementDeserializer == null) && (_elementTypeDeserializer == null);
    }
    
    /*
    /**********************************************************
    /* ContainerDeserializerBase API
    /**********************************************************
     */

    @Override
    public JavaType getContentType() {
        return _arrayType.getContentType();
    }

    @Override
    public JsonDeserializer<Object> getContentDeserializer() {
        return _elementDeserializer;
    }
    
    /*
    /**********************************************************
    /* JsonDeserializer API
    /**********************************************************
     */
    
    @Override
    public Object[] deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (!jp.isExpectedStartArrayToken()) {
            return handleNonArray(jp, ctxt);
        }

        final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] chunk = buffer.resetAndStart();
        int ix = 0;
        JsonToken t;
        final TypeDeserializer typeDeser = _elementTypeDeserializer;

        try {
            while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
                // Note: must handle null explicitly here; value deserializers won't
                Object value;
                
                if (t == JsonToken.VALUE_NULL) {
                    value = _elementDeserializer.getNullValue(ctxt);
                } else if (typeDeser == null) {
                    value = _elementDeserializer.deserialize(jp, ctxt);
                } else {
                    value = _elementDeserializer.deserializeWithType(jp, ctxt, typeDeser);
                }
                if (ix >= chunk.length) {
                    chunk = buffer.appendCompletedChunk(chunk);
                    ix = 0;
                }
                chunk[ix++] = value;
            }
        } catch (Exception e) {
            throw JsonMappingException.wrapWithPath(e, chunk, buffer.bufferedSize() + ix);
        }

        Object[] result;

        if (_untyped) {
            result = buffer.completeAndClearBuffer(chunk, ix);
        } else {
            result = buffer.completeAndClearBuffer(chunk, ix, _elementClass);
        }
        ctxt.returnObjectBuffer(buffer);
        return result;
    }

    @Override
    public Object[] deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        /* Should there be separate handling for base64 stuff?
         * for now this should be enough:
         */
        return (Object[]) typeDeserializer.deserializeTypedFromArray(jp, ctxt);
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    protected Byte[] deserializeFromBase64(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // First same as what PrimitiveArrayDeserializers.ByteDeser does:
        byte[] b = jp.getBinaryValue(ctxt.getBase64Variant());
        // But then need to convert to wrappers
        Byte[] result = new Byte[b.length];
        for (int i = 0, len = b.length; i < len; ++i) {
            result[i] = Byte.valueOf(b[i]);
        }
        return result;
    }

    private final Object[] handleNonArray(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // [JACKSON-620] Empty String can become null...
        if ((jp.getCurrentToken() == JsonToken.VALUE_STRING)
                && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
            String str = jp.getText();
            if (str.length() == 0) {
                return null;
            }
        }
        
        // Can we do implicit coercion to a single-element array still?
        if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
            /* 04-Oct-2009, tatu: One exception; byte arrays are generally
             *   serialized as base64, so that should be handled
             */
            if (jp.getCurrentToken() == JsonToken.VALUE_STRING
                && _elementClass == Byte.class) {
                return deserializeFromBase64(jp, ctxt);
            }
            throw ctxt.mappingException(_arrayType.getRawClass());
        }
        JsonToken t = jp.getCurrentToken();
        Object value;
        
        if (t == JsonToken.VALUE_NULL) {
            value = _elementDeserializer.getNullValue(ctxt);
        } else if (_elementTypeDeserializer == null) {
            value = _elementDeserializer.deserialize(jp, ctxt);
        } else {
            value = _elementDeserializer.deserializeWithType(jp, ctxt, _elementTypeDeserializer);
        }
        // Ok: bit tricky, since we may want T[], not just Object[]
        Object[] result;

        if (_untyped) {
            result = new Object[1];
        } else {
            result = (Object[]) Array.newInstance(_elementClass, 1);
        }
        result[0] = value;
        return result;
    }
}

