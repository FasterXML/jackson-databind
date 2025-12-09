package tools.jackson.databind.ser;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test to verify that the order of properties is preserved when using @JsonPropertyOrder
 * with @JsonUnwrapped and @JsonAnyGetter
 */
public class AnyGetterOrdering4388Test extends DatabindTestUtil
{
    // For [databind#518]

    @JsonPropertyOrder(alphabetic = true)
    static class Bean518
    {
        public int b;

        protected Map<String,Object> extra = new HashMap<>();

        public int a;

        public Bean518(int a, int b, Map<String,Object> x) {
            this.a = a;
            this.b = b;
            extra = x;
        }

        @JsonAnyGetter
        public Map<String,Object> getExtra() { return extra; }
    }
    
    // For [databind#4388]

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
    
    @JsonPropertyOrder({"child1", "child2"})
    static class Location {
        public int child1;
        public int child2;
    }

    @JsonIgnoreProperties("b")
    static class IgnorePropertiesOnFieldPojo {
        public int a = 1, b = 2;
        @JsonAnyGetter
        public Map<String, Object> map = new HashMap<>();
    }

    @JsonPropertyOrder({"a", "b"})
    static class IgnorePropertiesOnAnyGetterPojo {
        public int a = 1, b = 2;
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
        public int c = 3, a = 1, b = 2;
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

    @JsonPropertyOrder({ "firstProperty", "secondProperties", "thirdProperty", "fourthProperty" })
    static class PrivateAnyGetterPojo {
        public int firstProperty = 1, fourthProperty = 4, thirdProperty = 3;

        @JsonAnyGetter
        private Map<String, Object> secondProperties = new HashMap<>();

        public PrivateAnyGetterPojo add(String key, Object value) {
            secondProperties.put(key, value);
            return this;
        }

        public Map<String, Object> secondProperties() {
            return secondProperties;
        }
    }

    @JsonPropertyOrder({ "firstProperty", "secondProperties", "thirdProperty", "fourthProperty" })
    static class PrivateAnyGetterPojoSorted extends PrivateAnyGetterPojo {
        public Map<String, Object> getSecondProperties() {
            return super.secondProperties;
        }
    }

    // For [databind#5215]: Any-getter should be sorted last, by default
    static class DynaBean5215 {
        public String l;
        public String j;
        public String a;

        protected Map<String, Object> extensions = new LinkedHashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getExtensions() {
            return extensions;
        }

        @JsonAnySetter
        public void addExtension(String name, Object value) {
            extensions.put(name, value);
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */
    
    private final ObjectMapper MAPPER = newJsonMapper();

    // For [databind#518]
    @Test
    void anyBeanWithSort518() throws Exception
    {
        Map<String,Object> extra = new LinkedHashMap<>();
        extra.put("y", 4);
        extra.put("x", 3);
        String json = MAPPER.writeValueAsString(new Bean518(2, 1, extra));
        assertEquals(a2q("{'a':2,'b':1,'y':4,'x':3}"), json);
    }

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


    @Test
    public void testPrivateAnyGetter() throws Exception {
        PrivateAnyGetterPojo pojo = new PrivateAnyGetterPojo();
        pojo.add("secondProperty", 2);
        String json = MAPPER.writeValueAsString(pojo);

        assertEquals(a2q("{" +
                "'firstProperty':1," +
                "'thirdProperty':3," +
                "'fourthProperty':4," +
                "'secondProperty':2}"), // private accesor, wont' work here
            json);
    }

    @Test
    public void testPrivateAnyGetterSorted() throws Exception {
        PrivateAnyGetterPojoSorted pojo = new PrivateAnyGetterPojoSorted();
        pojo.add("secondProperty", 2);
        String json = MAPPER.writeValueAsString(pojo);

        assertEquals(a2q("{" +
                "'firstProperty':1," +
                "'secondProperty':2," + // private accesor, wont' work here
                "'thirdProperty':3," +
                "'fourthProperty':4}"),
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

    // For [databind#5215]: Any-getter should be sorted last, by default
    @Test
    public void dynaBean5215() throws Exception
    {
        final ObjectMapper mapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .build();

        DynaBean5215 b = new DynaBean5215();
        b.a = "1";
        b.j = "2";
        b.l = "3";
        b.addExtension("z", "5");
        b.addExtension("b", "4");
        assertEquals(a2q("{" +
                "'a':'1'," +
                "'j':'2'," +
                "'l':'3'," +
                "'b':'4'," +
                "'z':'5'}"), mapper.writeValueAsString(b));
    }
}
