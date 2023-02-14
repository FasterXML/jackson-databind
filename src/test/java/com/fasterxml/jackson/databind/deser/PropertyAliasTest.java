package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class PropertyAliasTest extends BaseMapTest
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
    public void testAliasInFactoryMethod() throws Exception
    {
        AliasBean2378 bean = MAPPER.readValue(a2q(
                "{'partitionId' : 'a', 'userId' : '123'}"
                ), AliasBean2378.class);
        assertEquals("a", bean.partitionId);
        assertEquals("123", bean._id);
    }

    // [databind#2669]
    public void testCaseInsensitiveAliases() throws Exception {

        ObjectMapper mapper = JsonMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .build();

        String text = "{\"name\":\"test\"}";
        Pojo2669 pojo = mapper.readValue(text, Pojo2669.class);
        assertNotNull(pojo);
        assertEquals("test", pojo.getName());
    }
}
