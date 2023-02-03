package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.impl.PropertyBasedCreator;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId.Referring;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.IgnorePropertiesUtil;

/**
 * Basic deserializer that can take JSON "Object" structure and
 * construct a {@link java.util.Map} instance, with typed contents.
 *<p>
 * Note: for untyped content (one indicated by passing Object.class
 * as the type), {@link UntypedObjectDeserializer} is used instead.
 * It can also construct {@link java.util.Map}s, but not with specific
 * POJO types, only other containers and primitives/wrappers.
 */
@JacksonStdImpl
public class MapDeserializer
    extends ContainerDeserializerBase<Map<Object,Object>>
    implements ContextualDeserializer, ResolvableDeserializer
{
    private static final long serialVersionUID = 1L;

    // // Configuration: typing, deserializers

    /**
     * Key deserializer to use; either passed via constructor
     * (when indicated by annotations), or resolved when
     * {@link #resolve} is called;
     */
    protected final KeyDeserializer _keyDeserializer;

    /**
     * Flag set to indicate that the key type is
     * {@link java.lang.String} (or {@link java.lang.Object}, for
     * which String is acceptable), <b>and</b> that the
     * default Jackson key deserializer would be used.
     * If both are true, can optimize handling.
     */
    protected boolean _standardStringKey;

    /**
     * Value deserializer.
     */
    protected final JsonDeserializer<Object> _valueDeserializer;

    /**
     * If value instances have polymorphic type information, this
     * is the type deserializer that can handle it
     */
    protected final TypeDeserializer _valueTypeDeserializer;

    // // Instance construction settings:

    protected final ValueInstantiator _valueInstantiator;

    /**
     * Deserializer that is used iff delegate-based creator is
     * to be used for deserializing from JSON Object.
     */
    protected JsonDeserializer<Object> _delegateDeserializer;

    /**
     * If the Map is to be instantiated using non-default constructor
     * or factory method
     * that takes one or more named properties as argument(s),
     * this creator is used for instantiation.
     */
    protected PropertyBasedCreator _propertyBasedCreator;

    protected final boolean _hasDefaultCreator;

    // // Any properties to ignore if seen?

    protected Set<String> _ignorableProperties;

    /**
     * @since 2.12
     */
    protected Set<String> _includableProperties;

    /**
     * Helper object used for name-based filtering
     *
     * @since 2.12
     */
    protected IgnorePropertiesUtil.Checker _inclusionChecker;


    /**
     * Flag used to check, whether the {@link com.fasterxml.jackson.core.StreamReadCapability#DUPLICATE_PROPERTIES}
     * can be applied, because the Map has declared value type of {@code java.lang.Object}.
     *
     * @since 2.14
     */
    protected boolean _checkDupSquash;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public MapDeserializer(JavaType mapType, ValueInstantiator valueInstantiator,
            KeyDeserializer keyDeser, JsonDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser)
    {
        super(mapType, null, null);
        _keyDeserializer = keyDeser;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
        _valueInstantiator = valueInstantiator;
        _hasDefaultCreator = valueInstantiator.canCreateUsingDefault();
        _delegateDeserializer = null;
        _propertyBasedCreator = null;
        _standardStringKey = _isStdKeyDeser(mapType, keyDeser);
        _inclusionChecker = null;
        _checkDupSquash = mapType.getContentType().hasRawClass(Object.class);
    }

    /**
     * Copy-constructor that can be used by sub-classes to allow
     * copy-on-write styling copying of settings of an existing instance.
     */
    protected MapDeserializer(MapDeserializer src)
    {
        super(src);
        _keyDeserializer = src._keyDeserializer;
        _valueDeserializer = src._valueDeserializer;
        _valueTypeDeserializer = src._valueTypeDeserializer;
        _valueInstantiator = src._valueInstantiator;
        _propertyBasedCreator = src._propertyBasedCreator;
        _delegateDeserializer = src._delegateDeserializer;
        _hasDefaultCreator = src._hasDefaultCreator;
        // should we make a copy here?
        _ignorableProperties = src._ignorableProperties;
        _includableProperties = src._includableProperties;
        _inclusionChecker = src._inclusionChecker;

        _standardStringKey = src._standardStringKey;
        _checkDupSquash = src._checkDupSquash;
    }

    protected MapDeserializer(MapDeserializer src,
            KeyDeserializer keyDeser, JsonDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser,
            NullValueProvider nuller,
            Set<String> ignorable)
    {
       this(src, keyDeser,valueDeser, valueTypeDeser, nuller, ignorable, null);
    }

    /**
     * @since 2.12
     */
    protected MapDeserializer(MapDeserializer src,
            KeyDeserializer keyDeser, JsonDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser,
            NullValueProvider nuller,
            Set<String> ignorable,
            Set<String> includable)
    {
        super(src, nuller, src._unwrapSingle);
        _keyDeserializer = keyDeser;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
        _valueInstantiator = src._valueInstantiator;
        _propertyBasedCreator = src._propertyBasedCreator;
        _delegateDeserializer = src._delegateDeserializer;
        _hasDefaultCreator = src._hasDefaultCreator;
        _ignorableProperties = ignorable;
        _includableProperties = includable;
        _inclusionChecker = IgnorePropertiesUtil.buildCheckerIfNeeded(ignorable, includable);

        _standardStringKey = _isStdKeyDeser(_containerType, keyDeser);
        _checkDupSquash = src._checkDupSquash;
    }

    /**
     * Fluent factory method used to create a copy with slightly
     * different settings. When sub-classing, MUST be overridden.
     */
    protected MapDeserializer withResolved(KeyDeserializer keyDeser,
            TypeDeserializer valueTypeDeser, JsonDeserializer<?> valueDeser,
            NullValueProvider nuller,
            Set<String> ignorable)
    {
        return withResolved(keyDeser, valueTypeDeser, valueDeser, nuller, ignorable, _includableProperties);
    }

    /**
     * @since 2.12
     */
    @SuppressWarnings("unchecked")
    protected MapDeserializer withResolved(KeyDeserializer keyDeser,
            TypeDeserializer valueTypeDeser, JsonDeserializer<?> valueDeser,
            NullValueProvider nuller,
            Set<String> ignorable, Set<String> includable)
    {
        if ((_keyDeserializer == keyDeser) && (_valueDeserializer == valueDeser)
                && (_valueTypeDeserializer == valueTypeDeser) && (_nullProvider == nuller)
                && (_ignorableProperties == ignorable) && (_includableProperties == includable)) {
            return this;
        }
        return new MapDeserializer(this,
                keyDeser, (JsonDeserializer<Object>) valueDeser, valueTypeDeser,
                nuller, ignorable, includable);
    }

    /**
     * Helper method used to check whether we can just use the default key
     * deserialization, where JSON String becomes Java String.
     */
    protected final boolean _isStdKeyDeser(JavaType mapType, KeyDeserializer keyDeser)
    {
        if (keyDeser == null) {
            return true;
        }
        JavaType keyType = mapType.getKeyType();
        if (keyType == null) { // assumed to be Object
            return true;
        }
        Class<?> rawKeyType = keyType.getRawClass();
        return ((rawKeyType == String.class || rawKeyType == Object.class)
                && isDefaultKeyDeserializer(keyDeser));
    }

    /**
     * @deprecated in 2.12, remove from 3.0
     */
    @Deprecated
    public void setIgnorableProperties(String[] ignorable) {
        _ignorableProperties = (ignorable == null || ignorable.length == 0) ?
            null : ArrayBuilders.arrayToSet(ignorable);
        _inclusionChecker = IgnorePropertiesUtil.buildCheckerIfNeeded(_ignorableProperties, _includableProperties);
    }

    public void setIgnorableProperties(Set<String> ignorable) {
        _ignorableProperties = (ignorable == null || ignorable.isEmpty()) ?
                null : ignorable;
        _inclusionChecker = IgnorePropertiesUtil.buildCheckerIfNeeded(_ignorableProperties, _includableProperties);
    }

    public void setIncludableProperties(Set<String> includable) {
        _includableProperties = includable;
        _inclusionChecker = IgnorePropertiesUtil.buildCheckerIfNeeded(_ignorableProperties, _includableProperties);
    }

    /*
    /**********************************************************
    /* Validation, post-processing (ResolvableDeserializer)
    /**********************************************************
     */

    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException
    {
        // May need to resolve types for delegate- and/or property-based creators:
        if (_valueInstantiator.canCreateUsingDelegate()) {
            JavaType delegateType = _valueInstantiator.getDelegateType(ctxt.getConfig());
            if (delegateType == null) {
                ctxt.reportBadDefinition(_containerType, String.format(
"Invalid delegate-creator definition for %s: value instantiator (%s) returned true for 'canCreateUsingDelegate()', but null for 'getDelegateType()'",
                _containerType,
                _valueInstantiator.getClass().getName()));
            }
            // Theoretically should be able to get CreatorProperty for delegate
            // parameter to pass; but things get tricky because DelegateCreator
            // may contain injectable values. So, for now, let's pass nothing.
            _delegateDeserializer = findDeserializer(ctxt, delegateType, null);
        } else if (_valueInstantiator.canCreateUsingArrayDelegate()) {
            JavaType delegateType = _valueInstantiator.getArrayDelegateType(ctxt.getConfig());
            if (delegateType == null) {
                ctxt.reportBadDefinition(_containerType, String.format(
"Invalid delegate-creator definition for %s: value instantiator (%s) returned true for 'canCreateUsingArrayDelegate()', but null for 'getArrayDelegateType()'",
                    _containerType,
                    _valueInstantiator.getClass().getName()));
            }
            _delegateDeserializer = findDeserializer(ctxt, delegateType, null);
        }
        if (_valueInstantiator.canCreateFromObjectWith()) {
            SettableBeanProperty[] creatorProps = _valueInstantiator.getFromObjectArguments(ctxt.getConfig());
            _propertyBasedCreator = PropertyBasedCreator.construct(ctxt, _valueInstantiator, creatorProps,
                    ctxt.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
        }
        _standardStringKey = _isStdKeyDeser(_containerType, _keyDeserializer);
    }

    /**
     * Method called to finalize setup of this deserializer,
     * when it is known for which property deserializer is needed for.
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        KeyDeserializer keyDeser = _keyDeserializer;
        if (keyDeser == null) {
            keyDeser = ctxt.findKeyDeserializer(_containerType.getKeyType(), property);
        } else {
            if (keyDeser instanceof ContextualKeyDeserializer) {
                keyDeser = ((ContextualKeyDeserializer) keyDeser).createContextual(ctxt, property);
            }
        }

        JsonDeserializer<?> valueDeser = _valueDeserializer;
        // [databind#125]: May have a content converter
        if (property != null) {
            valueDeser = findConvertingContentDeserializer(ctxt, property, valueDeser);
        }
        final JavaType vt = _containerType.getContentType();
        if (valueDeser == null) {
            valueDeser = ctxt.findContextualValueDeserializer(vt, property);
        } else { // if directly assigned, probably not yet contextual, so:
            valueDeser = ctxt.handleSecondaryContextualization(valueDeser, property, vt);
        }
        TypeDeserializer vtd = _valueTypeDeserializer;
        if (vtd != null) {
            vtd = vtd.forProperty(property);
        }
        Set<String> ignored = _ignorableProperties;
        Set<String> included = _includableProperties;
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (_neitherNull(intr, property)) {
            AnnotatedMember member = property.getMember();
            if (member != null) {
                final DeserializationConfig config = ctxt.getConfig();
                JsonIgnoreProperties.Value ignorals = intr.findPropertyIgnoralByName(config, member);
                if (ignorals != null) {
                    Set<String> ignoresToAdd = ignorals.findIgnoredForDeserialization();
                    if (!ignoresToAdd.isEmpty()) {
                        ignored = (ignored == null) ? new HashSet<String>() : new HashSet<String>(ignored);
                        for (String str : ignoresToAdd) {
                            ignored.add(str);
                        }
                    }
                }
                JsonIncludeProperties.Value inclusions = intr.findPropertyInclusionByName(config, member);
                if (inclusions != null) {
                    Set<String> includedToAdd = inclusions.getIncluded();
                    if (includedToAdd != null) {
                        Set<String> newIncluded = new HashSet<>();
                        if (included == null) {
                            newIncluded = new HashSet<>(includedToAdd);
                        } else {
                            for (String str : includedToAdd) {
                                if (included.contains(str)) {
                                    newIncluded.add(str);
                                }
                            }
                        }
                        included = newIncluded;
                    }
                }
            }
        }
        return withResolved(keyDeser, vtd, valueDeser,
                findContentNullProvider(ctxt, property, valueDeser), ignored, included);
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

    /**
     * Turns out that these are expensive enough to create so that caching
     * does make sense.
     *<p>
     * IMPORTANT: but, note, that instances CAN NOT BE CACHED if there is
     * a value type deserializer; this caused an issue with 2.4.4 of
     * JAXB Annotations (failing a test).
     * It is also possible that some other settings could make deserializers
     * un-cacheable; but on the other hand, caching can make a big positive
     * difference with performance... so it's a hard choice.
     *
     * @since 2.4.4
     */
    @Override
    public boolean isCachable() {
        // As per [databind#735], existence of value or key deserializer (only passed
        // if annotated to use non-standard one) should also prevent caching.
        return (_valueDeserializer == null)
                && (_keyDeserializer == null)
                && (_valueTypeDeserializer == null)
                && (_ignorableProperties == null)
                && (_includableProperties == null);
    }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Map;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Object,Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        if (_propertyBasedCreator != null) {
            return _deserializeUsingCreator(p, ctxt);
        }
        if (_delegateDeserializer != null) {
            return (Map<Object,Object>) _valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(p, ctxt));
        }
        if (!_hasDefaultCreator) {
            return (Map<Object,Object> ) ctxt.handleMissingInstantiator(getMapClass(),
                    getValueInstantiator(), p,
                    "no default constructor found");
        }
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_START_OBJECT:
        case JsonTokenId.ID_END_OBJECT:
        case JsonTokenId.ID_FIELD_NAME:
            final Map<Object,Object> result = (Map<Object,Object>) _valueInstantiator.createUsingDefault(ctxt);
            if (_standardStringKey) {
                return _readAndBindStringKeyMap(p, ctxt, result);
            }
            return _readAndBind(p, ctxt, result);
        case JsonTokenId.ID_STRING:
            // (empty) String may be ok however; or single-String-arg ctor
            return _deserializeFromString(p, ctxt);
        case JsonTokenId.ID_START_ARRAY:
            // Empty array, or single-value wrapped in array?
            return _deserializeFromArray(p, ctxt);
        default:
        }
        return (Map<Object,Object>) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Object,Object> deserialize(JsonParser p, DeserializationContext ctxt,
            Map<Object,Object> result)
        throws IOException
    {
        // [databind#631]: Assign current value, to be accessible by custom deserializers
        p.setCurrentValue(result);

        // Ok: must point to START_OBJECT or FIELD_NAME
        JsonToken t = p.currentToken();
        if (t != JsonToken.START_OBJECT && t != JsonToken.FIELD_NAME) {
            return (Map<Object,Object>) ctxt.handleUnexpectedToken(getMapClass(), p);
        }
        // 21-Apr-2017, tatu: Need separate methods to do proper merging
        if (_standardStringKey) {
            _readAndUpdateStringKeyMap(p, ctxt, result);
            return result;
        }
        _readAndUpdate(p, ctxt, result);
        return result;
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException
    {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromObject(p, ctxt);
    }

    /*
    /**********************************************************
    /* Other public accessors
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public final Class<?> getMapClass() { return (Class<Map<Object,Object>>) _containerType.getRawClass(); }

    @Override public JavaType getValueType() { return _containerType; }

    /*
    /**********************************************************
    /* Internal methods, non-merging deserialization
    /**********************************************************
     */

    protected final Map<Object,Object> _readAndBind(JsonParser p, DeserializationContext ctxt,
            Map<Object,Object> result) throws IOException
    {
        final KeyDeserializer keyDes = _keyDeserializer;
        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        MapReferringAccumulator referringAccumulator = null;
        boolean useObjectId = valueDes.getObjectIdReader() != null;
        if (useObjectId) {
            referringAccumulator = new MapReferringAccumulator(_containerType.getContentType().getRawClass(),
                    result);
        }

        String keyStr;
        if (p.isExpectedStartObjectToken()) {
            keyStr = p.nextFieldName();
        } else {
            JsonToken t = p.currentToken();
            if (t != JsonToken.FIELD_NAME) {
                if (t == JsonToken.END_OBJECT) {
                    return result;
                }
                ctxt.reportWrongTokenException(this, JsonToken.FIELD_NAME, null);
            }
            keyStr = p.currentName();
        }

        for (; keyStr != null; keyStr = p.nextFieldName()) {
            Object key = keyDes.deserializeKey(keyStr, ctxt);
            // And then the value...
            JsonToken t = p.nextToken();
            if ((_inclusionChecker != null) && _inclusionChecker.shouldIgnore(keyStr)) {
                p.skipChildren();
                continue;
            }
            try {
                // Note: must handle null explicitly here; value deserializers won't
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
                if (useObjectId) {
                    referringAccumulator.put(key, value);
                } else {
                    Object oldValue = result.put(key, value);
                    if (oldValue != null) {
                        _squashDups(ctxt, result, key, oldValue, value);
                    }
                }
            } catch (UnresolvedForwardReference reference) {
                handleUnresolvedReference(ctxt, referringAccumulator, key, reference);
            } catch (Exception e) {
                wrapAndThrow(ctxt, e, result, keyStr);
            }
        }
        return result;
    }

    /**
     * Optimized method used when keys can be deserialized as plain old
     * {@link java.lang.String}s, and there is no custom deserialized
     * specified.
     */
    protected final Map<Object,Object> _readAndBindStringKeyMap(JsonParser p, DeserializationContext ctxt,
            Map<Object,Object> result) throws IOException
    {
        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;
        MapReferringAccumulator referringAccumulator = null;
        boolean useObjectId = (valueDes.getObjectIdReader() != null);
        if (useObjectId) {
            referringAccumulator = new MapReferringAccumulator(_containerType.getContentType().getRawClass(), result);
        }

        String key;
        if (p.isExpectedStartObjectToken()) {
            key = p.nextFieldName();
        } else {
            JsonToken t = p.currentToken();
            if (t == JsonToken.END_OBJECT) {
                return result;
            }
            if (t != JsonToken.FIELD_NAME) {
                ctxt.reportWrongTokenException(this, JsonToken.FIELD_NAME, null);
            }
            key = p.currentName();
        }

        for (; key != null; key = p.nextFieldName()) {
            JsonToken t = p.nextToken();
            if ((_inclusionChecker != null) && _inclusionChecker.shouldIgnore(key)) {
                p.skipChildren();
                continue;
            }
            try {
                // Note: must handle null explicitly here; value deserializers won't
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
                if (useObjectId) {
                    referringAccumulator.put(key, value);
                } else {
                    Object oldValue = result.put(key, value);
                    if (oldValue != null) {
                        _squashDups(ctxt, result, key, oldValue, value);
                    }
                }
            } catch (UnresolvedForwardReference reference) {
                handleUnresolvedReference(ctxt, referringAccumulator, key, reference);
            } catch (Exception e) {
                wrapAndThrow(ctxt, e, result, key);
            }
        }
        // 23-Mar-2015, tatu: TODO: verify we got END_OBJECT?

        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<Object,Object> _deserializeUsingCreator(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        // null -> no ObjectIdReader for Maps (yet?)
        PropertyValueBuffer buffer = creator.startBuilding(p, ctxt, null);

        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        String key;
        if (p.isExpectedStartObjectToken()) {
            key = p.nextFieldName();
        } else if (p.hasToken(JsonToken.FIELD_NAME)) {
            key = p.currentName();
        } else {
            key = null;
        }

        for (; key != null; key = p.nextFieldName()) {
            JsonToken t = p.nextToken(); // to get to value
            if ((_inclusionChecker != null) && _inclusionChecker.shouldIgnore(key)) {
                p.skipChildren(); // and skip it (in case of array/object)
                continue;
            }
            // creator property?
            SettableBeanProperty prop = creator.findCreatorProperty(key);
            if (prop != null) {
                // Last property to set?
                if (buffer.assignParameter(prop, prop.deserialize(p, ctxt))) {
                    p.nextToken(); // from value to END_OBJECT or FIELD_NAME
                    Map<Object,Object> result;
                    try {
                        result = (Map<Object,Object>)creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        return wrapAndThrow(ctxt, e, _containerType.getRawClass(), key);
                    }
                    return _readAndBind(p, ctxt, result);
                }
                continue;
            }
            // other property? needs buffering
            Object actualKey = _keyDeserializer.deserializeKey(key, ctxt);
            Object value;

            try {
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
            } catch (Exception e) {
                wrapAndThrow(ctxt, e, _containerType.getRawClass(), key);
                return null;
            }
            buffer.bufferMapProperty(actualKey, value);
        }
        // end of JSON object?
        // if so, can just construct and leave...
        try {
            return (Map<Object,Object>)creator.build(ctxt, buffer);
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, _containerType.getRawClass(), key);
            return null;
        }
    }

    /*
    /**********************************************************
    /* Internal methods, non-merging deserialization
    /**********************************************************
     */

    /**
     * @since 2.9
     */
    protected final void _readAndUpdate(JsonParser p, DeserializationContext ctxt,
            Map<Object,Object> result) throws IOException
    {
        final KeyDeserializer keyDes = _keyDeserializer;
        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        // Note: assumption is that Object Id handling can't really work with merging
        // and thereby we can (and should) just drop that part

        String keyStr;
        if (p.isExpectedStartObjectToken()) {
            keyStr = p.nextFieldName();
        } else {
            JsonToken t = p.currentToken();
            if (t == JsonToken.END_OBJECT) {
                return;
            }
            if (t != JsonToken.FIELD_NAME) {
                ctxt.reportWrongTokenException(this, JsonToken.FIELD_NAME, null);
            }
            keyStr = p.currentName();
        }

        for (; keyStr != null; keyStr = p.nextFieldName()) {
            Object key = keyDes.deserializeKey(keyStr, ctxt);
            // And then the value...
            JsonToken t = p.nextToken();
            if ((_inclusionChecker != null) && _inclusionChecker.shouldIgnore(keyStr)) {
                p.skipChildren();
                continue;
            }
            try {
                // Note: must handle null explicitly here, can't merge etc
                if (t == JsonToken.VALUE_NULL) {
                    if (_skipNullValues) {
                        continue;
                    }
                    result.put(key, _nullProvider.getNullValue(ctxt));
                    continue;
                }
                Object old = result.get(key);
                Object value;
                if (old != null) {
                    if (typeDeser == null) {
                        value = valueDes.deserialize(p, ctxt, old);
                    } else {
                        value = valueDes.deserializeWithType(p, ctxt, typeDeser, old);
                    }
                } else if (typeDeser == null) {
                    value = valueDes.deserialize(p, ctxt);
                } else {
                    value = valueDes.deserializeWithType(p, ctxt, typeDeser);
                }
                if (value != old) {
                    result.put(key, value);
                }
            } catch (Exception e) {
                wrapAndThrow(ctxt, e, result, keyStr);
            }
        }
    }

    /**
     * Optimized method used when keys can be deserialized as plain old
     * {@link java.lang.String}s, and there is no custom deserializer
     * specified.
     *
     * @since 2.9
     */
    protected final void _readAndUpdateStringKeyMap(JsonParser p, DeserializationContext ctxt,
            Map<Object,Object> result) throws IOException
    {
        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        // Note: assumption is that Object Id handling can't really work with merging
        // and thereby we can (and should) just drop that part

        String key;
        if (p.isExpectedStartObjectToken()) {
            key = p.nextFieldName();
        } else {
            JsonToken t = p.currentToken();
            if (t == JsonToken.END_OBJECT) {
                return;
            }
            if (t != JsonToken.FIELD_NAME) {
                ctxt.reportWrongTokenException(this, JsonToken.FIELD_NAME, null);
            }
            key = p.currentName();
        }

        for (; key != null; key = p.nextFieldName()) {
            JsonToken t = p.nextToken();
            if ((_inclusionChecker != null) && _inclusionChecker.shouldIgnore(key)) {
                p.skipChildren();
                continue;
            }
            try {
                // Note: must handle null explicitly here, can't merge etc
                if (t == JsonToken.VALUE_NULL) {
                    if (_skipNullValues) {
                        continue;
                    }
                    result.put(key, _nullProvider.getNullValue(ctxt));
                    continue;
                }
                Object old = result.get(key);
                Object value;
                if (old != null) {
                    if (typeDeser == null) {
                        value = valueDes.deserialize(p, ctxt, old);
                    } else {
                        value = valueDes.deserializeWithType(p, ctxt, typeDeser, old);
                    }
                } else if (typeDeser == null) {
                    value = valueDes.deserialize(p, ctxt);
                } else {
                    value = valueDes.deserializeWithType(p, ctxt, typeDeser);
                }
                if (value != old) {
                    result.put(key, value);
                }
            } catch (Exception e) {
                wrapAndThrow(ctxt, e, result, key);
            }
        }
    }

    /**
     * @since 2.14
     */
    @SuppressWarnings("unchecked")
    protected void _squashDups(final DeserializationContext ctxt,
            final Map<Object, Object> result,
            final Object key, final Object oldValue, final Object newValue)
    {
        if (_checkDupSquash && ctxt.isEnabled(StreamReadCapability.DUPLICATE_PROPERTIES)) {
            if (oldValue instanceof List<?>) {
                ((List<Object>) oldValue).add(newValue);
                result.put(key, oldValue);
            } else {
                ArrayList<Object> l = new ArrayList<>();
                l.add(oldValue);
                l.add(newValue);
                result.put(key, l);
            }
        }
    }

    /*
    /**********************************************************
    /* Internal methods, other
    /**********************************************************
     */

    private void handleUnresolvedReference(DeserializationContext ctxt,
            MapReferringAccumulator accumulator,
            Object key, UnresolvedForwardReference reference)
        throws JsonMappingException
    {
        if (accumulator == null) {
            ctxt.reportInputMismatch(this,
                    "Unresolved forward reference but no identity info: "+reference);
        }
        Referring referring = accumulator.handleUnresolvedReference(reference, key); // lgtm [java/dereferenced-value-may-be-null]
        reference.getRoid().appendReferring(referring);
    }

    private final static class MapReferringAccumulator {
        private final Class<?> _valueType;
        private Map<Object,Object> _result;
        /**
         * A list of {@link MapReferring} to maintain ordering.
         */
        private List<MapReferring> _accumulator = new ArrayList<MapReferring>();

        public MapReferringAccumulator(Class<?> valueType, Map<Object, Object> result) {
            _valueType = valueType;
            _result = result;
        }

        public void put(Object key, Object value)
        {
            if (_accumulator.isEmpty()) {
                _result.put(key, value);
            } else {
                MapReferring ref = _accumulator.get(_accumulator.size() - 1);
                ref.next.put(key, value);
            }
        }

        public Referring handleUnresolvedReference(UnresolvedForwardReference reference, Object key)
        {
            MapReferring id = new MapReferring(this, reference, _valueType, key);
            _accumulator.add(id);
            return id;
        }

        public void resolveForwardReference(Object id, Object value) throws IOException
        {
            Iterator<MapReferring> iterator = _accumulator.iterator();
            // Resolve ordering after resolution of an id. This means either:
            // 1- adding to the result map in case of the first unresolved id.
            // 2- merge the content of the resolved id with its previous unresolved id.
            Map<Object,Object> previous = _result;
            while (iterator.hasNext()) {
                MapReferring ref = iterator.next();
                if (ref.hasId(id)) {
                    iterator.remove();
                    previous.put(ref.key, value);
                    previous.putAll(ref.next);
                    return;
                }
                previous = ref.next;
            }

            throw new IllegalArgumentException("Trying to resolve a forward reference with id [" + id
                    + "] that wasn't previously seen as unresolved.");
        }
    }

    /**
     * Helper class to maintain processing order of value.
     * The resolved object associated with {@link #key} comes before the values in
     * {@link #next}.
     */
    static class MapReferring extends Referring {
        private final MapReferringAccumulator _parent;

        public final Map<Object, Object> next = new LinkedHashMap<Object, Object>();
        public final Object key;

        MapReferring(MapReferringAccumulator parent, UnresolvedForwardReference ref,
                Class<?> valueType, Object key)
        {
            super(ref, valueType);
            _parent = parent;
            this.key = key;
        }

        @Override
        public void handleResolvedForwardReference(Object id, Object value) throws IOException {
            _parent.resolveForwardReference(id, value);
        }
    }
}
