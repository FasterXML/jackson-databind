package com.fasterxml.jackson.databind.deser.filter;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class IgnorePropertyOnDeserTest extends BaseMapTest
{
    // [databind#426]
    @JsonIgnoreProperties({ "userId" })
    static class User {
        public String firstName;
        Integer userId;

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(CharSequence id) {
            userId = Integer.valueOf(id.toString());
        }

        public void setUserId(Integer v) {
            this.userId = v;
        }

        public void setUserId(User u) {
            // bogus
        }

        public void setUserId(boolean b) {
            // bogus
        }
    }

    // [databind#1217]
    static class IgnoreObject {
        public int x = 1;
        public int y = 2;
    }

    final static class TestIgnoreObject {
        @JsonIgnoreProperties({ "x" })
        public IgnoreObject obj;

        @JsonIgnoreProperties({ "y" })
        public IgnoreObject obj2;
    }

    // [databind#1595]
    @JsonIgnoreProperties(value = {"name"}, allowSetters = true)
    @JsonPropertyOrder(alphabetic=true)
    static class Simple1595 {
        private int id;
        private String name;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // [databind#2627]

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MyPojoValue {
        @JsonIgnoreProperties(ignoreUnknown = true)
        MyPojo2627 value;

        public MyPojo2627 getValue() {
            return value;
        }
    }

    static class MyPojo2627 {
        public String name;
    }

    /*
    /****************************************************************
    /* Unit tests
    /****************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#426]
    public void testIssue426() throws Exception
    {
        final String JSON = a2q("{'userId': 9, 'firstName': 'Mike' }");
        User result = MAPPER.readerFor(User.class).readValue(JSON);
        assertNotNull(result);
        assertEquals("Mike", result.firstName);
        assertNull(result.userId);
    }

    // [databind#1217]
    public void testIgnoreOnProperty1217() throws Exception
    {
        TestIgnoreObject result = MAPPER.readValue(
                a2q("{'obj':{'x': 10, 'y': 20}, 'obj2':{'x': 10, 'y': 20}}"),
                TestIgnoreObject.class);
        assertEquals(20, result.obj.y);
        assertEquals(10, result.obj2.x);

        assertEquals(1, result.obj.x);
        assertEquals(2, result.obj2.y);

        TestIgnoreObject result1 = MAPPER.readValue(
                  a2q("{'obj':{'x': 20, 'y': 30}, 'obj2':{'x': 20, 'y': 40}}"),
                  TestIgnoreObject.class);
        assertEquals(1, result1.obj.x);
        assertEquals(30, result1.obj.y);

        assertEquals(20, result1.obj2.x);
        assertEquals(2, result1.obj2.y);
    }

    // [databind#1217]
    public void testIgnoreViaConfigOverride1217() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
            .withConfigOverride(Point.class,
                cfg -> cfg.setIgnorals(JsonIgnoreProperties.Value.forIgnoredProperties("y")))
            .build();
        Point p = mapper.readValue(a2q("{'x':1,'y':2}"), Point.class);
        // bind 'x', but ignore 'y'
        assertEquals(1, p.x);
        assertEquals(0, p.y);
    }

    // [databind#3721]
    public void testIgnoreUnknownViaConfigOverride() throws Exception
    {
        final String DOC = a2q("{'x':2,'foobar':3}");

        // First, fail without overrides
        try {
            MAPPER.readValue(DOC, Point.class);
            fail("Should not pass");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "foobar"); // message varies between 2.x and 3.x
        }

        // But pass with specific class override:
        ObjectMapper mapper = JsonMapper.builder()
            .withConfigOverride(Point.class,
                cfg -> cfg.setIgnorals(JsonIgnoreProperties.Value.forIgnoreUnknown(true)))
            .build();
        Point p = mapper.readValue(DOC, Point.class);
        assertEquals(2, p.x);

        // 13-Jan-2023, tatu: Alas, no global defaulting yet!
    }

    // [databind#1595]
    public void testIgnoreGetterNotSetter1595() throws Exception
    {
        Simple1595 config = new Simple1595();
        config.setId(123);
        config.setName("jack");
        String json = MAPPER.writeValueAsString(config);
        assertEquals(a2q("{'id':123}"), json);
        Simple1595 des = MAPPER.readValue(a2q("{'id':123,'name':'jack'}"), Simple1595.class);
        assertEquals("jack", des.getName());
    }

    // [databind#2627]
    public void testIgnoreUnknownOnField() throws IOException
    {
        String json = "{\"value\": {\"name\": \"my_name\", \"extra\": \"val\"}, \"type\":\"Json\"}";
        MyPojoValue value = MAPPER.readValue(json, MyPojoValue.class);
        assertNotNull(value);
        assertNotNull(value.getValue());
        assertEquals("my_name", value.getValue().name);
    }
}

