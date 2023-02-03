package com.fasterxml.jackson.databind.util;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.exc.InputCoercionException;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.util.JsonParserSequence;

import com.fasterxml.jackson.databind.*;

@SuppressWarnings("resource")
public class TokenBufferTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = objectMapper();

    static class Base1730 { }

    static class Sub1730 extends Base1730 { }

    /*
    /**********************************************************
    /* Basic TokenBuffer tests
    /**********************************************************
     */

    public void testBasicConfig() throws IOException
    {
        TokenBuffer buf;

        buf = new TokenBuffer(MAPPER, false);
        assertEquals(MAPPER.version(), buf.version());
        assertSame(MAPPER, buf.getCodec());
        assertNotNull(buf.getOutputContext());
        assertFalse(buf.isClosed());
        assertTrue(buf.isEmpty());

        buf.setCodec(null);
        assertNull(buf.getCodec());

        assertFalse(buf.isEnabled(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN));
        buf.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        assertTrue(buf.isEnabled(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN));
        buf.disable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        assertFalse(buf.isEnabled(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN));

        buf.close();
        assertTrue(buf.isClosed());
    }

    // for [databind#3528]
    public void testParserFeatureDefaults() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(MAPPER, false);
        try (JsonParser p = buf.asParser()) {
            for (JsonParser.Feature feat : JsonParser.Feature.values()) {
                assertEquals("Feature "+feat, feat.enabledByDefault(), p.isEnabled(feat));
            }
        }
    }

    /**
     * Test writing of individual simple values
     */
    public void testSimpleWrites() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null, false); // no ObjectCodec
        assertTrue(buf.isEmpty());

        // First, with empty buffer
        JsonParser p = buf.asParser();
        assertNull(p.currentToken());
        assertNull(p.nextToken());
        p.close();

        // Then with simple text
        buf.writeString("abc");
        assertFalse(buf.isEmpty());

        p = buf.asParser();
        assertNull(p.currentToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("abc", p.getText());
        assertNull(p.nextToken());
        p.close();

        // Then, let's append at root level
        buf.writeNumber(13);
        p = buf.asParser();
        assertNull(p.currentToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(13, p.getIntValue());
        assertNull(p.nextToken());
        p.close();
        buf.close();
    }

    // For 2.9, explicit "isNaN" check
    public void testSimpleNumberWrites() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null, false);

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

        JsonParser p = buf.asParser();
        assertNull(p.currentToken());

        for (double v : values1) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            double actual = p.getDoubleValue();
            boolean expNan = Double.isNaN(v) || Double.isInfinite(v);
            assertEquals(expNan, p.isNaN());
            assertEquals(0, Double.compare(v, actual));
        }
        for (float v : values2) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            float actual = p.getFloatValue();
            boolean expNan = Float.isNaN(v) || Float.isInfinite(v);
            assertEquals(expNan, p.isNaN());
            assertEquals(0, Float.compare(v, actual));
        }
        p.close();
        buf.close();
    }

    // [databind#1729]
    public void testNumberOverflowInt() throws IOException
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            long big = 1L + Integer.MAX_VALUE;
            buf.writeNumber(big);
            try (JsonParser p = buf.asParser()) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(NumberType.LONG, p.getNumberType());
                try {
                    p.getIntValue();
                    fail("Expected failure for `int` overflow");
                } catch (InputCoercionException e) {
                    verifyException(e, "Numeric value ("+big+") out of range of int");
                }
            }
        }
        // and ditto for coercion.
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            long big = 1L + Integer.MAX_VALUE;
            buf.writeNumber(String.valueOf(big));
            try (JsonParser p = buf.asParser()) {
                // NOTE: oddity of buffering, no inspection of "real" type if given String...
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                try {
                    p.getIntValue();
                    fail("Expected failure for `int` overflow");
                } catch (InputCoercionException e) {
                    verifyException(e, "Numeric value ("+big+") out of range of int");
                }
            }
        }
    }

    public void testNumberOverflowLong() throws IOException
    {
        try (TokenBuffer buf = new TokenBuffer(null, false)) {
            BigInteger big = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
            buf.writeNumber(big);
            try (JsonParser p = buf.asParser()) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
                try {
                    p.getLongValue();
                    fail("Expected failure for `long` overflow");
                } catch (InputCoercionException e) {
                    verifyException(e, "Numeric value ("+big+") out of range of long");
                }
            }
        }
    }

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

    public void testParentContext() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null, false); // no ObjectCodec
        buf.writeStartObject();
        buf.writeFieldName("b");
        buf.writeStartObject();
        buf.writeFieldName("c");
        //This assertion succeeds as expected
        assertEquals("b", buf.getOutputContext().getParent().getCurrentName());
        buf.writeString("cval");
        buf.writeEndObject();
        buf.writeEndObject();
        buf.close();
    }

    public void testSimpleArray() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null, false); // no ObjectCodec

        // First, empty array
        assertTrue(buf.getOutputContext().inRoot());
        buf.writeStartArray();
        assertTrue(buf.getOutputContext().inArray());
        buf.writeEndArray();
        assertTrue(buf.getOutputContext().inRoot());

        JsonParser p = buf.asParser();
        assertNull(p.currentToken());
        assertTrue(p.getParsingContext().inRoot());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertTrue(p.getParsingContext().inArray());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertTrue(p.getParsingContext().inRoot());
        assertNull(p.nextToken());
        p.close();
        buf.close();

        // Then one with simple contents
        buf = new TokenBuffer(null, false);
        buf.writeStartArray();
        buf.writeBoolean(true);
        buf.writeNull();
        buf.writeEndArray();
        p = buf.asParser();
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertTrue(p.getBooleanValue());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
        buf.close();

        // And finally, with array-in-array
        buf = new TokenBuffer(null, false);
        buf.writeStartArray();
        buf.writeStartArray();
        buf.writeBinary(new byte[3]);
        buf.writeEndArray();
        buf.writeEndArray();
        p = buf.asParser();
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        // TokenBuffer exposes it as embedded object...
        assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.nextToken());
        Object ob = p.getEmbeddedObject();
        assertNotNull(ob);
        assertTrue(ob instanceof byte[]);
        assertEquals(3, ((byte[]) ob).length);
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
        buf.close();
    }

    public void testSimpleObject() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null, false);

        // First, empty JSON Object
        assertTrue(buf.getOutputContext().inRoot());
        buf.writeStartObject();
        assertTrue(buf.getOutputContext().inObject());
        buf.writeEndObject();
        assertTrue(buf.getOutputContext().inRoot());

        JsonParser p = buf.asParser();
        assertNull(p.currentToken());
        assertTrue(p.getParsingContext().inRoot());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertTrue(p.getParsingContext().inObject());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertTrue(p.getParsingContext().inRoot());
        assertNull(p.nextToken());
        p.close();
        buf.close();

        // Then one with simple contents
        buf = new TokenBuffer(null, false);
        buf.writeStartObject();
        buf.writeNumberField("num", 1.25);
        buf.writeEndObject();

        p = buf.asParser();
        assertNull(p.currentToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertNull(p.currentName());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.currentName());
        // and override should also work:
        p.overrideCurrentName("bah");
        assertEquals("bah", p.currentName());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(1.25, p.getDoubleValue());
        // should still have access to (overridden) name
        assertEquals("bah", p.currentName());
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
    public void testWithJSONSampleDoc() throws Exception
    {
        // First, copy events from known good source (StringReader)
        JsonParser p = createParserUsingReader(SAMPLE_DOC_JSON_SPEC);
        TokenBuffer tb = new TokenBuffer(null, false);
        while (p.nextToken() != null) {
            tb.copyCurrentEvent(p);
        }

        // And then request verification; first structure only:
        verifyJsonSpecSampleDoc(tb.asParser(), false);

        // then content check too:
        verifyJsonSpecSampleDoc(tb.asParser(), true);
        tb.close();
        p.close();


        // 19-Oct-2016, tatu: Just for fun, trigger `toString()` for code coverage
        String desc = tb.toString();
        assertNotNull(desc);
    }

    public void testAppend() throws IOException
    {
        TokenBuffer buf1 = new TokenBuffer(null, false);
        buf1.writeStartObject();
        buf1.writeFieldName("a");
        buf1.writeBoolean(true);

        TokenBuffer buf2 = new TokenBuffer(null, false);
        buf2.writeFieldName("b");
        buf2.writeNumber(13);
        buf2.writeEndObject();

        buf1.append(buf2);

        // and verify that we got it all...
        JsonParser p = buf1.asParser();
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.currentName());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
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
            TokenBuffer buf = new TokenBuffer(MAPPER, false); // no ObjectCodec
            UUID uuid = UUID.fromString(value);
            MAPPER.writeValue(buf, uuid);
            buf.close();

            // and bring it back
            UUID out = MAPPER.readValue(buf.asParser(), UUID.class);
            assertEquals(uuid.toString(), out.toString());

            // second part: As per [databind#362], should NOT use binary with TokenBuffer
            JsonParser p = buf.asParser();
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            String str = p.getText();
            assertEquals(value, str);
            p.close();
        }
    }

    /*
    /**********************************************************
    /* Tests for read/output contexts
    /**********************************************************
     */

    // for [databind#984]: ensure output context handling identical
    public void testOutputContext() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null, false); // no ObjectCodec
        StringWriter w = new StringWriter();
        JsonGenerator gen = MAPPER.createGenerator(w);

        // test content: [{"a":1,"b":{"c":2}},{"a":2,"b":{"c":3}}]

        buf.writeStartArray();
        gen.writeStartArray();
        _verifyOutputContext(buf, gen);

        buf.writeStartObject();
        gen.writeStartObject();
        _verifyOutputContext(buf, gen);

        buf.writeFieldName("a");
        gen.writeFieldName("a");
        _verifyOutputContext(buf, gen);

        buf.writeNumber(1);
        gen.writeNumber(1);
        _verifyOutputContext(buf, gen);

        buf.writeFieldName("b");
        gen.writeFieldName("b");
        _verifyOutputContext(buf, gen);

        buf.writeStartObject();
        gen.writeStartObject();
        _verifyOutputContext(buf, gen);

        buf.writeFieldName("c");
        gen.writeFieldName("c");
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
        _verifyOutputContext(gen1.getOutputContext(), gen2.getOutputContext());
    }

    private void _verifyOutputContext(JsonStreamContext ctxt1, JsonStreamContext ctxt2)
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
            String str1 = ctxt1.getCurrentName();
            String str2 = ctxt2.getCurrentName();

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
    public void testParentSiblingContext() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null, false); // no ObjectCodec

        // {"a":{},"b":{"c":"cval"}}

        buf.writeStartObject();
        buf.writeFieldName("a");
        buf.writeStartObject();
        buf.writeEndObject();

        buf.writeFieldName("b");
        buf.writeStartObject();
        buf.writeFieldName("c");
        //This assertion fails (because of 'a')
        assertEquals("b", buf.getOutputContext().getParent().getCurrentName());
        buf.writeString("cval");
        buf.writeEndObject();
        buf.writeEndObject();
        buf.close();
    }

    public void testBasicSerialize() throws IOException
    {
        TokenBuffer buf;

        // let's see how empty works...
        buf = new TokenBuffer(MAPPER, false);
        assertEquals("", MAPPER.writeValueAsString(buf));
        buf.close();

        buf = new TokenBuffer(MAPPER, false);
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

        buf = new TokenBuffer(MAPPER, false);
        buf.writeStartObject();
        buf.writeFieldName(new SerializedString("foo"));
        buf.writeNull();
        buf.writeFieldName("bar");
        buf.writeNumber(BigInteger.valueOf(123));
        buf.writeFieldName("dec");
        buf.writeNumber(BigDecimal.valueOf(5).movePointLeft(2));
        assertEquals(a2q("{'foo':null,'bar':123,'dec':0.05}"), MAPPER.writeValueAsString(buf));
        buf.close();
    }

    /*
    /**********************************************************
    /* Tests to verify interaction of TokenBuffer and JsonParserSequence
    /**********************************************************
     */

    public void testWithJsonParserSequenceSimple() throws IOException
    {
        // Let's join a TokenBuffer with JsonParser first
        TokenBuffer buf = new TokenBuffer(null, false);
        buf.writeStartArray();
        buf.writeString("test");
        JsonParser p = createParserUsingReader("[ true, null ]");

        JsonParserSequence seq = JsonParserSequence.createFlattened(false, buf.asParser(), p);
        assertEquals(2, seq.containedParsersCount());

        assertFalse(p.isClosed());

        assertFalse(seq.hasCurrentToken());
        assertNull(seq.currentToken());
        assertNull(seq.currentName());

        assertToken(JsonToken.START_ARRAY, seq.nextToken());
        assertToken(JsonToken.VALUE_STRING, seq.nextToken());
        assertEquals("test", seq.getText());
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
    public void testWithMultipleJsonParserSequences() throws IOException
    {
        TokenBuffer buf1 = new TokenBuffer(null, false);
        buf1.writeStartArray();
        TokenBuffer buf2 = new TokenBuffer(null, false);
        buf2.writeString("a");
        TokenBuffer buf3 = new TokenBuffer(null, false);
        buf3.writeNumber(13);
        TokenBuffer buf4 = new TokenBuffer(null, false);
        buf4.writeEndArray();

        JsonParserSequence seq1 = JsonParserSequence.createFlattened(false, buf1.asParser(), buf2.asParser());
        assertEquals(2, seq1.containedParsersCount());
        JsonParserSequence seq2 = JsonParserSequence.createFlattened(false, buf3.asParser(), buf4.asParser());
        assertEquals(2, seq2.containedParsersCount());
        JsonParserSequence combo = JsonParserSequence.createFlattened(false, seq1, seq2);
        // should flatten it to have 4 underlying parsers
        assertEquals(4, combo.containedParsersCount());

        assertToken(JsonToken.START_ARRAY, combo.nextToken());
        assertToken(JsonToken.VALUE_STRING, combo.nextToken());
        assertEquals("a", combo.getText());
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
    public void testRawValues() throws Exception
    {
        final String RAW = "{\"a\":1}";
        TokenBuffer buf = new TokenBuffer(null, false);
        buf.writeRawValue(RAW);
        // first: raw value won't be transformed in any way:
        JsonParser p = buf.asParser();
        assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.nextToken());
        assertEquals(RawValue.class, p.getEmbeddedObject().getClass());
        assertNull(p.nextToken());
        p.close();
        buf.close();

        // then verify it would be serialized just fine
        assertEquals(RAW, MAPPER.writeValueAsString(buf));
    }

    // [databind#1730]
    public void testEmbeddedObjectCoerceCheck() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(null, false);
        Object inputPojo = new Sub1730();
        buf.writeEmbeddedObject(inputPojo);
        assertEquals(JsonToken.VALUE_EMBEDDED_OBJECT, buf.firstToken());

        // first: raw value won't be transformed in any way:
        JsonParser p = buf.asParser();
        Base1730 out = MAPPER.readValue(p, Base1730.class);

        assertSame(inputPojo, out);
        p.close();
        buf.close();
    }

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
}
