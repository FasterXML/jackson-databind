package com.fasterxml.jackson.databind.ext;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

// Tests for `java.sql.Date`, `java.sql.Time` and `java.sql.Timestamp`
public class SqlDateSerializationTest extends BaseMapTest
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
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    @SuppressWarnings("deprecation")
    public void testSqlDate() throws IOException
    {
        // use date 1999-04-01 (note: months are 0-based, use constant)
        final java.sql.Date date99 = new java.sql.Date(99, Calendar.APRIL, 1);
        final java.sql.Date date0 = new java.sql.Date(0);

        // 11-Oct-2016, tatu: As per [databind#219] we really should use global
        //   defaults in 2.9, even if this changes behavior.

        assertEquals(String.valueOf(date99.getTime()),
                MAPPER.writeValueAsString(date99));

        assertEquals(a2q("{'date':0}"),
                MAPPER.writeValueAsString(new SqlDateAsDefaultBean(0L)));

        // but may explicitly force timestamp too
        assertEquals(a2q("{'date':0}"),
                MAPPER.writeValueAsString(new SqlDateAsNumberBean(0L)));

        // And also should be able to use String output as need be:
        ObjectWriter w = MAPPER.writer().without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        assertEquals(q("1999-04-01"), w.writeValueAsString(date99));
        assertEquals(q(date0.toString()), w.writeValueAsString(date0));
        assertEquals(a2q("{'date':'"+date0.toString()+"'}"),
                w.writeValueAsString(new SqlDateAsDefaultBean(0L)));
    }

    public void testSqlTime() throws IOException
    {
        java.sql.Time time = new java.sql.Time(0L);
        // not 100% sure what we should expect wrt timezone, but what serializes
        // does use is quite simple:
        assertEquals(q(time.toString()), MAPPER.writeValueAsString(time));
    }

    public void testSqlTimestamp() throws IOException
    {
        java.sql.Timestamp input = new java.sql.Timestamp(0L);
        // just should produce same output as standard `java.util.Date`:
        Date altTnput = new Date(0L);
        assertEquals(MAPPER.writeValueAsString(altTnput),
                MAPPER.writeValueAsString(input));
    }

    public void testPatternWithSqlDate() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // `java.sql.Date` applies system default zone (and not UTC)
        mapper.setTimeZone(TimeZone.getDefault());

        Person i = new Person();
        i.dateOfBirth = java.sql.Date.valueOf("1980-04-14");
        assertEquals(a2q("{'dateOfBirth':'1980.04.14'}"),
                mapper.writeValueAsString(i));
    }

    // [databind#2064]
    public void testSqlDateConfigOverride() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        mapper.setTimeZone(TimeZone.getDefault());
        mapper.configOverride(java.sql.Date.class)
            .setFormat(JsonFormat.Value.forPattern("yyyy+MM+dd"));
        assertEquals("\"1980+04+14\"",
            mapper.writeValueAsString(java.sql.Date.valueOf("1980-04-14")));
    }
}
