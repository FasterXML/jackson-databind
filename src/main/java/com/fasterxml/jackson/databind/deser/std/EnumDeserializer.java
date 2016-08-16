package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.Method;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.CompactStringObjectMap;
import com.fasterxml.jackson.databind.util.EnumResolver;

/**
 * Deserializer class that can deserialize instances of
 * specified Enum class from Strings and Integers.
 */
@JacksonStdImpl // was missing until 2.6
public class EnumDeserializer
    extends StdScalarDeserializer<Object>
{
    private static final long serialVersionUID = 1L;

    /**
     * @since 2.6
     */
    protected Object[] _enumsByIndex;

    /**
     * @since 2.7.3
     */
    protected final CompactStringObjectMap _lookupByName;

    /**
     * Alternatively, we may need a different lookup object if "use toString"
     * is defined.
     *
     * @since 2.7.3
     */
    protected CompactStringObjectMap _lookupByToString;
    
    public EnumDeserializer(EnumResolver byNameResolver)
    {
        super(byNameResolver.getEnumClass());
        _lookupByName = byNameResolver.constructLookup();
        _enumsByIndex = byNameResolver.getRawEnums();
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
            ClassUtil.checkAndFixAccess(factory.getMember(),
                    config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
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
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        JsonToken curr = p.getCurrentToken();
        
        // Usually should just get string value:
        if (curr == JsonToken.VALUE_STRING || curr == JsonToken.FIELD_NAME) {
            CompactStringObjectMap lookup = ctxt.isEnabled(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                    ? _getToStringLookup() : _lookupByName;
            final String name = p.getText();
            Object result = lookup.find(name);
            if (result == null) {
                return _deserializeAltString(p, ctxt, lookup, name);
            }
            return result;
        }
        // But let's consider int acceptable as well (if within ordinal range)
        if (curr == JsonToken.VALUE_NUMBER_INT) {
            // ... unless told not to do that
            int index = p.getIntValue();
            if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)) {
                _failOnNumber(ctxt, p, index);
            }
            if (index >= 0 && index < _enumsByIndex.length) {
                return _enumsByIndex[index];
            }
            if (!ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
                throw ctxt.weirdNumberException(index, _enumClass(),
                        "index value outside legal index range [0.."+(_enumsByIndex.length-1)+"]");
            }
            return null;
        }
        return _deserializeOther(p, ctxt);
    }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */
    
    private final Object _deserializeAltString(JsonParser p, DeserializationContext ctxt,
            CompactStringObjectMap lookup, String name) throws IOException
    {
        name = name.trim();
        if (name.length() == 0) {
            if (ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                return null;
            }
        } else {
            // [databind#149]: Allow use of 'String' indexes as well
            char c = name.charAt(0);
            if (c >= '0' && c <= '9') {
                try {
                    int ix = Integer.parseInt(name);
                    if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)) {
                        _failOnNumber(ctxt, p, ix);
                    }
                    if (ix >= 0 && ix < _enumsByIndex.length) {
                        return _enumsByIndex[ix];
                    }
                } catch (NumberFormatException e) {
                    // fine, ignore, was not an integer
                }
            }
        }
        if (!ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
            throw ctxt.weirdStringException(name, _enumClass(),
                    "value not one of declared Enum instance names: "+lookup.keys());
        }
        return null;
    }

    protected Object _deserializeOther(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        JsonToken curr = p.getCurrentToken();
        // [databind#381]
        if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                && p.isExpectedStartArrayToken()) {
            p.nextToken();
            final Object parsed = deserialize(p, ctxt);
            curr = p.nextToken();
            if (curr != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(p, JsonToken.END_ARRAY,
                        "Attempted to unwrap single value array for single '" + _enumClass().getName() + "' value but there was more than a single value in the array");
            }
            return parsed;
        }
        throw ctxt.mappingException(_enumClass());
    }

    protected void _failOnNumber(DeserializationContext ctxt, JsonParser p, int index)
        throws IOException
    {
        throw InvalidFormatException.from(p,
                String.format("Not allowed to deserialize Enum value out of JSON number (%d): disable DeserializationConfig.DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS to allow",
                        index),
                        index, _enumClass());
    }

    protected Class<?> _enumClass() {
        return handledType();
    }

    protected CompactStringObjectMap _getToStringLookup()
    {
        CompactStringObjectMap lookup = _lookupByToString;
        // note: exact locking not needed; all we care for here is to try to
        // reduce contention for the initial resolution
        if (lookup == null) {
            synchronized (this) {
                lookup = EnumResolver.constructUnsafeUsingToString(_enumClass())
                        .constructLookup();
            }
            _lookupByToString = lookup;
        }
        return lookup;
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
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            Object value;
            if (_deser != null) {
                value = _deser.deserialize(p, ctxt);
            } else {
                JsonToken curr = p.getCurrentToken();
                if (curr == JsonToken.VALUE_STRING || curr == JsonToken.FIELD_NAME) {
                    value = p.getText();
                } else {
                    value = p.getValueAsString();
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
        public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
            if (_deser == null) { // String never has type info
                return deserialize(p, ctxt);
            }
            return typeDeserializer.deserializeTypedFromAny(p, ctxt);
        }
    }
}
