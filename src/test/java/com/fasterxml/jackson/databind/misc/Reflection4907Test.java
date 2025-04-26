package com.fasterxml.jackson.databind.misc;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Reflection4907Test extends DatabindTestUtil
{
    static class SqlDatePojo {
        public String name;
        public java.sql.Date date;

        public SqlDatePojo() {
        }

        public SqlDatePojo(String name, java.sql.Date date) {
            this.name = name;
            this.date = date;
        }
        
        public SqlDatePojo(java.sql.Date date) {
            this.date = date;
        }

        public java.sql.Date getDate() {
            return date;
        }

        public void setDate(java.sql.Date date) {
            this.date = date;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#4907]
    @Test
    public void test4907Read() throws Exception {
        SqlDatePojo pojo = MAPPER.readValue(a2q("{'date':'2000-01-01', 'name':'foo'}"),
                SqlDatePojo.class);
        assertNotNull(pojo);
    }

    // [databind#4907]
    @Test
    public void test4907Write() throws Exception {
         String json = MAPPER.writeValueAsString(new SqlDatePojo("foobar",
                 java.sql.Date.valueOf("2000-01-01")));
         assertNotNull(json);
    }
}
