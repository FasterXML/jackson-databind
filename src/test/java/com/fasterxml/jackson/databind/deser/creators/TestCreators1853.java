package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

// Reproduction for [databind#1853], problem with delegating creator,
// but only explicit case
public class TestCreators1853 extends BaseMapTest
{
    public static class Product {
        String name;

        public Object other, errors;
        
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Product(@JsonProperty("name") String name) {
            this.name = "PROP:" + name;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Product from(String name){
            return new Product(false, "DELEG:"+name);
        }

        private Product(boolean bogus, String name) {
            this.name = name;
        }

        @JsonValue
        public String getName(){
            return name;
        }
    }

    private static final String EXAMPLE_DATA = "{\"name\":\"dummy\",\"other\":{},\"errors\":{}}";

    private final ObjectMapper MAPPER = newObjectMapper();

    public void testSerialization() throws Exception {
        assertEquals(quote("testProduct"),
                MAPPER.writeValueAsString(new Product(false, "testProduct")));
    }

    public void testDeserializationFromObject() throws Exception {
        assertEquals("PROP:dummy", MAPPER.readValue(EXAMPLE_DATA, Product.class).getName());
    }

    public void testDeserializationFromString() throws Exception {
        assertEquals("DELEG:testProduct",
                MAPPER.readValue(quote("testProduct"), Product.class).getName());
    }
}
