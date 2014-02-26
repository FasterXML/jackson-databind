package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.ObjectBuffer;

/**
 * Separate implementation for serializing String arrays (instead of
 * using {@link ObjectArrayDeserializer}.
 * Used if (and only if) no custom value deserializers are used.
 */
@JacksonStdImpl
public final class StringArrayDeserializer
    extends StdDeserializer<String[]>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = -7589512013334920693L;

    public final static StringArrayDeserializer instance = new StringArrayDeserializer();
    
    /**
     * Value serializer to use, if not the standard one (which is inlined)
     */
    protected JsonDeserializer<String> _elementDeserializer;

    public StringArrayDeserializer() {
        super(String[].class);
        _elementDeserializer = null;
    }

    @SuppressWarnings("unchecked")
    protected StringArrayDeserializer(JsonDeserializer<?> deser) {
        super(String[].class);
        _elementDeserializer = (JsonDeserializer<String>) deser;
    }
   
    @Override
    public String[] deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (!jp.isExpectedStartArrayToken()) {
            return handleNonArray(jp, ctxt);
        }
        if (_elementDeserializer != null) {
            return _deserializeCustom(jp, ctxt);
        }

        final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] chunk = buffer.resetAndStart();
        
        int ix = 0;
        JsonToken t;
        
        while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
            // Ok: no need to convert Strings, but must recognize nulls
            String value;
            if (t == JsonToken.VALUE_STRING) {
                value = jp.getText();
            } else if (t == JsonToken.VALUE_NULL) {
                value = _elementDeserializer.getNullValue();
            } else {
                value = _parseString(jp, ctxt);
            }
            if (ix >= chunk.length) {
                chunk = buffer.appendCompletedChunk(chunk);
                ix = 0;
            }
            chunk[ix++] = value;
        }
        String[] result = buffer.completeAndClearBuffer(chunk, ix, String.class);
        ctxt.returnObjectBuffer(buffer);
        return result;
    }

    /**
     * Offlined version used when we do not use the default deserialization method.
     */
    protected final String[] _deserializeCustom(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] chunk = buffer.resetAndStart();
        final JsonDeserializer<String> deser = _elementDeserializer;
        
        int ix = 0;
        JsonToken t;
        
        while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
            // Ok: no need to convert Strings, but must recognize nulls
            String value = (t == JsonToken.VALUE_NULL) ? null : deser.deserialize(jp, ctxt);
            if (ix >= chunk.length) {
                chunk = buffer.appendCompletedChunk(chunk);
                ix = 0;
            }
            chunk[ix++] = value;
        }
        String[] result = buffer.completeAndClearBuffer(chunk, ix, String.class);
        ctxt.returnObjectBuffer(buffer);
        return result;
    }
    
    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        return typeDeserializer.deserializeTypedFromArray(jp, ctxt);
    }

    private final String[] handleNonArray(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        // [JACKSON-526]: implicit arrays from single values?
        if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
            // [JACKSON-620] Empty String can become null...
            if ((jp.getCurrentToken() == JsonToken.VALUE_STRING)
                    && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                String str = jp.getText();
                if (str.length() == 0) {
                    return null;
                }
            }
            throw ctxt.mappingException(_valueClass);
        }
        return new String[] { (jp.getCurrentToken() == JsonToken.VALUE_NULL) ? null : _parseString(jp, ctxt) };
    }

    /**
     * Contextualization is needed to see whether we can "inline" deserialization
     * of String values, or if we have to use separate value deserializer.
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException
    {
        JsonDeserializer<?> deser = _elementDeserializer;
        // #125: May have a content converter
        deser = findConvertingContentDeserializer(ctxt, property, deser);
        if (deser == null) {
            deser = ctxt.findContextualValueDeserializer(ctxt.constructType(String.class), property);
        } else { // if directly assigned, probably not yet contextual, so:
            deser = ctxt.handleSecondaryContextualization(deser, property);
        }
        // Ok ok: if all we got is the default String deserializer, can just forget about it
        if (deser != null && this.isDefaultDeserializer(deser)) {
            deser = null;
        }
        if (_elementDeserializer != deser) {
            return new StringArrayDeserializer(deser);
        }
        return this;
    }
}
