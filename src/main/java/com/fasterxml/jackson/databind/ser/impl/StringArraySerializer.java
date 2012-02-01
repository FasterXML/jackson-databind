package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.ser.std.ArraySerializerBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Standard serializer used for <code>String[]</code> values.
 */
@JacksonStdImpl
public class StringArraySerializer
    extends ArraySerializerBase<String[]>
    implements ResolvableSerializer
{
    /* Note: not clean in general, but we are betting against
     * anyone re-defining properties of String.class here...
     */
    private final static JavaType VALUE_TYPE = TypeFactory.defaultInstance().uncheckedSimpleType(String.class);
        
    /**
     * Value serializer to use, if it's not the standard one
     * (if it is we can optimize serialization a lot)
     */
    protected JsonSerializer<Object> _elementSerializer;

    public StringArraySerializer(BeanProperty prop) {
        super(String[].class, null, prop);
    }

    /**
     * Strings never add type info; hence, even if type serializer is suggested,
     * we'll ignore it...
     */
    @Override
    public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return this;
    }

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

    private void serializeContentsSlow(String[] value, JsonGenerator jgen, SerializerProvider provider,
            JsonSerializer<Object> ser)
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

    /**
     * Need to get callback to resolve value serializer, which may
     * be overridden by custom serializer
     */
    @Override
    public void resolve(SerializerProvider provider)
        throws JsonMappingException
    {
        JsonSerializer<Object> ser = provider.findValueSerializer(String.class, _property);
        // Retain if not the standard implementation
        if (ser != null && ser.getClass().getAnnotation(JacksonStdImpl.class) == null) {
            _elementSerializer = ser;
        }
    }        
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        ObjectNode o = createSchemaNode("array", true);
        o.put("items", createSchemaNode("string"));
        return o;
    }
}
