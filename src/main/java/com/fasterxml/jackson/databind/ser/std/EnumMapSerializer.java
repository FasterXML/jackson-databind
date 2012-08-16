package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import com.fasterxml.jackson.core.*;


import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.util.EnumValues;

/**
 * Specialized serializer for {@link EnumMap}s. Somewhat tricky to
 * implement because actual Enum value type may not be available;
 * and if not, it can only be gotten from actual instance.
 */
@JacksonStdImpl
public class EnumMapSerializer
    extends ContainerSerializer<EnumMap<? extends Enum<?>, ?>>
    implements ContextualSerializer
{
    protected final boolean _staticTyping;

    /**
     * Propery for which this serializer is being used, if any;
     * null for root values.
     */
    protected final BeanProperty _property;
    
    /**
     * If we know enumeration used as key, this will contain
     * value set to use for serialization
     */
    protected final EnumValues _keyEnums;

    protected final JavaType _valueType;
    
    /**
     * Value serializer to use, if it can be statically determined
     */
    protected final JsonSerializer<Object> _valueSerializer;

    /**
     * Type serializer used for values, if any.
     */
    protected final TypeSerializer _valueTypeSerializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public EnumMapSerializer(JavaType valueType, boolean staticTyping, EnumValues keyEnums,
            TypeSerializer vts, JsonSerializer<Object> valueSerializer)
    {
        super(EnumMap.class, false);
        _property = null; // not yet known
        _staticTyping = staticTyping || (valueType != null && valueType.isFinal());
        _valueType = valueType;
        _keyEnums = keyEnums;
        _valueTypeSerializer = vts;
        _valueSerializer = valueSerializer;
    }

    /**
     * Constructor called when a contextual instance is created.
     */
    @SuppressWarnings("unchecked")
    public EnumMapSerializer(EnumMapSerializer src, BeanProperty property,
            JsonSerializer<?> ser)
    {
        super(src);
        _property = property;
        _staticTyping = src._staticTyping;
        _valueType = src._valueType;
        _keyEnums = src._keyEnums;
        _valueTypeSerializer = src._valueTypeSerializer;
        _valueSerializer = (JsonSerializer<Object>) ser;
    }
    
    @Override
    public EnumMapSerializer _withValueTypeSerializer(TypeSerializer vts) {
        return new EnumMapSerializer(_valueType, _staticTyping, _keyEnums, vts,  _valueSerializer);
    }

    public EnumMapSerializer withValueSerializer(BeanProperty prop, JsonSerializer<?> ser) {
        if (_property == prop && ser == _valueSerializer) {
            return this;
        }
        return new EnumMapSerializer(this, prop, ser);
    }
    
//  @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property)
        throws JsonMappingException
    {
        if (_valueSerializer == null) {
            if (_staticTyping) {
                return withValueSerializer(property, provider.findValueSerializer(_valueType, property));
            }
        } else if (_valueSerializer instanceof ContextualSerializer) {
            return withValueSerializer(property, ((ContextualSerializer) _valueSerializer)
                    .createContextual(provider, property));
        }
        return this;
    }
    
    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */
    
    @Override
    public JavaType getContentType() {
        return _valueType;
    }

    @Override
    public JsonSerializer<?> getContentSerializer() {
        return _valueSerializer;
    }
    
    @Override
    public boolean isEmpty(EnumMap<? extends Enum<?>,?> value) {
        return (value == null) || value.isEmpty();
    }

    @Override
    public boolean hasSingleElement(EnumMap<? extends Enum<?>, ?> value) {
        return value.size() == 1;
    }
    
    /*
    /**********************************************************
    /* Serialization
    /**********************************************************
     */
    
    @Override
    public void serialize(EnumMap<? extends Enum<?>,?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeStartObject();
        if (!value.isEmpty()) {
            serializeContents(value, jgen, provider);
        }        
        jgen.writeEndObject();
    }

    @Override
    public void serializeWithType(EnumMap<? extends Enum<?>,?> value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        typeSer.writeTypePrefixForObject(value, jgen);
        if (!value.isEmpty()) {
            serializeContents(value, jgen, provider);
        }
        typeSer.writeTypeSuffixForObject(value, jgen);
    }
    
    protected void serializeContents(EnumMap<? extends Enum<?>,?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        if (_valueSerializer != null) {
            serializeContentsUsing(value, jgen, provider, _valueSerializer);
            return;
        }
        JsonSerializer<Object> prevSerializer = null;
        Class<?> prevClass = null;
        EnumValues keyEnums = _keyEnums;

        for (Map.Entry<? extends Enum<?>,?> entry : value.entrySet()) {
            // First, serialize key
            Enum<?> key = entry.getKey();
            if (keyEnums == null) {
                /* 15-Oct-2009, tatu: This is clumsy, but still the simplest efficient
                 * way to do it currently, as Serializers get cached. (it does assume we'll always use
                 * default serializer tho -- so ideally code should be rewritten)
                 */
                // ... and lovely two-step casting process too...
                StdSerializer<?> ser = (StdSerializer<?>) provider.findValueSerializer(
                        key.getDeclaringClass(), _property);
                keyEnums = ((EnumSerializer) ser).getEnumValues();
            }
            jgen.writeFieldName(keyEnums.serializedValueFor(key));
            // And then value
            Object valueElem = entry.getValue();
            if (valueElem == null) {
                provider.defaultSerializeNull(jgen);
            } else {
                Class<?> cc = valueElem.getClass();
                JsonSerializer<Object> currSerializer;
                if (cc == prevClass) {
                    currSerializer = prevSerializer;
                } else {
                    currSerializer = provider.findValueSerializer(cc, _property);
                    prevSerializer = currSerializer;
                    prevClass = cc;
                }
                try {
                    currSerializer.serialize(valueElem, jgen, provider);
                } catch (Exception e) {
                    // [JACKSON-55] Need to add reference information
                    wrapAndThrow(provider, e, value, entry.getKey().name());
                }
            }
        }
    }

    protected void serializeContentsUsing(EnumMap<? extends Enum<?>,?> value, JsonGenerator jgen, SerializerProvider provider,
            JsonSerializer<Object> valueSer)
        throws IOException, JsonGenerationException
    {
        EnumValues keyEnums = _keyEnums;
        for (Map.Entry<? extends Enum<?>,?> entry : value.entrySet()) {
            Enum<?> key = entry.getKey();
            if (keyEnums == null) {
                // clumsy, but has to do for now:
                StdSerializer<?> ser = (StdSerializer<?>) provider.findValueSerializer(key.getDeclaringClass(),
                        _property);
                keyEnums = ((EnumSerializer) ser).getEnumValues();
            }
            jgen.writeFieldName(keyEnums.serializedValueFor(key));
            Object valueElem = entry.getValue();
            if (valueElem == null) {
                provider.defaultSerializeNull(jgen);
            } else {
                try {
                    valueSer.serialize(valueElem, jgen, provider);
                } catch (Exception e) {
                    wrapAndThrow(provider, e, value, entry.getKey().name());
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        ObjectNode o = createSchemaNode("object", true);
        if (typeHint instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) typeHint).getActualTypeArguments();
            if (typeArgs.length == 2) {
                JavaType enumType = provider.constructType(typeArgs[0]);
                JavaType valueType = provider.constructType(typeArgs[1]);
                ObjectNode propsNode = JsonNodeFactory.instance.objectNode();
                Class<Enum<?>> enumClass = (Class<Enum<?>>) enumType.getRawClass();
                for (Enum<?> enumValue : enumClass.getEnumConstants()) {
                    JsonSerializer<Object> ser = provider.findValueSerializer(valueType.getRawClass(), _property);
                    JsonNode schemaNode = (ser instanceof SchemaAware) ?
                            ((SchemaAware) ser).getSchema(provider, null) :
                            JsonSchema.getDefaultSchemaNode();
                    propsNode.put(provider.getConfig().getAnnotationIntrospector().findEnumValue((Enum<?>)enumValue), schemaNode);
                }
                o.put("properties", propsNode);
            }
        }
        return o;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
    	JsonObjectFormatVisitor objectVisitor = visitor.expectObjectFormat(typeHint);
        if (typeHint instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) typeHint).getActualTypeArguments();
            if (typeArgs.length == 2) {
                JavaType enumType = visitor.getProvider().constructType(typeArgs[0]);
                JavaType valueType = visitor.getProvider().constructType(typeArgs[1]);
//                ObjectNode propsNode = JsonNodeFactory.instance.objectNode();
                Class<Enum<?>> enumClass = (Class<Enum<?>>) enumType.getRawClass();
                for (Enum<?> enumValue : enumClass.getEnumConstants()) {
                	JsonSerializer<Object> ser;
                	String name = visitor.getProvider().getConfig().getAnnotationIntrospector().findEnumValue((Enum<?>)enumValue);
                	try {
                		ser = visitor.getProvider().findValueSerializer(valueType.getRawClass(), _property);
                		if (ser instanceof JsonFormatVisitable)  {
                			objectVisitor.property(name, (JsonFormatVisitable) ser, valueType);
                		} 
                		continue;
                	} catch (JsonMappingException e) {
                		//TODO: log error
                	}
                	objectVisitor.property(name);
                }
            }
        }
    }
}
