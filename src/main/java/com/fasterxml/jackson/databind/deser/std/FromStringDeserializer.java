package com.fasterxml.jackson.databind.deser.std;

import static java.lang.Character.isLetter;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.Currency;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.TimeZone;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.VersionUtil;
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
            Path.class, // since 3.0
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
    /**********************************************************************
    /* Deserializer implementations
    /**********************************************************************
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
        } else if (rawType == Path.class) {
            kind = Std.STD_PATH;
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
    /**********************************************************************
    /* Deserializer implementations
    /**********************************************************************
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
                return _deserializeFromEmptyString(ctxt);
            }
            Exception cause = null;
            try {
                // 19-May-2017, tatu: Used to require non-null result (assuming `null`
                //    indicated error; but that seems wrong. Should be able to return
                //    `null` as value.
                return _deserialize(text, ctxt);
            } catch (IllegalArgumentException | MalformedURLException e) {
                cause = e;
            }
            // note: `cause` can't be null
            String msg = "not a valid textual representation";
            String m2 = cause.getMessage();
            if (m2 != null) {
                msg = msg + ", problem: "+m2;
            }
            // 05-May-2016, tatu: Unlike most usage, this seems legit, so...
            JsonMappingException e = ctxt.weirdStringException(text, _valueClass, msg);
            e.initCause(cause);
            throw e;
            // nothing to do here, yet? We'll fail anyway
        }
        JsonToken t = p.currentToken();
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
                ClassUtil.classNameOf(ob), _valueClass.getName());
        return null;
    }

    @SuppressWarnings("unchecked")
    protected T _deserializeFromEmptyString(DeserializationContext ctxt) throws IOException {
        return (T) _coerceEmptyString(ctxt, false);
    }

    /*
    /**********************************************************************
    /* A general-purpose implementation
    /**********************************************************************
     */

    /**
     * "Chameleon" deserializer that works on simple types that are deserialized
     * from a simple String.
     */
    public static class Std extends FromStringDeserializer<Object>
    {
        private static final long serialVersionUID = 1;

        public final static int STD_FILE = 1;
        public final static int STD_URL = 2;
        public final static int STD_URI = 3;
        public final static int STD_PATH = 4;
        public final static int STD_CLASS = 5;
        public final static int STD_JAVA_TYPE = 6;
        public final static int STD_CURRENCY = 7;
        public final static int STD_PATTERN = 8;
        public final static int STD_LOCALE = 9;
        public final static int STD_CHARSET = 10;
        public final static int STD_TIME_ZONE = 11;
        public final static int STD_INET_ADDRESS = 12;
        public final static int STD_INET_SOCKET_ADDRESS = 13;
        public final static int STD_STRING_BUILDER = 14;

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
            case STD_PATH:
                // 06-Sep-2018, tatu: Offlined due to additions in [databind#2120]
                return NioPathHelper.deserialize(ctxt, value);
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
            VersionUtil.throwInternal();
            return null;
        }

        @Override
        protected Object _deserializeFromEmptyString(DeserializationContext ctxt) throws IOException {
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
            return super._deserializeFromEmptyString(ctxt);
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

    private static class NioPathHelper {
        private static final boolean areWindowsFilePathsSupported;
        static {
            boolean isWindowsRootFound = false;
            for (File file : File.listRoots()) {
                String path = file.getPath();
                if (path.length() >= 2 && isLetter(path.charAt(0)) && path.charAt(1) == ':') {
                    isWindowsRootFound = true;
                    break;
                }
            }
            areWindowsFilePathsSupported = isWindowsRootFound;
        }

        public static Path deserialize(DeserializationContext ctxt, String value) throws IOException {
            // If someone gives us an input with no : at all, treat as local path, instead of failing
            // with invalid URI.
            if (value.indexOf(':') < 0) {
                return Paths.get(value);
            }

            if (areWindowsFilePathsSupported) {
                if (value.length() >= 2 && isLetter(value.charAt(0)) && value.charAt(1) == ':') {
                    return Paths.get(value);
                }
            }

            final URI uri;
            try {
                uri = new URI(value);
            } catch (URISyntaxException e) {
                return (Path) ctxt.handleInstantiationProblem(Path.class, value, e);
            }
            try {
                return Paths.get(uri);
            } catch (FileSystemNotFoundException cause) {
                try {
                    final String scheme = uri.getScheme();
                    // We want to use the current thread's context class loader, not system class loader that is used in Paths.get():
                    for (FileSystemProvider provider : ServiceLoader.load(FileSystemProvider.class)) {
                        if (provider.getScheme().equalsIgnoreCase(scheme)) {
                            return provider.getPath(uri);
                        }
                    }
                    return (Path) ctxt.handleInstantiationProblem(Path.class, value, cause);
                } catch (Throwable e) {
                    e.addSuppressed(cause);
                    return (Path) ctxt.handleInstantiationProblem(Path.class, value, e);
                }
            } catch (Throwable e) {
                return (Path) ctxt.handleInstantiationProblem(Path.class, value, e);
            }
        }        
    }
}
