package tools.jackson.databind.deser.jdk;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.deser.NullValueProvider;
import tools.jackson.databind.deser.ReadableObjectId.Referring;
import tools.jackson.databind.deser.UnresolvedForwardReference;
import tools.jackson.databind.deser.std.ContainerDeserializerBase;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.ArrayType;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.util.AccessPattern;
import tools.jackson.databind.util.ObjectBuffer;

/**
 * Serializer that can serialize non-primitive arrays.
 */
@JacksonStdImpl
public class ObjectArrayDeserializer
    extends ContainerDeserializerBase<Object>
{
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
    protected ValueDeserializer<Object> _elementDeserializer;

    /**
     * If element instances have polymorphic type information, this
     * is the type deserializer that can handle it
     */
    protected final TypeDeserializer _elementTypeDeserializer;

    /**
     * Zero-sized value of array type.
     */
    protected final Object[] _emptyValue;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public ObjectArrayDeserializer(JavaType arrayType0,
            ValueDeserializer<Object> elemDeser, TypeDeserializer elemTypeDeser)
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
            ValueDeserializer<Object> elemDeser, TypeDeserializer elemTypeDeser,
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
            ValueDeserializer<?> elemDeser)
    {
        return withResolved(elemTypeDeser, elemDeser,
                _nullProvider, _unwrapSingle);
    }

    @SuppressWarnings("unchecked")
    public ObjectArrayDeserializer withResolved(TypeDeserializer elemTypeDeser,
            ValueDeserializer<?> elemDeser, NullValueProvider nuller, Boolean unwrapSingle)
    {
        if ((Objects.equals(unwrapSingle, _unwrapSingle)) && (nuller == _nullProvider)
                && (elemDeser == _elementDeserializer)
                && (elemTypeDeser == _elementTypeDeserializer)) {
            return this;
        }
        return new ObjectArrayDeserializer(this,
                (ValueDeserializer<Object>) elemDeser, elemTypeDeser,
                nuller, unwrapSingle);
    }

    @Override
    public boolean isCachable() {
        // Important: do NOT cache if polymorphic values, or if there are annotation-based
        // custom deserializers
        return (_elementDeserializer == null) && (_elementTypeDeserializer == null);
    }

    @Override
    public LogicalType logicalType() {
        return LogicalType.Array;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        ValueDeserializer<?> valueDeser = _elementDeserializer;
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
    /**********************************************************************
    /* ContainerDeserializerBase API
    /**********************************************************************
     */

    @Override
    public ValueDeserializer<Object> getContentDeserializer() {
        return _elementDeserializer;
    }

    @Override
    public AccessPattern getEmptyAccessPattern() {
        // immutable, shareable so:
        return AccessPattern.CONSTANT;
    }

    // need to override as we can't expose ValueInstantiator
    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        // 03-Jul-2020, tatu: Must be assignment-compatible; cannot just return `new Object[0]`
        //   if element type is different
        return _emptyValue;
    }

    /*
    /**********************************************************************
    /* ValueDeserializer API
    /**********************************************************************
     */

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (!p.isExpectedStartArrayToken()) {
            return _handleNonArray(p, ctxt);
        }
        if (_elementDeserializer.getObjectIdReader(ctxt) != null) {
            return _deserializeWithObjectId(p, ctxt);
        }
        final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] chunk = buffer.resetAndStart();
        int ix = 0;
        return _deserialize(p, ctxt, buffer, ix, chunk);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws JacksonException
    {
        // Should there be separate handling for base64 stuff?
        // for now this should be enough:
        return (Object[]) typeDeserializer.deserializeTypedFromArray(p, ctxt);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt,
            Object intoValue0)
        throws JacksonException
    {
        final Object[] intoValue = (Object[]) intoValue0;
        if (!p.isExpectedStartArrayToken()) {
            Object[] arr = (Object[]) _handleNonArray(p, ctxt);
            if (arr == null) {
                return intoValue;
            }
            final int offset = intoValue.length;
            Object[] result = Arrays.copyOf(intoValue, offset + arr.length);
            System.arraycopy(arr, 0, result, offset, arr.length);
            return result;
        }
        if (_elementDeserializer.getObjectIdReader(ctxt) != null) {
            return _deserializeWithObjectId(p, ctxt);
        }

        final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        int ix = intoValue.length;
        Object[] chunk = buffer.resetAndStart(intoValue, ix);
        return _deserialize(p, ctxt, buffer, ix, chunk);
    }

    protected Object[] _deserialize(JsonParser p, DeserializationContext ctxt,
            final ObjectBuffer buffer, int ix, Object[] chunk)
    {
        JsonToken t;
        while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
            Object value;
            try {
                if (t == JsonToken.VALUE_NULL) {
                    if (_skipNullValues) {
                        continue;
                    }
                    value = null;
                } else {
                    value = _deserializeNoNullChecks(p, ctxt);
                }
                if (value == null) {
                    value = _nullProvider.getNullValue(ctxt);
                    if (value == null && _skipNullValues) {
                        continue;
                    }
                }
            } catch (Exception e) {
                throw DatabindException.wrapWithPath(ctxt, e,
                        new JacksonException.Reference(chunk, buffer.bufferedSize() + ix));
            }

            if (ix >= chunk.length) {
                chunk = buffer.appendCompletedChunk(chunk);
                ix = 0;
            }
            chunk[ix++] = value;
        }

        final Object[] result;
        if (_untyped) {
            result = buffer.completeAndClearBuffer(chunk, ix);
        } else {
            result = buffer.completeAndClearBuffer(chunk, ix, _elementClass);
        }
        ctxt.returnObjectBuffer(buffer);
        return result;
    }
    
    protected Object[] _deserializeWithObjectId(JsonParser p, DeserializationContext ctxt)
    {
        final ObjectArrayReferringAccumulator acc = new ObjectArrayReferringAccumulator(_untyped, _elementClass);

        JsonToken t;

        int ix = 0;
        while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
            try {
                Object value;

                if (t == JsonToken.VALUE_NULL) {
                    if (_skipNullValues) {
                        continue;
                    }
                    value = null;
                } else {
                    value = _deserializeNoNullChecks(p, ctxt);
                }

                if (value == null) {
                    value = _nullProvider.getNullValue(ctxt);

                    if (value == null && _skipNullValues) {
                        continue;
                    }
                }
                acc.add(value);
            } catch (UnresolvedForwardReference reference) {
                if (acc == null) {
                    throw reference;
                }
                ArrayReferring referring = new ArrayReferring(reference, _elementClass, acc);
                reference.getRoid().appendReferring(referring);
            } catch (Exception e) {
                throw DatabindException.wrapWithPath(ctxt, e,
                        // 22-Nov-2025, tatu: Not ideal but has to do
                        new JacksonException.Reference(acc.buildArray(), ix));
            }
            ++ix;
        }

        return acc.buildArray();
    }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    protected Byte[] deserializeFromBase64(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // First same as what PrimitiveArrayDeserializers.ByteDeser does:
        byte[] b = p.getBinaryValue(ctxt.getBase64Variant());
        // But then need to convert to wrappers
        Byte[] result = new Byte[b.length];
        for (int i = 0, len = b.length; i < len; ++i) {
            result[i] = b[i];
        }
        return result;
    }

    protected Object _handleNonArray(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
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
            return ctxt.handleUnexpectedToken(_containerType, p);
        }

        // 03-Jan-2026: [databind#5541] Support Object Id for implicit Object[]s too
        if (_elementDeserializer.getObjectIdReader(ctxt) != null) {
            return _wrapSingleWithObjectId(p, ctxt);
        }

        Object value;

        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_NULL) {
            // 03-Feb-2017, tatu: Should this be skipped or not?
            if (_skipNullValues) {
                return _emptyValue;
            }
            value = null;
        } else {
            if (p.hasToken(JsonToken.VALUE_STRING)) {
                String textValue = p.getString();
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

            value = _deserializeNoNullChecks(p, ctxt);
        }

        if (value == null) {
            value = _nullProvider.getNullValue(ctxt);

            if (value == null && _skipNullValues) {
                return _emptyValue;
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

    // @since 2.21
    // Copied from `_deserializeWithObjectId()`
    protected Object[] _wrapSingleWithObjectId(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        final ObjectArrayReferringAccumulator acc = new ObjectArrayReferringAccumulator(_untyped, _elementClass);
        Object value;
        try {
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                if (_skipNullValues) {
                    return _emptyValue;
                }
                value = null;
            } else {
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    String textValue = p.getString();
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

                value = _deserializeNoNullChecks(p, ctxt);
            }

            if (value == null) {
                value = _nullProvider.getNullValue(ctxt);
                if (value == null && _skipNullValues) {
                    return _emptyValue;
                }
            }
            acc.add(value);
        } catch (UnresolvedForwardReference reference) {
            ArrayReferring referring = new ArrayReferring(reference, _elementClass, acc);
            reference.getRoid().appendReferring(referring);
        }
        return acc.buildArray();
    }

    /**
     * Deserialize the content of the map.
     * If _elementTypeDeserializer is null, use _elementDeserializer.deserialize; if non-null,
     * use _elementDeserializer.deserializeWithType to deserialize value.
     * This method only performs deserialization and does not consider _skipNullValues, _nullProvider, etc.
     */
    protected Object _deserializeNoNullChecks(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        if (_elementTypeDeserializer == null) {
            return _elementDeserializer.deserialize(p, ctxt);
        }
        return _elementDeserializer.deserializeWithType(p, ctxt, _elementTypeDeserializer);
    }

    // @since 3.1
    private static class ObjectArrayReferringAccumulator {
        private final boolean _untyped;
        private final Class<?> _elementType;
        private final List<Object> _accumulator = new ArrayList<>();

        private Object[] _array;

        ObjectArrayReferringAccumulator(boolean untyped, Class<?> elementType) {
            _untyped = untyped;
            _elementType = elementType;
        }

        void add(Object value) {
            _accumulator.add(value);
        }

        Object[] buildArray() {
            if (_untyped) {
                _array = new Object[_accumulator.size()];
            } else {
                _array = (Object[]) Array.newInstance(_elementType, _accumulator.size());
            }
            for (int i = 0; i < _accumulator.size(); i++) {
                if (!(_accumulator.get(i) instanceof ArrayReferring)) {
                    _array[i] = _accumulator.get(i);
                }
            }
            return _array;
        }
    }

    private static class ArrayReferring extends Referring {
        private final ObjectArrayReferringAccumulator _parent;

        ArrayReferring(UnresolvedForwardReference ref,
                Class<?> type,
                ObjectArrayReferringAccumulator acc) {
            super(ref, type);
            _parent = acc;
            _parent._accumulator.add(this);
        }

        @Override
        public void handleResolvedForwardReference(DeserializationContext ctxt,
                Object id, Object value) throws JacksonException {
            for (int i = 0; i < _parent._accumulator.size(); i++) {
                if (_parent._accumulator.get(i) == this) {
                    _parent._array[i] = value;
                    return;
                }
            }
            throw new IllegalArgumentException("Trying to resolve unknown reference: " + id);
        }
    }
}
