package com.fasterxml.jackson.databind.ser;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Test to verify that the order of properties is preserved when using @JsonPropertyOrder
 * with @JsonUnwrapped and @JsonAnyGetter
 */
public class JsonPropertyOrder4388Test extends DatabindTestUtil
{
    // Base class with properties
    static class BaseWithProperties {
        public String entityName;
        public int entityId;
        public Integer totalTests;
        @JsonAnyGetter
        public Map<String, Object> products;
        @JsonUnwrapped
        public Location childEntities;
    }

    // @JsonAnyGetter, with different property order
    @JsonPropertyOrder({"childEntities", "entityId", "totalTests", "entityName", "products"})
    static class PojoPropertyVersion1 extends BaseWithProperties {
    }

    // @JsonAnyGetter, with different property order
    @JsonPropertyOrder({"entityId", "totalTests", "childEntities", "entityName", "products"})
    static class PojoPropertyVersion2 extends BaseWithProperties {
    }

    // @JsonUnwrapped, with different property order
    @JsonPropertyOrder({"childEntities", "entityId", "totalTests", "entityName", "products"})
    static class PojoUnwrappedVersion1 extends BaseWithProperties {
    }

    // @JsonUnwrapped, with different property order
    @JsonPropertyOrder({"entityId", "totalTests", "childEntities", "entityName", "products"})
    static class PojoUnwrappedVersion2 extends BaseWithProperties {
    }

    static class Location {
        public int child1;
        public int child2;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // For [databind#4388]
    @Test
    public void testSerializationOrderVersion1() throws Exception {
        PojoPropertyVersion1 input = new PojoPropertyVersion1();
        _configureValues(input);

        String json = MAPPER.writeValueAsString(input);

        assertEquals(a2q("{" +
                "'child1':3," +
                "'child2':3," +
                "'entityId':1," +
                "'totalTests':2," +
                "'entityName':'Bob'," +
                "'product1':4}"),
            json);
    }

    @Test
    public void testSerializationOrderVersion2() throws Exception {
        PojoPropertyVersion2 input = new PojoPropertyVersion2();
        _configureValues(input);

        String json = MAPPER.writeValueAsString(input);

        assertEquals(a2q("{" +
                "'entityId':1," +
                "'totalTests':2," +
                "'child1':3," +
                "'child2':3," +
                "'entityName':'Bob'," +
                "'product1':4}"),
            json);
    }

    @Test
    public void testSerializationOrderUnwrappedVersion1() throws Exception {
        PojoUnwrappedVersion1 input = new PojoUnwrappedVersion1();
        _configureValues(input);

        String json = MAPPER.writeValueAsString(input);

        assertEquals(a2q("{" +
                "'child1':3," +
                "'child2':3," +
                "'entityId':1," +
                "'totalTests':2," +
                "'entityName':'Bob'," +
                "'product1':4}"),
            json);
    }

    @Test
    public void testSerializationOrderUnwrappedVersion2() throws Exception {
        PojoUnwrappedVersion2 input = new PojoUnwrappedVersion2();
        _configureValues(input);

        String json = MAPPER.writeValueAsString(input);

        assertEquals(a2q("{" +
                "'entityId':1," +
                "'totalTests':2," +
                "'child1':3," +
                "'child2':3," +
                "'entityName':'Bob'," +
                "'product1':4}"),
            json);
    }

    private void _configureValues(BaseWithProperties base) {
        base.entityId = 1;
        base.entityName = "Bob";
        base.totalTests = 2;
        base.childEntities = new Location();
        base.childEntities.child1 = 3;
        base.childEntities.child2 = 3;
        base.products = new HashMap<>();
        base.products.put("product1", 4);
    }
}
