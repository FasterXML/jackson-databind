package tools.jackson.databind.ext.javatime.ser;

import java.time.Month;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OneBasedMonthSerTest extends DateTimeTestBase
{
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
    public void testSerializationFromEnumWithPattern_oneBased() throws Exception
    {
        ObjectWriter w = writerForOneBased().with(EnumFeature.WRITE_ENUMS_USING_INDEX);
        assertEquals( "{\"month\":1}" , w.writeValueAsString(new Wrapper(Month.JANUARY)));
    }

    @Test
    public void testSerializationFromEnumWithPattern_zeroBased() throws Exception
    {
        ObjectWriter w = writerForZeroBased().with(EnumFeature.WRITE_ENUMS_USING_INDEX);
        assertEquals( "{\"month\":0}" , w.writeValueAsString(new Wrapper(Month.JANUARY)));
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
