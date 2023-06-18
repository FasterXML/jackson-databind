package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// see failing test
public class RecordUpdate3079Test extends BaseMapTest
{
    public record IdNameRecord(int id, String name) { }

    static class IdNameWrapper {
        public IdNameRecord value;

        protected IdNameWrapper() { }
        public IdNameWrapper(IdNameRecord v) { value = v; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

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
