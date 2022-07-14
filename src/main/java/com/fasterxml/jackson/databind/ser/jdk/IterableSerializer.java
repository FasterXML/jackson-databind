package com.fasterxml.jackson.databind.ser.jdk;

import java.util.*;

import tools.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase;
import com.fasterxml.jackson.databind.ser.std.StdContainerSerializer;

@JacksonStdImpl
public class IterableSerializer
    extends AsArraySerializerBase<Iterable<?>>
{
    public IterableSerializer(JavaType elemType, boolean staticTyping,
            TypeSerializer vts) {
        super(Iterable.class, elemType, staticTyping, vts, null);
    }

    public IterableSerializer(IterableSerializer src,
            TypeSerializer vts, ValueSerializer<?> valueSerializer,
            Boolean unwrapSingle, BeanProperty property) {
        super(src, vts, valueSerializer, unwrapSingle, property);
    }

    @Override
    protected StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new IterableSerializer(this, vts, _elementSerializer, _unwrapSingle, _property);
    }

    @Override
    public IterableSerializer withResolved(BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle) {
        return new IterableSerializer(this, vts, elementSerializer, unwrapSingle, property);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    @Override
    public boolean isEmpty(SerializerProvider prov, Iterable<?> value) {
        // Not really good way to implement this, but has to do for now:
        return !value.iterator().hasNext();
    }

    @Override
    public boolean hasSingleElement(Iterable<?> value) {
        // we can do it actually (fixed in 2.3.1)
        if (value != null) {
            Iterator<?> it = value.iterator();
            if (it.hasNext()) {
                it.next();
                if (!it.hasNext()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public final void serialize(Iterable<?> value, JsonGenerator g,
        SerializerProvider ctxt) throws JacksonException
    {
        if (((_unwrapSingle == null) &&
                ctxt.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                || (_unwrapSingle == Boolean.TRUE)) {
            if (hasSingleElement(value)) {
                serializeContents(value, g, ctxt);
                return;
            }
        }
        g.writeStartArray(value);
        serializeContents(value, g, ctxt);
        g.writeEndArray();
    }

    @Override
    public void serializeContents(Iterable<?> value, JsonGenerator g,
        SerializerProvider ctxt) throws JacksonException
    {
        Iterator<?> it = value.iterator();
        if (it.hasNext()) {
            final TypeSerializer typeSer = _valueTypeSerializer;
            do {
                Object elem = it.next();
                if (elem == null) {
                    ctxt.defaultSerializeNullValue(g);
                    continue;
                }
                ValueSerializer<Object> serializer = _elementSerializer;
                if (serializer == null) {
                    Class<?> cc = elem.getClass();
                    serializer = _dynamicValueSerializers.serializerFor(cc);
                    if (serializer == null) {
                        if (_elementType.hasGenericTypes()) {
                            serializer = _findAndAddDynamic(ctxt, ctxt.constructSpecializedType(_elementType, cc));
                        } else {
                            serializer = _findAndAddDynamic(ctxt, cc);
                        }
                    }
                }
                if (typeSer == null) {
                    serializer.serialize(elem, g, ctxt);
                } else {
                    serializer.serializeWithType(elem, g, ctxt, typeSer);
                }
            } while (it.hasNext());
        }
    }
}
