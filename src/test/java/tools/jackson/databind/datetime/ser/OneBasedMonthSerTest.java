package tools.jackson.databind.datetime.ser;

import java.time.Month;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import tools.jackson.databind.datetime.JavaTimeFeature;
import tools.jackson.databind.datetime.JavaTimeModule;
import tools.jackson.databind.datetime.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OneBasedMonthSerTest extends ModuleTestBase
{
    static class Wrapper {
        public Month month;

        public Wrapper(Month m) { month = m; }
        public Wrapper() { }
    }

    @Test
    public void testSerializationFromEnum() throws Exception
    {
        assertEquals( "\"JANUARY\"" , writerForOneBased()
            .with(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            .writeValueAsString(Month.JANUARY));
        assertEquals( "\"JANUARY\"" , writerForZeroBased()
            .with(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            .writeValueAsString(Month.JANUARY));
    }

    @Test
    public void testSerializationFromEnumWithPattern_oneBased() throws Exception
    {
        ObjectWriter w = writerForOneBased().with(SerializationFeature.WRITE_ENUMS_USING_INDEX);
        assertEquals( "{\"month\":1}" , w.writeValueAsString(new Wrapper(Month.JANUARY)));
    }

    @Test
    public void testSerializationFromEnumWithPattern_zeroBased() throws Exception
    {
        ObjectWriter w = writerForZeroBased().with(SerializationFeature.WRITE_ENUMS_USING_INDEX);
        assertEquals( "{\"month\":0}" , w.writeValueAsString(new Wrapper(Month.JANUARY)));
    }


    private ObjectWriter writerForZeroBased() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(JavaTimeFeature.ONE_BASED_MONTHS)
                .build()
                .writer();
    }

    private ObjectWriter writerForOneBased() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .enable(JavaTimeFeature.ONE_BASED_MONTHS)
                .build()
                .writer();
    }

}
