package tools.jackson.databind.ext.javatime.key;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class DurationAsKeyTest extends DateTimeTestBase
{
    private static final Duration DURATION = Duration.ofMinutes(13).plusSeconds(37).plusNanos(120 * 1000 * 1000L);
    private static final String DURATION_STRING = "PT13M37.12S";

    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(new TypeReference<Map<Duration, String>>() { });

    @Test
    public void testSerialization() throws Exception {
        assertEquals(mapAsString(DURATION_STRING, "test"),
                MAPPER.writeValueAsString(Collections.singletonMap(DURATION, "test")));
    }

    @Test
    public void testDeserialization() throws Exception {
        assertEquals(Collections.singletonMap(DURATION, "test"), READER.readValue(mapAsString(DURATION_STRING, "test")));
    }
}
