package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
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
 * Default {@link KeyDeserializer} implementation used for most {@link java.util.Map}
 * types Jackson supports.
 * Implemented as "chameleon" (or swiss pocket knife) class; not particularly elegant,
 * but helps reduce number of classes and jar size (class metadata adds significant
 * per-class overhead; much more than bytecode).
 */
@JacksonStdImpl
public class StdKeyDeserializer extends KeyDeserializer
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public final static int TYPE_BOOLEAN = 1;
    public final static int TYPE_BYTE = 2;
    public final static int TYPE_SHORT = 3;
    public final static int TYPE_CHAR = 4;
    public final static int TYPE_INT = 5;
    public final static int TYPE_LONG = 6;
    public final static int TYPE_FLOAT = 7;
    public final static int TYPE_DOUBLE = 8;
    public final static int TYPE_LOCALE = 9;
    public final static int TYPE_DATE = 10;
    public final static int TYPE_CALENDAR = 11;
    public final static int TYPE_UUID = 12;
    public final static int TYPE_URI = 13;
    public final static int TYPE_URL = 14;
    public final static int TYPE_CLASS = 15;
    public final static int TYPE_CURRENCY = 16;

    final protected int _kind;
    final protected Class<?> _keyClass;

    /**
     * Some types that are deserialized using a helper deserializer.
     */
    protected final FromStringDeserializer<?> _deser;
    
    protected StdKeyDeserializer(int kind, Class<?> cls) {
        this(kind, cls, null);
    }

    protected StdKeyDeserializer(int kind, Class<?> cls, FromStringDeserializer<?> deser) {
        _kind = kind;
        _keyClass = cls;
        _deser = deser;
    }

    public static StdKeyDeserializer forType(Class<?> raw)
    {
        int kind;

        // first common types:
        if (raw == String.class || raw == Object.class) {
            return StringKD.forType(raw);
        } else if (raw == UUID.class) {
            kind = TYPE_UUID;
        } else if (raw == Integer.class) {
            kind = TYPE_INT;
        } else if (raw == Long.class) {
            kind = TYPE_LONG;
        } else if (raw == Date.class) {
            kind = TYPE_DATE;
        } else if (raw == Calendar.class) {
            kind = TYPE_CALENDAR;
        // then less common ones...
        } else if (raw == Boolean.class) {
            kind = TYPE_BOOLEAN;
        } else if (raw == Byte.class) {
            kind = TYPE_BYTE;
        } else if (raw == Character.class) {
            kind = TYPE_CHAR;
        } else if (raw == Short.class) {
            kind = TYPE_SHORT;
        } else if (raw == Float.class) {
            kind = TYPE_FLOAT;
        } else if (raw == Double.class) {
            kind = TYPE_DOUBLE;
        } else if (raw == URI.class) {
            kind = TYPE_URI;
        } else if (raw == URL.class) {
            kind = TYPE_URL;
        } else if (raw == Class.class) {
            kind = TYPE_CLASS;
        } else if (raw == Locale.class) {
            FromStringDeserializer<?> deser = FromStringDeserializer.findDeserializer(Locale.class);
            return new StdKeyDeserializer(TYPE_LOCALE, raw, deser);
        } else if (raw == Currency.class) {
            FromStringDeserializer<?> deser = FromStringDeserializer.findDeserializer(Currency.class);
            return new StdKeyDeserializer(TYPE_CURRENCY, raw, deser);
        } else {
            return null;
        }
        return new StdKeyDeserializer(kind, raw);
    }
    
    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt)
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

    protected Object _parse(String key, DeserializationContext ctxt) throws Exception
    {
        switch (_kind) {
        case TYPE_BOOLEAN:
            if ("true".equals(key)) {
                return Boolean.TRUE;
            }
            if ("false".equals(key)) {
                return Boolean.FALSE;
            }
            throw ctxt.weirdKeyException(_keyClass, key, "value not 'true' or 'false'");
        case TYPE_BYTE:
            {
                int value = _parseInt(key);
                // as per [JACKSON-804], allow range up to 255, inclusive
                if (value < Byte.MIN_VALUE || value > 255) {
                    throw ctxt.weirdKeyException(_keyClass, key, "overflow, value can not be represented as 8-bit value");
                }
                return Byte.valueOf((byte) value);
            }
        case TYPE_SHORT:
            {
                int value = _parseInt(key);
                if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                    throw ctxt.weirdKeyException(_keyClass, key, "overflow, value can not be represented as 16-bit value");
                }
                return Short.valueOf((short) value);
            }
        case TYPE_CHAR:
            if (key.length() == 1) {
                return Character.valueOf(key.charAt(0));
            }
            throw ctxt.weirdKeyException(_keyClass, key, "can only convert 1-character Strings");
        case TYPE_INT:
            return _parseInt(key);

        case TYPE_LONG:
            return _parseLong(key);

        case TYPE_FLOAT:
            // Bounds/range checks would be tricky here, so let's not bother even trying...
            return Float.valueOf((float) _parseDouble(key));
        case TYPE_DOUBLE:
            return _parseDouble(key);
        case TYPE_LOCALE:
            try {
                return _deser._deserialize(key, ctxt);
            } catch (IOException e) {
                throw ctxt.weirdKeyException(_keyClass, key, "unable to parse key as locale");
            }
        case TYPE_CURRENCY:
            try {
                return _deser._deserialize(key, ctxt);
            } catch (IOException e) {
                throw ctxt.weirdKeyException(_keyClass, key, "unable to parse key as currency");
            }
        case TYPE_DATE:
            return ctxt.parseDate(key);
        case TYPE_CALENDAR:
            java.util.Date date = ctxt.parseDate(key);
            return (date == null)  ? null : ctxt.constructCalendar(date);
        case TYPE_UUID:
            return UUID.fromString(key);
        case TYPE_URI:
            return URI.create(key);
        case TYPE_URL:
            return new URL(key);
        case TYPE_CLASS:
            try {
                return ctxt.findClass(key);
            } catch (Exception e) {
                throw ctxt.weirdKeyException(_keyClass, key, "unable to parse key as Class");
            }
        }
        return null;
    }

    /*
    /**********************************************************
    /* Helper methods for sub-classes
    /**********************************************************
     */

    protected int _parseInt(String key) throws IllegalArgumentException {
        return Integer.parseInt(key);
    }

    protected long _parseLong(String key) throws IllegalArgumentException {
        return Long.parseLong(key);
    }

    protected double _parseDouble(String key) throws IllegalArgumentException {
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
        
        private StringKD(Class<?> nominalType) { super(-1, nominalType); }

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
        public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return key;
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

        protected final EnumResolver _resolver;

        protected final AnnotatedMethod _factory;

        protected EnumKD(EnumResolver er, AnnotatedMethod factory) {
            super(-1, er.getEnumClass());
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
            super(-1, ctor.getDeclaringClass());
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
            super(-1, fm.getDeclaringClass());
            _factoryMethod = fm;
        }

        @Override
        public Object _parse(String key, DeserializationContext ctxt) throws Exception
        {
            return _factoryMethod.invoke(null, key);
        }
    }
}

