package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.NumberInput;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.cfg.EnumFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.EnumResolver;
import com.fasterxml.jackson.databind.util.TokenBuffer;

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
    public final static int TYPE_BYTE_ARRAY = 17; // since 2.9

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
        if (raw == String.class || raw == Object.class
                || raw == CharSequence.class
                // see [databind#2115]:
                || raw == Serializable.class) {
            return StringKD.forType(raw);
        }
        if (raw == UUID.class) {
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
        } else if (raw == byte[].class) {
            kind = TYPE_BYTE_ARRAY;
        } else {
            return null;
        }
        return new StdKeyDeserializer(kind, raw);
    }

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt)
        throws IOException
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
            return ctxt.handleWeirdKey(_keyClass, key, "not a valid representation, problem: (%s) %s",
                    re.getClass().getName(),
                    ClassUtil.exceptionMessage(re));
        }
        if (ClassUtil.isEnumType(_keyClass)
                && ctxt.getConfig().isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
            return null;
        }
        return ctxt.handleWeirdKey(_keyClass, key, "not a valid representation");
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
            return ctxt.handleWeirdKey(_keyClass, key, "value not 'true' or 'false'");
        case TYPE_BYTE:
            {
                int value = _parseInt(key);
                // allow range up to 255, inclusive (to support "unsigned" byte)
                if (value < Byte.MIN_VALUE || value > 255) {
                    return ctxt.handleWeirdKey(_keyClass, key, "overflow, value cannot be represented as 8-bit value");
                }
                return Byte.valueOf((byte) value);
            }
        case TYPE_SHORT:
            {
                int value = _parseInt(key);
                if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                    return ctxt.handleWeirdKey(_keyClass, key, "overflow, value cannot be represented as 16-bit value");
                    // fall-through and truncate if need be
                }
                return Short.valueOf((short) value);
            }
        case TYPE_CHAR:
            if (key.length() == 1) {
                return Character.valueOf(key.charAt(0));
            }
            return ctxt.handleWeirdKey(_keyClass, key, "can only convert 1-character Strings");
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
            } catch (IllegalArgumentException e) {
                return _weirdKey(ctxt, key, e);
            }
        case TYPE_CURRENCY:
            try {
                return _deser._deserialize(key, ctxt);
            } catch (IllegalArgumentException e) {
                return _weirdKey(ctxt, key, e);
            }
        case TYPE_DATE:
            return ctxt.parseDate(key);
        case TYPE_CALENDAR:
            return ctxt.constructCalendar(ctxt.parseDate(key));
        case TYPE_UUID:
            try {
                return UUID.fromString(key);
            } catch (Exception e) {
                return _weirdKey(ctxt, key, e);
            }
        case TYPE_URI:
            try {
                return URI.create(key);
            } catch (Exception e) {
                return _weirdKey(ctxt, key, e);
            }
        case TYPE_URL:
            try {
                return new URL(key);
            } catch (MalformedURLException e) {
                return _weirdKey(ctxt, key, e);
            }
        case TYPE_CLASS:
            try {
                return ctxt.findClass(key);
            } catch (Exception e) {
                return ctxt.handleWeirdKey(_keyClass, key, "unable to parse key as Class");
            }
        case TYPE_BYTE_ARRAY:
            try {
                return ctxt.getConfig().getBase64Variant().decode(key);
            } catch (IllegalArgumentException e) {
                return _weirdKey(ctxt, key, e);
            }
        default:
            throw new IllegalStateException("Internal error: unknown key type "+_keyClass);
        }
    }

    /*
    /**********************************************************
    /* Helper methods for sub-classes
    /**********************************************************
     */

    protected int _parseInt(String key) throws IllegalArgumentException {
        return NumberInput.parseInt(key);
    }

    protected long _parseLong(String key) throws IllegalArgumentException {
        return NumberInput.parseLong(key);
    }

    protected double _parseDouble(String key) throws IllegalArgumentException {
        return NumberInput.parseDouble(key);
    }

    // @since 2.9
    protected Object _weirdKey(DeserializationContext ctxt, String key, Exception e) throws IOException {
        return ctxt.handleWeirdKey(_keyClass, key, "problem: %s",
                ClassUtil.exceptionMessage(e));
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
        public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
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

        @SuppressWarnings("resource")
        @Override
        public final Object deserializeKey(String key, DeserializationContext ctxt)
            throws IOException
        {
            if (key == null) { // is this even legal call?
                return null;
            }
            TokenBuffer tb = ctxt.bufferForInputBuffering();
            tb.writeString(key);
            try {
                // Ugh... should not have to give parser which may or may not be correct one...
                JsonParser p = tb.asParser();
                p.nextToken();
                Object result = _delegate.deserialize(p, ctxt);
                if (result != null) {
                    return result;
                }
                return ctxt.handleWeirdKey(_keyClass, key, "not a valid representation");
            } catch (Exception re) {
                return ctxt.handleWeirdKey(_keyClass, key, "not a valid representation: %s", re.getMessage());
            }
        }

        public Class<?> getKeyClass() { return _keyClass; }
    }

    @JacksonStdImpl
    final static class EnumKD extends StdKeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        protected final EnumResolver _byNameResolver;

        protected final AnnotatedMethod _factory;

        /**
         * Lazily constructed alternative in case there is need to
         * use 'toString()' method as the source.
         *
         * @since 2.7.3
         */
        protected volatile EnumResolver _byToStringResolver;

        /**
         * Lazily constructed alternative in case there is need to
         * parse using enum index method as the source.
         *
         * @since 2.15
         */
        protected volatile EnumResolver _byIndexResolver;

        protected final Enum<?> _enumDefaultValue;

        protected EnumKD(EnumResolver er, AnnotatedMethod factory) {
            super(-1, er.getEnumClass());
            _byNameResolver = er;
            _factory = factory;
            _enumDefaultValue = er.getDefaultValue();
        }

        @Override
        public Object _parse(String key, DeserializationContext ctxt) throws IOException
        {
            if (_factory != null) {
                try {
                    return _factory.call1(key);
                } catch (Exception e) {
                    ClassUtil.unwrapAndThrowAsIAE(e);
                }
            }
            EnumResolver res = ctxt.isEnabled(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                    ? _getToStringResolver(ctxt) : _byNameResolver;
            Enum<?> e = res.findEnum(key);
            // If enum is found, no need to try deser using index
            if (e == null && ctxt.isEnabled(EnumFeature.READ_ENUM_KEYS_USING_INDEX)) {
                res = _getIndexResolver(ctxt);
                e = res.findEnum(key);
            }
            if (e == null) {
                if ((_enumDefaultValue != null)
                        && ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)) {
                    e = _enumDefaultValue;
                } else if (!ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
                    return ctxt.handleWeirdKey(_keyClass, key, "not one of the values accepted for Enum class: %s",
                        res.getEnumIds());
                }
                // fall-through if problems are collected, not immediately thrown
            }
            return e;
        }

        private EnumResolver _getToStringResolver(DeserializationContext ctxt)
        {
            EnumResolver res = _byToStringResolver;
            if (res == null) {
                synchronized (this) {
                    res = _byToStringResolver;
                    if (res == null) {
                        res = EnumResolver.constructUsingToString(ctxt.getConfig(),
                            _byNameResolver.getEnumClass());
                        _byToStringResolver = res;
                    }
                }
            }
            return res;
        }

        private EnumResolver _getIndexResolver(DeserializationContext ctxt) {
            EnumResolver res = _byIndexResolver;
            if (res == null) {
                synchronized (this) {
                    res = _byIndexResolver;
                    if (res == null) {
                        res = EnumResolver.constructUsingIndex(ctxt.getConfig(),
                            _byNameResolver.getEnumClass());
                        _byIndexResolver = res;
                    }
                }
            }
            return res;
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

