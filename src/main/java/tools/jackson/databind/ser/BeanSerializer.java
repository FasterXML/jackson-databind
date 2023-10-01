package tools.jackson.databind.ser;

import java.util.Set;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.ser.bean.BeanAsArraySerializer;
import tools.jackson.databind.ser.bean.BeanSerializerBase;
import tools.jackson.databind.ser.bean.UnwrappingBeanSerializer;
import tools.jackson.databind.ser.impl.ObjectIdWriter;
import tools.jackson.databind.util.NameTransformer;

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
@JacksonStdImpl
public class BeanSerializer
    extends BeanSerializerBase
{
    /*
    /**********************************************************************
    /* Life-cycle: constructors
    /**********************************************************************
     */

    /**
     * @param builder Builder object that contains collected information
     *   that may be needed for serializer
     * @param properties Property writers used for actual serialization
     */
    public BeanSerializer(JavaType type, BeanSerializerBuilder builder,
            BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties)
    {
        super(type, builder, properties, filteredProperties);
    }

    /**
     * Alternate copy constructor that can be used to construct
     * standard {@link BeanSerializer} passing an instance of
     * "compatible enough" source serializer.
     */
    protected BeanSerializer(BeanSerializerBase src) {
        super(src);
    }

    protected BeanSerializer(BeanSerializerBase src,
            ObjectIdWriter objectIdWriter) {
        super(src, objectIdWriter);
    }

    protected BeanSerializer(BeanSerializerBase src,
            ObjectIdWriter objectIdWriter, Object filterId) {
        super(src, objectIdWriter, filterId);
    }

    protected BeanSerializer(BeanSerializerBase src, Set<String> toIgnore, Set<String> toInclude) {
        super(src, toIgnore, toInclude);
    }

    protected BeanSerializer(BeanSerializerBase src,
            BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties) {
        super(src, properties, filteredProperties);
    }

    /*
    /**********************************************************************
    /* Life-cycle: factory methods, fluent factories
    /**********************************************************************
     */

    /**
     * Method for constructing dummy bean serializer; one that
     * never outputs any properties
     */
    public static BeanSerializer createDummy(JavaType forType, BeanSerializerBuilder builder)
    {
        return new BeanSerializer(forType, builder, NO_PROPS, null);
    }

    @Override
    public ValueSerializer<Object> unwrappingSerializer(NameTransformer unwrapper) {
        return new UnwrappingBeanSerializer(this, unwrapper);
    }

    @Override
    public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
        return new BeanSerializer(this, objectIdWriter, _propertyFilterId);
    }

    @Override
    public BeanSerializerBase withFilterId(Object filterId) {
        return new BeanSerializer(this, _objectIdWriter, filterId);
    }

    @Override
    protected BeanSerializerBase withByNameInclusion(Set<String> toIgnore, Set<String> toInclude) {
        return new BeanSerializer(this, toIgnore, toInclude);
    }

    @Override
    protected BeanSerializerBase withProperties(BeanPropertyWriter[] properties,
            BeanPropertyWriter[] filteredProperties) {
        return new BeanSerializer(this, properties, filteredProperties);
    }

    @Override
    public ValueSerializer<?> withIgnoredProperties(Set<String> toIgnore) {
        return new BeanSerializer(this, toIgnore, null);
    }

    /**
     * Implementation has to check whether as-array serialization
     * is possible reliably; if (and only if) so, will construct
     * a {@link BeanAsArraySerializer}, otherwise will return this
     * serializer as is.
     */
    @Override
    protected BeanSerializerBase asArraySerializer()
    {
        if (canCreateArraySerializer()) {
            return BeanAsArraySerializer.construct(this);
        }
        // already is one, so:
        return this;
    }

    /*
    /**********************************************************************
    /* ValueSerializer implementation that differs between impls
    /**********************************************************************
     */

    /**
     * Main serialization method that will delegate actual output to
     * configured
     * {@link BeanPropertyWriter} instances.
     */
    @Override
    public final void serialize(Object bean, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        if (_objectIdWriter != null) {
            _serializeWithObjectId(bean, gen, provider, true);
            return;
        }
        if (_propertyFilterId != null) {
            gen.writeStartObject(bean);
            _serializePropertiesFiltered(bean, gen, provider, _propertyFilterId);
            gen.writeEndObject();
            return;
        }
        BeanPropertyWriter[] fProps = _filteredProps;
        if ((fProps != null) && (provider.getActiveView() != null)) {
            gen.writeStartObject(bean);
            _serializePropertiesMaybeView(bean, gen, provider, fProps);
            gen.writeEndObject();
            return;
        }
        gen.writeStartObject(bean);
        _serializePropertiesNoView(bean, gen, provider, _props);
        gen.writeEndObject();
    }
}
