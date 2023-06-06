package com.fasterxml.jackson.databind.ser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test; // TODO JUnit 4 or 5 for tests?

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

class CanonicalJsonTest {

    private static final double NEGATIVE_ZERO = -0.;
    private static final JsonAssert JSON_ASSERT = new JsonAssert();
    private static final JsonTestResource CANONICAL_1 = new JsonTestResource("/data/canonical-1.json");

    // TODO There are several ways to make sure we really have a negative sign.
    // Double.toString(NEGATIVE_ZERO) seems to be the most simple.
    @Test
    void testSignOfNegativeZero() {
        assertEquals("-0.0", Double.toString(Math.signum(NEGATIVE_ZERO)));
    }

    @Test
    void testSignOfNegativeZero2() {
        long bits = Double.doubleToRawLongBits(NEGATIVE_ZERO);
        assertTrue(bits < 0);
    }

    @Test
    void testSignOfNegativeZero3() {
        long sign = 1L << (Double.SIZE - 1); // Highest bit represents the sign
        long bits = Double.doubleToRawLongBits(NEGATIVE_ZERO);
        assertEquals(sign, bits & sign);
    }

    @Test
    void testSignOfNegativeZero4() {
        assertEquals("-0.0", Double.toString(NEGATIVE_ZERO));
    }

    @Test
    void testNegativeZeroIsEqualToZero() {
        assertEquals(0.0, NEGATIVE_ZERO, 1e-9);
    }

    @Test
    void testCanonicalBigDecimalSerializationTrailingZeros() throws Exception {
        assertSerialized("1", new BigDecimal("1.0000"), newCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalNegativeZeroBigDecimal() throws Exception {
        assertSerialized("0", new BigDecimal("-0"), newCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalNegativeZeroBigDecimal2() throws Exception {
        assertSerialized("0", new BigDecimal(NEGATIVE_ZERO), newCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalNegativeZeroDouble() throws Exception {
        assertSerialized("0", NEGATIVE_ZERO, newCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalDecimalHandling() throws Exception {
        assertSerialized("1.01E1", new BigDecimal("10.1000"), newCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalHugeDecimalHandling() throws Exception {
        BigDecimal actual = new BigDecimal("123456789123456789123456789123456789.123456789123456789123456789123456789123456789000");
        assertSerialized("1.23456789123456789123456789123456789123456789123456789123456789123456789123456789E35", actual, newCanonicalMapperBuilder());
    }

    @Test
    void testPrettyDecimalHandling() throws Exception {
        assertSerialized("10.1", new BigDecimal("10.1000"), newPrettyCanonicalMapperBuilder());
    }

    @Test
    void testPrettyHugeDecimalHandling() throws Exception {
        BigDecimal actual = new BigDecimal("123456789123456789123456789123456789.123456789123456789123456789123456789123456789000");
        assertSerialized("123456789123456789123456789123456789.123456789123456789123456789123456789123456789", actual, newPrettyCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalJsonSerialization() throws Exception {
        JsonNode expected = JSON_ASSERT.loadResource(CANONICAL_1);
        JsonNode actual = buildTestData();

        assertCanonicalJson(expected, actual);
    }

    @Test
    void testCanonicalJsonSerializationRandomizedChildren() throws Exception {
        JsonNode expected = JSON_ASSERT.loadResource(CANONICAL_1);
        JsonNode actual = randomize(buildTestData());

        assertCanonicalJson(expected, actual);
    }

    @Test
    void testPrettyJsonSerialization() throws Exception {
        JsonNode actual = buildTestData();

        JSON_ASSERT.assertJson(CANONICAL_1, actual);
    }

    @Test
    void testPrettyJsonSerializationRandomizedChildren() throws Exception {
        JsonNode actual = randomize(buildTestData());

        JSON_ASSERT.assertJson(CANONICAL_1, actual);
    }

    private void assertSerialized(String expected, Object input, JsonMapper mapper) {
        String actual;
        try {
            actual = mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new AssertionError("Unable to serialize " + input, e);
        }
        assertEquals(expected, actual);
    }

    private JsonMapper newCanonicalMapperBuilder() {
        return CanonicalJsonMapper.builder().build();
    }

    private JsonMapper newPrettyCanonicalMapperBuilder() {
        return CanonicalJsonMapper.builder().prettyPrint().build();
    }
    
    private JsonNode randomize(JsonNode input) {
        if (input instanceof ObjectNode) {
            List<Map.Entry<String, JsonNode>> copy = Lists.newArrayList(input.fields());
            Collections.shuffle(copy);

            Map<String, JsonNode> randomized = new LinkedHashMap<>();
            copy.forEach(entry -> {
                randomized.put(entry.getKey(), randomize(entry.getValue()));
            });

            return new ObjectNode(JsonNodeFactory.instance, randomized);
        } else {
            return input;
        }
    }

    private void assertCanonicalJson(JsonNode expected, JsonNode actual) {
        ObjectMapper mapper = newCanonicalMapperBuilder();
        assertEquals(serialize(expected, mapper), serialize(actual, mapper));
    }

    private String serialize(JsonNode input, ObjectMapper mapper) {
        try {
            // TODO Is there a better way to sort the keys than deserializing the whole tree?
            Object obj = mapper.treeToValue(input, Object.class);
            return mapper.writeValueAsString(obj);
        } catch (JacksonException e) {
            throw new AssertionError("Unable to serialize " + input, e);
        }
    }

    private JsonNode buildTestData() {
        return new ObjectNode(JsonNodeFactory.instance) //
                .put("-0", NEGATIVE_ZERO) //
                .put("-1", -1) //
                .put("0.1", new BigDecimal("0.100")) //
                .put("1", new BigDecimal("1")) //
                .put("10.1", new BigDecimal("10.100")) //
                .put("emoji", "\uD83D\uDE03") //
                .put("escape", "\u001B") //
                .put("lone surrogate", "\uDEAD") //
                .put("whitespace", " \t\n\r") //
                ;
    }
}
