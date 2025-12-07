package tools.jackson.databind.ser.jdk;

import java.util.*;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.impl.PropertySerializerMap;
import tools.jackson.databind.ser.std.AsArraySerializerBase;
import tools.jackson.databind.ser.std.StdContainerSerializer;

/**
 * This is an optimized serializer for Lists that can be efficiently
 * traversed by index (as opposed to others, such as {@link LinkedList}
 * that cannot}.
 */
@JacksonStdImpl
public final class IndexedListSerializer
    extends AsArraySerializerBase<Object>
{
    public IndexedListSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts,
            ValueSerializer<Object> valueSerializer)
    {
        super(List.class, elemType, staticTyping, vts, valueSerializer);
    }

    @Deprecated // since 3.1.0
    public IndexedListSerializer(IndexedListSerializer src,
            TypeSerializer vts, ValueSerializer<?> valueSerializer,
            Boolean unwrapSingle, BeanProperty property) {
        this(src, vts, valueSerializer, unwrapSingle, property, src._suppressableValue, src._suppressNulls);
    }

    /**
     * @since 3.1.0
     */
    public IndexedListSerializer(IndexedListSerializer src,
             TypeSerializer vts, ValueSerializer<?> valueSerializer, Boolean unwrapSingle,
             BeanProperty property, Object suppressableValue, boolean suppressNulls) {
        super(src, vts, valueSerializer, unwrapSingle, property, suppressableValue, suppressNulls);
    }

    @Override
    protected StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new IndexedListSerializer(this,
                vts, _elementSerializer, _unwrapSingle, _property);
    }

    @Override
    public IndexedListSerializer withResolved(BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle) {
        return new IndexedListSerializer(this, vts, elementSerializer, unwrapSingle, property);
    }

    @Override
    public IndexedListSerializer withResolved(BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
        return new IndexedListSerializer(this, vts, elementSerializer, unwrapSingle, property,
                suppressableValue, suppressNulls);
    }


    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    @Override
    public boolean isEmpty(SerializationContext prov, Object value) {
        return ((List<?>)value).isEmpty();
    }

    @Override
    public boolean hasSingleElement(Object value) {
        return (((List<?>)value).size() == 1);
    }

    @Override
    public final void serialize(Object value0, JsonGenerator gen, SerializationContext provider)
        throws JacksonException
    {
        final List<?> value = (List<?>) value0;
        final int len = value.size();
        if (len == 1) {
            if (((_unwrapSingle == null) &&
                    provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                if (provider.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS)
                    && ((_suppressableValue != null) || _suppressNulls)
                ) {
                    serializeFilteredContents(value, gen, provider);
                } else {
                    serializeContents(value, gen, provider);
                }
                return;
            }
        }
        gen.writeStartArray(value, len);
        if (provider.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS)
            && ((_suppressableValue != null) || _suppressNulls)
        ) {
            serializeFilteredContents(value, gen, provider);
        } else {
            serializeContents(value, gen, provider);
        }
        gen.writeEndArray();
    }

    @Override
    public void serializeContents(Object value0, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        serializeContentsImpl(value0, g, ctxt,
            false);
    }

    @Override
    public void serializeFilteredContents(Object value, JsonGenerator g, SerializationContext provider)
        throws JacksonException
    {
        serializeContentsImpl(value, g, provider,
            provider.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS));
    }

    private void serializeContentsImpl(Object value0, JsonGenerator g, SerializationContext ctxt, boolean filtered)
            throws JacksonException
    {
        final List<?> value = (List<?>) value0;
        if (_elementSerializer != null) {
            if (filtered) {
                serializeFilteredContentsUsing(value, g, ctxt, _elementSerializer);
            } else {
                serializeContentsUsing(value, g, ctxt, _elementSerializer);
            }
            return;
        }
        if (_valueTypeSerializer != null) {
            if (filtered) {
                serializeFilteredTypedContents(value, g, ctxt);
            } else {
                serializeTypedContents(value, g, ctxt);
            }
            return;
        }
        final int len = value.size();
        if (len == 0) {
            return;
        }
        int i = 0;
        try {
            for (; i < len; ++i) {
                Object elem = value.get(i);
                if (elem == null) {
                    if (filtered && _suppressNulls) {
                        continue;
                    }
                    ctxt.defaultSerializeNullValue(g);
                } else {
                    Class<?> cc = elem.getClass();
                    ValueSerializer<Object> serializer = _dynamicValueSerializers.serializerFor(cc);
                    if (serializer == null) {
                        // To fix [JACKSON-508]
                        if (_elementType.hasGenericTypes()) {
                            serializer = _findAndAddDynamic(ctxt,
                                    ctxt.constructSpecializedType(_elementType, cc));
                        } else {
                            serializer = _findAndAddDynamic(ctxt, cc);
                        }
                    }
                    // Check if this element should be suppressed (only in filtered mode)
                    if (filtered && !_shouldSerializeElement(elem, serializer, ctxt)) {
                        continue;
                    }
                    serializer.serialize(elem, g, ctxt);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, value, i);
        }
    }

    public void serializeContentsUsing(List<?> value, JsonGenerator jgen, SerializationContext ctxt,
            ValueSerializer<Object> ser)
        throws JacksonException
    {
        serializeContentsUsingImpl(value, jgen, ctxt, ser,
            false);
    }

    private void serializeFilteredContentsUsing(List<?> value, JsonGenerator jgen, SerializationContext ctxt,
                                                ValueSerializer<Object> ser)
        throws JacksonException
    {
        serializeContentsUsingImpl(value, jgen, ctxt, ser,
                ctxt.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS));
    }

    private void serializeContentsUsingImpl(List<?> value, JsonGenerator jgen, SerializationContext ctxt,
                                            ValueSerializer<Object> ser, boolean filtered)
        throws JacksonException
    {
        final int len = value.size();
        if (len == 0) {
            return;
        }
        final TypeSerializer typeSer = _valueTypeSerializer;
        for (int i = 0; i < len; ++i) {
            Object elem = value.get(i);
            try {
                if (elem == null) {
                    if (filtered && _suppressNulls) {
                        continue;
                    }
                    ctxt.defaultSerializeNullValue(jgen);
                } else {
                    // Check if this element should be suppressed (only in filtered mode)
                    if (filtered && !_shouldSerializeElement(elem, ser, ctxt)) {
                        continue;
                    }
                    if (typeSer == null) {
                        ser.serialize(elem, jgen, ctxt);
                    } else {
                        ser.serializeWithType(elem, jgen, ctxt, typeSer);
                    }
                }
            } catch (Exception e) {
                // [JACKSON-55] Need to add reference information
                wrapAndThrow(ctxt, e, value, i);
            }
        }
    }

    public void serializeTypedContents(List<?> value, JsonGenerator jgen, SerializationContext ctxt)
            throws JacksonException
    {
        serializeTypedContentsImpl(value, jgen, ctxt,
            false);
    }

    public void serializeFilteredTypedContents(List<?> value, JsonGenerator jgen, SerializationContext ctxt)
            throws JacksonException
    {
        serializeTypedContentsImpl(value, jgen, ctxt,
            ctxt.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS));
    }

    private void serializeTypedContentsImpl(List<?> value, JsonGenerator jgen, SerializationContext ctxt, boolean filtered)
        throws JacksonException
    {
        final int len = value.size();
        if (len == 0) {
            return;
        }
        int i = 0;
        try {
            final TypeSerializer typeSer = _valueTypeSerializer;
            PropertySerializerMap serializers = _dynamicValueSerializers;
            for (; i < len; ++i) {
                Object elem = value.get(i);
                if (elem == null) {
                    if (filtered && _suppressNulls) {
                        continue;
                    }
                    ctxt.defaultSerializeNullValue(jgen);
                } else {
                    Class<?> cc = elem.getClass();
                    ValueSerializer<Object> serializer = serializers.serializerFor(cc);
                    if (serializer == null) {
                        if (_elementType.hasGenericTypes()) {
                            serializer = _findAndAddDynamic(ctxt,
                                    ctxt.constructSpecializedType(_elementType, cc));
                        } else {
                            serializer = _findAndAddDynamic(ctxt, cc);
                        }
                        serializers = _dynamicValueSerializers;
                    }
                    // Check if this element should be suppressed (only in filtered mode)
                    if (filtered && !_shouldSerializeElement(elem, serializer, ctxt)) {
                        continue;
                    }
                    serializer.serializeWithType(elem, jgen, ctxt, typeSer);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, value, i);
        }
    }
}
