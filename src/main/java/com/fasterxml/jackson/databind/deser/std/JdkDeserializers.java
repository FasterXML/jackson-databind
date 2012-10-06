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

@SuppressWarnings("serial")
public class JdkDeserializers
{
    public static StdDeserializer<?>[] all()
    {
        return new StdDeserializer[] {

            // from String types:
            new StringDeserializer(),
            new UUIDDeserializer(),
            new URLDeserializer(),
            new URIDeserializer(),
            new CurrencyDeserializer(),
            new PatternDeserializer(),
            new LocaleDeserializer(),
            new InetAddressDeserializer(),
            new CharsetDeserializer(),

            // other types:

            // (note: AtomicInteger/Long work due to single-arg constructor;
            new AtomicBooleanDeserializer(),
            new ClassDeserializer(),
            new StackTraceElementDeserializer()
        };
    }
    
    /*
    /**********************************************************
    /* Deserializer implementations: from-String deserializers
    /**********************************************************
     */
    
    /**
     * Note: final as performance optimization: not expected to need sub-classing;
     * if sub-classing was needed could re-factor into reusable part, final
     * "Impl" sub-class
     */
    /*
    @JacksonStdImpl
    public final static class StringDeserializer
        extends StdScalarDeserializer<String>
    {
        public StringDeserializer() { super(String.class); }

        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // 22-Sep-2012, tatu: For 2.1, use this new method, may force coercion:
            String text = jp.getValueAsString();
            if (text != null) {
                return text;
            }
            // [JACKSON-330]: need to gracefully handle byte[] data, as base64
            JsonToken curr = jp.getCurrentToken();
            if (curr == JsonToken.VALUE_EMBEDDED_OBJECT) {
                Object ob = jp.getEmbeddedObject();
                if (ob == null) {
                    return null;
                }
                if (ob instanceof byte[]) {
                    return Base64Variants.getDefaultVariant().encode((byte[]) ob, false);
                }
                // otherwise, try conversion using toString()...
                return ob.toString();
            }
            throw ctxt.mappingException(_valueClass, curr);
        }

        // 1.6: since we can never have type info ("natural type"; String, Boolean, Integer, Double):
        // (is it an error to even call this version?)
        @Override
        public String deserializeWithType(JsonParser jp, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer)
            throws IOException, JsonProcessingException
        {
            return deserialize(jp, ctxt);
        }
    }
    */
    
    public static class UUIDDeserializer
        extends FromStringDeserializer<UUID>
    {
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
        public CharsetDeserializer() { super(Charset.class); }
    
        @Override
        protected Charset _deserialize(String value, DeserializationContext ctxt)
            throws IOException
        {
            return Charset.forName(value);
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
        
//        @Override
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
