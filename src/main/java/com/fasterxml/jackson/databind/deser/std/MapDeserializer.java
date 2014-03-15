package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.impl.PropertyBasedCreator;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId.Referring;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.ArrayBuilders;

/**
 * Basic serializer that can take Json "Object" structure and
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
    private static final long serialVersionUID = -3378654289961736240L;

    // // Configuration: typing, deserializers

    protected final JavaType _mapType;

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

    protected final boolean _hasDefaultCreator;

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

    // // Any properties to ignore if seen?
    
    protected HashSet<String> _ignorableProperties;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public MapDeserializer(JavaType mapType, ValueInstantiator valueInstantiator,
            KeyDeserializer keyDeser, JsonDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser)
    {
        super(mapType);
        _mapType = mapType;
        _keyDeserializer = keyDeser;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
        _valueInstantiator = valueInstantiator;
        _hasDefaultCreator = valueInstantiator.canCreateUsingDefault();
        _delegateDeserializer = null;
        _propertyBasedCreator = null;
        _standardStringKey = _isStdKeyDeser(mapType, keyDeser);
    }

    /**
     * Copy-constructor that can be used by sub-classes to allow
     * copy-on-write styling copying of settings of an existing instance.
     */
    protected MapDeserializer(MapDeserializer src)
    {
        super(src._mapType);
        _mapType = src._mapType;
        _keyDeserializer = src._keyDeserializer;
        _valueDeserializer = src._valueDeserializer;
        _valueTypeDeserializer = src._valueTypeDeserializer;
        _valueInstantiator = src._valueInstantiator;
        _propertyBasedCreator = src._propertyBasedCreator;
        _delegateDeserializer = src._delegateDeserializer;
        _hasDefaultCreator = src._hasDefaultCreator;
        // should we make a copy here?
        _ignorableProperties = src._ignorableProperties;

        _standardStringKey = src._standardStringKey;
    }

    protected MapDeserializer(MapDeserializer src,
            KeyDeserializer keyDeser, JsonDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser,
            HashSet<String> ignorable)
    {
        super(src._mapType);
        _mapType = src._mapType;
        _keyDeserializer = keyDeser;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
        _valueInstantiator = src._valueInstantiator;
        _propertyBasedCreator = src._propertyBasedCreator;
        _delegateDeserializer = src._delegateDeserializer;
        _hasDefaultCreator = src._hasDefaultCreator;
        _ignorableProperties = ignorable;

        _standardStringKey = _isStdKeyDeser(_mapType, keyDeser);
    }

    /**
     * Fluent factory method used to create a copy with slightly
     * different settings. When sub-classing, MUST be overridden.
     */
    @SuppressWarnings("unchecked")
    protected MapDeserializer withResolved(KeyDeserializer keyDeser,
            TypeDeserializer valueTypeDeser, JsonDeserializer<?> valueDeser,
            HashSet<String> ignorable)
    {
        
        if ((_keyDeserializer == keyDeser) && (_valueDeserializer == valueDeser)
                && (_valueTypeDeserializer == valueTypeDeser) && (_ignorableProperties == ignorable)) {
            return this;
        }
        return new MapDeserializer(this,
                keyDeser, (JsonDeserializer<Object>) valueDeser, valueTypeDeser, ignorable);
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
    
    public void setIgnorableProperties(String[] ignorable) {
        _ignorableProperties = (ignorable == null || ignorable.length == 0) ?
            null : ArrayBuilders.arrayToSet(ignorable);
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
                throw new IllegalArgumentException("Invalid delegate-creator definition for "+_mapType
                        +": value instantiator ("+_valueInstantiator.getClass().getName()
                        +") returned true for 'canCreateUsingDelegate()', but null for 'getDelegateType()'");
            }
            /* Theoretically should be able to get CreatorProperty for delegate
             * parameter to pass; but things get tricky because DelegateCreator
             * may contain injectable values. So, for now, let's pass nothing.
             */
            _delegateDeserializer = findDeserializer(ctxt, delegateType, null);
        }
        if (_valueInstantiator.canCreateFromObjectWith()) {
            SettableBeanProperty[] creatorProps = _valueInstantiator.getFromObjectArguments(ctxt.getConfig());
            _propertyBasedCreator = PropertyBasedCreator.construct(ctxt, _valueInstantiator, creatorProps);
        }
        _standardStringKey = _isStdKeyDeser(_mapType, _keyDeserializer);
    }

    /**
     * Method called to finalize setup of this deserializer,
     * when it is known for which property deserializer is needed for.
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        KeyDeserializer kd = _keyDeserializer;
        if (kd == null) {
            kd = ctxt.findKeyDeserializer(_mapType.getKeyType(), property);
        } else {
            if (kd instanceof ContextualKeyDeserializer) {
                kd = ((ContextualKeyDeserializer) kd).createContextual(ctxt, property);
            }
        }
        JsonDeserializer<?> vd = _valueDeserializer;
        // #125: May have a content converter
        vd = findConvertingContentDeserializer(ctxt, property, vd);
        if (vd == null) {
            vd = ctxt.findContextualValueDeserializer(_mapType.getContentType(), property);
        } else { // if directly assigned, probably not yet contextual, so:
            vd = ctxt.handleSecondaryContextualization(vd, property);
        }
        TypeDeserializer vtd = _valueTypeDeserializer;
        if (vtd != null) {
            vtd = vtd.forProperty(property);
        }
        HashSet<String> ignored = _ignorableProperties;
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr != null && property != null) {
            String[] moreToIgnore = intr.findPropertiesToIgnore(property.getMember());
            if (moreToIgnore != null) {
                ignored = (ignored == null) ? new HashSet<String>() : new HashSet<String>(ignored);
                for (String str : moreToIgnore) {
                    ignored.add(str);
                }
            }
        }
        return withResolved(kd, vtd, vd, ignored);
    }
    
    /*
    /**********************************************************
    /* ContainerDeserializerBase API
    /**********************************************************
     */

    @Override
    public JavaType getContentType() {
        return _mapType.getContentType();
    }

    @Override
    public JsonDeserializer<Object> getContentDeserializer() {
        return _valueDeserializer;
    }
    
    /*
    /**********************************************************
    /* JsonDeserializer API
    /**********************************************************
     */

    @Override
    @SuppressWarnings("unchecked")
    public Map<Object,Object> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (_propertyBasedCreator != null) {
            return _deserializeUsingCreator(jp, ctxt);
        }
        if (_delegateDeserializer != null) {
            return (Map<Object,Object>) _valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(jp, ctxt));
        }
        if (!_hasDefaultCreator) {
            throw ctxt.instantiationException(getMapClass(), "No default constructor found");
        }
        // Ok: must point to START_OBJECT, FIELD_NAME or END_OBJECT
        JsonToken t = jp.getCurrentToken();
        if (t != JsonToken.START_OBJECT && t != JsonToken.FIELD_NAME && t != JsonToken.END_OBJECT) {
            // [JACKSON-620] (empty) String may be ok however:
            if (t == JsonToken.VALUE_STRING) {
                return (Map<Object,Object>) _valueInstantiator.createFromString(ctxt, jp.getText());
            }
            throw ctxt.mappingException(getMapClass());
        }
        final Map<Object,Object> result = (Map<Object,Object>) _valueInstantiator.createUsingDefault(ctxt);
        if (_standardStringKey) {
            _readAndBindStringMap(jp, ctxt, result);
            return result;
        }
        _readAndBind(jp, ctxt, result);
        return result;
    }

    @Override
    public Map<Object,Object> deserialize(JsonParser jp, DeserializationContext ctxt,
            Map<Object,Object> result)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_OBJECT or FIELD_NAME
        JsonToken t = jp.getCurrentToken();
        if (t != JsonToken.START_OBJECT && t != JsonToken.FIELD_NAME) {
            throw ctxt.mappingException(getMapClass());
        }
        if (_standardStringKey) {
            _readAndBindStringMap(jp, ctxt, result);
            return result;
        }
        _readAndBind(jp, ctxt, result);
        return result;
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromObject(jp, ctxt);
    }
    
    /*
    /**********************************************************
    /* Other public accessors
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public final Class<?> getMapClass() { return (Class<Map<Object,Object>>) _mapType.getRawClass(); }

    @Override public JavaType getValueType() { return _mapType; }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected final void _readAndBind(JsonParser jp, DeserializationContext ctxt,
            Map<Object,Object> result)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        final KeyDeserializer keyDes = _keyDeserializer;
        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        MapReferringAccumulator referringAccumulator = null;
        boolean useObjectId = valueDes.getObjectIdReader() != null;
        if (useObjectId) {
            referringAccumulator = new MapReferringAccumulator(_mapType.getContentType().getRawClass(), result);
        }
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            // Must point to field name
            String fieldName = jp.getCurrentName();
            Object key = keyDes.deserializeKey(fieldName, ctxt);
            // And then the value...
            t = jp.nextToken();
            if (_ignorableProperties != null && _ignorableProperties.contains(fieldName)) {
                jp.skipChildren();
                continue;
            }
            try{
                // Note: must handle null explicitly here; value deserializers won't
                Object value;
                if (t == JsonToken.VALUE_NULL) {
                    value = valueDes.getNullValue();
                } else if (typeDeser == null) {
                    value = valueDes.deserialize(jp, ctxt);
                } else {
                    value = valueDes.deserializeWithType(jp, ctxt, typeDeser);
                }
                /* !!! 23-Dec-2008, tatu: should there be an option to verify
                 *   that there are no duplicate field names? (and/or what
                 *   to do, keep-first or keep-last)
                 */
                if (useObjectId) {
                    referringAccumulator.put(key, value);
                } else {
                    result.put(key, value);
                }
            } catch(UnresolvedForwardReference reference) {
                handleUnresolvedReference(jp, referringAccumulator, key, reference);
            }
        }
    }

    /**
     * Optimized method used when keys can be deserialized as plain old
     * {@link java.lang.String}s, and there is no custom deserialized
     * specified.
     */
    protected final void _readAndBindStringMap(JsonParser jp, DeserializationContext ctxt,
            Map<Object,Object> result)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;
        MapReferringAccumulator referringAccumulator = null;
        boolean useObjectId = valueDes.getObjectIdReader() != null;
        if (useObjectId) {
            referringAccumulator = new MapReferringAccumulator(_mapType.getContentType().getRawClass(), result);
        }
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            // Must point to field name
            String fieldName = jp.getCurrentName();
            // And then the value...
            t = jp.nextToken();
            if (_ignorableProperties != null && _ignorableProperties.contains(fieldName)) {
                jp.skipChildren();
                continue;
            }
            try {
                // Note: must handle null explicitly here; value deserializers won't
                Object value;
                if (t == JsonToken.VALUE_NULL) {
                    value = valueDes.getNullValue();
                } else if (typeDeser == null) {
                    value = valueDes.deserialize(jp, ctxt);
                } else {
                    value = valueDes.deserializeWithType(jp, ctxt, typeDeser);
                }
                if (useObjectId) {
                    referringAccumulator.put(fieldName, value);
                } else {
                    result.put(fieldName, value);
                }
            } catch (UnresolvedForwardReference reference) {
                handleUnresolvedReference(jp, referringAccumulator, fieldName, reference);
            }
        }
    }
    
    @SuppressWarnings("unchecked") 
    public Map<Object,Object> _deserializeUsingCreator(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        // null -> no ObjectIdReader for Maps (yet?)
        PropertyValueBuffer buffer = creator.startBuilding(jp, ctxt, null);

        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        final JsonDeserializer<Object> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            t = jp.nextToken(); // to get to value
            if (_ignorableProperties != null && _ignorableProperties.contains(propName)) {
                jp.skipChildren(); // and skip it (in case of array/object)
                continue;
            }
            // creator property?
            SettableBeanProperty prop = creator.findCreatorProperty(propName);
            if (prop != null) {
                // Last property to set?
                Object value = prop.deserialize(jp, ctxt);
                if (buffer.assignParameter(prop.getCreatorIndex(), value)) {
                    jp.nextToken();
                    Map<Object,Object> result;
                    try {
                        result = (Map<Object,Object>)creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        wrapAndThrow(e, _mapType.getRawClass());
                        return null;
                    }
                    _readAndBind(jp, ctxt, result);
                    return result;
                }
                continue;
            }
            // other property? needs buffering
            String fieldName = jp.getCurrentName();
            Object key = _keyDeserializer.deserializeKey(fieldName, ctxt);
            Object value;            
            if (t == JsonToken.VALUE_NULL) {
                value = valueDes.getNullValue();
            } else if (typeDeser == null) {
                value = valueDes.deserialize(jp, ctxt);
            } else {
                value = valueDes.deserializeWithType(jp, ctxt, typeDeser);
            }
            buffer.bufferMapProperty(key, value);
        }
        // end of JSON object?
        // if so, can just construct and leave...
        try {
            return (Map<Object,Object>)creator.build(ctxt, buffer);
        } catch (Exception e) {
            wrapAndThrow(e, _mapType.getRawClass());
            return null;
        }
    }

    // note: copied from BeanDeserializer; should try to share somehow...
    protected void wrapAndThrow(Throwable t, Object ref)
        throws IOException
    {
        // to handle StackOverflow:
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as is
        if (t instanceof Error) {
            throw (Error) t;
        }
        // ... except for mapping exceptions
        if (t instanceof IOException && !(t instanceof JsonMappingException)) {
            throw (IOException) t;
        }
        throw JsonMappingException.wrapWithPath(t, ref, null);
    }

    private void handleUnresolvedReference(JsonParser jp, MapReferringAccumulator accumulator, Object key,
            UnresolvedForwardReference reference)
        throws JsonMappingException
    {
        if (accumulator == null) {
            throw JsonMappingException.from(jp, "Unresolved forward reference but no identity info.", reference);
        }
        Referring referring = accumulator.handleUnresolvedReference(reference, key);
        reference.getRoid().appendReferring(referring);
    }

    private final static class MapReferringAccumulator  {
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
     * Helper class to maintain processing order of value. The resolved
     * object associated with {@link #_id} comes before the values in
     * {@link _next}.
     */
    private final static class MapReferring extends Referring {
        private final MapReferringAccumulator _parent;

        public final Map<Object, Object> next = new LinkedHashMap<Object, Object>();
        public final Object key;
        
        private MapReferring(MapReferringAccumulator parent, UnresolvedForwardReference ref,
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
