package com.fasterxml.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5115] @JsonUnwrapped can't handle name collision #5115
public class RecordUnwrapped5115Test
    extends DatabindTestUtil
{
    record FooRecord5115(int a, int b) { }
    record BarRecordFail5115(@JsonUnwrapped FooRecord5115 a, int c) { }
    record BarRecordPass5115(@JsonUnwrapped FooRecord5115 foo, int c) { }

    static class FooPojo5115 {
        public int a;
        public int b;
    }

    static class BarPojo5115 {
        @JsonUnwrapped
        public FooPojo5115 a;
        public int c;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void unwrappedPojoShouldRoundTrip() throws Exception
    {
        BarPojo5115 input  = new BarPojo5115();
        input.a = new FooPojo5115();
        input.c = 4;
        input.a.a = 1;
        input.a.b = 2;

        String      json   = MAPPER.writeValueAsString(input);
        BarPojo5115 output = MAPPER.readValue(json, BarPojo5115.class);

        assertEquals(4, output.c);
        assertEquals(1, output.a.a);
        assertEquals(2, output.a.b);
    }

    @Test
    void unwrappedRecordShouldRoundTripPass() throws Exception
    {
        BarRecordPass5115 input = new BarRecordPass5115(new FooRecord5115(1, 2), 3);

        // Serialize
        String json = MAPPER.writeValueAsString(input);

        // Deserialize (currently fails)
        BarRecordPass5115 output = MAPPER.readValue(json, BarRecordPass5115.class);

        // Should match after bug is fixed
        assertEquals(input, output);
    }

    @JacksonTestFailureExpected
    @Test
    void unwrappedRecordShouldRoundTrip() throws Exception
    {
        BarRecordFail5115 input = new BarRecordFail5115(new FooRecord5115(1, 2), 3);

        // Serialize
        String json = MAPPER.writeValueAsString(input);

        // Once the bug is fixed, this assertion will pass and the
        // @JacksonTestFailureExpected annotation can be removed.
        BarRecordFail5115 output = MAPPER.readValue(json, BarRecordFail5115.class);

        // Should match after bug is fixed
        assertEquals(input, output);
    }

}
