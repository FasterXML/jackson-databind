package com.fasterxml.jackson.databind.deser.jdk;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

/**
 * Additional `java.util.Date` deserialization tests for cases where `ObjectMapper`
 * is configured to use timezone different from UTC.
 */
public class DateDeserializationTZTest
    extends BaseMapTest
{
    private static final String LOCAL_TZ = "GMT+2";

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    static class Annot_TimeZone {
        @JsonFormat(timezone="GMT+4")
        java.util.Date date;
    }

    static class Annot_Pattern {
        @JsonFormat(pattern="'*'d MMM yyyy HH:mm:ss'*'")
        java.util.Date pattern;

        @JsonFormat(pattern="'*'d MMM yyyy HH:mm:ss'*'", locale="FR")
        java.util.Date pattern_FR;

        @JsonFormat(pattern="'*'d MMM yyyy HH:mm:ss'*'", timezone="GMT+4")
        java.util.Date pattern_GMT4;

        @JsonFormat(pattern="'*'d MMM yyyy HH:mm:ss'*'", locale="FR", timezone="GMT+4")
        java.util.Date pattern_FR_GMT4;
    }

    static class DateAsStringBean {
        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="/yyyy/MM/dd/")
        public Date date;
    }

    static class DateAsStringBeanGermany {
        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="/yyyy/MM/dd/", locale="fr_FR")
        public Date date;
    }

    private ObjectMapper MAPPER;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Create an ObjectMapper with its timezone set to something other than the default (UTC).
        // This way we can verify that serialization and deserialization actually consider the time
        // zone set on the mapper.
        ObjectMapper m = new ObjectMapper();
        m.setTimeZone(TimeZone.getTimeZone(LOCAL_TZ));
        MAPPER = m;

        FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    /**
     * Test the various timezone/offset representations
     */
    public void testDateUtilISO8601_Timezone() throws Exception
    {
        // Complete offset, with ':' between hours and minutes
        verify( MAPPER, "2000-01-02T03:04:05.678+01:00", judate(2000, 1, 2,   3, 4, 5, 678, "GMT+1"));
        // Complete offset, without ':' between hours and minutes
        verify( MAPPER, "2000-01-02T03:04:05.678+0100",  judate(2000, 1, 2,   3, 4, 5, 678, "GMT+1"));
        // Hour offset (no minutes)
        verify( MAPPER, "2000-01-02T03:04:05.678+01",    judate(2000, 1, 2,   3, 4, 5, 678, "GMT+1"));

        // 'zulu' offset
        verify( MAPPER, "2000-01-02T03:04:05.678Z",      judate(2000, 1, 2,   3, 4, 5, 678, "UTC"));

        // ---------------------------------------------------------------------------------------------
        // WARNING:
        //   According to ISO8601, hours and minutes of the offset must be expressed with 2 digits
        //   (not more, not less), i.e. Z or +hh:mm or -hh:mm. See https://www.w3.org/TR/NOTE-datetime.
        //
        //   The forms below are therefore ILLEGAL and must be refused.
        // ---------------------------------------------------------------------------------------------

        failure( MAPPER, "2000-01-02T03:04:05.678+");
        failure( MAPPER, "2000-01-02T03:04:05.678+1");

        failure( MAPPER, "2000-01-02T03:04:05.678+001");
        failure( MAPPER, "2000-01-02T03:04:05.678+00:");
        failure( MAPPER, "2000-01-02T03:04:05.678+00:001");
        failure( MAPPER, "2000-01-02T03:04:05.678+001:001");

        failure( MAPPER, "2000-01-02T03:04:05.678+1:");
        failure( MAPPER, "2000-01-02T03:04:05.678+00:1");
    }

    /**
     * Test the millis
     */
    public void testDateUtilISO8601_DateTimeMillis() throws Exception
    {
        // WITH timezone (from 4 to 0 digits)
    		failure(MAPPER, "2000-01-02T03:04:05.0123456789+01:00");	// at most 9 digits for the millis
        verify( MAPPER, "2000-01-02T03:04:05.6789+01:00", judate(2000, 1, 2,   3, 4, 5, 678, "GMT+1"));
        verify( MAPPER, "2000-01-02T03:04:05.678+01:00",  judate(2000, 1, 2,   3, 4, 5, 678, "GMT+1"));
        verify( MAPPER, "2000-01-02T03:04:05.67+01:00",   judate(2000, 1, 2,   3, 4, 5, 670, "GMT+1"));
        verify( MAPPER, "2000-01-02T03:04:05.6+01:00",    judate(2000, 1, 2,   3, 4, 5, 600, "GMT+1"));
        verify( MAPPER, "2000-01-02T03:04:05+01:00",      judate(2000, 1, 2,   3, 4, 5, 000, "GMT+1"));


        // WITH timezone Z (from 4 to 0 digits)
		failure(MAPPER, "2000-01-02T03:04:05.0123456789Z");	// at most 9 digits for the millis
        verify( MAPPER, "2000-01-02T03:04:05.6789Z",      judate(2000, 1, 2,   3, 4, 5, 678, "UTC"));
        verify( MAPPER, "2000-01-02T03:04:05.678Z",       judate(2000, 1, 2,   3, 4, 5, 678, "UTC"));
        verify( MAPPER, "2000-01-02T03:04:05.67Z",        judate(2000, 1, 2,   3, 4, 5, 670, "UTC"));
        verify( MAPPER, "2000-01-02T03:04:05.6Z",         judate(2000, 1, 2,   3, 4, 5, 600, "UTC"));
        verify( MAPPER, "2000-01-02T03:04:05Z",           judate(2000, 1, 2,   3, 4, 5,   0, "UTC"));


        // WITHOUT timezone (from 4 to 0 digits)
		failure(MAPPER, "2000-01-02T03:04:05.0123456789");	// at most 9 digits for the millis
        verify( MAPPER, "2000-01-02T03:04:05.6789",       judate(2000, 1, 2,   3, 4,  5, 678, LOCAL_TZ));
        verify( MAPPER, "2000-01-02T03:04:05.678",        judate(2000, 1, 2,   3, 4,  5, 678, LOCAL_TZ));
        verify( MAPPER, "2000-01-02T03:04:05.67",         judate(2000, 1, 2,   3, 4,  5, 670, LOCAL_TZ));
        verify( MAPPER, "2000-01-02T03:04:05.6",          judate(2000, 1, 2,   3, 4,  5, 600, LOCAL_TZ));
        verify( MAPPER, "2000-01-02T03:04:05",            judate(2000, 1, 2,   3, 4,  5, 000, LOCAL_TZ));


        // ---------------------------------------------------------------------------------------------
        // WARNING:
        //   RFC339 includes an Internet profile of the ISO 8601 standard for representation of dates
        //   and times using the Gregorian calendar (https://tools.ietf.org/html/rfc3339).
        //
        //   The RFC defines a partial time with the following BNF notation (chapter 5.6):
        //      time-hour       = 2DIGIT  ; 00-23
        //      time-minute     = 2DIGIT  ; 00-59
        //      time-second     = 2DIGIT  ; 00-58, 00-59, 00-60 based on leap second rules
        //      time-secfrac    = "." 1*DIGIT
        //      partial-time    = time-hour ":" time-minute ":" time-second [time-secfrac]
        //
        //   The second fraction (ie the millis) is optional and can be omitted. However, a fraction
        //   with only a dot (.) and no digit is not allowed.
        //
        //   The forms below are therefore ILLEGAL and must be refused.
        // ---------------------------------------------------------------------------------------------

        // millis part with only a dot (.) and no digits
        failure( MAPPER, "2000-01-02T03:04:05.+01:00");
        failure( MAPPER, "2000-01-02T03:04:05.");
        failure( MAPPER, "2000-01-02T03:04:05.Z");
    }


    /**
     * Date+Time representations
     *
     * NOTE: millis are not tested here since they are covered by another test case
     */
    public void testDateUtilISO8601_DateTime() throws Exception
    {
        // Full representation with a timezone
        verify(MAPPER, "2000-01-02T03:04:05+01:00",  judate(2000, 1, 2,   3, 4, 5, 0, "GMT+1"));

        // No timezone --> the one configured on the ObjectMapper must be used
        verify(MAPPER, "2000-01-02T03:04:05",        judate(2000, 1, 2,   3, 4, 5, 0, LOCAL_TZ));

        // Hours, minutes are mandatory but seconds are optional
        failure(MAPPER, "2000-01-02T");
        failure(MAPPER, "2000-01-02T03");
        failure(MAPPER, "2000-01-02T03:");
        verify(MAPPER, "2000-01-02T03:04", judate(2000, 1, 2,  3, 4, 0, 0, LOCAL_TZ));
        failure(MAPPER, "2000-01-02T03:04:");

        // Hours, minutes are mandatory but seconds are optional - test with a TZ
        failure(MAPPER, "2000-01-02T+01:00");
        failure(MAPPER, "2000-01-02T03+01:00");
        failure(MAPPER, "2000-01-02T03:+01:00");
        verify( MAPPER, "2000-01-02T03:04+01:00", judate(2000, 1, 2,   3, 4, 0, 0, "GMT+1"));
        failure(MAPPER, "2000-01-02T03:04:+01:00");

        failure(MAPPER, "2000-01-02TZ");
        failure(MAPPER, "2000-01-02T03Z");
        failure(MAPPER, "2000-01-02T03:Z");
        verify(MAPPER, "2000-01-02T03:04Z", judate(2000, 1, 2,  3, 4, 0, 0, "UTC"));
        failure(MAPPER, "2000-01-02T03:04:Z");


        // ---------------------------------------------------------------------------------------------
        // WARNING:
        //   ISO8601 (https://en.wikipedia.org/wiki/ISO_8601#Times) and its RFC339 profile
        //   (https://tools.ietf.org/html/rfc3339, chapter 5.6) seem to require 2 DIGITS for
        //   the hours, minutes and seconds.
        //
        //   The following forms should therefore be refused but are accepted by Jackson (and
        //   java.text.SimpleDateFormat). They are verified here to detect any changes in future
        //   releases.
        //
        // ---------------------------------------------------------------------------------------------

        // FIXME As highlighted in the tests below, the behaviour is not consistent and largely
        // depends on wether a timezone and or millis are specified or not.
        // The tests assert the behavior with different number of digits for hour, min and sec.
        // Behavior should be the SAME whatever the timezone and/or the millis.

        // seconds (no TZ)
        failure( MAPPER, "2000-01-02T03:04:5");
        failure( MAPPER, "2000-01-02T03:04:5.000");
        failure(MAPPER, "2000-01-02T03:04:005");

        // seconds (+01:00)
        failure(MAPPER, "2000-01-02T03:04:5+01:00");
        failure(MAPPER, "2000-01-02T03:04:5.000+01:00");
        failure(MAPPER, "2000-01-02T03:04:005+01:00");

        // seconds (Z)
        failure(MAPPER, "2000-01-02T03:04:5Z");
        failure( MAPPER, "2000-01-02T03:04:5.000Z");
        failure(MAPPER, "2000-01-02T03:04:005Z");


        // minutes (no TZ)
        failure( MAPPER, "2000-01-02T03:4:05");
        failure( MAPPER, "2000-01-02T03:4:05.000");
        failure(MAPPER, "2000-01-02T03:004:05");

        // minutes (+01:00)
        failure(MAPPER, "2000-01-02T03:4:05+01:00");
        failure(MAPPER, "2000-01-02T03:4:05.000+01:00");
        failure(MAPPER, "2000-01-02T03:004:05+01:00");

        // minutes (Z)
        failure( MAPPER, "2000-01-02T03:4:05Z");
        failure( MAPPER, "2000-01-02T03:4:05.000Z");
        failure( MAPPER, "2000-01-02T03:004:05Z");

        // hour (no TZ)
        failure( MAPPER, "2000-01-02T3:04:05");
        failure( MAPPER, "2000-01-02T3:04:05.000");
        failure(MAPPER, "2000-01-02T003:04:05");

        // hour (+01:00)
        failure(MAPPER, "2000-01-02T3:04:05+01:00");
        failure(MAPPER, "2000-01-02T3:04:05.000+01:00");
        failure(MAPPER, "2000-01-02T003:04:05+01:00");

        // hour (Z)
        failure( MAPPER, "2000-01-02T3:04:05Z");
        failure( MAPPER, "2000-01-02T3:04:05.000Z");
        failure( MAPPER, "2000-01-02T003:04:05Z");
    }

    /**
     * Date-only representations (no Time part)
     *
     * NOTE: time part is not tested here since they it is covered by another test case
     */
    public void testDateUtilISO8601_Date() throws Exception
    {
        // Date is constructed with the timezone of the ObjectMapper. Time part is set to zero.
        verify(MAPPER, "2000-01-02", judate(2000, 1, 2,   0, 0, 0, 0, LOCAL_TZ));


        // ---------------------------------------------------------------------------------------------
        // WARNING:
        //   ISO8601 (https://en.wikipedia.org/wiki/ISO_8601#Times) and its RFC339 profile
        //   (https://tools.ietf.org/html/rfc3339, chapter 5.6) seem to require 2 DIGITS for
        //   the month and dayofweek but 4 DIGITS for the year.
        //
        //   The following forms should therefore be refused but are accepted by Jackson (and
        //   java.text.SimpleDateFormat). They are verified here to detect any changes in future
        //   releases.
        // ---------------------------------------------------------------------------------------------

        // day
        failure( MAPPER, "2000-01-2");
        failure( MAPPER, "2000-01-002");

        // month
        failure( MAPPER, "2000-1-02");
        failure( MAPPER, "2000-001-02");

        // year
        failure( MAPPER, "20000-01-02");
        failure( MAPPER, "200-01-02"  );
        failure( MAPPER, "20-01-02"   );
        failure(  MAPPER, "2-01-02");
    }

    /**
     * DateTime as numeric representation
     */
    public void testDateUtil_Numeric() throws Exception
    {
        {
            long now = 123456789L;
            verify( MAPPER,                now, new java.util.Date(now) ); // as a long
            verify( MAPPER, Long.toString(now), new java.util.Date(now) ); // as a string
        }
        {
            // should be ok to pass as JSON String, as long
            // as it is plain timestamp (all numbers, 64-bit)
            long now = 1321992375446L;
            verify( MAPPER,                now, new java.util.Date(now) );	// as a long
            verify( MAPPER, Long.toString(now), new java.util.Date(now) );  // as a string
        }
        {
            // #267: should handle negative timestamps too; like 12 hours before 1.1.1970
            long now = - (24 * 3600 * 1000L);
            verify( MAPPER,                now, new java.util.Date(now) );	// as a long
            verify( MAPPER, Long.toString(now), new java.util.Date(now) );  // as a string
        }

        // value larger than a long (Long.MAX_VALUE+1)
        BigInteger tooLarge = BigInteger.valueOf(Long.MAX_VALUE).add( BigInteger.valueOf(1) );
        failure(MAPPER, tooLarge, InvalidFormatException.class);
        failure(MAPPER, tooLarge.toString(), InvalidFormatException.class);

        // decimal value
        failure(MAPPER, 0.0, MismatchedInputException.class);
        failure(MAPPER, "0.0", InvalidFormatException.class);
    }

    /**
     * Note: may be these cases are already covered by {@link #testDateUtil_Annotation_PatternAndLocale()}
     */
    public void testDateUtil_Annotation() throws Exception
    {
        // Build the input JSON and expected value
        String json = a2q("{'date':'/2005/05/25/'}");
        java.util.Date expected = judate(2005, 05, 25, 0, 0, 0, 0, LOCAL_TZ);


        // Read it to make sure the format specified by the annotation is taken into account
        {
            DateAsStringBean result = MAPPER.readValue(json, DateAsStringBean.class);
            assertNotNull(result);
            assertEquals( expected, result.date );
        }
        {
            DateAsStringBean result = MAPPER.readerFor(DateAsStringBean.class)
                    .with(Locale.GERMANY)
                    .readValue(json);
            assertNotNull(result);
            assertEquals( expected, result.date );
        }

        // or, via annotations
        {
            DateAsStringBeanGermany result = MAPPER.readerFor(DateAsStringBeanGermany.class)
                                                   .readValue(json);
            assertNotNull(result);
            assertEquals( expected, result.date );
        }
    }

    /**
     * Test a POJO annotated with @JsonFormat to force an pattern.
     * Alternate with different combination of Locale and TimeZone.
     */
    public void testDateUtil_Annotation_PatternAndLocale() throws Exception
    {
        // Change the default locale set on the ObjectMapper to something else than the default.
        // This way we know if the default is correctly taken into account
        ObjectMapper mapper = MAPPER.copy();
        mapper.setLocale( Locale.ITALY );

        // Build the JSON string. This is a mixed of ITALIAN and FRENCH (no ENGLISH because this
        // would be the default).
        String json = a2q("{ 'pattern': '*1 giu 2000 01:02:03*', 'pattern_FR': '*01 juin 2000 01:02:03*', 'pattern_GMT4': '*1 giu 2000 01:02:03*', 'pattern_FR_GMT4': '*1 juin 2000 01:02:03*'}");
        Annot_Pattern result = mapper.readValue(json, Annot_Pattern.class);

        assertNotNull(result);
        assertEquals( judate(2000, 6, 1, 1, 2, 3, 0, LOCAL_TZ), result.pattern        );
        assertEquals( judate(2000, 6, 1, 1, 2, 3, 0, LOCAL_TZ), result.pattern_FR     );
        assertEquals( judate(2000, 6, 1, 1, 2, 3, 0, "GMT+4"),  result.pattern_GMT4    );
        assertEquals( judate(2000, 6, 1, 1, 2, 3, 0, "GMT+4"),  result.pattern_FR_GMT4 );
    }

    /**
     * Test a POJO annotated with @JsonFormat to force a default TimeZone
     * other than the one set on the ObjectMapper when the JSON doesn't contain any.
     */
    public void testDateUtil_Annotation_TimeZone() throws Exception
    {
        // WITHOUT timezone
        {
            String json = a2q("{ 'date': '2000-01-02T03:04:05.678' }");
            Annot_TimeZone result = MAPPER.readValue(json, Annot_TimeZone.class);

            assertNotNull(result);
            assertEquals( judate(2000, 1, 2, 3, 4, 5, 678, "GMT+4"), result.date);
        }

        // WITH timezone
        //   --> the annotation acts as the "default" timezone. The timezone specified
        //       in the JSON should be considered first.
        {
            String json = a2q("{ 'date': '2000-01-02T03:04:05.678+01:00' }");
            Annot_TimeZone result = MAPPER.readValue(json, Annot_TimeZone.class);

            assertNotNull(result);
            assertEquals( judate(2000, 1, 2, 3, 4, 5, 678, "GMT+1"), result.date);
        }
    }

    /**
     * ObjectMapper configured with a custom date format that does NOT handle the TIMEZONE.
     * Dates must be constructed with the time zone set on the ObjectMapper.
     */
    public void testDateUtil_customDateFormat_withoutTZ() throws Exception
    {
        // FIXME
        //
        // The general rule with the StdDateFormat is:
        //     the TimeZone of the ObjectMapper is used if the JSON doesn't hold
        //     any timezone/offset information.
        //
        // This rule remains valid with the @JsonFormat annotation unless it forces
        // an explicit timezeone, in which case the latter takes precedence.
        //
        // One would expect the same behavior when the StdDateFormat is replaced by a
        // custom DateFormat on the ObjectMapper. In other words, the timezone of the
        // DateFormat is of no importance: the ObjectMapper's default should be used
        // whenever it is needed.


        // Test first with a non default TZ on the ObjectMapper
        // --> OK: the mapper's default TZ is used to parse the date.
        {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'X'HH:mm:ss");
            df.setTimeZone( TimeZone.getTimeZone("GMT+4") );    // TZ different from mapper's default

            ObjectMapper mapper = new ObjectMapper();
            mapper.setTimeZone( TimeZone.getTimeZone(LOCAL_TZ) );
            mapper.setDateFormat(df);

            // The mapper's default TZ is used...
            verify(mapper, "2000-01-02X04:00:00", judate(2000, 1, 2, 4, 00, 00, 00, LOCAL_TZ));
        }

        // Test a second time with the default TZ on the ObjectMapper
        // Note it is important NOT TO CALL mapper.setTimeZone(...) in this test..
        // --> KO: the custom format's TZ is used instead of the mapper's default as above.
        //
        {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'X'HH:mm:ss");
            df.setTimeZone( TimeZone.getTimeZone("GMT+4") );    // TZ different from mapper's default

            ObjectMapper mapper = new ObjectMapper();
            mapper.setDateFormat(df);

            // FIXME mapper's default TZ should have been used
            verify(mapper, "2000-01-02X04:00:00", judate(2000, 1, 2, 4, 00, 00, 00, "GMT+4"));
        }
    }

    /**
     * ObjectMapper configured with a custom date format that DOES handle the TIMEZONE.
     * Dates must be constructed from the timezone of the input, regardless of the one
     * of the ObjectMapper.
     */
    public void testDateUtil_customDateFormat_withTZ() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'X'HH:mm:ssZ");
        df.setTimeZone(TimeZone.getTimeZone("GMT+4"));    // use a timezone different than the ObjectMapper and the system default
        mapper.setDateFormat(df);

        verify(mapper, "2000-01-02X03:04:05+0300", judate(2000, 1, 2, 3, 4, 5, 00, "GMT+3"));
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * Create a {@link java.util.Date} with all the fields set to the given value.
     *
     * @param year year
     * @param month month (1-12)
     * @param day day of month (1-31)
     * @param hour hour (0-23)
     * @param minutes minutes (0-59)
     * @param seconds seconds (0-59)
     * @param millis millis
     * @param tz timezone id as accepted by {@link TimeZone#getTimeZone(String)}
     * @return a new {@link Date} instance
     */
    private static Date judate(int year, int month, int day, int hour, int minutes, int seconds, int millis, String tz)
    {
        Calendar cal = Calendar.getInstance();
        cal.setLenient(false);
        cal.set(year, month-1, day, hour, minutes, seconds);
        cal.set(Calendar.MILLISECOND, millis);
        cal.setTimeZone(TimeZone.getTimeZone(tz));
        return cal.getTime();
    }

    private static void verify(ObjectMapper mapper, Object input, Date expected) throws Exception {
		// Deserialize using the supplied ObjectMapper
		Date actual = read(mapper, input, java.util.Date.class);

		// Test against the expected
		if( expected==null && actual==null) {
			return;
		}
		if( expected==null && actual != null) {
			fail("Failed to deserialize "+input+", actual: '"+FORMAT.format(actual)+"', expected: <null>'");
		}
		if( expected != null && actual == null ) {
			fail("Failed to deserialize "+input+", actual: <null>, expected: '"+FORMAT.format(expected)+"'");
		}
		if( actual.getTime() != expected.getTime() ) {
			fail("Failed to deserialize "+input+", actual: '"+FORMAT.format(actual)+"', expected: '"+FORMAT.format(expected)+"'");
		}
    }

    private static void failure(ObjectMapper mapper, Object input) throws Exception {
        failure(mapper, input, MismatchedInputException.class);
    }

    private static void failure(ObjectMapper mapper, Object input, Class<? extends Exception> exceptionType) throws Exception {
        try {
            Date date = read(mapper, input, java.util.Date.class);
            fail("Input "+input+" should not have been accepted but was deserialized into "+FORMAT.format(date));
        } catch (Exception e) {
            // Is it the expected exception ?
            if (!exceptionType.isAssignableFrom(e.getClass()) ) {
                fail("Wrong exception thrown when reading "+input+", actual: "+e.getClass().getName() + "("+e.getMessage()+"), expected: "+exceptionType.getName());
            }
        }
    }

    private static <T> T read(ObjectMapper mapper, Object input, Class<T> type) throws Exception {
        // Construct the json representation from the input
        String json = input.toString();
        if( !(input instanceof Number) ) {
            json = "\""+json+"\"";
        }
        // Deserialize using the supplied ObjectMapper
        return (T) mapper.readValue(json, type);
    }
}
