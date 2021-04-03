package com.fasterxml.jackson.databind.failing;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.exc.InvalidNullException;

// [databind#3102]: fails on JDK 16 which finally blocks mutation
// of Record fields.
public class RecordWithJsonNaming3102Test extends BaseMapTest
{
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SnakeRecord(int id, String toSnakeCase) {
        @JsonCreator
        public SnakeRecord(int id, String toSnakeCase) {
            this.id = id;
            this.toSnakeCase = toSnakeCase;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, Record type introspection
    /**********************************************************************
     */

    // [databind#3102]
    public void testDeserializeWithJsonNaming() throws Exception
    {
        final ObjectReader r = MAPPER.readerFor(SnakeRecord.class);
        // First, regular case
        SnakeRecord value = r.readValue(a2q(
 "{'id':123,'to_snake_case':'snakey'}"));
        assertEquals(123, value.id);
        assertEquals("snakey", value.toSnakeCase);
    }
}
