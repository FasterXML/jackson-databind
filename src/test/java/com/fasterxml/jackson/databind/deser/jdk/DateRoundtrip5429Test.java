package com.fasterxml.jackson.databind.deser.jdk;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateRoundtrip5429Test extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void testDateRoundTripWithMaxValue() throws Exception {

        Date original = new Date(Long.MAX_VALUE);
        String json = MAPPER.writeValueAsString(original);
        Date parsed = MAPPER.readValue(json, Date.class);

        assertEquals(original.getTime(), parsed.getTime());
    }

    @Test
    void testDateRoundTripWithMinValue() throws Exception {
        Date original = new Date(Long.MIN_VALUE);
        String json = MAPPER.writeValueAsString(original);
        Date parsed = MAPPER.readValue(json, Date.class);

        assertEquals(original.getTime(), parsed.getTime());
    }
}
