package com.fasterxml.jackson.databind.deser;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Test for [JACKSON-429]
 */
public class TestIgnoredTypes extends BaseMapTest
{
    /*
    /**********************************************************
    /* Annotated helper classes
    /**********************************************************
     */
    
    @JsonIgnoreType
    class IgnoredType { // note: non-static, can't be deserializer
        public IgnoredType(IgnoredType src) { }
    }

    @JsonIgnoreType(false)
    static class NonIgnoredType
    {
        public int value = 13;
        public IgnoredType ignored;
    }

    // // And test for mix-in annotations
    
    @JsonIgnoreType
    static class Person {
        public String name;
        public Person() { }
        public Person(String name) {
            this.name = name;
        }
    }

    static class PersonWrapper {
        public int value = 1;
        public Person person = new Person("Foo");
    }
    
    @JsonIgnoreType
    static abstract class PersonMixin {
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testIgnoredType() throws Exception
    {
        // First: should be ok in general, even though couldn't build deserializer (due to non-static inner class):
        ObjectMapper mapper = new ObjectMapper();
        NonIgnoredType bean = mapper.readValue("{\"value\":13}", NonIgnoredType.class);
        assertNotNull(bean);
        assertEquals(13, bean.value);

        // And also ok to see something with that value; will just get ignored
        bean = mapper.readValue("{ \"ignored\":[1,2,{}], \"value\":9 }", NonIgnoredType.class);
        assertNotNull(bean);
        assertEquals(9, bean.value);
    }

    public void testSingleWithMixins() throws Exception {
        SimpleModule module = new SimpleModule();
        module.setMixInAnnotation(Person.class, PersonMixin.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        PersonWrapper input = new PersonWrapper();
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"value\":1}", json);
    }
    
    public void testListWithMixins() throws Exception {
        SimpleModule module = new SimpleModule();
        module.setMixInAnnotation(Person.class, PersonMixin.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        List<Person> persons = new ArrayList<Person>();
        persons.add(new Person("Bob"));
        String json = mapper.writeValueAsString(persons);
        assertEquals("[{\"name\":\"Bob\"}]", json);
    }
}
