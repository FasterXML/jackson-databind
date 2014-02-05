package com.fasterxml.jackson.databind.deser.std;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.*;

/**
 * Container class that contains serializers for JDK types that
 * require special handling for some reason.
 */
@SuppressWarnings("serial")
public class JdkDeserializers
{
    private final static HashSet<String> _classNames = new HashSet<String>();
    static {
        // note: can skip primitive types; other ways to check them:
        Class<?>[] numberTypes = new Class<?>[] {
                UUID.class,
                URL.class,
                URI.class,
                File.class,
                Currency.class,
                Pattern.class,
                Locale.class,
                InetAddress.class,
                InetSocketAddress.class,
                Charset.class,
                AtomicBoolean.class,
                Class.class,
                StackTraceElement.class,
                ByteBuffer.class
        };
        for (Class<?> cls : numberTypes) {
            _classNames.add(cls.getName());
        }
    }

    public static JsonDeserializer<?> find(Class<?> rawType, String clsName)
    {
        if (!_classNames.contains(clsName)) {
            return null;
        }
        /* Ok: following ones would work via String-arg detection too;
         * if we get more may want to formally change.
         */
        if (rawType == URI.class) {
            return URIDeserializer.instance;
        }
        if (rawType == URL.class) {
            return URLDeserializer.instance;
        }
        if (rawType == File.class) {
            return FileDeserializer.instance;
        }
        /* But these will require custom handling regardless:
         */
        if (rawType == UUID.class) {
            return UUIDDeserializer.instance;
        }
        if (rawType == Currency.class) {
            return CurrencyDeserializer.instance;
        }
        if (rawType == Pattern.class) {
            return PatternDeserializer.instance;
        }
        if (rawType == Locale.class) {
            return LocaleDeserializer.instance;
        }
        if (rawType == InetAddress.class) {
            return InetAddressDeserializer.instance;
        }
        if (rawType == InetSocketAddress.class) {
            return InetSocketAddressDeserializer.instance;
        }
        if (rawType == Charset.class) {
            return new CharsetDeserializer();
        }
        if (rawType == Class.class) {
            return ClassDeserializer.instance;
        }
        if (rawType == StackTraceElement.class) {
            return StackTraceElementDeserializer.instance;
        }
        if (rawType == AtomicBoolean.class) {
            // (note: AtomicInteger/Long work due to single-arg constructor. For now?
            return AtomicBooleanDeserializer.instance;
        }
        if (rawType == ByteBuffer.class) {
            return new ByteBufferDeserializer();
        }
        // should never occur
        throw new IllegalArgumentException("Internal error: can't find deserializer for "+clsName);
    }
    
    /*
    /**********************************************************
    /* Deserializer implementations: from-String deserializers
    /**********************************************************
     */

    public static class URLDeserializer
        extends FromStringDeserializer<URL>
    {
        public final static URLDeserializer instance = new URLDeserializer();

        public URLDeserializer() { super(URL.class); }
        
        @Override
        protected URL _deserialize(String value, DeserializationContext ctxt)
            throws IOException
        {
            return new URL(value);
        }
    }
    
    public static class URIDeserializer
        extends FromStringDeserializer<URI>
    {
        public final static URIDeserializer instance = new URIDeserializer();

        public URIDeserializer() { super(URI.class); }
    
        @Override
        protected URI _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            return URI.create(value);
        }

        @Override
        protected URI _deserializeFromEmptyString() {
            // [#398] Need to produce non-null URI from empty String
            return URI.create("");
        }
    }
    
    public static class CurrencyDeserializer
        extends FromStringDeserializer<Currency>
    {
        public final static CurrencyDeserializer instance = new CurrencyDeserializer();

        public CurrencyDeserializer() { super(Currency.class); }
        
        @Override
        protected Currency _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            // will throw IAE if unknown:
            return Currency.getInstance(value);
        }
    }
    
    public static class PatternDeserializer
        extends FromStringDeserializer<Pattern>
    {
        public final static PatternDeserializer instance = new PatternDeserializer();

        public PatternDeserializer() { super(Pattern.class); }
        
        @Override
        protected Pattern _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            // will throw IAE (or its subclass) if malformed
            return Pattern.compile(value);
        }
    }
    
    /**
     * Kept protected as it's not meant to be extensible at this point
     */
    protected static class LocaleDeserializer
        extends FromStringDeserializer<Locale>
    {
        public final static LocaleDeserializer instance = new LocaleDeserializer();

        public LocaleDeserializer() { super(Locale.class); }
        
        @Override
        protected Locale _deserialize(String value, DeserializationContext ctxt)
            throws IOException
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
    
    public static class FileDeserializer
        extends FromStringDeserializer<File>
    {
        public final static FileDeserializer instance = new FileDeserializer();

        public FileDeserializer() { super(File.class); }
        
        @Override
        protected File _deserialize(String value, DeserializationContext ctxt)
        {
            return new File(value);
        }
    }
}
