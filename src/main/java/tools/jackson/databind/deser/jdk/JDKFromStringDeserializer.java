package tools.jackson.databind.deser.jdk;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import tools.jackson.core.*;
import tools.jackson.core.util.VersionUtil;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.FromStringDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.util.ClassUtil;

/**
 * Base class for simple deserializers that serialize values from String
 * representation: this includes JSON Strings and other Scalar values that
 * can be coerced into text, like Numbers and Booleans).
 * Simple JSON String values are trimmed using {@link java.lang.String#trim}.
 * Partial deserializer implementation will try to first access current token as
 * a String, calls {@code _deserialize(String,DeserializationContext)} and
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
 *   returned by {@link ValueDeserializer#getNullValue(DeserializationContext)}: default
 *   implementation simply returns Java `null` but this may be overridden.
 *  </li>
 * <li>Empty String (after trimming) will result in {@link #_deserializeFromEmptyString}
 *   getting called, and return value being returned as deserialization: default implementation
 *   simply returns `null`.
 *  </li>
 * </ul>
 */
public class JDKFromStringDeserializer
    extends FromStringDeserializer<Object>
{
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

            // Special impl:
            StringBuffer.class,
            StringBuilder.class,
        };
    }

    protected final int _kind;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected JDKFromStringDeserializer(Class<?> valueType, int kind) {
        super(valueType);
        _kind = kind;
    }

    /**
     * Factory method for trying to find a deserializer for one of supported
     * types that have simple from-String serialization.
     */
    public static JDKFromStringDeserializer findDeserializer(Class<?> rawType)
    {
        int kind = 0;
        if (rawType == File.class) {
            kind = STD_FILE;
        } else if (rawType == URL.class) {
            kind = STD_URL;
        } else if (rawType == URI.class) {
            kind = STD_URI;
        } else if (rawType == Path.class) {
            kind = STD_PATH;
        } else if (rawType == Class.class) {
            kind = STD_CLASS;
        } else if (rawType == JavaType.class) {
            kind = STD_JAVA_TYPE;
        } else if (rawType == Currency.class) {
            kind = STD_CURRENCY;
        } else if (rawType == Pattern.class) {
            kind = STD_PATTERN;
        } else if (rawType == Locale.class) {
            kind = STD_LOCALE;
        } else if (rawType == Charset.class) {
            kind = STD_CHARSET;
        } else if (rawType == TimeZone.class) {
            kind = STD_TIME_ZONE;
        } else if (rawType == InetAddress.class) {
            kind = STD_INET_ADDRESS;
        } else if (rawType == InetSocketAddress.class) {
            kind = STD_INET_SOCKET_ADDRESS;
        } else if (rawType == StringBuilder.class) {
            return new StringBuilderDeserializer();
        } else if (rawType == StringBuffer.class) {
            return new StringBufferDeserializer();
        } else {
            return null;
        }
        return new JDKFromStringDeserializer(rawType, kind);
    }

    /*
    /**********************************************************************
    /* A general-purpose implementation
    /**********************************************************************
     */

    // NOTE: public (unlike base class) to give JDKKeyDeserializer access
    @Override
    public Object _deserialize(String value, DeserializationContext ctxt)
        throws JacksonException,
            MalformedURLException, UnknownHostException
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
            try {
                return Currency.getInstance(value);
            } catch (IllegalArgumentException e) {
                // Looks like there is no more information
                return ctxt.handleWeirdStringValue(_valueClass, value,
                        "Unrecognized currency");
            }
        case STD_PATTERN:
            try {
                return Pattern.compile(value);
            } catch (PatternSyntaxException e) {
                return ctxt.handleWeirdStringValue(_valueClass, value,
                        "Invalid Pattern, problem: "+e.getDescription());
            }
        case STD_LOCALE:
            return _deserializeLocale(value, ctxt);
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
        }
        VersionUtil.throwInternal();
        return null;
    }

    @Override
    protected boolean _shouldTrim() {
        // 04-Dec-2021, tatu: [databind#3299] Do not trim (trailing) white space:
        return (_kind != STD_PATTERN);
    }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt)
    {
        switch (_kind) {
        case STD_URI:
            // As per [databind#398], URI requires special handling
            return URI.create("");
        case STD_LOCALE:
            // As per [databind#1123], Locale too
            return Locale.ROOT;
        }
        return super.getEmptyValue(ctxt);
    }

    @Override
    protected Object _deserializeFromEmptyStringDefault(DeserializationContext ctxt) throws JacksonException {
        // 09-Jun-2020, tatu: For backwards compatibility deserialize "as-empty"
        //    as URI and Locale did that in 2.11 (and StringBuilder probably ought to).
        //   But doing this here instead of super-class lets UUID return "as-null" instead
        return getEmptyValue(ctxt);
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

    private Locale _deserializeLocale(String fullValue, DeserializationContext ctxt)
        throws JacksonException
    {
        // First things first: simple single-segment value easiest to check for
        // manually
        int ix = _firstHyphenOrUnderscore(fullValue);
        if (ix < 0) { // single argument
            return new Locale(fullValue);
        }
        // But also of interest: "_" signals "old" serialization, simpleish;
        // but "-" language-tag
        boolean newStyle = fullValue.charAt(ix) == '-';
        if (newStyle) {
            return Locale.forLanguageTag(fullValue);
        }
        final String first = fullValue.substring(0, ix);
        final String rest = fullValue.substring(ix+1);
        ix = _firstHyphenOrUnderscore(rest);
        if (ix < 0) {
            return new Locale(first, rest);
        }
        return new Locale(first, rest.substring(0, ix), rest.substring(ix+1));
    }

    static class StringBuilderDeserializer extends JDKFromStringDeserializer
    {
        public StringBuilderDeserializer() { super(StringBuilder.class, -1); }

        @Override
        public LogicalType logicalType() { return LogicalType.Textual;}

        @Override
        public Object getEmptyValue(DeserializationContext ctxt) { return new StringBuilder(); }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
        {
            String text = p.getValueAsString();
            if (text != null) {
                return _deserialize(text, ctxt);
            }
            return super.deserialize(p, ctxt);
        }

        @Override
        public Object _deserialize(String value, DeserializationContext ctxt) {
            return new StringBuilder(value);
        }
    }

    static class StringBufferDeserializer extends JDKFromStringDeserializer
    {
        public StringBufferDeserializer() { super(StringBuffer.class, -1); }

        @Override
        public LogicalType logicalType() { return LogicalType.Textual;}

        @Override
        public Object getEmptyValue(DeserializationContext ctxt) { return new StringBuffer(); }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
        {
            String text = p.getValueAsString();
            if (text != null) {
                return _deserialize(text, ctxt);
            }
            return super.deserialize(p, ctxt);
        }

        @Override
        public Object _deserialize(String value, DeserializationContext ctxt) {
            return new StringBuffer(value);
        }
    }

    private static class NioPathHelper {
        private static final boolean areWindowsFilePathsSupported;
        static {
            boolean isWindowsRootFound = false;
            for (File file : File.listRoots()) {
                String path = file.getPath();
                if (path.length() >= 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') {
                    isWindowsRootFound = true;
                    break;
                }
            }
            areWindowsFilePathsSupported = isWindowsRootFound;
        }

        public static Path deserialize(DeserializationContext ctxt, String value) throws JacksonException {
            // If someone gives us an input with no : at all, treat as local path, instead of failing
            // with invalid URI.
            if (value.indexOf(':') < 0) {
                return Paths.get(value);
            }

            if (areWindowsFilePathsSupported) {
                if (value.length() >= 2 && Character.isLetter(value.charAt(0)) && value.charAt(1) == ':') {
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
                } catch (Exception e) {
                    e.addSuppressed(cause);
                    return (Path) ctxt.handleInstantiationProblem(Path.class, value, e);
                }
            } catch (Exception e) {
                return (Path) ctxt.handleInstantiationProblem(Path.class, value, e);
            }
        }
    }
}
