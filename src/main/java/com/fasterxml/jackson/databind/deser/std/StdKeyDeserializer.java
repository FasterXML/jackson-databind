package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.NumberInput;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.EnumResolver;

/**
 * Base class for simple key deserializers.
 */
public abstract class StdKeyDeserializer
    extends KeyDeserializer
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    final protected Class<?> _keyClass;

    protected StdKeyDeserializer(Class<?> cls) { _keyClass = cls; }

    @Override
    public final Object deserializeKey(String key, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (key == null) { // is this even legal call?
            return null;
        }
        try {
            Object result = _parse(key, ctxt);
            if (result != null) {
                return result;
            }
        } catch (Exception re) {
            throw ctxt.weirdKeyException(_keyClass, key, "not a valid representation: "+re.getMessage());
        }
        if (_keyClass.isEnum() && ctxt.getConfig().isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
            return null;
        }
        throw ctxt.weirdKeyException(_keyClass, key, "not a valid representation");
    }

    public Class<?> getKeyClass() { return _keyClass; }

    protected abstract Object _parse(String key, DeserializationContext ctxt) throws Exception;

    /*
    /**********************************************************
    /* Helper methods for sub-classes
    /**********************************************************
     */

    protected int _parseInt(String key) throws IllegalArgumentException
    {
        return Integer.parseInt(key);
    }

    protected long _parseLong(String key) throws IllegalArgumentException
    {
        return Long.parseLong(key);
    }

    protected double _parseDouble(String key) throws IllegalArgumentException
    {
        return NumberInput.parseDouble(key);
    }

    /*
    /**********************************************************
    /* First: the standard "String as String" deserializer
    /**********************************************************
     */

    @JacksonStdImpl
    final static class StringKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        private final static StringKD sString = new StringKD(String.class);
        private final static StringKD sObject = new StringKD(Object.class);
        
        private StringKD(Class<?> nominalType) { super(nominalType); }

        public static StringKD forType(Class<?> nominalType)
        {
            if (nominalType == String.class) {
                return sString;
            }
            if (nominalType == Object.class) {
                return sObject;
            }
            return new StringKD(nominalType);
        }
        
        @Override
        public String _parse(String key, DeserializationContext ctxt) throws JsonMappingException {
            return key;
        }
    }    
    
    /*
    /**********************************************************
    /* Key deserializer implementations; wrappers
    /**********************************************************
     */

    @JacksonStdImpl
    final static class BoolKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        BoolKD() { super(Boolean.class); }

        @Override
        public Boolean _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            if ("true".equals(key)) {
                return Boolean.TRUE;
            }
            if ("false".equals(key)) {
                return Boolean.FALSE;
            }
            throw ctxt.weirdKeyException(_keyClass, key, "value not 'true' or 'false'");
        }
    }

    @JacksonStdImpl
    final static class ByteKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        ByteKD() { super(Byte.class); }

        @Override
		public Byte _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            int value = _parseInt(key);
            // as per [JACKSON-804], allow range up to 255, inclusive
            if (value < Byte.MIN_VALUE || value > 255) {
                throw ctxt.weirdKeyException(_keyClass, key, "overflow, value can not be represented as 8-bit value");
            }
            return Byte.valueOf((byte) value);
        }
    }

    @JacksonStdImpl
    final static class ShortKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        ShortKD() { super(Integer.class); }

        @Override
		public Short _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            int value = _parseInt(key);
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                throw ctxt.weirdKeyException(_keyClass, key, "overflow, value can not be represented as 16-bit value");
            }
            return Short.valueOf((short) value);
        }
    }

    /**
     * Dealing with Characters is bit trickier: let's assume it must be a String, and that
     * Unicode numeric value is never used.
     */
    @JacksonStdImpl
    final static class CharKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        CharKD() { super(Character.class); }

        @Override
		public Character _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            if (key.length() == 1) {
                return Character.valueOf(key.charAt(0));
            }
            throw ctxt.weirdKeyException(_keyClass, key, "can only convert 1-character Strings");
        }
    }

    @JacksonStdImpl
    final static class IntKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        IntKD() { super(Integer.class); }

        @Override
		public Integer _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            return _parseInt(key);
        }
    }

    @JacksonStdImpl
    final static class LongKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        LongKD() { super(Long.class); }

        @Override
        public Long _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            return _parseLong(key);
        }
    }

    @JacksonStdImpl
    final static class DoubleKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        DoubleKD() { super(Double.class); }

        @Override
        public Double _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            return _parseDouble(key);
        }
    }

    @JacksonStdImpl
    final static class FloatKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        FloatKD() { super(Float.class); }

        @Override
        public Float _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            /* 22-Jan-2009, tatu: Bounds/range checks would be tricky
             *   here, so let's not bother even trying...
             */
            return Float.valueOf((float) _parseDouble(key));
        }
    }

    @JacksonStdImpl
    final static class LocaleKD extends StdKeyDeserializer {
        private static final long serialVersionUID = 1L;

        protected JdkDeserializers.LocaleDeserializer _localeDeserializer;

        LocaleKD() { super(Locale.class); _localeDeserializer = new JdkDeserializers.LocaleDeserializer();}

        @Override
        protected Locale _parse(String key, DeserializationContext ctxt) throws JsonMappingException {
            try {
                return _localeDeserializer._deserialize(key,ctxt);
            } catch (IOException e) {
                throw ctxt.weirdKeyException(_keyClass, key, "unable to parse key as locale");
            }
        }
    }

    /*
    /**********************************************************
    /* Key deserializer implementations; other
    /**********************************************************
     */

    /**
     * Key deserializer that wraps a "regular" deserializer (but one
     * that must recognize FIELD_NAMEs as text!) to reuse existing
     * handlers as key handlers.
     */
    final static class DelegatingKD
        extends KeyDeserializer // note: NOT the std one
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        final protected Class<?> _keyClass;

        protected final JsonDeserializer<?> _delegate;
        
        protected DelegatingKD(Class<?> cls, JsonDeserializer<?> deser) {
            _keyClass = cls;
            _delegate = deser;
        }

        @Override
        public final Object deserializeKey(String key, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (key == null) { // is this even legal call?
                return null;
            }
            try {
                // Ugh... should not have to give parser which may or may not be correct one...
                Object result = _delegate.deserialize(ctxt.getParser(), ctxt);
                if (result != null) {
                    return result;
                }
            } catch (Exception re) {
                throw ctxt.weirdKeyException(_keyClass, key, "not a valid representation: "+re.getMessage());
            }
            throw ctxt.weirdKeyException(_keyClass, key, "not a valid representation");
        }

        public Class<?> getKeyClass() { return _keyClass; }
    }
     
    @JacksonStdImpl
    final static class EnumKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        protected final EnumResolver<?> _resolver;

        protected final AnnotatedMethod _factory;

        protected EnumKD(EnumResolver<?> er, AnnotatedMethod factory) {
            super(er.getEnumClass());
            _resolver = er;
            _factory = factory;
        }

        @Override
        public Object _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            if (_factory != null) {
                try {
                    return _factory.call1(key);
                } catch (Exception e) {
                    ClassUtil.unwrapAndThrowAsIAE(e);
                }
            }
            Enum<?> e = _resolver.findEnum(key);
            if (e == null && !ctxt.getConfig().isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
                throw ctxt.weirdKeyException(_keyClass, key, "not one of values for Enum class");
            }
            return e;
        }
    }
    
    /**
     * Key deserializer that calls a single-string-arg constructor
     * to instantiate desired key type.
     */
    final static class StringCtorKeyDeserializer extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        protected final Constructor<?> _ctor;

        public StringCtorKeyDeserializer(Constructor<?> ctor) {
            super(ctor.getDeclaringClass());
            _ctor = ctor;
        }

        @Override
        public Object _parse(String key, DeserializationContext ctxt) throws Exception
        {
            return _ctor.newInstance(key);
        }
    }

    /**
     * Key deserializer that calls a static no-args factory method
     * to instantiate desired key type.
     */
    final static class StringFactoryKeyDeserializer extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        final Method _factoryMethod;

        public StringFactoryKeyDeserializer(Method fm) {
            super(fm.getDeclaringClass());
            _factoryMethod = fm;
        }

        @Override
        public Object _parse(String key, DeserializationContext ctxt) throws Exception
        {
            return _factoryMethod.invoke(null, key);
        }
    }

    // as per [JACKSON-657]
    @JacksonStdImpl
    final static class DateKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        protected DateKD() {
            super(java.util.Date.class);
        }

        @Override
        public Object _parse(String key, DeserializationContext ctxt)
            throws IllegalArgumentException, JsonMappingException
        {
            return ctxt.parseDate(key);
        }
    }
        
    // as per [JACKSON-657]
    @JacksonStdImpl
    final static class CalendarKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        protected CalendarKD() {
            super(java.util.Calendar.class);
        }

        @Override
        public Object _parse(String key, DeserializationContext ctxt)
            throws IllegalArgumentException, JsonMappingException
        {
            java.util.Date date = ctxt.parseDate(key);
            return (date == null)  ? null : ctxt.constructCalendar(date);
        }
    }

    // as per [JACKSON-726]
    @JacksonStdImpl
    final static class UuidKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        protected UuidKD() {
            super(UUID.class);
        }

        @Override
        public Object _parse(String key, DeserializationContext ctxt)
            throws IllegalArgumentException, JsonMappingException
        {
            return UUID.fromString(key);
        }
    }
}

