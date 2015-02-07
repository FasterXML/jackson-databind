package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonMapFormatVisitor;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ArrayBuilders;

/**
 * Standard serializer implementation for serializing {link java.util.Map} types.
 *<p>
 * Note: about the only configurable setting currently is ability to filter out
 * entries with specified names.
 */
@JacksonStdImpl
public class MapSerializer
    extends ContainerSerializer<Map<?,?>>
    implements ContextualSerializer
{
    private static final long serialVersionUID = -3465193297149553004L;

    protected final static JavaType UNSPECIFIED_TYPE = TypeFactory.unknownType();

    /**
     * Map-valued property being serialized with this instance
     */
    protected final BeanProperty _property;

    /**
     * Set of entries to omit during serialization, if any
     */
    protected final HashSet<String> _ignoredEntries;

    /**
     * Whether static types should be used for serialization of values
     * or not (if not, dynamic runtime type is used)
     */
    protected final boolean _valueTypeIsStatic;

    /**
     * Declared type of keys
     */
    protected final JavaType _keyType;

    /**
     * Declared type of contained values
     */
    protected final JavaType _valueType;

    /**
     * Key serializer to use, if it can be statically determined
     */
    protected JsonSerializer<Object> _keySerializer;

    /**
     * Value serializer to use, if it can be statically determined
     */
    protected JsonSerializer<Object> _valueSerializer;

    /**
     * Type identifier serializer used for values, if any.
     */
    protected final TypeSerializer _valueTypeSerializer;

    /**
     * If value type can not be statically determined, mapping from
     * runtime value types to serializers are stored in this object.
     */
    protected PropertySerializerMap _dynamicValueSerializers;

    /**
     * Id of the property filter to use, if any; null if none.
     *
     * @since 2.3
     */
    protected final Object _filterId;

    /**
     * Flag set if output is forced to be sorted by keys (usually due
     * to annotation).
     * 
     * @since 2.4
     */
    protected final boolean _sortKeys;

    /**
     * Value that indicates suppression mechanism to use; either one of
     * values of {@link com.fasterxml.jackson.annotation.JsonInclude.Include}, or actual object to compare
     * against ("default value")
     * 
     * @since 2.5
     */
    protected final Object _suppressableValue;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    /**
     * @since 2.5
     */
    @SuppressWarnings("unchecked")
    protected MapSerializer(HashSet<String> ignoredEntries,
            JavaType keyType, JavaType valueType, boolean valueTypeIsStatic,
            TypeSerializer vts,
            JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer)
    {
        super(Map.class, false);
        _ignoredEntries = ignoredEntries;
        _keyType = keyType;
        _valueType = valueType;
        _valueTypeIsStatic = valueTypeIsStatic;
        _valueTypeSerializer = vts;
        _keySerializer = (JsonSerializer<Object>) keySerializer;
        _valueSerializer = (JsonSerializer<Object>) valueSerializer;
        _dynamicValueSerializers = PropertySerializerMap.emptyForProperties();
        _property = null;
        _filterId = null;
        _sortKeys = false;
        _suppressableValue = null;
    }

    /**
     * @since 2.5
     */
    protected void _ensureOverride() {
        if (getClass() != MapSerializer.class) {
            throw new IllegalStateException("Missing override in class "+getClass().getName());
        }
    }
    
    @SuppressWarnings("unchecked")
    protected MapSerializer(MapSerializer src, BeanProperty property,
            JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer,
            HashSet<String> ignored)
    {
        super(Map.class, false);
        _ignoredEntries = ignored;
        _keyType = src._keyType;
        _valueType = src._valueType;
        _valueTypeIsStatic = src._valueTypeIsStatic;
        _valueTypeSerializer = src._valueTypeSerializer;
        _keySerializer = (JsonSerializer<Object>) keySerializer;
        _valueSerializer = (JsonSerializer<Object>) valueSerializer;
        _dynamicValueSerializers = src._dynamicValueSerializers;
        _property = property;
        _filterId = src._filterId;
        _sortKeys = src._sortKeys;
        _suppressableValue = src._suppressableValue;
    }

    @Deprecated // since 2.5
    protected MapSerializer(MapSerializer src, TypeSerializer vts) {
        this(src, vts, src._suppressableValue);
    }

    /**
     * @since 2.5
     */
    protected MapSerializer(MapSerializer src, TypeSerializer vts,
            Object suppressableValue)
    {
        super(Map.class, false);
        _ignoredEntries = src._ignoredEntries;
        _keyType = src._keyType;
        _valueType = src._valueType;
        _valueTypeIsStatic = src._valueTypeIsStatic;
        _valueTypeSerializer = vts;
        _keySerializer = src._keySerializer;
        _valueSerializer = src._valueSerializer;
        _dynamicValueSerializers = src._dynamicValueSerializers;
        _property = src._property;
        _filterId = src._filterId;
        _sortKeys = src._sortKeys;
        _suppressableValue = suppressableValue;
    }

    protected MapSerializer(MapSerializer src, Object filterId, boolean sortKeys)
    {
        super(Map.class, false);
        _ignoredEntries = src._ignoredEntries;
        _keyType = src._keyType;
        _valueType = src._valueType;
        _valueTypeIsStatic = src._valueTypeIsStatic;
        _valueTypeSerializer = src._valueTypeSerializer;
        _keySerializer = src._keySerializer;
        _valueSerializer = src._valueSerializer;
        _dynamicValueSerializers = src._dynamicValueSerializers;
        _property = src._property;
        _filterId = filterId;
        _sortKeys = sortKeys;
        _suppressableValue = src._suppressableValue;
    }

    @Override
    public MapSerializer _withValueTypeSerializer(TypeSerializer vts) {
        if (_valueTypeSerializer == vts) {
            return this;
        }
        _ensureOverride();
        return new MapSerializer(this, vts, null);
    }

    /**
     * @since 2.4
     */
    public MapSerializer withResolved(BeanProperty property,
            JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer,
            HashSet<String> ignored, boolean sortKeys)
    {
        _ensureOverride();
        MapSerializer ser = new MapSerializer(this, property, keySerializer, valueSerializer, ignored);
        if (sortKeys != ser._sortKeys) {
            ser = new MapSerializer(ser, _filterId, sortKeys);
        }
        return ser;
    }

    /**
     * @since 2.3
     */
    public MapSerializer withFilterId(Object filterId) {
        if (_filterId == filterId) {
            return this;
        }
        _ensureOverride();
        return new MapSerializer(this, filterId, _sortKeys);
    }

    /**
     * Mutant factory for constructing an instance with different inclusion strategy
     * for content (Map values).
     * 
     * @since 2.5
     */
    public MapSerializer withContentInclusion(Object suppressableValue) {
        if (suppressableValue == _suppressableValue) {
            return this;
        }
        _ensureOverride();
        return new MapSerializer(this, _valueTypeSerializer, suppressableValue);
    }                
    
    /**
     * @since 2.3
     */
    public static MapSerializer construct(String[] ignoredList, JavaType mapType,
            boolean staticValueType, TypeSerializer vts,
            JsonSerializer<Object> keySerializer, JsonSerializer<Object> valueSerializer,
            Object filterId)
    {
        HashSet<String> ignoredEntries = (ignoredList == null || ignoredList.length == 0)
                ? null : ArrayBuilders.arrayToSet(ignoredList);

        JavaType keyType, valueType;
        
        if (mapType == null) {
            keyType = valueType = UNSPECIFIED_TYPE;
        } else { 
            keyType = mapType.getKeyType();
            valueType = mapType.getContentType();
        }
        // If value type is final, it's same as forcing static value typing:
        if (!staticValueType) {
            staticValueType = (valueType != null && valueType.isFinal());
        } else {
            // also: Object.class can not be handled as static, ever
            if (valueType.getRawClass() == Object.class) {
                staticValueType = false;
            }
        }
        MapSerializer ser = new MapSerializer(ignoredEntries, keyType, valueType, staticValueType, vts,
                keySerializer, valueSerializer);
        if (filterId != null) {
            ser = ser.withFilterId(filterId);
        }
        return ser;
    }

    /*
    /**********************************************************
    /* Post-processing (contextualization)
    /**********************************************************
     */

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property)
        throws JsonMappingException
    {
        /* 29-Sep-2012, tatu: Actually, we need to do much more contextual
         *    checking here since we finally know for sure the property,
         *    and it may have overrides
         */
        JsonSerializer<?> ser = null;
        JsonSerializer<?> keySer = null;
        final AnnotationIntrospector intr = provider.getAnnotationIntrospector();
        final AnnotatedMember propertyAcc = (property == null) ? null : property.getMember();
        Object suppressableValue = _suppressableValue;

        // First: if we have a property, may have property-annotation overrides
        if (propertyAcc != null && intr != null) {
            Object serDef = intr.findKeySerializer(propertyAcc);
            if (serDef != null) {
                keySer = provider.serializerInstance(propertyAcc, serDef);
            }
            serDef = intr.findContentSerializer(propertyAcc);
            if (serDef != null) {
                ser = provider.serializerInstance(propertyAcc, serDef);
            }
            JsonInclude.Include incl = intr.findSerializationInclusionForContent(propertyAcc, null);
            if (incl != null) {
                suppressableValue = incl;
            }
        }
        if (ser == null) {
            ser = _valueSerializer;
        }
        // [Issue#124]: May have a content converter
        ser = findConvertingContentSerializer(provider, property, ser);
        if (ser == null) {
            // 30-Sep-2012, tatu: One more thing -- if explicit content type is annotated,
            //   we can consider it a static case as well.
            // 20-Aug-2013, tatu: Need to avoid trying to access serializer for java.lang.Object tho
            if ((_valueTypeIsStatic && _valueType.getRawClass() != Object.class)
                    || hasContentTypeAnnotation(provider, property)) {
                ser = provider.findValueSerializer(_valueType, property);
            }
        } else {
            ser = provider.handleSecondaryContextualization(ser, property);
        }
        if (keySer == null) {
            keySer = _keySerializer;
        }
        if (keySer == null) {
            keySer = provider.findKeySerializer(_keyType, property);
        } else {
            keySer = provider.handleSecondaryContextualization(keySer, property);
        }
        HashSet<String> ignored = _ignoredEntries;
        boolean sortKeys = false;
        if (intr != null && propertyAcc != null) {
            String[] moreToIgnore = intr.findPropertiesToIgnore(propertyAcc);
            if (moreToIgnore != null) {
                ignored = (ignored == null) ? new HashSet<String>() : new HashSet<String>(ignored);
                for (String str : moreToIgnore) {
                    ignored.add(str);
                }
            }
            Boolean b = intr.findSerializationSortAlphabetically(propertyAcc);
            sortKeys = (b != null) && b.booleanValue();
        }
        MapSerializer mser = withResolved(property, keySer, ser, ignored, sortKeys);
        if (suppressableValue != _suppressableValue) {
            mser = mser.withContentInclusion(suppressableValue);
        }

        // [Issue#307]: allow filtering
        if (property != null) {
            AnnotatedMember m = property.getMember();
            if (m != null) {
                Object filterId = intr.findFilterId(m);
                if (filterId != null) {
                    mser = mser.withFilterId(filterId);
                }
            }
        }
        return mser;
    }
    
    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override
    public JavaType getContentType() {
        return _valueType;
    }

    @Override
    public JsonSerializer<?> getContentSerializer() {
        return _valueSerializer;
    }
    
    @Override
    public boolean isEmpty(SerializerProvider prov, Map<?,?> value) {
        return (value == null) || value.isEmpty();
    }

    @Override
    public boolean hasSingleElement(Map<?,?> value) {
        return (value.size() == 1);
    }
    
    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    /**
     * Accessor for currently assigned key serializer. Note that
     * this may return null during construction of <code>MapSerializer</code>:
     * depedencies are resolved during {@link #createContextual} method
     * (which can be overridden by custom implementations), but for some
     * dynamic types, it is possible that serializer is only resolved
     * during actual serialization.
     * 
     * @since 2.0
     */
    public JsonSerializer<?> getKeySerializer() {
        return _keySerializer;
    }
    
    /*
    /**********************************************************
    /* JsonSerializer implementation
    /**********************************************************
     */

    @Override
    public void serialize(Map<?,?> value, JsonGenerator gen, SerializerProvider provider)
        throws IOException
    {
        gen.writeStartObject();
        // [databind#631]: Assign current value, to be accessible by custom serializers
        gen.setCurrentValue(value);
        if (!value.isEmpty()) {
            Object suppressableValue = _suppressableValue;
            if (suppressableValue == null) {
                if (!provider.isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES)) {
                    suppressableValue = JsonInclude.Include.NON_NULL;
                }
            } else if (suppressableValue == JsonInclude.Include.ALWAYS) {
                suppressableValue = null;
            }
            if (_sortKeys || provider.isEnabled(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)) {
                value = _orderEntries(value);
            }
            if (_filterId != null) {
                serializeFilteredFields(value, gen, provider,
                        findPropertyFilter(provider, _filterId, value), suppressableValue);
            } else if (suppressableValue != null) {
                serializeOptionalFields(value, gen, provider, suppressableValue);
            } else if (_valueSerializer != null) {
                serializeFieldsUsing(value, gen, provider, _valueSerializer);
            } else {
                serializeFields(value, gen, provider);
            }
        }
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Map<?,?> value, JsonGenerator gen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException
    {
        typeSer.writeTypePrefixForObject(value, gen);
        // [databind#631]: Assign current value, to be accessible by custom serializers
        gen.setCurrentValue(value);
        if (!value.isEmpty()) {
            Object suppressableValue = _suppressableValue;
            if (suppressableValue == null) {
                if (!provider.isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES)) {
                    suppressableValue = JsonInclude.Include.NON_NULL;
                }
            } else if (suppressableValue == JsonInclude.Include.ALWAYS) {
                suppressableValue = null;
            }
            if (_sortKeys || provider.isEnabled(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)) {
                value = _orderEntries(value);
            }
            if (_filterId != null) {
                serializeFilteredFields(value, gen, provider,
                        findPropertyFilter(provider, _filterId, value), suppressableValue);
            } else if (suppressableValue != null) {
                serializeOptionalFields(value, gen, provider, suppressableValue);
            } else if (_valueSerializer != null) {
                serializeFieldsUsing(value, gen, provider, _valueSerializer);
            } else {
                serializeFields(value, gen, provider);
            }
        }
        typeSer.writeTypeSuffixForObject(value, gen);
    }

    /*
    /**********************************************************
    /* JsonSerializer implementation
    /**********************************************************
     */
    
    /**
     * Method called to serialize fields, when the value type is not statically known;
     * but we know that no value suppression is needed (which simplifies processing a bit)
     */
    public void serializeFields(Map<?,?> value, JsonGenerator gen, SerializerProvider provider)
        throws IOException
    {
        // If value type needs polymorphic type handling, some more work needed:
        if (_valueTypeSerializer != null) {
            serializeTypedFields(value, gen, provider, null);
            return;
        }
        final JsonSerializer<Object> keySerializer = _keySerializer;
        final HashSet<String> ignored = _ignoredEntries;

        PropertySerializerMap serializers = _dynamicValueSerializers;

        for (Map.Entry<?,?> entry : value.entrySet()) {
            Object valueElem = entry.getValue();
            // First, serialize key
            Object keyElem = entry.getKey();
            if (keyElem == null) {
                provider.findNullKeySerializer(_keyType, _property).serialize(null, gen, provider);
            } else {
                // One twist: is entry ignorable? If so, skip
                if (ignored != null && ignored.contains(keyElem)) continue;
                keySerializer.serialize(keyElem, gen, provider);
            }

            // And then value
            if (valueElem == null) {
                provider.defaultSerializeNull(gen);
            } else {
                Class<?> cc = valueElem.getClass();
                JsonSerializer<Object> serializer = serializers.serializerFor(cc);
                if (serializer == null) {
                    if (_valueType.hasGenericTypes()) {
                        serializer = _findAndAddDynamic(serializers,
                                provider.constructSpecializedType(_valueType, cc), provider);
                    } else {
                        serializer = _findAndAddDynamic(serializers, cc, provider);
                    }
                    serializers = _dynamicValueSerializers;
                }
                try {
                    serializer.serialize(valueElem, gen, provider);
                } catch (Exception e) {
                    // Add reference information
                    String keyDesc = ""+keyElem;
                    wrapAndThrow(provider, e, value, keyDesc);
                }
            }
        }
    }

    public void serializeOptionalFields(Map<?,?> value, JsonGenerator gen, SerializerProvider provider,
            Object suppressableValue)
        throws IOException
    {
        // If value type needs polymorphic type handling, some more work needed:
        if (_valueTypeSerializer != null) {
            serializeTypedFields(value, gen, provider, suppressableValue);
            return;
        }
        final HashSet<String> ignored = _ignoredEntries;
        PropertySerializerMap serializers = _dynamicValueSerializers;

        for (Map.Entry<?,?> entry : value.entrySet()) {
            // First find key serializer
            final Object keyElem = entry.getKey();
            JsonSerializer<Object> keySerializer;
            if (keyElem == null) {
                keySerializer = provider.findNullKeySerializer(_keyType, _property);
            } else {
                if (ignored != null && ignored.contains(keyElem)) continue;
                keySerializer = _keySerializer;
            }

            // Then value serializer
            final Object valueElem = entry.getValue();
            JsonSerializer<Object> valueSer;
            if (valueElem == null) {
                if (suppressableValue != null) { // all suppressions include null-suppression
                    continue;
                }
                valueSer = provider.getDefaultNullValueSerializer();
            } else {
                valueSer = _valueSerializer;
                if (valueSer == null) {
                    Class<?> cc = valueElem.getClass();
                    valueSer = serializers.serializerFor(cc);
                    if (valueSer == null) {
                        if (_valueType.hasGenericTypes()) {
                            valueSer = _findAndAddDynamic(serializers,
                                    provider.constructSpecializedType(_valueType, cc), provider);
                        } else {
                            valueSer = _findAndAddDynamic(serializers, cc, provider);
                        }
                        serializers = _dynamicValueSerializers;
                    }
                }
                // also may need to skip non-empty values:
                if ((suppressableValue == JsonInclude.Include.NON_EMPTY)
                        && valueSer.isEmpty(provider, valueElem)) {
                    continue;
                }
            }
            // and then serialize, if all went well
            try {
                keySerializer.serialize(keyElem, gen, provider);
                valueSer.serialize(valueElem, gen, provider);
            } catch (Exception e) {
                String keyDesc = ""+keyElem;
                wrapAndThrow(provider, e, value, keyDesc);
            }
        }
    }
    
    /**
     * Method called to serialize fields, when the value type is statically known,
     * so that value serializer is passed and does not need to be fetched from
     * provider.
     */
    protected void serializeFieldsUsing(Map<?,?> value, JsonGenerator gen, SerializerProvider provider,
            JsonSerializer<Object> ser)
        throws IOException
    {
        final JsonSerializer<Object> keySerializer = _keySerializer;
        final HashSet<String> ignored = _ignoredEntries;
        final TypeSerializer typeSer = _valueTypeSerializer;

        for (Map.Entry<?,?> entry : value.entrySet()) {
            Object keyElem = entry.getKey();
            if (ignored != null && ignored.contains(keyElem)) continue;

            if (keyElem == null) {
                provider.findNullKeySerializer(_keyType, _property).serialize(null, gen, provider);
            } else {
                keySerializer.serialize(keyElem, gen, provider);
            }
            final Object valueElem = entry.getValue();
            if (valueElem == null) {
                provider.defaultSerializeNull(gen);
            } else {
                try {
                    if (typeSer == null) {
                        ser.serialize(valueElem, gen, provider);
                    } else {
                        ser.serializeWithType(valueElem, gen, provider, typeSer);
                    }
                } catch (Exception e) {
                    String keyDesc = ""+keyElem;
                    wrapAndThrow(provider, e, value, keyDesc);
                }
            }
        }
    }

    /**
     * Helper method used when we have a JSON Filter to use for potentially
     * filtering out Map entries.
     * 
     * @since 2.5
     */
    public void serializeFilteredFields(Map<?,?> value, JsonGenerator gen, SerializerProvider provider,
            PropertyFilter filter,
            Object suppressableValue) // since 2.5
        throws IOException
    {
        final HashSet<String> ignored = _ignoredEntries;

        PropertySerializerMap serializers = _dynamicValueSerializers;
        final MapProperty prop = new MapProperty(_valueTypeSerializer, _property);

        for (Map.Entry<?,?> entry : value.entrySet()) {
            // First, serialize key; unless ignorable by key
            final Object keyElem = entry.getKey();
            if (ignored != null && ignored.contains(keyElem)) continue;

            JsonSerializer<Object> keySerializer;
            if (keyElem == null) {
                keySerializer = provider.findNullKeySerializer(_keyType, _property);
            } else {
                keySerializer = _keySerializer;
            }
            // or by value; nulls often suppressed
            final Object valueElem = entry.getValue();

            JsonSerializer<Object> valueSer;
            // And then value
            if (valueElem == null) {
                if (suppressableValue != null) { // all suppressions include null-suppression
                    continue;
                }
                valueSer = provider.getDefaultNullValueSerializer();
            } else {
                valueSer = _valueSerializer;
                if (valueSer == null) {
                    Class<?> cc = valueElem.getClass();
                    valueSer = serializers.serializerFor(cc);
                    if (valueSer == null) {
                        if (_valueType.hasGenericTypes()) {
                            valueSer = _findAndAddDynamic(serializers,
                                    provider.constructSpecializedType(_valueType, cc), provider);
                        } else {
                            valueSer = _findAndAddDynamic(serializers, cc, provider);
                        }
                        serializers = _dynamicValueSerializers;
                    }
                }
                // also may need to skip non-empty values:
                if ((suppressableValue == JsonInclude.Include.NON_EMPTY)
                        && valueSer.isEmpty(provider, valueElem)) {
                    continue;
                }
            }
            // and with that, ask filter to handle it
            prop.reset(keyElem, keySerializer, valueSer);
            try {
                filter.serializeAsField(valueElem, gen, provider, prop);
            } catch (Exception e) {
                String keyDesc = ""+keyElem;
                wrapAndThrow(provider, e, value, keyDesc);
            }
        }
    }

    @Deprecated // since 2.5
    public void serializeFilteredFields(Map<?,?> value, JsonGenerator gen, SerializerProvider provider,
            PropertyFilter filter) throws IOException {
        serializeFilteredFields(value, gen, provider, filter,
                provider.isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES) ? null : JsonInclude.Include.NON_NULL);
    }
    
    /**
     * @since 2.5
     */
    protected void serializeTypedFields(Map<?,?> value, JsonGenerator gen, SerializerProvider provider,
            Object suppressableValue) // since 2.5
        throws IOException
    {
        final HashSet<String> ignored = _ignoredEntries;
        PropertySerializerMap serializers = _dynamicValueSerializers;

        for (Map.Entry<?,?> entry : value.entrySet()) {
            Object keyElem = entry.getKey();
            JsonSerializer<Object> keySerializer;
            if (keyElem == null) {
                keySerializer = provider.findNullKeySerializer(_keyType, _property);
            } else {
                // One twist: is entry ignorable? If so, skip
                if (ignored != null && ignored.contains(keyElem)) continue;
                keySerializer = _keySerializer;
            }
            final Object valueElem = entry.getValue();
    
            // And then value
            JsonSerializer<Object> valueSer;
            if (valueElem == null) {
                if (suppressableValue != null) { // all suppression include null suppression
                    continue;
                }
                valueSer = provider.getDefaultNullValueSerializer();
            } else {
                valueSer = _valueSerializer;
                Class<?> cc = valueElem.getClass();
                valueSer = serializers.serializerFor(cc);
                if (valueSer == null) {
                    if (_valueType.hasGenericTypes()) {
                        valueSer = _findAndAddDynamic(serializers,
                                provider.constructSpecializedType(_valueType, cc), provider);
                    } else {
                        valueSer = _findAndAddDynamic(serializers, cc, provider);
                    }
                    serializers = _dynamicValueSerializers;
                }
                // also may need to skip non-empty values:
                if ((suppressableValue == JsonInclude.Include.NON_EMPTY)
                        && valueSer.isEmpty(provider, valueElem)) {
                    continue;
                }
            }
            keySerializer.serialize(keyElem, gen, provider);
            try {
                valueSer.serializeWithType(valueElem, gen, provider, _valueTypeSerializer);
            } catch (Exception e) {
                String keyDesc = ""+keyElem;
                wrapAndThrow(provider, e, value, keyDesc);
            }
        }
    }

    @Deprecated // since 2.5
    protected void serializeTypedFields(Map<?,?> value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
        serializeTypedFields(value, gen, provider,
                provider.isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES) ? null : JsonInclude.Include.NON_NULL);
    }

    /*
    /**********************************************************
    /* Schema related functionality
    /**********************************************************
     */
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        ObjectNode o = createSchemaNode("object", true);
        //(ryan) even though it's possible to statically determine the "value" type of the map,
        // there's no way to statically determine the keys, so the "Entries" can't be determined.
        return o;
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        JsonMapFormatVisitor v2 = (visitor == null) ? null : visitor.expectMapFormat(typeHint);        
        if (v2 != null) {
            v2.keyFormat(_keySerializer, _keyType);
            JsonSerializer<?> valueSer = _valueSerializer;
            if (valueSer == null) {
                valueSer = _findAndAddDynamic(_dynamicValueSerializers,
                            _valueType, visitor.getProvider());
            }
            v2.valueFormat(valueSer, _valueType);
        }
    }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */
    
    protected final JsonSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
            Class<?> type, SerializerProvider provider) throws JsonMappingException
    {
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type, provider, _property);
        // did we get a new map of serializers? If so, start using it
        if (map != result.map) {
            _dynamicValueSerializers = result.map;
        }
        return result.serializer;
    }

    protected final JsonSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
            JavaType type, SerializerProvider provider) throws JsonMappingException
    {
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type, provider, _property);
        if (map != result.map) {
            _dynamicValueSerializers = result.map;
        }
        return result.serializer;
    }

    protected Map<?,?> _orderEntries(Map<?,?> input)
    {
        // minor optimization: may already be sorted?
        if (input instanceof SortedMap<?,?>) {
            return input;
        }
        return new TreeMap<Object,Object>(input);
    }
}

