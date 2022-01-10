package com.fasterxml.jackson.databind.ser.bean;

import java.util.Set;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
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
 */
public class BeanAsArraySerializer
    extends BeanSerializerBase
{
    /**
     * Serializer that would produce JSON Object version; used in
     * cases where array output cannot be used.
     */
    protected final BeanSerializerBase _defaultSerializer;
    
    /*
    /**********************************************************************
    /* Life-cycle: constructors
    /**********************************************************************
     */

    public BeanAsArraySerializer(BeanSerializerBase src) {    
        super(src, (ObjectIdWriter) null);
        _defaultSerializer = src;
    }

    protected BeanAsArraySerializer(BeanSerializerBase src, Set<String> toIgnore, Set<String> toInclude) {
        super(src, toIgnore, toInclude);
        _defaultSerializer = src;
    }

    protected BeanAsArraySerializer(BeanSerializerBase src,
            ObjectIdWriter oiw, Object filterId) {
        super(src, oiw, filterId);
        _defaultSerializer = src;
    }

    /*
    /**********************************************************************
    /* Life-cycle: factory methods, fluent factories
    /**********************************************************************
     */

    /**
     * @since 3.0
     */
    public static BeanSerializerBase construct(BeanSerializerBase src)
    {
        BeanSerializerBase ser = UnrolledBeanAsArraySerializer.tryConstruct(src);
        if (ser != null) {
            return ser;
        }
        return new BeanAsArraySerializer(src);
    }
    
    @Override
    public ValueSerializer<Object> unwrappingSerializer(NameTransformer transformer) {
        // If this gets called, we will just need delegate to the default
        // serializer, to "undo" as-array serialization
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
    public BeanSerializerBase withFilterId(Object filterId) {
        return new BeanAsArraySerializer(this, _objectIdWriter, filterId);
    }

    @Override
    protected BeanAsArraySerializer withByNameInclusion(Set<String> toIgnore, Set<String> toInclude) {
        return new BeanAsArraySerializer(this, toIgnore, toInclude);
    }

    @Override // @since 2.11.1
    protected BeanSerializerBase withProperties(BeanPropertyWriter[] properties,
            BeanPropertyWriter[] filteredProperties) {
        // 16-Jun-2020, tatu: Added for [databind#2759] but with as-array we
        //    probably do not want to reorder anything; so actually leave unchanged
        return this;
    }

    @Override
    protected BeanSerializerBase asArraySerializer() {
        // already is one, so:
        return this;
    }

    /*
    /**********************************************************************
    /* ValueSerializer implementation that differs between impls
    /**********************************************************************
     */

    // Re-defined from base class, due to differing prefixes
    @Override
    public void serializeWithType(Object bean, JsonGenerator gen,
            SerializerProvider ctxt, TypeSerializer typeSer)
        throws JacksonException
    {
        // 10-Dec-2014, tatu: Not sure if this can be made to work reliably;
        //   but for sure delegating to default implementation will not work. So:
        if (_objectIdWriter != null) {
            _serializeWithObjectId(bean, gen, ctxt, typeSer);
            return;
        }
        gen.assignCurrentValue(bean);
        WritableTypeId typeIdDef = _typeIdDef(typeSer, bean, JsonToken.START_ARRAY);
        typeSer.writeTypePrefix(gen, ctxt, typeIdDef);
        final boolean filtered = (_filteredProps != null && ctxt.getActiveView() != null);
        if (filtered) {
            serializeFiltered(bean, gen, ctxt);
        } else {
            serializeNonFiltered(bean, gen, ctxt);
        }
        typeSer.writeTypeSuffix(gen, ctxt, typeIdDef);
    }

    /**
     * Main serialization method that will delegate actual output to
     * configured
     * {@link BeanPropertyWriter} instances.
     */
    @Override
    public final void serialize(Object bean, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        final boolean filtered = (_filteredProps != null && provider.getActiveView() != null);
        if (provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                && hasSingleElement(provider)) {
            if (filtered) serializeFiltered(bean, gen, provider);
            else serializeNonFiltered(bean, gen, provider);
            return;
        }
        // note: it is assumed here that limitations (type id, object id,
        // any getter, filtering) have already been checked; so code here
        // is trivial.

        gen.writeStartArray(bean, _props.length);
        if (filtered) serializeFiltered(bean, gen, provider);
        else serializeNonFiltered(bean, gen, provider);
        gen.writeEndArray();
    }

    /*
    /**********************************************************************
    /* Property serialization methods
    /**********************************************************************
     */

    private boolean hasSingleElement(SerializerProvider provider) {
        return _props.length == 1;
    }

    protected final void serializeNonFiltered(Object bean, JsonGenerator gen,
            SerializerProvider provider)
        throws JacksonException
    {
        final BeanPropertyWriter[] props = _props;
        int i = 0;
        int left = props.length;
        BeanPropertyWriter prop = null;

        try {
            if (left > 3) {
                do {
                    prop = props[i];
                    prop.serializeAsElement(bean, gen, provider);
                    prop = props[i+1];
                    prop.serializeAsElement(bean, gen, provider);
                    prop = props[i+2];
                    prop.serializeAsElement(bean, gen, provider);
                    prop = props[i+3];
                    prop.serializeAsElement(bean, gen, provider);
                    left -= 4;
                    i += 4;
                } while (left > 3);
            }
            switch (left) {
            case 3:
                prop = props[i++];
                prop.serializeAsElement(bean, gen, provider);
            case 2:
                prop = props[i++];
                prop.serializeAsElement(bean, gen, provider);
            case 1:
                prop = props[i++];
                prop.serializeAsElement(bean, gen, provider);
            }
            // NOTE: any getters cannot be supported either
            //if (_anyGetterWriter != null) {
            //    _anyGetterWriter.getAndSerialize(bean, gen, provider);
            //}
        } catch (Exception e) {
            wrapAndThrow(provider, e, bean, prop.getName());
        } catch (StackOverflowError e) {
            throw DatabindException.from(gen, "Infinite recursion (StackOverflowError)", e)
                .prependPath(bean, prop.getName());
        }
    }

    protected final void serializeFiltered(Object bean, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        final BeanPropertyWriter[] props = _filteredProps;
        int i = 0;
        int left = props.length;
        BeanPropertyWriter prop = null;

        try {
            if (left > 3) {
                do {
                    prop = props[i];
                    if (prop == null) { // can have nulls in filtered list; but if so, MUST write placeholders
                        gen.writeNull();
                    } else {
                        prop.serializeAsElement(bean, gen, provider);
                    }

                    prop = props[i+1];
                    if (prop == null) {
                        gen.writeNull();
                    } else {
                        prop.serializeAsElement(bean, gen, provider);
                    }

                    prop = props[i+2];
                    if (prop == null) {
                        gen.writeNull();
                    } else {
                        prop.serializeAsElement(bean, gen, provider);
                    }

                    prop = props[i+3];
                    if (prop == null) {
                        gen.writeNull();
                    } else {
                        prop.serializeAsElement(bean, gen, provider);
                    }

                    left -= 4;
                    i += 4;
                } while (left > 3);
            }
            switch (left) {
            case 3:
                prop = props[i++];
                if (prop == null) {
                    gen.writeNull();
                } else {
                    prop.serializeAsElement(bean, gen, provider);
                }
            case 2:
                prop = props[i++];
                if (prop == null) {
                    gen.writeNull();
                } else {
                    prop.serializeAsElement(bean, gen, provider);
                }
            case 1:
                prop = props[i++];
                if (prop == null) {
                    gen.writeNull();
                } else {
                    prop.serializeAsElement(bean, gen, provider);
                }
            }
            // NOTE: any getters cannot be supported either
            //if (_anyGetterWriter != null) {
            //    _anyGetterWriter.getAndSerialize(bean, gen, provider);
            //}
        } catch (Exception e) {
            wrapAndThrow(provider, e, bean, prop.getName());
        } catch (StackOverflowError e) {
            throw DatabindException.from(gen, "Infinite recursion (StackOverflowError)", e)
                .prependPath(bean, prop.getName());
        }
    }
}
