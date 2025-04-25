package tools.jackson.databind.ext.javatime;

import java.time.ZoneId;
import java.util.*;

import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;

public class DateTimeTestBase
    extends tools.jackson.databind.testutil.DatabindTestUtil
{
    protected static final ZoneId UTC = ZoneId.of("UTC");

    protected static final ZoneId Z_CHICAGO = ZoneId.of("America/Chicago");
    protected static final ZoneId Z_BUDAPEST = ZoneId.of("Europe/Budapest");

    // 14-Mar-2016, tatu: Serialization of trailing zeroes may change [datatype-jsr310#67]
    //   Note, tho, that "0.0" itself is special case; need to avoid scientific notation:
    final protected static String NO_NANOSECS_SER = "0.0";
    final protected static String NO_NANOSECS_SUFFIX = ".000000000";

    protected static ObjectMapper newMapper() {
        return newMapperBuilder().build();
    }

    protected static MapperBuilder<?,?> newMapperBuilder() {
        return JsonMapper.builder()
                .disable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES);
    }

    protected static MapperBuilder<?,?> newMapperBuilder(TimeZone tz) {
        return JsonMapper.builder()
                .defaultTimeZone(tz)
                .disable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES);
    }

    protected static ObjectMapper newMapper(TimeZone tz) {
        return newMapperBuilder(tz).build();
    }

    protected static JsonMapper.Builder mapperBuilder() {
        return JsonMapper.builder()
                .defaultLocale(Locale.ENGLISH)
                .disable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES);
    }

    protected static <T> Map<T, String> asMap(T key, String value) {
        return Collections.singletonMap(key, value);
    }

    protected static String mapAsString(String key, String value) {
        return String.format("{\"%s\":\"%s\"}", key, value);
    }
}
