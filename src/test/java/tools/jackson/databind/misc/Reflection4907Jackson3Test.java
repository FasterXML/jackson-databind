package tools.jackson.databind.misc;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Reflection4907Jackson3Test extends DatabindTestUtil
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

    /*
    @Test
    public void test4907ReadPojo() throws Exception {
System.err.println("<testRead>");
        SqlDatePojo pojo = MAPPER.readValue(a2q("{'date':'2000-01-01', 'name':'foo'}"),
                SqlDatePojo.class);
System.err.println("</testRead>");
        assertNotNull(pojo);
    }

    @Test
    public void test4907WritePojo() throws Exception {
System.err.println("<testWrite>");
         String json = MAPPER.writeValueAsString(new SqlDatePojo("foobar",
                 java.sql.Date.valueOf("2000-01-01")));
 System.err.println("</testWrite>");
         assertNotNull(json);
    }
    */

    @Test
    public void test4907ReadDate() throws Exception {
System.err.println("<testReadDate>");
        java.sql.Date date = MAPPER.readValue(a2q("'2000-01-01'"),
                java.sql.Date.class);
System.err.println("</testReadDate>");
        assertNotNull(date);
    }

    @Test
    public void test4907WriteDate() throws Exception {
System.err.println("<testWriteDate>");
         String json = MAPPER.writeValueAsString(java.sql.Date.valueOf("2000-01-01"));
 System.err.println("</testWriteDate>");
         assertNotNull(json);
    }
}
