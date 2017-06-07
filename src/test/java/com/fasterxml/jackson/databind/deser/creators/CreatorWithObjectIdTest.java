package com.fasterxml.jackson.databind.deser.creators;

import java.beans.ConstructorProperties;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.*;

// for [databind#1367]
public class CreatorWithObjectIdTest
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

    public void testObjectIdWithCreator() throws Exception
    {
        A a = new A("123", "A");

        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(a);
        A deser = om.readValue(json, A.class);
        assertEquals(a.name, deser.name);
    }
}
