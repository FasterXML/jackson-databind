package com.fasterxml.jackson.databind.ser;

import java.lang.reflect.Type;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
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
    protected VirtualBeanPropertyWriter(BeanPropertyDefinition propDef,
            Annotations contextAnnotations, JavaType declaredType,
            JsonSerializer<?> ser, TypeSerializer typeSer, JavaType serType) {
        this(propDef, contextAnnotations, declaredType, ser, typeSer, serType,
                propDef.findInclusion());
    }

    /**
     * Pass-through constructor that may be used by sub-classes that
     * want full control over implementation.
     */
    protected VirtualBeanPropertyWriter(BeanPropertyDefinition propDef,
            Annotations contextAnnotations, JavaType declaredType,
            JsonSerializer<?> ser, TypeSerializer typeSer, JavaType serType,
            JsonInclude.Include inclusion)
    {
        super(propDef, propDef.getPrimaryMember(), contextAnnotations, declaredType,
                ser, typeSer, serType,
                _suppressNulls(inclusion), _suppressableValue(inclusion));
    }

    protected VirtualBeanPropertyWriter(VirtualBeanPropertyWriter base) {
        super(base);
    }

    protected VirtualBeanPropertyWriter(VirtualBeanPropertyWriter base, PropertyName name) {
        super(base, name);
    }

    protected static boolean _suppressNulls(JsonInclude.Include inclusion) {
        return (inclusion != JsonInclude.Include.ALWAYS);
    }

    protected static Object _suppressableValue(JsonInclude.Include inclusion) {
        if ((inclusion == JsonInclude.Include.NON_EMPTY)
                || (inclusion == JsonInclude.Include.NON_EMPTY)) {
            return MARKER_FOR_EMPTY;
        }
        return null;
    }
    
    /*
    /**********************************************************
    /* Standard accessor overrides
    /**********************************************************
     */

    @Override
    public boolean isVirtual() { return true; }

    @Override
    public Class<?> getPropertyType() {
        return _declaredType.getRawClass();
    }

    @Override
    public Type getGenericPropertyType() {
        return getPropertyType();
    }

    /*
    /**********************************************************
    /* Abstract methods for sub-classes to define
    /**********************************************************
     */

    /**
     * Method called to figure out the value to serialize. For simple sub-types
     * (such as {@link com.fasterxml.jackson.databind.ser.impl.AttributePropertyWriter})
     * this may be one of few methods to define, although more advanced implementations
     * may choose to not even use this method (by overriding {@link #serializeAsField})
     * and define a bogus implementation.
     */
    protected abstract Object value(Object bean, JsonGenerator jgen, SerializerProvider prov) throws Exception;

    /**
     * Contextualization method called on a newly constructed virtual bean property.
     * If information is used to change behavior, it is recommended that a new instance
     * is constructed with given information and returned; however, this is not mandatory
     * and modifying (and returning) this instance is legal as well as calls are made
     * in thread-safe manner.
     *
     * @param config Currenct configuration; guaranteed to be {@link SerializationConfig}
     *   (but not typed since caller does not have full typing)
     * @param declaringClass Class that contains this property writer
     * @param propDef Nominal property definition to use
     * @param type Declared type for the property
     */
    public abstract VirtualBeanPropertyWriter withConfig(MapperConfig<?> config,
            AnnotatedClass declaringClass, BeanPropertyDefinition propDef, JavaType type);

    /*
    /**********************************************************
    /* PropertyWriter serialization method overrides
    /**********************************************************
     */
    
    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception
    {
        // NOTE: mostly copied from base class, but off-lined get() access
        final Object value = value(bean, gen, prov);

        if (value == null) {
            if (_nullSerializer != null) {
                gen.writeFieldName(_name);
                _nullSerializer.serialize(null, gen, prov);
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
            if (_handleSelfReference(bean, gen, prov, ser)) {
                return;
            }
        }
        gen.writeFieldName(_name);
        if (_typeSerializer == null) {
            ser.serialize(value, gen, prov);
        } else {
            ser.serializeWithType(value, gen, prov, _typeSerializer);
        }
    }

    // This one's fine as-is from base class
    //public void serializeAsOmittedField(Object bean, JsonGenerator jgen, SerializerProvider prov) throws Exception
    
    @Override
    public void serializeAsElement(Object bean, JsonGenerator gen, SerializerProvider prov)
        throws Exception
    {
        // NOTE: mostly copied from base class, but off-lined get() access
        final Object value = value(bean, gen, prov);

        if (value == null) {
            if (_nullSerializer != null) {
                _nullSerializer.serialize(null, gen, prov);
            } else {
                gen.writeNull();
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
                    serializeAsPlaceholder(bean, gen, prov);
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                serializeAsPlaceholder(bean, gen, prov);
                return;
            }
        }
        if (value == bean) {
            if (_handleSelfReference(bean, gen, prov, ser)) {
                return;
            }
        }
        if (_typeSerializer == null) {
            ser.serialize(value, gen, prov);
        } else {
            ser.serializeWithType(value, gen, prov, _typeSerializer);
        }
    }

    // This one's fine as-is from base class
    //public void serializeAsPlaceholder(Object bean, JsonGenerator jgen, SerializerProvider prov)
}
