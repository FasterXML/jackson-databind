package tools.jackson.databind.deser.jdk;

import java.util.*;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.deser.*;
import tools.jackson.databind.deser.ReadableObjectId.Referring;
import tools.jackson.databind.deser.std.ContainerDeserializerBase;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.util.ClassUtil;

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
{
    // // Configuration

    /**
     * Value deserializer.
     */
    protected final ValueDeserializer<Object> _valueDeserializer;

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
    protected final ValueDeserializer<Object> _delegateDeserializer;

    /**
     * Annotations defined on the actual Collection class; retained to avoid
     * re-introspection overhead during {@link #createContextual} calls.
     *
     * @since 3.1
     */
    protected transient final AnnotatedClass _classInfo;

    // NOTE: no PropertyBasedCreator, as JSON Arrays have no properties

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Constructor for context-free instances, where we do not yet know
     * which property is using this deserializer.
     *
     * @since 3.1
     */
    public CollectionDeserializer(JavaType collectionType,
            ValueDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser, ValueInstantiator valueInstantiator,
            AnnotatedClass classInfo)
    {
        this(collectionType, valueDeser, valueTypeDeser, valueInstantiator,
                null, null, null, classInfo);
    }

    @Deprecated // since 3.1
    public CollectionDeserializer(JavaType collectionType,
            ValueDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser, ValueInstantiator valueInstantiator)
    {
        this(collectionType, valueDeser, valueTypeDeser, valueInstantiator,
                null, null, null, null);
    }

    /**
     * Constructor used when creating contextualized instances.
     *
     * @since 3.1
     */
    protected CollectionDeserializer(JavaType collectionType,
            ValueDeserializer<Object> valueDeser, TypeDeserializer valueTypeDeser,
            ValueInstantiator valueInstantiator, ValueDeserializer<Object> delegateDeser,
            NullValueProvider nuller, Boolean unwrapSingle, AnnotatedClass classInfo)
    {
        super(collectionType, nuller, unwrapSingle);
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
        _valueInstantiator = valueInstantiator;
        _delegateDeserializer = delegateDeser;
        _classInfo = classInfo;
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
        _classInfo = src._classInfo;
    }

    /**
     * Factory method called by {@code BasicDeserializerFactory} to create
     * an instance
     *
     * @since 3.1
     */
    public static CollectionDeserializer create(JavaType collectionType,
            BeanDescription.Supplier beanDescRef,
            ValueDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser, ValueInstantiator valueInstantiator)
    {
        return new CollectionDeserializer(collectionType, valueDeser, valueTypeDeser,
                valueInstantiator, beanDescRef.getClassInfo());
    }

    /**
     * Fluent-factory method call to construct contextual instance.
     */
    @SuppressWarnings("unchecked")
    protected CollectionDeserializer withResolved(ValueDeserializer<?> dd,
            ValueDeserializer<?> vd, TypeDeserializer vtd,
            NullValueProvider nuller, Boolean unwrapSingle)
    {
        return new CollectionDeserializer(_containerType,
                (ValueDeserializer<Object>) vd, vtd,
                _valueInstantiator, (ValueDeserializer<Object>) dd,
                nuller, unwrapSingle, _classInfo);
    }

    // Important: do NOT cache if polymorphic values
    @Override
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
     * when it is known for which property deserializer is needed for.
     */
    @Override
    public CollectionDeserializer createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        // May need to resolve types for delegate-based creators:
        ValueDeserializer<Object> delegateDeser = null;
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
                _classInfo,
                JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        // also, often value deserializer is resolved here:
        ValueDeserializer<?> valueDeser = _valueDeserializer;

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
    public ValueDeserializer<Object> getContentDeserializer() {
        return _valueDeserializer;
    }

    @Override
    public ValueInstantiator getValueInstantiator() {
        return _valueInstantiator;
    }

    /*
    /**********************************************************
    /* ValueDeserializer impl
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Object> deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
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
            return _deserializeFromString(p, ctxt, p.getString());
        }
        return _handleNonArray(p, ctxt, createDefaultInstance(ctxt));
    }

    @SuppressWarnings("unchecked")
    protected Collection<Object> createDefaultInstance(DeserializationContext ctxt)
        throws JacksonException
    {
        return (Collection<Object>) _valueInstantiator.createUsingDefault(ctxt);
    }

    @Override
    public Collection<Object> deserialize(JsonParser p, DeserializationContext ctxt,
            Collection<Object> result)
        throws JacksonException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (p.isExpectedStartArrayToken()) {
            return _deserializeFromArray(p, ctxt, result);
        }
        return _handleNonArray(p, ctxt, result);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws JacksonException
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
        throws JacksonException
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
        return _handleNonArray(p, ctxt, createDefaultInstance(ctxt));
    }

    /**
     * @since 2.12
     */
    protected Collection<Object> _deserializeFromArray(JsonParser p, DeserializationContext ctxt,
            Collection<Object> result)
        throws JacksonException
    {
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.assignCurrentValue(result);

        // Let's offline handling of values with Object Ids (simplifies code here)
        if (_valueDeserializer.getObjectIdReader(ctxt) != null) {
            return _deserializeWithObjectId(p, ctxt, result);
        }
        JsonToken t;
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

                    // _skipNullValues is checked by _tryToAddNull.
                    if (value == null) {
                        _tryToAddNull(p, ctxt, result);
                        continue;
                    }
                }

                result.add(value);

                /* 17-Dec-2017, tatu: should not occur at this level...
            } catch (UnresolvedForwardReference reference) {
                throw DatabindException
                    .from(p, "Unresolved forward reference but no identity info", reference);
                */
            } catch (Exception e) {
                boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
                if (!wrap) {
                    ClassUtil.throwIfRTE(e);
                }
                throw DatabindException.wrapWithPath(ctxt, e,
                        new JacksonException.Reference(result, result.size()));
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
    protected final Collection<Object> _handleNonArray(JsonParser p,
            DeserializationContext ctxt, Collection<Object> result)
        throws JacksonException
    {
        // Implicit arrays from single values?
        boolean canWrap = (_unwrapSingle == Boolean.TRUE) ||
                ((_unwrapSingle == null) &&
                        ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
        if (!canWrap) {
            return (Collection<Object>) ctxt.handleUnexpectedToken(_containerType, p);
        }
        JsonToken t = p.currentToken();

        // 03-Jan-2026: [databind#5537] Support Object Id for implicit Collections too
        if (_valueDeserializer.getObjectIdReader(ctxt) != null) {
            return _wrapSingleWithObjectId(p, ctxt, result);
        }

        Object value;

        try {
            if (t == JsonToken.VALUE_NULL) {
                // 03-Feb-2017, tatu: Hmmh. I wonder... let's try skipping here, too
                if (_skipNullValues) {
                    return result;
                }
                value = null;
            } else {
                value = _deserializeNoNullChecks(p, ctxt);
            }

            if (value == null) {
                value = _nullProvider.getNullValue(ctxt);

                // _skipNullValues is checked by _tryToAddNull.
                if (value == null) {
                    _tryToAddNull(p, ctxt, result);
                    return result;
                }
            }
        } catch (Exception e) {
            boolean wrap = ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
            if (!wrap) {
                ClassUtil.throwIfRTE(e);
            }
            // note: pass Object.class, not Object[].class, as we need element type for error info
            throw DatabindException.wrapWithPath(ctxt, e,
                    new JacksonException.Reference(_containerType.getContentType().getRawClass(), result.size()));
        }
        result.add(value);
        return result;
    }

    protected Collection<Object> _deserializeWithObjectId(JsonParser p,
            DeserializationContext ctxt, Collection<Object> result)
        throws JacksonException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (!p.isExpectedStartArrayToken()) {
            return _handleNonArray(p, ctxt, result);
        }
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.assignCurrentValue(result);

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
                referringAccumulator.add(value);
            } catch (UnresolvedForwardReference reference) {
                Referring ref = referringAccumulator.handleUnresolvedReference(reference);
                reference.getRoid().appendReferring(ref);
            } catch (Exception e) {
                boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
                if (!wrap) {
                    ClassUtil.throwIfRTE(e);
                }
                throw DatabindException.wrapWithPath(ctxt, e,
                        new JacksonException.Reference(result, result.size()));
            }
        }
        return result;
    }

    // @since 2.20.2
    // Copied from `_deserializeWithObjectId()` above
    protected Collection<Object> _wrapSingleWithObjectId(JsonParser p,
            DeserializationContext ctxt, Collection<Object> result)
        throws JacksonException
    {
        final CollectionReferringAccumulator referringAccumulator =
                new CollectionReferringAccumulator(getContentType().getRawClass(), result);

        try {
            Object value;
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                if (_skipNullValues) {
                    return result;
                }
                value = null;
            } else {
                value = _deserializeNoNullChecks(p, ctxt);
            }

            if (value == null) {
                value = _nullProvider.getNullValue(ctxt);
                if (value == null) {
                    _tryToAddNull(p, ctxt, result);
                    return result;
                }
            }
            referringAccumulator.add(value);
        } catch (UnresolvedForwardReference reference) {
            Referring ref = referringAccumulator.handleUnresolvedReference(reference);
            reference.getRoid().appendReferring(ref);
        } catch (Exception e) {
            if (!ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS)) {
                ClassUtil.throwIfRTE(e);
            }
            throw DatabindException.wrapWithPath(ctxt, e,
                    new JacksonException.Reference(result, result.size()));
        }
        return result;
    }

    /**
     * Deserialize the content of the collection.
     * If _valueTypeDeserializer is null, use _valueDeserializer.deserialize; if non-null,
     * use _valueDeserializer.deserializeWithType to deserialize value.
     * This method only performs deserialization and does not consider _skipNullValues, _nullProvider, etc.
     */
    protected Object _deserializeNoNullChecks(JsonParser p, DeserializationContext ctxt)
    {
        if (_valueTypeDeserializer == null) {
            return _valueDeserializer.deserialize(p, ctxt);
        }
        return _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
    }

    /**
     * {@code java.util.TreeSet} (and possibly other {@link Collection} types) does not
     * allow addition of {@code null} values, so isolate handling here.
     *
     */
    protected void _tryToAddNull(JsonParser p, DeserializationContext ctxt, Collection<?> set)
    {
        if (_skipNullValues) {
            return;
        }

        // Ideally we'd have better idea of where nulls are accepted, but first
        // let's just produce something better than NPE:
        try {
            set.add(null);
        } catch (NullPointerException e) {
            ctxt.handleUnexpectedToken(_valueType, JsonToken.VALUE_NULL, p,
                    "`java.util.Collection` of type %s does not accept `null` values",
                    ClassUtil.getTypeDescription(getValueType(ctxt)));
        }
    }
    /**
     * Helper class for dealing with Object Id references for values contained in
     * collections being deserialized.
     */
    public static class CollectionReferringAccumulator {
        private final Class<?> _elementType;
        private final Collection<Object> _result;

        /**
         * Values read at or after the first still unresolved reference, in encounter
         * order: plain values, and {@link CollectionReferring}s for references (resolved
         * or not). Values are moved to the result collection from the front (starting at
         * {@link #_pendingStart}) once no unresolved reference precedes them: this retains
         * ordering while moving each value just once -- instead of merging buffers of
         * resolved references into preceding ones, which takes O(N^2) time when
         * references resolve in reverse order (see [databind#6204]).
         */
        private final List<Object> _pending = new ArrayList<>();

        /**
         * Index of the first entry in {@link #_pending} not yet moved to the result.
         */
        private int _pendingStart;

        /**
         * Lookup from unresolved id to the -- usually single -- {@link CollectionReferring}s
         * with that id, in encounter order. Without this, resolution would need to scan
         * all pending references to find the matching one, which makes resolving N
         * references take O(N^2) time (see [databind#6204]).
         */
        private final Map<Object, Deque<CollectionReferring>> _unresolvedById = new HashMap<>();

        public CollectionReferringAccumulator(Class<?> elementType, Collection<Object> result) {
            _elementType = elementType;
            _result = result;
        }

        public void add(Object value)
        {
            if (_pendingStart == _pending.size()) {
                _result.add(value);
            } else {
                _pending.add(value);
            }
        }

        public Referring handleUnresolvedReference(UnresolvedForwardReference reference)
        {
            CollectionReferring id = new CollectionReferring(this, reference, _elementType);
            _pending.add(id);
            // Ids are usable as hash keys: `ObjectIdResolver`s already rely on that
            Deque<CollectionReferring> refs = _unresolvedById.get(reference.getUnresolvedId());
            if (refs == null) {
                refs = new ArrayDeque<>(2);
                _unresolvedById.put(reference.getUnresolvedId(), refs);
            }
            refs.add(id);
            return id;
        }

        public void resolveForwardReference(DeserializationContext ctxt, Object id, Object value) throws JacksonException
        {
            Deque<CollectionReferring> refs = _unresolvedById.get(id);
            if (refs == null) {
                throw new IllegalArgumentException("Trying to resolve a forward reference with id [" + id
                        + "] that wasn't previously seen as unresolved.");
            }
            CollectionReferring ref = refs.removeFirst();
            if (refs.isEmpty()) {
                _unresolvedById.remove(id);
            }
            ref.resolve(value);

            // Move values no longer preceded by an unresolved reference to the result
            final int end = _pending.size();
            int i = _pendingStart;
            for (; i < end; ++i) {
                Object pending = _pending.get(i);
                if (pending instanceof CollectionReferring) {
                    CollectionReferring pendingRef = (CollectionReferring) pending;
                    if (!pendingRef.isResolved()) {
                        break;
                    }
                    pendingRef.markMoved((_result instanceof List<?>) ? _result.size() : -1);
                    pending = pendingRef.resolvedValue();
                }
                _result.add(pending);
                _pending.set(i, null);
            }
            if (i == end) {
                _pending.clear();
                _pendingStart = 0;
            } else {
                _pendingStart = i;
            }
        }

        /**
         * Replace a resolved item in the result collection. Called when the bound
         * item is rebound (e.g., builder → built object) via
         * {@link Referring#handleItemRebind}.
         *
         * @param oldItem Item to replace (Builder)
         * @param newItem Item to replace {@code oldItem} with (Built value)
         *
         * @since 3.2
         */
        public void replaceResolvedItem(Object oldItem, Object newItem) {
            if (_result instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) _result;
                for (int i = 0, len = list.size(); i < len; i++) {
                    if (list.get(i) == oldItem) {
                        list.set(i, newItem);
                    }
                }
            } else if (_containsIdentity(_result, oldItem)) {
                // Non-list collections (e.g. LinkedHashSet, TreeSet): snapshot,
                // clear, and replay with substitution so insertion order /
                // comparator-based ordering is preserved and counts stay correct
                // for any Collection impl.
                List<Object> snapshot = new ArrayList<>(_result);
                _result.clear();
                for (Object item : snapshot) {
                    _result.add(item == oldItem ? newItem : item);
                }
            }
            // Same item may also still be pending, if an earlier forward reference has
            // not yet been resolved: as the value of a resolved reference, or as a plain
            // value (see [databind#6204] for the pending list)
            for (int i = _pendingStart, end = _pending.size(); i < end; ++i) {
                Object pending = _pending.get(i);
                if (pending instanceof CollectionReferring) {
                    CollectionReferring ref = (CollectionReferring) pending;
                    if (ref.isResolved() && (ref.resolvedValue() == oldItem)) {
                        ref.resolve(newItem);
                    }
                } else if (pending == oldItem) {
                    _pending.set(i, newItem);
                }
            }
        }

        /**
         * Replace the item resolved for given reference, after the bound item is rebound
         * (e.g., builder → built object). Unlike {@link #replaceResolvedItem(Object, Object)},
         * only replaces the slot of the reference itself (other references to the same
         * item get calls of their own) where possible: scanning the whole collection
         * instead would make rebinding N references take O(N^2) time.
         *
         * @since 3.2.3
         */
        void replaceResolvedItem(CollectionReferring ref, Object oldItem, Object newItem) {
            if (ref.resolvedValue() == oldItem) {
                ref.resolve(newItem);
                // Still pending: will be moved to result as is
                if (!ref.isMoved()) {
                    return;
                }
                final int index = ref.resultIndex();
                if (index >= 0) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) _result;
                    if ((index < list.size()) && (list.get(index) == oldItem)) {
                        list.set(index, newItem);
                        return;
                    }
                } else if (_result.getClass() == HashSet.class) {
                    // Unordered so position need not be retained
                    if (_result.remove(oldItem)) {
                        _result.add(newItem);
                        return;
                    }
                    // Not found: either already replaced via another reference to it, or
                    // hash code of `oldItem` changed after being added (need to scan)
                    if (_result.contains(newItem)) {
                        return;
                    }
                }
            }
            // Ordered Sets and other Collections (or List modified by caller): need to
            // replace by scanning, to retain ordering
            replaceResolvedItem(oldItem, newItem);
        }

        // @since 3.2
        private static boolean _containsIdentity(Collection<?> coll, Object target) {
            for (Object item : coll) {
                if (item == target) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Placeholder for a value that is a forward reference, to maintain processing order
     * of values: holds the resolved value, once the reference has been resolved.
     */
    private final static class CollectionReferring extends Referring {
        private final CollectionReferringAccumulator _parent;

        private boolean _resolved;
        private Object _value;

        /**
         * Whether resolved value has been moved to the result collection.
         */
        private boolean _moved;

        /**
         * Index of the resolved value in the result collection, if moved there and
         * result is a {@link List}; -1 otherwise.
         */
        private int _resultIndex = -1;

        CollectionReferring(CollectionReferringAccumulator parent,
                UnresolvedForwardReference reference, Class<?> contentType)
        {
            super(reference, contentType);
            _parent = parent;
        }

        void resolve(Object value) {
            _value = value;
            _resolved = true;
        }

        boolean isResolved() { return _resolved; }

        Object resolvedValue() { return _value; }

        void markMoved(int resultIndex) {
            _moved = true;
            _resultIndex = resultIndex;
        }

        boolean isMoved() { return _moved; }

        int resultIndex() { return _resultIndex; }

        @Override
        public void handleResolvedForwardReference(DeserializationContext ctxt, Object id, Object value) throws JacksonException {
            _parent.resolveForwardReference(ctxt, id, value);
        }

        @Override
        public void handleItemRebind(Object oldItem, Object newItem) {
            _parent.replaceResolvedItem(this, oldItem, newItem);
        }
    }

}
