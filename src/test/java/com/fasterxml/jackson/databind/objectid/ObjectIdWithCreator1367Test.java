package com.fasterxml.jackson.databind.objectid;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.beans.ConstructorProperties;

public class ObjectIdWithCreator1367Test
    extends BaseMapTest
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
//            resolver = SimpleObjectIdResolver.class)
    public static class A {
        String id;
        String name;

        public A() { }

        @ConstructorProperties({"id", "name"})
        public A(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }

    public void testObjectIdWithCreator1367() throws Exception
    {
        A a = new A("123", "A");

        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(a);
        A deser = om.readValue(json, A.class);
        assertEquals(a.name, deser.name);
    }
}
