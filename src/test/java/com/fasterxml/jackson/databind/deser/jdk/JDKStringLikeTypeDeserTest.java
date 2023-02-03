package com.fasterxml.jackson.databind.deser.jdk;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.TokenBuffer;

public class JDKStringLikeTypeDeserTest extends BaseMapTest
{
    static class ParamClassBean
    {
         public String name = "bar";
         public Class<String> clazz ;

         public ParamClassBean() { }
         public ParamClassBean(String name) {
             this.name = name;
             clazz = String.class;
         }
    }

    // [databind#429]
    static class StackTraceBean {
        public final static int NUM = 13;

        @JsonProperty("Location")
        @JsonDeserialize(using=MyStackTraceElementDeserializer.class)
        protected StackTraceElement location;
    }

    @SuppressWarnings("serial")
    static class MyStackTraceElementDeserializer extends StdDeserializer<StackTraceElement>
    {
        public MyStackTraceElementDeserializer() { super(StackTraceElement.class); }

        @Override
        public StackTraceElement deserialize(JsonParser jp,
                DeserializationContext ctxt) throws IOException {
            jp.skipChildren();
            return new StackTraceElement("a", "b", "b", StackTraceBean.NUM);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    // [databind#239]
    public void testByteBuffer() throws Exception
    {
        byte[] INPUT = new byte[] { 1, 3, 9, -1, 6 };
        String exp = MAPPER.writeValueAsString(INPUT);
        ByteBuffer result = MAPPER.readValue(exp,  ByteBuffer.class);
        assertNotNull(result);
        assertEquals(INPUT.length, result.remaining());
        for (int i = 0; i < INPUT.length; ++i) {
            assertEquals(INPUT[i], result.get());
        }
        assertEquals(0, result.remaining());
    }

    public void testCharset() throws Exception
    {
        Charset UTF8 = Charset.forName("UTF-8");
        assertSame(UTF8, MAPPER.readValue(q("UTF-8"), Charset.class));
    }

    public void testClass() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        assertSame(String.class, mapper.readValue(q("java.lang.String"), Class.class));

        // then primitive types
        assertSame(Boolean.TYPE, mapper.readValue(q("boolean"), Class.class));
        assertSame(Byte.TYPE, mapper.readValue(q("byte"), Class.class));
        assertSame(Short.TYPE, mapper.readValue(q("short"), Class.class));
        assertSame(Character.TYPE, mapper.readValue(q("char"), Class.class));
        assertSame(Integer.TYPE, mapper.readValue(q("int"), Class.class));
        assertSame(Long.TYPE, mapper.readValue(q("long"), Class.class));
        assertSame(Float.TYPE, mapper.readValue(q("float"), Class.class));
        assertSame(Double.TYPE, mapper.readValue(q("double"), Class.class));
        assertSame(Void.TYPE, mapper.readValue(q("void"), Class.class));
    }

    public void testClassWithParams() throws IOException
    {
        String json = MAPPER.writeValueAsString(new ParamClassBean("Foobar"));

        ParamClassBean result = MAPPER.readValue(json, ParamClassBean.class);
        assertEquals("Foobar", result.name);
        assertSame(String.class, result.clazz);
    }

    public void testCurrency() throws IOException
    {
        Currency usd = Currency.getInstance("USD");
        assertEquals(usd, new ObjectMapper().readValue(q("USD"), Currency.class));
    }

    public void testFile() throws Exception
    {
        // Not portable etc... has to do:
        File src = new File("/test").getAbsoluteFile();
        String abs = src.getAbsolutePath();

        // escape backslashes (for portability with windows)
        String json = MAPPER.writeValueAsString(abs);
        File result = MAPPER.readValue(json, File.class);
        assertEquals(abs, result.getAbsolutePath());
    }

    public void testLocale() throws IOException
    {
        assertEquals(new Locale("en"), MAPPER.readValue(q("en"), Locale.class));
        assertEquals(new Locale("es", "ES"), MAPPER.readValue(q("es_ES"), Locale.class));
        assertEquals(new Locale("FI", "fi", "savo"),
                MAPPER.readValue(q("fi_FI_savo"), Locale.class));
        assertEquals(new Locale("en", "US"),
                MAPPER.readValue(q("en-US"), Locale.class));
    }

    public void testCharSequence() throws IOException
    {
        CharSequence cs = MAPPER.readValue("\"abc\"", CharSequence.class);
        assertEquals(String.class, cs.getClass());
        assertEquals("abc", cs.toString());
    }

    public void testInetAddress() throws IOException
    {
        InetAddress address = MAPPER.readValue(q("127.0.0.1"), InetAddress.class);
        assertEquals("127.0.0.1", address.getHostAddress());

        // should we try resolving host names? That requires connectivity...
        final String HOST = "google.com";
        address = MAPPER.readValue(q(HOST), InetAddress.class);
        assertEquals(HOST, address.getHostName());
    }

    public void testInetSocketAddress() throws IOException
    {
        InetSocketAddress address = MAPPER.readValue(q("127.0.0.1"), InetSocketAddress.class);
        assertEquals("127.0.0.1", address.getAddress().getHostAddress());

        InetSocketAddress ip6 = MAPPER.readValue(
                q("2001:db8:85a3:8d3:1319:8a2e:370:7348"), InetSocketAddress.class);
        assertEquals("2001:db8:85a3:8d3:1319:8a2e:370:7348", ip6.getAddress().getHostAddress());

        InetSocketAddress ip6port = MAPPER.readValue(
                q("[2001:db8:85a3:8d3:1319:8a2e:370:7348]:443"), InetSocketAddress.class);
        assertEquals("2001:db8:85a3:8d3:1319:8a2e:370:7348", ip6port.getAddress().getHostAddress());
        assertEquals(443, ip6port.getPort());

        // should we try resolving host names? That requires connectivity...
        final String HOST = "www.google.com";
        address = MAPPER.readValue(q(HOST), InetSocketAddress.class);
        assertEquals(HOST, address.getHostName());

        final String HOST_AND_PORT = HOST+":80";
        address = MAPPER.readValue(q(HOST_AND_PORT), InetSocketAddress.class);
        assertEquals(HOST, address.getHostName());
        assertEquals(80, address.getPort());
    }

    public void testPattern() throws IOException
    {
        Pattern exp = Pattern.compile("abc:\\s?(\\d+)");
        // Ok: easiest way is to just serialize first; problem
        // is the backslash
        String json = MAPPER.writeValueAsString(exp);
        Pattern result = MAPPER.readValue(json, Pattern.class);
        assertEquals(exp.pattern(), result.pattern());

        // [databind#3290]: actually need to retain at least trailing space
        // (and since we do that, just retain all...)
        exp = Pattern.compile("^WIN\\ ");
        json = MAPPER.writeValueAsString(exp);
        result = MAPPER.readValue(json, Pattern.class);
        assertEquals(exp.pattern(), result.pattern());

        // [databind#3598]: should also handle invalid pattern serialization
        // somehwat gracefully
        try {
            MAPPER.readValue(q("[abc"), Pattern.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "not a valid textual representation, problem: Unclosed character class");
        }
    }

    public void testStackTraceElement() throws Exception
    {
        StackTraceElement elem = null;
        try {
            throw new IllegalStateException();
        } catch (Exception e) {
            elem = e.getStackTrace()[0];
        }
        String json = MAPPER.writeValueAsString(elem);
        StackTraceElement back = MAPPER.readValue(json, StackTraceElement.class);

        assertEquals("testStackTraceElement", back.getMethodName());
        assertEquals(elem.getLineNumber(), back.getLineNumber());
        assertEquals(elem.getClassName(), back.getClassName());
        assertEquals(elem.isNativeMethod(), back.isNativeMethod());
        assertTrue(back.getClassName().endsWith("JDKStringLikeTypeDeserTest"));
        assertFalse(back.isNativeMethod());
    }

    // [databind#429]
    public void testStackTraceElementWithCustom() throws Exception
    {
        // first, via bean that contains StackTraceElement
        StackTraceBean bean = MAPPER.readValue(a2q("{'Location':'foobar'}"),
                StackTraceBean.class);
        assertNotNull(bean);
        assertNotNull(bean.location);
        assertEquals(StackTraceBean.NUM, bean.location.getLineNumber());

        // and then directly, iff registered
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(StackTraceElement.class, new MyStackTraceElementDeserializer());
        mapper.registerModule(module);

        StackTraceElement elem = mapper.readValue("123", StackTraceElement.class);
        assertNotNull(elem);
        assertEquals(StackTraceBean.NUM, elem.getLineNumber());

        // and finally, even as part of real exception

        IOException ioe = mapper.readValue(a2q("{'stackTrace':[ 123, 456 ]}"),
                IOException.class);
        assertNotNull(ioe);
        StackTraceElement[] traces = ioe.getStackTrace();
        assertNotNull(traces);
        assertEquals(2, traces.length);
        assertEquals(StackTraceBean.NUM, traces[0].getLineNumber());
        assertEquals(StackTraceBean.NUM, traces[1].getLineNumber());
    }

    public void testStringBuilder() throws Exception
    {
        StringBuilder sb = MAPPER.readValue(q("abc"), StringBuilder.class);
        assertEquals("abc", sb.toString());
    }

    public void testStringBuffer() throws Exception
    {
        StringBuffer sb = MAPPER.readValue(q("abc"), StringBuffer.class);
        assertEquals("abc", sb.toString());
    }

    public void testURI() throws Exception
    {
        final ObjectReader reader = MAPPER.readerFor(URI.class);
        final URI value = new URI("http://foo.com");
        assertEquals(value, reader.readValue("\""+value.toString()+"\""));

        // and finally: broken URI should give proper failure
        try {
            URI result = reader.readValue(q("a b"));
            fail("Should not accept malformed URI, instead got: "+result);
        } catch (InvalidFormatException e) {
            verifyException(e, "not a valid textual representation");
        }
    }

    public void testURL() throws Exception
    {
        URL exp = new URL("http://foo.com");
        assertEquals(exp, MAPPER.readValue("\""+exp.toString()+"\"", URL.class));

        // trivial case; null to null, embedded URL to URL
        TokenBuffer buf = new TokenBuffer(null, false);
        buf.writeObject(null);
        assertNull(MAPPER.readValue(buf.asParser(), URL.class));
        buf.close();

        // then, URLitself come as is:
        buf = new TokenBuffer(null, false);
        buf.writeObject(exp);
        assertSame(exp, MAPPER.readValue(buf.asParser(), URL.class));
        buf.close();

        // and finally, invalid URL should be handled appropriately too
        try {
            URL result = MAPPER.readValue(q("a b"), URL.class);
            fail("Should not accept malformed URI, instead got: "+result);
        } catch (InvalidFormatException e) {
            verifyException(e, "not a valid textual representation");
        }
    }

    public void testUUID() throws Exception
    {
        final String NULL_UUID = "00000000-0000-0000-0000-000000000000";
        final ObjectReader r = MAPPER.readerFor(UUID.class);

        // first, couple of generated UUIDs:
        for (String value : new String[] {
                "76e6d183-5f68-4afa-b94a-922c1fdb83f8",
                "540a88d1-e2d8-4fb1-9396-9212280d0a7f",
                "2c9e441d-1cd0-472d-9bab-69838f877574",
                "591b2869-146e-41d7-8048-e8131f1fdec5",
                "82994ac2-7b23-49f2-8cc5-e24cf6ed77be",
                "00000007-0000-0000-0000-000000000000"
        }) {
            UUID uuid = UUID.fromString(value);
            assertEquals(uuid,
                    r.without(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                        .readValue(q(value)));
        }
        // then use templating; note that these are not exactly valid UUIDs
        // wrt spec (type bits etc), but JDK UUID should deal ok
        final String TEMPL = NULL_UUID;
        final String chars = "123456789abcdefABCDEF";

        for (int i = 0; i < chars.length(); ++i) {
            String value = TEMPL.replace('0', chars.charAt(i));
            assertEquals(UUID.fromString(value).toString(),
                    r.readValue(q(value)).toString());
        }

        // also: see if base64 encoding works as expected
        String base64 = Base64Variants.getDefaultVariant().encode(new byte[16]);
        assertEquals(UUID.fromString(NULL_UUID),
                r.readValue(q(base64)));
    }

    public void testUUIDInvalid() throws Exception
    {
        // and finally, exception handling too [databind#1000], for invalid cases
        try {
            MAPPER.readValue(q("abcde"), UUID.class);
            fail("Should fail on invalid UUID string");
        } catch (InvalidFormatException e) {
            verifyException(e, "UUID has to be represented by standard");
        }
        try {
            MAPPER.readValue(q("76e6d183-5f68-4afa-b94a-922c1fdb83fx"), UUID.class);
            fail("Should fail on invalid UUID string");
        } catch (InvalidFormatException e) {
            verifyException(e, "non-hex character 'x'");
        }
        // should also test from-bytes version, but that's trickier... leave for now.
    }

    public void testUUIDAux() throws Exception
    {
        final UUID value = UUID.fromString("76e6d183-5f68-4afa-b94a-922c1fdb83f8");

        // first, null should come as null
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeObject(null);
            assertNull(MAPPER.readValue(buf.asParser(), UUID.class));
        }

        // then, UUID itself come as is:
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeObject(value);
            assertSame(value, MAPPER.readValue(buf.asParser(), UUID.class));

            // and finally from byte[]
            // oh crap; JDK UUID just... sucks. Not even byte[] accessors or constructors? Huh?
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeLong(value.getMostSignificantBits());
            out.writeLong(value.getLeastSignificantBits());
            out.close();
            byte[] data = bytes.toByteArray();
            assertEquals(16, data.length);

            buf.writeObject(data);

            UUID value2 = MAPPER.readValue(buf.asParser(), UUID.class);

            assertEquals(value, value2);
        }
    }
}
