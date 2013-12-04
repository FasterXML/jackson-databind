package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;

@JacksonStdImpl
public class IterableSerializer
    extends AsArraySerializerBase<Iterable<?>>
{
    public IterableSerializer(JavaType elemType, boolean staticTyping,
            TypeSerializer vts, BeanProperty property)
    {
        super(Iterable.class, elemType, staticTyping, vts, property, null);
    }

    public IterableSerializer(IterableSerializer src, BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> valueSerializer)
    {
        super(src, property, vts, valueSerializer);
    }
    
    @Override
    public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new IterableSerializer(_elementType, _staticTyping, vts, _property);
    }

    @Override
    public IterableSerializer withResolved(BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> elementSerializer) {
        return new IterableSerializer(this, property, vts, elementSerializer);
    }
    
    @Override
    public boolean isEmpty(Iterable<?> value) {
        // Not really good way to implement this, but has to do for now:
        return (value == null) || !value.iterator().hasNext();
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
    public void serializeContents(Iterable<?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
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
