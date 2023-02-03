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

    public IndexedListSerializer(IndexedListSerializer src,
            TypeSerializer vts, ValueSerializer<?> valueSerializer,
            Boolean unwrapSingle, BeanProperty property) {
        super(src, vts, valueSerializer, unwrapSingle, property);
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

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    @Override
    public boolean isEmpty(SerializerProvider prov, Object value) {
        return ((List<?>)value).isEmpty();
    }

    @Override
    public boolean hasSingleElement(Object value) {
        return (((List<?>)value).size() == 1);
    }

    @Override
    public final void serialize(Object value0, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        final List<?> value = (List<?>) value0;
        final int len = value.size();
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
    public void serializeContents(Object value0, JsonGenerator g, SerializerProvider ctxt)
        throws JacksonException
    {
        final List<?> value = (List<?>) value0;
        if (_elementSerializer != null) {
            serializeContentsUsing(value, g, ctxt, _elementSerializer);
            return;
        }
        if (_valueTypeSerializer != null) {
            serializeTypedContents(value, g, ctxt);
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
                    serializer.serialize(elem, g, ctxt);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, value, i);
        }
    }

    public void serializeContentsUsing(List<?> value, JsonGenerator jgen, SerializerProvider provider,
            ValueSerializer<Object> ser)
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
                    provider.defaultSerializeNullValue(jgen);
                } else if (typeSer == null) {
                    ser.serialize(elem, jgen, provider);
                } else {
                    ser.serializeWithType(elem, jgen, provider, typeSer);
                }
            } catch (Exception e) {
                // [JACKSON-55] Need to add reference information
                wrapAndThrow(provider, e, value, i);
            }
        }
    }

    public void serializeTypedContents(List<?> value, JsonGenerator g, SerializerProvider ctxt)
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
                    serializer.serializeWithType(elem, g, ctxt, typeSer);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, value, i);
        }
    }
}
