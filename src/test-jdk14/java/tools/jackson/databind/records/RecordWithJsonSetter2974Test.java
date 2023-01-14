<<<<<<< HEAD:src/test-jdk14/java/tools/jackson/databind/failing/RecordWithJsonSetter2974Test.java
package tools.jackson.databind.failing;
=======
package com.fasterxml.jackson.databind.records;
>>>>>>> 2.15:src/test-jdk14/java/tools/jackson/databind/records/RecordWithJsonSetter2974Test.java

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.exc.InvalidNullException;

public class RecordWithJsonSetter2974Test extends BaseMapTest
{
    record RecordWithNonNullDefs(@JsonSetter(nulls=Nulls.AS_EMPTY) List<String> names,
            @JsonSetter(nulls=Nulls.FAIL) Map<String, Integer> agesByNames)
    { }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, Record type introspection
    /**********************************************************************
     */

    // [databind#2974]
    public void testDeserializeWithNullAsEmpty() throws Exception
    {
        final ObjectReader r = MAPPER.readerFor(RecordWithNonNullDefs.class);
        // First, regular case
        RecordWithNonNullDefs value = r.readValue(a2q(
"{'names':['bob'],'agesByNames':{'bob':39}}"));
        assertEquals(1, value.names().size());
        assertEquals("bob", value.names().get(0));
        assertEquals(1, value.agesByNames().size());
        assertEquals(Integer.valueOf(39), value.agesByNames().get("bob"));

        // Then leave out list
        value = r.readValue(a2q("{'agesByNames':{'bob':42}}"));
        assertNotNull(value.names());
        assertEquals(0, value.names().size());
        assertNotNull(value.agesByNames());
        assertEquals(1, value.agesByNames().size());
        assertEquals(Integer.valueOf(42), value.agesByNames().get("bob"));
    }

    // [databind#2974]
    public void testDeserializeWithFailForNull() throws Exception
    {
        final ObjectReader r = MAPPER.readerFor(RecordWithNonNullDefs.class);
        // First, regular case
        // But attempting to leave out Map ought to fail
        try {
            /*RecordWithNonNullDefs value =*/ r.readValue(a2q("{'names':['bob']}"));
            fail("Should not pass with missing/null 'agesByNames'");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"agesByNames\"");
        }
    }
}
