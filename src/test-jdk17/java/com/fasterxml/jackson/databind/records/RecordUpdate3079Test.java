package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.Failing;
import org.junit.jupiter.api.Test;

import java.util.Collections;

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

    @Failing // 01-Dec-2022, tatu: Alas, fails on JDK 17
    // [databind#3079]: Should be able to Record value directly
    @Test
    public void testDirectRecordUpdate() throws Exception {
        RecordUpdate3079Test.IdNameRecord orig = new RecordUpdate3079Test.IdNameRecord(123, "Bob");
        RecordUpdate3079Test.IdNameRecord result = MAPPER.updateValue(orig,
                Collections.singletonMap("id", 137));
        assertNotNull(result);
        assertEquals(137, result.id());
        assertEquals("Bob", result.name());
        assertNotSame(orig, result);
    }
}
