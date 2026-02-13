package tools.jackson.databind.deser.jdk;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Currency;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import tools.jackson.core.ObjectReadContext;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

public class JDKStringLikeTypeDeserTest
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

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testCharset() throws Exception
    {
        Charset UTF8 = Charset.forName("UTF-8");
        assertSame(UTF8, MAPPER.readValue(q("UTF-8"), Charset.class));
    }

    @Test
    public void testClass() throws Exception
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

    @Test
    public void testClassWithParams() throws Exception
    {
        String json = MAPPER.writeValueAsString(new ParamClassBean("Foobar"));

        ParamClassBean result = MAPPER.readValue(json, ParamClassBean.class);
        assertEquals("Foobar", result.name);
        assertSame(String.class, result.clazz);
    }

    @Test
    public void testCurrency() throws Exception
    {
        Currency usd = Currency.getInstance("USD");
        assertEquals(usd, MAPPER.readValue(q("USD"), Currency.class));

        try {
            MAPPER.readValue(q("poobah"), Currency.class);
            fail("Should not pass!");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize value of type `java.util.Currency` from String \"Poobah\"");
            verifyException(e, "Unrecognized currency");
        }
    }

    @Test
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

    @Test
    public void testCharSequence() throws Exception
    {
        CharSequence cs = MAPPER.readValue("\"abc\"", CharSequence.class);
        assertEquals(String.class, cs.getClass());
        assertEquals("abc", cs.toString());
    }

    @Test
    public void testInetAddress() throws Exception
    {
        InetAddress address = MAPPER.readValue(q("127.0.0.1"), InetAddress.class);
        assertEquals("127.0.0.1", address.getHostAddress());

        // should we try resolving host names? That requires connectivity...
        final String HOST = "google.com";
        address = MAPPER.readValue(q(HOST), InetAddress.class);
        assertEquals(HOST, address.getHostName());
    }

    @Test
    public void testInetSocketAddress() throws Exception
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

    @Test
    public void testPattern() throws Exception
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
        // somewhat gracefully
        try {
            MAPPER.readValue(q("[abc"), Pattern.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize value of type `java.util.regex.Pattern` from String \"[abc\"");
            verifyException(e, "Invalid pattern, problem");
        }
    }

    @Test
    public void testStringBuilder() throws Exception
    {
        StringBuilder sb = MAPPER.readValue(q("abc"), StringBuilder.class);
        assertEquals("abc", sb.toString());
    }

    @Test
    public void testStringBuffer() throws Exception
    {
        StringBuffer sb = MAPPER.readValue(q("abc"), StringBuffer.class);
        assertEquals("abc", sb.toString());
    }

    @Test
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

    @Test
    public void testURL() throws Exception
    {
        URL exp = new URL("http://foo.com");
        assertEquals(exp, MAPPER.readValue("\""+exp.toString()+"\"", URL.class));

        // trivial case; null to null, embedded URL to URL
        TokenBuffer buf = TokenBuffer.forGeneration();
        buf.writePOJO(null);
        assertNull(MAPPER.readValue(buf.asParser(ObjectReadContext.empty()), URL.class));
        buf.close();

        // then, URLitself come as is:
        buf = TokenBuffer.forGeneration();
        buf.writePOJO(exp);
        assertSame(exp, MAPPER.readValue(buf.asParser(ObjectReadContext.empty()), URL.class));
        buf.close();
    }

    public void testURLInvalid() throws Exception
    {
        // and finally, invalid URL should be handled appropriately too
        try {
            URL result = MAPPER.readValue(q("a b"), URL.class);
            fail("Should not accept malformed URI, instead got: "+result);
        } catch (InvalidFormatException e) {
            verifyException(e, "not a valid textual representation");
        }
    }


}
