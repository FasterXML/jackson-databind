package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.Array;

import com.fasterxml.jackson.annotation.JsonFormat;

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

    /**
     * Specific override for this instance (from proper, or global per-type overrides)
     * to indicate whether single value may be taken to mean an unwrapped one-element array
     * or not. If null, left to global defaults.
     *
     * @since 2.7
     */
    protected final Boolean _unwrapSingle;

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
        _unwrapSingle = null;
    }

    protected ObjectArrayDeserializer(ObjectArrayDeserializer base,
            JsonDeserializer<Object> elemDeser, TypeDeserializer elemTypeDeser,
            Boolean unwrapSingle)
    {
        super(base._arrayType);
        _arrayType = base._arrayType;
        _elementClass = base._elementClass;
        _untyped = base._untyped;

        _elementDeserializer = elemDeser;
        _elementTypeDeserializer = elemTypeDeser;
        _unwrapSingle = unwrapSingle;
    }
    
    /**
     * Overridable fluent-factory method used to create contextual instances
     */
    public ObjectArrayDeserializer withDeserializer(TypeDeserializer elemTypeDeser,
            JsonDeserializer<?> elemDeser)
    {
        return withResolved(elemTypeDeser, elemDeser, _unwrapSingle);
    }

    /**
     * @since 2.7
     */
    @SuppressWarnings("unchecked")
    public ObjectArrayDeserializer withResolved(TypeDeserializer elemTypeDeser,
            JsonDeserializer<?> elemDeser, Boolean unwrapSingle)
    {
        if ((unwrapSingle == _unwrapSingle)
                && (elemDeser == _elementDeserializer)
                && (elemTypeDeser == _elementTypeDeserializer)) {
            return this;
        }
        return new ObjectArrayDeserializer(this,
                (JsonDeserializer<Object>) elemDeser, elemTypeDeser, unwrapSingle);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        JsonDeserializer<?> deser = _elementDeserializer;
        Boolean unwrapSingle = findFormatFeature(ctxt, property, _arrayType.getRawClass(),
                JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        // May have a content converter
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
        return withResolved(elemTypeDeser, deser, unwrapSingle);
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
    public Object[] deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (!p.isExpectedStartArrayToken()) {
            return handleNonArray(p, ctxt);
        }

        final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] chunk = buffer.resetAndStart();
        int ix = 0;
        JsonToken t;
        final TypeDeserializer typeDeser = _elementTypeDeserializer;

        try {
            while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
                // Note: must handle null explicitly here; value deserializers won't
                Object value;
                
                if (t == JsonToken.VALUE_NULL) {
                    value = _elementDeserializer.getNullValue(ctxt);
                } else if (typeDeser == null) {
                    value = _elementDeserializer.deserialize(p, ctxt);
                } else {
                    value = _elementDeserializer.deserializeWithType(p, ctxt, typeDeser);
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
    public Object[] deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException
    {
        /* Should there be separate handling for base64 stuff?
         * for now this should be enough:
         */
        return (Object[]) typeDeserializer.deserializeTypedFromArray(p, ctxt);
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    protected Byte[] deserializeFromBase64(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // First same as what PrimitiveArrayDeserializers.ByteDeser does:
        byte[] b = p.getBinaryValue(ctxt.getBase64Variant());
        // But then need to convert to wrappers
        Byte[] result = new Byte[b.length];
        for (int i = 0, len = b.length; i < len; ++i) {
            result[i] = Byte.valueOf(b[i]);
        }
        return result;
    }

    protected Object[] handleNonArray(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // Empty String can become null...
        if (p.hasToken(JsonToken.VALUE_STRING)
                && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
            String str = p.getText();
            if (str.length() == 0) {
                return null;
            }
        }
        
        // Can we do implicit coercion to a single-element array still?
        boolean canWrap = (_unwrapSingle == Boolean.TRUE) ||
                ((_unwrapSingle == null) &&
                        ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
        if (!canWrap) {
            // One exception; byte arrays are generally serialized as base64, so that should be handled
            JsonToken t = p.getCurrentToken();
            if (t == JsonToken.VALUE_STRING
                    // note: not `byte[]`, but `Byte[]` -- former is primitive array
                    && _elementClass == Byte.class) {
                return deserializeFromBase64(p, ctxt);
            }
            return (Object[]) ctxt.handleUnexpectedToken(_arrayType.getRawClass(), p);
        }
        JsonToken t = p.getCurrentToken();
        Object value;
        
        if (t == JsonToken.VALUE_NULL) {
            value = _elementDeserializer.getNullValue(ctxt);
        } else if (_elementTypeDeserializer == null) {
            value = _elementDeserializer.deserialize(p, ctxt);
        } else {
            value = _elementDeserializer.deserializeWithType(p, ctxt, _elementTypeDeserializer);
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

