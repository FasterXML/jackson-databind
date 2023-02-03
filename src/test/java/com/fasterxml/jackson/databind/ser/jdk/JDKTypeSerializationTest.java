package com.fasterxml.jackson.databind.ser.jdk;

import java.io.*;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for JDK types not covered by other tests (i.e. things
 * that are not Enums, Collections, Maps, or standard Date/Time types)
 */
public class JDKTypeSerializationTest
    extends BaseMapTest
{
    private final ObjectMapper MAPPER = sharedMapper();

    static class InetAddressBean {
        public InetAddress value;

        public InetAddressBean(InetAddress i) { value = i; }
    }

    // [databind#2197]
    static class VoidBean {
        public Void value;
    }

    public void testBigDecimal() throws Exception
    {
        Map<String, Object> map = new HashMap<String, Object>();
        String PI_STR = "3.14159265";
        map.put("pi", new BigDecimal(PI_STR));
        String str = MAPPER.writeValueAsString(map);
        assertEquals("{\"pi\":3.14159265}", str);
    }

    public void testBigDecimalAsPlainString() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();

        mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        Map<String, Object> map = new HashMap<String, Object>();
        String PI_STR = "3.00000000";
        map.put("pi", new BigDecimal(PI_STR));
        String str = mapper.writeValueAsString(map);
        assertEquals("{\"pi\":3.00000000}", str);
    }

    public void testFile() throws IOException
    {
        // this may get translated to different representation on Windows, maybe Mac:
        File f = new File(new File("/tmp"), "foo.text");
        String str = MAPPER.writeValueAsString(f);
        // escape backslashes (for portability with windows)
        String escapedAbsPath = f.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\");
        assertEquals(q(escapedAbsPath), str);
    }

    public void testRegexps() throws IOException
    {
        final String PATTERN_STR = "\\s+([a-b]+)\\w?";
        Pattern p = Pattern.compile(PATTERN_STR);
        Map<String,Object> input = new HashMap<String,Object>();
        input.put("p", p);
        Map<String,Object> result = writeAndMap(MAPPER, input);
        assertEquals(p.pattern(), result.get("p"));
    }

    public void testCurrency() throws IOException
    {
        Currency usd = Currency.getInstance("USD");
        assertEquals(q("USD"), MAPPER.writeValueAsString(usd));
    }

    public void testLocale() throws IOException
    {
        assertEquals(q("en"), MAPPER.writeValueAsString(new Locale("en")));
        assertEquals(q("es_ES"), MAPPER.writeValueAsString(new Locale("es", "ES")));
        assertEquals(q("fi_FI_savo"), MAPPER.writeValueAsString(new Locale("FI", "fi", "savo")));

        assertEquals(q("en_US"), MAPPER.writeValueAsString(Locale.US));

        // [databind#1123]
        assertEquals(q(""), MAPPER.writeValueAsString(Locale.ROOT));
    }

    public void testInetAddress() throws IOException
    {
        assertEquals(q("127.0.0.1"), MAPPER.writeValueAsString(InetAddress.getByName("127.0.0.1")));
        InetAddress input = InetAddress.getByName("google.com");
        assertEquals(q("google.com"), MAPPER.writeValueAsString(input));

        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(InetAddress.class)
            .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NUMBER));
        String json = mapper.writeValueAsString(input);
        assertEquals(q(input.getHostAddress()), json);

        assertEquals(String.format("{\"value\":\"%s\"}", input.getHostAddress()),
                mapper.writeValueAsString(new InetAddressBean(input)));
    }

    public void testInetSocketAddress() throws IOException
    {
        assertEquals(q("127.0.0.1:8080"),
                MAPPER.writeValueAsString(new InetSocketAddress("127.0.0.1", 8080)));
        assertEquals(q("google.com:6667"),
                MAPPER.writeValueAsString(new InetSocketAddress("google.com", 6667)));
        assertEquals(q("[2001:db8:85a3:8d3:1319:8a2e:370:7348]:443"),
                MAPPER.writeValueAsString(new InetSocketAddress("2001:db8:85a3:8d3:1319:8a2e:370:7348", 443)));
    }

    // [JACKSON-597]
    public void testClass() throws IOException
    {
        assertEquals(q("java.lang.String"), MAPPER.writeValueAsString(String.class));
        assertEquals(q("int"), MAPPER.writeValueAsString(Integer.TYPE));
        assertEquals(q("boolean"), MAPPER.writeValueAsString(Boolean.TYPE));
        assertEquals(q("void"), MAPPER.writeValueAsString(Void.TYPE));
    }

    public void testCharset() throws IOException
    {
        assertEquals(q("UTF-8"), MAPPER.writeValueAsString(Charset.forName("UTF-8")));
    }

    // [databind#239]: Support serialization of ByteBuffer
    public void testByteBuffer() throws IOException
    {
        final byte[] INPUT_BYTES = new byte[] { 1, 2, 3, 4, 5 };
        String exp = MAPPER.writeValueAsString(INPUT_BYTES);
        ByteBuffer bbuf = ByteBuffer.wrap(INPUT_BYTES);
        assertEquals(exp, MAPPER.writeValueAsString(bbuf));

        // so far so good, but must ensure Native buffers also work:
        ByteBuffer bbuf2 = ByteBuffer.allocateDirect(5);
        bbuf2.put(INPUT_BYTES);
        assertEquals(exp, MAPPER.writeValueAsString(bbuf2));
    }

    // [databind#1662]: Sliced ByteBuffers
    public void testSlicedByteBuffer() throws IOException
    {
        final byte[] INPUT_BYTES = new byte[] { 1, 2, 3, 4, 5 };
        ByteBuffer bbuf = ByteBuffer.wrap(INPUT_BYTES);

        bbuf.position(2);
        ByteBuffer slicedBuf = bbuf.slice();

        assertEquals(MAPPER.writeValueAsString(new byte[] { 3, 4, 5 }),
                MAPPER.writeValueAsString(slicedBuf));

        // but how about offset within?
        slicedBuf.position(1);
        assertEquals(MAPPER.writeValueAsString(new byte[] { 4, 5 }),
                MAPPER.writeValueAsString(slicedBuf));
    }

    // [databind#2602]: Need to consider position()
    public void testDuplicatedByteBufferWithCustomPosition() throws IOException
    {
        final byte[] INPUT_BYTES = new byte[] { 1, 2, 3, 4, 5 };

        String exp = MAPPER.writeValueAsString(new byte[] { 3, 4, 5 });
        ByteBuffer bbuf = ByteBuffer.wrap(INPUT_BYTES);
        bbuf.position(2);
        ByteBuffer duplicated = bbuf.duplicate();
        assertEquals(exp, MAPPER.writeValueAsString(duplicated));

        // also check differently constructed bytebuffer (noting that
        // offset given is the _position_ to use, NOT array offset
        exp = MAPPER.writeValueAsString(new byte[] { 2, 3, 4 });
        bbuf = ByteBuffer.wrap(INPUT_BYTES, 1, 3);
        assertEquals(exp, MAPPER.writeValueAsString(bbuf.duplicate()));
    }

    // [databind#2197]
    public void testVoidSerialization() throws Exception
    {
        assertEquals(a2q("{'value':null}"),
                MAPPER.writeValueAsString(new VoidBean()));
    }

    // [databind#2657]
    public void testNonStandardProperties() throws Exception
    {
        Properties properties = new Properties();
        // Bad usage: Properties should NOT contain non-Strings. But
        // some do that regardless and compiler won't stop it so.
        properties.put("key", 1);
        String json = MAPPER.writeValueAsString(properties);
        assertEquals("{\"key\":1}", json);
    }

    // [databind#3130]: fails on JDK 11+
    public void testThreadSerialization() throws Exception
    {
        final Thread input = Thread.currentThread();
//        String json = MAPPER.writerWithDefaultPrettyPrinter()
//                .writeValueAsString(input);
        Map<?,?> asMap = MAPPER.convertValue(input, Map.class);
//        System.err.println("PROPS -> "+asMap.keySet());

        // Should get empty "contextClassLoader"
        Map<?,?> cl = (Map<?,?>) asMap.get("contextClassLoader");
        assertNotNull(cl);
        assertEquals(0, cl.size());
    }
}
