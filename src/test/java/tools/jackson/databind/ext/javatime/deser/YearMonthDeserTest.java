package tools.jackson.databind.ext.javatime.deser;

import java.io.IOException;
import java.time.Month;
import java.time.YearMonth;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class YearMonthDeserTest extends DateTimeTestBase
{
    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(YearMonth.class);
    private final TypeReference<Map<String, YearMonth>> MAP_TYPE_REF = new TypeReference<Map<String, YearMonth>>() { };

    @Test
    public void testDeserializationAsString01() throws Exception
    {
        final YearMonth value = read("'2000-01'");
        assertEquals(YearMonth.of(2000, Month.JANUARY), value,
                "The value is not correct");
    }

    @Test
    public void testBadDeserializationAsString01() throws Exception
    {
        try {
            read(q("notayearmonth"));
            fail("expected DateTimeParseException");
        } catch (InvalidFormatException e) {
            verifyException(e, "could not be parsed");
        }
    }

    @Test
    public void testDeserializationAsArrayDisabled() throws Exception
    {
        try {
            read("['2000-01']");
            fail("expected MismatchedInputException");
        } catch (MismatchedInputException e) {
            verifyException(e,
"Unexpected token (`JsonToken.VALUE_STRING`), expected `JsonToken.VALUE_NUMBER_INT`");
        }
    }

    @Test
    public void testDeserializationAsEmptyArrayDisabled() throws Exception
    {
        // works even without the feature enabled
        assertNull(read("[]"));
    }

    @Test
    public void testDeserializationAsArrayEnabled() throws Exception
    {
        YearMonth value = READER.with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue(a2q("['2000-01']"));
        assertEquals(YearMonth.of(2000, Month.JANUARY), value,
                "The value is not correct");
    }

    @Test
    public void testDeserializationAsEmptyArrayEnabled() throws Exception
    {
        YearMonth value = READER.with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS,
                DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
            .readValue( "[]");
        assertNull(value);
    }

    // [modules-java8#249]
    @Test
    public void testYearAbove10k() throws Exception
    {
        YearMonth input = YearMonth.of(10000, 1);
        String json = MAPPER.writer()
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(input);
        YearMonth result = READER.readValue(json);
        assertEquals(input, result);
    }

    /*
    /**********************************************************
    /* Tests for empty string handling
    /**********************************************************
     */

    @Test
    public void testLenientDeserializeFromEmptyString() throws Exception {

        String key = "yearMonth";
        ObjectMapper mapper = newMapper();
        ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);

        String dateValAsEmptyStr = "";

        String valueFromNullStr = mapper.writeValueAsString(asMap(key, null));
        Map<String, YearMonth> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        YearMonth actualDateFromNullStr = actualMapFromNullStr.get(key);
        assertNull(actualDateFromNullStr);

        String valueFromEmptyStr = mapper.writeValueAsString(asMap(key, dateValAsEmptyStr));
        Map<String, YearMonth> actualMapFromEmptyStr = objectReader.readValue(valueFromEmptyStr);
        YearMonth actualDateFromEmptyStr = actualMapFromEmptyStr.get(key);
        assertNull(actualDateFromEmptyStr, "empty string failed to deserialize to null with lenient setting");
    }

    @Test
    public void testStrictDeserializeFromEmptyString() throws Exception {

        final String key = "YearMonth";
        final ObjectMapper mapper = mapperBuilder()
                .withConfigOverride(YearMonth.class,
                        o -> o.setFormat(JsonFormat.Value.forLeniency(false)))
                .build();
        final ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);

        String valueFromNullStr = mapper.writeValueAsString(asMap(key, null));
        Map<String, YearMonth> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        assertNull(actualMapFromNullStr.get(key));

        String valueFromEmptyStr = mapper.writeValueAsString(asMap("date", ""));
        assertThrows(MismatchedInputException.class, () -> objectReader.readValue(valueFromEmptyStr));
    }

    private YearMonth read(final String json) throws IOException {
        return READER.readValue(a2q(json));
    }
}
