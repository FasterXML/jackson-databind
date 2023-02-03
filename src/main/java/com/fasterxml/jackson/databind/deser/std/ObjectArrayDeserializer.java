package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.AccessPattern;
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
     * @since 2.12
     */
    protected final Object[] _emptyValue;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public ObjectArrayDeserializer(JavaType arrayType0,
            JsonDeserializer<Object> elemDeser, TypeDeserializer elemTypeDeser)
    {
        super(arrayType0, null, null);
        ArrayType arrayType = (ArrayType) arrayType0;
        _elementClass = arrayType.getContentType().getRawClass();
        _untyped = (_elementClass == Object.class);
        _elementDeserializer = elemDeser;
        _elementTypeDeserializer = elemTypeDeser;
        _emptyValue = arrayType.getEmptyArray();
    }

    protected ObjectArrayDeserializer(ObjectArrayDeserializer base,
            JsonDeserializer<Object> elemDeser, TypeDeserializer elemTypeDeser,
            NullValueProvider nuller, Boolean unwrapSingle)
    {
        super(base, nuller, unwrapSingle);
        _elementClass = base._elementClass;
        _untyped = base._untyped;
        _emptyValue = base._emptyValue;

        _elementDeserializer = elemDeser;
        _elementTypeDeserializer = elemTypeDeser;
    }

    /**
     * Overridable fluent-factory method used to create contextual instances
     */
    public ObjectArrayDeserializer withDeserializer(TypeDeserializer elemTypeDeser,
            JsonDeserializer<?> elemDeser)
    {
        return withResolved(elemTypeDeser, elemDeser,
                _nullProvider, _unwrapSingle);
    }

    /**
     * @since 2.7
     */
    @SuppressWarnings("unchecked")
    public ObjectArrayDeserializer withResolved(TypeDeserializer elemTypeDeser,
            JsonDeserializer<?> elemDeser, NullValueProvider nuller, Boolean unwrapSingle)
    {
        if ((Objects.equals(unwrapSingle, _unwrapSingle)) && (nuller == _nullProvider)
                && (elemDeser == _elementDeserializer)
                && (elemTypeDeser == _elementTypeDeserializer)) {
            return this;
        }
        return new ObjectArrayDeserializer(this,
                (JsonDeserializer<Object>) elemDeser, elemTypeDeser,
                nuller, unwrapSingle);
    }

    @Override // since 2.5
    public boolean isCachable() {
        // Important: do NOT cache if polymorphic values, or if there are annotation-based
        // custom deserializers
        return (_elementDeserializer == null) && (_elementTypeDeserializer == null);
    }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Array;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        JsonDeserializer<?> valueDeser = _elementDeserializer;
        // 07-May-2020, tatu: Is the argument `containerType.getRawClass()` right here?
        //    In a way seems like it should rather refer to value class... ?
        //    (as it's individual value of element type, not Container)...
        Boolean unwrapSingle = findFormatFeature(ctxt, property, _containerType.getRawClass(),
                JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        // May have a content converter
        valueDeser = findConvertingContentDeserializer(ctxt, property, valueDeser);
        final JavaType vt = _containerType.getContentType();
        if (valueDeser == null) {
            valueDeser = ctxt.findContextualValueDeserializer(vt, property);
        } else { // if directly assigned, probably not yet contextual, so:
            valueDeser = ctxt.handleSecondaryContextualization(valueDeser, property, vt);
        }
        TypeDeserializer elemTypeDeser = _elementTypeDeserializer;
        if (elemTypeDeser != null) {
            elemTypeDeser = elemTypeDeser.forProperty(property);
        }
        NullValueProvider nuller = findContentNullProvider(ctxt, property, valueDeser);
        return withResolved(elemTypeDeser, valueDeser, nuller, unwrapSingle);
    }

    /*
    /**********************************************************
    /* ContainerDeserializerBase API
    /**********************************************************
     */

    @Override
    public JsonDeserializer<Object> getContentDeserializer() {
        return _elementDeserializer;
    }

    @Override // since 2.9
    public AccessPattern getEmptyAccessPattern() {
        // immutable, shareable so:
        return AccessPattern.CONSTANT;
    }

    // need to override as we can't expose ValueInstantiator
    @Override // since 2.9
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        // 03-Jul-2020, tatu: Must be assignment-compatible; can not just return `new Object[0]`
        //   if element type is different
        return _emptyValue;
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
                    if (_skipNullValues) {
                        continue;
                    }
                    value = _nullProvider.getNullValue(ctxt);
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
        // Should there be separate handling for base64 stuff?
        // for now this should be enough:
        return (Object[]) typeDeserializer.deserializeTypedFromArray(p, ctxt);
    }

    @Override // since 2.9
    public Object[] deserialize(JsonParser p, DeserializationContext ctxt,
            Object[] intoValue) throws IOException
    {
        if (!p.isExpectedStartArrayToken()) {
            Object[] arr = handleNonArray(p, ctxt);
            if (arr == null) {
                return intoValue;
            }
            final int offset = intoValue.length;
            Object[] result = new Object[offset + arr.length];
            System.arraycopy(intoValue, 0, result, 0, offset);
            System.arraycopy(arr, 0, result, offset, arr.length);
            return result;
        }

        final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        int ix = intoValue.length;
        Object[] chunk = buffer.resetAndStart(intoValue, ix);
        JsonToken t;
        final TypeDeserializer typeDeser = _elementTypeDeserializer;

        try {
            while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
                Object value;

                if (t == JsonToken.VALUE_NULL) {
                    if (_skipNullValues) {
                        continue;
                    }
                    value = _nullProvider.getNullValue(ctxt);
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
        // Can we do implicit coercion to a single-element array still?
        boolean canWrap = (_unwrapSingle == Boolean.TRUE) ||
                ((_unwrapSingle == null) &&
                        ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
        if (!canWrap) {
            // 2 exceptions with Strings:
            if (p.hasToken(JsonToken.VALUE_STRING)) {
                // One exception; byte arrays are generally serialized as base64, so that should be handled
                // note: not `byte[]`, but `Byte[]` -- former is primitive array
                if (_elementClass == Byte.class) {
                    return deserializeFromBase64(p, ctxt);
                }
                // Second: empty (and maybe blank) String
                return _deserializeFromString(p, ctxt);
            }
            return (Object[]) ctxt.handleUnexpectedToken(_containerType, p);
        }

        Object value;
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            // 03-Feb-2017, tatu: Should this be skipped or not?
            if (_skipNullValues) {
                return _emptyValue;
            }
            value = _nullProvider.getNullValue(ctxt);
        } else {
            if (p.hasToken(JsonToken.VALUE_STRING)) {
                String textValue = p.getText();
                // https://github.com/FasterXML/jackson-dataformat-xml/issues/513
                if (textValue.isEmpty()) {
                    final CoercionAction act = ctxt.findCoercionAction(logicalType(), handledType(),
                            CoercionInputShape.EmptyString);
                    if (act != CoercionAction.Fail) {
                        return (Object[]) _deserializeFromEmptyString(p, ctxt, act, handledType(),
                                "empty String (\"\")");
                    }
                } else if (_isBlank(textValue)) {
                    final CoercionAction act = ctxt.findCoercionFromBlankString(logicalType(), handledType(),
                            CoercionAction.Fail);
                    if (act != CoercionAction.Fail) {
                        return (Object[]) _deserializeFromEmptyString(p, ctxt, act, handledType(),
                                "blank String (all whitespace)");
                    }
                }
                // if coercion failed, we can still add it to a list
            }

            if (_elementTypeDeserializer == null) {
                value = _elementDeserializer.deserialize(p, ctxt);
            } else {
                value = _elementDeserializer.deserializeWithType(p, ctxt, _elementTypeDeserializer);
            }
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

