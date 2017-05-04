package com.fasterxml.jackson.databind.filter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;

public class IgnorePropertiesDeser1575Test extends BaseMapTest
{
    static class Person {
        public String name;

        @JsonProperty("person_z") // renaming this to person_p works
        @JsonIgnoreProperties({"person_z"}) // renaming this to person_p works
//        public Set<Person> personZ;
        public Person personZ;
    }

    public void testIgnorePropDeser1575() throws Exception
    {
        String st = aposToQuotes("{ 'name': 'admin',\n"
//                + "    'person_z': [ { 'name': 'admin' } ]"
              + "    'person_z': { 'name': 'admin' }"
                + "}");

        ObjectMapper mapper = new ObjectMapper();
        Person result = mapper.readValue(st, Person.class);
        assertEquals("admin", result.name);
    }
}
