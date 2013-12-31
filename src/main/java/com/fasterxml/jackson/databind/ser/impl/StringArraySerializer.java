package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.ArraySerializerBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Standard serializer used for <code>String[]</code> values.
 */
@JacksonStdImpl
public class StringArraySerializer
    extends ArraySerializerBase<String[]>
    implements ContextualSerializer
{
    /* Note: not clean in general, but we are betting against
     * anyone re-defining properties of String.class here...
     */
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
        super(String[].class, null);
        _elementSerializer = null;
    }

    @SuppressWarnings("unchecked")
    public StringArraySerializer(StringArraySerializer src,
            BeanProperty prop, JsonSerializer<?> ser) {
        super(src, prop);
        _elementSerializer = (JsonSerializer<Object>) ser;
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
            AnnotatedMember m = property.getMember();
            if (m != null) {
                Object serDef = provider.getAnnotationIntrospector().findContentSerializer(m);
                if (serDef != null) {
                    ser = provider.serializerInstance(m, serDef);
                }
            }
        }
        if (ser == null) {
            ser = _elementSerializer;
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
        if (ser == _elementSerializer) {
            return this;
        }
        return new StringArraySerializer(this, property, ser);
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
    public boolean isEmpty(String[] value) {
        return (value == null) || (value.length == 0);
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
    public void serializeContents(String[] value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        final int len = value.length;
        if (len == 0) {
            return;
        }
        if (_elementSerializer != null) {
            serializeContentsSlow(value, jgen, provider, _elementSerializer);
            return;
        }
        /* 08-Dec-2008, tatus: If we want this to be fully overridable
         *  (for example, to support String cleanup during writing
         *  or something), we should find serializer  by provider.
         *  But for now, that seems like an overkill: and caller can
         *  add custom serializer if that is needed as well.
         * (ditto for null values)
         */
        //JsonSerializer<String> ser = (JsonSerializer<String>)provider.findValueSerializer(String.class);
        for (int i = 0; i < len; ++i) {
            String str = value[i];
            if (str == null) {
                jgen.writeNull();
            } else {
                //ser.serialize(value[i], jgen, provider);
                jgen.writeString(value[i]);
            }
        }
    }

    private void serializeContentsSlow(String[] value, JsonGenerator jgen, SerializerProvider provider, JsonSerializer<Object> ser)
        throws IOException, JsonGenerationException
    {
        for (int i = 0, len = value.length; i < len; ++i) {
            String str = value[i];
            if (str == null) {
                provider.defaultSerializeNull(jgen);
            } else {
                ser.serialize(value[i], jgen, provider);
            }
        }
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
        return createSchemaNode("array", true).set("items", createSchemaNode("string"));
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
    {
        if (visitor != null) {
            JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
            if (v2 != null) {
                v2.itemsFormat(JsonFormatTypes.STRING);
            }
        }
    }
}
