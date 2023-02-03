package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;

@JacksonStdImpl
@SuppressWarnings("serial")
public class IterableSerializer
    extends AsArraySerializerBase<Iterable<?>>
{
    public IterableSerializer(JavaType elemType, boolean staticTyping,
            TypeSerializer vts) {
        super(Iterable.class, elemType, staticTyping, vts, null);
    }

    public IterableSerializer(IterableSerializer src, BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> valueSerializer,
            Boolean unwrapSingle) {
        super(src, property, vts, valueSerializer, unwrapSingle);
    }

    @Override
    public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new IterableSerializer(this, _property, vts, _elementSerializer, _unwrapSingle);
    }

    @Override
    public IterableSerializer withResolved(BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> elementSerializer,
            Boolean unwrapSingle) {
        return new IterableSerializer(this, property, vts, elementSerializer, unwrapSingle);
    }

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
    public final void serialize(Iterable<?> value, JsonGenerator gen,
        SerializerProvider provider)throws IOException
    {
        if (((_unwrapSingle == null) &&
                provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                || (_unwrapSingle == Boolean.TRUE)) {
            if (hasSingleElement(value)) {
                serializeContents(value, gen, provider);
                return;
            }
        }
        gen.writeStartArray(value);
        serializeContents(value, gen, provider);
        gen.writeEndArray();
    }

    @Override
    public void serializeContents(Iterable<?> value, JsonGenerator jgen,
        SerializerProvider provider) throws IOException
    {
        Iterator<?> it = value.iterator();
        if (it.hasNext()) {
            final TypeSerializer typeSer = _valueTypeSerializer;
            JsonSerializer<Object> prevSerializer = null;
            Class<?> prevClass = null;

            do {
                Object elem = it.next();
                if (elem == null) {
                    provider.defaultSerializeNull(jgen);
                    continue;
                }
                JsonSerializer<Object> currSerializer = _elementSerializer;
                if (currSerializer == null) {
                    // Minor optimization to avoid most lookups:
                    Class<?> cc = elem.getClass();
                    if (cc == prevClass) {
                        currSerializer = prevSerializer;
                    } else {
                        currSerializer = provider.findValueSerializer(cc, _property);
                        prevSerializer = currSerializer;
                        prevClass = cc;
                    }
                }
                if (typeSer == null) {
                    currSerializer.serialize(elem, jgen, provider);
                } else {
                    currSerializer.serializeWithType(elem, jgen, provider, typeSer);
                }
            } while (it.hasNext());
        }
    }
}
