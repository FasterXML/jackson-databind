package com.fasterxml.jackson.databind.deser.std;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Base class for simple deserializers that serialize values from String
 * representation: this includes JSON Strings and other Scalar values that
 * can be coerced into text, like Numbers and Booleans).
 * Simple JSON String values are trimmed using {@link java.lang.String#trim}.
 * Partial deserializer implementation will try to first access current token as
 * a String, calls {@link #_deserialize(String,DeserializationContext)} and
 * returns return value.
 * If this does not work (current token not a simple scalar type), attempts
 * are made so that:
 *<ul>
 * <li>Embedded values ({@link JsonToken#VALUE_EMBEDDED_OBJECT}) are returned as-is
 *    if they are of compatible type
 *  </li>
 * <li>Arrays may be "unwrapped" if (and only if) {@link DeserializationFeature#UNWRAP_SINGLE_VALUE_ARRAYS}
 *    is enabled, and array contains just a single scalar value that can be deserialized
 *    (for example, JSON Array with single JSON String element).
 *  </li>
 * </ul>
 *<p>
 * Special handling includes:
 * <ul>
 * <li>Null values ({@link JsonToken#VALUE_NULL}) are handled by returning value
 *   returned by {@link JsonDeserializer#getNullValue(DeserializationContext)}: default
 *   implementation simply returns Java `null` but this may be overridden.
 *  </li>
 * <li>Empty String (after trimming) will result in {@link #_deserializeFromEmptyString}
 *   getting called, and return value being returned as deserialization: default implementation
 *   simply returns `null`.
 *  </li>
 * </ul>
 */
@SuppressWarnings("serial")
public abstract class FromStringDeserializer<T> extends StdScalarDeserializer<T>
{
    public static Class<?>[] types() {
        return new Class<?>[] {
            File.class,
            URL.class,
            URI.class,
            Class.class,
            JavaType.class,
            Currency.class,
            Pattern.class,
            Locale.class,
            Charset.class,
            TimeZone.class,
            InetAddress.class,
            InetSocketAddress.class,
            StringBuilder.class,
        };
    }
    
    /*
    /**********************************************************
    /* Deserializer implementations
    /**********************************************************
     */
    
    protected FromStringDeserializer(Class<?> vc) {
        super(vc);
    }

    /**
     * Factory method for trying to find a deserializer for one of supported
     * types that have simple from-String serialization.
     */
    public static Std findDeserializer(Class<?> rawType)
    {
        int kind = 0;
        if (rawType == File.class) {
            kind = Std.STD_FILE;
        } else if (rawType == URL.class) {
            kind = Std.STD_URL;
        } else if (rawType == URI.class) {
            kind = Std.STD_URI;
        } else if (rawType == Class.class) {
            kind = Std.STD_CLASS;
        } else if (rawType == JavaType.class) {
            kind = Std.STD_JAVA_TYPE;
        } else if (rawType == Currency.class) {
            kind = Std.STD_CURRENCY;
        } else if (rawType == Pattern.class) {
            kind = Std.STD_PATTERN;
        } else if (rawType == Locale.class) {
            kind = Std.STD_LOCALE;
        } else if (rawType == Charset.class) {
            kind = Std.STD_CHARSET;
        } else if (rawType == TimeZone.class) {
            kind = Std.STD_TIME_ZONE;
        } else if (rawType == InetAddress.class) {
            kind = Std.STD_INET_ADDRESS;
        } else if (rawType == InetSocketAddress.class) {
            kind = Std.STD_INET_SOCKET_ADDRESS;
        } else if (rawType == StringBuilder.class) {
            kind = Std.STD_STRING_BUILDER;
        } else {
            return null;
        }
        return new Std(rawType, kind);
    }
    
    /*
    /**********************************************************
    /* Deserializer implementations
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // Let's get textual value, possibly via coercion from other scalar types
        String text = p.getValueAsString();
        if (text != null) { // has String representation
            if (text.length() == 0 || (text = text.trim()).length() == 0) {
                // Usually should become null; but not always
                return _deserializeFromEmptyString();
            }
            Exception cause = null;
            try {
                T result = _deserialize(text, ctxt);
                if (result != null) {
                    return result;
                }
            } catch (IllegalArgumentException | MalformedURLException e) {
                cause = e;
            }
            String msg = "not a valid textual representation";
            if (cause != null) {
                String m2 = cause.getMessage();
                if (m2 != null) {
                    msg = msg + ", problem: "+m2;
                }
            }
            // 05-May-2016, tatu: Unlike most usage, this seems legit, so...
            JsonMappingException e = ctxt.weirdStringException(text, _valueClass, msg);
            if (cause != null) {
                e.initCause(cause);
            }
            throw e;
            // nothing to do here, yet? We'll fail anyway
        }
        JsonToken t = p.getCurrentToken();
        // [databind#381]
        if (t == JsonToken.START_ARRAY) {
            return _deserializeFromArray(p, ctxt);
        }
        if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
            // Trivial cases; null to null, instance of type itself returned as is
            Object ob = p.getEmbeddedObject();
            if (ob == null) {
                return null;
            }
            if (_valueClass.isAssignableFrom(ob.getClass())) {
                return (T) ob;
            }
            return _deserializeEmbedded(ob, ctxt);
        }
        return (T) ctxt.handleUnexpectedToken(_valueClass, p);
    }
        
    protected abstract T _deserialize(String value, DeserializationContext ctxt) throws IOException;

    protected T _deserializeEmbedded(Object ob, DeserializationContext ctxt) throws IOException {
        // default impl: error out
        ctxt.reportInputMismatch(this,
                "Don't know how to convert embedded Object of type %s into %s",
                ob.getClass().getName(), _valueClass.getName());
        return null;
    }

    protected T _deserializeFromEmptyString() throws IOException {
        return null;
    }

    /*
    /**********************************************************
    /* A general-purpose implementation
    /**********************************************************
     */

    /**
     * "Chameleon" deserializer that works on simple types that are deserialized
     * from a simple String.
     * 
     * @since 2.4
     */
    public static class Std extends FromStringDeserializer<Object>
    {
        private static final long serialVersionUID = 1;

        public final static int STD_FILE = 1;
        public final static int STD_URL = 2;
        public final static int STD_URI = 3;
        public final static int STD_CLASS = 4;
        public final static int STD_JAVA_TYPE = 5;
        public final static int STD_CURRENCY = 6;
        public final static int STD_PATTERN = 7;
        public final static int STD_LOCALE = 8;
        public final static int STD_CHARSET = 9;
        public final static int STD_TIME_ZONE = 10;
        public final static int STD_INET_ADDRESS = 11;
        public final static int STD_INET_SOCKET_ADDRESS = 12;
        public final static int STD_STRING_BUILDER = 13;

        protected final int _kind;
        
        protected Std(Class<?> valueType, int kind) {
            super(valueType);
            _kind = kind;
        }

        @Override
        protected Object _deserialize(String value, DeserializationContext ctxt) throws IOException
        {
            switch (_kind) {
            case STD_FILE:
                return new File(value);
            case STD_URL:
                return new URL(value);
            case STD_URI:
                return URI.create(value);
            case STD_CLASS:
                try {
                    return ctxt.findClass(value);
                } catch (Exception e) {
                    return ctxt.handleInstantiationProblem(_valueClass, value,
                            ClassUtil.getRootCause(e));
                }
            case STD_JAVA_TYPE:
                return ctxt.getTypeFactory().constructFromCanonical(value);
            case STD_CURRENCY:
                // will throw IAE if unknown:
                return Currency.getInstance(value);
            case STD_PATTERN:
                // will throw IAE (or its subclass) if malformed
                return Pattern.compile(value);
            case STD_LOCALE:
                {
                    int ix = _firstHyphenOrUnderscore(value);
                    if (ix < 0) { // single argument
                        return new Locale(value);
                    }
                    String first = value.substring(0, ix);
                    value = value.substring(ix+1);
                    ix = _firstHyphenOrUnderscore(value);
                    if (ix < 0) { // two pieces
                        return new Locale(first, value);
                    }
                    String second = value.substring(0, ix);
                    return new Locale(first, second, value.substring(ix+1));
                }
            case STD_CHARSET:
                return Charset.forName(value);
            case STD_TIME_ZONE:
                return TimeZone.getTimeZone(value);
            case STD_INET_ADDRESS:
                return InetAddress.getByName(value);
            case STD_INET_SOCKET_ADDRESS:
                if (value.startsWith("[")) {
                    // bracketed IPv6 (with port number)

                    int i = value.lastIndexOf(']');
                    if (i == -1) {
                        throw new InvalidFormatException(ctxt.getParser(),
                                "Bracketed IPv6 address must contain closing bracket",
                                value, InetSocketAddress.class);
                    }

                    int j = value.indexOf(':', i);
                    int port = j > -1 ? Integer.parseInt(value.substring(j + 1)) : 0;
                    return new InetSocketAddress(value.substring(0, i + 1), port);
                }
                int ix = value.indexOf(':');
                if (ix >= 0 && value.indexOf(':', ix + 1) < 0) {
                    // host:port
                    int port = Integer.parseInt(value.substring(ix+1));
                    return new InetSocketAddress(value.substring(0, ix), port);
                }
                // host or unbracketed IPv6, without port number
                return new InetSocketAddress(value, 0);
            case STD_STRING_BUILDER:
                return new StringBuilder(value);
            }
            throw new IllegalArgumentException();
        }

        @Override
        protected Object _deserializeFromEmptyString() throws IOException {
            // As per [databind#398], URI requires special handling
            if (_kind == STD_URI) {
                return URI.create("");
            }
            // As per [databind#1123], Locale too
            if (_kind == STD_LOCALE) {
                return Locale.ROOT;
            }
            if (_kind == STD_STRING_BUILDER) {
                return new StringBuilder();
            }
            return super._deserializeFromEmptyString();
        }

        protected int _firstHyphenOrUnderscore(String str)
        {
            for (int i = 0, end = str.length(); i < end; ++i) {
                char c = str.charAt(i);
                if (c == '_' || c == '-') {
                    return i;
                }
            }
            return -1;
        }
    }
}
