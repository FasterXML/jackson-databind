package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestUntypedPolymorphicSerialization extends TestCase {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = ClassA.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "a", value = ClassA.class),
            @JsonSubTypes.Type(name = "b", value = ClassB.class)
    })
    public static interface BaseInterface
    {
        public String getBase();
    }

    public static class ClassA implements BaseInterface {
        @Override
        public String getBase() {
            return "base-a";
        }

        public String getClassA() {
            return "class-a";
        }
    }

    public static class ClassB implements BaseInterface
    {
        @Override
        public String getBase() {
            return "base-a";
        }

        public String getClassB() {
            return "class-b";
        }
    }

    public static class Container {
        public List<Object> values = new ArrayList<Object>();
    }

    public void testTypedSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Container cont = new Container();
        cont.values.add(new ClassA());
        cont.values.add(new ClassB());

        String str = mapper.writeValueAsString(cont);

        // Check that the "type" property is output
        assertTrue(str.contains("\"type\":\"a\""));
        assertTrue(str.contains("\"type\":\"b\""));
    }
}
