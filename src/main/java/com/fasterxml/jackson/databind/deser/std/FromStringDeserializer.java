package com.fasterxml.jackson.databind.deser.std;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Base class for simple deserializers that only accept JSON String
 * values as the source.
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
    public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        // Issue#381
        if (jp.getCurrentToken() == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            jp.nextToken();
            final T value = deserialize(jp, ctxt);
            if (jp.nextToken() != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, 
                                "Attempted to unwrap single value array for single '" + _valueClass.getName() + "' value but there was more than a single value in the array");
            }
            return value;
        }
        // 22-Sep-2012, tatu: For 2.1, use this new method, may force coercion:
        String text = jp.getValueAsString();
        if (text != null) { // has String representation
            if (text.length() == 0 || (text = text.trim()).length() == 0) {
                // 04-Feb-2013, tatu: Usually should become null; but not always
                return _deserializeFromEmptyString();
            }
            Exception cause = null;
            try {
                T result = _deserialize(text, ctxt);
                if (result != null) {
                    return result;
                }
            } catch (IllegalArgumentException iae) {
                cause = iae;
            }
            String msg = "not a valid textual representation";
            if (cause != null) {
                String m2 = cause.getMessage();
                if (m2 != null) {
                    msg = msg + ", problem: "+m2;
                }
            }
            JsonMappingException e = ctxt.weirdStringException(text, _valueClass, msg);
            if (cause != null) {
                e.initCause(cause);
            }
            throw e;
            // nothing to do here, yet? We'll fail anyway
        }
        if (jp.getCurrentToken() == JsonToken.VALUE_EMBEDDED_OBJECT) {
            // Trivial cases; null to null, instance of type itself returned as is
            Object ob = jp.getEmbeddedObject();
            if (ob == null) {
                return null;
            }
            if (_valueClass.isAssignableFrom(ob.getClass())) {
                return (T) ob;
            }
            return _deserializeEmbedded(ob, ctxt);
        }
        throw ctxt.mappingException(_valueClass);
    }
        
    protected abstract T _deserialize(String value, DeserializationContext ctxt) throws IOException;

    protected T _deserializeEmbedded(Object ob, DeserializationContext ctxt) throws IOException {
        // default impl: error out
        throw ctxt.mappingException("Don't know how to convert embedded Object of type %s into %s",
                ob.getClass().getName(), _valueClass.getName());
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
                    throw ctxt.instantiationException(_valueClass, ClassUtil.getRootCause(e));
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
                    int ix = value.indexOf('_');
                    if (ix < 0) { // single argument
                        return new Locale(value);
                    }
                    String first = value.substring(0, ix);
                    value = value.substring(ix+1);
                    ix = value.indexOf('_');
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
                        throw new InvalidFormatException("Bracketed IPv6 address must contain closing bracket",
                                value, InetSocketAddress.class);
                    }

                    int j = value.indexOf(':', i);
                    int port = j > -1 ? Integer.parseInt(value.substring(j + 1)) : 0;
                    return new InetSocketAddress(value.substring(0, i + 1), port);
                } else {
                    int ix = value.indexOf(':');
                    if (ix >= 0 && value.indexOf(':', ix + 1) < 0) {
                        // host:port
                        int port = Integer.parseInt(value.substring(ix+1));
                        return new InetSocketAddress(value.substring(0, ix), port);
                    }
                    // host or unbracketed IPv6, without port number
                    return new InetSocketAddress(value, 0);
                }
            }
            throw new IllegalArgumentException();
        }

        @Override
        protected Object _deserializeFromEmptyString() throws IOException {
            // As per [#398], URI requires special handling
            if (_kind == STD_URI) {
                return URI.create("");
            }
            return super._deserializeFromEmptyString();
        }
    }
}
