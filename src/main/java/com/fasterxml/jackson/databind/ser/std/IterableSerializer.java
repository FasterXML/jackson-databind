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

@JacksonStdImpl
public class IterableSerializer
    extends AsArraySerializerBase<Iterable<?>>
{
    public IterableSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts, BeanProperty property)
    {
        super(Iterable.class, elemType, staticTyping, vts, property, null);
    }

    @Override
    public ContainerSerializerBase<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new IterableSerializer(_elementType, _staticTyping, vts, _property);
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
                } else {
                    // Minor optimization to avoid most lookups:
                    Class<?> cc = elem.getClass();
                    JsonSerializer<Object> currSerializer;
                    if (cc == prevClass) {
                        currSerializer = prevSerializer;
                    } else {
                        currSerializer = provider.findValueSerializer(cc, _property);
                        prevSerializer = currSerializer;
                        prevClass = cc;
                    }
                    if (typeSer == null) {
                        currSerializer.serialize(elem, jgen, provider);
                    } else {
                        currSerializer.serializeWithType(elem, jgen, provider, typeSer);
                    }
                }
            } while (it.hasNext());
        }
    }
}
