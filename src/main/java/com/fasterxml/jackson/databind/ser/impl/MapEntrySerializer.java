package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.BeanUtil;

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
     * @since 2.9
     */
    public final static Object MARKER_FOR_EMPTY = JsonInclude.Include.NON_EMPTY;

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

    /*
    /**********************************************************
    /* Serializers used
    /**********************************************************
     */

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
     * If value type cannot be statically determined, mapping from
     * runtime value types to serializers are stored in this object.
     */
    protected PropertySerializerMap _dynamicValueSerializers;

    /*
    /**********************************************************
    /* Config settings, filtering
    /**********************************************************
     */

    /**
     * Value that indicates suppression mechanism to use for <b>values contained</b>;
     * either "filter" (of which <code>equals()</code> is called), or marker
     * value of {@link #MARKER_FOR_EMPTY}, or null to indicate no filtering for
     * non-null values.
     * Note that inclusion value for Map instance itself is handled by caller (POJO
     * property that refers to the Map value).
     *
     * @since 2.5
     */
    protected final Object _suppressableValue;

    /**
     * Flag that indicates what to do with `null` values, distinct from
     * handling of {@link #_suppressableValue}
     *
     * @since 2.9
     */
    protected final boolean _suppressNulls;

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
        _suppressableValue = null;
        _suppressNulls = false;
    }

    @Deprecated // since 2.9
    protected MapEntrySerializer(MapEntrySerializer src, BeanProperty property,
            TypeSerializer vts,
            JsonSerializer<?> keySer, JsonSerializer<?> valueSer)
    {
        this(src, property, vts, keySer, valueSer,
                src._suppressableValue, src._suppressNulls);
    }

    @SuppressWarnings("unchecked")
    protected MapEntrySerializer(MapEntrySerializer src, BeanProperty property,
            TypeSerializer vts,
            JsonSerializer<?> keySer, JsonSerializer<?> valueSer,
            Object suppressableValue, boolean suppressNulls)
    {
        super(Map.class, false);
        _entryType = src._entryType;
        _keyType = src._keyType;
        _valueType = src._valueType;
        _valueTypeIsStatic = src._valueTypeIsStatic;
        _valueTypeSerializer = src._valueTypeSerializer;
        _keySerializer = (JsonSerializer<Object>) keySer;
        _valueSerializer = (JsonSerializer<Object>) valueSer;
        // [databind#2181]: may not be safe to reuse, start from empty
        _dynamicValueSerializers = PropertySerializerMap.emptyForProperties();
        _property = src._property;
        _suppressableValue = suppressableValue;
        _suppressNulls = suppressNulls;
    }

    @Override
    public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new MapEntrySerializer(this, _property, vts, _keySerializer, _valueSerializer,
                _suppressableValue, _suppressNulls);
    }

    /**
     * @since 2.9
     */
    public MapEntrySerializer withResolved(BeanProperty property,
            JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer,
            Object suppressableValue, boolean suppressNulls) {
        return new MapEntrySerializer(this, property, _valueTypeSerializer,
                keySerializer, valueSerializer, suppressableValue, suppressNulls);
    }

    /**
     * @since 2.9
     */
    public MapEntrySerializer withContentInclusion(Object suppressableValue,
            boolean suppressNulls) {
        if ((_suppressableValue == suppressableValue)
                && (_suppressNulls == suppressNulls)) {
            return this;
        }
        return new MapEntrySerializer(this, _property, _valueTypeSerializer,
                _keySerializer, _valueSerializer, suppressableValue, suppressNulls);
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
        // [databind#124]: May have a content converter
        ser = findContextualConvertingSerializer(provider, property, ser);
        if (ser == null) {
            // 30-Sep-2012, tatu: One more thing -- if explicit content type is annotated,
            //   we can consider it a static case as well.
            // 20-Aug-2013, tatu: Need to avoid trying to access serializer for java.lang.Object tho
            if (_valueTypeIsStatic && !_valueType.isJavaLangObject()) {
                ser = provider.findContentValueSerializer(_valueType, property);
            }
        }
        if (keySer == null) {
            keySer = _keySerializer;
        }
        if (keySer == null) {
            keySer = provider.findKeySerializer(_keyType, property);
        } else {
            keySer = provider.handleSecondaryContextualization(keySer, property);
        }

        Object valueToSuppress = _suppressableValue;
        boolean suppressNulls = _suppressNulls;
        if (property != null) {
            JsonInclude.Value inclV = property.findPropertyInclusion(provider.getConfig(), null);
            if (inclV != null) {
                JsonInclude.Include incl = inclV.getContentInclusion();
                if (incl != JsonInclude.Include.USE_DEFAULTS) {
                    switch (incl) {
                    case NON_DEFAULT:
                        valueToSuppress = BeanUtil.getDefaultValue(_valueType);
                        suppressNulls = true;
                        if (valueToSuppress != null) {
                            if (valueToSuppress.getClass().isArray()) {
                                valueToSuppress = ArrayBuilders.getArrayComparator(valueToSuppress);
                            }
                        }
                        break;
                    case NON_ABSENT:
                        suppressNulls = true;
                        valueToSuppress = _valueType.isReferenceType() ? MARKER_FOR_EMPTY : null;
                        break;
                    case NON_EMPTY:
                        suppressNulls = true;
                        valueToSuppress = MARKER_FOR_EMPTY;
                        break;
                    case CUSTOM:
                        valueToSuppress = provider.includeFilterInstance(null, inclV.getContentFilter());
                        if (valueToSuppress == null) { // is this legal?
                            suppressNulls = true;
                        } else {
                            suppressNulls = provider.includeFilterSuppressNulls(valueToSuppress);
                        }
                        break;
                    case NON_NULL:
                        valueToSuppress = null;
                        suppressNulls = true;
                        break;
                    case ALWAYS: // default
                    default:
                        valueToSuppress = null;
                        // 30-Sep-2016, tatu: Should not need to check global flags here,
                        //   if inclusion forced to be ALWAYS
                        suppressNulls = false;
                        break;
                    }
                }
            }
        }

        MapEntrySerializer mser = withResolved(property, keySer, ser,
                valueToSuppress, suppressNulls);
        // but note: no (full) filtering or sorting (unlike Maps)
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
    public boolean isEmpty(SerializerProvider prov, Entry<?, ?> entry)
    {
        Object value = entry.getValue();
        if (value == null) {
            return _suppressNulls;
        }
        if (_suppressableValue == null) {
            return false;
        }
        JsonSerializer<Object> valueSer = _valueSerializer;
        if (valueSer == null) {
            // Let's not worry about generic types here, actually;
            // unlikely to make any difference, but does add significant overhead
            Class<?> cc = value.getClass();
            valueSer = _dynamicValueSerializers.serializerFor(cc);
            if (valueSer == null) {
                try {
                    valueSer = _findAndAddDynamic(_dynamicValueSerializers, cc, prov);
                } catch (JsonMappingException e) { // Ugh... cannot just throw as-is, so...
                    return false;
                }
            }
        }
        if (_suppressableValue == MARKER_FOR_EMPTY) {
            return valueSer.isEmpty(prov, value);
        }
        return _suppressableValue.equals(value);
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
        gen.writeStartObject(value);
        serializeDynamic(value, gen, provider);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Map.Entry<?, ?> value, JsonGenerator g,
            SerializerProvider provider, TypeSerializer typeSer) throws IOException
    {
        // [databind#631]: Assign current value, to be accessible by custom serializers
        g.setCurrentValue(value);
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                typeSer.typeId(value, JsonToken.START_OBJECT));
        serializeDynamic(value, g, provider);
        typeSer.writeTypeSuffix(g, typeIdDef);
    }

    protected void serializeDynamic(Map.Entry<?, ?> value, JsonGenerator gen,
            SerializerProvider provider)
        throws IOException
    {
        final TypeSerializer vts = _valueTypeSerializer;
        final Object keyElem = value.getKey();

        JsonSerializer<Object> keySerializer;
        if (keyElem == null) {
            keySerializer = provider.findNullKeySerializer(_keyType, _property);
        } else {
            keySerializer = _keySerializer;
        }
        // or by value; nulls often suppressed
        final Object valueElem = value.getValue();
        JsonSerializer<Object> valueSer;
        // And then value
        if (valueElem == null) {
            if (_suppressNulls) {
                return;
            }
            valueSer = provider.getDefaultNullValueSerializer();
        } else {
            valueSer = _valueSerializer;
            if (valueSer == null) {
                Class<?> cc = valueElem.getClass();
                valueSer = _dynamicValueSerializers.serializerFor(cc);
                if (valueSer == null) {
                    if (_valueType.hasGenericTypes()) {
                        valueSer = _findAndAddDynamic(_dynamicValueSerializers,
                                provider.constructSpecializedType(_valueType, cc), provider);
                    } else {
                        valueSer = _findAndAddDynamic(_dynamicValueSerializers, cc, provider);
                    }
                }
            }
            // also may need to skip non-empty values:
            if (_suppressableValue != null) {
                if (_suppressableValue == MARKER_FOR_EMPTY) {
                    if (valueSer.isEmpty(provider, valueElem)) {
                        return;
                    }
                } if (_suppressableValue.equals(valueElem)) {
                    return;
                }
            }
        }
        keySerializer.serialize(keyElem, gen, provider);
        try {
            if (vts == null) {
                valueSer.serialize(valueElem, gen, provider);
            } else {
                valueSer.serializeWithType(valueElem, gen, provider, vts);
            }
        } catch (Exception e) {
            String keyDesc = ""+keyElem;
            wrapAndThrow(provider, e, value, keyDesc);
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