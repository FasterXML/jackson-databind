package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.ArraySerializerBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Standard serializer used for <code>String[]</code> values.
 */
@JacksonStdImpl
@SuppressWarnings("serial")
public class StringArraySerializer
    extends ArraySerializerBase<String[]>
    implements ContextualSerializer
{
    /* Note: not clean in general, but we are betting against
     * anyone re-defining properties of String.class here...
     */
    @SuppressWarnings("deprecation")
    private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(String.class);

    public final static StringArraySerializer instance = new StringArraySerializer();

    /**
     * Value serializer to use, if it's not the standard one
     * (if it is we can optimize serialization a lot)
     */
    protected final JsonSerializer<Object> _elementSerializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected StringArraySerializer() {
        super(String[].class);
        _elementSerializer = null;
    }

    @SuppressWarnings("unchecked")
    public StringArraySerializer(StringArraySerializer src,
            BeanProperty prop, JsonSerializer<?> ser, Boolean unwrapSingle) {
        super(src, prop, unwrapSingle);
        _elementSerializer = (JsonSerializer<Object>) ser;
    }

    @Override
    public JsonSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
        return new StringArraySerializer(this, prop, _elementSerializer, unwrapSingle);
    }

    /**
     * Strings never add type info; hence, even if type serializer is suggested,
     * we'll ignore it...
     */
    @Override
    public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return this;
    }

    /*
    /**********************************************************
    /* Post-processing
    /**********************************************************
     */

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property)
        throws JsonMappingException
    {
        /* 29-Sep-2012, tatu: Actually, we need to do much more contextual
         *    checking here since we finally know for sure the property,
         *    and it may have overrides
         */
        JsonSerializer<?> ser = null;

        // First: if we have a property, may have property-annotation overrides
        if (property != null) {
            final AnnotationIntrospector ai = provider.getAnnotationIntrospector();
            AnnotatedMember m = property.getMember();
            if (m != null) {
                Object serDef = ai.findContentSerializer(m);
                if (serDef != null) {
                    ser = provider.serializerInstance(m, serDef);
                }
            }
        }
        // but since formats have both property overrides and global per-type defaults,
        // need to do that separately
        Boolean unwrapSingle = findFormatFeature(provider, property, String[].class,
                JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        if (ser == null) {
            ser = _elementSerializer;
        }
        // May have a content converter
        ser = findContextualConvertingSerializer(provider, property, ser);
        if (ser == null) {
            ser = provider.findContentValueSerializer(String.class, property);
        }
        // Optimization: default serializer just writes String, so we can avoid a call:
        if (isDefaultSerializer(ser)) {
            ser = null;
        }
        // note: will never have TypeSerializer, because Strings are "natural" type
        if ((ser == _elementSerializer) && (Objects.equals(unwrapSingle, _unwrapSingle))) {
            return this;
        }
        return new StringArraySerializer(this, property, ser, unwrapSingle);
    }

    /*
    /**********************************************************
    /* Simple accessors
    /**********************************************************
     */

    @Override
    public JavaType getContentType() {
        return VALUE_TYPE;
    }

    @Override
    public JsonSerializer<?> getContentSerializer() {
        return _elementSerializer;
    }

    @Override
    public boolean isEmpty(SerializerProvider prov, String[] value) {
        return (value.length == 0);
    }

    @Override
    public boolean hasSingleElement(String[] value) {
        return (value.length == 1);
    }

    /*
    /**********************************************************
    /* Actual serialization
    /**********************************************************
     */

    @Override
    public final void serialize(String[] value, JsonGenerator gen, SerializerProvider provider)
        throws IOException
    {
        final int len = value.length;
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
    public void serializeContents(String[] value, JsonGenerator gen, SerializerProvider provider)
        throws IOException
    {
        final int len = value.length;
        if (len == 0) {
            return;
        }
        if (_elementSerializer != null) {
            serializeContentsSlow(value, gen, provider, _elementSerializer);
            return;
        }
        for (int i = 0; i < len; ++i) {
            String str = value[i];
            if (str == null) {
                gen.writeNull();
            } else {
                gen.writeString(value[i]);
            }
        }
    }

    private void serializeContentsSlow(String[] value, JsonGenerator gen, SerializerProvider provider, JsonSerializer<Object> ser)
        throws IOException
    {
        for (int i = 0, len = value.length; i < len; ++i) {
            String str = value[i];
            if (str == null) {
                provider.defaultSerializeNull(gen);
            } else {
                ser.serialize(value[i], gen, provider);
            }
        }
    }

    /**
     * @deprecated Since 2.15
     */
    @Deprecated
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
        return createSchemaNode("array", true).set("items", createSchemaNode("string"));
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
    {
        visitArrayFormat(visitor, typeHint, JsonFormatTypes.STRING);
    }
}
