package com.fasterxml.jackson.databind.deser.filter;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.a2q;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class RecursiveIgnorePropertiesTest
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

    @Test
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

    @Test
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

    @Test
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
