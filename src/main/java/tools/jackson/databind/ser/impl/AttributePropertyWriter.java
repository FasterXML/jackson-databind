package tools.jackson.databind.ser.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.ser.VirtualBeanPropertyWriter;
import tools.jackson.databind.util.Annotations;

/**
 * {@link VirtualBeanPropertyWriter} implementation used for
 * {@link tools.jackson.databind.annotation.JsonAppend},
 * to serialize properties backed-by dynamically assignable attribute
 * values.
 *
 * @since 2.5
 */
public class AttributePropertyWriter
    extends VirtualBeanPropertyWriter
{
    private static final long serialVersionUID = 1;

    protected final String _attrName;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected AttributePropertyWriter(String attrName, BeanPropertyDefinition propDef,
            Annotations contextAnnotations, JavaType declaredType) {
        this(attrName, propDef, contextAnnotations, declaredType, propDef.findInclusion());
    }

    protected AttributePropertyWriter(String attrName, BeanPropertyDefinition propDef,
            Annotations contextAnnotations, JavaType declaredType,
            JsonInclude.Value inclusion)
    {
        super(propDef, contextAnnotations, declaredType,
                /* value serializer */ null, /* type serializer */ null, /* ser type */ null,
                inclusion,
                // 10-Oct-2016, tatu: Could enable per-view settings too in future
                null);
        _attrName = attrName;
    }

    public static AttributePropertyWriter construct(String attrName,
            BeanPropertyDefinition propDef,
            Annotations contextAnnotations,
            JavaType declaredType)
    {
        return new AttributePropertyWriter(attrName, propDef,
                contextAnnotations, declaredType);
    }

    protected AttributePropertyWriter(AttributePropertyWriter base) {
        super(base);
        _attrName = base._attrName;
    }

    /**
     * Since this method should typically not be called on this sub-type,
     * default implementation simply throws an {@link IllegalStateException}.
     */
    @Override
    public VirtualBeanPropertyWriter withConfig(MapperConfig<?> config,
            AnnotatedClass declaringClass, BeanPropertyDefinition propDef, JavaType type) {
        throw new IllegalStateException("Should not be called on this type");
    }

    /*
    /**********************************************************
    /* Overrides for actual serialization, value access
    /**********************************************************
     */

    @Override
    protected Object value(Object bean, JsonGenerator jgen, SerializerProvider prov) throws Exception {
        return prov.getAttribute(_attrName);
    }
}
