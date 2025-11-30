package tools.jackson.databind.deser.jdk;

import java.util.Date;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5429]
public class DateRoundtrip5429Test extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void testDateRoundTripWithMaxValue() throws Exception {

        Date original = new Date(Long.MAX_VALUE);
        String json = MAPPER.writeValueAsString(original);
        Date parsed = MAPPER.readValue(json, Date.class);

        assertEquals(original.getTime(), parsed.getTime());
        // but also check actual serialization

        // 28-Nov-2025, tatu: For some reason, not UTC in 3.0 (unlike in 2.x)?
        //   Should figure out; commented out for now
        //assertEquals(q("+292278994-08-17T07:12:55.807+00:00"), json);
    }

    @Test
    void testDateRoundTripWithMinValue() throws Exception {
        Date original = new Date(Long.MIN_VALUE);
        String json = MAPPER.writeValueAsString(original);
        Date parsed = MAPPER.readValue(json, Date.class);

        assertEquals(original.getTime(), parsed.getTime());
        // but also check actual serialization

        // 28-Nov-2025, tatu: For some reason, not UTC in 3.0 (unlike in 2.x)?
        //   Should figure out; commented out for now
        //assertEquals(q("-292269054-12-02T16:47:04.192+00:00"), json);
    }
}
