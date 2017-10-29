package com.fasterxml.jackson.databind.ser.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.util.NameTransformer;

import java.io.IOException;
import java.util.Set;

public class UnwrappingBeanSerializer
    extends BeanSerializerBase
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

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
        super(src, toIgnore);
        _nameTransformer = src._nameTransformer;
    }

    /*
    /**********************************************************
    /* Life-cycle: factory methods, fluent factories
    /**********************************************************
     */

    @Override
    public JsonSerializer<Object> unwrappingSerializer(NameTransformer transformer) {
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
    protected BeanSerializerBase withIgnorals(Set<String> toIgnore) {
        return new UnwrappingBeanSerializer(this, toIgnore);
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
    /* JsonSerializer implementation that differs between impls
    /**********************************************************
     */

    /**
     * Main serialization method that will delegate actual output to
     * configured
     * {@link BeanPropertyWriter} instances.
     */
    @Override
    public final void serialize(Object bean, JsonGenerator gen, SerializerProvider provider) throws IOException
    {
        if (_objectIdWriter != null) {
            _serializeWithObjectId(bean, gen, provider, false);
            return;
        }
        // Because we do not write start-object need to call this explicitly:
        // (although... is that a problem, overwriting it now?)
        gen.setCurrentValue(bean); // [databind#631]
        if (_propertyFilterId != null) {
            _serializeFieldsFiltered(bean, gen, provider, _propertyFilterId);
            return;
        }
        BeanPropertyWriter[] fProps = _filteredProps;
        if ((fProps != null) && (provider.getActiveView() != null)) {
            _serializeFieldsMaybeView(bean, gen, provider, fProps);
            return;
        }
        _serializeFieldsNoView(bean, gen, provider, _props);
    }

    @Override
    public void serializeWithType(Object bean, JsonGenerator gen, SerializerProvider provider,
    		TypeSerializer typeSer) throws IOException
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
        gen.setCurrentValue(bean);
        if (_propertyFilterId != null) {
            _serializeFieldsFiltered(bean, gen, provider, _propertyFilterId);
            return;
        }
        BeanPropertyWriter[] fProps = _filteredProps;
        if ((fProps != null) && (provider.getActiveView() != null)) {
            _serializeFieldsMaybeView(bean, gen, provider, fProps);
            return;
        }
        _serializeFieldsNoView(bean, gen, provider, _props);
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
