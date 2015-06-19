package com.fasterxml.jackson.databind.ser.std;

import java.lang.reflect.Type;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * Intermediate base class for Lists, Collections and Arrays
 * that contain static (non-dynamic) value types.
 */
@SuppressWarnings("serial")
public abstract class StaticListSerializerBase<T extends Collection<?>>
    extends StdSerializer<T>
    implements ContextualSerializer
{
    protected final JsonSerializer<String> _serializer;

    /**
     * Setting for specific local override for "unwrap single element arrays":
     * true for enable unwrapping, false for preventing it, `null` for using
     * global configuration.
     *
     * @since 2.6
     */
    protected final Boolean _unwrapSingle;
    
    protected StaticListSerializerBase(Class<?> cls) {
        super(cls, false);
        _serializer = null;
        _unwrapSingle = null;
    }

    /**
     * @since 2.6
     */
    @SuppressWarnings("unchecked")
    protected StaticListSerializerBase(StaticListSerializerBase<?> src,
            JsonSerializer<?> ser, Boolean unwrapSingle) {
        super(src);
        _serializer = (JsonSerializer<String>) ser;
        _unwrapSingle = unwrapSingle;
    }

    /**
     * @since 2.6
     */
    public abstract JsonSerializer<?> _withResolved(BeanProperty prop,
            JsonSerializer<?> ser, Boolean unwrapSingle);

    /*
    /**********************************************************
    /* Post-processing
    /**********************************************************
     */

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
            throws JsonMappingException
    {
        JsonSerializer<?> ser = null;
        Boolean unwrapSingle = null;
        
        if (property != null) {
            final AnnotationIntrospector intr = provider.getAnnotationIntrospector();
            AnnotatedMember m = property.getMember();
            if (m != null) {
                Object serDef = intr.findContentSerializer(m);
                if (serDef != null) {
                    ser = provider.serializerInstance(m, serDef);
                }
            }
            JsonFormat.Value format = property.findFormatOverrides(intr);
            if (format != null) {
                unwrapSingle = format.getFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
            }
        }
        if (ser == null) {
            ser = _serializer;
        }
        // #124: May have a content converter
        ser = findConvertingContentSerializer(provider, property, ser);
        if (ser == null) {
            ser = provider.findValueSerializer(String.class, property);
        } else {
            ser = provider.handleSecondaryContextualization(ser, property);
        }
        // Optimization: default serializer just writes String, so we can avoid a call:
        if (isDefaultSerializer(ser)) {
            ser = null;
        }
        // note: will never have TypeSerializer, because Strings are "natural" type
        if ((ser == _serializer) && (unwrapSingle == _unwrapSingle)) {
            return this;
        }
        return _withResolved(property, ser, unwrapSingle);
    }
    
    @Deprecated // since 2.5
    @Override
    public boolean isEmpty(T value) {
        return isEmpty(null, value);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, T value) {
        return (value == null) || (value.size() == 0);
    }
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
        return createSchemaNode("array", true).set("items", contentSchema());
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
        acceptContentVisitor(visitor.expectArrayFormat(typeHint));
    }

    /*
    /**********************************************************
    /* Abstract methods for sub-classes to implement
    /**********************************************************
     */

    protected abstract JsonNode contentSchema();
    
    protected abstract void acceptContentVisitor(JsonArrayFormatVisitor visitor)
        throws JsonMappingException;
}
