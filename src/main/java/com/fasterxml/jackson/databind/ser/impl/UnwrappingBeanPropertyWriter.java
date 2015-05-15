package com.fasterxml.jackson.databind.ser.impl;

import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Variant of {@link BeanPropertyWriter} which will handle unwrapping
 * of JSON Object (including of properties of Object within surrounding
 * JSON object, and not as sub-object).
 */
public class UnwrappingBeanPropertyWriter
    extends BeanPropertyWriter
{
    /**
     * Transformer used to add prefix and/or suffix for properties
     * of unwrapped POJO.
     */
    protected final NameTransformer _nameTransformer;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public UnwrappingBeanPropertyWriter(BeanPropertyWriter base, NameTransformer unwrapper) {
        super(base);
        _nameTransformer = unwrapper;
    }

    private UnwrappingBeanPropertyWriter(UnwrappingBeanPropertyWriter base, NameTransformer transformer,
            SerializedString name) {
        super(base, name);
        _nameTransformer = transformer;
    }

    @Override
    public UnwrappingBeanPropertyWriter rename(NameTransformer transformer)
    {
        String oldName = _name.getValue();
        String newName = transformer.transform(oldName);

        // important: combine transformers:
        transformer = NameTransformer.chainedTransformer(transformer, _nameTransformer);
    
        return new UnwrappingBeanPropertyWriter(this, transformer, new SerializedString(newName));
    }

    /*
    /**********************************************************
    /* Overrides, public methods
    /**********************************************************
     */

    @Override
    public boolean isUnwrapping() {
        return true;
    }
    
    @Override
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
            throws Exception
    {
        final Object value = get(bean);
        if (value == null) {
            // Hmmh. I assume we MUST pretty much suppress nulls, since we
            // can't really unwrap them...
            return;
        }
        JsonSerializer<Object> ser = _serializer;
        if (ser == null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap map = _dynamicSerializers;
            ser = map.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(map, cls, prov);
            }
        }
        if (_suppressableValue != null) {
            if (MARKER_FOR_EMPTY == _suppressableValue) {
                if (ser.isEmpty(prov, value)) {
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                return;
            }
        }
        // For non-nulls, first: simple check for direct cycles
        if (value == bean) {
            if (_handleSelfReference(bean, jgen, prov, ser)) {
                return;
            }
        }

        // note: must verify we are using unwrapping serializer; if not, will write field name
        if (!ser.isUnwrappingSerializer()) {
            jgen.writeFieldName(_name);
        }

        if (_typeSerializer == null) {
            ser.serialize(value, jgen, prov);
        } else {
            ser.serializeWithType(value, jgen, prov, _typeSerializer);
        }
    }

    // need to override as we must get unwrapping instance...
    @Override
    public void assignSerializer(JsonSerializer<Object> ser)
    {
        super.assignSerializer(ser);
        if (_serializer != null) {
            NameTransformer t = _nameTransformer;
            if (_serializer.isUnwrappingSerializer()) {
                t = NameTransformer.chainedTransformer(t, ((UnwrappingBeanSerializer) _serializer)._nameTransformer);
            }
            _serializer = _serializer.unwrappingSerializer(t);
        }
    }

    /*
    /**********************************************************
    /* Overrides: schema generation
    /**********************************************************
     */

    @Override
    public void depositSchemaProperty(final JsonObjectFormatVisitor visitor)
            throws JsonMappingException {
        SerializerProvider provider = visitor.getProvider();
        JsonSerializer<Object> ser = provider
                .findValueSerializer(this.getType(), this)
                .unwrappingSerializer(_nameTransformer);

        if (ser.isUnwrappingSerializer()) {
            ser.acceptJsonFormatVisitor(new JsonFormatVisitorWrapper.Base(provider) {
                // an unwrapping serializer will always expect ObjectFormat,
                // hence, the other cases do not have to be implemented
                @Override
                public JsonObjectFormatVisitor expectObjectFormat(JavaType type)
                        throws JsonMappingException {
                    return visitor;
                }
            }, this.getType());
        } else {
            super.depositSchemaProperty(visitor);
        }
    }

    // Override needed to support legacy JSON Schema generator
    @Override
    protected void _depositSchemaProperty(ObjectNode propertiesNode, JsonNode schemaNode)
    {
        JsonNode props = schemaNode.get("properties");
        if (props != null) {
            Iterator<Entry<String, JsonNode>> it = props.fields();
            while (it.hasNext()) {
                Entry<String,JsonNode> entry = it.next();
                String name = entry.getKey();
                if (_nameTransformer != null) {
                    name = _nameTransformer.transform(name);
                }
                propertiesNode.set(name, entry.getValue());
            }
        }
    }

    /*
    /**********************************************************
    /* Overrides: internal, other
    /**********************************************************
     */
    
    // need to override as we must get unwrapping instance...
    @Override
    protected JsonSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
            Class<?> type, SerializerProvider provider) throws JsonMappingException
    {
        JsonSerializer<Object> serializer;
        if (_nonTrivialBaseType != null) {
            JavaType subtype = provider.constructSpecializedType(_nonTrivialBaseType, type);
            serializer = provider.findValueSerializer(subtype, this);
        } else {
            serializer = provider.findValueSerializer(type, this);
        }
        NameTransformer t = _nameTransformer;
        if (serializer.isUnwrappingSerializer()) {
            t = NameTransformer.chainedTransformer(t, ((UnwrappingBeanSerializer) serializer)._nameTransformer);
        }
        serializer = serializer.unwrappingSerializer(t);
        
        _dynamicSerializers = _dynamicSerializers.newWith(type, serializer);
        return serializer;
    }
}
