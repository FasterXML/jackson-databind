package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private static final long serialVersionUID = 2L;

    public final static StringArrayDeserializer instance = new StringArrayDeserializer();

    /**
     * Value serializer to use, if not the standard one (which is inlined)
     */
    protected JsonDeserializer<String> _elementDeserializer;

    /**
     * Specific override for this instance (from proper, or global per-type overrides)
     * to indicate whether single value may be taken to mean an unwrapped one-element array
     * or not. If null, left to global defaults.
     *
     * @since 2.7
     */
    protected final Boolean _unwrapSingle;

    public StringArrayDeserializer() {
        this(null, null);
    }

    @SuppressWarnings("unchecked")
    protected StringArrayDeserializer(JsonDeserializer<?> deser, Boolean unwrapSingle) {
        super(String[].class);
        _elementDeserializer = (JsonDeserializer<String>) deser;
        _unwrapSingle = unwrapSingle;
    }

    /**
     * Contextualization is needed to see whether we can "inline" deserialization
     * of String values, or if we have to use separate value deserializer.
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException
    {
        JsonDeserializer<?> deser = _elementDeserializer;
        // May have a content converter
        deser = findConvertingContentDeserializer(ctxt, property, deser);
        JavaType type = ctxt.constructType(String.class);
        if (deser == null) {
            deser = ctxt.findContextualValueDeserializer(type, property);
        } else { // if directly assigned, probably not yet contextual, so:
            deser = ctxt.handleSecondaryContextualization(deser, property, type);
        }
        // One more thing: allow unwrapping?
        Boolean unwrapSingle = findFormatFeature(ctxt, property, String[].class,
                JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        // Ok ok: if all we got is the default String deserializer, can just forget about it
        if ((deser != null) && isDefaultDeserializer(deser)) {
            deser = null;
        }
        if ((_elementDeserializer == deser) && (_unwrapSingle == unwrapSingle)) {
            return this;
        }
        return new StringArrayDeserializer(deser, unwrapSingle);
    }

    @Override
    public String[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (!p.isExpectedStartArrayToken()) {
            return handleNonArray(p, ctxt);
        }
        if (_elementDeserializer != null) {
            return _deserializeCustom(p, ctxt);
        }

        final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] chunk = buffer.resetAndStart();

        int ix = 0;

        try {
            while (true) {
                String value = p.nextTextValue();
                if (value == null) {
                    JsonToken t = p.getCurrentToken();
                    if (t == JsonToken.END_ARRAY) {
                        break;
                    }
                    if (t != JsonToken.VALUE_NULL) {
                        value = _parseString(p, ctxt);
                    }
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
        String[] result = buffer.completeAndClearBuffer(chunk, ix, String.class);
        ctxt.returnObjectBuffer(buffer);
        return result;
    }

    /**
     * Offlined version used when we do not use the default deserialization method.
     */
    protected final String[] _deserializeCustom(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] chunk = buffer.resetAndStart();
        final JsonDeserializer<String> deser = _elementDeserializer;
        
        int ix = 0;

        try {
            while (true) {
                /* 30-Dec-2014, tatu: This may look odd, but let's actually call method
                 *   that suggest we are expecting a String; this helps with some formats,
                 *   notably XML. Note, however, that while we can get String, we can't
                 *   assume that's what we use due to custom deserializer
                 */
                String value;
                if (p.nextTextValue() == null) {
                    JsonToken t = p.getCurrentToken();
                    if (t == JsonToken.END_ARRAY) {
                        break;
                    }
                    // Ok: no need to convert Strings, but must recognize nulls
                    value = (t == JsonToken.VALUE_NULL) ? deser.getNullValue(ctxt) : deser.deserialize(p, ctxt);
                } else {
                    value = deser.deserialize(p, ctxt);
                }
                if (ix >= chunk.length) {
                    chunk = buffer.appendCompletedChunk(chunk);
                    ix = 0;
                }
                chunk[ix++] = value;
            }
        } catch (Exception e) {
            // note: pass String.class, not String[].class, as we need element type for error info
            throw JsonMappingException.wrapWithPath(e, String.class, ix);
        }
        String[] result = buffer.completeAndClearBuffer(chunk, ix, String.class);
        ctxt.returnObjectBuffer(buffer);
        return result;
    }
    
    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        return typeDeserializer.deserializeTypedFromArray(p, ctxt);
    }

    private final String[] handleNonArray(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // implicit arrays from single values?
        boolean canWrap = (_unwrapSingle == Boolean.TRUE) ||
                ((_unwrapSingle == null) &&
                        ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
        if (canWrap) {
            return new String[] { p.hasToken(JsonToken.VALUE_NULL) ? null : _parseString(p, ctxt) };
        } else if (p.hasToken(JsonToken.VALUE_STRING)
                    && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
            String str = p.getText();
            if (str.length() == 0) {
                return null;
            }
        }
        throw ctxt.mappingException(_valueClass);
    }
}
