package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;

/**
 * Intermediate base class for serializers used for various
 * Java arrays.
 * 
 * @param <T> Type of arrays serializer handles
 */
public abstract class ArraySerializerBase<T>
    extends ContainerSerializer<T>
{
    protected final BeanProperty _property;

    protected ArraySerializerBase(Class<T> cls)
    {
        super(cls);
        _property = null;
    }

    protected ArraySerializerBase(Class<T> cls, BeanProperty property)
    {
        super(cls);
        _property = property;
    }

    protected ArraySerializerBase(ArraySerializerBase<?> src)
    {
        super(src._handledType, false);
        _property = src._property;
    }
    
    protected ArraySerializerBase(ArraySerializerBase<?> src, BeanProperty property)
    {
        super(src._handledType, false);
        _property = property;
    }

    // NOTE: as of 2.5, sub-classes SHOULD override (in 2.4 and before, was final),
    // at least if they can provide access to actual size of value and use `writeStartArray()`
    // variant that passes size of array to output, which is helpful with some data formats
    @Override
    public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException
    {
        if (provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                && hasSingleElement(value)) {
            serializeContents(value, jgen, provider);
            return;
        }
        jgen.writeStartArray();
        serializeContents(value, jgen, provider);
        jgen.writeEndArray();
    }

    @Override
    public final void serializeWithType(T value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        // note: let's NOT consider [JACKSON-805] here; gets too complicated, and probably just won't work
        typeSer.writeTypePrefixForArray(value, jgen);
        serializeContents(value, jgen, provider);
        typeSer.writeTypeSuffixForArray(value, jgen);
    }
    
    protected abstract void serializeContents(T value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException;
}
