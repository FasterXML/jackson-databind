package tools.jackson.databind.ext.javatime.deser;

import java.time.*;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.ext.javatime.DateTimeParseException;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for proper wrapping of {@link DateTimeException} and
 * {@link ArithmeticException} into {@link DateTimeParseException} when deserializing
 * java.time types.
 */
public class DateTimeExceptionHandlingTest extends DateTimeTestBase
{
    private final ObjectMapper MAPPER = newMapper();

    /*
    /**********************************************************
    /* Test for LocalDateTime - invalid date
    /**********************************************************
     */

    @Test
    public void testLocalDateTimeInvalidDate() throws Exception
    {
        // February 30 is invalid
        ObjectReader r = MAPPER.readerFor(LocalDateTime.class);
        try {
            r.readValue("[2023,2,30,12,30]");
            fail("Should not pass with invalid date");
        } catch (DateTimeParseException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
            assertThat(e).hasMessageContaining("Failed to deserialize");
            assertThat(e).hasMessageContaining("[2023,2,30,12,30]");
        }
    }

    @Test
    public void testLocalDateTimeInvalidMonth() throws Exception
    {
        // Month 13 is invalid
        ObjectReader r = MAPPER.readerFor(LocalDateTime.class);
        try {
            r.readValue("[2023,13,15,12,30]");
            fail("Should not pass with invalid month");
        } catch (DateTimeParseException e) {
             assertInstanceOf(DateTimeException.class, e.getCause());
        }
    }

    @Test
    public void testLocalDateTimeInvalidTime() throws Exception
    {
        // Hour 25 is invalid
        ObjectReader r = MAPPER.readerFor(LocalDateTime.class);
        try {
            r.readValue("[2023,2,15,25,30]");
            fail("Should not pass with invalid hour");
        } catch (DateTimeParseException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
        }
    }

    @Test
    public void testLocalDateTimeInvalidDateString() throws Exception
    {
        // February 30 is invalid - string format
        ObjectReader r = MAPPER.readerFor(LocalDateTime.class);
        try {
            r.readValue("\"2025-02-30T12:00:00\"");
            fail("Should not pass with invalid date string");
        } catch (DatabindException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
        }
    }

    /*
    /**********************************************************
    /* Test for LocalDate - invalid date
    /**********************************************************
     */

    @Test
    public void testLocalDateInvalidDate() throws Exception
    {
        // February 30 is invalid
        ObjectReader r = MAPPER.readerFor(LocalDate.class);
        try {
            r.readValue("[2023,2,30]");
            fail("Should not pass with invalid date");
        } catch (DateTimeParseException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
            assertThat(e).hasMessageContaining("Failed to deserialize");
            assertThat(e).hasMessageContaining("[2023,2,30]");
        }
    }

    @Test
    public void testLocalDateInvalidMonth() throws Exception
    {
        // Month 0 is invalid
        ObjectReader r = MAPPER.readerFor(LocalDate.class);
        try {
            r.readValue("[2023,0,15]");
            fail("Should not pass with invalid month");
        } catch (DateTimeParseException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
        }
    }

    @Test
    public void testLocalDateInvalidDateString() throws Exception
    {
        // February 30 is invalid - string format
        ObjectReader r = MAPPER.readerFor(LocalDate.class);
        try {
            r.readValue("\"2025-02-30\"");
            fail("Should not pass with invalid date string");
        } catch (DatabindException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
        }
    }

    /*
    /**********************************************************
    /* Test for LocalTime - invalid time
    /**********************************************************
     */

    @Test
    public void testLocalTimeInvalidHour() throws Exception
    {
        // Hour 25 is invalid
        ObjectReader r = MAPPER.readerFor(LocalTime.class);
        try {
            r.readValue("[25,30]");
            fail("Should not pass with invalid hour");
        } catch (DateTimeParseException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
            assertThat(e).hasMessageContaining("Failed to deserialize");
        }
    }

    @Test
    public void testLocalTimeInvalidMinute() throws Exception
    {
        // Minute 60 is invalid
        ObjectReader r = MAPPER.readerFor(LocalTime.class);
        try {
            r.readValue("[12,60]");
            fail("Should not pass with invalid minute");
        } catch (DateTimeParseException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
        }
    }

    @Test
    public void testLocalTimeInvalidMinuteString() throws Exception
    {
        // Minute 69 is invalid - string format
        ObjectReader r = MAPPER.readerFor(LocalTime.class);
        try {
            r.readValue("\"12:69:00\"");
            fail("Should not pass with invalid minute string");
        } catch (DatabindException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
        }
    }

    /*
    /**********************************************************
    /* Test for MonthDay - invalid date
    /**********************************************************
     */

    @Test
    public void testMonthDayInvalidDate() throws Exception
    {
        // February 30 is invalid
        ObjectReader r = MAPPER.readerFor(MonthDay.class);
        try {
            r.readValue("[2,30]");
            fail("Should not pass with invalid date for MonthDay");
        } catch (DateTimeParseException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
            assertThat(e).hasMessageContaining("Failed to deserialize");
        }
    }

    @Test
    public void testMonthDayInvalidMonth() throws Exception
    {
        // Month 13 is invalid
        ObjectReader r = MAPPER.readerFor(MonthDay.class);
        try {
            r.readValue("[13,15]");
            fail("Should not pass with invalid month");
        } catch (DateTimeParseException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
        }
    }

    /*
    /**********************************************************
    /* Test for YearMonth - invalid values
    /**********************************************************
     */

    @Test
    public void testYearMonthInvalidMonth() throws Exception
    {
        // Month 0 is invalid
        ObjectReader r = MAPPER.readerFor(YearMonth.class);
        try {
            r.readValue("[2023,0]");
            fail("Should not pass with invalid month");
        } catch (DateTimeParseException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
            assertThat(e).hasMessageContaining("Failed to deserialize");
        }
    }

    @Test
    public void testYearMonthInvalidMonth13() throws Exception
    {
        // Month 13 is invalid
        ObjectReader r = MAPPER.readerFor(YearMonth.class);
        try {
            r.readValue("[2023,13]");
            fail("Should not pass with invalid month");
        } catch (DateTimeParseException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
        }
    }

    /*
    /**********************************************************
    /* Test for Year - invalid year
    /**********************************************************
     */

    @Test
    public void testYearOutOfRange() throws Exception
    {
        // Year value outside valid range
        ObjectReader r = MAPPER.readerFor(Year.class);
        try {
            r.readValue("1000000000");
            fail("Should not pass with out of range year");
        } catch (DateTimeParseException e) {
            assertInstanceOf(DateTimeException.class, e.getCause());
            assertThat(e).hasMessageContaining("Failed to deserialize");
        }
    }

    /*
    /**********************************************************
    /* Test for Duration - arithmetic exceptions
    /**********************************************************
     */

    @Test
    public void testDurationFromLargeDecimal() throws Exception
    {
        // Very large decimal that could cause ArithmeticException
        ObjectReader r = MAPPER.readerFor(Duration.class);
        try {
            // Using a decimal with extremely large scale that could trigger ArithmeticException
            r.readValue("7E2147483647");
            fail("Should not pass with extreme decimal value");
        } catch (DateTimeParseException e) {
            assertInstanceOf(ArithmeticException.class, e.getCause());
            assertThat(e).hasMessageContaining("Failed to deserialize");
        }
    }

    @Test
    public void testInstantFromLargeDecimal() throws Exception
    {
        // Very large decimal that could cause ArithmeticException  
        ObjectReader r = MAPPER.readerFor(Instant.class);
        try {
            // Using a decimal with extremely large scale that could trigger ArithmeticException
            r.readValue("7E2147483647");
            fail("Should not pass with extreme decimal value");
        } catch (DateTimeParseException e) {
            assertInstanceOf(ArithmeticException.class, e.getCause());
            assertThat(e).hasMessageContaining("Failed to deserialize");
        }
    }
}
