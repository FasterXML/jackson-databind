package tools.jackson.databind.ext.javatime.ser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.*;

public class TestLocalDateSerializationWithCustomFormatter
{
    @ParameterizedTest
    @MethodSource("customFormatters")
    void testSerialization(DateTimeFormatter formatter) throws Exception {
        LocalDate date = LocalDate.now();
        assertTrue(serializeWith(date, formatter).contains(date.format(formatter)),
            "Serialized value should contain the formatted date");
    }

    private String serializeWith(LocalDate date, DateTimeFormatter f) throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .disable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                .addModule(new SimpleModule()
                        .addSerializer(new LocalDateSerializer(f)))
                .build();
        return mapper.writeValueAsString(date);
    }

    @ParameterizedTest
    @MethodSource("customFormatters")
    void testDeserialization(DateTimeFormatter formatter) throws Exception {
        LocalDate date = LocalDate.now();
        assertEquals(date, deserializeWith(date.format(formatter), formatter),
            "Deserialized value should match the original date");
    }

    private LocalDate deserializeWith(String json, DateTimeFormatter f) throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new SimpleModule()
                        .addDeserializer(LocalDate.class, new LocalDateDeserializer(f)))
                .build();
        return mapper.readValue("\"" + json + "\"", LocalDate.class);
    }

    static Stream<DateTimeFormatter> customFormatters() {
        return Stream.of(
            DateTimeFormatter.BASIC_ISO_DATE,
            DateTimeFormatter.ISO_DATE,
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_ORDINAL_DATE,
            DateTimeFormatter.ISO_WEEK_DATE,
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
        );
    }
}
