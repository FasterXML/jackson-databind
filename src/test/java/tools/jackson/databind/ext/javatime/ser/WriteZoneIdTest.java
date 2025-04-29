package tools.jackson.databind.ext.javatime.ser;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;

import static org.junit.jupiter.api.Assertions.*;

public class WriteZoneIdTest extends DateTimeTestBase
{
    static class DummyClassWithDate {
        @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "dd-MM-yyyy'T'hh:mm:ss Z",
                with = JsonFormat.Feature.WRITE_DATES_WITH_ZONE_ID)
        public ZonedDateTime date;

        DummyClassWithDate() { }

        public DummyClassWithDate(ZonedDateTime date) {
            this.date = date;
        }
    }

    private static ObjectMapper MAPPER = newMapper();

    @Test
    public void testSerialization01() throws Exception
    {
        ZoneId id = ZoneId.of("America/Chicago");
        String value = MAPPER.writeValueAsString(id);
        assertEquals("\"America/Chicago\"", value);
    }

    @Test
    public void testSerialization02() throws Exception
    {
        ZoneId id = ZoneId.of("America/Anchorage");
        String value = MAPPER.writeValueAsString(id);
        assertEquals("\"America/Anchorage\"", value);
    }

    @Test
    public void testSerializationWithTypeInfo01() throws Exception
    {
        ZoneId id = ZoneId.of("America/Denver");
        ObjectMapper mapper = mapperBuilder()
                .addMixIn(ZoneId.class, MockObjectConfiguration.class)
                .build();
        String value = mapper.writeValueAsString(id);
        assertEquals("[\"java.time.ZoneId\",\"America/Denver\"]", value);
    }

    @Test
    public void testJacksonAnnotatedPOJOWithDateWithTimezoneToJson() throws Exception
    {
        String ZONE_ID_STR = "Asia/Krasnoyarsk";
        final ZoneId ZONE_ID = ZoneId.of(ZONE_ID_STR);

        DummyClassWithDate input = new DummyClassWithDate(ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), ZONE_ID));

        // 30-Jun-2016, tatu: Exact time seems to vary a bit based on DST, so let's actually
        //    just verify appending of timezone id itself:
        String json = MAPPER.writeValueAsString(input);
        if (!json.contains("\"01-01-1970T")) {
            fail("Should contain time prefix, did not: "+json);
        }
        String match = String.format("[%s]", ZONE_ID_STR);
        if (!json.contains(match)) {
            fail("Should contain zone id "+match+", does not: "+json);
        }
    }

    @Test
    public void testMapSerialization() throws Exception {
        final ZonedDateTime datetime = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Warsaw]");
        final HashMap<ZonedDateTime, String> map = new HashMap<>();
        map.put(datetime, "");
        String json = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
                .writeValueAsString(map);
        assertEquals("{\"2007-12-03T10:15:30+01:00[Europe/Warsaw]\":\"\"}", json);
    }
}
