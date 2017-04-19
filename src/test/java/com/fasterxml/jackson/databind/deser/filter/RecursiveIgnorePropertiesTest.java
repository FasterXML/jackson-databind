package com.fasterxml.jackson.databind.deser.filter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;

public class RecursiveIgnorePropertiesTest extends BaseMapTest
{
    static class Person {
        public String name;

        @JsonProperty("person_z") // renaming this to person_p works
        @JsonIgnoreProperties({"person_z"}) // renaming this to person_p works
//        public Set<Person> personZ;
        public Person personZ;
    }

    public void testRecursiveForDeser() throws Exception
    {
        String st = aposToQuotes("{ 'name': 'admin',\n"
//                + "    'person_z': [ { 'name': 'admin' } ]"
              + "    'person_z': { 'name': 'admin' }"
                + "}");

        ObjectMapper mapper = newObjectMapper();
        Person result = mapper.readValue(st, Person.class);
        assertEquals("admin", result.name);
    }

    public void testRecursiveForSer() throws Exception
    {
        ObjectMapper mapper = newObjectMapper();
        Person input = new Person();
        input.name = "Bob";
        Person p2 = new Person();
        p2.name = "Bill";
        input.personZ = p2;
        p2.personZ = input;

        String json = mapper.writeValueAsString(input);
        assertNotNull(json);
    }
}
