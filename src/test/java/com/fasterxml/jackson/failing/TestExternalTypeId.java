package com.fasterxml.jackson.failing;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class TestExternalTypeId extends BaseMapTest
{
	@SuppressWarnings("unused")
	public void testTypes() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final Point _date = new Point(new Date());
        final Point _integer = new Point(12231321);
        final Point _boolean = new Point(Boolean.TRUE);
        final Point _long = new Point(1234L);

        final Point _pojo = new Point(new Pojo(1));
        final String s_date = mapper.writeValueAsString(_date);
        final String s_integer = mapper.writeValueAsString(_integer);

//System.err.println("Int -> "+s_integer);   
    
        final String s_boolean = mapper.writeValueAsString(_boolean);
        final String s_long = mapper.writeValueAsString(_long);
        final String s_pojo = mapper.writeValueAsString(_pojo);

        final Point d_date = mapper.readValue(s_date, Point.class);
        final Point d_long = mapper.readValue(s_long, Point.class);
        final Point d_pojo = mapper.readValue(s_pojo, Point.class);
        final Point d_integer = mapper.readValue(s_integer, Point.class);
        final Point d_boolean = mapper.readValue(s_boolean, Point.class);
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
    static class Point {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        property = "t",
        visible = true,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        defaultImpl = String.class)
        @JsonSubTypes({
        @JsonSubTypes.Type(value = Date.class, name = "date"),
        @JsonSubTypes.Type(value = Integer.class, name = "int"),
        @JsonSubTypes.Type(value = Long.class, name = "long"),
        @JsonSubTypes.Type(value = Boolean.class, name = "bool"),
        @JsonSubTypes.Type(value = Pojo.class, name = "pojo"),
        @JsonSubTypes.Type(value = String.class, name = "")
        })
        private final Object v;
    
        @JsonCreator
        public Point(@JsonProperty("v") Object v) {
            this.v = v;
        }
    
        public Object getValue() {
            return v;
        }
    }
     

    static class Pojo {
        public final int p;

        @JsonCreator
        private Pojo(@JsonProperty("p") int p) {
            this.p = p;
        }
    }

    // [Issue#222]
    static class Issue222Bean
    {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                property = "type",
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
        public Issue222BeanB value;

        public String type = "foo";
        
        public Issue222Bean() { }
        public Issue222Bean(int v) {
            value = new Issue222BeanB(v);
        }
    }

    static class Issue222BeanB
    {
        public int x;
        
        public Issue222BeanB() { }
        public Issue222BeanB(int value) { x = value; }
    }

    public void testIssue222() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        Issue222Bean input = new Issue222Bean(13);
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"value\":{\"x\":13},\"type\":\"foo\"}", json);
    }
    

}
