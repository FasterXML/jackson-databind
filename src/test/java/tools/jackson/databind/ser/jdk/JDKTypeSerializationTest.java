package tools.jackson.databind.ser.jdk;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JDK types not covered by other tests (i.e. things
 * that are not Enums, Collections, Maps, or standard Date/Time types)
 */
public class JDKTypeSerializationTest
    extends DatabindTestUtil
{
    // // // Inner types from JDKTypeSerializationTest

    static final class AppId implements CharSequence {
        private final long value;

        public AppId(long value) throws IllegalArgumentException {
            this.value = value;
        }

        public static AppId valueOf(String value) throws IllegalArgumentException {
            if (value == null) {
                throw new IllegalArgumentException("value is null");
            }
            return new AppId(Long.parseLong(value));
        }

        @Override
        public int length() {
            return toString().length();
        }

        @Override
        public char charAt(int index) {
            return toString().charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return toString().subSequence(start, end);
        }

        // pay attention: no @JsonValue here
        @Override
        public String toString() {
            return Long.toString(value);
        }
    }

    static class InetAddressBean {
        public InetAddress value;

        public InetAddressBean(InetAddress i) { value = i; }
    }

    // [databind#2197]
    static class VoidBean {
        public Void value;
    }

    // // // Inner types from CustomExceptionSer5194Test

    static class MyIllegalArgumentException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public MyIllegalArgumentException() {
            super();
        }

        public MyIllegalArgumentException(String s) {
            super(s);
        }

        public MyIllegalArgumentException(String message, Throwable cause) {
            super(message, cause);
        }

        public MyIllegalArgumentException(Throwable cause) {
            super(cause);
        }
    }

    // // // Inner types from UUIDSerializationTest

    private final static String nullUUIDStr = "00000000-0000-0000-0000-000000000000";
    private final static UUID nullUUID = UUID.fromString(nullUUIDStr);

    static class UUIDWrapperVanilla {
        public UUID uuid;

        public UUIDWrapperVanilla(UUID u) { uuid = u; }
    }

    static class UUIDWrapperBinary {
        // default with JSON is String, for use of (base64-encoded) Binary:
        @JsonFormat(shape = JsonFormat.Shape.BINARY)
        public UUID uuid;

        public UUIDWrapperBinary(UUID u) { uuid = u; }
    }

    // // // Inner types from AtomicTypeSerializationTest

    static class UpperCasingSerializer extends StdScalarSerializer<String>
    {
        public UpperCasingSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator gen,
                SerializationContext provider) {
            gen.writeString(value.toUpperCase());
        }
    }

    static class UCStringWrapper {
        @JsonSerialize(contentUsing=UpperCasingSerializer.class)
        public AtomicReference<String> value;

        public UCStringWrapper(String s) { value = new AtomicReference<String>(s); }
    }

    // [datatypes-java8#17]
    @JsonPropertyOrder({ "date1", "date2", "date" })
    static class ContextualOptionals
    {
        public AtomicReference<Date> date;

        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy+MM+dd")
        public AtomicReference<Date> date1;

        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy*MM*dd")
        public AtomicReference<Date> date2;
    }

    // [databind#1673]
    static class ContainerA {
        public AtomicReference<Strategy> strategy =
                new AtomicReference<>((Strategy) new Foo(42));
    }

    static class ContainerB {
        public AtomicReference<List<Strategy>> strategy;
        {
            List<Strategy> list = new ArrayList<>();
            list.add(new Foo(42));
            strategy = new AtomicReference<>(list);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(name = "Foo", value = Foo.class) })
    interface Strategy { }

    static class Foo implements Strategy {
        public int foo;

        @JsonCreator
        Foo(@JsonProperty("foo") int foo) {
            this.foo = foo;
        }
    }

    // [databind#2565]: problems with JsonUnwrapped, non-unwrappable type
    static class MyBean2565 {
        @JsonUnwrapped
        public AtomicReference<String> maybeText = new AtomicReference<>("value");
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = sharedMapper();
    private final ObjectWriter WRITER = MAPPER.writer();

    // // // Tests from JDKTypeSerializationTest

    @Test
    public void testFile() throws IOException
    {
        // this may get translated to different representation on Windows, maybe Mac:
        File f = new File(new File("/tmp"), "foo.text");
        String str = MAPPER.writer()
                .without(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .writeValueAsString(f);
        // escape backslashes (for portability with windows)
        String escapedAbsPath = f.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\");
        assertEquals(q(escapedAbsPath), str);
    }

    @Test
    public void testRegexps() throws IOException
    {
        final String PATTERN_STR = "\\s+([a-b]+)\\w?";
        Pattern p = Pattern.compile(PATTERN_STR);
        Map<String,Object> input = new HashMap<String,Object>();
        input.put("p", p);
        Map<String,Object> result = writeAndMap(MAPPER, input);
        assertEquals(p.pattern(), result.get("p"));
    }

    @Test
    public void testCurrency() throws IOException
    {
        Currency usd = Currency.getInstance("USD");
        assertEquals(q("USD"), MAPPER.writeValueAsString(usd));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testLocale() throws IOException
    {
        assertEquals(q("en"), MAPPER.writeValueAsString(new Locale("en")));
        assertEquals(q("es-ES"), MAPPER.writeValueAsString(new Locale("es", "ES")));
        // 15-Feb-2017, tatu: wrt [databind#1600], can only assume this is expected...
        assertEquals(q("fi-FI-x-lvariant-savo"), MAPPER.writeValueAsString(new Locale("FI", "fi", "savo")));

        assertEquals(q("en-US"), MAPPER.writeValueAsString(Locale.US));

        // [databind#1123]
        assertEquals(q(""), MAPPER.writeValueAsString(Locale.ROOT));
    }

    @Test
    public void testInetAddress() throws IOException
    {
        assertEquals(q("127.0.0.1"), MAPPER.writeValueAsString(InetAddress.getByName("127.0.0.1")));
        InetAddress input = InetAddress.getByName("google.com");
        assertEquals(q("google.com"), MAPPER.writeValueAsString(input));

        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(InetAddress.class,
                        o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NUMBER)))
                .build();
        String json = mapper.writeValueAsString(input);
        assertEquals(q(input.getHostAddress()), json);

        assertEquals(String.format("{\"value\":\"%s\"}", input.getHostAddress()),
                mapper.writeValueAsString(new InetAddressBean(input)));
    }

    @Test
    public void testInetSocketAddress() throws IOException
    {
        assertEquals(q("127.0.0.1:8080"),
                MAPPER.writeValueAsString(new InetSocketAddress("127.0.0.1", 8080)));
        assertEquals(q("127.0.0.1:8080"),
                MAPPER.writeValueAsString(InetSocketAddress.createUnresolved("127.0.0.1", 8080)));
        assertEquals(q("google.com:6667"),
                MAPPER.writeValueAsString(new InetSocketAddress("google.com", 6667)));
        assertEquals(q("google.com:6667"),
                MAPPER.writeValueAsString(InetSocketAddress.createUnresolved("google.com", 6667)));
        assertEquals(q("[2001:db8:85a3:8d3:1319:8a2e:370:7348]:443"),
                MAPPER.writeValueAsString(new InetSocketAddress("2001:db8:85a3:8d3:1319:8a2e:370:7348", 443)));
    }

    @Test
    public void testClass() throws IOException
    {
        assertEquals(q("java.lang.String"), MAPPER.writeValueAsString(String.class));
        assertEquals(q("int"), MAPPER.writeValueAsString(Integer.TYPE));
        assertEquals(q("boolean"), MAPPER.writeValueAsString(Boolean.TYPE));
        assertEquals(q("void"), MAPPER.writeValueAsString(Void.TYPE));
    }

    @Test
    public void testCharset() throws IOException
    {
        assertEquals(q("UTF-8"), MAPPER.writeValueAsString(Charset.forName("UTF-8")));
    }

    // NOTE: Does NOT really belong here as it is about serialization...
    // but there's no proper place yet.
    // [databind#3305]
    @Test
    public void testCharSequenceSerialization() throws Exception
    {
        final String APP_ID = "3074457345618296002";
        AppId appId = AppId.valueOf(APP_ID);

        String serialized = MAPPER.writeValueAsString(appId);

        //Without a fix fails on JDK17 with
        // ComparisonFailure:
        //Expected :{"empty":false}
        //Actual   :"3074457345618296002"
        assertEquals("\"" + APP_ID + "\"", serialized);
    }

    // [databind#239]: Support serialization of ByteBuffer
    @Test
    public void testByteBuffer() throws IOException
    {
        final byte[] INPUT_BYTES = new byte[] { 1, 2, 3, 4, 5 };
        String exp = MAPPER.writeValueAsString(INPUT_BYTES);
        ByteBuffer bbuf = ByteBuffer.wrap(INPUT_BYTES);
        assertEquals(exp, MAPPER.writeValueAsString(bbuf));

        // so far so good, but must ensure Native buffers also work:
        ByteBuffer bbuf2 = ByteBuffer.allocateDirect(5);
        bbuf2.put(INPUT_BYTES);
        bbuf2.flip();
        assertEquals(exp, MAPPER.writeValueAsString(bbuf2));
    }

    // [databind#1662]: Sliced ByteBuffers
    @Test
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
    @Test
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

    // [databind#4164]: No rewinding for direct buffer
    @Test
    public void testDuplicatedByteBufferWithCustomPositionDirect() throws IOException
    {
        final byte[] INPUT_BYTES = new byte[] { 1, 2, 3, 4, 5 };

        String exp = MAPPER.writeValueAsString(new byte[] { 3, 4, 5 });
        ByteBuffer bbuf = ByteBuffer.allocateDirect(INPUT_BYTES.length);
        bbuf.put(INPUT_BYTES);
        bbuf.position(2);
        ByteBuffer duplicated = bbuf.duplicate();
        assertEquals(exp, MAPPER.writeValueAsString(duplicated));
    }

    // [databind#2197]
    @Test
    public void testVoidSerialization() throws Exception
    {
        assertEquals(a2q("{'value':null}"),
                MAPPER.writeValueAsString(new VoidBean()));
    }

    // [databind#2657]
    @Test
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
    @Test
    public void testThreadSerialization() throws Exception
    {
        final Thread input = Thread.currentThread();
        Map<?,?> asMap = MAPPER.convertValue(input, Map.class);

        // Should get empty "contextClassLoader"
        Map<?,?> cl = (Map<?,?>) asMap.get("contextClassLoader");
        assertNotNull(cl);
        assertEquals(0, cl.size());
    }

    // [databind#3522]: ByteArrayOutputStream
    @Test
    public void testByteArrayOutputStreamSerialization() throws Exception
    {
        byte[] bytes = new byte[] { 1, 11, 111 };
        final String exp = MAPPER.writeValueAsString(bytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(bytes);
        assertEquals(exp, MAPPER.writeValueAsString(baos));
    }

    // // // Tests from UntypedSerializationTest

    @Test
    public void testFromArray() throws Exception
    {
        ArrayList<Object> doc = new ArrayList<Object>();
        doc.add("Elem1");
        doc.add(Integer.valueOf(3));
        Map<String,Object> struct = new LinkedHashMap<String, Object>();
        struct.put("first", Boolean.TRUE);
        struct.put("Second", new ArrayList<Object>());
        doc.add(struct);
        doc.add(Boolean.FALSE);

        // loop more than once, just to ensure caching works ok (during second round)
        for (int i = 0; i < 3; ++i) {
            String str = MAPPER.writeValueAsString(doc);

            try (JsonParser p = MAPPER.createParser(str)) {
                assertEquals(JsonToken.START_ARRAY, p.nextToken());

                assertEquals(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals("Elem1", getAndVerifyText(p));

                assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(3, p.getIntValue());

                assertEquals(JsonToken.START_OBJECT, p.nextToken());
                assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
                assertEquals("first", getAndVerifyText(p));

                assertEquals(JsonToken.VALUE_TRUE, p.nextToken());
                assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
                assertEquals("Second", getAndVerifyText(p));

                if (p.nextToken() != JsonToken.START_ARRAY) {
                    fail("Expected START_ARRAY: JSON == '"+str+"'");
                }
                assertEquals(JsonToken.END_ARRAY, p.nextToken());
                assertEquals(JsonToken.END_OBJECT, p.nextToken());

                assertEquals(JsonToken.VALUE_FALSE, p.nextToken());

                assertEquals(JsonToken.END_ARRAY, p.nextToken());
                assertNull(p.nextToken());
            }
        }
    }

    @Test
    public void testFromMap() throws Exception
    {
        LinkedHashMap<String,Object> doc = new LinkedHashMap<String,Object>();

        doc.put("a1", "\"text\"");
        doc.put("int", Integer.valueOf(137));
        doc.put("foo bar", Long.valueOf(1234567890L));

        for (int i = 0; i < 3; ++i) {
            String str = MAPPER.writeValueAsString(doc);
            try (JsonParser p = MAPPER.createParser(str)) {
                assertEquals(JsonToken.START_OBJECT, p.nextToken());

                assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
                assertEquals("a1", getAndVerifyText(p));
                assertEquals(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals("\"text\"", getAndVerifyText(p));

                assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
                assertEquals("int", getAndVerifyText(p));
                assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(137, p.getIntValue());

                assertEquals(JsonToken.PROPERTY_NAME, p.nextToken());
                assertEquals("foo bar", getAndVerifyText(p));
                assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(1234567890L, p.getLongValue());

                assertEquals(JsonToken.END_OBJECT, p.nextToken());

                assertNull(p.nextToken());
            }
        }
    }

    // // // Tests from CustomExceptionSer5194Test

    // [databind#5194]: failed to serialize custom exception
    // 09-Jul-2025, tatu: Works for 2.x, fails for 3.x -- no idea why, disabled for now
    @Disabled
    @Test
    public void test5194() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultVisibility(vc -> vc
                    .withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                    .withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                    )
                .build();

        String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(new MyIllegalArgumentException());
        assertNotNull(json);
    }

    // // // Tests from UUIDSerializationTest

    // Verify that efficient UUID codec won't mess things up:
    @Test
    public void testBasicUUIDs() throws Exception
    {
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
            String json = MAPPER.writeValueAsString(uuid);
            assertEquals(q(uuid.toString()), json);

            // Also, wrt [#362], should convert cleanly
            String str = MAPPER.convertValue(uuid, String.class);
            assertEquals(value, str);
        }

        // then use templating; note that these are not exactly valid UUIDs
        // wrt spec (type bits etc), but JDK UUID should deal ok
        final String TEMPL = "00000000-0000-0000-0000-000000000000";
        final String chars = "123456789abcdef";

        for (int i = 0; i < chars.length(); ++i) {
            String value = TEMPL.replace('0', chars.charAt(i));
            UUID uuid = UUID.fromString(value);
            String json = MAPPER.writeValueAsString(uuid);
            assertEquals(q(uuid.toString()), json);
        }
    }

    @Test
    public void testShapeOverrides() throws Exception
    {
        // First, see that Binary per-property override works:
        assertEquals("{\"uuid\":\"AAAAAAAAAAAAAAAAAAAAAA==\"}",
                MAPPER.writeValueAsString(new UUIDWrapperBinary(nullUUID)));

        // but that without one we'd get String
        assertEquals("{\"uuid\":\""+nullUUIDStr+"\"}",
                MAPPER.writeValueAsString(new UUIDWrapperVanilla(nullUUID)));

        // but can also override by type
        ObjectMapper m = JsonMapper.builder()
                .withConfigOverride(UUID.class,
                        cfg -> cfg.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.BINARY))
                        )
                .build();
        assertEquals("{\"uuid\":\"AAAAAAAAAAAAAAAAAAAAAA==\"}",
                m.writeValueAsString(new UUIDWrapperVanilla(nullUUID)));
    }

    // [databind#5225]: problem with tree conversion
    @Test
    public void testTreeConversion() throws Exception
    {
        // First, reported issue
        JsonNode node = MAPPER.valueToTree(nullUUID);
        assertEquals(nullUUIDStr, node.asString());

        // and then a variations
        Object ob = MAPPER.convertValue(nullUUID, Object.class);
        assertEquals(String.class, ob.getClass());
    }

    // [databind#5323]: problem via JsonGenerator
    @Test
    public void testSerialization5323Mapper1() throws Exception
    {
        StringWriter sw = new StringWriter();
        _write5323(MAPPER.createGenerator(sw));
        _assert5323(sw.toString());
    }

    @Test
    public void testSerialization5323Mapper2() throws Exception
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        _write5323(MAPPER.createGenerator(b));
        _assert5323(b.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void testSerialization5323Mapper2b() throws Exception
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        _write5323(MAPPER.createGenerator(b, JsonEncoding.UTF8));
        _assert5323(b.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void testSerialization5323ObjectWriter1() throws Exception
    {
        StringWriter sw = new StringWriter();
        _write5323(WRITER.createGenerator(sw));
        _assert5323(sw.toString());
    }

    @Test
    public void testSerialization5323ObjectWriter2() throws Exception
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        _write5323(WRITER.createGenerator(b));
        _assert5323(b.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void testSerialization5323ObjectWriter2b() throws Exception
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        _write5323(WRITER.createGenerator(b, JsonEncoding.UTF8));
        _assert5323(b.toString(StandardCharsets.UTF_8));
    }

    // // // Tests from AtomicTypeSerializationTest

    @Test
    public void testAtomicBoolean() throws Exception
    {
        assertEquals("true", MAPPER.writeValueAsString(new AtomicBoolean(true)));
        assertEquals("false", MAPPER.writeValueAsString(new AtomicBoolean(false)));
    }

    @Test
    public void testAtomicInteger() throws Exception
    {
        assertEquals("1", MAPPER.writeValueAsString(new AtomicInteger(1)));
        assertEquals("-9", MAPPER.writeValueAsString(new AtomicInteger(-9)));
    }

    @Test
    public void testAtomicLong() throws Exception
    {
        assertEquals("0", MAPPER.writeValueAsString(new AtomicLong(0)));
    }

    @Test
    public void testAtomicReference() throws Exception
    {
        String[] strs = new String[] { "abc" };
        assertEquals("[\"abc\"]", MAPPER.writeValueAsString(new AtomicReference<String[]>(strs)));
    }

    @Test
    public void testCustomSerializer() throws Exception
    {
        final String VALUE = "fooBAR";
        String json = MAPPER.writeValueAsString(new UCStringWrapper(VALUE));
        assertEquals(json, a2q("{'value':'FOOBAR'}"));
    }

    @Test
    public void testContextualAtomicReference() throws Exception
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        ObjectMapper mapper = jsonMapperBuilder()
                .disable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .defaultDateFormat(df)
                .build();

        ContextualOptionals input = new ContextualOptionals();
        input.date = new AtomicReference<>(new Date(0L));
        input.date1 = new AtomicReference<>(new Date(0L));
        input.date2 = new AtomicReference<>(new Date(0L));
        final String json = mapper.writeValueAsString(input);
        assertEquals(a2q(
                "{'date1':'1970+01+01','date2':'1970*01*01','date':'1970/01/01'}"),
                json);
    }

    // [databind#1673]
    @Test
    public void testPolymorphicReferenceSimple() throws Exception
    {
        final String EXPECTED = "{\"type\":\"Foo\",\"foo\":42}";
        String json = MAPPER.writeValueAsString(new ContainerA());
        assertEquals("{\"strategy\":" + EXPECTED + "}", json);
    }

    // [databind#1673]
    @Test
    public void testPolymorphicReferenceListOf() throws Exception
    {
        final String EXPECTED = "{\"type\":\"Foo\",\"foo\":42}";
        // Reproduction of issue seen with scala.Option and java8 Optional types:
        // https://github.com/FasterXML/jackson-module-scala/issues/346#issuecomment-336483326
        String json = MAPPER.writeValueAsString(new ContainerB());
        assertEquals("{\"strategy\":[" + EXPECTED + "]}", json);
    }

    // [databind#2565]: problems with JsonUnwrapped, non-unwrappable type
    @Test
    public void testWithUnwrappableUnwrapped() throws Exception
    {
        assertEquals(a2q("{'maybeText':'value'}"),
                MAPPER.writeValueAsString(new MyBean2565()));
    }

    // [databind#5616]: AtomicReference with subtype, serialization as supertype
    @Test
    public void testAtomicReferenceWithSubtypeProperties() throws Exception
    {
        String json = MAPPER.writerFor(new TypeReference<AtomicReference<Strategy>>() {})
                .writeValueAsString(new AtomicReference<>(new Foo(99)));

        // Must include subtype property "foo", not just type info
        assertEquals("{\"type\":\"Foo\",\"foo\":99}", json);
    }

    // // // Private helpers from UUIDSerializationTest

    private void _write5323(JsonGenerator g) {
        g.writeStartObject();
        g.writePOJOProperty("id", nullUUID);
        g.writeEndObject();
        g.close();
    }

    private void _assert5323(String json) {
        assertEquals("{\"id\":\""+nullUUIDStr+"\"}", json);
    }
}
