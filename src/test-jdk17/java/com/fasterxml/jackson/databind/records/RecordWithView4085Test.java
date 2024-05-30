package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonView;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

// [databind#4085]
public class RecordWithView4085Test extends DatabindTestUtil
{
    static class View4085Default { }
    static class View4085Field { }
    
    @JsonView(View4085Default.class)
    public record Record4085(int total, @JsonView(View4085Field.class) int current) { }

    @Test
    public void testRecordWithView4085() throws Exception
    {
        final Record4085 input = new Record4085(1, 2);
        final String EXP = a2q("{'total':1,'current':2}");
        final ObjectWriter w = newJsonMapper().writer();

        // by default, all properties included, without view
        assertEquals(EXP, w.writeValueAsString(input));

        // with non-inclusive view, nothing included:
        assertEquals("{}", w.withView(Void.class).writeValueAsString(input));

        // But other combinations exist
        assertEquals(a2q("{'total':1}"),
                w.withView(View4085Default.class).writeValueAsString(input));
        assertEquals(a2q("{'current':2}"),
                w.withView(View4085Field.class).writeValueAsString(input));
    }
}
