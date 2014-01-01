package com.fasterxml.jackson.databind.deser.std;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.Currency;
import java.util.Locale;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * Base class for simple deserializers that only accept JSON String
 * values as the source.
 */
@SuppressWarnings("serial")
public abstract class FromStringDeserializer<T> extends StdScalarDeserializer<T>
{
    public static Class<?>[] types() {
        return new Class<?>[] {
            URL.class,
            URI.class,
            File.class,
            Currency.class,
            Pattern.class,
            Locale.class,
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
        } else if (rawType == Currency.class) {
            kind = Std.STD_CURRENCY;
        } else if (rawType == Pattern.class) {
            kind = Std.STD_PATTERN;
        } else if (rawType == Locale.class) {
            kind = Std.STD_LOCALE;
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
    public final T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        // 22-Sep-2012, tatu: For 2.1, use this new method, may force coercion:
        String text = jp.getValueAsString();
        if (text != null) { // has String representation
            if (text.length() == 0 || (text = text.trim()).length() == 0) {
                // 15-Oct-2010, tatu: Empty String usually means null, so
                return null;
            }
            try {
                T result = _deserialize(text, ctxt);
                if (result != null) {
                    return result;
                }
            } catch (IllegalArgumentException iae) {
                // nothing to do here, yet? We'll fail anyway
            }
            throw ctxt.weirdStringException(text, _valueClass, "not a valid textual representation");
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
        throw ctxt.mappingException("Don't know how to convert embedded Object of type "+ob.getClass().getName()+" into "+_valueClass.getName());
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
        public final static int STD_CURRENCY = 4;
        public final static int STD_PATTERN = 5;
        public final static int STD_LOCALE = 6;
        
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
            }
            throw new IllegalArgumentException();
        }
        
    }
}
