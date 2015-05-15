package com.fasterxml.jackson.databind.util;

import java.util.*;
import java.text.ParsePosition;
import java.text.ParseException;

/**
 * Utilities methods for manipulating dates in iso8601 format. This is much much faster and GC friendly than using SimpleDateFormat so
 * highly suitable if you (un)serialize lots of date objects.
 * 
 * Supported parse format: [yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.sss]]|hhmm[ss[.sss]])]?[Z|[+-]hh:mm]]
 * 
 * @see <a href="http://www.w3.org/TR/NOTE-datetime">this specification</a>
 */
public class ISO8601Utils
{
    /**
     * ID to represent the 'GMT' string
     */
    private static final String GMT_ID = "GMT";

    /**
     * The GMT timezone
     */
    private static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone(GMT_ID);

    /*
    /**********************************************************
    /* Static factories
    /**********************************************************
     */

    /**
     * Accessor for static GMT timezone instance.
     */
    public static TimeZone timeZoneGMT() {
        return TIMEZONE_GMT;
    }

    /*
    /**********************************************************
    /* Formatting
    /**********************************************************
     */

    /**
     * Format a date into 'yyyy-MM-ddThh:mm:ssZ' (GMT timezone, no milliseconds precision)
     * 
     * @param date the date to format
     * @return the date formatted as 'yyyy-MM-ddThh:mm:ssZ'
     */
    public static String format(Date date) {
        return format(date, false, TIMEZONE_GMT);
    }

    /**
     * Format a date into 'yyyy-MM-ddThh:mm:ss[.sss]Z' (GMT timezone)
     * 
     * @param date the date to format
     * @param millis true to include millis precision otherwise false
     * @return the date formatted as 'yyyy-MM-ddThh:mm:ss[.sss]Z'
     */
    public static String format(Date date, boolean millis) {
        return format(date, millis, TIMEZONE_GMT);
    }

    /**
     * Format date into yyyy-MM-ddThh:mm:ss[.sss][Z|[+-]hh:mm]
     * 
     * @param date the date to format
     * @param millis true to include millis precision otherwise false
     * @param tz timezone to use for the formatting (GMT will produce 'Z')
     * @return the date formatted as yyyy-MM-ddThh:mm:ss[.sss][Z|[+-]hh:mm]
     */
    public static String format(Date date, boolean millis, TimeZone tz) {
        Calendar calendar = new GregorianCalendar(tz, Locale.US);
        calendar.setTime(date);

        // estimate capacity of buffer as close as we can (yeah, that's pedantic ;)
        int capacity = "yyyy-MM-ddThh:mm:ss".length();
        capacity += millis ? ".sss".length() : 0;
        capacity += tz.getRawOffset() == 0 ? "Z".length() : "+hh:mm".length();
        StringBuilder formatted = new StringBuilder(capacity);

        padInt(formatted, calendar.get(Calendar.YEAR), "yyyy".length());
        formatted.append('-');
        padInt(formatted, calendar.get(Calendar.MONTH) + 1, "MM".length());
        formatted.append('-');
        padInt(formatted, calendar.get(Calendar.DAY_OF_MONTH), "dd".length());
        formatted.append('T');
        padInt(formatted, calendar.get(Calendar.HOUR_OF_DAY), "hh".length());
        formatted.append(':');
        padInt(formatted, calendar.get(Calendar.MINUTE), "mm".length());
        formatted.append(':');
        padInt(formatted, calendar.get(Calendar.SECOND), "ss".length());
        if (millis) {
            formatted.append('.');
            padInt(formatted, calendar.get(Calendar.MILLISECOND), "sss".length());
        }

        int offset = tz.getOffset(calendar.getTimeInMillis());
        if (offset != 0) {
            int hours = Math.abs((offset / (60 * 1000)) / 60);
            int minutes = Math.abs((offset / (60 * 1000)) % 60);
            formatted.append(offset < 0 ? '-' : '+');
            padInt(formatted, hours, "hh".length());
            formatted.append(':');
            padInt(formatted, minutes, "mm".length());
        } else {
            formatted.append('Z');
        }

        return formatted.toString();
    }

    /*
    /**********************************************************
    /* Parsing
    /**********************************************************
     */

    /**
     * Parse a date from ISO-8601 formatted string. It expects a format
     * [yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.sss]]|hhmm[ss[.sss]])]?[Z|[+-]hh:mm]]
     * 
     * @param date ISO string to parse in the appropriate format.
     * @param pos The position to start parsing from, updated to where parsing stopped.
     * @return the parsed date
     * @throws ParseException if the date is not in the appropriate format
     */
    public static Date parse(String date, ParsePosition pos) throws ParseException {
        Exception fail = null;
        try {
            int offset = pos.getIndex();

            // extract year
            int year = parseInt(date, offset, offset += 4);
            if (checkOffset(date, offset, '-')) {
                offset += 1;
            }

            // extract month
            int month = parseInt(date, offset, offset += 2);
            if (checkOffset(date, offset, '-')) {
                offset += 1;
            }

            // extract day
            int day = parseInt(date, offset, offset += 2);
            // default time value
            int hour = 0;
            int minutes = 0;
            int seconds = 0;
            int milliseconds = 0; // always use 0 otherwise returned date will include millis of current time
            if (checkOffset(date, offset, 'T')) {

                // extract hours, minutes, seconds and milliseconds
                hour = parseInt(date, offset += 1, offset += 2);
                if (checkOffset(date, offset, ':')) {
                    offset += 1;
                }

                minutes = parseInt(date, offset, offset += 2);
                if (checkOffset(date, offset, ':')) {
                    offset += 1;
                }
                // second and milliseconds can be optional
                if (date.length() > offset) {
                    char c = date.charAt(offset);
                    if (c != 'Z' && c != '+' && c != '-') {
                        seconds = parseInt(date, offset, offset += 2);
                        // milliseconds can be optional in the format
                        if (checkOffset(date, offset, '.')) {
                            milliseconds = parseInt(date, offset += 1, offset += 3);
                        }
                    }
                }
            }

            // extract timezone
            String timezoneId;
            if (date.length() <= offset) {
                throw new IllegalArgumentException("No time zone indicator");
            }
            char timezoneIndicator = date.charAt(offset);
            if (timezoneIndicator == '+' || timezoneIndicator == '-') {
                String timezoneOffset = date.substring(offset);
                timezoneId = GMT_ID + timezoneOffset;
                offset += timezoneOffset.length();
            } else if (timezoneIndicator == 'Z') {
                timezoneId = GMT_ID;
                offset += 1;
            } else {
                throw new IndexOutOfBoundsException("Invalid time zone indicator " + timezoneIndicator);
            }

            TimeZone timezone = TimeZone.getTimeZone(timezoneId);
            String act = timezone.getID();
            if (!act.equals(timezoneId)) {
                /* 22-Jan-2015, tatu: Looks like canonical version has colons, but we may be given
                 *    one without. If so, don't sweat.
                 *   Yes, very inefficient. Hopefully not hit often.
                 *   If it becomes a perf problem, add 'loose' comparison instead.
                 */
                String cleaned = act.replace(":", "");
                if (!cleaned.equals(timezoneId)) {
                    throw new IndexOutOfBoundsException("Mismatching time zone indicator: "+timezoneId+" given, resolves to "
                            +timezone.getID());
                }
            }

            Calendar calendar = new GregorianCalendar(timezone);
            calendar.setLenient(false);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minutes);
            calendar.set(Calendar.SECOND, seconds);
            calendar.set(Calendar.MILLISECOND, milliseconds);

            pos.setIndex(offset);
            return calendar.getTime();
            // If we get a ParseException it'll already have the right message/offset.
            // Other exception types can convert here.
        } catch (IndexOutOfBoundsException e) {
            fail = e;
        } catch (NumberFormatException e) {
            fail = e;
        } catch (IllegalArgumentException e) {
            fail = e;
        }
        String input = (date == null) ? null : ('"' + date + "'");
        String msg = fail.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = "("+fail.getClass().getName()+")";
        }
        ParseException ex = new ParseException("Failed to parse date [" + input + "]: " + msg, pos.getIndex());
        ex.initCause(fail);
        throw ex;
    }

    /**
     * Check if the expected character exist at the given offset in the value.
     * 
     * @param value the string to check at the specified offset
     * @param offset the offset to look for the expected character
     * @param expected the expected character
     * @return true if the expected character exist at the given offset
     */
    private static boolean checkOffset(String value, int offset, char expected) {
        return (offset < value.length()) && (value.charAt(offset) == expected);
    }

    /**
     * Parse an integer located between 2 given offsets in a string
     * 
     * @param value the string to parse
     * @param beginIndex the start index for the integer in the string
     * @param endIndex the end index for the integer in the string
     * @return the int
     * @throws NumberFormatException if the value is not a number
     */
    private static int parseInt(String value, int beginIndex, int endIndex) throws NumberFormatException {
        if (beginIndex < 0 || endIndex > value.length() || beginIndex > endIndex) {
            throw new NumberFormatException(value);
        }
        // use same logic as in Integer.parseInt() but less generic we're not supporting negative values
        int i = beginIndex;
        int result = 0;
        int digit;
        if (i < endIndex) {
            digit = Character.digit(value.charAt(i++), 10);
            if (digit < 0) {
                throw new NumberFormatException("Invalid number: " + value);
            }
            result = -digit;
        }
        while (i < endIndex) {
            digit = Character.digit(value.charAt(i++), 10);
            if (digit < 0) {
                throw new NumberFormatException("Invalid number: " + value);
            }
            result *= 10;
            result -= digit;
        }
        return -result;
    }

    /**
     * Zero pad a number to a specified length
     * 
     * @param buffer buffer to use for padding
     * @param value the integer value to pad if necessary.
     * @param length the length of the string we should zero pad
     */
    private static void padInt(StringBuilder buffer, int value, int length) {
        String strValue = Integer.toString(value);
        for (int i = length - strValue.length(); i > 0; i--) {
            buffer.append('0');
        }
        buffer.append(strValue);
    }
}
