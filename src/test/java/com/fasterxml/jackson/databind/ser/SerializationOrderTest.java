package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for verifying that constraints on ordering of serialized
 * properties are held.
 */
public class SerializationOrderTest
    extends BaseMapTest
{
    static class BeanWithCreator
    {
        public int a;
        public int b;
        public int c;

        @JsonCreator public BeanWithCreator(@JsonProperty("c") int c, @JsonProperty("a") int a) {
            this.a = a;
            this.c = c;
        }
    }

    @JsonPropertyOrder({"c", "a", "b"})
    static class BeanWithOrder
    {
        public int d, b, a, c;

        public BeanWithOrder(int a, int b, int c, int d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }
    }

    @JsonPropertyOrder(value={"d"}, alphabetic=true)
    static class SubBeanWithOrder extends BeanWithOrder
    {
        public SubBeanWithOrder(int a, int b, int c, int d) {
            super(a, b, c, d);
        }
    }

    @JsonPropertyOrder({"b", "a",
        // note: including non-existant properties is fine (has no effect, but not an error)
        "foobar",
        "c"
    })
    static class OrderMixIn { }

    @JsonPropertyOrder(value={"a","b","x","z"})
    static class BeanFor268 {
        @JsonProperty("a") public String xA = "a";
        @JsonProperty("z") public String aZ = "z";
    	   @JsonProperty("b") public String xB() { return "b"; }
    	   @JsonProperty("x") public String aX() { return "x"; }
    }

    static class BeanFor459 {
        public int d = 4;
        public int c = 3;
        public int b = 2;
        public int a = 1;
    }

    // For [databind#311]
    @JsonPropertyOrder(alphabetic = true)
    static class BeanForGH311 {
        private final int a;
        private final int b;

        @JsonCreator
        public BeanForGH311(@JsonProperty("b") int b, @JsonProperty("a") int a) { //b and a are out of order, although alphabetic = true
            this.a = a;
            this.b = b;
        }

        public int getA() { return a; }
        public int getB() { return b; }
    }

    // We'll expect ordering of "FUBAR"
    @JsonPropertyOrder({ "f"  })
    static class OrderingByIndexBean {
        public int r;
        public int a;

        @JsonProperty(index = 1)
        public int b;

        @JsonProperty(index = 0)
        public int u;

        public int f;
    }

    // For [databind#2879]
    @JsonPropertyOrder({ "a", "c" })
    static class BeanFor2879 {
        public int c;
        public int b;
        public int a;

        @JsonCreator
        public BeanFor2879(@JsonProperty("a") int a,
                @JsonProperty("b") int b,
                @JsonProperty("c") int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    // For [databind#2879]
    static class BeanForStrictOrdering {
        private final int a;
        private int b;
        private final int c;

        @JsonCreator
        public BeanForStrictOrdering(@JsonProperty("c") int c, @JsonProperty("a") int a) { //b and a are out of order, although alphabetic = true
            this.a = a;
            this.c = c;
        }

        public int getA() { return a; }
        public int getB() { return b; }
        public int getC() { return c; }
    }

    /*
    /*********************************************
    /* Unit tests
    /*********************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper ALPHA_MAPPER = jsonMapperBuilder()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .build();

    public void testImplicitOrderByCreator() throws Exception {
        assertEquals("{\"c\":1,\"a\":2,\"b\":0}",
                MAPPER.writeValueAsString(new BeanWithCreator(1, 2)));
    }

    public void testExplicitOrder() throws Exception {
        assertEquals("{\"c\":3,\"a\":1,\"b\":2,\"d\":4}",
                MAPPER.writeValueAsString(new BeanWithOrder(1, 2, 3, 4)));
    }

    public void testAlphabeticOrder() throws Exception {
        assertEquals("{\"d\":4,\"a\":1,\"b\":2,\"c\":3}",
                MAPPER.writeValueAsString(new SubBeanWithOrder(1, 2, 3, 4)));
    }

    public void testOrderWithMixins() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .addMixIn(BeanWithOrder.class, OrderMixIn.class)
                .build();
        assertEquals("{\"b\":2,\"a\":1,\"c\":3,\"d\":4}",
                serializeAsString(m, new BeanWithOrder(1, 2, 3, 4)));
    }

    public void testOrderWrt268() throws Exception
    {
        assertEquals("{\"a\":\"a\",\"b\":\"b\",\"x\":\"x\",\"z\":\"z\"}",
                MAPPER.writeValueAsString(new BeanFor268()));
    }

    public void testOrderWithFeature() throws Exception
    {
        assertEquals("{\"a\":1,\"b\":2,\"c\":3,\"d\":4}",
                ALPHA_MAPPER.writeValueAsString(new BeanFor459()));
    }

    // [databind#2879]: verify that Creator properties never override explicit
    //   order
    public void testCreatorVsExplicitOrdering() throws Exception
    {
        assertEquals(a2q("{'a':1,'c':3,'b':2}"),
                MAPPER.writeValueAsString(new BeanFor2879(1, 2, 3)));
        assertEquals(a2q("{'a':1,'c':3,'b':2}"),
                ALPHA_MAPPER.writeValueAsString(new BeanFor2879(1, 2, 3)));
    }

    // [databind#311]
    public void testAlphaAndCreatorOrdering() throws Exception
    {
        String json = ALPHA_MAPPER.writeValueAsString(new BeanForGH311(2, 1));
        assertEquals("{\"a\":1,\"b\":2}", json);
    }

    // [databind#2555]
    public void testOrderByIndexEtc() throws Exception
    {
        // since "default" order can actually vary with later JDKs, only verify
        // case of alphabetic-as-default
        assertEquals(a2q("{'f':0,'u':0,'b':0,'a':0,'r':0}"),
                ALPHA_MAPPER.writeValueAsString(new OrderingByIndexBean()));
    }

    // [databind#2879]: allow preventing Creator properties from overriding
    //    alphabetic ordering
    public void testStrictAlphaAndCreatorOrdering() throws Exception
    {
        // without changing defaults, creators are sorted before other properties
        // BUT are sorted within their own category
        assertTrue(ALPHA_MAPPER.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        assertTrue(ALPHA_MAPPER.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST));
        assertEquals(a2q("{'a':3,'c':2,'b':0}"),
                ALPHA_MAPPER.writeValueAsString(new BeanForStrictOrdering(2, 3)));

        // but can change that
        final ObjectMapper STRICT_ALPHA_MAPPER = jsonMapperBuilder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .disable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
                .build();

        assertEquals(a2q("{'a':2,'b':0,'c':1}"),
                STRICT_ALPHA_MAPPER.writeValueAsString(new BeanForStrictOrdering(1, 2)));
    }
}
