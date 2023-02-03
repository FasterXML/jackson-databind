package com.fasterxml.jackson.databind.deser.creators;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

// Misc Creator tests, part 3
public class TestCreators3 extends BaseMapTest
{
    static final class Foo {

        @JsonProperty("foo")
        protected Map<Integer, Bar> foo;
        @JsonProperty("anumber")
        protected long anumber;

        public Foo() {
            anumber = 0;
        }

        public Map<Integer, Bar> getFoo() {
            return foo;
        }

        public long getAnumber() {
            return anumber;
        }
    }

    static final class Bar {

        private final long p;
        private final List<String> stuff;

        @JsonCreator
        public Bar(@JsonProperty("p") long p, @JsonProperty("stuff") List<String> stuff) {
            this.p = p;
            this.stuff = stuff;
        }

        @JsonProperty("s")
        public List<String> getStuff() {
            return stuff;
        }

        @JsonProperty("stuff")
        private List<String> getStuffDeprecated() {
            return stuff;
        }

        public long getP() {
            return p;
        }
    }

    // [databind#421]

    static class MultiCtor
    {
        protected String _a, _b;

        private MultiCtor() { }
        private MultiCtor(String a, String b, Boolean c) {
            if (c == null) {
                throw new RuntimeException("Wrong factory!");
            }
            _a = a;
            _b = b;
        }

        @JsonCreator
        static MultiCtor factory(@JsonProperty("a") String a, @JsonProperty("b") String b) {
            return new MultiCtor(a, b, Boolean.TRUE);
        }
    }

    @SuppressWarnings("serial")
    static class MyParamIntrospector extends JacksonAnnotationIntrospector
    {
        @Override
        public String findImplicitPropertyName(AnnotatedMember param) {
            if (param instanceof AnnotatedParameter) {
                AnnotatedParameter ap = (AnnotatedParameter) param;
                switch (ap.getIndex()) {
                case 0: return "a";
                case 1: return "b";
                case 2: return "c";
                default:
                    return "param"+ap.getIndex();
                }
            }
            return super.findImplicitPropertyName(param);
        }
    }

    // [databind#1853]
    public static class Product1853 {
        String name;

        public Object other, errors;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Product1853(@JsonProperty("name") String name) {
            this.name = "PROP:" + name;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Product1853 from(String name){
            return new Product1853(false, "DELEG:"+name);
        }

        Product1853(boolean bogus, String name) {
            this.name = name;
        }

        @JsonValue
        public String getName(){
            return name;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testCreator541() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(
                MapperFeature.AUTO_DETECT_CREATORS,
                MapperFeature.AUTO_DETECT_FIELDS,
                MapperFeature.AUTO_DETECT_GETTERS,
                MapperFeature.AUTO_DETECT_IS_GETTERS,
                MapperFeature.AUTO_DETECT_SETTERS,
                MapperFeature.USE_GETTERS_AS_SETTERS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
        .build();

        final String JSON = "{\n"
                + "    \"foo\": {\n"
                + "        \"0\": {\n"
                + "            \"p\": 0,\n"
                + "            \"stuff\": [\n"
                + "              \"a\", \"b\" \n"
                + "            ]   \n"
                + "        },\n"
                + "        \"1\": {\n"
                + "            \"p\": 1000,\n"
                + "            \"stuff\": [\n"
                + "              \"c\", \"d\" \n"
                + "            ]   \n"
                + "        },\n"
                + "        \"2\": {\n"
                + "            \"p\": 2000,\n"
                + "            \"stuff\": [\n"
                + "            ]   \n"
                + "        }\n"
                + "    },\n"
                + "    \"anumber\": 25385874\n"
                + "}";

        Foo obj = mapper.readValue(JSON, Foo.class);
        assertNotNull(obj);
        assertNotNull(obj.foo);
        assertEquals(3, obj.foo.size());
        assertEquals(25385874L, obj.getAnumber());
    }

    // [databind#421]
    public void testMultiCtor421() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        mapper.setAnnotationIntrospector(new MyParamIntrospector());

        MultiCtor bean = mapper.readValue(a2q("{'a':'123','b':'foo'}"), MultiCtor.class);
        assertNotNull(bean);
        assertEquals("123", bean._a);
        assertEquals("foo", bean._b);
    }

    // [databind#1853]
    public void testSerialization() throws Exception {
        assertEquals(q("testProduct"),
                MAPPER.writeValueAsString(new Product1853(false, "testProduct")));
    }

    public void testDeserializationFromObject() throws Exception {
        final String EXAMPLE_DATA = "{\"name\":\"dummy\",\"other\":{},\"errors\":{}}";
        assertEquals("PROP:dummy", MAPPER.readValue(EXAMPLE_DATA, Product1853.class).getName());
    }

    public void testDeserializationFromString() throws Exception {
        assertEquals("DELEG:testProduct",
                MAPPER.readValue(q("testProduct"), Product1853.class).getName());
    }

    public void testDeserializationFromWrappedString() throws Exception {
        Product1853 result = MAPPER.readerFor(Product1853.class)
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue("[\"testProduct\"]");
        assertEquals("DELEG:testProduct", result.getName());
    }
}

