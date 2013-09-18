package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for verifying that constraints on ordering of serialized
 * properties are held.
 */
public class TestSerializationOrder
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Annotated helper classes
    /**********************************************************
     */

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

    // For [Issue#311]
    @JsonPropertyOrder(alphabetic = true)
    public class BeanForGH311 {
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
    
    /*
    /*********************************************
    /* Unit tests
    /*********************************************
     */

    final ObjectMapper MAPPER = new ObjectMapper();
    
    // Test for [JACKSON-170]
    public void testImplicitOrderByCreator() throws Exception
    {
        assertEquals("{\"c\":1,\"a\":2,\"b\":0}", MAPPER.writeValueAsString(new BeanWithCreator(1, 2)));
    }

    public void testExplicitOrder() throws Exception
    {
        assertEquals("{\"c\":3,\"a\":1,\"b\":2,\"d\":4}", MAPPER.writeValueAsString(new BeanWithOrder(1, 2, 3, 4)));
    }

    public void testAlphabeticOrder() throws Exception
    {
        assertEquals("{\"d\":4,\"a\":1,\"b\":2,\"c\":3}", MAPPER.writeValueAsString(new SubBeanWithOrder(1, 2, 3, 4)));
    }


    public void testOrderWithMixins() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.addMixInAnnotations(BeanWithOrder.class, OrderMixIn.class);
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
        ObjectMapper m = new ObjectMapper();
        m.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        assertEquals("{\"a\":1,\"b\":2,\"c\":3,\"d\":4}",
                m.writeValueAsString(new BeanFor459()));
    }

    // [Issue#311]

    public void testAlphaAndCreatorOrdering() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        String json = m.writeValueAsString(new BeanForGH311(2, 1));
        assertEquals("{\"a\":1,\"b\":2}", json);
    }
}
