package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;

/**
 * Serializer implementation where given Java type is first converted
 * to an intermediate "delegate type" (using a configured
 * {@link Converter}, and then this delegate value is serialized by Jackson.
 *<p>
 * Note that although types may be related, they must not be same; trying
 * to do this will result in an exception.
 */
@SuppressWarnings("serial")
public class StdDelegatingSerializer
    extends StdSerializer<Object>
{
    // @since 3.0
    protected final BeanProperty _property;

    protected final Converter<Object,?> _converter;

    /**
     * Fully resolved delegate type, with generic information if any available.
     */
    protected final JavaType _delegateType;
    
    /**
     * Underlying serializer for type <code>T</code>.
     */
    protected final JsonSerializer<Object> _delegateSerializer;

    /**
     * If delegate serializer needs to be accessed dynamically (non-final
     * type, static type not forced), this data structure helps with efficient
     * lookups.
     *
     * @since 3.0
     */
    protected PropertySerializerMap _dynamicValueSerializers = PropertySerializerMap.emptyForProperties();
    
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    public StdDelegatingSerializer(Converter<?,?> converter)
    {
        super(Object.class);
        _converter = (Converter<Object,?>)converter;
        _delegateType = null;
        _delegateSerializer = null;
        _property = null;
    }

    @SuppressWarnings("unchecked")
    public <T> StdDelegatingSerializer(Class<T> cls, Converter<T,?> converter)
    {
        super(cls, false);
        _converter = (Converter<Object,?>)converter;
        _delegateType = null;
        _delegateSerializer = null;
        _property = null;
    }

    @Deprecated // since 3.0
    public StdDelegatingSerializer(Converter<Object,?> converter,
            JavaType delegateType, JsonSerializer<?> delegateSerializer)
    {
        this(converter, delegateType, delegateSerializer, null);
    }

    /**
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    public StdDelegatingSerializer(Converter<Object,?> converter,
            JavaType delegateType, JsonSerializer<?> delegateSerializer,
            BeanProperty prop)
    {
        super(delegateType);
        _converter = converter;
        _delegateType = delegateType;
        _delegateSerializer = (JsonSerializer<Object>) delegateSerializer;
        _property = prop;
    }
    
    /**
     * Method used for creating resolved contextual instances. Must be
     * overridden when sub-classing.
     */
    protected StdDelegatingSerializer withDelegate(Converter<Object,?> converter,
            JavaType delegateType, JsonSerializer<?> delegateSerializer,
            BeanProperty prop)
    {
        ClassUtil.verifyMustOverride(StdDelegatingSerializer.class, this, "withDelegate");
        return new StdDelegatingSerializer(converter, delegateType, delegateSerializer, prop);
    }

    /*
    /**********************************************************************
    /* Contextualization
    /**********************************************************************
     */

    @Override
    public void resolve(SerializerProvider ctxt) throws JsonMappingException
    {
        if (_delegateSerializer != null) {
            _delegateSerializer.resolve(ctxt);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider ctxt, BeanProperty property)
        throws JsonMappingException
    {
        JsonSerializer<?> delSer = _delegateSerializer;
        JavaType delegateType = _delegateType;

        if (delSer == null) {
            // Otherwise, need to locate serializer to delegate to. For that we need type information...
            if (delegateType == null) {
                delegateType = _converter.getOutputType(ctxt.getTypeFactory());
            }
            // 02-Apr-2015, tatu: For "dynamic case", where type is only specified as
            //    java.lang.Object (or missing generic), [databind#731]
            if (!delegateType.isJavaLangObject()) {
                delSer = ctxt.findValueSerializer(delegateType);
            }
        }
        if (delSer != null) {
            delSer = ctxt.handleSecondaryContextualization(delSer, property);
        }
        if ((delSer == _delegateSerializer)
                && (delegateType == _delegateType) && (property == _property)) {
            return this;
        }
        return withDelegate(_converter, delegateType, delSer, property);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    protected Converter<Object, ?> getConverter() {
        return _converter;
    }

    @Override
    public JsonSerializer<?> getDelegatee() {
        return _delegateSerializer;
    }

    /*
    /**********************************************************************
    /* Serialization
    /**********************************************************************
     */

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider ctxt) throws IOException
    {
        Object delegateValue = convertValue(value);
        // should we accept nulls?
        if (delegateValue == null) {
            ctxt.defaultSerializeNullValue(gen);
            return;
        }
        // 02-Apr-2015, tatu: As per [databind#731] may need to do dynamic lookup
        JsonSerializer<Object> ser = _delegateSerializer;
        if (ser == null) {
            ser = _findSerializer(delegateValue, ctxt);
        }
        ser.serialize(delegateValue, gen, ctxt);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider ctxt,
            TypeSerializer typeSer) throws IOException
    {
        /* 03-Oct-2012, tatu: This is actually unlikely to work ok... but for now,
         *    let's give it a chance?
         */
        Object delegateValue = convertValue(value);
        JsonSerializer<Object> ser = _delegateSerializer;
        if (ser == null) {
            ser = _findSerializer(value, ctxt);
        }
        ser.serializeWithType(delegateValue, gen, ctxt, typeSer);
    }

    @Override
    public boolean isEmpty(SerializerProvider ctxt, Object value) throws IOException
    {
        Object delegateValue = convertValue(value);
        if (delegateValue == null) {
            return true;
        }
        JsonSerializer<Object> ser = _delegateSerializer;
        if (ser == null) {
            ser = _findSerializer(value, ctxt);
        }
        return ser.isEmpty(ctxt, delegateValue);
    }

    /*
    /**********************************************************************
    /* Schema functionality
    /**********************************************************************
     */

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        /* 03-Sep-2012, tatu: Not sure if this can be made to really work
         *    properly... but for now, try this:
         */
        // 02-Apr-2015, tatu: For dynamic case, very little we can do
        if (_delegateSerializer != null) {
            _delegateSerializer.acceptJsonFormatVisitor(visitor, typeHint);
        }
    }

    /*
    /**********************************************************************
    /* Overridable methods
    /**********************************************************************
     */

    /**
     * Method called to convert from source Java value into delegate
     * value (which will be serialized using standard Jackson serializer for delegate type)
     *<P>
     * The default implementation uses configured {@link Converter} to do
     * conversion.
     * 
     * @param value Value to convert
     * 
     * @return Result of conversion
     */
    protected Object convertValue(Object value) {
        return _converter.convert(value);
    }

    /**
     * Helper method used for locating serializer to use in dynamic use case, where
     * actual type value gets converted to is not specified beyond basic
     * {@link java.lang.Object}, and where serializer needs to be located dynamically
     * based on actual value type.
     */
    protected JsonSerializer<Object> _findSerializer(Object value, SerializerProvider ctxt)
        throws JsonMappingException
    {
        // 17-Apr-2018, tatu: Basically inline `_findAndAddDynamic(...)`
        // 17-Apr-2018, tatu: difficult to know if these are primary or secondary serializers...
        Class<?> cc = value.getClass();
        PropertySerializerMap.SerializerAndMapResult result = _dynamicValueSerializers.findAndAddSecondarySerializer(cc,
                ctxt, _property);
        if (_dynamicValueSerializers != result.map) {
            _dynamicValueSerializers = result.map;
        }
        return result.serializer;
    }
}
