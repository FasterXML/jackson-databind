package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Intermediate base class for Lists, Collections and Arrays
 * that contain static (non-dynamic) value types.
 */
@SuppressWarnings("serial")
public abstract class StaticListSerializerBase<T extends Collection<?>>
    extends StdSerializer<T>
{
    /**
     * Setting for specific local override for "unwrap single element arrays":
     * true for enable unwrapping, false for preventing it, `null` for using
     * global configuration.
     */
    protected final Boolean _unwrapSingle;

    protected StaticListSerializerBase(Class<?> cls) {
        super(cls);
        _unwrapSingle = null;
    }

    protected StaticListSerializerBase(StaticListSerializerBase<?> src,
            Boolean unwrapSingle) {
        super(src);
        _unwrapSingle = unwrapSingle;
    }

    public abstract JsonSerializer<?> _withResolved(BeanProperty prop,
            Boolean unwrapSingle);

    /*
    /**********************************************************
    /* Post-processing
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializers,
            BeanProperty property)
        throws JsonMappingException
    {
        JsonSerializer<?> ser = null;
        
        if (property != null) {
            final AnnotationIntrospector intr = serializers.getAnnotationIntrospector();
            AnnotatedMember m = property.getMember();
            if (m != null) {
                ser = serializers.serializerInstance(m,
                        intr.findContentSerializer(serializers.getConfig(), m));
            }
        }
        Boolean unwrapSingle = null;
        JsonFormat.Value format = findFormatOverrides(serializers, property, handledType());
        if (format != null) {
            unwrapSingle = format.getFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        }
        // [databind#124]: May have a content converter
        ser = findContextualConvertingSerializer(serializers, property, ser);
        if (ser == null) {
            ser = serializers.findSecondaryPropertySerializer(String.class, property);
        }
        // Optimization: default serializer just writes String, so we can avoid a call:
        if (isDefaultSerializer(ser)) {
            if (unwrapSingle == _unwrapSingle) {
                return this;
            }
            return _withResolved(property, unwrapSingle);
        }
        // otherwise...
        // note: will never have TypeSerializer, because Strings are "natural" type
        return new CollectionSerializer(serializers.constructType(String.class),
                true, /*TypeSerializer*/ null, (JsonSerializer<Object>) ser);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, T value) {
        return (value == null) || (value.size() == 0);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
        JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
        if (v2 != null) {
            acceptContentVisitor(v2);
        }
    }

    /*
    /**********************************************************
    /* Abstract methods for sub-classes to implement
    /**********************************************************
     */

    protected abstract JsonNode contentSchema();

    protected abstract void acceptContentVisitor(JsonArrayFormatVisitor visitor)
        throws JsonMappingException;

    // just to make sure it gets implemented:
    @Override
    public abstract void serializeWithType(T value, JsonGenerator g,
            SerializerProvider provider, TypeSerializer typeSer) throws IOException;
}
