package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId.Referring;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Basic serializer that can take JSON "Array" structure and
 * construct a {@link java.util.Collection} instance, with typed contents.
 *<p>
 * Note: for untyped content (one indicated by passing Object.class
 * as the type), {@link UntypedObjectDeserializer} is used instead.
 * It can also construct {@link java.util.List}s, but not with specific
 * POJO types, only other containers and primitives/wrappers.
 */
@JacksonStdImpl
public class CollectionDeserializer
    extends ContainerDeserializerBase<Collection<Object>>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = -1L; // since 2.5

    // // Configuration

    /**
     * Value deserializer.
     */
    protected final JsonDeserializer<Object> _valueDeserializer;

    /**
     * If element instances have polymorphic type information, this
     * is the type deserializer that can handle it
     */
    protected final TypeDeserializer _valueTypeDeserializer;

    // // Instance construction settings:

    protected final ValueInstantiator _valueInstantiator;

    /**
     * Deserializer that is used iff delegate-based creator is
     * to be used for deserializing from JSON Object.
     */
    protected final JsonDeserializer<Object> _delegateDeserializer;

    // NOTE: no PropertyBasedCreator, as JSON Arrays have no properties

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Constructor for context-free instances, where we do not yet know
     * which property is using this deserializer.
     */
    public CollectionDeserializer(JavaType collectionType,
            JsonDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser, ValueInstantiator valueInstantiator)
    {
        this(collectionType, valueDeser, valueTypeDeser, valueInstantiator, null, null, null);
    }

    /**
     * Constructor used when creating contextualized instances.
     *
     * @since 2.9
     */
    protected CollectionDeserializer(JavaType collectionType,
            JsonDeserializer<Object> valueDeser, TypeDeserializer valueTypeDeser,
            ValueInstantiator valueInstantiator, JsonDeserializer<Object> delegateDeser,
            NullValueProvider nuller, Boolean unwrapSingle)
    {
        super(collectionType, nuller, unwrapSingle);
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
        _valueInstantiator = valueInstantiator;
        _delegateDeserializer = delegateDeser;
    }

    /**
     * Copy-constructor that can be used by sub-classes to allow
     * copy-on-write styling copying of settings of an existing instance.
     */
    protected CollectionDeserializer(CollectionDeserializer src)
    {
        super(src);
        _valueDeserializer = src._valueDeserializer;
        _valueTypeDeserializer = src._valueTypeDeserializer;
        _valueInstantiator = src._valueInstantiator;
        _delegateDeserializer = src._delegateDeserializer;
    }

    /**
     * Fluent-factory method call to construct contextual instance.
     *
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    protected CollectionDeserializer withResolved(JsonDeserializer<?> dd,
            JsonDeserializer<?> vd, TypeDeserializer vtd,
            NullValueProvider nuller, Boolean unwrapSingle)
    {
        return new CollectionDeserializer(_containerType,
                (JsonDeserializer<Object>) vd, vtd,
                _valueInstantiator, (JsonDeserializer<Object>) dd,
                nuller, unwrapSingle);
    }

    // Important: do NOT cache if polymorphic values
    @Override // since 2.5
    public boolean isCachable() {
        // 26-Mar-2015, tatu: As per [databind#735], need to be careful
        return (_valueDeserializer == null)
                && (_valueTypeDeserializer == null)
                && (_delegateDeserializer == null)
                ;
    }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Collection;
    }

    /*
    /**********************************************************
    /* Validation, post-processing (ResolvableDeserializer)
    /**********************************************************
     */

    /**
     * Method called to finalize setup of this deserializer,
     * when it is known for which property deserializer is needed
     * for.
     */
    @Override
    public CollectionDeserializer createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        // May need to resolve types for delegate-based creators:
        JsonDeserializer<Object> delegateDeser = null;
        if (_valueInstantiator != null) {
            if (_valueInstantiator.canCreateUsingDelegate()) {
                JavaType delegateType = _valueInstantiator.getDelegateType(ctxt.getConfig());
                if (delegateType == null) {
                    ctxt.reportBadDefinition(_containerType, String.format(
"Invalid delegate-creator definition for %s: value instantiator (%s) returned true for 'canCreateUsingDelegate()', but null for 'getDelegateType()'",
_containerType,
                            _valueInstantiator.getClass().getName()));
                }
                delegateDeser = findDeserializer(ctxt, delegateType, property);
            } else if (_valueInstantiator.canCreateUsingArrayDelegate()) {
                JavaType delegateType = _valueInstantiator.getArrayDelegateType(ctxt.getConfig());
                if (delegateType == null) {
                    ctxt.reportBadDefinition(_containerType, String.format(
"Invalid delegate-creator definition for %s: value instantiator (%s) returned true for 'canCreateUsingArrayDelegate()', but null for 'getArrayDelegateType()'",
                            _containerType,
                            _valueInstantiator.getClass().getName()));
                }
                delegateDeser = findDeserializer(ctxt, delegateType, property);
            }
        }
        // [databind#1043]: allow per-property allow-wrapping of single overrides:
        // 11-Dec-2015, tatu: Should we pass basic `Collection.class`, or more refined? Mostly
        //   comes down to "List vs Collection" I suppose... for now, pass Collection
        Boolean unwrapSingle = findFormatFeature(ctxt, property, Collection.class,
                JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        // also, often value deserializer is resolved here:
        JsonDeserializer<?> valueDeser = _valueDeserializer;

        // May have a content converter
        valueDeser = findConvertingContentDeserializer(ctxt, property, valueDeser);
        final JavaType vt = _containerType.getContentType();
        if (valueDeser == null) {
            valueDeser = ctxt.findContextualValueDeserializer(vt, property);
        } else { // if directly assigned, probably not yet contextual, so:
            valueDeser = ctxt.handleSecondaryContextualization(valueDeser, property, vt);
        }
        // and finally, type deserializer needs context as well
        TypeDeserializer valueTypeDeser = _valueTypeDeserializer;
        if (valueTypeDeser != null) {
            valueTypeDeser = valueTypeDeser.forProperty(property);
        }
        NullValueProvider nuller = findContentNullProvider(ctxt, property, valueDeser);
        if ((!Objects.equals(unwrapSingle, _unwrapSingle))
                || (nuller != _nullProvider)
                || (delegateDeser != _delegateDeserializer)
                || (valueDeser != _valueDeserializer)
                || (valueTypeDeser != _valueTypeDeserializer)
        ) {
            return withResolved(delegateDeser, valueDeser, valueTypeDeser,
                    nuller, unwrapSingle);
        }
        return this;
    }

    /*
    /**********************************************************
    /* ContainerDeserializerBase API
    /**********************************************************
     */

    @Override
    public JsonDeserializer<Object> getContentDeserializer() {
        return _valueDeserializer;
    }

    @Override
    public ValueInstantiator getValueInstantiator() {
        return _valueInstantiator;
    }

    /*
    /**********************************************************
    /* JsonDeserializer API
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Object> deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        if (_delegateDeserializer != null) {
            return (Collection<Object>) _valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(p, ctxt));
        }
        // 16-May-2020, tatu: As per [dataformats-text#199] need to first check for
        //   possible Array-coercion and only after that String coercion
        if (p.isExpectedStartArrayToken()) {
            return _deserializeFromArray(p, ctxt, createDefaultInstance(ctxt));
        }
        // Empty String may be ok; bit tricky to check, however, since
        // there is also possibility of "auto-wrapping" of single-element arrays.
        // Hence we only accept empty String here.
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return _deserializeFromString(p, ctxt, p.getText());
        }
        return handleNonArray(p, ctxt, createDefaultInstance(ctxt));
    }

    /**
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    protected Collection<Object> createDefaultInstance(DeserializationContext ctxt)
        throws IOException
    {
        return (Collection<Object>) _valueInstantiator.createUsingDefault(ctxt);
    }

    @Override
    public Collection<Object> deserialize(JsonParser p, DeserializationContext ctxt,
            Collection<Object> result)
        throws IOException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (p.isExpectedStartArrayToken()) {
            return _deserializeFromArray(p, ctxt, result);
        }
        return handleNonArray(p, ctxt, result);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException
    {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromArray(p, ctxt);
    }

    /**
     * Logic extracted to deal with incoming String value.
     *
     * @since 2.12
     */
    @SuppressWarnings("unchecked")
    protected Collection<Object> _deserializeFromString(JsonParser p, DeserializationContext ctxt,
            String value)
        throws IOException
    {
        final Class<?> rawTargetType = handledType();

        // 05-Nov-2020, ckozak: As per [jackson-databind#2922] string values may be handled
        // using handleNonArray, however empty strings may result in a null or empty collection
        // depending on configuration.

        // Start by verifying if we got empty/blank string since accessing
        // CoercionAction may be costlier than String value we'll almost certainly
        // need anyway
        if (value.isEmpty()) {
            CoercionAction act = ctxt.findCoercionAction(logicalType(), rawTargetType,
                    CoercionInputShape.EmptyString);
            // handleNonArray may successfully deserialize the result (if
            // ACCEPT_SINGLE_VALUE_AS_ARRAY is enabled, for example) otherwise it
            // is capable of failing just as well as _deserializeFromEmptyString.
            if (act != null && act != CoercionAction.Fail) {
                return (Collection<Object>) _deserializeFromEmptyString(
                        p, ctxt, act, rawTargetType, "empty String (\"\")");
            }
            // note: `CoercionAction.Fail` falls through because we may need to allow
            // `ACCEPT_SINGLE_VALUE_AS_ARRAY` handling later on
        }
        // 26-Mar-2021, tatu: Some day is today; as per [dataformat-xml#460],
        //    we do need to support blank String too...
        else if (_isBlank(value)) {
            final CoercionAction act = ctxt.findCoercionFromBlankString(logicalType(), rawTargetType,
                    CoercionAction.Fail);
            if (act != CoercionAction.Fail) {
                return (Collection<Object>) _deserializeFromEmptyString(
                        p, ctxt, act, rawTargetType, "blank String (all whitespace)");
            }
            // note: `CoercionAction.Fail` falls through because we may need to allow
            // `ACCEPT_SINGLE_VALUE_AS_ARRAY` handling later on
        }
        return handleNonArray(p, ctxt, createDefaultInstance(ctxt));
    }

    /**
     * @since 2.12
     */
    protected Collection<Object> _deserializeFromArray(JsonParser p, DeserializationContext ctxt,
            Collection<Object> result)
        throws IOException
    {
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(result);

        JsonDeserializer<Object> valueDes = _valueDeserializer;
        // Let's offline handling of values with Object Ids (simplifies code here)
        if (valueDes.getObjectIdReader() != null) {
            return _deserializeWithObjectId(p, ctxt, result);
        }
        final TypeDeserializer typeDeser = _valueTypeDeserializer;
        JsonToken t;
        while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
            try {
                Object value;
                if (t == JsonToken.VALUE_NULL) {
                    if (_skipNullValues) {
                        continue;
                    }
                    value = _nullProvider.getNullValue(ctxt);
                } else if (typeDeser == null) {
                    value = valueDes.deserialize(p, ctxt);
                } else {
                    value = valueDes.deserializeWithType(p, ctxt, typeDeser);
                }
                result.add(value);

                /* 17-Dec-2017, tatu: should not occur at this level...
            } catch (UnresolvedForwardReference reference) {
                throw JsonMappingException
                    .from(p, "Unresolved forward reference but no identity info", reference);
                */
            } catch (Exception e) {
                boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
                if (!wrap) {
                    ClassUtil.throwIfRTE(e);
                }
                throw JsonMappingException.wrapWithPath(e, result, result.size());
            }
        }
        return result;
    }

    /**
     * Helper method called when current token is no START_ARRAY. Will either
     * throw an exception, or try to handle value as if member of implicit
     * array, depending on configuration.
     */
    @SuppressWarnings("unchecked")
    protected final Collection<Object> handleNonArray(JsonParser p, DeserializationContext ctxt,
            Collection<Object> result)
        throws IOException
    {
        // Implicit arrays from single values?
        boolean canWrap = (_unwrapSingle == Boolean.TRUE) ||
                ((_unwrapSingle == null) &&
                        ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
        if (!canWrap) {
            return (Collection<Object>) ctxt.handleUnexpectedToken(_containerType, p);
        }
        JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        Object value;

        try {
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                // 03-Feb-2017, tatu: Hmmh. I wonder... let's try skipping here, too
                if (_skipNullValues) {
                    return result;
                }
                value = _nullProvider.getNullValue(ctxt);
            } else if (typeDeser == null) {
                value = valueDes.deserialize(p, ctxt);
            } else {
                value = valueDes.deserializeWithType(p, ctxt, typeDeser);
            }
        } catch (Exception e) {
            boolean wrap = ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
            if (!wrap) {
                ClassUtil.throwIfRTE(e);
            }
            // note: pass Object.class, not Object[].class, as we need element type for error info
            throw JsonMappingException.wrapWithPath(e, Object.class, result.size());
        }
        result.add(value);
        return result;
    }

    protected Collection<Object> _deserializeWithObjectId(JsonParser p, DeserializationContext ctxt,
            Collection<Object> result)
        throws IOException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (!p.isExpectedStartArrayToken()) {
            return handleNonArray(p, ctxt, result);
        }
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(result);

        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;
        CollectionReferringAccumulator referringAccumulator =
                new CollectionReferringAccumulator(_containerType.getContentType().getRawClass(), result);

        JsonToken t;
        while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
            try {
                Object value;
                if (t == JsonToken.VALUE_NULL) {
                    if (_skipNullValues) {
                        continue;
                    }
                    value = _nullProvider.getNullValue(ctxt);
                } else if (typeDeser == null) {
                    value = valueDes.deserialize(p, ctxt);
                } else {
                    value = valueDes.deserializeWithType(p, ctxt, typeDeser);
                }
                referringAccumulator.add(value);
            } catch (UnresolvedForwardReference reference) {
                Referring ref = referringAccumulator.handleUnresolvedReference(reference);
                reference.getRoid().appendReferring(ref);
            } catch (Exception e) {
                boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
                if (!wrap) {
                    ClassUtil.throwIfRTE(e);
                }
                throw JsonMappingException.wrapWithPath(e, result, result.size());
            }
        }
        return result;
    }

    /**
     * Helper class for dealing with Object Id references for values contained in
     * collections being deserialized.
     */
    public static class CollectionReferringAccumulator {
        private final Class<?> _elementType;
        private final Collection<Object> _result;

        /**
         * A list of {@link CollectionReferring} to maintain ordering.
         */
        private List<CollectionReferring> _accumulator = new ArrayList<CollectionReferring>();

        public CollectionReferringAccumulator(Class<?> elementType, Collection<Object> result) {
            _elementType = elementType;
            _result = result;
        }

        public void add(Object value)
        {
            if (_accumulator.isEmpty()) {
                _result.add(value);
            } else {
                CollectionReferring ref = _accumulator.get(_accumulator.size() - 1);
                ref.next.add(value);
            }
        }

        public Referring handleUnresolvedReference(UnresolvedForwardReference reference)
        {
            CollectionReferring id = new CollectionReferring(this, reference, _elementType);
            _accumulator.add(id);
            return id;
        }

        public void resolveForwardReference(Object id, Object value) throws IOException
        {
            Iterator<CollectionReferring> iterator = _accumulator.iterator();
            // Resolve ordering after resolution of an id. This mean either:
            // 1- adding to the result collection in case of the first unresolved id.
            // 2- merge the content of the resolved id with its previous unresolved id.
            Collection<Object> previous = _result;
            while (iterator.hasNext()) {
                CollectionReferring ref = iterator.next();
                if (ref.hasId(id)) {
                    iterator.remove();
                    previous.add(value);
                    previous.addAll(ref.next);
                    return;
                }
                previous = ref.next;
            }

            throw new IllegalArgumentException("Trying to resolve a forward reference with id [" + id
                    + "] that wasn't previously seen as unresolved.");
        }
    }

    /**
     * Helper class to maintain processing order of value. The resolved
     * object associated with {@link #_id} comes before the values in
     * {@link #next}.
     */
    private final static class CollectionReferring extends Referring {
        private final CollectionReferringAccumulator _parent;
        public final List<Object> next = new ArrayList<Object>();

        CollectionReferring(CollectionReferringAccumulator parent,
                UnresolvedForwardReference reference, Class<?> contentType)
        {
            super(reference, contentType);
            _parent = parent;
        }

        @Override
        public void handleResolvedForwardReference(Object id, Object value) throws IOException {
            _parent.resolveForwardReference(id, value);
        }
    }
}
