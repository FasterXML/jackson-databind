package com.fasterxml.jackson.databind.records;

import java.util.Collections;
import java.util.Map;

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
    
    // [databind#3079]: Should be able to Record value directly
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
