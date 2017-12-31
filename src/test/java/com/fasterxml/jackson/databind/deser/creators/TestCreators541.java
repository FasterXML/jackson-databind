package com.fasterxml.jackson.databind.deser.creators;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;

public class TestCreators541 extends BaseMapTest
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
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testCreator541() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(MapperFeature.USE_GETTERS_AS_SETTERS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);  

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
}
