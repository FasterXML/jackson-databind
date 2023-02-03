package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;

/**
 * Generic serializer for Object arrays (<code>Object[]</code>).
 */
@JacksonStdImpl
@SuppressWarnings("serial")
public class ObjectArraySerializer
    extends ArraySerializerBase<Object[]>
    implements ContextualSerializer
{
    /**
     * Whether we are using static typing (using declared types, ignoring
     * runtime type) or not for elements.
     */
    protected final boolean _staticTyping;

    /**
     * Declared type of element entries
     */
    protected final JavaType _elementType;

    /**
     * Type serializer to use for values, if any.
     */
    protected final TypeSerializer _valueTypeSerializer;

    /**
     * Value serializer to use, if it can be statically determined.
     */
    protected JsonSerializer<Object> _elementSerializer;

    /**
     * If element type cannot be statically determined, mapping from
     * runtime type to serializer is handled using this object
     */
    protected PropertySerializerMap _dynamicSerializers;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public ObjectArraySerializer(JavaType elemType, boolean staticTyping,
            TypeSerializer vts, JsonSerializer<Object> elementSerializer)
    {
        super(Object[].class);
        _elementType = elemType;
        _staticTyping = staticTyping;
        _valueTypeSerializer = vts;
        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
        _elementSerializer = elementSerializer;
    }

    public ObjectArraySerializer(ObjectArraySerializer src, TypeSerializer vts)
    {
        super(src);
        _elementType = src._elementType;
        _valueTypeSerializer = vts;
        _staticTyping = src._staticTyping;
        // 22-Nov-2018, tatu: probably safe (even with [databind#2181]) since it's just
        //   inclusion, type serializer but NOT serializer
        _dynamicSerializers = src._dynamicSerializers;
        _elementSerializer = src._elementSerializer;
    }

    @SuppressWarnings("unchecked")
    public ObjectArraySerializer(ObjectArraySerializer src,
            BeanProperty property, TypeSerializer vts, JsonSerializer<?> elementSerializer,
            Boolean unwrapSingle)
    {
        super(src,  property, unwrapSingle);
        _elementType = src._elementType;
        _valueTypeSerializer = vts;
        _staticTyping = src._staticTyping;
        // [databind#2181]: may not be safe to reuse, start from empty
        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
        _elementSerializer = (JsonSerializer<Object>) elementSerializer;
    }

    @Override
    public JsonSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
        return new ObjectArraySerializer(this, prop,
                _valueTypeSerializer, _elementSerializer, unwrapSingle);
    }

    @Override
    public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new ObjectArraySerializer(_elementType, _staticTyping, vts, _elementSerializer);
    }

    public ObjectArraySerializer withResolved(BeanProperty prop,
            TypeSerializer vts, JsonSerializer<?> ser, Boolean unwrapSingle) {
        if ((_property == prop) && (ser == _elementSerializer)
                && (_valueTypeSerializer == vts) && (Objects.equals(_unwrapSingle, unwrapSingle))) {
            return this;
        }
        return new ObjectArraySerializer(this, prop, vts, ser, unwrapSingle);
    }

    /*
    /**********************************************************
    /* Post-processing
    /**********************************************************
     */

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializers,
            BeanProperty property)
        throws JsonMappingException
    {
        TypeSerializer vts = _valueTypeSerializer;
        if (vts != null) {
            vts = vts.forProperty(property);
        }
        JsonSerializer<?> ser = null;
        Boolean unwrapSingle = null;

        // First: if we have a property, may have property-annotation overrides
        if (property != null) {
            AnnotatedMember m = property.getMember();
            final AnnotationIntrospector intr = serializers.getAnnotationIntrospector();
            if (m != null) {
                Object serDef = intr.findContentSerializer(m);
                if (serDef != null) {
                    ser = serializers.serializerInstance(m, serDef);
                }
            }
        }
        JsonFormat.Value format = findFormatOverrides(serializers, property, handledType());
        if (format != null) {
            unwrapSingle = format.getFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        }
        if (ser == null) {
            ser = _elementSerializer;
        }
        // [databind#124]: May have a content converter
        ser = findContextualConvertingSerializer(serializers, property, ser);
        if (ser == null) {
            // 30-Sep-2012, tatu: One more thing -- if explicit content type is annotated,
            //   we can consider it a static case as well.
            if (_elementType != null) {
                if (_staticTyping && !_elementType.isJavaLangObject()) {
                    ser = serializers.findContentValueSerializer(_elementType, property);
                }
            }
        }
        return withResolved(property, vts, ser, unwrapSingle);
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override
    public JavaType getContentType() {
        return _elementType;
    }

    @Override
    public JsonSerializer<?> getContentSerializer() {
        return _elementSerializer;
    }

    @Override
    public boolean isEmpty(SerializerProvider prov, Object[] value) {
        return value.length == 0;
    }

    @Override
    public boolean hasSingleElement(Object[] value) {
        return (value.length == 1);
    }

    /*
    /**********************************************************
    /* Actual serialization
    /**********************************************************
     */

    @Override
    public final void serialize(Object[] value, JsonGenerator gen, SerializerProvider provider) throws IOException
    {
        final int len = value.length;
        if (len == 1) {
            if (((_unwrapSingle == null) &&
                    provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                serializeContents(value, gen, provider);
                return;
            }
        }
        gen.writeStartArray(value, len);
        serializeContents(value, gen, provider);
        gen.writeEndArray();
    }

    @Override
    public void serializeContents(Object[] value, JsonGenerator gen, SerializerProvider provider) throws IOException
    {
        final int len = value.length;
        if (len == 0) {
            return;
        }
        if (_elementSerializer != null) {
            serializeContentsUsing(value, gen, provider, _elementSerializer);
            return;
        }
        if (_valueTypeSerializer != null) {
            serializeTypedContents(value, gen, provider);
            return;
        }
        int i = 0;
        Object elem = null;
        try {
            PropertySerializerMap serializers = _dynamicSerializers;
            for (; i < len; ++i) {
                elem = value[i];
                if (elem == null) {
                    provider.defaultSerializeNull(gen);
                    continue;
                }
                Class<?> cc = elem.getClass();
                JsonSerializer<Object> serializer = serializers.serializerFor(cc);
                if (serializer == null) {
                    if (_elementType.hasGenericTypes()) {
                        serializer = _findAndAddDynamic(serializers,
                                provider.constructSpecializedType(_elementType, cc), provider);
                    } else {
                        serializer = _findAndAddDynamic(serializers, cc, provider);
                    }
                }
                serializer.serialize(elem, gen, provider);
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, elem, i);
        }
    }

    public void serializeContentsUsing(Object[] value, JsonGenerator jgen, SerializerProvider provider,
            JsonSerializer<Object> ser) throws IOException
    {
        final int len = value.length;
        final TypeSerializer typeSer = _valueTypeSerializer;

        int i = 0;
        Object elem = null;
        try {
            for (; i < len; ++i) {
                elem = value[i];
                if (elem == null) {
                    provider.defaultSerializeNull(jgen);
                    continue;
                }
                if (typeSer == null) {
                    ser.serialize(elem, jgen, provider);
                } else {
                    ser.serializeWithType(elem, jgen, provider, typeSer);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, elem, i);
        }
    }

    public void serializeTypedContents(Object[] value, JsonGenerator jgen, SerializerProvider provider) throws IOException
    {
        final int len = value.length;
        final TypeSerializer typeSer = _valueTypeSerializer;
        int i = 0;
        Object elem = null;
        try {
            PropertySerializerMap serializers = _dynamicSerializers;
            for (; i < len; ++i) {
                elem = value[i];
                if (elem == null) {
                    provider.defaultSerializeNull(jgen);
                    continue;
                }
                Class<?> cc = elem.getClass();
                JsonSerializer<Object> serializer = serializers.serializerFor(cc);
                if (serializer == null) {
                    serializer = _findAndAddDynamic(serializers, cc, provider);
                }
                serializer.serializeWithType(elem, jgen, provider, typeSer);
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, elem, i);
        }
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        JsonArrayFormatVisitor arrayVisitor = visitor.expectArrayFormat(typeHint);
        if (arrayVisitor != null) {
            JavaType contentType = _elementType;

            // [databind#1793]: Was getting `null` for `typeHint`. But why would we even use it...
/*
            TypeFactory tf = visitor.getProvider().getTypeFactory();
            contentType = tf.moreSpecificType(_elementType, typeHint.getContentType());
            if (contentType == null) {
                visitor.getProvider().reportBadDefinition(_elementType,
                        "Could not resolve type: "+_elementType);
            }
*/
            JsonSerializer<?> valueSer = _elementSerializer;
            if (valueSer == null) {
                valueSer = visitor.getProvider().findContentValueSerializer(contentType, _property);
            }
            arrayVisitor.itemsFormat(valueSer, contentType);
        }
    }

    protected final JsonSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
            Class<?> type, SerializerProvider provider) throws JsonMappingException
    {
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type, provider, _property);
        // did we get a new map of serializers? If so, start using it
        if (map != result.map) {
            _dynamicSerializers = result.map;
        }
        return result.serializer;
    }

    protected final JsonSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
            JavaType type, SerializerProvider provider) throws JsonMappingException
    {
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type, provider, _property);
        // did we get a new map of serializers? If so, start using it
        if (map != result.map) {
            _dynamicSerializers = result.map;
        }
        return result.serializer;
    }
}
