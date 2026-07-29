package tools.jackson.databind.deser.jdk;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Currency;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import tools.jackson.core.ObjectReadContext;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.ValueInstantiationException;
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
    public void testCharset()
    {
        assertSame(StandardCharsets.UTF_8, MAPPER.readValue(q("UTF-8"), Charset.class));
    }

    @Test
    public void testClass() throws Exception
    {
        ObjectReader classR = MAPPER.readerFor(Class.class);
        assertSame(String.class, classR.readValue(q("java.lang.String")));

        // then primitive types
        assertSame(Boolean.TYPE, classR.readValue(q("boolean")));
        assertSame(Byte.TYPE, classR.readValue(q("byte")));
        assertSame(Short.TYPE, classR.readValue(q("short")));
        assertSame(Character.TYPE, classR.readValue(q("char")));
        assertSame(Integer.TYPE, classR.readValue(q("int")));
        assertSame(Long.TYPE, classR.readValue(q("long")));
        assertSame(Float.TYPE, classR.readValue(q("float")));
        assertSame(Double.TYPE, classR.readValue(q("double")));
        assertSame(Void.TYPE, classR.readValue(q("void")));

        // and then error handling
        try {
            classR.readValue(q("UNKNOWN"));
            fail("Should not pass");
        } catch (ValueInstantiationException e) {
            verifyException(e, "instance of `java.lang.Class`");
            // 13-Feb-2026, tatu: Not a good message, should improve but...
            verifyException(e, "UNKNOWN");
        }
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
        ObjectReader r = MAPPER.readerFor(Currency.class);
        assertEquals(Currency.getInstance("USD"), r.readValue(q("USD")));

        try {
            r.readValue(q("poobah"));
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
        // Valid IPv4 address
        InetAddress address = MAPPER.readValue(q("127.0.0.1"), InetAddress.class);
        assertEquals("127.0.0.1", address.getHostAddress());

        // Valid IPv6 address
        InetAddress ip6 = MAPPER.readValue(
                q("2001:db8:85a3:8d3:1319:8a2e:370:7348"), InetAddress.class);
        assertEquals("2001:db8:85a3:8d3:1319:8a2e:370:7348", ip6.getHostAddress());

        // IPv6 loopback
        InetAddress loopback6 = MAPPER.readValue(q("::1"), InetAddress.class);
        assertEquals("0:0:0:0:0:0:0:1", loopback6.getHostAddress());
    }

    @Test
    public void testInetAddressNoDNSLookup() throws Exception
    {
        // [databind#6058]: should NOT perform DNS lookup — only IP address literals accepted
        expectInvalidInetAddress("localhost");
        expectInvalidInetAddress("google.com");
        // starts off looking like an IP address (edge case)
        expectInvalidInetAddress("1.2.3.4.example.com");
        // punycode
        expectInvalidInetAddress("xn--bcher-kva.example.com");
    }

    @Test
    public void testInetAddressNonAsciiDigits() throws Exception
    {
        // [databind#6116], follow-up to [databind#6058]: Character.digit() accepts
        // non-ASCII digits, but InetAddress.getByName() only treats ASCII literals as
        // addresses and resolves the rest via DNS, so these must be rejected too.
        // NOTE: unicode escapes used on purpose -- non-ASCII digit forms are visually
        // hard to tell apart from the ASCII ones they mimic.
        expectInvalidInetAddress("\uFF11\uFF12\uFF17.0.0.1"); // full-width "127".0.0.1
        expectInvalidInetAddress("\u0661\u0662\u0667.0.0.1"); // Arabic-Indic "127".0.0.1
        expectInvalidInetAddress("\uFF12001:db8::1"); // full-width digit in IPv6 hextet
        expectInvalidInetAddress("fe80::\uFF11"); // full-width digit in IPv6 hextet
        // and same goes for non-ASCII hex letters (radix 16), not just digits
        expectInvalidInetAddress("\uFF41bcd::1"); // full-width "a"bcd::1
    }

    private void expectInvalidInetAddress(String address) throws Exception {
        try {
            MAPPER.readValue(q(address), InetAddress.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "Not a valid IP address string literal");
        }
    }

    @Test
    public void testInetSocketAddress() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(InetSocketAddress.class);
        InetSocketAddress address = r.readValue(q("127.0.0.1"));
        assertEquals("127.0.0.1", address.getHostName());

        InetSocketAddress ip6 = r.readValue(q("2001:db8:85a3:8d3:1319:8a2e:370:7348"));
        assertEquals("2001:db8:85a3:8d3:1319:8a2e:370:7348", ip6.getHostName());

        InetSocketAddress ip6port = r.readValue(
                q("[2001:db8:85a3:8d3:1319:8a2e:370:7348]:443"));
        assertEquals("[2001:db8:85a3:8d3:1319:8a2e:370:7348]", ip6port.getHostName());
        assertEquals(443, ip6port.getPort());

        // should we try resolving host names? That requires connectivity...
        final String HOST = "www.google.com";
        address = r.readValue(q(HOST));
        assertEquals(HOST, address.getHostName());

        final String HOST_AND_PORT = HOST+":80";
        address = r.readValue(q(HOST_AND_PORT));
        assertEquals(HOST, address.getHostName());
        assertEquals(80, address.getPort());

        final String BAD_VALUE = "[2001:";
        try {
            r.readValue(q(BAD_VALUE));
            fail("Should not pass!");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize value of type `java.net.InetSocketAddress`");
            verifyException(e, "from String \""+BAD_VALUE+"\"");
            verifyException(e, "Bracketed IPv6 address must contain closing bracket");
        }
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

    // [databind#]: Reject excessively long regex patterns to prevent
    // catastrophic backtracking and excessive resource usage
    @Test
    public void testPatternLengthLimit() throws Exception
    {
        // A normal-length pattern should still work
        Pattern result = MAPPER.readValue(q("abc"), Pattern.class);
        assertEquals("abc", result.pattern());

        // A pattern at exactly the limit (1000 chars) should work
        String limitPattern = "a".repeat(1000);
        result = MAPPER.readValue(q(limitPattern), Pattern.class);
        assertEquals(limitPattern, result.pattern());

        // A pattern exceeding the limit (1001 chars) should be rejected
        String overLimitPattern = "a".repeat(1001);
        try {
            MAPPER.readValue(q(overLimitPattern), Pattern.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "regex pattern length");
            verifyException(e, "exceeds maximum");
        }
    }

    @Test
    public void testStringBuilder() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(StringBuilder.class);
        assertEquals("abc", r.readValue(q("abc")).toString());
        try {
            r.readValue("[ ]");
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.lang.StringBuilder` from Array value");
        }
    }

    @Test
    public void testStringBuffer() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(StringBuffer.class);
        assertEquals("def", r.readValue(q("def")).toString());
        try {
            r.readValue("[ ]");
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.lang.StringBuffer` from Array value");
        }
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
