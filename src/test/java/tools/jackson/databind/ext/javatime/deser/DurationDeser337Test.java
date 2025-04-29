package tools.jackson.databind.ext.javatime.deser;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DurationDeser337Test extends DateTimeTestBase
{
    @Test
    public void testWithDurationsAsTimestamps() throws Exception
    {
        final ObjectMapper MAPPER_DURATION_TIMESTAMPS = mapperBuilder()
                .enable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .build();

        Duration duration = Duration.parse("PT-43.636S");

        String ser = MAPPER_DURATION_TIMESTAMPS.writeValueAsString(duration);

        assertEquals("-43.636000000", ser);

        Duration deser = MAPPER_DURATION_TIMESTAMPS.readValue(ser, Duration.class);

        assertEquals(duration, deser);
        assertEquals(deser.toString(), "PT-43.636S");
    }

    @Test
    public void testWithoutDurationsAsTimestamps() throws Exception
    {
        ObjectMapper mapper = mapperBuilder()
                .disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .build();

        Duration duration = Duration.parse("PT-43.636S");

        String ser = mapper.writeValueAsString(duration);
        assertEquals(q("PT-43.636S"), ser);

        Duration deser = mapper.readValue(ser, Duration.class);
        assertEquals(duration, deser);
    }
}
