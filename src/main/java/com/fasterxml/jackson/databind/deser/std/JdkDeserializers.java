package com.fasterxml.jackson.databind.deser.std;

import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

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
                Charset.class,
                AtomicBoolean.class,
                Class.class,
                StackTraceElement.class

        };
        for (Class<?> cls : numberTypes) {
            _classNames.add(cls.getName());
        }
    }

    /**
     * @deprecated Since 2.2 -- use {@link #find} instead.
     */
    @Deprecated
    public static StdDeserializer<?>[] all()
    {
        return new StdDeserializer[] {
            // from String types:
            UUIDDeserializer.instance,
            URLDeserializer.instance,
            URIDeserializer.instance,
            FileDeserializer.instance,
            CurrencyDeserializer.instance,
            PatternDeserializer.instance,
            LocaleDeserializer.instance,
            InetAddressDeserializer.instance,
            CharsetDeserializer.instance,
            
            // other types:

            // (note: AtomicInteger/Long work due to single-arg constructor;
            AtomicBooleanDeserializer.instance,
            ClassDeserializer.instance,
            StackTraceElementDeserializer.instance
        };
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
        if (rawType == Charset.class) {
            return CharsetDeserializer.instance;
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
        // should never occur
        throw new IllegalArgumentException("Internal error: can't find deserializer for "+clsName);
    }
    
    /*
    /**********************************************************
    /* Deserializer implementations: from-String deserializers
    /**********************************************************
     */
    
    public static class UUIDDeserializer
        extends FromStringDeserializer<UUID>
    {
        public final static UUIDDeserializer instance = new UUIDDeserializer();
        
        public UUIDDeserializer() { super(UUID.class); }

        @Override
        protected UUID _deserialize(String value, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return UUID.fromString(value);
        }
    
        @Override
        protected UUID _deserializeEmbedded(Object ob, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (ob instanceof byte[]) {
                byte[] bytes = (byte[]) ob;
                if (bytes.length != 16) {
                    ctxt.mappingException("Can only construct UUIDs from 16 byte arrays; got "+bytes.length+" bytes");
                }
                // clumsy, but should work for now...
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
                long l1 = in.readLong();
                long l2 = in.readLong();
                return new UUID(l1, l2);
            }
            super._deserializeEmbedded(ob, ctxt);
            return null; // never gets here
        }
    }

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
    
    /**
     * As per [JACKSON-484], also need special handling for InetAddress...
     */
    protected static class InetAddressDeserializer
        extends FromStringDeserializer<InetAddress>
    {
        public final static InetAddressDeserializer instance = new InetAddressDeserializer();

        public InetAddressDeserializer() { super(InetAddress.class); }
    
        @Override
        protected InetAddress _deserialize(String value, DeserializationContext ctxt)
            throws IOException
        {
            return InetAddress.getByName(value);
        }
    }

    // [JACKSON-789]
    protected static class CharsetDeserializer
        extends FromStringDeserializer<Charset>
    {
        public final static CharsetDeserializer instance = new CharsetDeserializer();

        public CharsetDeserializer() { super(Charset.class); }
    
        @Override
        protected Charset _deserialize(String value, DeserializationContext ctxt)
            throws IOException
        {
            return Charset.forName(value);
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
    
    /*
    /**********************************************************
    /* AtomicXxx types
    /**********************************************************
     */
    
    public static class AtomicReferenceDeserializer
        extends StdScalarDeserializer<AtomicReference<?>>
        implements ContextualDeserializer
    {
        /**
         * Type of value that we reference
         */
        protected final JavaType _referencedType;
        
        protected final JsonDeserializer<?> _valueDeserializer;
        
        /**
         * @param referencedType Parameterization of this reference
         */
        public AtomicReferenceDeserializer(JavaType referencedType) {
            this(referencedType, null);
        }
        
        public AtomicReferenceDeserializer(JavaType referencedType,
                JsonDeserializer<?> deser)
        {
            super(AtomicReference.class);
            _referencedType = referencedType;
            _valueDeserializer = deser;
        }
        
        @Override
        public AtomicReference<?> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return new AtomicReference<Object>(_valueDeserializer.deserialize(jp, ctxt));
        }
        
        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
                BeanProperty property) throws JsonMappingException
        {
            JsonDeserializer<?> deser = _valueDeserializer;
            if (deser != null) {
                return this;
            }
            return new AtomicReferenceDeserializer(_referencedType,
                    ctxt.findContextualValueDeserializer(_referencedType, property));
        }
    }

    public static class AtomicBooleanDeserializer
        extends StdScalarDeserializer<AtomicBoolean>
    {
        public final static AtomicBooleanDeserializer instance = new AtomicBooleanDeserializer();

        public AtomicBooleanDeserializer() { super(AtomicBoolean.class); }
        
        @Override
        public AtomicBoolean deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // 16-Dec-2010, tatu: Should we actually convert null to null AtomicBoolean?
            return new AtomicBoolean(_parseBooleanPrimitive(jp, ctxt));
        }
    }
    
    /*
    /**********************************************************
    /* Deserializers for other JDK types
    /**********************************************************
     */

    public static class StackTraceElementDeserializer
        extends StdScalarDeserializer<StackTraceElement>
    {
        public final static StackTraceElementDeserializer instance = new StackTraceElementDeserializer();

        public StackTraceElementDeserializer() { super(StackTraceElement.class); }
    
        @Override
        public StackTraceElement deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            // Must get an Object
            if (t == JsonToken.START_OBJECT) {
                String className = "", methodName = "", fileName = "";
                int lineNumber = -1;
    
                while ((t = jp.nextValue()) != JsonToken.END_OBJECT) {
                    String propName = jp.getCurrentName();
                    if ("className".equals(propName)) {
                        className = jp.getText();
                    } else if ("fileName".equals(propName)) {
                        fileName = jp.getText();
                    } else if ("lineNumber".equals(propName)) {
                        if (t.isNumeric()) {
                            lineNumber = jp.getIntValue();
                        } else {
                            throw JsonMappingException.from(jp, "Non-numeric token ("+t+") for property 'lineNumber'");
                        }
                    } else if ("methodName".equals(propName)) {
                        methodName = jp.getText();
                    } else if ("nativeMethod".equals(propName)) {
                        // no setter, not passed via constructor: ignore
                    } else {
                        handleUnknownProperty(jp, ctxt, _valueClass, propName);
                    }
                }
                return new StackTraceElement(className, methodName, fileName, lineNumber);
            }
            throw ctxt.mappingException(_valueClass, t);
        }
    }
}
