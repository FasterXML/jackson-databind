package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.Method;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
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
    public Enum<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        JsonToken curr = jp.getCurrentToken();
        
        // Usually should just get string value:
        if (curr == JsonToken.VALUE_STRING || curr == JsonToken.FIELD_NAME) {
            String name = jp.getText();
            Enum<?> result = _resolver.findEnum(name);
            if (result == null) {
                return _deserializeAltString(jp, ctxt, name);
            }
            return result;
        }
        // But let's consider int acceptable as well (if within ordinal range)
        if (curr == JsonToken.VALUE_NUMBER_INT) {
            // ... unless told not to do that. :-) (as per [JACKSON-412]
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
        return _deserializeOther(jp, ctxt);
    }

    private final Enum<?> _deserializeAltString(JsonParser jp, DeserializationContext ctxt,
            String name) throws IOException
    {
        name = name.trim();
        if (name.length() == 0) {
            if (ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                return null;
            }
        } else {
            // [#149]: Allow use of 'String' indexes as well
            char c = name.charAt(0);
            if (c >= '0' && c <= '9') {
                try {
                    int ix = Integer.parseInt(name);
                    Enum<?> result = _resolver.getEnum(ix);
                    if (result != null) {
                        return result;
                    }
                } catch (NumberFormatException e) {
                    // fine, ignore, was not an integer
                }
            }
        }
        if (!ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
            throw ctxt.weirdStringException(name, _resolver.getEnumClass(),
                    "value not one of declared Enum instance names: "+_resolver.getEnums());
        }
        return null;
    }

    private final Enum<?> _deserializeOther(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        JsonToken curr = jp.getCurrentToken();
        // Issue#381
        if (curr == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            jp.nextToken();
            final Enum<?> parsed = deserialize(jp, ctxt);
            curr = jp.nextToken();
            if (curr != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY,
                        "Attempted to unwrap single value array for single '" + _resolver.getEnumClass().getName() + "' value but there was more than a single value in the array");
            }
            return parsed;
        }
        throw ctxt.mappingException(_resolver.getEnumClass());
    }
    
    /*
    /**********************************************************
    /* Additional helper classes
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
                Throwable t = ClassUtil.getRootCause(e);
                if (t instanceof IOException) {
                    throw (IOException) t;
                }
                throw ctxt.instantiationException(_enumClass, t);
            }
        }
    }
}
