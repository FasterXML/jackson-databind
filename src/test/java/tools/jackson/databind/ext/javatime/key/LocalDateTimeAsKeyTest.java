package tools.jackson.databind.ext.javatime.key;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class LocalDateTimeAsKeyTest extends DateTimeTestBase
{
    private static final LocalDateTime DATE_TIME_0 = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
    /*
     * Current serializer is LocalDateTime.toString(), which omits seconds if it can
     */
    private static final String DATE_TIME_0_STRING = "1970-01-01T00:00";
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2015, 3, 14, 9, 26, 53, 590 * 1000 * 1000);
    private static final String DATE_TIME_STRING = "2015-03-14T09:26:53.590";

    private final TypeReference<Map<LocalDateTime, String>> TYPE_REF = new TypeReference<Map<LocalDateTime, String>>() { };
    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(TYPE_REF);

    @Test
    public void testSerialization0() throws Exception {
        String value = MAPPER.writeValueAsString(asMap(DATE_TIME_0, "test"));
        assertEquals(mapAsString(DATE_TIME_0_STRING, "test"), value);
    }

    @Test
    public void testSerialization1() throws Exception {
        String value = MAPPER.writeValueAsString(asMap(DATE_TIME, "test"));
        assertEquals(mapAsString(DATE_TIME_STRING, "test"), value);
    }

    @Test
    public void testDeserialization0() throws Exception {
        Map<LocalDateTime, String> value = READER.readValue(
                mapAsString(DATE_TIME_0_STRING, "test"));
        assertEquals(asMap(DATE_TIME_0, "test"), value, "Value is incorrect");
    }

    @Test
    public void testDeserialization1() throws Exception {
        Map<LocalDateTime, String> value = READER.readValue(
                mapAsString(DATE_TIME_STRING, "test"));
        assertEquals(asMap(DATE_TIME, "test"), value, "Value is incorrect");
    }

    @Test
    public void testDateTimeExceptionIsHandled() throws Throwable
    {
        LocalDateTime now = LocalDateTime.now();
        DeserializationProblemHandler handler = new DeserializationProblemHandler() {
            @Override
            public Object handleWeirdKey(DeserializationContext ctxt, Class<?> targetType,
                   String valueToConvert, String failureMsg) {
                if (LocalDateTime.class == targetType) {
                    if ("now".equals(valueToConvert)) {
                        return now;
                    }
                }
                return NOT_HANDLED;
            }
        };
        Map<LocalDateTime, String> value = mapperBuilder().addHandler(handler)
                .build()
                .readValue(mapAsString("now", "test"), TYPE_REF);
        
        assertEquals(asMap(now, "test"), value);
    }
}
