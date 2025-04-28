package tools.jackson.databind.ext.javatime.ser;

import java.time.Month;
import java.time.temporal.TemporalAccessor;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonthSerializerTest extends DateTimeTestBase
{
    private final ObjectMapper MAPPER = newMapper();

    static class Wrapper {
        public Month month;

        public Wrapper(Month m) { month = m; }
        public Wrapper() { }
    }

    @Test
    public void testSerializationFromEnum() throws Exception
    {
        assertEquals(q("JANUARY"), writerForOneBased()
            .with(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
            .writeValueAsString(Month.JANUARY));
        assertEquals(q("JANUARY"), writerForZeroBased()
            .with(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
            .writeValueAsString(Month.JANUARY));
    }

    @Test
    public void testSerializationWithTypeInfo() throws Exception
    {
        ObjectMapper mapper = mapperBuilder()
                .addMixIn(TemporalAccessor.class, MockObjectConfiguration.class)
                .build();
        String json = mapper.writeValueAsString(Month.MARCH);
        assertEquals("[\"" + Month.class.getName() + "\",\"MARCH\"]", json);
    }

    @Test
    public void testDefaultSerialization() throws Exception
    {
        // default without WRITE_ENUMS_USING_TO_STRING/INDEX: emits enum name
        assertEquals(q("JANUARY"), MAPPER.writeValueAsString(Month.JANUARY));
    }

    @ParameterizedTest(name = "oneBased={0}, writeEnumUsingIndex={1}, expectedJson={2}, input={3}")
    @MethodSource("oneBasedVsIndex")
    public void testParameterizedOneBasedVsIndex(boolean oneBased, boolean writeEnumUsingIndex, String expectedJson, Object input)
            throws Exception
    {
        JsonMapper.Builder builder = JsonMapper.builder();

        if (oneBased) { builder.enable(DateTimeFeature.ONE_BASED_MONTHS); }
        else { builder.disable(DateTimeFeature.ONE_BASED_MONTHS); }

        if (writeEnumUsingIndex) { builder.enable(EnumFeature.WRITE_ENUMS_USING_INDEX); }
        else { builder.disable(EnumFeature.WRITE_ENUMS_USING_INDEX); }

        ObjectWriter writer = builder.build().writer();

        assertEquals(expectedJson, writer.writeValueAsString(input));
    }

    private static Stream<Arguments> oneBasedVsIndex() {
        return Stream.of(
                // oneBased, writeIndex, expectedJson
                Arguments.of(false, false, "\"JANUARY\"", Month.JANUARY),
                Arguments.of(false, true , "{\"month\":0}", new Wrapper(Month.JANUARY)),
                Arguments.of(true , false, "\"JANUARY\"", Month.JANUARY),
                Arguments.of(true , true , "{\"month\":1}", new Wrapper(Month.JANUARY))
        );
    }

    private ObjectWriter writerForZeroBased() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.ONE_BASED_MONTHS)
                .build()
                .writer();
    }

    private ObjectWriter writerForOneBased() {
        return JsonMapper.builder()
                .enable(DateTimeFeature.ONE_BASED_MONTHS)
                .build()
                .writer();
    }
}