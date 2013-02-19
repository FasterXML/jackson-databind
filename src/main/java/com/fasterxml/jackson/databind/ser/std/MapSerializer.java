package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

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
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.type.TypeFactory;

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
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
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
        _dynamicValueSerializers = PropertySerializerMap.emptyMap();
        _property = null;
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
    }

    protected MapSerializer(MapSerializer src, TypeSerializer vts)
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
    }
    
    @Override
    public MapSerializer _withValueTypeSerializer(TypeSerializer vts)
    {
        return new MapSerializer(this, vts);
    }

    public MapSerializer withResolved(BeanProperty property,
            JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer,
            HashSet<String> ignored)
    {
        return new MapSerializer(this, property, keySerializer, valueSerializer, ignored);
    }
    
    public static MapSerializer construct(String[] ignoredList, JavaType mapType,
            boolean staticValueType, TypeSerializer vts,
            JsonSerializer<Object> keySerializer, JsonSerializer<Object> valueSerializer)
    {
        HashSet<String> ignoredEntries = toSet(ignoredList);
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
        }
        return new MapSerializer(ignoredEntries, keyType, valueType, staticValueType, vts,
                keySerializer, valueSerializer);
    }

    private static HashSet<String> toSet(String[] ignoredEntries) {
        if (ignoredEntries == null || ignoredEntries.length == 0) {
            return null;
        }
        HashSet<String> result = new HashSet<String>(ignoredEntries.length);
        for (String prop : ignoredEntries) {
            result.add(prop);
        }
        return result;
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

        // First: if we have a property, may have property-annotation overrides
        if (property != null) {
            AnnotatedMember m = property.getMember();
            if (m != null) {
                Object serDef;
                final AnnotationIntrospector intr = provider.getAnnotationIntrospector();
                serDef = intr.findKeySerializer(m);
                if (serDef != null) {
                    keySer = provider.serializerInstance(m, serDef);
                }
                serDef = intr.findContentSerializer(m);
                if (serDef != null) {
                    ser = provider.serializerInstance(m, serDef);
                }
            }
        }
        if (ser == null) {
            ser = _valueSerializer;
        }
        // #124: May have a content converter
        ser = findConvertingContentSerializer(provider, property, ser);
        if (ser == null) {
            // 30-Sep-2012, tatu: One more thing -- if explicit content type is annotated,
            //   we can consider it a static case as well.
            if (_valueTypeIsStatic || hasContentTypeAnnotation(provider, property)) {
                ser = provider.findValueSerializer(_valueType, property);
            }
        } else if (ser instanceof ContextualSerializer) {
            ser = ((ContextualSerializer) ser).createContextual(provider, property);
        }
        if (keySer == null) {
            keySer = _keySerializer;
        }
        if (keySer == null) {
            keySer = provider.findKeySerializer(_keyType, property);
        } else if (keySer instanceof ContextualSerializer) {
            keySer = ((ContextualSerializer) keySer).createContextual(provider, property);
        }
        HashSet<String> ignored = this._ignoredEntries;
        AnnotationIntrospector intr = provider.getAnnotationIntrospector();
        if (intr != null && property != null) {
            String[] moreToIgnore = intr.findPropertiesToIgnore(property.getMember());
            if (moreToIgnore != null) {
                ignored = (ignored == null) ? new HashSet<String>() : new HashSet<String>(ignored);
                for (String str : moreToIgnore) {
                    ignored.add(str);
                }
            }
        }
        return withResolved(property, keySer, ser, ignored);
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
    public boolean isEmpty(Map<?,?> value) {
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
    public void serialize(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeStartObject();
        if (!value.isEmpty()) {
            if (provider.isEnabled(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)) {
                value = _orderEntries(value);
            }
            if (_valueSerializer != null) {
                serializeFieldsUsing(value, jgen, provider, _valueSerializer);
            } else {
                serializeFields(value, jgen, provider);
            }
        }        
        jgen.writeEndObject();
    }

    @Override
    public void serializeWithType(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        typeSer.writeTypePrefixForObject(value, jgen);
        if (!value.isEmpty()) {
            if (provider.isEnabled(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)) {
                value = _orderEntries(value);
            }
            if (_valueSerializer != null) {
                serializeFieldsUsing(value, jgen, provider, _valueSerializer);
            } else {
                serializeFields(value, jgen, provider);
            }
        }
        typeSer.writeTypeSuffixForObject(value, jgen);
    }

    /*
    /**********************************************************
    /* JsonSerializer implementation
    /**********************************************************
     */
    
    /**
     * Method called to serialize fields, when the value type is not statically known.
     */
    public void serializeFields(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        // If value type needs polymorphic type handling, some more work needed:
        if (_valueTypeSerializer != null) {
            serializeTypedFields(value, jgen, provider);
            return;
        }
        final JsonSerializer<Object> keySerializer = _keySerializer;
        
        final HashSet<String> ignored = _ignoredEntries;
        final boolean skipNulls = !provider.isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES);

        PropertySerializerMap serializers = _dynamicValueSerializers;

        for (Map.Entry<?,?> entry : value.entrySet()) {
            Object valueElem = entry.getValue();
            // First, serialize key
            Object keyElem = entry.getKey();
            if (keyElem == null) {
                provider.findNullKeySerializer(_keyType, _property).serialize(null, jgen, provider);
            } else {
                // [JACKSON-314] skip entries with null values?
                if (skipNulls && valueElem == null) continue;
                // One twist: is entry ignorable? If so, skip
                if (ignored != null && ignored.contains(keyElem)) continue;
                keySerializer.serialize(keyElem, jgen, provider);
            }

            // And then value
            if (valueElem == null) {
                provider.defaultSerializeNull(jgen);
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
                    serializer.serialize(valueElem, jgen, provider);
                } catch (Exception e) {
                    // [JACKSON-55] Need to add reference information
                    String keyDesc = ""+keyElem;
                    wrapAndThrow(provider, e, value, keyDesc);
                }
            }
        }
    }

    /**
     * Method called to serialize fields, when the value type is statically known,
     * so that value serializer is passed and does not need to be fetched from
     * provider.
     */
    protected void serializeFieldsUsing(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider,
            JsonSerializer<Object> ser)
            throws IOException, JsonGenerationException
    {
        final JsonSerializer<Object> keySerializer = _keySerializer;
        final HashSet<String> ignored = _ignoredEntries;
        final TypeSerializer typeSer = _valueTypeSerializer;
        final boolean skipNulls = !provider.isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES);

        for (Map.Entry<?,?> entry : value.entrySet()) {
            Object valueElem = entry.getValue();
            Object keyElem = entry.getKey();
            if (keyElem == null) {
                provider.findNullKeySerializer(_keyType, _property).serialize(null, jgen, provider);
            } else {
                // [JACKSON-314] also may need to skip entries with null values
                if (skipNulls && valueElem == null) continue;
                if (ignored != null && ignored.contains(keyElem)) continue;
                keySerializer.serialize(keyElem, jgen, provider);
            }
            if (valueElem == null) {
                provider.defaultSerializeNull(jgen);
            } else {
                try {
                    if (typeSer == null) {
                        ser.serialize(valueElem, jgen, provider);
                    } else {
                        ser.serializeWithType(valueElem, jgen, provider, typeSer);
                    }
                } catch (Exception e) {
                    // [JACKSON-55] Need to add reference information
                    String keyDesc = ""+keyElem;
                    wrapAndThrow(provider, e, value, keyDesc);
                }
            }
        }
    }

    protected void serializeTypedFields(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        final JsonSerializer<Object> keySerializer = _keySerializer;
        JsonSerializer<Object> prevValueSerializer = null;
        Class<?> prevValueClass = null;
        final HashSet<String> ignored = _ignoredEntries;
        final boolean skipNulls = !provider.isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES);
    
        for (Map.Entry<?,?> entry : value.entrySet()) {
            Object valueElem = entry.getValue();
            // First, serialize key
            Object keyElem = entry.getKey();
            if (keyElem == null) {
                provider.findNullKeySerializer(_keyType, _property).serialize(null, jgen, provider);
            } else {
                // [JACKSON-314] also may need to skip entries with null values
                if (skipNulls && valueElem == null) continue;
                // One twist: is entry ignorable? If so, skip
                if (ignored != null && ignored.contains(keyElem)) continue;
                keySerializer.serialize(keyElem, jgen, provider);
            }
    
            // And then value
            if (valueElem == null) {
                provider.defaultSerializeNull(jgen);
            } else {
                Class<?> cc = valueElem.getClass();
                JsonSerializer<Object> currSerializer;
                if (cc == prevValueClass) {
                    currSerializer = prevValueSerializer;
                } else {
                    currSerializer = provider.findValueSerializer(cc, _property);
                    prevValueSerializer = currSerializer;
                    prevValueClass = cc;
                }
                try {
                    currSerializer.serializeWithType(valueElem, jgen, provider, _valueTypeSerializer);
                } catch (Exception e) {
                    // [JACKSON-55] Need to add reference information
                    String keyDesc = ""+keyElem;
                    wrapAndThrow(provider, e, value, keyDesc);
                }
            }
        }
    }
    
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
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSerializer(type, provider, _property);
        // did we get a new map of serializers? If so, start using it
        if (map != result.map) {
            _dynamicValueSerializers = result.map;
        }
        return result.serializer;
    }

    protected final JsonSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
            JavaType type, SerializerProvider provider) throws JsonMappingException
    {
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSerializer(type, provider, _property);
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

