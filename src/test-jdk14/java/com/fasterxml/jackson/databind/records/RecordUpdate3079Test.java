package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.databind.*;

public class RecordUpdate3079Test extends BaseMapTest
{
    record IdNameRecord(int id, String name) { }

    static class IdNameWrapper {
        public IdNameRecord value;

        protected IdNameWrapper() { }
        public IdNameWrapper(IdNameRecord v) { value = v; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();
    
    // TODO Ignore this test case
    /**
     * This test no longer works as of JDK 15 for records. Maybe Unsafe will work?
     * 
     * https://medium.com/@atdsqdjyfkyziqkezu/java-15-breaks-deserialization-of-records-902fcc81253d
     * https://stackoverflow.com/questions/61141836/change-static-final-field-in-java-12
     */
    // [databind#3079]: Should be able to Record value directly
    // [databind#3102]: fails on JDK 16 which finally blocks mutation
    // of Record fields.
    // public void testDirectRecordUpdate() throws Exception
    // {
    //     IdNameRecord orig = new IdNameRecord(123, "Bob");
    //     IdNameRecord result = MAPPER.updateValue(orig,
    //             Collections.singletonMap("id", 137));
    //     assertNotNull(result);
    //     assertEquals(137, result.id());
    //     assertEquals("Bob", result.name());
    //     assertNotSame(orig, result);
    // }

    // [databind#3079]: also: should be able to Record valued property
    public void testRecordAsPropertyUpdate() throws Exception
    {
        IdNameRecord origRecord = new IdNameRecord(123, "Bob");
        IdNameWrapper orig = new IdNameWrapper(origRecord);

        IdNameWrapper delta = new IdNameWrapper(new IdNameRecord(200, "Gary"));
        IdNameWrapper result = MAPPER.updateValue(orig, delta);

        assertEquals(200, result.value.id());
        assertEquals("Gary", result.value.name());
        assertSame(orig, result);
        assertNotSame(origRecord, result.value);
    }
}
