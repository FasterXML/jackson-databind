package tools.jackson.databind.util;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.JsonParser.NumberType;
import tools.jackson.core.JsonParser.NumberTypeFP;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.type.TypeReference;
import tools.jackson.core.util.JsonParserSequence;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
public class TokenBufferTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    static class Base1730 { }

    static class Sub1730 extends Base1730 { }

    // [databind#3816]
    @JsonSerialize(using = Serializer3816.class)
    static class Foo3816 { }

    static class Serializer3816 extends StdSerializer<Foo3816> {
        Serializer3816() {
            super(Foo3816.class);
        }

        @Override
        public void serialize(Foo3816 value, JsonGenerator gen, SerializationContext provider) {
            gen.writeStartObject();
            gen.writeName("field");
            gen.writeString(new StringReader("foobar"), 6);
            gen.writeEndObject();
        }
    }    

    /*
    /**********************************************************************
    /* Basic TokenBuffer tests, direct access
    /**********************************************************************
     */

    @Test
    public void testBasicConfig() throws IOException
    {
        TokenBuffer buf;

        buf = TokenBuffer.forGeneration();
        assertEquals(MAPPER.version(), buf.version());
        assertNotNull(buf.streamWriteContext());
        assertFalse(buf.isClosed());
        assertTrue(buf.isEmpty());

        assertFalse(buf.isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN));
        buf.configure(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        assertTrue(buf.isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN));
        buf.configure(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN, false);
        assertFalse(buf.isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN));
        buf.close();
        assertTrue(buf.isClosed());
    }

    // for [databind#3528]
    @Test
    public void testParserFeatureDefaults() throws IOException
    {
        TokenBuffer buf = TokenBuffer.forGeneration();
        try (JsonParser p = buf.asParser()) {
            for (StreamReadFeature feat : StreamReadFeature.values()) {
                assertEquals(feat.enabledByDefault(), p.isEnabled(feat), "Feature "+feat);
            }
        }
    }

    /**
     * Test writing of individual simple values
     */
    @Test
    public void testSimpleWrites() throws IOException
    {
        TokenBuffer buf = TokenBuffer.forGeneration();
        assertTrue(buf.isEmpty());

        // First, with empty buffer
        JsonParser p = buf.asParser(ObjectReadContext.empty());
        assertNull(p.currentToken());
        assertNull(p.nextToken());
        p.close();

        // Then with simple text
        buf.writeString("abc");
        assertFalse(buf.isEmpty());

        p = buf.asParser(ObjectReadContext.empty());
        assertNull(p.currentToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("abc", p.getString());
        assertNull(p.nextToken());
        p.close();

        // Then, let's append at root level
        buf.writeNumber(13);
        p = buf.asParser(ObjectReadContext.empty());
        assertNull(p.currentToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(13, p.getIntValue());
        assertNull(p.nextToken());
        p.close();
        buf.close();
    }

    // For 2.9, explicit "isNaN" check
    @Test
    public void testSimpleNumberWrites() throws IOException
    {
        TokenBuffer buf = TokenBuffer.forGeneration();

        double[] values1 = new double[] {
                0.25, Double.NaN, -2.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY
        };
        float[] values2 = new float[] {
                Float.NEGATIVE_INFINITY,
                0.25f,
                Float.POSITIVE_INFINITY
        };

        for (double v : values1) {
            buf.writeNumber(v);
        }
        for (float v : values2) {
            buf.writeNumber(v);
        }

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        assertNull(p.currentToken());

        for (double v : values1) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(NumberType.DOUBLE, p.getNumberType());
            // not retained from JSON, but is when comes as specific number:
            assertEquals(NumberTypeFP.DOUBLE64, p.getNumberTypeFP());
            double actual = p.getDoubleValue();
            boolean expNan = Double.isNaN(v) || Double.isInfinite(v);
            assertEquals(expNan, p.isNaN());
            assertEquals(0, Double.compare(v, actual));
        }
        for (float v : values2) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            // Exact number type info retained when buffering:
            assertEquals(NumberType.FLOAT, p.getNumberType());
            assertEquals(NumberTypeFP.FLOAT32, p.getNumberTypeFP());
            float actual = p.getFloatValue();
            boolean expNan = Float.isNaN(v) || Float.isInfinite(v);
            assertEquals(expNan, p.isNaN());
            assertEquals(0, Float.compare(v, actual));
        }
        p.close();
        buf.close();
    }

    // [databind#1729]
    @Test
    public void testNumberOverflowInt() throws IOException
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            long big = 1L + Integer.MAX_VALUE;
            buf.writeNumber(big);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(NumberType.LONG, p.getNumberType());
                try {
                    p.getIntValue();
                    fail("Expected failure for `int` overflow");
                } catch (InputCoercionException e) {
                    verifyException(e, "Numeric value ("+big+") out of range of `int`");
                }
            }
        }
        // and ditto for coercion.
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            long big = 1L + Integer.MAX_VALUE;
            buf.writeNumber(String.valueOf(big));
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                // NOTE: oddity of buffering, no inspection of "real" type if given String...
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                try {
                    p.getIntValue();
                    fail("Expected failure for `int` overflow");
                } catch (InputCoercionException e) {
                    verifyException(e, "Numeric value ("+big+") out of range of `int`");
                }
            }
        }
    }

    @Test
    public void testNumberOverflowLong() throws IOException
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            BigInteger big = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
            buf.writeNumber(big);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
                try {
                    p.getLongValue();
                    fail("Expected failure for `long` overflow");
                } catch (InputCoercionException e) {
                    verifyException(e, "Numeric value ("+big+") out of range of `long`");
                }
            }
        }
    }

    @Test
    public void testNumberIntAsString() throws IOException
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber("123", true);
            try (JsonParser p = buf.asParser()) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
                assertEquals(123, p.getIntValue());
            }
        }
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber("-123", true);
            try (JsonParser p = buf.asParser()) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
                assertEquals(-123L, p.getLongValue());
            }
        }
        // [databind#3524] String-backed float defaults to DOUBLE, not BIG_DECIMAL
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber("123", false);
            try (JsonParser p = buf.asParser()) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(NumberType.DOUBLE, p.getNumberType());
                assertEquals(123.0, p.getFloatValue());
            }
        }
        // legacy method assumes Decimal type
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber("123");
            try (JsonParser p = buf.asParser()) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(NumberType.DOUBLE, p.getNumberType());
                assertEquals(123.0, p.getFloatValue());
            }
        }
    }

    @Test
    public void testNumberNonIntAsStringNoCoerce() throws IOException
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber("1234.567", true);
            try (JsonParser p = buf.asParser()) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
                assertThrows(NumberFormatException.class, p::getIntValue);
            }
        }
    }

    @Test
    public void testBigIntAsString() throws IOException
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            BigInteger big = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(1));
            buf.writeNumber(big.toString());
            try (JsonParser p = buf.asParser()) {
                // NOTE: oddity of buffering, no inspection of "real" type if given String...
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(big, p.getBigIntegerValue());
            }
        }
    }

    @Test
    public void testBigDecimalAsString() throws IOException
    {
        final String num = "-10000000000.0000000001";
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(num);
            try (JsonParser p = buf.asParser()) {
                // NOTE: oddity of buffering, no inspection of "real" type if given String...
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(new BigDecimal(num), p.getDecimalValue());
            }
        }
    }

    @Test
    public void testParentContext() throws IOException
    {
        TokenBuffer buf = TokenBuffer.forGeneration();
        buf.writeStartObject();
        buf.writeName("b");
        buf.writeStartObject();
        buf.writeName("c");
        //This assertion succeeds as expected
        assertEquals("b", buf.streamWriteContext().getParent().currentName());
        buf.writeString("cval");
        buf.writeEndObject();
        buf.writeEndObject();
        buf.close();
    }

    @Test
    public void testSimpleArray() throws IOException
    {
        TokenBuffer buf = TokenBuffer.forGeneration();

        // First, empty array
        assertTrue(buf.streamWriteContext().inRoot());
        buf.writeStartArray();
        assertTrue(buf.streamWriteContext().inArray());
        buf.writeEndArray();
        assertTrue(buf.streamWriteContext().inRoot());

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        assertNull(p.currentToken());
        assertTrue(p.streamReadContext().inRoot());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertTrue(p.streamReadContext().inArray());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertTrue(p.streamReadContext().inRoot());
        assertNull(p.nextToken());
        p.close();
        buf.close();

        // Then one with simple contents
        buf = TokenBuffer.forGeneration();
        buf.writeStartArray();
        buf.writeBoolean(true);
        buf.writeNull();
        buf.writeEndArray();
        p = buf.asParser(ObjectReadContext.empty());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertTrue(p.getBooleanValue());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
        buf.close();

        // And finally, with array-in-array
        buf = TokenBuffer.forGeneration();
        buf.writeStartArray();
        buf.writeStartArray();
        buf.writeBinary(new byte[3]);
        buf.writeEndArray();
        buf.writeEndArray();
        p = buf.asParser(ObjectReadContext.empty());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        // TokenBuffer exposes it as embedded object...
        assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.nextToken());
        Object ob = p.getEmbeddedObject();
        assertNotNull(ob);
        assertInstanceOf(byte[].class, ob);
        assertEquals(3, ((byte[]) ob).length);
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
        buf.close();
    }

    @Test
    public void testSimpleObject() throws IOException
    {
        TokenBuffer buf = TokenBuffer.forGeneration();

        // First, empty JSON Object
        assertTrue(buf.streamWriteContext().inRoot());
        buf.writeStartObject();
        assertTrue(buf.streamWriteContext().inObject());
        buf.writeEndObject();
        assertTrue(buf.streamWriteContext().inRoot());

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        assertNull(p.currentToken());
        assertTrue(p.streamReadContext().inRoot());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertTrue(p.streamReadContext().inObject());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertTrue(p.streamReadContext().inRoot());
        assertNull(p.nextToken());
        p.close();
        buf.close();

        // Then one with simple contents
        buf = TokenBuffer.forGeneration();
        buf.writeStartObject();
        buf.writeNumberProperty("num", 1.25);
        buf.writeEndObject();

        p = buf.asParser(ObjectReadContext.empty());
        assertNull(p.currentToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertNull(p.currentName());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("num", p.currentName());
        // and override should also work:
//        p.overrideCurrentName("bah");
//        assertEquals("bah", p.currentName());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(1.25, p.getDoubleValue(), 0.0);
        // should still have access to (overridden) name
//        assertEquals("bah", p.currentName());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        // but not any more
        assertNull(p.currentName());
        assertNull(p.nextToken());
        p.close();
        buf.close();
    }

    /**
     * Verify handling of that "standard" test document (from JSON
     * specification)
     */
    @Test
    public void testWithJSONSampleDoc() throws Exception
    {
        // First, copy events from known good source (StringReader)
        TokenBuffer tb = TokenBuffer.forGeneration();
        try (JsonParser p = MAPPER.createParser(SAMPLE_DOC_JSON_SPEC)) {
            while (p.nextToken() != null) {
                tb.copyCurrentEvent(p);
            }
        
            // And then request verification; first structure only:
            verifyJsonSpecSampleDoc(tb.asParser(ObjectReadContext.empty()), false);
        
            // then content check too:
            verifyJsonSpecSampleDoc(tb.asParser(ObjectReadContext.empty()), true);
            tb.close();
        }
        // 19-Oct-2016, tatu: Just for fun, trigger `toString()` for code coverage
        String desc = tb.toString();
        assertNotNull(desc);
    }

    @Test
    public void testAppend() throws IOException
    {
        TokenBuffer buf1 = TokenBuffer.forGeneration();
        buf1.writeStartObject();
        buf1.writeName("a");
        buf1.writeBoolean(true);

        TokenBuffer buf2 = TokenBuffer.forGeneration();
        buf2.writeName("b");
        buf2.writeNumber(13);
        buf2.writeEndObject();

        buf1.append(buf2);

        // and verify that we got it all...
        JsonParser p = buf1.asParser(ObjectReadContext.empty());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("a", p.currentName());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("b", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(13, p.getIntValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
        buf1.close();
        buf2.close();
    }

    // Since 2.3 had big changes to UUID handling, let's verify we can
    // deal with
    @Test
    public void testWithUUID() throws IOException
    {
        for (String value : new String[] {
                "00000007-0000-0000-0000-000000000000",
                "76e6d183-5f68-4afa-b94a-922c1fdb83f8",
                "540a88d1-e2d8-4fb1-9396-9212280d0a7f",
                "2c9e441d-1cd0-472d-9bab-69838f877574",
                "591b2869-146e-41d7-8048-e8131f1fdec5",
                "82994ac2-7b23-49f2-8cc5-e24cf6ed77be",
        }) {
            TokenBuffer buf = TokenBuffer.forGeneration();
            UUID uuid = UUID.fromString(value);
            MAPPER.writeValue(buf, uuid);
            buf.close();

            // and bring it back
            UUID out = MAPPER.readValue(buf.asParser(ObjectReadContext.empty()), UUID.class);
            assertEquals(uuid.toString(), out.toString());

            // second part: As per [databind#362], should NOT use binary with TokenBuffer
            JsonParser p = buf.asParser(ObjectReadContext.empty());
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            String str = p.getString();
            assertEquals(value, str);
            p.close();
        }
    }

    /*
    /**********************************************************************
    /* TokenBuffer as source for ObjectMapper/ObjectReader tests
    /**********************************************************************
     */

    @Test
    public void readFromBufferViaObjectMapper() throws Exception {
        final Point TEST_OB = new Point(123, -456);

        assertEquals(TEST_OB, MAPPER.readValue(_tokenBufferFor(TEST_OB),
                Point.class));
        assertEquals(TEST_OB, MAPPER.readValue(_tokenBufferFor(TEST_OB),
                MAPPER.constructType(Point.class)));
        assertEquals(TEST_OB, MAPPER.readValue(_tokenBufferFor(TEST_OB),
                new TypeReference<Point>() {}));

        assertEquals(MAPPER.valueToTree(TEST_OB),
                MAPPER.readTree(_tokenBufferFor(TEST_OB)));

        // Try alternate that fills TokenBuffer using mapper itself
        assertEquals(TEST_OB, MAPPER.readValue(MAPPER.writeValueIntoBuffer(TEST_OB),
                Point.class));
    }

    @Test
    public void readFromBufferViaObjectReader() throws Exception {
        final Point TEST_OB = new Point(234, 5678);
        final ObjectReader R = MAPPER.readerFor(Point.class);

        assertEquals(TEST_OB, R.readValue(_tokenBufferFor(TEST_OB)));

        assertEquals(MAPPER.valueToTree(TEST_OB),
                R.readTree(_tokenBufferFor(TEST_OB)));

        // Try alternate that fills TokenBuffer using ObjectWriter
        assertEquals(TEST_OB,
                R.readValue(MAPPER.writer().writeValueIntoBuffer(TEST_OB)));
    }

    private TokenBuffer _tokenBufferFor(Object value) {
        final String json = MAPPER.writeValueAsString(value);
        TokenBuffer buf = TokenBuffer.forGeneration();
        try (JsonParser p = MAPPER.createParser(json)) {
            while (p.nextToken() != null) {
                buf.copyCurrentEvent(p);
            }
        }
        return buf;
    }
    
    /*
    /**********************************************************
    /* Tests for read/output contexts
    /**********************************************************
     */

    // for [databind#984]: ensure output context handling identical
    @Test
    public void testOutputContext() throws IOException
    {
        TokenBuffer buf = TokenBuffer.forGeneration();
        StringWriter w = new StringWriter();
        JsonGenerator gen = MAPPER.createGenerator(w);

        // test content: [{"a":1,"b":{"c":2}},{"a":2,"b":{"c":3}}]

        buf.writeStartArray();
        gen.writeStartArray();
        _verifyOutputContext(buf, gen);

        buf.writeStartObject();
        gen.writeStartObject();
        _verifyOutputContext(buf, gen);

        buf.writeName("a");
        gen.writeName("a");
        _verifyOutputContext(buf, gen);

        buf.writeNumber(1);
        gen.writeNumber(1);
        _verifyOutputContext(buf, gen);

        buf.writeName("b");
        gen.writeName("b");
        _verifyOutputContext(buf, gen);

        buf.writeStartObject();
        gen.writeStartObject();
        _verifyOutputContext(buf, gen);

        buf.writeName("c");
        gen.writeName("c");
        _verifyOutputContext(buf, gen);

        buf.writeNumber(2);
        gen.writeNumber(2);
        _verifyOutputContext(buf, gen);

        buf.writeEndObject();
        gen.writeEndObject();
        _verifyOutputContext(buf, gen);

        buf.writeEndObject();
        gen.writeEndObject();
        _verifyOutputContext(buf, gen);

        buf.writeEndArray();
        gen.writeEndArray();
        _verifyOutputContext(buf, gen);

        buf.close();
        gen.close();
    }

    private void _verifyOutputContext(JsonGenerator gen1, JsonGenerator gen2)
    {
        _verifyOutputContext(gen1.streamWriteContext(), gen2.streamWriteContext());
    }

    private void _verifyOutputContext(TokenStreamContext ctxt1, TokenStreamContext ctxt2)
    {
        if (ctxt1 == null) {
            if (ctxt2 == null) {
                return;
            }
            fail("Context 1 null, context 2 not null: "+ctxt2);
        } else if (ctxt2 == null) {
            fail("Context 2 null, context 1 not null: "+ctxt1);
        }
        if (!ctxt1.toString().equals(ctxt2.toString())) {
            fail("Different output context: token-buffer's = "+ctxt1+", json-generator's: "+ctxt2);
        }

        if (ctxt1.inObject()) {
            assertTrue(ctxt2.inObject());
            String str1 = ctxt1.currentName();
            String str2 = ctxt2.currentName();

            if ((str1 != str2) && !str1.equals(str2)) {
                fail("Expected name '"+str2+"' (JsonParser), TokenBuffer had '"+str1+"'");
            }
        } else if (ctxt1.inArray()) {
            assertTrue(ctxt2.inArray());
            assertEquals(ctxt1.getCurrentIndex(), ctxt2.getCurrentIndex());
        }
        _verifyOutputContext(ctxt1.getParent(), ctxt2.getParent());
    }

    // [databind#1253]
    @Test
    public void testParentSiblingContext() throws IOException
    {
        TokenBuffer buf = TokenBuffer.forGeneration();

        // {"a":{},"b":{"c":"cval"}}

        buf.writeStartObject();
        buf.writeName("a");
        buf.writeStartObject();
        buf.writeEndObject();

        buf.writeName("b");
        buf.writeStartObject();
        buf.writeName("c");
        //This assertion fails (because of 'a')
        assertEquals("b", buf.streamWriteContext().getParent().currentName());
        buf.writeString("cval");
        buf.writeEndObject();
        buf.writeEndObject();
        buf.close();
    }

    @Test
    public void testBasicSerialize() throws IOException
    {
        TokenBuffer buf;

        // let's see how empty works...
        buf = TokenBuffer.forGeneration();
        assertEquals("", MAPPER.writeValueAsString(buf));
        buf.close();

        buf = TokenBuffer.forGeneration();
        buf.writeStartArray();
        buf.writeBoolean(true);
        buf.writeBoolean(false);
        long l = 1L + Integer.MAX_VALUE;
        buf.writeNumber(l);
        buf.writeNumber((short) 4);
        buf.writeNumber(0.5);
        buf.writeEndArray();
        assertEquals(a2q("[true,false,"+l+",4,0.5]"), MAPPER.writeValueAsString(buf));
        buf.close();

        buf = TokenBuffer.forGeneration();
        buf.writeStartObject();
        buf.writeName(new SerializedString("foo"));
        buf.writeNull();
        buf.writeName("bar");
        buf.writeNumber(BigInteger.valueOf(123));
        buf.writeName("dec");
        buf.writeNumber(BigDecimal.valueOf(5).movePointLeft(2));
        assertEquals(a2q("{'foo':null,'bar':123,'dec':0.05}"), MAPPER.writeValueAsString(buf));
        buf.close();
    }

    /*
    /**********************************************************
    /* Tests to verify interaction of TokenBuffer and JsonParserSequence
    /**********************************************************
     */

    @Test
    public void testWithJsonParserSequenceSimple() throws IOException
    {
        // Let's join a TokenBuffer with JsonParser first
        TokenBuffer buf = TokenBuffer.forGeneration();
        buf.writeStartArray();
        buf.writeString("test");
        JsonParser p = MAPPER.createParser("[ true, null ]");

        JsonParserSequence seq = JsonParserSequence.createFlattened(false,
                buf.asParser(ObjectReadContext.empty()), p);
        assertEquals(2, seq.containedParsersCount());

        assertFalse(p.isClosed());

        assertFalse(seq.hasCurrentToken());
        assertNull(seq.currentToken());
        assertNull(seq.currentName());

        assertToken(JsonToken.START_ARRAY, seq.nextToken());
        assertToken(JsonToken.VALUE_STRING, seq.nextToken());
        assertEquals("test", seq.getString());
        // end of first parser input, should switch over:

        assertToken(JsonToken.START_ARRAY, seq.nextToken());
        assertToken(JsonToken.VALUE_TRUE, seq.nextToken());
        assertToken(JsonToken.VALUE_NULL, seq.nextToken());
        assertToken(JsonToken.END_ARRAY, seq.nextToken());

        /* 17-Jan-2009, tatus: At this point, we may or may not get an
         *   exception, depending on how underlying parsers work.
         *   Ideally this should be fixed, probably by asking underlying
         *   parsers to disable checking for balanced start/end markers.
         */

        // for this particular case, we won't get an exception tho...
        assertNull(seq.nextToken());
        // not an error to call again...
        assertNull(seq.nextToken());

        // also: original parsers should be closed
        assertTrue(p.isClosed());
        p.close();
        buf.close();
        seq.close();
    }

    /**
     * Test to verify that TokenBuffer and JsonParserSequence work together
     * as expected.
     */
    @Test
    public void testWithMultipleJsonParserSequences() throws IOException
    {
        TokenBuffer buf1 = TokenBuffer.forGeneration();
        buf1.writeStartArray();
        TokenBuffer buf2 = TokenBuffer.forGeneration();
        buf2.writeString("a");
        TokenBuffer buf3 = TokenBuffer.forGeneration();
        buf3.writeNumber(13);
        TokenBuffer buf4 = TokenBuffer.forGeneration();
        buf4.writeEndArray();

        JsonParserSequence seq1 = JsonParserSequence.createFlattened(false,
                buf1.asParser(ObjectReadContext.empty()), buf2.asParser(ObjectReadContext.empty()));
        assertEquals(2, seq1.containedParsersCount());
        JsonParserSequence seq2 = JsonParserSequence.createFlattened(false,
                buf3.asParser(ObjectReadContext.empty()), buf4.asParser(ObjectReadContext.empty()));
        assertEquals(2, seq2.containedParsersCount());
        JsonParserSequence combo = JsonParserSequence.createFlattened(false, seq1, seq2);
        // should flatten it to have 4 underlying parsers
        assertEquals(4, combo.containedParsersCount());

        assertToken(JsonToken.START_ARRAY, combo.nextToken());
        assertToken(JsonToken.VALUE_STRING, combo.nextToken());
        assertEquals("a", combo.getString());
        assertToken(JsonToken.VALUE_NUMBER_INT, combo.nextToken());
        assertEquals(13, combo.getIntValue());
        assertToken(JsonToken.END_ARRAY, combo.nextToken());
        assertNull(combo.nextToken());
        buf1.close();
        buf2.close();
        buf3.close();
        buf4.close();
    }

    // [databind#743]
    @Test
    public void testRawValues() throws Exception
    {
        final String RAW = "{\"a\":1}";
        TokenBuffer buf = TokenBuffer.forGeneration();
        buf.writeRawValue(RAW);
        // first: raw value won't be transformed in any way:
        JsonParser p = buf.asParser(ObjectReadContext.empty());
        assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.nextToken());
        assertEquals(RawValue.class, p.getEmbeddedObject().getClass());
        assertNull(p.nextToken());
        p.close();
        buf.close();

        // then verify it would be serialized just fine
        assertEquals(RAW, MAPPER.writeValueAsString(buf));
    }

    // [databind#1730]
    @Test
    public void testEmbeddedObjectCoerceCheck() throws Exception
    {
        TokenBuffer buf = TokenBuffer.forGeneration();
        Object inputPojo = new Sub1730();
        buf.writeEmbeddedObject(inputPojo);
        assertEquals(JsonToken.VALUE_EMBEDDED_OBJECT, buf.firstToken());

        // first: raw value won't be transformed in any way:
        JsonParser p = buf.asParser(ObjectReadContext.empty());
        Base1730 out = MAPPER.readValue(p, Base1730.class);

        assertSame(inputPojo, out);
        p.close();
        buf.close();
    }

    @Test
    public void testIsEmpty() throws Exception
    {
        // Let's check that segment boundary won't ruin it
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            assertTrue(buf.isEmpty());

            for (int i = 0; i < 100; ++i) {
                buf.writeNumber(i);
                assertFalse(buf.isEmpty());
            }

            assertEquals(JsonToken.VALUE_NUMBER_INT, buf.firstToken());
        }
    }

    /*
    /**********************************************************
    /* Misc other tests
    /**********************************************************
     */

    // [databind#3816]
    @Test
    public void testWriteStringFromStream() throws Exception
    {
        Map<String, String> map = MAPPER.convertValue(new Foo3816(),
                new TypeReference<Map<String, String>>() {});
        assertNotNull(map);
    }

    /*
    /**********************************************************
    /* Tests for serialize() branch coverage
    /**********************************************************
     */

    // Cover Float branch in VALUE_NUMBER_FLOAT in serialize()
    @Test
    public void testSerializeWithFloatNumber() throws Exception
    {
        TokenBuffer buf = TokenBuffer.forGeneration();
        buf.writeStartArray();
        buf.writeNumber(3.14f);
        buf.writeEndArray();
        String json = MAPPER.writeValueAsString(buf);
        // Float serializes as float value
        assertEquals("[3.14]", json);
        buf.close();
    }

    // Cover String branch in VALUE_NUMBER_FLOAT in serialize()
    @Test
    public void testSerializeWithStringNumber() throws Exception
    {
        TokenBuffer buf = TokenBuffer.forGeneration();
        buf.writeStartArray();
        buf.writeNumber("1.23e10");
        buf.writeEndArray();
        String json = MAPPER.writeValueAsString(buf);
        assertEquals("[1.23e10]", json);
        buf.close();
    }

    // Cover VALUE_EMBEDDED_OBJECT branches: RawValue, byte[] (embedded), JacksonSerializable
    @Test
    public void testSerializeEmbeddedObjectTypes() throws Exception
    {
        // RawValue path
        TokenBuffer buf = TokenBuffer.forGeneration();
        buf.writeRawValue("{\"x\":1}");
        String json = MAPPER.writeValueAsString(buf);
        assertEquals("{\"x\":1}", json);
        buf.close();

        // byte[] embedded object path
        buf = TokenBuffer.forGeneration();
        buf.writeStartArray();
        byte[] data = new byte[] { 1, 2, 3 };
        buf.writeEmbeddedObject(data);
        buf.writeEndArray();
        // Embedded byte arrays are serialized as base64
        json = MAPPER.writeValueAsString(buf);
        assertNotNull(json);
        buf.close();
    }

    // Cover native IDs branches in serialize()
    @Test
    public void testSerializeWithNativeIds() throws Exception
    {
        // Use TokenBuffer with native IDs enabled
        TokenBuffer buf = new TokenBuffer(null, true);
        buf.writeStartObject();
        // Write typeId and objectId before a value
        buf.writeTypeId("myType");
        buf.writeObjectId("myObjId");
        buf.writeName("field");
        buf.writeString("value");
        buf.writeEndObject();

        // Serialize to another TokenBuffer (which supports native IDs too)
        TokenBuffer target = new TokenBuffer(null, true);
        buf.serialize(target);
        target.close();
        buf.close();
    }

    /*
    /**********************************************************
    /* Tests for toString() branch coverage
    /**********************************************************
     */

    // Cover toString() truncation branch (>100 tokens)
    @Test
    public void testToStringWithManyTokens() throws Exception
    {
        TokenBuffer buf = TokenBuffer.forGeneration();
        buf.writeStartArray();
        // Write >100 tokens to trigger truncation
        for (int i = 0; i < 110; i++) {
            buf.writeNumber(i);
        }
        buf.writeEndArray();
        String desc = buf.toString();
        assertNotNull(desc);
        assertTrue(desc.contains("... (truncated"));
        buf.close();
    }

    // Cover _appendNativeIds() in toString()
    @Test
    public void testToStringWithNativeIds() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(null, true);
        buf.writeTypeId("typeA");
        buf.writeObjectId("objB");
        buf.writeStartObject();
        buf.writeEndObject();
        String desc = buf.toString();
        assertNotNull(desc);
        buf.close();
    }

    /*
    /**********************************************************
    /* Tests for writeString(null), writeRawValue, writePOJO/writeTree nulls
    /**********************************************************
     */

    // Cover writeString(String null) -> writeNull() branch
    @Test
    public void testWriteNullStrings() throws Exception
    {
        // String null
        try (TokenBuffer buf = TokenBuffer.forGeneration()) {
            buf.writeString((String) null);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NULL, p.nextToken());
            }
        }

        // SerializableString null
        try (TokenBuffer buf = TokenBuffer.forGeneration()) {
            buf.writeString((SerializableString) null);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NULL, p.nextToken());
            }
        }
    }

    // Cover writeRawValue(String, offset, len) substring branch
    @Test
    public void testWriteRawValueWithOffsetAndLen() throws Exception
    {
        // String variant with offset > 0
        try (TokenBuffer buf = TokenBuffer.forGeneration()) {
            buf.writeRawValue("xxxABCyyy", 3, 3);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.nextToken());
                Object embedded = p.getEmbeddedObject();
                assertInstanceOf(RawValue.class, embedded);
            }
        }

        // char[] variant
        try (TokenBuffer buf = TokenBuffer.forGeneration()) {
            char[] chars = "Hello World!".toCharArray();
            buf.writeRawValue(chars, 6, 5);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.nextToken());
                // char[] variant stores as a String, not RawValue
                Object embedded = p.getEmbeddedObject();
                assertInstanceOf(String.class, embedded);
                assertEquals("World", embedded);
            }
        }
    }

    // Cover writePOJO(null) -> writeNull() branch
    @Test
    public void testWritePOJONull() throws Exception
    {
        try (TokenBuffer buf = TokenBuffer.forGeneration()) {
            buf.writePOJO(null);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NULL, p.nextToken());
            }
        }
    }

    // Cover writePOJO without objectWriteContext -> VALUE_EMBEDDED_OBJECT
    @Test
    public void testWritePOJOWithoutWriteContext() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            Point pojo = new Point(1, 2);
            buf.writePOJO(pojo);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.nextToken());
                assertSame(pojo, p.getEmbeddedObject());
            }
        }
    }

    // Cover writeTree(null) -> writeNull() branch
    @Test
    public void testWriteTreeNull() throws Exception
    {
        try (TokenBuffer buf = TokenBuffer.forGeneration()) {
            buf.writeTree(null);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NULL, p.nextToken());
            }
        }
    }

    // Cover writeTree without objectWriteContext -> VALUE_EMBEDDED_OBJECT
    @Test
    public void testWriteTreeWithoutWriteContext() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("x", 42);
            buf.writeTree(node);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.nextToken());
                assertSame(node, p.getEmbeddedObject());
            }
        }
    }

    /*
    /**********************************************************
    /* Tests for Parser: _convertNumberToInt / _convertNumberToLong
    /**********************************************************
     */

    // Double -> int conversion (in range)
    @Test
    public void testConvertDoubleToInt() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(42.0);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(42, p.getIntValue());
            }
        }
    }

    // Double -> int overflow
    @Test
    public void testConvertDoubleToIntOverflow() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(1e20);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertThrows(InputCoercionException.class, p::getIntValue);
            }
        }
    }

    // BigDecimal -> int conversion (in range)
    @Test
    public void testConvertBigDecimalToInt() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(BigDecimal.valueOf(99));
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(99, p.getIntValue());
            }
        }
    }

    // BigDecimal -> int overflow
    @Test
    public void testConvertBigDecimalToIntOverflow() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(new BigDecimal("99999999999"));
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertThrows(InputCoercionException.class, p::getIntValue);
            }
        }
    }

    // BigInteger in-range -> int
    @Test
    public void testConvertBigIntegerToInt() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(BigInteger.valueOf(77));
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(77, p.getIntValue());
            }
        }
    }

    // Double -> long conversion (in range)
    @Test
    public void testConvertDoubleToLong() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(123456.0);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(123456L, p.getLongValue());
            }
        }
    }

    // Double -> long overflow
    @Test
    public void testConvertDoubleToLongOverflow() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(1e30);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertThrows(InputCoercionException.class, p::getLongValue);
            }
        }
    }

    // BigDecimal -> long conversion (in range)
    @Test
    public void testConvertBigDecimalToLong() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(BigDecimal.valueOf(1234567890L));
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(1234567890L, p.getLongValue());
            }
        }
    }

    // BigDecimal -> long overflow
    @Test
    public void testConvertBigDecimalToLongOverflow() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(new BigDecimal("99999999999999999999"));
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertThrows(InputCoercionException.class, p::getLongValue);
            }
        }
    }

    // Float -> int conversion (in range)
    @Test
    public void testConvertFloatToInt() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(7.0f);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(7, p.getIntValue());
            }
        }
    }

    // Float -> long conversion (in range)
    @Test
    public void testConvertFloatToLong() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(42.0f);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(42L, p.getLongValue());
            }
        }
    }

    /*
    /**********************************************************
    /* Tests for Parser: getDecimalValue, getNumberType, getBinaryValue
    /**********************************************************
     */

    // Cover getDecimalValue from Integer, Long, BigInteger, Double/Float
    @Test
    public void testGetDecimalValueFromVariousTypes() throws Exception
    {
        // From Integer
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(42);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(new BigDecimal(42), p.getDecimalValue());
            }
        }

        // From Long
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(123456789012L);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(BigDecimal.valueOf(123456789012L), p.getDecimalValue());
            }
        }

        // From BigInteger
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            BigInteger bi = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
            buf.writeNumber(bi);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(new BigDecimal(bi), p.getDecimalValue());
            }
        }

        // From Double (falls through to doubleValue() path)
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(3.14);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(BigDecimal.valueOf(3.14), p.getDecimalValue());
            }
        }

        // From Float (falls through to doubleValue() path)
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(2.5f);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                BigDecimal result = p.getDecimalValue();
                assertNotNull(result);
            }
        }
    }

    // Cover getNumberType for Short and Float, and null _currToken case
    @Test
    public void testGetNumberTypeForAllTypes() throws Exception
    {
        // Short -> returns INT
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber((short) 5);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(NumberType.INT, p.getNumberType());
            }
        }

        // Float -> returns FLOAT
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(1.5f);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(NumberType.FLOAT, p.getNumberType());
            }
        }

        // Before calling nextToken, _currToken is null -> returns null
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber(1);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                // Don't call nextToken — _currToken is null
                assertNull(p.getNumberType());
            }
        }

        // [databind#3524] String-based number as VALUE_NUMBER_FLOAT -> DOUBLE (not BIG_DECIMAL)
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber("123.45");
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(NumberType.DOUBLE, p.getNumberType());
            }
        }

        // String-based number as VALUE_NUMBER_INT -> BIG_INTEGER
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeNumber("12345", true);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
            }
        }
    }

    // Cover getBinaryValue from VALUE_STRING (base64 decode path)
    @Test
    public void testGetBinaryValueFromString() throws Exception
    {
        byte[] original = new byte[] { 10, 20, 30, 40, 50 };
        String base64 = Base64.getEncoder().encodeToString(original);
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeString(base64);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                byte[] result = p.getBinaryValue();
                assertNotNull(result);
                assertArrayEquals(original, result);
            }
        }
    }

    // Cover getBinaryValue from VALUE_EMBEDDED_OBJECT with byte[]
    @Test
    public void testGetBinaryValueFromEmbeddedByteArray() throws Exception
    {
        byte[] data = new byte[] { 1, 2, 3 };
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeEmbeddedObject(data);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.nextToken());
                byte[] result = p.getBinaryValue();
                assertArrayEquals(data, result);
            }
        }
    }

    // Cover getBinaryValue called twice (exercises _byteBuilder reuse/reset path)
    @Test
    public void testGetBinaryValueReuse() throws Exception
    {
        byte[] original1 = new byte[] { 1, 2, 3 };
        byte[] original2 = new byte[] { 4, 5, 6, 7 };
        String base64_1 = Base64.getEncoder().encodeToString(original1);
        String base64_2 = Base64.getEncoder().encodeToString(original2);
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeString(base64_1);
            buf.writeString(base64_2);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertArrayEquals(original1, p.getBinaryValue());
                // Second call exercises the _byteBuilder.reset() path
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertArrayEquals(original2, p.getBinaryValue());
            }
        }
    }

    /*
    /**********************************************************
    /* Tests for Segment overflow / multi-segment coverage
    /**********************************************************
     */

    // Writing >16 tokens triggers segment overflow (new segment allocation)
    @Test
    public void testSegmentOverflow() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            buf.writeStartArray();
            // Write exactly 20 values (plus START_ARRAY = 21 tokens, exceeding 16-token segment)
            for (int i = 0; i < 20; i++) {
                buf.writeNumber(i);
            }
            buf.writeEndArray();
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                for (int i = 0; i < 20; i++) {
                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    assertEquals(i, p.getIntValue());
                }
                assertToken(JsonToken.END_ARRAY, p.nextToken());
                assertNull(p.nextToken());
            }
        }
    }

    // Segment overflow with native IDs exercises Segment.assignNativeIds across segments
    @Test
    public void testSegmentOverflowWithNativeIds() throws Exception
    {
        try (TokenBuffer buf = new TokenBuffer(null, true)) {
            buf.writeStartArray();
            for (int i = 0; i < 20; i++) {
                buf.writeTypeId("type" + i);
                buf.writeNumber(i);
            }
            buf.writeEndArray();
            // Verify tokens can be read back
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                for (int i = 0; i < 20; i++) {
                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    assertEquals(i, p.getIntValue());
                }
                assertToken(JsonToken.END_ARRAY, p.nextToken());
                assertNull(p.nextToken());
            }
        }
    }
}
