package tools.jackson.databind.ext.javatime.ser;

import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ext.javatime.deser.YearDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.*;

public class TestYearSerializationWithCustomFormatter
{
    @ParameterizedTest
    @MethodSource("customFormatters")
    void testSerialization(DateTimeFormatter formatter) throws Exception {
        Year year = Year.now();
        String expected = "\"" + year.format(formatter) + "\"";
        assertEquals(expected, serializeWith(year, formatter));
    }

    private String serializeWith(Year dateTime, DateTimeFormatter f) throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new SimpleModule()
                        .addSerializer(new YearSerializer(f)))
                .build();
        return mapper.writeValueAsString(dateTime);
    }

    @ParameterizedTest
    @MethodSource("customFormatters")
    void testDeserialization(DateTimeFormatter formatter) throws Exception {
        Year year = Year.now();
        assertEquals(year, deserializeWith(year.format(formatter), formatter));
    }

    private Year deserializeWith(String json, DateTimeFormatter f) throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new SimpleModule()
                        .addDeserializer(Year.class, new YearDeserializer(f)))
                .build();
        return mapper.readValue("\"" + json + "\"", Year.class);
    }

    static Stream<DateTimeFormatter> customFormatters() {
        return Stream.of(
                DateTimeFormatter.ofPattern("yyyy"),
                DateTimeFormatter.ofPattern("yy")
        );
    }
}
