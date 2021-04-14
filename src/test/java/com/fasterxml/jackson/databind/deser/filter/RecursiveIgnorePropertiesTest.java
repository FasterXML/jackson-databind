package com.fasterxml.jackson.databind.deser.filter;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;

public class RecursiveIgnorePropertiesTest extends BaseMapTest
{
    static class Person {
        public String name;

        @JsonProperty("person_z") // renaming this to person_p works
        @JsonIgnoreProperties({"person_z"}) // renaming this to person_p works
        public Person personZ;
    }

    static class Persons {
        public String name;

        @JsonProperty("person_z") // renaming this to person_p works
        @JsonIgnoreProperties({"person_z"}) // renaming this to person_p works
        public Set<Persons> personZ;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testRecursiveForDeser() throws Exception
    {
        String st = a2q("{ 'name': 'admin',\n"
                + "    'person_z': { 'name': 'wyatt' }"
                + "}");
        Person result = MAPPER.readValue(st, Person.class);
        assertEquals("admin", result.name);
        assertNotNull(result.personZ);
        assertEquals("wyatt", result.personZ.name);
    }

    public void testRecursiveWithCollectionDeser() throws Exception
    {
        String st = a2q("{ 'name': 'admin',\n"
                + "    'person_z': [ { 'name': 'Foor' }, { 'name' : 'Bar' } ]"
                + "}");
        Persons result = MAPPER.readValue(st, Persons.class);
        assertEquals("admin", result.name);
        assertNotNull(result.personZ);
        assertEquals(2, result.personZ.size());
    }

    public void testRecursiveForSer() throws Exception
    {
        Person input = new Person();
        input.name = "Bob";
        Person p2 = new Person();
        p2.name = "Bill";
        input.personZ = p2;
        p2.personZ = input;

        String json = MAPPER.writeValueAsString(input);
        assertNotNull(json);
    }
}
