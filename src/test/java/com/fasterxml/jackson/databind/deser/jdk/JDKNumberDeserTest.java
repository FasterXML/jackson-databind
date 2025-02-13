package com.fasterxml.jackson.databind.deser.jdk;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class JDKNumberDeserTest
    extends DatabindTestUtil
{
    /*
    /**********************************************************************
    /* Helper classes, beans
    /**********************************************************************
     */

    static class MyBeanHolder {
        public Long id;
        public MyBeanDefaultValue defaultValue;
    }

    static class MyBeanDefaultValue
    {
        public MyBeanValue value;
    }

    @JsonDeserialize(using=MyBeanDeserializer.class)
    static class MyBeanValue {
        public BigDecimal decimal;
        public MyBeanValue() { this(null); }
        public MyBeanValue(BigDecimal d) { this.decimal = d; }
    }

    // [databind#2644]
    static class NodeRoot2644 {
        public String type;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes(value = {
                @JsonSubTypes.Type(value = NodeParent2644.class, name = "NodeParent")
        })
        public Node2644 node;
    }

    public static class NodeParent2644 extends Node2644 { }

    public static abstract class Node2644 {
        @JsonProperty("amount")
        BigDecimal val;

        public BigDecimal getVal() {
            return val;
        }

        public void setVal(BigDecimal val) {
            this.val = val;
        }
    }

    // [databind#2784]
    static class BigDecimalHolder2784 {
        public BigDecimal value;
    }

    static class NestedBigDecimalHolder2784 {
        @JsonUnwrapped
        public BigDecimalHolder2784 holder;
    }

    static class DeserializationIssue4917 {
        public DecimalHolder4917 decimalHolder;
        public double number;
    }

    static class DeserializationIssue4917V2 {
        public DecimalHolder4917 decimalHolder;
        public int number;
    }

    static class DeserializationIssue4917V3 {
        public BigDecimal decimal;
        public double number;
    }

    static class DecimalHolder4917 {
        public BigDecimal value;

        private DecimalHolder4917(BigDecimal value) {
            this.value = value;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static DecimalHolder4917 of(BigDecimal value) {
            return new DecimalHolder4917(value);
        }
    }

    static class Point {
        private Double x;
        private Double y;

        public Double getX() {
            return x;
        }

        public void setX(Double x) {
            this.x = x;
        }

        public Double getY() {
            return y;
        }

        public void setY(Double y) {
            this.y = y;
        }
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "type",
            visible = true)
    @JsonSubTypes(@JsonSubTypes.Type(value = CenterResult.class, name = "center"))
    static abstract class Result {
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    static class CenterResult extends Result {
        private Point center;

        private Double radius;

        public Double getRadius() {
            return radius;
        }

        public void setRadius(Double radius) {
            this.radius = radius;
        }

        public Point getCenter() {
            return center;
        }

        public void setCenter(Point center) {
            this.center = center;
        }
    }

    static class Root {
        private Result[] results;

        public Result[] getResults() {
            return results;
        }

        public void setResults(Result[] results) {
            this.results = results;
        }
    }

    /*
    /**********************************************************************
    /* Helper classes, serializers/deserializers/resolvers
    /**********************************************************************
     */

    static class MyBeanDeserializer extends JsonDeserializer<MyBeanValue>
    {
        @Override
        public MyBeanValue deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException
        {
            return new MyBeanValue(jp.getDecimalValue());
        }
    }

    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testNaN() throws Exception
    {
        Float result = MAPPER.readValue(" \"NaN\"", Float.class);
        assertEquals(Float.valueOf(Float.NaN), result);

        Double d = MAPPER.readValue(" \"NaN\"", Double.class);
        assertEquals(Double.valueOf(Double.NaN), d);

        Number num = MAPPER.readValue(" \"NaN\"", Number.class);
        assertEquals(Double.valueOf(Double.NaN), num);
    }

    @Test
    public void testDoubleInf() throws Exception
    {
        Double result = MAPPER.readValue(" \""+Double.POSITIVE_INFINITY+"\"", Double.class);
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), result);

        result = MAPPER.readValue(" \""+Double.NEGATIVE_INFINITY+"\"", Double.class);
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), result);
    }

    // 01-Mar-2017, tatu: This is bit tricky... in some ways, mapping to "empty value"
    //    would be best; but due to legacy reasons becomes `null` at this point
    @Test
    public void testEmptyAsNumber() throws Exception
    {
        assertNull(MAPPER.readValue(q(""), Byte.class));
        assertNull(MAPPER.readValue(q(""), Short.class));
        assertNull(MAPPER.readValue(q(""), Character.class));
        assertNull(MAPPER.readValue(q(""), Integer.class));
        assertNull(MAPPER.readValue(q(""), Long.class));
        assertNull(MAPPER.readValue(q(""), Float.class));
        assertNull(MAPPER.readValue(q(""), Double.class));

        assertNull(MAPPER.readValue(q(""), BigInteger.class));
        assertNull(MAPPER.readValue(q(""), BigDecimal.class));
    }

    @Test
    public void testTextualNullAsNumber() throws Exception
    {
        final String NULL_JSON = q("null");
        assertNull(MAPPER.readValue(NULL_JSON, Byte.class));
        assertNull(MAPPER.readValue(NULL_JSON, Short.class));
        // Character is bit special, can't do:
//        assertNull(MAPPER.readValue(JSON, Character.class));
        assertNull(MAPPER.readValue(NULL_JSON, Integer.class));
        assertNull(MAPPER.readValue(NULL_JSON, Long.class));
        assertNull(MAPPER.readValue(NULL_JSON, Float.class));
        assertNull(MAPPER.readValue(NULL_JSON, Double.class));

        ObjectMapper nullOksMapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();

        assertEquals(Byte.valueOf((byte) 0), nullOksMapper.readValue(NULL_JSON, Byte.TYPE));
        assertEquals(Short.valueOf((short) 0), nullOksMapper.readValue(NULL_JSON, Short.TYPE));
        // Character is bit special, can't do:
//        assertEquals(Character.valueOf((char) 0), nullOksMapper.readValue(JSON, Character.TYPE));
        assertEquals(Integer.valueOf(0), nullOksMapper.readValue(NULL_JSON, Integer.TYPE));
        assertEquals(Long.valueOf(0L), nullOksMapper.readValue(NULL_JSON, Long.TYPE));
        assertEquals(Float.valueOf(0f), nullOksMapper.readValue(NULL_JSON, Float.TYPE));
        assertEquals(Double.valueOf(0d), nullOksMapper.readValue(NULL_JSON, Double.TYPE));

        assertNull(MAPPER.readValue(NULL_JSON, BigInteger.class));
        assertNull(MAPPER.readValue(NULL_JSON, BigDecimal.class));

        // Also: verify failure for at least some
        try {
            MAPPER.readerFor(Integer.TYPE).with(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .readValue(NULL_JSON);
            fail("Should not have passed");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce String \"null\"");
        }

        ObjectMapper noCoerceMapper = jsonMapperBuilder()
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .build();
        try {
            noCoerceMapper.readValue(NULL_JSON, Integer.TYPE);
            fail("Should not have passed");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce String value");
        }
    }

    @Test
    public void testDeserializeDecimalHappyPath() throws Exception {
        String json = "{\"defaultValue\": { \"value\": 123 } }";
        MyBeanHolder result = MAPPER.readValue(json, MyBeanHolder.class);
        assertEquals(BigDecimal.valueOf(123), result.defaultValue.value.decimal);
    }

    @Test
    public void testDeserializeDecimalProperException() throws Exception {
        String json = "{\"defaultValue\": { \"value\": \"123\" } }";
        try {
            MAPPER.readValue(json, MyBeanHolder.class);
            fail("should have raised exception");
        } catch (DatabindException e) {
            verifyException(e, "not numeric");
        }
    }

    @Test
    public void testDeserializeDecimalProperExceptionWhenIdSet() throws Exception {
        String json = "{\"id\": 5, \"defaultValue\": { \"value\": \"123\" } }";
        try {
            MyBeanHolder result = MAPPER.readValue(json, MyBeanHolder.class);
            fail("should have raised exception instead value was set to " + result.defaultValue.value.decimal.toString());
        } catch (DatabindException e) {
            verifyException(e, "not numeric");
        }
    }

    // And then [databind#852]
    @Test
    public void testScientificNotationAsStringForNumber() throws Exception
    {
        Object ob = MAPPER.readValue("\"3E-8\"", Number.class);
        assertEquals(Double.class, ob.getClass());
        ob = MAPPER.readValue("\"3e-8\"", Number.class);
        assertEquals(Double.class, ob.getClass());
        ob = MAPPER.readValue("\"300000000\"", Number.class);
        assertEquals(Integer.class, ob.getClass());
        ob = MAPPER.readValue("\"123456789012\"", Number.class);
        assertEquals(Long.class, ob.getClass());
    }

    @Test
    public void testIntAsNumber() throws Exception
    {
        /* Even if declared as 'generic' type, should return using most
         * efficient type... here, Integer
         */
        Number result = MAPPER.readValue(" 123 ", Number.class);
        assertEquals(Integer.valueOf(123), result);
    }

    @Test
    public void testLongAsNumber() throws Exception
    {
        // And beyond int range, should get long
        long exp = 1234567890123L;
        Number result = MAPPER.readValue(String.valueOf(exp), Number.class);
        assertEquals(Long.valueOf(exp), result);
    }

    @Test
    public void testBigIntAsNumber() throws Exception
    {
        // and after long, BigInteger
        BigInteger biggie = new BigInteger("1234567890123456789012345678901234567890");
        Number result = MAPPER.readValue(biggie.toString(), Number.class);
        assertEquals(BigInteger.class, biggie.getClass());
        assertEquals(biggie, result);
    }

    @Test
    public void testIntTypeOverride() throws Exception
    {
        /* Slight twist; as per [JACKSON-100], can also request binding
         * to BigInteger even if value would fit in Integer
         */
        ObjectReader r = MAPPER.reader(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

        BigInteger exp = BigInteger.valueOf(123L);

        // first test as any Number
        Number result = r.forType(Number.class).readValue(" 123 ");
        assertEquals(BigInteger.class, result.getClass());
        assertEquals(exp, result);

        // then as any Object
        /*Object value =*/ r.forType(Object.class).readValue("123");
        assertEquals(BigInteger.class, result.getClass());
        assertEquals(exp, result);

        // and as JsonNode
        JsonNode node = r.readTree("  123");
        assertTrue(node.isBigInteger());
        assertEquals(123, node.asInt());
    }

    @Test
    public void testDoubleAsNumber() throws Exception
    {
        Number result = MAPPER.readValue(new StringReader(" 1.0 "), Number.class);
        assertEquals(Double.valueOf(1.0), result);
    }

    @Test
    public void testFpTypeOverrideSimple() throws Exception
    {
        ObjectReader r = MAPPER.reader(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        BigDecimal dec = new BigDecimal("0.1");

        // First test generic stand-alone Number
        Number result = r.forType(Number.class).readValue(dec.toString());
        assertEquals(BigDecimal.class, result.getClass());
        assertEquals(dec, result);

        // Then plain old Object
        Object value = r.forType(Object.class).readValue(dec.toString());
        assertEquals(BigDecimal.class, result.getClass());
        assertEquals(dec, value);

        JsonNode node = r.readTree(dec.toString());
        assertTrue(node.isBigDecimal());
        assertEquals(dec.doubleValue(), node.asDouble());
    }

    @Test
    public void testFpTypeOverrideStructured() throws Exception
    {
        ObjectReader r = MAPPER.reader(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        BigDecimal dec = new BigDecimal("-19.37");
        // List element types
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) r.forType(List.class).readValue("[ "+dec.toString()+" ]");
        assertEquals(1, list.size());
        Object val = list.get(0);
        assertEquals(BigDecimal.class, val.getClass());
        assertEquals(dec, val);

        // and a map
        Map<?,?> map = r.forType(Map.class).readValue("{ \"a\" : "+dec.toString()+" }");
        assertEquals(1, map.size());
        val = map.get("a");
        assertEquals(BigDecimal.class, val.getClass());
        assertEquals(dec, val);
    }

    // [databind#504]
    @Test
    public void testForceIntsToLongs() throws Exception
    {
        ObjectReader r = MAPPER.reader(DeserializationFeature.USE_LONG_FOR_INTS);

        Object ob = r.forType(Object.class).readValue("42");
        assertEquals(Long.class, ob.getClass());
        assertEquals(Long.valueOf(42L), ob);

        Number n = r.forType(Number.class).readValue("42");
        assertEquals(Long.class, n.getClass());
        assertEquals(Long.valueOf(42L), n);

        // and one more: should get proper node as well
        JsonNode node = r.readTree("42");
        if (!node.isLong()) {
            fail("Expected LongNode, got: "+node.getClass().getName());
        }
        assertEquals(42, node.asInt());
    }

    // [databind#2644]
    @Test
    public void testBigDecimalSubtypes() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .registerSubtypes(NodeParent2644.class)
                .build();
        NodeRoot2644 root = mapper.readValue(
                "{\"type\": \"NodeParent\",\"node\": {\"amount\": 9999999999999999.99} }",
                NodeRoot2644.class
        );

        assertEquals(new BigDecimal("9999999999999999.99"), root.node.getVal());
    }

    // [databind#2784]
    @Test
    public void testBigDecimalUnwrapped() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        // mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        final String JSON = "{\"value\": 5.00}";
        NestedBigDecimalHolder2784 result = mapper.readValue(JSON, NestedBigDecimalHolder2784.class);
        assertEquals(new BigDecimal("5.00"), result.holder.value);
    }

    private final String BIG_DEC_STR;
    {
        StringBuilder sb = new StringBuilder("-1234.");
        // Above 500 chars we get a problem:
        for (int i = 520; --i >= 0; ) {
            sb.append('0');
        }
        BIG_DEC_STR = sb.toString();
    }
    private final BigDecimal BIG_DEC = new BigDecimal(BIG_DEC_STR);

    // [databind#4694]: decoded wrong by jackson-core/FDP for over 500 char numbers
    @Test
    public void bigDecimal4694FromString() throws Exception
    {
        assertEquals(BIG_DEC, MAPPER.readValue(BIG_DEC_STR, BigDecimal.class));
    }

    @Test
    public void bigDecimal4694FromBytes() throws Exception
    {
        byte[] b = utf8Bytes(BIG_DEC_STR);
        assertEquals(BIG_DEC, MAPPER.readValue(b, 0, b.length, BigDecimal.class));
    }

    // [databind#4917]    
    @Test
    public void bigDecimal4917() throws Exception
    {
        DeserializationIssue4917 issue = MAPPER.readValue(
                a2q("{'decimalHolder':100.00,'number':50}"),
                DeserializationIssue4917.class);
        assertEquals(new BigDecimal("100.00"), issue.decimalHolder.value);
        assertEquals(50.0, issue.number);
    }

    @Test
    public void bigDecimal4917V2() throws Exception
    {
        DeserializationIssue4917V2 issue = MAPPER.readValue(
                a2q("{'decimalHolder':100.00,'number':50}"),
                DeserializationIssue4917V2.class);
        assertEquals(new BigDecimal("100.00"), issue.decimalHolder.value);
        assertEquals(50, issue.number);
    }

    @Test
    public void bigDecimal4917V3() throws Exception
    {
        DeserializationIssue4917V3 issue = MAPPER.readValue(
                a2q("{'decimal':100.00,'number':50}"),
                DeserializationIssue4917V3.class);
        assertEquals(new BigDecimal("100.00"), issue.decimal);
        assertEquals(50, issue.number);
    }

    // https://github.com/FasterXML/jackson-core/issues/1397
    @Test
    public void issue1397() throws Exception {
        final String dataString = a2q("{ 'results': [ { " +
                      "'radius': 179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000, " +
                      "'type': 'center', " +
                      "'center': { " +
                      "'x': -11.0, " +
                      "'y': -2.0 } } ] }");

        Root object = MAPPER.readValue(dataString, Root.class);

        CenterResult result = (CenterResult) Arrays.stream(object.getResults()).findFirst().get();

        assertEquals(-11.0d, result.getCenter().getX());
        assertEquals(-2.0d, result.getCenter().getY());
    }
}
