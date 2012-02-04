package com.fasterxml.jackson.databind.ser;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.impl.UnwrappingBeanSerializer;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.util.NameTransformer;


/**
 * Serializer class that can serialize Java objects that map
 * to JSON Object output. Internally handling is mostly dealt with
 * by a sequence of {@link BeanPropertyWriter}s that will handle
 * access value to serialize and call appropriate serializers to
 * write out JSON.
 *<p>
 * Implementation note: we will post-process resulting serializer,
 * to figure out actual serializers for final types. This must be
 * done from {@link #resolve} method, and NOT from constructor;
 * otherwise we could end up with an infinite loop.
 */
public class BeanSerializer
    extends BeanSerializerBase
{
    /*
    /**********************************************************
    /* Life-cycle: constructors
    /**********************************************************
     */

    /**
     * @param type Nominal type of values handled by this serializer
     * @param properties Property writers used for actual serialization
     */
    public BeanSerializer(JavaType type,
            BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties,
            AnyGetterWriter anyGetterWriter, AnnotatedMember typeId,
            Object filterId)
    {
        super(type, properties, filteredProperties, anyGetterWriter, typeId, filterId);
    }

    /**
     * Copy-constructor that is useful for sub-classes that just want to
     * copy all super-class properties without modifications.
     */
    protected BeanSerializer(BeanSerializer src) {
        super(src);
    }

    /**
     * Alternate copy constructor that can be used to construct
     * standard {@link BeanSerializer} passing an instance of
     * "compatible enough" source serializer.
     */
    protected BeanSerializer(BeanSerializerBase src) {
        super(src);
    }
    
    /*
    /**********************************************************
    /* Life-cycle: factory methods, fluent factories
    /**********************************************************
     */

    /**
     * Method for constructing dummy bean deserializer; one that
     * never outputs any properties
     */
    public static BeanSerializer createDummy(JavaType forType)
    {
        return new BeanSerializer(forType, NO_PROPS, null, null, null, null);
    }

    @Override
    public JsonSerializer<Object> unwrappingSerializer(NameTransformer unwrapper) {
        return new UnwrappingBeanSerializer(this, unwrapper);
    }
    
    /*
    /**********************************************************
    /* JsonSerializer implementation that differs between impls
    /**********************************************************
     */

    /**
     * Main serialization method that will delegate actual output to
     * configured
     * {@link BeanPropertyWriter} instances.
     */
    @Override
    public final void serialize(Object bean, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeStartObject();
        if (_propertyFilterId != null) {
            serializeFieldsFiltered(bean, jgen, provider);
        } else {
            serializeFields(bean, jgen, provider);
        }
        jgen.writeEndObject();
    }

    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override public String toString() {
        return "BeanSerializer for "+handledType().getName();
    }
}
