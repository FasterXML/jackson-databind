/*
 * Copyright 2013 FasterXML.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package tools.jackson.databind.ext.javatime.deser;

import java.time.*;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.ext.javatime.DateTimeParseException;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.exc.MismatchedInputException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for proper wrapping of {@link java.time.DateTimeException} into
 * {@link DateTimeParseException} when deserializing java.time types.
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
            // Expected
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof java.time.DateTimeException);
            assertTrue(e.getMessage().contains("Failed to deserialize"));
            assertTrue(e.getMessage().contains("[2023,2,30,12,30]"));
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
            // Expected
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof java.time.DateTimeException);
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
            // Expected
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof java.time.DateTimeException);
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
            // Expected
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof java.time.DateTimeException);
            assertTrue(e.getMessage().contains("Failed to deserialize"));
            assertTrue(e.getMessage().contains("[2023,2,30]"));
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
            // Expected
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof java.time.DateTimeException);
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
            // Expected
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof java.time.DateTimeException);
            assertTrue(e.getMessage().contains("Failed to deserialize"));
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
            // Expected
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof java.time.DateTimeException);
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
            // Expected
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof java.time.DateTimeException);
            assertTrue(e.getMessage().contains("Failed to deserialize"));
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
            // Expected
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof java.time.DateTimeException);
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
            // Expected
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof java.time.DateTimeException);
            assertTrue(e.getMessage().contains("Failed to deserialize"));
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
            // Expected
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof java.time.DateTimeException);
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
            // Expected
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof java.time.DateTimeException);
            assertTrue(e.getMessage().contains("Failed to deserialize"));
        }
    }
}
