package com.fasterxml.jackson.databind.deser.filter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IgnorePropertyOnDeserTest extends BaseMapTest
{
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

    /*
    /****************************************************************
    /* Unit tests
    /****************************************************************
     */

    private final ObjectMapper MAPPER = newObjectMapper();

    // [databind#1217]
    public void testIgnoreOnProperty1217() throws Exception
    {
        TestIgnoreObject result = MAPPER.readValue(
                aposToQuotes("{'obj':{'x': 10, 'y': 20}, 'obj2':{'x': 10, 'y': 20}}"),
                TestIgnoreObject.class);
        assertEquals(20, result.obj.y);
        assertEquals(10, result.obj2.x);

        assertEquals(1, result.obj.x);
        assertEquals(2, result.obj2.y);

        TestIgnoreObject result1 = MAPPER.readValue(
                  aposToQuotes("{'obj':{'x': 20, 'y': 30}, 'obj2':{'x': 20, 'y': 40}}"),
                  TestIgnoreObject.class);
        assertEquals(1, result1.obj.x);
        assertEquals(30, result1.obj.y);
       
        assertEquals(20, result1.obj2.x);
        assertEquals(2, result1.obj2.y);
    }

    public void testIgnoreViaConfigOverride1217() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(Point.class)
            .setIgnorals(JsonIgnoreProperties.Value.forIgnoredProperties("y"));
        Point p = mapper.readValue(aposToQuotes("{'x':1,'y':2}"), Point.class);
        // bind 'x', but ignore 'y'
        assertEquals(1, p.x);
        assertEquals(0, p.y);
    }

    // [databind#1595]
    public void testIgnoreGetterNotSetter1595() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Simple1595 config = new Simple1595();
        config.setId(123);
        config.setName("jack");
        String json = mapper.writeValueAsString(config);
        assertEquals(aposToQuotes("{'id':123}"), json);
        Simple1595 des = mapper.readValue(aposToQuotes("{'id':123,'name':'jack'}"), Simple1595.class);
        assertEquals("jack", des.getName());
    }
}
