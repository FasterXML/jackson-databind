package tools.jackson.databind.format;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that {@code @JsonFormat} {@code locale} property
 * correctly handles compound locale strings (language + country),
 * such as {@code "de_DE"} or {@code "en-US"}.
 *<p>
 * See <a href="https://github.com/FasterXML/jackson-databind/issues/3316">[databind#3316]</a>.
 */
public class DateFormatLocale3316Test extends DatabindTestUtil
{
    // Bean using locale with underscore separator (e.g. "de_DE")
    static class DateWithLocaleUnderscore {
        @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "dd MMM yyyy",
                locale = "de_DE",
                timezone = "UTC")
        public Date value;
    }

    // Bean using locale with hyphen separator (e.g. "de-DE")
    static class DateWithLocaleHyphen {
        @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "dd MMM yyyy",
                locale = "de-DE",
                timezone = "UTC")
        public Date value;
    }

    // Bean using language-only locale (should still work as before)
    static class DateWithLocaleLanguageOnly {
        @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "dd MMM yyyy",
                locale = "de",
                timezone = "UTC")
        public Date value;
    }

    // Bean using Italian locale with variant
    static class DateWithLocaleVariant {
        @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "dd MMM yyyy",
                locale = "it_IT_POSIX",
                timezone = "UTC")
        public Date value;
    }

    // Bean using java.time.LocalDate with compound locale
    static class LocalDateWithLocale {
        @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "dd MMM yyyy",
                locale = "de_DE")
        public LocalDate value;
    }

    // Bean using French locale with underscore
    static class DateWithFrenchLocale {
        @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "dd MMMM yyyy",
                locale = "fr_FR",
                timezone = "UTC")
        public Date value;
    }

    private final static ObjectMapper MAPPER = newJsonMapper();

    // Use a known date: 2022-10-15 (15 October 2022)
    // In German, "Oktober" abbreviated is "Okt" (or "Okt." depending on JDK)
    // In French, "octobre" is the full month name

    private Date date20221015() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2022, Calendar.OCTOBER, 15);
        return cal.getTime();
    }

    // [databind#3316]: locale "de_DE" (underscore) should produce German month names
    @Test
    public void testSerializationWithUnderscoreLocale() throws Exception
    {
        DateWithLocaleUnderscore input = new DateWithLocaleUnderscore();
        input.value = date20221015();
        String json = MAPPER.writeValueAsString(input);

        // Verify it uses German locale — month abbreviation should be "Okt" (or "Okt."),
        // not English "Oct"
        assertTrue(json.contains("Okt"),
                "Should contain German month abbreviation 'Okt', got: " + json);
    }

    // [databind#3316]: locale "de-DE" (hyphen) should also produce German month names
    @Test
    public void testSerializationWithHyphenLocale() throws Exception
    {
        DateWithLocaleHyphen input = new DateWithLocaleHyphen();
        input.value = date20221015();
        String json = MAPPER.writeValueAsString(input);

        assertTrue(json.contains("Okt"),
                "Should contain German month abbreviation 'Okt', got: " + json);
    }

    // [databind#3316]: language-only locale should continue to work
    @Test
    public void testSerializationWithLanguageOnlyLocale() throws Exception
    {
        DateWithLocaleLanguageOnly input = new DateWithLocaleLanguageOnly();
        input.value = date20221015();
        String json = MAPPER.writeValueAsString(input);

        assertTrue(json.contains("Okt"),
                "Should contain German month abbreviation 'Okt', got: " + json);
    }

    // [databind#3316]: round-trip with "de_DE" locale (underscore)
    @Test
    public void testRoundTripWithCompoundLocale() throws Exception
    {
        DateWithLocaleUnderscore input = new DateWithLocaleUnderscore();
        input.value = date20221015();

        String json = MAPPER.writeValueAsString(input);
        DateWithLocaleUnderscore result = MAPPER.readValue(json, DateWithLocaleUnderscore.class);

        assertNotNull(result.value);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(result.value);
        assertEquals(2022, cal.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH));
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    }

    // [databind#3316]: locale with variant ("it_IT_POSIX") should work
    @Test
    public void testSerializationWithLocaleVariant() throws Exception
    {
        DateWithLocaleVariant input = new DateWithLocaleVariant();
        input.value = date20221015();
        String json = MAPPER.writeValueAsString(input);

        // Italian abbreviation for October is "ott" (or "ott.")
        assertTrue(json.toLowerCase(Locale.ROOT).contains("ott"),
                "Should contain Italian month abbreviation 'ott', got: " + json);
    }

    // [databind#3316]: java.time.LocalDate with compound locale should also work
    @Test
    public void testLocalDateWithCompoundLocale() throws Exception
    {
        LocalDateWithLocale input = new LocalDateWithLocale();
        input.value = LocalDate.of(2022, 10, 15);

        String json = MAPPER.writeValueAsString(input);
        assertTrue(json.contains("Okt"),
                "Should contain German month abbreviation 'Okt', got: " + json);

        LocalDateWithLocale result = MAPPER.readValue(json, LocalDateWithLocale.class);
        assertEquals(LocalDate.of(2022, 10, 15), result.value);
    }

    // [databind#3316]: French locale "fr_FR" should produce French month names
    @Test
    public void testSerializationWithFrenchLocale() throws Exception
    {
        DateWithFrenchLocale input = new DateWithFrenchLocale();
        input.value = date20221015();
        String json = MAPPER.writeValueAsString(input);

        // French full month name for October is "octobre"
        assertTrue(json.toLowerCase(Locale.ROOT).contains("octobre"),
                "Should contain French month name 'octobre', got: " + json);
    }

    // [databind#3316]: deserialization with "fr_FR" locale
    @Test
    public void testDeserializationWithFrenchLocale() throws Exception
    {
        String json = a2q("{'value':'15 octobre 2022'}");
        DateWithFrenchLocale result = MAPPER.readValue(json, DateWithFrenchLocale.class);

        assertNotNull(result.value);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(result.value);
        assertEquals(2022, cal.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH));
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    }
}
