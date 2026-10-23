package com.fasterxml.jackson.databind.tofix;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4629] @JsonIncludeProperties & @JsonIgnoreProperties
//                are ignored when deserializing Records
public class RecordsWithJsonIncludeAndIgnorals4629Test
        extends DatabindTestUtil {

    record Id2Name(
            int id,
            String name
    ) { }

    record RecordWithInclude4629(
            @JsonIncludeProperties("id") Id2Name child
    ) { }

    record RecordWIthIgnore4629(
            @JsonIgnoreProperties("name") Id2Name child
    ) { }

    private final ObjectMapper MAPPER = newJsonMapper();

    @JacksonTestFailureExpected
    @Test
    void testJsonInclude4629()
        throws Exception
    {
        RecordWithInclude4629 expected = new RecordWithInclude4629(new Id2Name(123, null));
        String input = "{\"child\":{\"id\":123,\"name\":\"Bob\"}}";

        RecordWithInclude4629 actual = MAPPER.readValue(input, RecordWithInclude4629.class);

        assertEquals(expected, actual);
    }

    @JacksonTestFailureExpected
    @Test
    void testJsonIgnore4629()
        throws Exception
    {
        RecordWIthIgnore4629 expected = new RecordWIthIgnore4629(new Id2Name(123, null));
        String input = "{\"child\":{\"id\":123,\"name\":\"Bob\"}}";

        RecordWIthIgnore4629 actual = MAPPER.readValue(input, RecordWIthIgnore4629.class);

        assertEquals(expected, actual);
    }

}
