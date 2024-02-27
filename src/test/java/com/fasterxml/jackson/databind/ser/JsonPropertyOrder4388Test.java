package com.fasterxml.jackson.databind.ser;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test to verify that the order of properties is preserved when using @JsonPropertyOrder
 * with @JsonUnwrapped and @JsonAnyGetter
 */
public class JsonPropertyOrder4388Test extends DatabindTestUtil {
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
    @JsonPropertyOrder({"entityId", "totalTests", "childEntities", "products", "entityName"})
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

    @JsonIgnoreProperties("b")
    static class IgnorePropertiesOnFieldPojo {
        public int a = 1;
        public int b = 2;
        @JsonAnyGetter
        public Map<String, Object> map = new HashMap<>();
    }

    static class IgnorePropertiesOnAnyGetterPojo {
        public int a = 1;
        public int b = 2;
        @JsonIgnoreProperties("b")
        @JsonAnyGetter
        public Map<String, Object> map = new HashMap<>();
    }

    static class IgnoreOnFieldPojo {
        public int a = 1;
        @JsonIgnore
        public int b = 2;
        @JsonAnyGetter
        public Map<String, Object> map = new HashMap<>();
    }

    static class AlphabeticOrderOnAnyGetterBean {
        @JsonPropertyOrder(alphabetic = true)
        @JsonAnyGetter
        public Map<String, Object> map = new LinkedHashMap<>();
    }

    @JsonPropertyOrder(alphabetic = true)
    static class AlphabeticOrderOnClassBean {
        public int c = 3;
        public int a = 1;
        public int b = 2;
        @JsonAnyGetter
        public Map<String, Object> map = new LinkedHashMap<>();
    }

    static class LinkUnlinkConflictPojo {
        private Map<String, Object> properties = new HashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getProperties() {
            properties.put("key", "value");
            return properties;
        }

        @JsonIgnore
        public String getProperties(String key) {
            // This method is unrelated to the any-getter and should not affect serialization
            return "unrelated";
        }

        @JsonIgnore
        public String getKey() {
            // This method is unrelated to the any-getter and should not affect serialization
            return "unrelated";
        }
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
                "'product1':4," +
                "'entityName':'Bob'}"),
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

    @Test
    public void testIgnoreProperties() throws Exception {
        // Respsect @JsonIgnoreProperties 'b' from Pojo, but not from map
        IgnorePropertiesOnFieldPojo bean = new IgnorePropertiesOnFieldPojo();
        bean.map.put("b", 3);
        assertEquals(a2q("{'a':1,'b':3}"), MAPPER.writeValueAsString(bean));

        // Respect @JsonIgnoreProperties 'b' from Pojo, but not from map
        IgnorePropertiesOnAnyGetterPojo bean2 = new IgnorePropertiesOnAnyGetterPojo();
        bean2.map.put("b", 3);
        assertEquals(a2q("{'a':1,'b':2}"), MAPPER.writeValueAsString(bean2));

        // Respect @JsonIgnore from Pojo, but not from map
        IgnoreOnFieldPojo bean3 = new IgnoreOnFieldPojo();
        bean3.map.put("b", 3);
        assertEquals(a2q("{'a':1,'b':3}"), MAPPER.writeValueAsString(bean3));
    }

    // Sorting works on @JsonAnyGetter, when adding @JsonPropertyOrder directly on the AnyGetter method
    @Test
    public void testSortingOnAnyGetter() throws Exception {
        AlphabeticOrderOnAnyGetterBean bean = new AlphabeticOrderOnAnyGetterBean();
        bean.map.put("zd", 4);
        bean.map.put("zc", 3);
        bean.map.put("za", 1);
        bean.map.put("zb", 2);
        assertEquals(a2q("{" +
            "'za':1," +
            "'zb':2," +
            "'zc':3," +
            "'zd':4}"), MAPPER.writeValueAsString(bean));
    }

    // Sorting does not work on @JsonAnyGetter, when adding @JsonPropertyOrder on the class
    @Test
    public void testSortingOnClassNotPropagateToAnyGetter() throws Exception {
        AlphabeticOrderOnClassBean bean = new AlphabeticOrderOnClassBean();
        bean.map.put("zc", 3);
        bean.map.put("za", 1);
        bean.map.put("zb", 2);
        assertEquals(a2q("{" +
            "'a':1," +
            "'b':2," +
            "'c':3," +
            "'zc':3," +
            "'za':1," +
            "'zb':2}"), MAPPER.writeValueAsString(bean));
    }

    @Test
    public void testLinkUnlinkWithJsonIgnore() throws Exception {
        LinkUnlinkConflictPojo pojo = new LinkUnlinkConflictPojo();
        String json = MAPPER.writeValueAsString(pojo);

        assertEquals(a2q("{'key':'value'}"), json);
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
