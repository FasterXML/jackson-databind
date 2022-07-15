package tools.jackson.databind.ser.bean;

import java.util.Set;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.impl.ObjectIdWriter;
import tools.jackson.databind.util.NameTransformer;

public class UnwrappingBeanSerializer
    extends BeanSerializerBase
{
    /**
     * Transformer used to add prefix and/or suffix for properties
     * of unwrapped POJO.
     */
    protected final NameTransformer _nameTransformer;
    
    /*
    /**********************************************************
    /* Life-cycle: constructors
    /**********************************************************
     */

    /**
     * Constructor used for creating unwrapping instance of a
     * standard <code>BeanSerializer</code>
     */
    public UnwrappingBeanSerializer(BeanSerializerBase src, NameTransformer transformer) {
        super(src, transformer);
        _nameTransformer = transformer;
    }

    public UnwrappingBeanSerializer(UnwrappingBeanSerializer src,
            ObjectIdWriter objectIdWriter) {
        super(src, objectIdWriter);
        _nameTransformer = src._nameTransformer;
    }

    public UnwrappingBeanSerializer(UnwrappingBeanSerializer src,
            ObjectIdWriter objectIdWriter, Object filterId) {
        super(src, objectIdWriter, filterId);
        _nameTransformer = src._nameTransformer;
    }

    protected UnwrappingBeanSerializer(UnwrappingBeanSerializer src, Set<String> toIgnore) {
        this(src, toIgnore, null);
    }

    protected UnwrappingBeanSerializer(UnwrappingBeanSerializer src, Set<String> toIgnore, Set<String> toInclude) {
        super(src, toIgnore, toInclude);
        _nameTransformer = src._nameTransformer;
    }

    // @since 2.11.1
    protected UnwrappingBeanSerializer(UnwrappingBeanSerializer src,
            BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties) {
        super(src, properties, filteredProperties);
        _nameTransformer = src._nameTransformer;
    }

    /*
    /**********************************************************
    /* Life-cycle: factory methods, fluent factories
    /**********************************************************
     */

    @Override
    public ValueSerializer<Object> unwrappingSerializer(NameTransformer transformer) {
        // !!! 23-Jan-2012, tatu: Should we chain transformers?
        return new UnwrappingBeanSerializer(this, transformer);
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return true; // sure is
    }

    @Override
    public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
        return new UnwrappingBeanSerializer(this, objectIdWriter);
    }

    @Override
    public BeanSerializerBase withFilterId(Object filterId) {
        return new UnwrappingBeanSerializer(this, _objectIdWriter, filterId);
    }

    @Override
    protected BeanSerializerBase withByNameInclusion(Set<String> toIgnore, Set<String> toInclude) {
        return new UnwrappingBeanSerializer(this, toIgnore, toInclude);
    }

    @Override
    protected BeanSerializerBase withProperties(BeanPropertyWriter[] properties,
            BeanPropertyWriter[] filteredProperties) {
        return new UnwrappingBeanSerializer(this, properties, filteredProperties);
    }

    /**
     * JSON Array output cannot be done if unwrapping operation is
     * requested; so implementation will simply return 'this'.
     */
    @Override
    protected BeanSerializerBase asArraySerializer() {
        return this;
    }
    
    /*
    /**********************************************************
    /* ValueSerializer implementation that differs between impls
    /**********************************************************
     */

    /**
     * Main serialization method that will delegate actual output to
     * configured
     * {@link BeanPropertyWriter} instances.
     */
    @Override
    public final void serialize(Object bean, JsonGenerator gen, SerializerProvider provider) throws JacksonException
    {
        if (_objectIdWriter != null) {
            _serializeWithObjectId(bean, gen, provider, false);
            return;
        }
        // Because we do not write start-object need to call this explicitly:
        // (although... is that a problem, overwriting it now?)
        gen.assignCurrentValue(bean); // [databind#631]
        if (_propertyFilterId != null) {
            _serializePropertiesFiltered(bean, gen, provider, _propertyFilterId);
            return;
        }
        BeanPropertyWriter[] fProps = _filteredProps;
        if ((fProps != null) && (provider.getActiveView() != null)) {
            _serializePropertiesMaybeView(bean, gen, provider, fProps);
            return;
        }
        _serializePropertiesNoView(bean, gen, provider, _props);
    }

    @Override
    public void serializeWithType(Object bean, JsonGenerator gen, SerializerProvider provider,
    		TypeSerializer typeSer) throws JacksonException
    {
        if (provider.isEnabled(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS)) {
            provider.reportBadDefinition(handledType(),
                    "Unwrapped property requires use of type information: cannot serialize without disabling `SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS`");
        }
        if (_objectIdWriter != null) {
            _serializeWithObjectId(bean, gen, provider, typeSer);
            return;
        }
        // Because we do not write start-object need to call this explicitly:
        gen.assignCurrentValue(bean);
        if (_propertyFilterId != null) {
            _serializePropertiesFiltered(bean, gen, provider, _propertyFilterId);
            return;
        }
        BeanPropertyWriter[] fProps = _filteredProps;
        if ((fProps != null) && (provider.getActiveView() != null)) {
            _serializePropertiesMaybeView(bean, gen, provider, fProps);
            return;
        }
        _serializePropertiesNoView(bean, gen, provider, _props);
    }

    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override public String toString() {
        return "UnwrappingBeanSerializer for "+handledType().getName();
    }
}
