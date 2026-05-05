package tools.jackson.databind.ser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.std.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class AnyGetterTest extends DatabindTestUtil
{
    static class Bean
    {
        final static Map<String,Boolean> extra = new HashMap<String,Boolean>();
        static {
            extra.put("a", Boolean.TRUE);
        }

        public int getX() { return 3; }

        @JsonAnyGetter
        public Map<String,Boolean> getExtra() { return extra; }
    }

    static class AnyOnlyBean
    {
        @JsonAnyGetter
        public Map<String,Integer> any() {
            HashMap<String,Integer> map = new HashMap<String,Integer>();
            map.put("a", 3);
            return map;
        }
    }

    // For [databind#1376]: allow disabling any-getter
    static class NotEvenAnyBean extends AnyOnlyBean
    {
        @JsonAnyGetter(enabled=false)
        @Override
        public Map<String,Integer> any() {
            throw new RuntimeException("Should not get called!)");
        }

        public int getValue() { return 42; }
    }

    static class MapAsAny
    {
        protected Map<String,Object> stuff = new LinkedHashMap<String,Object>();

        @JsonAnyGetter
        public Map<String,Object> any() {
            return stuff;
        }

        public void add(String key, Object value) {
            stuff.put(key, value);
        }
    }

    static class Issue705Bean
    {
        protected Map<String,String> stuff;

        public Issue705Bean(String key, String value) {
            stuff = new LinkedHashMap<String,String>();
            stuff.put(key, value);
        }

        @JsonSerialize(using = Issue705Serializer.class)
        @JsonAnyGetter
        public Map<String, String> getParameters(){
            return stuff;
        }
    }

    static class Issue705Serializer extends StdSerializer<Object>
    {
        public Issue705Serializer() {
            super(Map.class);
        }

        @Override
        public void serialize(Object value, JsonGenerator g, SerializationContext ctxt)
        {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?,?> entry : ((Map<?,?>) value).entrySet()) {
                sb.append('[').append(entry.getKey()).append('/').append(entry.getValue()).append(']');
            }
            g.writeStringProperty("stuff", sb.toString());
        }
    }

    // [databind#1124]
    static class Bean1124
    {
        protected Map<String,String> additionalProperties;

        public void addAdditionalProperty(String key, String value) {
            if (additionalProperties == null) {
                additionalProperties = new HashMap<String,String>();
            }
            additionalProperties.put(key,value);
        }

        public void setAdditionalProperties(Map<String, String> additionalProperties) {
            this.additionalProperties = additionalProperties;
        }

        @JsonAnyGetter
        @JsonSerialize(contentUsing=MyUCSerializer.class)
        public Map<String,String> getAdditionalProperties() { return additionalProperties; }
    }

    // [databind#1124]
    static class MyUCSerializer extends StdScalarSerializer<String>
    {
        public MyUCSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator gen,
                SerializationContext provider) {
            gen.writeString(value.toUpperCase());
        }
    }

    static class Bean2592NoAnnotations
    {
        protected Map<String, String> properties = new LinkedHashMap<>();

        @JsonAnyGetter
        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        public void add(String key, String value) {
            properties.put(key, value);
        }
    }

    static class Bean2592PropertyIncludeNonEmpty extends Bean2592NoAnnotations
    {
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        @JsonAnyGetter
        @Override
        public Map<String, String> getProperties() {
            return properties;
        }
    }

    @JsonFilter("Bean2592")
    static class Bean2592WithFilter extends Bean2592NoAnnotations {}

    // [databind#1458]: Allow `@JsonAnyGetter` on fields too
    static class DynaFieldBean {
        public int id;

        @JsonAnyGetter
        @JsonAnySetter
        protected HashMap<String,String> other = new HashMap<String,String>();

        public Map<String,String> any() {
            return other;
        }

        public void set(String name, String value) {
            other.put(name, value);
        }
    }

    // [databind#1458]: Allow `@JsonAnyGetter` on fields too
    static class DynaFieldOrderedBean {
        public int id = 123;

        @JsonPropertyOrder(alphabetic = true)
        @JsonAnyGetter
        @JsonAnySetter
        private HashMap<String,String> other = new LinkedHashMap<>();

        public Map<String,String> any() {
            return other;
        }

        public void set(String name, String value) {
            other.put(name, value);
        }
    }

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
    static class BaseWithProperties {
        public String entityName;
        public int entityId;
        public Integer totalTests;
        @JsonAnyGetter
        public Map<String, Object> products;
        @JsonUnwrapped
        public Location childEntities;
    }

    @JsonPropertyOrder({"childEntities", "entityId", "totalTests", "entityName", "products"})
    static class PojoPropertyVersion1 extends BaseWithProperties { }

    @JsonPropertyOrder({"entityId", "totalTests", "childEntities", "products", "entityName"})
    static class PojoPropertyVersion2 extends BaseWithProperties { }

    @JsonPropertyOrder({"childEntities", "entityId", "totalTests", "entityName", "products"})
    static class PojoUnwrappedVersion1 extends BaseWithProperties { }

    @JsonPropertyOrder({"entityId", "totalTests", "childEntities", "entityName", "products"})
    static class PojoUnwrappedVersion2 extends BaseWithProperties { }

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
            return "unrelated";
        }

        @JsonIgnore
        public String getKey() {
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

    // [databind#3604]: Allow ObjectNode for @JsonAnyGetter (field)
    static class ObjectNodeAnyGetterFieldBean {
        public int id;

        @JsonAnyGetter
        public ObjectNode extra;
    }

    // [databind#3604]: Allow ObjectNode for @JsonAnyGetter (method)
    static class ObjectNodeAnyGetterMethodBean {
        public int id;
        private ObjectNode extra;

        @JsonAnyGetter
        public ObjectNode getExtra() { return extra; }
        public void setExtra(ObjectNode extra) { this.extra = extra; }
    }

    // [databind#3604]: Allow JsonNode for @JsonAnyGetter (field)
    static class JsonNodeAnyGetterFieldBean {
        public int id;

        @JsonAnyGetter
        public JsonNode extra;
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
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1458]: Allow `@JsonAnyGetter` on fields too
    @Test
    public void testDynaFieldBean() throws Exception
    {
        DynaFieldBean b = new DynaFieldBean();
        b.id = 123;
        b.set("name", "Billy");
        assertEquals("{\"id\":123,\"name\":\"Billy\"}", MAPPER.writeValueAsString(b));

        DynaFieldBean result = MAPPER.readValue("{\"id\":2,\"name\":\"Joe\"}", DynaFieldBean.class);
        assertEquals(2, result.id);
        assertEquals("Joe", result.other.get("name"));
    }

    // [databind#4388]: Allow `@JsonPropertyOrder` AND `@JsonAnyGetter` on fields too
    @Test
    public void testDynaFieldOrderedBean() throws Exception
    {
        DynaFieldOrderedBean b = new DynaFieldOrderedBean();
        b.set("nameC", "Cilly");
        b.set("nameB", "Billy");
        b.set("nameA", "Ailly");

        assertEquals("{\"id\":123,\"nameA\":\"Ailly\",\"nameB\":\"Billy\",\"nameC\":\"Cilly\"}", MAPPER.writeValueAsString(b));
    }

    @Test
    public void testSimpleAnyBean() throws Exception
    {
        String json = MAPPER.writeValueAsString(new Bean());
        Map<?,?> map = MAPPER.readValue(json, Map.class);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("x"));
        assertEquals(Boolean.TRUE, map.get("a"));
    }

    @Test
    public void testAnyOnly() throws Exception
    {
        ObjectMapper m;

        // First, with normal fail settings:
        m = jsonMapperBuilder()
                .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
        assertEquals("{\"a\":3}", m.writeValueAsString(new AnyOnlyBean()));

        // then without fail
        String json = m.writer()
                .without(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .writeValueAsString(new AnyOnlyBean());
        assertEquals("{\"a\":3}", json);
    }

    @Test
    public void testAnyDisabling() throws Exception
    {
        String json = MAPPER.writeValueAsString(new NotEvenAnyBean());
        assertEquals(a2q("{'value':42}"), json);
    }

    // Trying to repro [databind#577]
    @Test
    public void testAnyWithNull() throws Exception
    {
        MapAsAny input = new MapAsAny();
        input.add("bar", null);
        assertEquals(a2q("{'bar':null}"),
                MAPPER.writeValueAsString(input));
    }

    @Test
    public void testIssue705() throws Exception
    {
        Issue705Bean input = new Issue705Bean("key", "value");
        String json = MAPPER.writer()
                .without(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .writeValueAsString(input);
        assertEquals("{\"stuff\":\"[key/value]\"}", json);
    }

    // [databind#1124]
    @Test
    public void testAnyGetterWithValueSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Bean1124 input = new Bean1124();
        input.addAdditionalProperty("key", "value");
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"key\":\"VALUE\"}", json);
    }

    // [databind#2592]
    @Test
    public void testAnyGetterWithMapperDefaultIncludeNonEmpty() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl
                        .withValueInclusion(JsonInclude.Include.NON_EMPTY)
                        .withContentInclusion(JsonInclude.Include.NON_EMPTY))
                .build();
        Bean2592NoAnnotations input = new Bean2592NoAnnotations();
        input.add("non-empty", "property");
        input.add("empty", "");
        input.add("null", null);
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"non-empty\":\"property\"}", json);
    }

    // [databind#2592]
    @Test
    public void testAnyGetterWithMapperDefaultIncludeNonEmptyAndFilterOnBean() throws Exception
    {
        FilterProvider filters = new SimpleFilterProvider()
                .addFilter("Bean2592", SimpleBeanPropertyFilter.serializeAllExcept("something"));
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl
                        .withValueInclusion(JsonInclude.Include.NON_EMPTY)
                        .withContentInclusion(JsonInclude.Include.NON_EMPTY))
                .filterProvider(filters)
                .build();
        Bean2592WithFilter input = new Bean2592WithFilter();
        input.add("non-empty", "property");
        input.add("empty", "");
        input.add("null", null);
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"non-empty\":\"property\"}", json);
    }

    // [databind#2592]
    @Test
    public void testAnyGetterWithPropertyIncludeNonEmpty() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Bean2592PropertyIncludeNonEmpty input = new Bean2592PropertyIncludeNonEmpty();
        input.add("non-empty", "property");
        input.add("empty", "");
        input.add("null", null);
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"non-empty\":\"property\"}", json);
    }

    // [databind#2592]
    @Test
    public void testAnyGetterConfigIncludeNonEmpty() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(Map.class, incl -> incl.setInclude(
                    JsonInclude.Value.construct(JsonInclude.Include.USE_DEFAULTS,
                    JsonInclude.Include.NON_EMPTY)))
                .build();
        Bean2592NoAnnotations input = new Bean2592NoAnnotations();
        input.add("non-empty", "property");
        input.add("empty", "");
        input.add("null", null);
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"non-empty\":\"property\"}", json);
    }

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
        // Respect @JsonIgnoreProperties 'b' from Pojo, but not from map
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
                "'secondProperty':2}"),
            json);
    }

    @Test
    public void testPrivateAnyGetterSorted() throws Exception {
        PrivateAnyGetterPojoSorted pojo = new PrivateAnyGetterPojoSorted();
        pojo.add("secondProperty", 2);
        String json = MAPPER.writeValueAsString(pojo);
        assertEquals(a2q("{" +
                "'firstProperty':1," +
                "'secondProperty':2," +
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

    // [databind#3604]: Allow ObjectNode field for @JsonAnyGetter
    @Test
    public void testAnyGetterWithObjectNodeField() throws Exception
    {
        ObjectNodeAnyGetterFieldBean bean = new ObjectNodeAnyGetterFieldBean();
        bean.id = 1;
        bean.extra = MAPPER.createObjectNode();
        bean.extra.put("a", 2);
        bean.extra.put("b", "text");

        String json = MAPPER.writeValueAsString(bean);
        assertEquals(a2q("{'id':1,'a':2,'b':'text'}"), json);
    }

    // [databind#3604]: Allow ObjectNode method for @JsonAnyGetter
    @Test
    public void testAnyGetterWithObjectNodeMethod() throws Exception
    {
        ObjectNodeAnyGetterMethodBean bean = new ObjectNodeAnyGetterMethodBean();
        bean.id = 1;
        ObjectNode node = MAPPER.createObjectNode();
        node.put("x", true);
        node.put("y", 42);
        bean.setExtra(node);

        String json = MAPPER.writeValueAsString(bean);
        assertEquals(a2q("{'id':1,'x':true,'y':42}"), json);
    }

    // [databind#3604]: Null ObjectNode for @JsonAnyGetter should be fine
    @Test
    public void testAnyGetterWithNullObjectNode() throws Exception
    {
        ObjectNodeAnyGetterFieldBean bean = new ObjectNodeAnyGetterFieldBean();
        bean.id = 1;
        bean.extra = null;

        String json = MAPPER.writeValueAsString(bean);
        assertEquals(a2q("{'id':1}"), json);
    }

    // [databind#3604]: JsonNode (ObjectNode) field for @JsonAnyGetter
    @Test
    public void testAnyGetterWithJsonNodeField() throws Exception
    {
        JsonNodeAnyGetterFieldBean bean = new JsonNodeAnyGetterFieldBean();
        bean.id = 1;
        ObjectNode node = MAPPER.createObjectNode();
        node.put("name", "test");
        bean.extra = node;

        String json = MAPPER.writeValueAsString(bean);
        assertEquals(a2q("{'id':1,'name':'test'}"), json);
    }

    // [databind#3604]: Empty ObjectNode for @JsonAnyGetter should produce no extra properties
    @Test
    public void testAnyGetterWithEmptyObjectNode() throws Exception
    {
        ObjectNodeAnyGetterFieldBean bean = new ObjectNodeAnyGetterFieldBean();
        bean.id = 1;
        bean.extra = MAPPER.createObjectNode();

        String json = MAPPER.writeValueAsString(bean);
        assertEquals(a2q("{'id':1}"), json);
    }

    // [databind#3604]: Non-ObjectNode JsonNode (e.g. ArrayNode) should fail with clear error
    @Test
    public void testAnyGetterWithArrayNodeFails() throws Exception
    {
        JsonNodeAnyGetterFieldBean bean = new JsonNodeAnyGetterFieldBean();
        bean.id = 1;
        bean.extra = MAPPER.createArrayNode().add(1).add(2);

        DatabindException ex = assertThrows(DatabindException.class,
                () -> MAPPER.writeValueAsString(bean));
        assertThat(ex.getMessage()).contains("ObjectNode");
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
