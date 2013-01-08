package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.Method;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.EnumResolver;

/**
 * Deserializer class that can deserialize instances of
 * specified Enum class from Strings and Integers.
 */
public class EnumDeserializer
    extends StdScalarDeserializer<Enum<?>>
{
    private static final long serialVersionUID = -5893263645879532318L;

    protected final EnumResolver<?> _resolver;
    
    public EnumDeserializer(EnumResolver<?> res)
    {
        super(Enum.class);
        _resolver = res;
    }

    /**
     * Factory method used when Enum instances are to be deserialized
     * using a creator (static factory method)
     * 
     * @return Deserializer based on given factory method, if type was suitable;
     *  null if type can not be used
     */
    public static JsonDeserializer<?> deserializerForCreator(DeserializationConfig config,
            Class<?> enumClass, AnnotatedMethod factory)
    {
        // note: caller has verified there's just one arg; but we must verify its type
        Class<?> paramClass = factory.getRawParameterType(0);
        if (paramClass == String.class) {
            paramClass = null;
        } else  if (paramClass == Integer.TYPE || paramClass == Integer.class) {
            paramClass = Integer.class;
        } else  if (paramClass == Long.TYPE || paramClass == Long.class) {
            paramClass = Long.class;
        } else {
            throw new IllegalArgumentException("Parameter #0 type for factory method ("+factory
                    +") not suitable, must be java.lang.String or int/Integer/long/Long");
        }
        if (config.canOverrideAccessModifiers()) {
            ClassUtil.checkAndFixAccess(factory.getMember());
        }
        return new FactoryBasedDeserializer(enumClass, factory, paramClass);
    }
    
    /*
    /**********************************************************
    /* Default JsonDeserializer implementation
    /**********************************************************
     */

    /**
     * Because of costs associated with constructing Enum resolvers,
     * let's cache instances by default.
     */
    @Override
    public boolean isCachable() { return true; }
    
    @Override
    public Enum<?> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken curr = jp.getCurrentToken();
        
        // Usually should just get string value:
        if (curr == JsonToken.VALUE_STRING || curr == JsonToken.FIELD_NAME) {
            String name = jp.getText();
            Enum<?> result = _resolver.findEnum(name);
            if (result == null) {
                if (ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                    if (name.length() == 0 || name.trim().length() == 0) {
                        return null;
                    }
                }
                if (!ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
                    throw ctxt.weirdStringException(name, _resolver.getEnumClass(),
                            "value not one of declared Enum instance names");
                }
            }
            return result;
        }
        // But let's consider int acceptable as well (if within ordinal range)
        if (curr == JsonToken.VALUE_NUMBER_INT) {
            /* ... unless told not to do that. :-)
             * (as per [JACKSON-412]
             */
            if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)) {
                throw ctxt.mappingException("Not allowed to deserialize Enum value out of JSON number (disable DeserializationConfig.DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS to allow)");
            }
            
            int index = jp.getIntValue();
            Enum<?> result = _resolver.getEnum(index);
            if (result == null && !ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
                throw ctxt.weirdNumberException(Integer.valueOf(index), _resolver.getEnumClass(),
                        "index value outside legal index range [0.."+_resolver.lastValidIndex()+"]");
            }
            return result;
        }
        throw ctxt.mappingException(_resolver.getEnumClass());
    }

    /*
    /**********************************************************
    /* Default JsonDeserializer implementation
    /**********************************************************
     */

    /**
     * Deserializer that uses a single-String static factory method
     * for locating Enum values by String id.
     */
    protected static class FactoryBasedDeserializer
        extends StdScalarDeserializer<Object>
    {
        private static final long serialVersionUID = -7775129435872564122L;

        protected final Class<?> _enumClass;
        // Marker type; null if String expected; otherwise numeric wrapper
        protected final Class<?> _inputType;
        protected final Method _factory;
        
        public FactoryBasedDeserializer(Class<?> cls, AnnotatedMethod f,
                Class<?> inputType)
        {
            super(Enum.class);
            _enumClass = cls;
            _factory = f.getAnnotated();
            _inputType = inputType;
        }

        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // couple of accepted types...
            Object value;
            if (_inputType == null) {
                value = jp.getText();
            } else  if (_inputType == Integer.class) {
                value = Integer.valueOf(jp.getValueAsInt());
            } else  if (_inputType == Long.class) {
                value = Long.valueOf(jp.getValueAsLong());
            } else {
                throw ctxt.mappingException(_enumClass);
            }
            try {
                return _factory.invoke(_enumClass, value);
            } catch (Exception e) {
                ClassUtil.unwrapAndThrowAsIAE(e);
            }
            return null;
        }
    }
}
