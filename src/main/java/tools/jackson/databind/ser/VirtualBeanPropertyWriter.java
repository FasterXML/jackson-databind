package tools.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.*;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.impl.PropertySerializerMap;
import tools.jackson.databind.util.Annotations;

/**
 * {@link BeanPropertyWriter} implementation used with
 * {@link tools.jackson.databind.annotation.JsonAppend}
 * to add "virtual" properties in addition to regular ones.
 * 
 * @see tools.jackson.databind.ser.impl.AttributePropertyWriter
 */
public abstract class VirtualBeanPropertyWriter
    extends BeanPropertyWriter
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructor used by most sub-types.
     */
    protected VirtualBeanPropertyWriter(BeanPropertyDefinition propDef,
            Annotations contextAnnotations, JavaType declaredType)
    {
        this(propDef, contextAnnotations, declaredType, null, null, null,
                propDef.findInclusion(), null);
    }

    /**
     * Constructor that may be used by sub-classes for constructing a "blue-print" instance;
     * one that will only become (or create) actual usable instance when its
     * {@link #withConfig} method is called.
     */
    protected VirtualBeanPropertyWriter() {
        super();
    }

    /**
     * Pass-through constructor that may be used by sub-classes that
     * want full control over implementation.
     */
    protected VirtualBeanPropertyWriter(BeanPropertyDefinition propDef,
            Annotations contextAnnotations, JavaType declaredType,
            ValueSerializer<?> ser, TypeSerializer typeSer, JavaType serType,
            JsonInclude.Value inclusion, Class<?>[] includeInViews)
    {
        super(propDef, propDef.getPrimaryMember(), contextAnnotations, declaredType,
                ser, typeSer, serType,
                _suppressNulls(inclusion), _suppressableValue(inclusion),
                includeInViews);
    }

    protected VirtualBeanPropertyWriter(VirtualBeanPropertyWriter base) {
        super(base);
    }

    protected VirtualBeanPropertyWriter(VirtualBeanPropertyWriter base, PropertyName name) {
        super(base, name);
    }

    protected static boolean _suppressNulls(JsonInclude.Value inclusion) {
        if (inclusion == null) {
            return false;
        }
        JsonInclude.Include incl = inclusion.getValueInclusion();
        return (incl != JsonInclude.Include.ALWAYS) && (incl != JsonInclude.Include.USE_DEFAULTS);
    }

    protected static Object _suppressableValue(JsonInclude.Value inclusion) {
        if (inclusion == null) {
            return false;
        }
        JsonInclude.Include incl = inclusion.getValueInclusion();
        if ((incl == JsonInclude.Include.ALWAYS)
                || (incl == JsonInclude.Include.NON_NULL)
                || (incl == JsonInclude.Include.USE_DEFAULTS)) {
            return null;
        }
        return MARKER_FOR_EMPTY;
    }

    /*
    /**********************************************************************
    /* Standard accessor overrides
    /**********************************************************************
     */

    @Override
    public boolean isVirtual() { return true; }

    /*
    /**********************************************************************
    /* Abstract methods for sub-classes to define
    /**********************************************************************
     */

    /**
     * Method called to figure out the value to serialize. For simple sub-types
     * (such as {@link tools.jackson.databind.ser.impl.AttributePropertyWriter})
     * this may be one of few methods to define, although more advanced implementations
     * may choose to not even use this method (by overriding {@link #serializeAsProperty})
     * and define a bogus implementation.
     */
    protected abstract Object value(Object bean, JsonGenerator g, SerializerProvider prov) throws Exception;

    /**
     * Contextualization method called on a newly constructed virtual bean property.
     * Usually a new intance needs to be created due to finality of some of configuration
     * members; otherwise while recommended, creating a new instance is not strictly-speaking
     * mandatory because calls are made in thread-safe manner, as part of initialization
     * before use.
     *
     * @param config Currenct configuration; guaranteed to be {@link SerializationConfig}
     *   (just not typed since caller does not have dependency to serialization-specific types)
     * @param declaringClass Class that contains this property writer
     * @param propDef Nominal property definition to use
     * @param type Declared type for the property
     */
    public abstract VirtualBeanPropertyWriter withConfig(MapperConfig<?> config,
            AnnotatedClass declaringClass, BeanPropertyDefinition propDef, JavaType type);

    /*
    /**********************************************************************
    /* PropertyWriter serialization method overrides
    /**********************************************************************
     */
    
    @Override
    public void serializeAsProperty(Object bean, JsonGenerator g, SerializerProvider prov)
        throws Exception
    {
        // NOTE: mostly copied from base class, but off-lined get() access
        final Object value = value(bean, g, prov);

        if (value == null) {
            if (_nullSerializer != null) {
                g.writeName(_name);
                _nullSerializer.serialize(null, g, prov);
            }
            return;
        }
        ValueSerializer<Object> ser = _serializer;
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
                if (ser.isEmpty(prov, value)) {
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                return;
            }
        }
        if (value == bean) { // simple check for direct cycles
            // four choices: exception; handled by call; or pass-through; write null
            if (_handleSelfReference(bean, g, prov, ser)) {
                return;
            }
        }
        g.writeName(_name);
        if (_typeSerializer == null) {
            ser.serialize(value, g, prov);
        } else {
            ser.serializeWithType(value, g, prov, _typeSerializer);
        }
    }

    // This one's fine as-is from base class
    //public void serializeAsOmittedProperty(Object bean, JsonGenerator g, SerializerProvider prov) throws Exception
    
    @Override
    public void serializeAsElement(Object bean, JsonGenerator g, SerializerProvider prov)
        throws Exception
    {
        // NOTE: mostly copied from base class, but off-lined get() access
        final Object value = value(bean, g, prov);

        if (value == null) {
            if (_nullSerializer != null) {
                _nullSerializer.serialize(null, g, prov);
            } else {
                g.writeNull();
            }
            return;
        }
        ValueSerializer<Object> ser = _serializer;
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
                    serializeAsOmittedElement(bean, g, prov);
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                serializeAsOmittedElement(bean, g, prov);
                return;
            }
        }
        if (value == bean) {
            if (_handleSelfReference(bean, g, prov, ser)) {
                return;
            }
        }
        if (_typeSerializer == null) {
            ser.serialize(value, g, prov);
        } else {
            ser.serializeWithType(value, g, prov, _typeSerializer);
        }
    }

    // This one's fine as-is from base class
    //public void serializeAsOmittedElement(Object bean, JsonGenerator g, SerializerProvider prov)
}
