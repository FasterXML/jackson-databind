package com.fasterxml.jackson.databind.deser;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

public class ReadOnlyDeser1805Test extends BaseMapTest
{
    static class Foo {
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        private List<Long> list = new ArrayList<>();

        List<Long> getList() {
            return list;
        }

        public Foo setList(List<Long> list) {
            this.list = list;
            return this;
        }
    }

    // [databind#1805]
    static class UserWithReadOnly {
        public String name;

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        public List<String> getRoles() {
            return Arrays.asList("admin", "monitor");
        }
    }

    // [databind#1805]
    @JsonIgnoreProperties(value={ "roles" }, allowGetters=true)
    static class UserAllowGetters {
        public String name;

        public List<String> getRoles() {
            return Arrays.asList("admin", "monitor");
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newObjectMapper();

    public void testReadOnly1382() throws Exception
    {
        String payload = "{\"list\":[1,2,3,4]}";
        Foo foo = MAPPER.readValue(payload, Foo.class);
        assertTrue("List should be empty", foo.getList().isEmpty());
    }

    // [databind#1805]
    public void testViaReadOnly() throws Exception {
        UserWithReadOnly user = new UserWithReadOnly();
        user.name = "foo";
        String json = MAPPER.writeValueAsString(user);
        UserWithReadOnly result = MAPPER.readValue(json, UserWithReadOnly.class);
        assertNotNull(result);
    }

    // [databind#1805]
    public void testUsingAllowGetters() throws Exception {
        UserAllowGetters user = new UserAllowGetters();
        user.name = "foo";
        String json = MAPPER.writeValueAsString(user);
        assertTrue(json.contains("roles"));
        UserAllowGetters result = MAPPER.readValue(json, UserAllowGetters.class);
        assertNotNull(result);
    }
}
