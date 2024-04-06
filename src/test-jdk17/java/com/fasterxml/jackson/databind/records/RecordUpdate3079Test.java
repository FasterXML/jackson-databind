package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


// see failing test
public class RecordUpdate3079Test extends DatabindTestUtil
{
    public record IdNameRecord(int id, String name) { }

    static class IdNameWrapper {
        public IdNameRecord value;

        protected IdNameWrapper() { }
        public IdNameWrapper(IdNameRecord v) { value = v; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#3079]: also: should be able to Record valued property
    @Test
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
