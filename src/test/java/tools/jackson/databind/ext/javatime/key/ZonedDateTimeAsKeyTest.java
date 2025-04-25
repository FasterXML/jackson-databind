package tools.jackson.databind.ext.javatime.key;

import java.time.*;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class ZonedDateTimeAsKeyTest extends DateTimeTestBase
{
    private static final TypeReference<Map<ZonedDateTime, String>> TYPE_REF = new TypeReference<Map<ZonedDateTime, String>>() {
    };
    private static final ZonedDateTime DATE_TIME_0 = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.UTC);
    private static final String DATE_TIME_0_STRING = "1970-01-01T00:00:00Z";
//    private static final Instant DATE_TIME_0_INSTANT = DATE_TIME_0.toInstant();

    private static final ZonedDateTime DATE_TIME_1 = ZonedDateTime.of(
            2015, 3, 14, 9, 26, 53, 590 * 1000 * 1000, ZoneOffset.UTC);
    private static final String DATE_TIME_1_STRING = "2015-03-14T09:26:53.59Z";

    private static final ZonedDateTime DATE_TIME_2 = ZonedDateTime.of(
            2015, 3, 14, 9, 26, 53, 590 * 1000 * 1000, ZoneId.of("Europe/Budapest"));
    /**
     * Value of {@link #DATE_TIME_2} after it's been serialized and read back. Serialization throws away time zone information, it only
     * keeps offset data.
     */
    private static final ZonedDateTime DATE_TIME_2_OFFSET = DATE_TIME_2.withZoneSameInstant(ZoneOffset.ofHours(1));
    private static final String DATE_TIME_2_STRING = "2015-03-14T09:26:53.59+01:00";;

    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(TYPE_REF);

    @Test
    public void testSerialization0() throws Exception {
        String value = MAPPER.writerFor(TYPE_REF).writeValueAsString(asMap(DATE_TIME_0, "test"));
        assertEquals(mapAsString(DATE_TIME_0_STRING, "test"), value);
    }

    @Test
    public void testSerialization1() throws Exception {
        String value = MAPPER.writerFor(TYPE_REF).writeValueAsString(asMap(DATE_TIME_1, "test"));
        assertEquals(mapAsString(DATE_TIME_1_STRING, "test"), value);
    }

    @Test
    public void testSerialization2() throws Exception {
        String value = MAPPER.writerFor(TYPE_REF).writeValueAsString(asMap(DATE_TIME_2, "test"));
        assertEquals(mapAsString(DATE_TIME_2_STRING, "test"), value);
    }

    @Test
    public void testDeserialization0() throws Exception {
        assertEquals(asMap(DATE_TIME_0, "test"),
                READER.readValue(mapAsString(DATE_TIME_0_STRING, "test")));
    }

    @Test
    public void testDeserialization1() throws Exception {
        assertEquals(asMap(DATE_TIME_1, "test"),
                READER.readValue(mapAsString(DATE_TIME_1_STRING, "test")));
    }

    @Test
    public void testDeserialization2() throws Exception {
        assertEquals(asMap(DATE_TIME_2_OFFSET, "test"),
                READER.readValue(mapAsString(DATE_TIME_2_STRING, "test")));
    }

    @Test
    public void testSerializationToInstantWithNanos() throws Exception {
        String value = mapperBuilder().enable(DateTimeFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS).build()
            .writerFor(TYPE_REF).writeValueAsString(asMap(DATE_TIME_1, "test"));
        assertEquals(mapAsString(String.valueOf(DATE_TIME_1.toEpochSecond()) + '.' + DATE_TIME_1.getNano(), "test"), value);
    }

    @Test
    public void testSerializationToInstantWithoutNanos() throws Exception {
        String value = mapperBuilder().enable(DateTimeFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS)
            .disable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS).build()
            .writerFor(TYPE_REF).writeValueAsString(asMap(DATE_TIME_1, "test"));
        assertEquals(mapAsString(String.valueOf(DATE_TIME_1.toInstant().toEpochMilli()), "test"), value);
    }
}
