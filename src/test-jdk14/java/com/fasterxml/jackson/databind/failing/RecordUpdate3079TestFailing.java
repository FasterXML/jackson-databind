package com.fasterxml.jackson.databind.failing;

import java.util.Collections;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.records.RecordUpdate3079Test;

/**
 * Tests in this class were moved from {@link RecordUpdate3079Test}.
 */
public class RecordUpdate3079TestFailing extends BaseMapTest {
    record IdNameRecord(int id, String name) { }

    static class IdNameWrapper {
        public IdNameRecord value;

        protected IdNameWrapper() { }
        public IdNameWrapper(IdNameRecord v) { value = v; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();
  
    /**
     *This test no longer works as of JDK 15 for records. Maybe Unsafe will work?
     * 
     * https://medium.com/@atdsqdjyfkyziqkezu/java-15-breaks-deserialization-of-records-902fcc81253d
     * https://stackoverflow.com/questions/61141836/change-static-final-field-in-java-12
     */
    // [databind#3079]: Should be able to Record value directly
    // [databind#3102]: fails on JDK 16 which finally blocks mutation
    // of Record fields.
    public void testDirectRecordUpdate() throws Exception
    {
        IdNameRecord orig = new IdNameRecord(123, "Bob");
        IdNameRecord result = MAPPER.updateValue(orig,
             Collections.singletonMap("id", 137));
        assertNotNull(result);
        assertEquals(137, result.id());
        assertEquals("Bob", result.name());
        assertNotSame(orig, result);
    }
}
