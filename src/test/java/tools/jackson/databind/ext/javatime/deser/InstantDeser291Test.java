package tools.jackson.databind.ext.javatime.deser;

import java.time.Instant;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

// [modules-java8#291] InstantDeserializer fails to parse negative numeric timestamp strings for
//   pre-1970 values.
public class InstantDeser291Test 
    extends DateTimeTestBase
{
    private final JsonMapper MAPPER = JsonMapper.builder()
        .defaultLocale(Locale.ENGLISH)
        .enable(DateTimeFeature.ALWAYS_ALLOW_STRINGIFIED_DATE_TIMESTAMPS)
        .build();
    private final ObjectReader READER = MAPPER.readerFor(Instant.class);

    private static final Instant INSTANT_3_SEC_AFTER_EPOC = Instant.ofEpochSecond(3);
    private static final Instant INSTANT_3_SEC_BEFORE_EPOC = Instant.ofEpochSecond(-3);

    private static final String STR_3_SEC = "\"3.000000000\"";
    private static final String STR_POSITIVE_3 = "\"+3.000000000\"";
    private static final String STR_NEGATIVE_3 = "\"-3.000000000\"";

    /**
     * Baseline that always succeeds, even before resolution of issue 291
     * @throws Exception
     */
    @Test
    public void testNormalNumericalString() throws Exception {
        assertEquals(INSTANT_3_SEC_AFTER_EPOC, READER.readValue(STR_3_SEC));
    }

    @Test
    public void testNegativeNumericalString() throws Exception {
        assertEquals(INSTANT_3_SEC_BEFORE_EPOC, READER.readValue(STR_NEGATIVE_3));
    }

    @Test
    public void testAllowedPlusSignNumericalString() throws Exception {
        assertEquals(INSTANT_3_SEC_AFTER_EPOC, READER.readValue(STR_POSITIVE_3));
    }
}
