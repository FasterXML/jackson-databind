package com.fasterxml.jackson.databind.deser.std;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Currency;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.ClassUtil;

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

            // Special impl:
            StringBuilder.class,
            StringBuffer.class, // since 2.13.3
        };
    }

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected FromStringDeserializer(Class<?> vc) {
        super(vc);
    }

    /**
     * Factory method for trying to find a deserializer for one of supported
     * types that have simple from-String serialization.
     */
    public static FromStringDeserializer<?> findDeserializer(Class<?> rawType)
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
            return new StringBuilderDeserializer();
        } else if (rawType == StringBuffer.class) { // since 2.13.3
            return new StringBufferDeserializer();
        } else {
            return null;
        }
        return new Std(rawType, kind);
    }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.OtherScalar;
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
        if (text == null) {
            JsonToken t = p.currentToken();
            if (t != JsonToken.START_OBJECT) {
                return (T) _deserializeFromOther(p, ctxt, t);
            }
            // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
            text = ctxt.extractScalarFromObject(p, this, _valueClass);
        }
        if (text.isEmpty()) {
            // 09-Jun-2020, tatu: Commonly `null` but may coerce to "empty" as well
            return (T) _deserializeFromEmptyString(ctxt);
        }
        if (_shouldTrim()) {
            final String old = text;
            text = text.trim();
            if (text != old) {
                if (text.isEmpty()) {
                    return (T) _deserializeFromEmptyString(ctxt);
                }
            }
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
        throw ctxt.weirdStringException(text, _valueClass, msg)
            .withCause(cause);
    }

    /**
     * Main method from trying to deserialize actual value from non-empty
     * String.
     */
    protected abstract T _deserialize(String value, DeserializationContext ctxt) throws IOException;

    // @since 2.13.1
    protected boolean _shouldTrim() {
        return true;
    }

    // @since 2.12
    protected Object _deserializeFromOther(JsonParser p, DeserializationContext ctxt,
            JsonToken t) throws IOException
    {
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
                return ob;
            }
            return _deserializeEmbedded(ob, ctxt);
        }
        return ctxt.handleUnexpectedToken(_valueClass, p);
    }

    /**
     * Overridable method to allow coercion from embedded value that is neither
     * {@code null} nor directly assignable to target type.
     * Used, for example, by {@link UUIDDeserializer} to coerce from {@code byte[]}.
     */
    protected T _deserializeEmbedded(Object ob, DeserializationContext ctxt) throws IOException {
        // default impl: error out
        ctxt.reportInputMismatch(this,
                "Don't know how to convert embedded Object of type %s into %s",
                ob.getClass().getName(), _valueClass.getName());
        return null;
    }

    @Deprecated // since 2.12 -- override variant that takes context
    protected final T _deserializeFromEmptyString() throws IOException {
        return null;
    }

    /**
     * @since 2.12
     */
    protected Object _deserializeFromEmptyString(DeserializationContext ctxt) throws IOException {
        CoercionAction act = ctxt.findCoercionAction(logicalType(), _valueClass,
                CoercionInputShape.EmptyString);
        if (act == CoercionAction.Fail) {
            ctxt.reportInputMismatch(this,
"Cannot coerce empty String (\"\") to %s (but could if enabling coercion using `CoercionConfig`)",
_coercedTypeDesc());
        }
        if (act == CoercionAction.AsNull) {
            return getNullValue(ctxt);
        }
        if (act == CoercionAction.AsEmpty) {
            return getEmptyValue(ctxt);
        }
        // 09-Jun-2020, tatu: semantics for `TryConvert` are bit interesting due to
        //    historical reasons
        return _deserializeFromEmptyStringDefault(ctxt);
    }

    /**
     * @since 2.12
     */
    protected Object _deserializeFromEmptyStringDefault(DeserializationContext ctxt) throws IOException {
        // by default, "as-null", but overridable by sub-classes
        return getNullValue(ctxt);
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
        // No longer implemented here since 2.12
        //        public final static int STD_STRING_BUILDER = 13;

        protected final static String LOCALE_EXT_MARKER = "_#";

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

        @Override // since 2.12
        public Object getEmptyValue(DeserializationContext ctxt)
                throws JsonMappingException
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
        protected Object _deserializeFromEmptyStringDefault(DeserializationContext ctxt) throws IOException {
            // 09-Jun-2020, tatu: For backwards compatibility deserialize "as-empty"
            //    as URI and Locale did that in 2.11 (and StringBuilder probably ought to).
            //   But doing this here instead of super-class lets UUID return "as-null" instead
            return getEmptyValue(ctxt);
        }

        // @since 2.13.1
        @Override
        protected boolean _shouldTrim() {
            // 04-Dec-2021, tatu: For [databund#3290]
            return (_kind != STD_PATTERN);
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

        private Locale _deserializeLocale(String value, DeserializationContext ctxt)
            throws IOException
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
            // [databind#3259]: Support for BCP 47 java.util.Locale ser/deser
            int extMarkerIx = value.indexOf(LOCALE_EXT_MARKER);
            if (extMarkerIx < 0) {
                return new Locale(first, second, value.substring(ix+1));
            }
            return _deSerializeBCP47Locale(value, ix, first, second, extMarkerIx);
        }

        private Locale _deSerializeBCP47Locale(String value, int ix, String first, String second,
                int extMarkerIx)
        {
            String third = "";
            try {
                // Below condition checks if variant value is present to handle empty variant values such as
                // en__#Latn_x-ext
                // _US_#Latn
                if (extMarkerIx > 0 && extMarkerIx > ix) {
                    third = value.substring(ix + 1, extMarkerIx);
                }
                value = value.substring(extMarkerIx + 2);

                int underscoreIx = value.indexOf('_');
                if (underscoreIx < 0) {
                    int hyphenIx = value.indexOf('-');
                    if (hyphenIx < 0) {
                        return new Locale.Builder().setLanguage(first)
                                .setRegion(second)
                                .setVariant(third)
                                .setScript(value)
                                .build();
                    }
                    return new Locale.Builder().setLanguage(first)
                            .setRegion(second).setVariant(third)
                            .setExtension(value.charAt(0), value.substring(hyphenIx + 1))
                            .build();
                }
                final int len = value.length();
                Locale.Builder b = new Locale.Builder()
                        .setLanguage(first)
                        .setRegion(second).setVariant(third)
                        .setScript(value.substring(0, underscoreIx));
                if ((underscoreIx + 1) < len) {
                    b = b.setExtension(value.charAt(underscoreIx + 1),
                                value.substring(Math.min(len, underscoreIx + 3)));
                }
                return b.build();
            } catch (IllformedLocaleException ex) {
                // should we really just swallow the exception?
                return new Locale(first, second, third);
            }
        }
    }

    // @since 2.12 to simplify logic a bit: should not use coercions when reading
    static class StringBuilderDeserializer extends FromStringDeserializer<Object>
    {
        public StringBuilderDeserializer() {
            super(StringBuilder.class);
        }

        @Override
        public LogicalType logicalType() {
            return LogicalType.Textual;
        }

        @Override
        public Object getEmptyValue(DeserializationContext ctxt)
            throws JsonMappingException
        {
            return new StringBuilder();
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            String text = p.getValueAsString();
            if (text != null) {
                return _deserialize(text, ctxt);
            }
            return super.deserialize(p, ctxt);
        }

        @Override
        protected Object _deserialize(String value, DeserializationContext ctxt)
            throws IOException
        {
            return new StringBuilder(value);
        }
    }

    // @since 2.13.3: duplicated code but for only 2 impls base class seems unnecessary
    static class StringBufferDeserializer extends FromStringDeserializer<Object>
    {
        public StringBufferDeserializer() { super(StringBuffer.class); }

        @Override
        public LogicalType logicalType() { return LogicalType.Textual; }

        @Override
        public Object getEmptyValue(DeserializationContext ctxt) {
            return new StringBuffer();
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            String text = p.getValueAsString();
            if (text != null) {
                return _deserialize(text, ctxt);
            }
            return super.deserialize(p, ctxt);
        }

        @Override
        protected Object _deserialize(String value, DeserializationContext ctxt)
            throws IOException
        {
            return new StringBuffer(value);
        }
    }
}
