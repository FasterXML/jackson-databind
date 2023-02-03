package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.impl.NullsConstantProvider;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.AccessPattern;
import com.fasterxml.jackson.databind.util.ObjectBuffer;

/**
 * Separate implementation for serializing String arrays (instead of
 * using {@link ObjectArrayDeserializer}.
 * Used if (and only if) no custom value deserializers are used.
 */
@JacksonStdImpl
public final class StringArrayDeserializer
// 28-Oct-2016, tatu: Should do this:
//    extends ContainerDeserializerBase<String[]>
// but for now won't:
    extends StdDeserializer<String[]>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = 2L;

    private final static String[] NO_STRINGS = new String[0];

    public final static StringArrayDeserializer instance = new StringArrayDeserializer();

    /**
     * Value serializer to use, if not the standard one (which is inlined)
     */
    protected JsonDeserializer<String> _elementDeserializer;

    /**
     * Handler we need for dealing with null values as elements
     *
     * @since 2.9
     */
    protected final NullValueProvider _nullProvider;

    /**
     * Specific override for this instance (from proper, or global per-type overrides)
     * to indicate whether single value may be taken to mean an unwrapped one-element array
     * or not. If null, left to global defaults.
     *
     * @since 2.7
     */
    protected final Boolean _unwrapSingle;

    /**
     * Marker flag set if the <code>_nullProvider</code> indicates that all null
     * content values should be skipped (instead of being possibly converted).
     *
     * @since 2.9
     */
    protected final boolean _skipNullValues;

    public StringArrayDeserializer() {
        this(null, null, null);
    }

    @SuppressWarnings("unchecked")
    protected StringArrayDeserializer(JsonDeserializer<?> deser,
            NullValueProvider nuller, Boolean unwrapSingle) {
        super(String[].class);
        _elementDeserializer = (JsonDeserializer<String>) deser;
        _nullProvider = nuller;
        _unwrapSingle = unwrapSingle;
        _skipNullValues = NullsConstantProvider.isSkipper(nuller);
    }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Array;
    }

    @Override // since 2.9
    public Boolean supportsUpdate(DeserializationConfig config) {
        return Boolean.TRUE;
    }

    @Override // since 2.9
    public AccessPattern getEmptyAccessPattern() {
        // immutable, shareable so:
        return AccessPattern.CONSTANT;
    }

    @Override // since 2.9
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        return NO_STRINGS;
    }

    /**
     * Contextualization is needed to see whether we can "inline" deserialization
     * of String values, or if we have to use separate value deserializer.
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
            throws JsonMappingException
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
        NullValueProvider nuller = findContentNullProvider(ctxt, property, deser);
        // Ok ok: if all we got is the default String deserializer, can just forget about it
        if ((deser != null) && isDefaultDeserializer(deser)) {
            deser = null;
        }
        if ((_elementDeserializer == deser)
                && (Objects.equals(_unwrapSingle, unwrapSingle))
                && (_nullProvider == nuller)) {
            return this;
        }
        return new StringArrayDeserializer(deser, nuller, unwrapSingle);
    }

    @Override
    public String[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (!p.isExpectedStartArrayToken()) {
            return handleNonArray(p, ctxt);
        }
        if (_elementDeserializer != null) {
            return _deserializeCustom(p, ctxt, null);
        }

        final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] chunk = buffer.resetAndStart();

        int ix = 0;

        try {
            while (true) {
                String value = p.nextTextValue();
                if (value == null) {
                    JsonToken t = p.currentToken();
                    if (t == JsonToken.END_ARRAY) {
                        break;
                    }
                    if (t == JsonToken.VALUE_NULL) {
                        if (_skipNullValues) {
                            continue;
                        }
                        value = (String) _nullProvider.getNullValue(ctxt);
                    } else {
                        value = _parseString(p, ctxt, _nullProvider);
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
    protected final String[] _deserializeCustom(JsonParser p, DeserializationContext ctxt,
            String[] old) throws IOException
    {
        final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        int ix;
        Object[] chunk;

        if (old == null) {
            ix = 0;
            chunk = buffer.resetAndStart();
        } else {
            ix = old.length;
            chunk = buffer.resetAndStart(old, ix);
        }

        final JsonDeserializer<String> deser = _elementDeserializer;

        try {
            while (true) {
                /* 30-Dec-2014, tatu: This may look odd, but let's actually call method
                 *   that suggest we are expecting a String; this helps with some formats,
                 *   notably XML. Note, however, that while we can get String, we can't
                 *   assume that's what we use due to custom deserializer
                 */
                String value;
                if (p.nextTextValue() == null) {
                    JsonToken t = p.currentToken();
                    if (t == JsonToken.END_ARRAY) {
                        break;
                    }
                    // Ok: no need to convert Strings, but must recognize nulls
                    if (t == JsonToken.VALUE_NULL) {
                        if (_skipNullValues) {
                            continue;
                        }
                        value = (String) _nullProvider.getNullValue(ctxt);
                    } else {
                        value = deser.deserialize(p, ctxt);
                    }
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

    @Override
    public String[] deserialize(JsonParser p, DeserializationContext ctxt,
            String[] intoValue) throws IOException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (!p.isExpectedStartArrayToken()) {
            String[] arr = handleNonArray(p, ctxt);
            if (arr == null) {
                return intoValue;
            }
            final int offset = intoValue.length;
            String[] result = new String[offset + arr.length];
            System.arraycopy(intoValue, 0, result, 0, offset);
            System.arraycopy(arr, 0, result, offset, arr.length);
            return result;
        }

        if (_elementDeserializer != null) {
            return _deserializeCustom(p, ctxt, intoValue);
        }
        final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        int ix = intoValue.length;
        Object[] chunk = buffer.resetAndStart(intoValue, ix);

        try {
            while (true) {
                String value = p.nextTextValue();
                if (value == null) {
                    JsonToken t = p.currentToken();
                    if (t == JsonToken.END_ARRAY) {
                        break;
                    }
                    if (t == JsonToken.VALUE_NULL) {
                        // 03-Feb-2017, tatu: Should we skip null here or not?
                        if (_skipNullValues) {
                            return NO_STRINGS;
                        }
                        value = (String) _nullProvider.getNullValue(ctxt);
                    } else {
                        value = _parseString(p, ctxt, _nullProvider);
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

    private final String[] handleNonArray(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // implicit arrays from single values?
        boolean canWrap = (_unwrapSingle == Boolean.TRUE) ||
                ((_unwrapSingle == null) &&
                        ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
        if (canWrap) {
            String value;
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                value = (String) _nullProvider.getNullValue(ctxt);
            } else {
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    String textValue = p.getText();
                    // https://github.com/FasterXML/jackson-dataformat-xml/issues/513
                    if (textValue.isEmpty()) {
                        final CoercionAction act = ctxt.findCoercionAction(logicalType(), handledType(),
                                CoercionInputShape.EmptyString);
                        if (act != CoercionAction.Fail) {
                            return (String[]) _deserializeFromEmptyString(p, ctxt, act, handledType(),
                                    "empty String (\"\")");
                        }
                    } else if (_isBlank(textValue)) {
                        final CoercionAction act = ctxt.findCoercionFromBlankString(logicalType(), handledType(),
                                CoercionAction.Fail);
                        if (act != CoercionAction.Fail) {
                            return (String[]) _deserializeFromEmptyString(p, ctxt, act, handledType(),
                                    "blank String (all whitespace)");
                        }
                    }
                    // if coercion failed, we can still add it to an array
                }

                value = _parseString(p, ctxt, _nullProvider);
            }
            return new String[] { value };
        }
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return _deserializeFromString(p, ctxt);
        }
        return (String[]) ctxt.handleUnexpectedToken(_valueClass, p);
    }
}
