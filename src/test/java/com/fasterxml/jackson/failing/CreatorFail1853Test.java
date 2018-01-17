package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class CreatorFail1853Test extends BaseMapTest
{
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        String name;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Product(@JsonProperty("name") String name)
        {
            this.name = name;
        }

        @JsonValue
        public String getName(){
            return name;
        }

        @Override
        public String toString() {
            return "|~" + name + "~|";
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Product from(String name){
            return new Product(name);
        }
    }

    private static final String EXAMPLE_DATA = "{\"name\":\"dummy\",\"other\":{},\"errors\":{}}";
    private static final String TEST_PRODUCT_JSON = "\"testProduct\"";

    private ObjectMapper objectMapper = new ObjectMapper();

    public void testSerialization() throws Exception {
        assertEquals(TEST_PRODUCT_JSON,
                objectMapper.writeValueAsString(new Product("testProduct")));
    }

    public void testDeserializationFromObject() throws Exception {
        assertEquals("dummy", objectMapper.readValue(EXAMPLE_DATA, Product.class).getName());
    }

    public void testDeserializationFromString() throws Exception {
        assertEquals("testProduct", objectMapper.readValue(TEST_PRODUCT_JSON, Product.class).getName());
    }
}
