package tools.jackson.databind.ext.javatime.ser;

import java.time.Month;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonthSerializerTest
    extends DateTimeTestBase
{
    private final ObjectMapper MAPPER = newMapper();

    static class Wrapper {
        public Month month;

        public Wrapper(Month m) { month = m; }
        public Wrapper() { }
    }

    static class ShapeIntWrapper {
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
        public Month value;
        public ShapeIntWrapper() { }
        public ShapeIntWrapper(Month v) { value = v; }
    }

    static class NoShapeIntWrapper {
        public Month value;
        public NoShapeIntWrapper() { }
        public NoShapeIntWrapper(Month v) { value = v; }
    }

    static class FrBean {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MMM", locale = "fr")
        public Month value;
        public FrBean() { }
        public FrBean(Month v) { value = v; }
    }

    static class ShapeStringBean {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public Month value;
        public ShapeStringBean() { }
        public ShapeStringBean(Month v) { value = v; }
    }

    static class ShapeArrayBean {
        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        public Month value;
        public ShapeArrayBean() { }
        public ShapeArrayBean(Month v) { value = v; }
    }

    @Test
    public void testSerializationFromEnum() throws Exception
    {
        assertEquals("1", writerForOneBased()
            .writeValueAsString(Month.JANUARY));
        assertEquals("0", writerForZeroBased()
            .writeValueAsString(Month.JANUARY));
    }

    @Test
    public void testSerializationWithTypeInfo() throws Exception
    {
        ObjectMapper mapper = mapperBuilder()
                .addMixIn(TemporalAccessor.class, MockObjectConfiguration.class)
                .build();
        String json = mapper.writeValueAsString(Month.MARCH);
        assertEquals("[\"" + Month.class.getName() + "\",3]", json);
    }

    @Test
    public void testDefaultSerialization() throws Exception
    {
        // default emits 1-based ordinal
        assertEquals("1", MAPPER.writeValueAsString(Month.JANUARY));
    }

    @ParameterizedTest(name = "oneBased={0}, expectedJson={1}, input={2}")
    @MethodSource("oneBasedVsIndex")
    public void testParameterizedOneBasedVsIndex(boolean oneBased, String expectedJson, Object input)
            throws Exception
    {
        JsonMapper.Builder builder = JsonMapper.builder();

        if (oneBased) { builder.enable(DateTimeFeature.ONE_BASED_MONTHS); }
        else { builder.disable(DateTimeFeature.ONE_BASED_MONTHS); }

        ObjectWriter writer = builder.build().writer();

        assertEquals(expectedJson, writer.writeValueAsString(input));
    }

    @Test
    public void testOneBasedSerialization() throws Exception
    {
        ObjectMapper disabled = mapperBuilder()
                .disable(DateTimeFeature.ONE_BASED_MONTHS)
                .build();

        assertEquals("{\"month\":0}", disabled.writeValueAsString(new Wrapper(Month.JANUARY)));

        ObjectMapper enabled = mapperBuilder()
                .enable(DateTimeFeature.ONE_BASED_MONTHS)
                .build();

        assertEquals("{\"month\":1}", enabled.writeValueAsString(new Wrapper(Month.JANUARY)));
    }

    // ShapeInt Test
    @Test
    public void testSerializationWithShapeInt() throws Exception
    {
        // One with shape
        String json = MAPPER.writeValueAsString(new ShapeIntWrapper(Month.MARCH));
        assertEquals("{\"value\":[3]}", json);

        // One without shape
        json = MAPPER.writeValueAsString(new NoShapeIntWrapper(Month.MARCH));
        assertEquals("{\"value\":3}", json);
    }

    @Test
    public void testSerializationWithFrLocale() throws Exception
    {
        String json = MAPPER.writeValueAsString(new FrBean(Month.MARCH));
        assertEquals("{\"value\":\"mars\"}", json);
    }

    @Test
    public void testSerializationWithShapeArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new ShapeArrayBean(Month.DECEMBER));
        assertEquals("{\"value\":[12]}", json);
    }

    // [databind#6233]: explicit String shape (no pattern) writes Enum name;
    // locale-independent so neither ONE_BASED_MONTHS nor default Locale matter
    @ParameterizedTest(name = "month={0}, oneBased={1}")
    @MethodSource("monthsAndOneBased")
    public void roundTripWithShapeString(Month month, boolean oneBased) throws Exception
    {
        ObjectMapper mapper = mapperBuilder()
                .configure(DateTimeFeature.ONE_BASED_MONTHS, oneBased)
                .defaultLocale(Locale.FRENCH)
                .build();
        String json = mapper.writeValueAsString(new ShapeStringBean(month));
        assertEquals("""
                {"value":"%s"}""".formatted(month.name()), json);
        assertEquals(month, mapper.readValue(json, ShapeStringBean.class).value);
    }

    // [databind#6233]: same as above but via config override, no annotation
    @ParameterizedTest(name = "month={0}, oneBased={1}")
    @MethodSource("monthsAndOneBased")
    public void roundTripWithConfigOverrideShapeString(Month month, boolean oneBased) throws Exception
    {
        ObjectMapper mapper = mapperBuilder()
                .configure(DateTimeFeature.ONE_BASED_MONTHS, oneBased)
                .withConfigOverride(Month.class, o -> o.setFormat(
                        JsonFormat.Value.forShape(JsonFormat.Shape.STRING)))
                .build();
        String json = mapper.writeValueAsString(month);
        assertEquals(q(month.name()), json);
        assertEquals(month, mapper.readValue(json, Month.class));
    }

    private static Stream<Arguments> monthsAndOneBased() {
        return Arrays.stream(Month.values())
                .flatMap(m -> Stream.of(Arguments.of(m, false), Arguments.of(m, true)));
    }

    private static Stream<Arguments> oneBasedVsIndex() {
        return Stream.of(
                // oneBased, writeIndex, expectedJson
                Arguments.of(false, "0", Month.JANUARY),
                Arguments.of(true , "1", Month.JANUARY),
                Arguments.of(false, "{\"month\":0}", new Wrapper(Month.JANUARY)),
                Arguments.of(true , "{\"month\":1}", new Wrapper(Month.JANUARY))
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