package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * @since 2.5
 */
@SuppressWarnings("serial")
@JacksonStdImpl
public class MapEntrySerializer
    extends ContainerSerializer<Map.Entry<?,?>>
    implements ContextualSerializer
{
    /**
     * Map-valued property being serialized with this instance
     */
    protected final BeanProperty _property;

    /**
     * Whether static types should be used for serialization of values
     * or not (if not, dynamic runtime type is used)
     */
    protected final boolean _valueTypeIsStatic;

    protected final JavaType _entryType, _keyType, _valueType;

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
    /* Construction, initialization
    /**********************************************************
     */
    
    public MapEntrySerializer(JavaType type, JavaType keyType, JavaType valueType,
            boolean staticTyping, TypeSerializer vts,
            BeanProperty property)
    {
        super(type);
        _entryType = type;
        _keyType = keyType;
        _valueType = valueType;
        _valueTypeIsStatic = staticTyping;
        _valueTypeSerializer = vts;
        _property = property;
        _dynamicValueSerializers = PropertySerializerMap.emptyForProperties();
    }

    @SuppressWarnings("unchecked")
    protected MapEntrySerializer(MapEntrySerializer src, BeanProperty property,
            TypeSerializer vts,
            JsonSerializer<?> keySer, JsonSerializer<?> valueSer)
    {
        super(Map.class, false);
        _entryType = src._entryType;
        _keyType = src._keyType;
        _valueType = src._valueType;
        _valueTypeIsStatic = src._valueTypeIsStatic;
        _valueTypeSerializer = src._valueTypeSerializer;
        _keySerializer = (JsonSerializer<Object>) keySer;
        _valueSerializer = (JsonSerializer<Object>) valueSer;
        _dynamicValueSerializers = src._dynamicValueSerializers;
        _property = src._property;
    }

    @Override
    public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new MapEntrySerializer(this, _property, vts, _keySerializer, _valueSerializer);
    }

    public MapEntrySerializer withResolved(BeanProperty property,
            JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer) {
        return new MapEntrySerializer(this, property, _valueTypeSerializer, keySerializer, valueSerializer);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property) throws JsonMappingException
    {
        JsonSerializer<?> ser = null;
        JsonSerializer<?> keySer = null;
        final AnnotationIntrospector intr = provider.getAnnotationIntrospector();
        final AnnotatedMember propertyAcc = (property == null) ? null : property.getMember();

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
        MapEntrySerializer mser = withResolved(property, keySer, ser);
        // but note: no filtering, ignored entries or sorting (unlike Maps)
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
    public boolean hasSingleElement(Map.Entry<?,?> value) {
        return true;
    }

    @Override
    public boolean isEmpty(SerializerProvider prov, Entry<?, ?> value) {
        return (value == null);
    }

    /*
    /**********************************************************
    /* Serialization methods
    /**********************************************************
     */

    @Override
    public void serialize(Map.Entry<?, ?> value, JsonGenerator gen, SerializerProvider provider)
        throws IOException
    {
        gen.writeStartObject();
        // [databind#631]: Assign current value, to be accessible by custom serializers
        gen.setCurrentValue(value);
        if (_valueSerializer != null) {
            serializeUsing(value, gen, provider, _valueSerializer);
        } else {
            serializeDynamic(value, gen, provider);
        }
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Map.Entry<?, ?> value, JsonGenerator gen, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException
    {
        typeSer.writeTypePrefixForObject(value, gen);
        // [databind#631]: Assign current value, to be accessible by custom serializers
        gen.setCurrentValue(value);
        if (_valueSerializer != null) {
            serializeUsing(value, gen, provider, _valueSerializer);
        } else {
            serializeDynamic(value, gen, provider);
        }
        typeSer.writeTypeSuffixForObject(value, gen);
    }

    protected void serializeDynamic(Map.Entry<?, ?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException
    {
        final JsonSerializer<Object> keySerializer = _keySerializer;
        final boolean skipNulls = !provider.isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES);
        final TypeSerializer vts = _valueTypeSerializer;

        PropertySerializerMap serializers = _dynamicValueSerializers;

        Object valueElem = value.getValue();
        Object keyElem = value.getKey();
        if (keyElem == null) {
            provider.findNullKeySerializer(_keyType, _property).serialize(null, jgen, provider);
        } else {
            // [JACKSON-314] skip entries with null values?
            if (skipNulls && valueElem == null) return;
            keySerializer.serialize(keyElem, jgen, provider);
        }
        // And then value
        if (valueElem == null) {
            provider.defaultSerializeNull(jgen);
        } else {
            Class<?> cc = valueElem.getClass();
            JsonSerializer<Object> ser = serializers.serializerFor(cc);
            if (ser == null) {
                if (_valueType.hasGenericTypes()) {
                    ser = _findAndAddDynamic(serializers,
                            provider.constructSpecializedType(_valueType, cc), provider);
                } else {
                    ser = _findAndAddDynamic(serializers, cc, provider);
                }
                serializers = _dynamicValueSerializers;
            }
            try {
                if (vts == null) {
                    ser.serialize(valueElem, jgen, provider);
                } else {
                    ser.serializeWithType(valueElem, jgen, provider, vts);
                }
            } catch (Exception e) {
                // [JACKSON-55] Need to add reference information
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
    protected void serializeUsing(Map.Entry<?, ?> value, JsonGenerator jgen, SerializerProvider provider,
            JsonSerializer<Object> ser)
        throws IOException, JsonGenerationException
    {
        final JsonSerializer<Object> keySerializer = _keySerializer;
        final TypeSerializer vts = _valueTypeSerializer;
        final boolean skipNulls = !provider.isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES);

        Object valueElem = value.getValue();
        Object keyElem = value.getKey();
        if (keyElem == null) {
            provider.findNullKeySerializer(_keyType, _property).serialize(null, jgen, provider);
        } else {
            // [JACKSON-314] also may need to skip entries with null values
            if (skipNulls && valueElem == null) return;
            keySerializer.serialize(keyElem, jgen, provider);
        }
        if (valueElem == null) {
            provider.defaultSerializeNull(jgen);
        } else {
            try {
                if (vts == null) {
                    ser.serialize(valueElem, jgen, provider);
                } else {
                    ser.serializeWithType(valueElem, jgen, provider, vts);
                }
            } catch (Exception e) {
                // [JACKSON-55] Need to add reference information
                String keyDesc = ""+keyElem;
                wrapAndThrow(provider, e, value, keyDesc);
            }
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

}