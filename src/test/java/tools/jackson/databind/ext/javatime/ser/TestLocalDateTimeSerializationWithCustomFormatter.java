package tools.jackson.databind.ext.javatime.ser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLocalDateTimeSerializationWithCustomFormatter
{
    @ParameterizedTest
    @MethodSource("customFormatters")
    void testSerialization(DateTimeFormatter formatter) throws Exception {
        LocalDateTime dateTime = LocalDateTime.now();
        assertTrue(serializeWith(dateTime, formatter).contains(dateTime.format(formatter)));
    }

    private String serializeWith(LocalDateTime dateTime, DateTimeFormatter f) throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new SimpleModule()
                        .addSerializer(new LocalDateTimeSerializer(f)))
                .build();
        return mapper.writeValueAsString(dateTime);
    }

    @ParameterizedTest
    @MethodSource("customFormatters")
    void testDeserialization(DateTimeFormatter formatter) throws Exception {
        LocalDateTime dateTime = LocalDateTime.now();
        assertEquals(dateTime, deserializeWith(dateTime.format(formatter), formatter));
    }

    private LocalDateTime deserializeWith(String json, DateTimeFormatter f) throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new SimpleModule()
                        .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(f)))
                .build();
        return mapper.readValue("\"" + json + "\"", LocalDateTime.class);
    }

    static Stream<DateTimeFormatter> customFormatters() {
        return Stream.of(
                DateTimeFormatter.ISO_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
    }
}
