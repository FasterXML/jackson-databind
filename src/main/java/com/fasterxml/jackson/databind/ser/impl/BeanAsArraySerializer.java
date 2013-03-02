package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Specialized POJO serializer that differs from
 * {@link com.fasterxml.jackson.databind.ser.BeanSerializer}
 * in that instead of producing a JSON Object it will output
 * a JSON Array, omitting field names, and serializing values in
 * specified serialization order.
 * This behavior is usually triggered by using annotation
 * {@link com.fasterxml.jackson.annotation.JsonFormat} or its
 * equivalents.
 *<p>
 * This serializer can be used for "simple" instances; and will NOT
 * be used if one of following is true:
 *<ul>
 * <li>Unwrapping is used (no way to expand out array in JSON Object)
 *  </li>
 * <li>Type information ("type id") is to be used: while this could work
 *   for some embedding methods, it would likely cause conflicts.
 *  </li>
 * <li>Object Identity ("object id") is used: while references would work,
 *    the problem is inclusion of id itself.
 *  </li>
 *</ul>
 * Note that it is theoretically possible that last 2 issues could be addressed
 * (by reserving room in array, for example); and if so, support improved.
 *<p>
 * In cases where array-based output is not feasible, this serializer
 * can instead delegate to the original Object-based serializer; this
 * is why a reference is retained to the original serializer.
 * 
 * @since 2.1
 */
public class BeanAsArraySerializer
    extends BeanSerializerBase
{
    /**
     * Serializer that would produce JSON Object version; used in
     * cases where array output can not be used.
     */
    protected final BeanSerializerBase _defaultSerializer;
    
    /*
    /**********************************************************
    /* Life-cycle: constructors
    /**********************************************************
     */


    public BeanAsArraySerializer(BeanSerializerBase src) {    
        super(src, (ObjectIdWriter) null);
        _defaultSerializer = src;
    }

    protected BeanAsArraySerializer(BeanSerializerBase src, String[] toIgnore) {
        super(src, toIgnore);
        _defaultSerializer = src;
    }

    /*
    /**********************************************************
    /* Life-cycle: factory methods, fluent factories
    /**********************************************************
     */

    @Override
    public JsonSerializer<Object> unwrappingSerializer(NameTransformer transformer) {
        /* If this gets called, we will just need delegate to the default
         * serializer, to "undo" as-array serialization
         */
        return _defaultSerializer.unwrappingSerializer(transformer);
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return false;
    }

    @Override
    public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
        // can't handle Object Ids, for now, so:
        return _defaultSerializer.withObjectIdWriter(objectIdWriter);
    }

    @Override
    protected BeanAsArraySerializer withIgnorals(String[] toIgnore) {
        return new BeanAsArraySerializer(this, toIgnore);
    }

    @Override
    protected BeanSerializerBase asArraySerializer() {
        // already is one, so:
        return this;
    }
    
    /*
    /**********************************************************
    /* JsonSerializer implementation that differs between impls
    /**********************************************************
     */

    // Re-defined from base class...
    @Override
    public void serializeWithType(Object bean, JsonGenerator jgen,
            SerializerProvider provider, TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        /* Should not even get here; but let's be nice and re-route
         * if need be.
         */
        _defaultSerializer.serializeWithType(bean, jgen, provider, typeSer);
    }
    
    /**
     * Main serialization method that will delegate actual output to
     * configured
     * {@link BeanPropertyWriter} instances.
     */
    @Override
    public final void serialize(Object bean, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        // [JACKSON-805]
        if (provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                && hasSingleElement(provider)) {
            serializeAsArray(bean, jgen, provider);
            return;
        }
        /* note: it is assumed here that limitations (type id, object id,
         * any getter, filtering) have already been checked; so code here
         * is trivial.
         */
        jgen.writeStartArray();
        serializeAsArray(bean, jgen, provider);
        jgen.writeEndArray();
    }

    /*
    /**********************************************************
    /* Field serialization methods
    /**********************************************************
     */
    private boolean hasSingleElement(SerializerProvider provider) {
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getActiveView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }
        return props.length == 1;
    }

    protected final void serializeAsArray(Object bean, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getActiveView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }

        int i = 0;
        try {
            for (final int len = props.length; i < len; ++i) {
                BeanPropertyWriter prop = props[i];
                if (prop == null) { // can have nulls in filtered list; but if so, MUST write placeholders
                    jgen.writeNull();
                } else {
                    prop.serializeAsColumn(bean, jgen, provider);
                }
            }
            // NOTE: any getters can not be supported either
            //if (_anyGetterWriter != null) {
            //    _anyGetterWriter.getAndSerialize(bean, jgen, provider);
            //}
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) {
            JsonMappingException mapE = new JsonMappingException("Infinite recursion (StackOverflowError)", e);
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            mapE.prependPath(new JsonMappingException.Reference(bean, name));
            throw mapE;
        }
    }
    
    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override public String toString() {
        return "BeanAsArraySerializer for "+handledType().getName();
    }
}
