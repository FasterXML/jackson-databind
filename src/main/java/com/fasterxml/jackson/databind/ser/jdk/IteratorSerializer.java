package com.fasterxml.jackson.databind.ser.jdk;

import java.util.Iterator;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase;
import com.fasterxml.jackson.databind.ser.std.StdContainerSerializer;

@JacksonStdImpl
public class IteratorSerializer
    extends AsArraySerializerBase<Iterator<?>>
{
    public IteratorSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts) {
        super(Iterator.class, elemType, staticTyping, vts, null);
    }

    public IteratorSerializer(IteratorSerializer src,
            TypeSerializer vts, ValueSerializer<?> valueSerializer,
            Boolean unwrapSingle, BeanProperty property) {
        super(src, vts, valueSerializer, unwrapSingle, property);
    }

    @Override
    protected StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new IteratorSerializer(this, vts, _elementSerializer, _unwrapSingle, _property);
    }

    @Override
    public IteratorSerializer withResolved(BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle) {
        return new IteratorSerializer(this, vts, elementSerializer, unwrapSingle, property);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    @Override
    public boolean isEmpty(SerializerProvider prov, Iterator<?> value) {
        return !value.hasNext();
    }

    @Override
    public boolean hasSingleElement(Iterator<?> value) {
        // no really good way to determine (without consuming iterator), so:
        return false;
    }

    @Override
    public final void serialize(Iterator<?> value, JsonGenerator gen,
            SerializerProvider provider)
        throws JacksonException
    {
        // 02-Dec-2016, tatu: As per comments above, can't determine single element so...
        /*
        if (((_unwrapSingle == null) &&
                provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                || (_unwrapSingle == Boolean.TRUE)) {
            if (hasSingleElement(value)) {
                serializeContents(value, gen, provider);
                return;
            }
        }
        */
        gen.writeStartArray(value);
        serializeContents(value, gen, provider);
        gen.writeEndArray();
    }
    
    @Override
    public void serializeContents(Iterator<?> value, JsonGenerator g,
            SerializerProvider provider)
        throws JacksonException
    {
        if (!value.hasNext()) {
            return;
        }
        ValueSerializer<Object> serializer = _elementSerializer;
        if (serializer == null) {
            _serializeDynamicContents(value, g, provider);
            return;
        }
        final TypeSerializer typeSer = _valueTypeSerializer;
        do {
            Object elem = value.next();
            if (elem == null) {
                provider.defaultSerializeNullValue(g);
            } else if (typeSer == null) {
                serializer.serialize(elem, g, provider);
            } else {
                serializer.serializeWithType(elem, g, provider, typeSer);
            }
        } while (value.hasNext());
    }
    
    protected void _serializeDynamicContents(Iterator<?> value, JsonGenerator g,
            SerializerProvider ctxt)
        throws JacksonException
    {
        final TypeSerializer typeSer = _valueTypeSerializer;
        do {
            Object elem = value.next();
            if (elem == null) {
                ctxt.defaultSerializeNullValue(g);
                continue;
            }
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
            if (typeSer == null) {
                serializer.serialize(elem, g, ctxt);
            } else {
                serializer.serializeWithType(elem, g, ctxt, typeSer);
            }
        } while (value.hasNext());
    }
}
