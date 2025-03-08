package tools.jackson.databind.ext.sql;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Tests for `java.sql.Date`, `java.sql.Time` and `java.sql.Timestamp`
public class SqlDateSerializationTest extends DatabindTestUtil
{
    static class SqlDateAsDefaultBean {
        public java.sql.Date date;
        public SqlDateAsDefaultBean(long l) { date = new java.sql.Date(l); }
    }

    static class SqlDateAsNumberBean {
        @JsonFormat(shape=JsonFormat.Shape.NUMBER)
        public java.sql.Date date;
        public SqlDateAsNumberBean(long l) { date = new java.sql.Date(l); }
    }

    // for [databind#1407]
    static class Person {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
        public java.sql.Date dateOfBirth;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSqlDate() throws Exception
    {
        ObjectWriter writer = MAPPER.writer()
                .with(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // use date 1999-04-01 (note: months are 0-based, use constant)
        final java.sql.Date date99 = javaSqlDate(1999, Calendar.APRIL, 1);
        final java.sql.Date date0 = new java.sql.Date(0);

        // 11-Oct-2016, tatu: As per [databind#219] we really should use global
        //   defaults in 2.9, even if this changes behavior.

        assertEquals(String.valueOf(date99.getTime()),
                writer.writeValueAsString(date99));

        assertEquals(a2q("{'date':0}"),
                writer.writeValueAsString(new SqlDateAsDefaultBean(0L)));

        // but may explicitly force timestamp too
        assertEquals(a2q("{'date':0}"),
                writer.writeValueAsString(new SqlDateAsNumberBean(0L)));

        // And also should be able to use String output as need be:
        ObjectWriter w = MAPPER.writer().without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 03-Feb-2021, tatu: As per [databind#2405], changed to include time part by
        //   default
        assertEquals(q("1999-04-01T00:00:00.000Z"), w.writeValueAsString(date99));
        assertEquals(q("1970-01-01T00:00:00.000Z"), w.writeValueAsString(date0));
        assertEquals(a2q("{'date':'1970-01-01T00:00:00.000Z'}"),
                w.writeValueAsString(new SqlDateAsDefaultBean(0L)));
    }

    @Test
    public void testSqlTime() throws Exception
    {
        java.sql.Time time = new java.sql.Time(0L);
        // not 100% sure what we should expect wrt timezone, but what serializes
        // does use is quite simple:
        assertEquals(q(time.toString()), MAPPER.writeValueAsString(time));
    }

    @Test
    public void testSqlTimestamp() throws Exception
    {
        java.sql.Timestamp input = new java.sql.Timestamp(0L);
        // just should produce same output as standard `java.util.Date`:
        Date altTnput = new Date(0L);
        assertEquals(MAPPER.writeValueAsString(altTnput),
                MAPPER.writeValueAsString(input));
    }

    @Test
    public void testPatternWithSqlDate() throws Exception
    {
        // `java.sql.Date` applies system default zone (and not UTC)
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultTimeZone(TimeZone.getDefault())
                .build();

        Person i = new Person();
        i.dateOfBirth = java.sql.Date.valueOf("1980-04-14");
        assertEquals(a2q("{'dateOfBirth':'1980.04.14'}"),
                mapper.writeValueAsString(i));
    }

    // [databind#2064]
    @Test
    public void testSqlDateConfigOverride() throws Exception
    {
        // `java.sql.Date` applies system default zone (and not UTC)
        final ObjectMapper mapper = jsonMapperBuilder()
                .defaultTimeZone(TimeZone.getDefault())
                .withConfigOverride(java.sql.Date.class,
                        o -> o.setFormat(JsonFormat.Value.forPattern("yyyy+MM+dd")))
                .build();
        assertEquals("\"1980+04+14\"",
            mapper.writeValueAsString(java.sql.Date.valueOf("1980-04-14")));
    }

    private static java.sql.Date javaSqlDate(int year, int monthConstant, int day)
    {
        Calendar cal = Calendar.getInstance();
        cal.set(year, monthConstant, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        return new java.sql.Date(cal.getTime().getTime());
    }
}
