package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.Method;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.EnumResolver;

/**
 * Deserializer class that can deserialize instances of
 * specified Enum class from Strings and Integers.
 */
public class EnumDeserializer
    extends StdScalarDeserializer<Enum<?>>
{
    private static final long serialVersionUID = 1L;

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
            _checkFailOnNumber(ctxt);
            
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
                    _checkFailOnNumber(ctxt);
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

    protected Enum<?> _deserializeOther(JsonParser jp, DeserializationContext ctxt) throws IOException
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

    protected void _checkFailOnNumber(DeserializationContext ctxt) throws IOException
    {
        if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)) {
            throw ctxt.mappingException("Not allowed to deserialize Enum value out of JSON number (disable DeserializationConfig.DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS to allow)");
        }
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
        extends StdDeserializer<Object>
        implements ContextualDeserializer
    {
        private static final long serialVersionUID = 1;

        // Marker type; null if String expected; otherwise numeric wrapper
        protected final Class<?> _inputType;
        protected final Method _factory;
        protected final JsonDeserializer<?> _deser;
        
        public FactoryBasedDeserializer(Class<?> cls, AnnotatedMethod f,
                Class<?> inputType)
        {
            super(cls);
            _factory = f.getAnnotated();
            _inputType = inputType;
            _deser = null;
        }

        protected FactoryBasedDeserializer(FactoryBasedDeserializer base,
                JsonDeserializer<?> deser) {
            super(base._valueClass);
            _inputType = base._inputType;
            _factory = base._factory;
            _deser = deser;
        }
        
        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
                BeanProperty property)
            throws JsonMappingException
        {
            if ((_deser == null) && (_inputType != String.class)) {
                return new FactoryBasedDeserializer(this,
                        ctxt.findContextualValueDeserializer(ctxt.constructType(_inputType), property));
            }
            return this;
        }
        
        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            Object value;
            if (_deser != null) {
                value = _deser.deserialize(jp, ctxt);
            } else {
                JsonToken curr = jp.getCurrentToken();
                if (curr == JsonToken.VALUE_STRING || curr == JsonToken.FIELD_NAME) {
                    value = jp.getText();
                } else {
                    value = jp.getValueAsString();
                }
            }
            try {
                return _factory.invoke(_valueClass, value);
            } catch (Exception e) {
                Throwable t = ClassUtil.getRootCause(e);
                if (t instanceof IOException) {
                    throw (IOException) t;
                }
                throw ctxt.instantiationException(_valueClass, t);
            }
        }

        @Override
        public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
            if (_deser == null) { // String never has type info
                return deserialize(jp, ctxt);
            }
            return typeDeserializer.deserializeTypedFromAny(jp, ctxt);
        }
    }
}
