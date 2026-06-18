package tools.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import static tools.jackson.databind.testutil.DatabindTestUtil.a2q;
import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class PropertyAliasTest
{
    static class AliasBean {
        @JsonAlias({ "nm", "Name" })
        public String name;

        int _xyz;

        int _a;

        @JsonCreator
        public AliasBean(@JsonProperty("a")
            @JsonAlias("A") int a) {
            _a = a;
        }

        @JsonAlias({ "Xyz" })
        public void setXyz(int x) {
            _xyz = x;
        }
    }

    static class AliasBean2378 {
        String partitionId;
        String _id;

        private AliasBean2378(boolean bogus, String partId, String userId) {
            partitionId = partId;
            _id = userId;
        }

        @JsonCreator
        public static AliasBean2378 create(@JsonProperty("partitionId") String partId,
                @JsonProperty("id") @JsonAlias("userId") String userId) {
            return new AliasBean2378(false, partId, userId);
        }
    }

    static class PolyWrapperForAlias {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.WRAPPER_ARRAY)
        @JsonSubTypes({
            @JsonSubTypes.Type(value = AliasBean.class,name = "ab"),
        })
        public Object value;

        protected PolyWrapperForAlias() { }
        public PolyWrapperForAlias(Object v) { value = v; }
    }

    // [databind#2669]
    static class Pojo2669 {
        @JsonAlias({"nick", "name"})
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    // [databind#6031]: a @JsonIgnore getter whose implicit name collides with a
    // creator parameter's @JsonAlias must not suppress that alias
    static class Bean6031 {
        @JsonProperty("newName")
        private final String value;

        @JsonCreator
        public Bean6031(@JsonProperty("newName") @JsonAlias("oldName") String value) {
            this.value = value;
        }

        public String getValue() { return value; }

        @JsonIgnore
        public String getOldName() { return value; }
    }

    // [databind#6031]: same as above, but exercising the record-update path
    // (`_deserializeRecordForUpdate`) where the creator-property ignore check lives
    // separately from the regular property-based path
    public record Record6031(@JsonProperty("newName") @JsonAlias("oldName") String value) {
        @JsonIgnore
        public String getOldName() { return value; }
    }

    // [databind#6031]: class-level @JsonIgnoreProperties is absolute and must keep
    // suppressing the name even when it coincides with a creator parameter's
    // @JsonAlias (unlike per-property @JsonIgnore, which yields to the alias)
    @JsonIgnoreProperties("oldName")
    static class ClassIgnoreBean6031 {
        @JsonProperty("newName")
        private final String value;

        @JsonCreator
        public ClassIgnoreBean6031(@JsonProperty("newName") @JsonAlias("oldName") String value) {
            this.value = value;
        }

        public String getValue() { return value; }
    }

    // [databind#6031]: multiple aliases on one creator param, and multiple creator
    // params each with an alias colliding with its own @JsonIgnore getter
    static class MultiAliasBean6031 {
        @JsonProperty("aName")
        private final String a;
        @JsonProperty("bName")
        private final String b;

        @JsonCreator
        public MultiAliasBean6031(
                @JsonProperty("aName") @JsonAlias({ "a1", "a2" }) String a,
                @JsonProperty("bName") @JsonAlias("b1") String b) {
            this.a = a;
            this.b = b;
        }

        public String getA() { return a; }
        public String getB() { return b; }

        @JsonIgnore
        public String getA1() { return a; }
        @JsonIgnore
        public String getA2() { return a; }
        @JsonIgnore
        public String getB1() { return b; }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1029]
    @Test
    public void testSimpleAliases() throws Exception
    {
        AliasBean bean;

        // first, one indicated by field annotation, set via field
        bean = MAPPER.readValue(a2q("{'Name':'Foobar','a':3,'xyz':37}"),
                AliasBean.class);
        assertEquals("Foobar", bean.name);
        assertEquals(3, bean._a);
        assertEquals(37, bean._xyz);

        // then method-bound one
        bean = MAPPER.readValue(a2q("{'name':'Foobar','a':3,'Xyz':37}"),
                AliasBean.class);
        assertEquals("Foobar", bean.name);
        assertEquals(3, bean._a);
        assertEquals(37, bean._xyz);

        // and finally, constructor-backed one
        bean = MAPPER.readValue(a2q("{'name':'Foobar','A':3,'xyz':37}"),
                AliasBean.class);
        assertEquals("Foobar", bean.name);
        assertEquals(3, bean._a);
        assertEquals(37, bean._xyz);
    }

    @Test
    public void testAliasWithPolymorphic() throws Exception
    {
        PolyWrapperForAlias value = MAPPER.readValue(a2q(
                "{'value': ['ab', {'nm' : 'Bob', 'A' : 17} ] }"
                ), PolyWrapperForAlias.class);
        assertNotNull(value.value);
        AliasBean bean = (AliasBean) value.value;
        assertEquals("Bob", bean.name);
        assertEquals(17, bean._a);
    }

    // [databind#2378]
    @Test
    public void testAliasInFactoryMethod() throws Exception
    {
        AliasBean2378 bean = MAPPER.readValue(a2q(
                "{'partitionId' : 'a', 'userId' : '123'}"
                ), AliasBean2378.class);
        assertEquals("a", bean.partitionId);
        assertEquals("123", bean._id);
    }

    // [databind#2669]
    @Test
    public void testCaseInsensitiveAliases() throws Exception {

        ObjectMapper mapper = JsonMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .build();

        String text = "{\"name\":\"test\"}";
        Pojo2669 pojo = mapper.readValue(text, Pojo2669.class);
        assertNotNull(pojo);
        assertEquals("test", pojo.getName());
    }

    static class FixedOrderAliasBean {
        @JsonAlias({"a", "b", "c"})
        public String value;
    }

    @Test
    public void testAliasDeserializedToLastMatchingKey_ascendingKeys() throws Exception {
        String ascendingOrderInput = a2q(
            "{\"a\": \"a-value\", " +
                "\"b\": \"b-value\", " +
                "\"c\": \"c-value\"}");

        FixedOrderAliasBean ascObj = MAPPER.readValue(ascendingOrderInput, FixedOrderAliasBean.class);
        assertEquals("c-value", ascObj.value);
    }

    @Test
    public void testAliasDeserializedToLastMatchingKey_descendingKeys() throws Exception {
        String descendingOrderInput = a2q(
            "{\"c\": \"c-value\", " +
                "\"b\": \"b-value\", " +
                "\"a\": \"a-value\"}");

        FixedOrderAliasBean descObj = MAPPER.readValue(descendingOrderInput, FixedOrderAliasBean.class);
        assertEquals("a-value", descObj.value);
    }

    static class AscendingOrderAliasBean {
        @JsonAlias({"a", "b", "c"})
        public String value;
    }

    @Test
    public void testAliasDeserializedToLastMatchingKey_ascendingAliases() throws Exception {
        String input = a2q(
                "{\"a\": \"a-value\", " +
                "\"b\": \"b-value\", " +
                "\"c\": \"c-value\"}");

        AscendingOrderAliasBean ascObj = MAPPER.readValue(input, AscendingOrderAliasBean.class);
        assertEquals("c-value", ascObj.value);
    }

    static class DescendingOrderAliasBean {
        @JsonAlias({"c", "b", "a"})
        public String value;
    }

    @Test
    public void testAliasDeserializedToLastMatchingKey_descendingAliases() throws Exception {
        String input = a2q(
            "{\"a\": \"a-value\", " +
                "\"b\": \"b-value\", " +
                "\"c\": \"c-value\"}");

        DescendingOrderAliasBean descObj = MAPPER.readValue(input, DescendingOrderAliasBean.class);
        assertEquals("c-value", descObj.value);
    }

    static class AliasTestBeanA {
        @JsonAlias({"fullName"})
        public String name;

        @JsonAlias({"fullName"})
        public String fullName;
    }

    @Test
    public void testAliasFallBackToField() throws Exception {
        AliasTestBeanA obj = MAPPER.readValue(a2q(
            "{\"fullName\": \"Faster Jackson\", \"name\":\"Jackson\"}"
        ), AliasTestBeanA.class);

        assertEquals("Jackson", obj.name);
        assertEquals("Faster Jackson", obj.fullName);
    }

    // [databind#6031]
    @Test
    public void testAliasOnCreatorWithIgnoredGetter() throws Exception {
        Bean6031 result = MAPPER.readValue(a2q("{'oldName':'hello'}"), Bean6031.class);
        assertEquals("hello", result.getValue());

        // and the primary name must still work
        result = MAPPER.readValue(a2q("{'newName':'hello'}"), Bean6031.class);
        assertEquals("hello", result.getValue());
    }

    // [databind#6031]: same defect on the record-update path (`_deserializeRecordForUpdate`)
    @Test
    public void testAliasOnRecordUpdateWithIgnoredGetter() throws Exception {
        Record6031 orig = new Record6031("orig");
        Record6031 result = MAPPER.readerForUpdating(orig)
                .readValue(a2q("{'oldName':'hello'}"));
        assertEquals("hello", result.value());

        // and the primary name must still work
        result = MAPPER.readerForUpdating(orig)
                .readValue(a2q("{'newName':'hello'}"));
        assertEquals("hello", result.value());
    }

    // [databind#6031]: the @JsonIgnore getter whose implicit name is the alias
    // ("oldName") must not leak into serialization output
    @Test
    public void testNoAliasNameInSerialization() throws Exception {
        assertEquals(a2q("{'newName':'hello'}"),
                MAPPER.writeValueAsString(new Bean6031("hello")));
        assertEquals(a2q("{'newName':'hello'}"),
                MAPPER.writeValueAsString(new Record6031("hello")));
    }

    // [databind#6031]: class-level @JsonIgnoreProperties stays absolute; unlike a
    // per-property @JsonIgnore it must keep suppressing the name even when it is a
    // creator parameter's alias. So "oldName" is dropped (value stays null) while
    // the primary name still binds.
    @Test
    public void testClassLevelIgnoreNotRescuedByAlias() throws Exception {
        ClassIgnoreBean6031 result = MAPPER.readValue(a2q("{'oldName':'hello'}"),
                ClassIgnoreBean6031.class);
        assertNull(result.getValue());

        result = MAPPER.readValue(a2q("{'newName':'hello'}"), ClassIgnoreBean6031.class);
        assertEquals("hello", result.getValue());
    }

    // [databind#6031]: every alias must be rescued — across multiple aliases on a
    // single creator param and across multiple creator params
    @Test
    public void testMultipleAliasesWithIgnoredGetters() throws Exception {
        // first alias of the multi-alias param
        MultiAliasBean6031 result = MAPPER.readValue(a2q("{'a1':'AA','b1':'BB'}"),
                MultiAliasBean6031.class);
        assertEquals("AA", result.getA());
        assertEquals("BB", result.getB());

        // second alias of the same param
        result = MAPPER.readValue(a2q("{'a2':'AA','b1':'BB'}"), MultiAliasBean6031.class);
        assertEquals("AA", result.getA());
        assertEquals("BB", result.getB());

        // and primary names still work
        result = MAPPER.readValue(a2q("{'aName':'AA','bName':'BB'}"), MultiAliasBean6031.class);
        assertEquals("AA", result.getA());
        assertEquals("BB", result.getB());
    }
}
