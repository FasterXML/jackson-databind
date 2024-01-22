package com.fasterxml.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.a2q;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

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
}
