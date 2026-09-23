package tools.jackson.databind.ext.javatime.misc;

import java.time.Month;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Round-trip tests for ways to serialize {@link Month} as a String
 * (instead of default number) that default {@code MonthDeserializer}
 * can read back.
 *<p>
 * See [databind#6233].
 */
public class MonthRoundTripTest extends DateTimeTestBase
{
    static class FullNameBean {
        @JsonFormat(pattern = "MMMM", locale = "en")
        public Month value;

        public FullNameBean() { }
        public FullNameBean(Month v) { value = v; }
    }

    static class ToStringBean {
        @JsonSerialize(using = ToStringSerializer.class)
        public Month value;

        public ToStringBean() { }
        public ToStringBean(Month v) { value = v; }
    }

    static class ShapeStringBean {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public Month value;

        public ShapeStringBean() { }
        public ShapeStringBean(Month v) { value = v; }
    }

    static class Wrapper {
        public Month value;

        public Wrapper() { }
        public Wrapper(Month v) { value = v; }
    }

    private final ObjectMapper MAPPER = newMapper();

    // [databind#6233]
    @ParameterizedTest
    @EnumSource(Month.class)
    public void testRoundTripWithFullNamePattern(Month month) throws Exception
    {
        String json = MAPPER.writeValueAsString(new FullNameBean(month));
        assertEquals(a2q("{'value':'" + displayName(month) + "'}"), json);
        assertEquals(month, MAPPER.readValue(json, FullNameBean.class).value);
    }

    // [databind#6233]: same as above but via config override, no annotation
    @ParameterizedTest
    @EnumSource(Month.class)
    public void testRoundTripWithConfigOverridePattern(Month month) throws Exception
    {
        ObjectMapper mapper = mapperBuilder()
                .withConfigOverride(Month.class, o -> o.setFormat(
                        JsonFormat.Value.forPattern("MMMM").withLocale(Locale.ENGLISH)))
                .build();
        String json = mapper.writeValueAsString(month);
        assertEquals(q(displayName(month)), json);
        assertEquals(month, mapper.readValue(json, Month.class));
    }

    // [databind#6233]: suggested work-around, writes Enum name like "JANUARY";
    // ONE_BASED_MONTHS must not affect it either way
    @ParameterizedTest
    @EnumSource(Month.class)
    public void testRoundTripWithToStringSerializerModule(Month month) throws Exception
    {
        for (boolean oneBased : new boolean[] { false, true }) {
            ObjectMapper mapper = mapperBuilder()
                    .configure(DateTimeFeature.ONE_BASED_MONTHS, oneBased)
                    .addModule(new SimpleModule()
                            .addSerializer(Month.class, ToStringSerializer.instance))
                    .build();
            String json = mapper.writeValueAsString(month);
            assertEquals(q(month.name()), json);
            assertEquals(month, mapper.readValue(json, Month.class));

            json = mapper.writeValueAsString(new Wrapper(month));
            assertEquals(a2q("{'value':'" + month.name() + "'}"), json);
            assertEquals(month, mapper.readValue(json, Wrapper.class).value);
        }
    }

    // [databind#6233]: per-property alternative to module registration
    @ParameterizedTest
    @EnumSource(Month.class)
    public void testRoundTripWithToStringSerializerAnnotation(Month month) throws Exception
    {
        String json = MAPPER.writeValueAsString(new ToStringBean(month));
        assertEquals(a2q("{'value':'" + month.name() + "'}"), json);
        assertEquals(month, MAPPER.readValue(json, ToStringBean.class).value);
    }

    // [databind#6233]: explicit String shape (no pattern) writes Enum name;
    // locale-independent so neither ONE_BASED_MONTHS nor default Locale matter
    @ParameterizedTest
    @EnumSource(Month.class)
    public void testRoundTripWithShapeString(Month month) throws Exception
    {
        for (boolean oneBased : new boolean[] { false, true }) {
            ObjectMapper mapper = mapperBuilder()
                    .configure(DateTimeFeature.ONE_BASED_MONTHS, oneBased)
                    .defaultLocale(Locale.FRENCH)
                    .build();
            String json = mapper.writeValueAsString(new ShapeStringBean(month));
            assertEquals(a2q("{'value':'" + month.name() + "'}"), json);
            assertEquals(month, mapper.readValue(json, ShapeStringBean.class).value);
        }
    }

    // [databind#6233]: same as above but via config override, no annotation
    @ParameterizedTest
    @EnumSource(Month.class)
    public void testRoundTripWithConfigOverrideShapeString(Month month) throws Exception
    {
        ObjectMapper mapper = mapperBuilder()
                .withConfigOverride(Month.class, o -> o.setFormat(
                        JsonFormat.Value.forShape(JsonFormat.Shape.STRING)))
                .build();
        String json = mapper.writeValueAsString(month);
        assertEquals(q(month.name()), json);
        assertEquals(month, mapper.readValue(json, Month.class));
    }

    // Map keys are always written as Enum names, regardless of value serializer
    @ParameterizedTest
    @EnumSource(Month.class)
    public void testRoundTripAsMapKey(Month month) throws Exception
    {
        Map<Month, String> input = asMap(month, "x");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(mapAsString(month.name(), "x"), json);
        assertEquals(input, MAPPER.readValue(json,
                new TypeReference<Map<Month, String>>() { }));
    }

    private static String displayName(Month month) {
        String name = month.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
