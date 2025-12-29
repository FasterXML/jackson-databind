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

    @Deprecated // since 3.1
    public IndexedListSerializer(IndexedListSerializer src,
            TypeSerializer vts, ValueSerializer<?> valueSerializer,
            Boolean unwrapSingle, BeanProperty property) {
        this(src, vts, valueSerializer, unwrapSingle, property, src._suppressableValue, src._suppressNulls);
    }

    /**
     * @since 3.1
     */
    public IndexedListSerializer(IndexedListSerializer src,
             TypeSerializer vts, ValueSerializer<?> valueSerializer, Boolean unwrapSingle,
             BeanProperty property, Object suppressableValue, boolean suppressNulls) {
        super(src, vts, valueSerializer, unwrapSingle, property, suppressableValue, suppressNulls);
    }

    @Override
    protected StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new IndexedListSerializer(this,
                vts, _elementSerializer, _unwrapSingle, _property,
                _suppressableValue, _suppressNulls);
                
    }

    @Deprecated // @since 3.1
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
    public final void serialize(Object value0, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        final List<?> value = (List<?>) value0;
        final int len = value.size();
        if (len == 1) {
            if (((_unwrapSingle == null) &&
                    ctxt.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                serializeContents(value, g, ctxt);
                return;
            }
        }
        g.writeStartArray(value, len);
        serializeContents(value, g, ctxt);
        g.writeEndArray();
    }

    @Override
    public void serializeContents(Object value0, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        final List<?> value = (List<?>) value0;
        if (_elementSerializer != null) {
            serializeContentsUsingImpl(value, g, ctxt, _elementSerializer);
        } else if (_valueTypeSerializer != null) {
            serializeTypedContentsImpl(value, g, ctxt);
        } else {
            serializeContentsImpl(value, g, ctxt);
        }
    }

    private void serializeContentsImpl(List<?> value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        final int len = value.size();
        if (len == 0) {
            return;
        }
        int i = 0;
        final boolean filtered = _needToCheckFiltering(ctxt);
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
                        if (_elementType.hasGenericTypes()) {
                            serializer = _findAndAddDynamic(ctxt,
                                    ctxt.constructSpecializedType(_elementType, cc));
                        } else {
                            serializer = _findAndAddDynamic(ctxt, cc);
                        }
                    }
                    // Check if this element should be suppressed (only in filtered mode)
                    if (filtered && !_shouldSerializeElement(ctxt, elem, serializer)) {
                        continue;
                    }
                    serializer.serialize(elem, g, ctxt);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, value, i);
        }
    }

    private void serializeContentsUsingImpl(List<?> value, JsonGenerator g,
            SerializationContext ctxt, ValueSerializer<Object> ser)
        throws JacksonException
    {
        final int len = value.size();
        if (len == 0) {
            return;
        }
        final TypeSerializer typeSer = _valueTypeSerializer;
        final boolean filtered = _needToCheckFiltering(ctxt);
        for (int i = 0; i < len; ++i) {
            Object elem = value.get(i);
            try {
                if (elem == null) {
                    if (filtered && _suppressNulls) {
                        continue;
                    }
                    ctxt.defaultSerializeNullValue(g);
                } else {
                    // Check if this element should be suppressed (only in filtered mode)
                    if (filtered && !_shouldSerializeElement(ctxt, elem, ser)) {
                        continue;
                    }
                    if (typeSer == null) {
                        ser.serialize(elem, g, ctxt);
                    } else {
                        ser.serializeWithType(elem, g, ctxt, typeSer);
                    }
                }
            } catch (Exception e) {
                wrapAndThrow(ctxt, e, value, i);
            }
        }
    }

    private void serializeTypedContentsImpl(List<?> value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        final int len = value.size();
        if (len == 0) {
            return;
        }
        int i = 0;
        final boolean filtered = _needToCheckFiltering(ctxt);
        try {
            final TypeSerializer typeSer = _valueTypeSerializer;
            PropertySerializerMap serializers = _dynamicValueSerializers;
            for (; i < len; ++i) {
                Object elem = value.get(i);
                if (elem == null) {
                    if (filtered && _suppressNulls) {
                        continue;
                    }
                    ctxt.defaultSerializeNullValue(g);
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
                    if (filtered && !_shouldSerializeElement(ctxt, elem, serializer)) {
                        continue;
                    }
                    serializer.serializeWithType(elem, g, ctxt, typeSer);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, value, i);
        }
    }
}
