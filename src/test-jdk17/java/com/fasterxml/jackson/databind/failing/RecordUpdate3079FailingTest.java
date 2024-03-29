package com.fasterxml.jackson.databind.failing;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.records.RecordUpdate3079Test;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// 01-Dec-2022, tatu: Alas, fails on JDK 17
// see related passing test in RecordUpdate3079Test
public class RecordUpdate3079FailingTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#3079]: Should be able to Record value directly
    @Test
    public void testDirectRecordUpdate() throws Exception
    {
        RecordUpdate3079Test.IdNameRecord orig = new RecordUpdate3079Test.IdNameRecord(123, "Bob");
        RecordUpdate3079Test.IdNameRecord result = MAPPER.updateValue(orig,
                Collections.singletonMap("id", 137));
        assertNotNull(result);
        assertEquals(137, result.id());
        assertEquals("Bob", result.name());
        assertNotSame(orig, result);
    }
}
