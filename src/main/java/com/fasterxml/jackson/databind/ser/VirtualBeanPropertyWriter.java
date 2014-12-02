package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.util.Annotations;

/**
 * {@link BeanPropertyWriter} implementation used with
 * {@link com.fasterxml.jackson.databind.annotation.JsonAppend}
 * to add "virtual" properties in addition to regular ones.
 * 
 * @since 2.5
 */
public abstract class VirtualBeanPropertyWriter
    extends BeanPropertyWriter
{
    /*
    protected VirtualBeanPropertyWriter() {
        
    }
    */

    /**
     * Pass-through constructor that may be used by sub-classes that
     * want full control over implementation.
     */
    protected VirtualBeanPropertyWriter(BeanPropertyDefinition propDef,
            AnnotatedMember member, Annotations contextAnnotations,
            JavaType declaredType,
            JsonSerializer<?> ser, TypeSerializer typeSer, JavaType serType,
            boolean suppressNulls, Object suppressableValue)
    {
        super(propDef, member, contextAnnotations, declaredType,
                ser, typeSer, serType, suppressNulls, suppressableValue);
    }

    /*
    /**********************************************************
    /* PropertyWriter serialization method overrides
    /**********************************************************
     */

    protected abstract Object value(Object bean) throws Exception;
    
    @Override
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov) throws Exception
    {
        // NOTE: mostly copied from base class, but off-lined get() access
        final Object value = value(bean);

        if (value == null) {
            if (_nullSerializer != null) {
                jgen.writeFieldName(_name);
                _nullSerializer.serialize(null, jgen, prov);
            }
            return;
        }
        JsonSerializer<Object> ser = _serializer;
        if (ser == null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap m = _dynamicSerializers;
            ser = m.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(m, cls, prov);
            }
        }
        if (_suppressableValue != null) {
            if (MARKER_FOR_EMPTY == _suppressableValue) {
                if (ser.isEmpty(value)) {
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                return;
            }
        }
        if (value == bean) { // simple check for direct cycles
            // three choices: exception; handled by call; or pass-through
            if (_handleSelfReference(bean, jgen, prov, ser)) {
                return;
            }
        }
        jgen.writeFieldName(_name);
        if (_typeSerializer == null) {
            ser.serialize(value, jgen, prov);
        } else {
            ser.serializeWithType(value, jgen, prov, _typeSerializer);
        }
    }

    // This one's fine as-is from base class
    //public void serializeAsOmittedField(Object bean, JsonGenerator jgen, SerializerProvider prov) throws Exception
    
    @Override
    public void serializeAsElement(Object bean, JsonGenerator jgen, SerializerProvider prov)
        throws Exception
    {
        // NOTE: mostly copied from base class, but off-lined get() access
        final Object value = value(bean);

        if (value == null) {
            if (_nullSerializer != null) {
                _nullSerializer.serialize(null, jgen, prov);
            } else {
                jgen.writeNull();
            }
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
                if (ser.isEmpty(value)) {
                    serializeAsPlaceholder(bean, jgen, prov);
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                serializeAsPlaceholder(bean, jgen, prov);
                return;
            }
        }
        if (value == bean) {
            if (_handleSelfReference(bean, jgen, prov, ser)) {
                return;
            }
        }
        if (_typeSerializer == null) {
            ser.serialize(value, jgen, prov);
        } else {
            ser.serializeWithType(value, jgen, prov, _typeSerializer);
        }
    }

    // This one's fine as-is from base class
    //public void serializeAsPlaceholder(Object bean, JsonGenerator jgen, SerializerProvider prov)
}
