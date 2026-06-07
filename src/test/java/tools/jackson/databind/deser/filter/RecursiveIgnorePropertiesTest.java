package tools.jackson.databind.deser.filter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static tools.jackson.databind.testutil.DatabindTestUtil.a2q;
import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class RecursiveIgnorePropertiesTest
{
    static class Person {
        public String name;

        @JsonProperty("person_z") // renaming this to person_p works
        @JsonIgnoreProperties({"person_z"}) // renaming this to person_p works
        public Person personZ;
    }

    // for [databind#1755]
    static class JackBase1755 {
        public String id;
    }

    static class JackExt extends JackBase1755 {
        public BigDecimal quantity;
        public String ignoreMe;

        @JsonIgnoreProperties({"ignoreMe"})
        public List<JackExt> linked;

        public List<KeyValue> metadata;
    }

    static class KeyValue {
        public String key;
        public String value;
    }

    // for [databind#4417]
    static class Item4417 {
        @JsonIgnoreProperties({ "whatever" })
        public List<Item4417> items;
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
        String st = """
                { "name": "admin",
                    "person_z": { "name": "wyatt" }\
                }
                """;
        Person result = MAPPER.readValue(st, Person.class);
        assertEquals("admin", result.name);
        assertNotNull(result.personZ);
        assertEquals("wyatt", result.personZ.name);
    }

    @Test
    public void testRecursiveWithCollectionDeser() throws Exception
    {
        String st = """
                { "name": "admin",
                    "person_z": [ { "name": "Foor" }, { "name" : "Bar" } ]\
                }
                """;
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

    // for [databind#1755]
    @Test
    public void testRecursiveIgnore1755() throws Exception
    {
        final String JSON = """
                {
                "id": "1",
                "quantity": 5,
                "ignoreMe": "yzx",
                "metadata": [
                           {
                              "key": "position",
                              "value": "2"
                          }
                       ],
                "linked": [
                     {
                         "id": "1",
                         "quantity": 5,
                         "ignoreMe": "yzx",
                         "metadata": [
                          {
                              "key": "position",
                             "value": "2"
                         }
                     ]
                   }
                  ]
                }
                """;
        JackExt value = MAPPER.readValue(JSON, JackExt.class);
        assertNotNull(value);
    }

    // for [databind#4417]
    @Test
    public void testRecursiveIgnore4417() throws Exception
    {
        Item4417 result = MAPPER.readValue(a2q("{'items': [{'items': []}]}"),
                Item4417.class);
        assertEquals(1, result.items.size(), 1);
    }
}
