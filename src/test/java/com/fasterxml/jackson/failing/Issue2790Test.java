package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.*;

public class Issue2790Test extends BaseMapTest
{
    class SomeNonDeserialisedClass {
        public void setX(String foo) { }
        public void setX(Object foo) { }
        public void setX(java.util.List<String> foo) { }
    }

    static class TestModel {

        private String name;

        public String getName() {
            return name;
        }

        public TestModel setName(String name) {
            this.name = name;
            return this;
        }

        static String format(SomeNonDeserialisedClass action) {
            return null;
        }
    }

    public void testIssue2790() throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.readValue("{\"name\":\"test\"}", TestModel.class);
    }
}
