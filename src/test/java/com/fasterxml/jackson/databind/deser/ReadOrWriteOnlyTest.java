package com.fasterxml.jackson.databind.deser;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;

public class ReadOrWriteOnlyTest extends BaseMapTest
{
    // for [databind#935], verify read/write-only cases
    static class ReadXWriteY {
        @JsonProperty(access=JsonProperty.Access.READ_ONLY)
        public int x = 1;

        @JsonProperty(access=JsonProperty.Access.WRITE_ONLY)
        public int y = 2;

        public void setX(int x) {
            throw new Error("Should NOT set x");
        }

        public int getY() {
            throw new Error("Should NOT get y");
        }
    }

    public static class Pojo935
    {
        private String firstName = "Foo";
        private String lastName = "Bar";

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        public String getFullName() {
            return firstName + " " + lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String n) {
            firstName = n;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String n) {
            lastName = n;
        }
    }

    // for [databind#1345], emulate way Lombok embellishes classes
    static class Foo1345 {
        @JsonProperty(access=JsonProperty.Access.READ_ONLY)
        public String id;
        public String name;

        @ConstructorProperties({ "id", "name" })
        public Foo1345(String id, String name) {
            this.id = id;
            this.name = name;
        }

        protected Foo1345() { }
    }

    // [databind#1382]
    static class Foo1382 {
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        private List<Long> list = new ArrayList<>();

        List<Long> getList() {
            return list;
        }

        public Foo1382 setList(List<Long> list) {
            this.list = list;
            return this;
        }
    }

    // [databind#1805]
    static class UserWithReadOnly1805 {
        public String name;

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        public List<String> getRoles() {
            return Arrays.asList("admin", "monitor");
        }
    }

    // [databind#1805]
    @JsonIgnoreProperties(value={ "roles" }, allowGetters=true)
    static class UserAllowGetters1805 {
        public String name;

        public List<String> getRoles() {
            return Arrays.asList("admin", "monitor");
        }
    }

    // [databind#2779]: ignorable property renaming
    static class Bean2779 {
        String works;

        @JsonProperty(value = "t", access = JsonProperty.Access.READ_ONLY)
        public String getDoesntWork() {
            return "pleaseFixThisBug";
        }

        public String getWorks() {
            return works;
        }

        public void setWorks(String works) {
            this.works = works;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    // [databind#935]
    public void testReadOnlyAndWriteOnly() throws Exception
    {
        String json = MAPPER.writeValueAsString(new ReadXWriteY());
        assertEquals("{\"x\":1}", json);

        ReadXWriteY result = MAPPER.readValue("{\"x\":5, \"y\":6}", ReadXWriteY.class);
        assertNotNull(result);
        assertEquals(1, result.x);
        assertEquals(6, result.y);
    }

    public void testReadOnly935() throws Exception
    {
        String json = MAPPER.writeValueAsString(new Pojo935());
        Pojo935 result = MAPPER.readValue(json, Pojo935.class);
        assertNotNull(result);
    }

    // [databind#1345]
    public void testReadOnly1345() throws Exception
    {
        Foo1345 result = MAPPER.readValue("{\"name\":\"test\"}", Foo1345.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNull(result.id);
    }

    // [databind#1382]
    public void testReadOnly1382() throws Exception
    {
        String payload = "{\"list\":[1,2,3,4]}";
        Foo1382 foo = MAPPER.readValue(payload, Foo1382.class);
        assertTrue("List should be empty", foo.getList().isEmpty());
    }

    // [databind#1805]
    public void testViaReadOnly() throws Exception {
        UserWithReadOnly1805 user = new UserWithReadOnly1805();
        user.name = "foo";
        String json = MAPPER.writeValueAsString(user);
        UserWithReadOnly1805 result = MAPPER.readValue(json, UserWithReadOnly1805.class);
        assertNotNull(result);
    }

    // [databind#1805]
    public void testUsingAllowGetters() throws Exception {
        UserAllowGetters1805 user = new UserAllowGetters1805();
        user.name = "foo";
        String json = MAPPER.writeValueAsString(user);
        assertTrue(json.contains("roles"));
        UserAllowGetters1805 result = MAPPER.readValue(json, UserAllowGetters1805.class);
        assertNotNull(result);
    }

    // [databind#2779]: ignorable property renaming
    public void testIssue2779() throws Exception
    {
        Bean2779 bean = new Bean2779();
        bean.setWorks("works");

        String json = MAPPER.writeValueAsString(bean);
        Bean2779 newBean = MAPPER.readValue(json, Bean2779.class);
        assertNotNull(newBean);
    }
}
