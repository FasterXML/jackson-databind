package tools.jackson.databind.misc;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

public class Reflection4907Jackson3Test extends DatabindTestUtil
{
    static class SqlDatePojo {
        public String name;
        public java.sql.Date date;
        public List<String> tags;

        public SqlDatePojo() {
        }

        public SqlDatePojo(String name, java.sql.Date date, String... tags) {
            this.name = name;
            this.date = date;
            this.tags = Arrays.asList(tags);
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

        public List<String> getTags() {
            return tags;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void test4907ReadPojo() throws Exception {
System.err.println("<testReadPojo>");
        SqlDatePojo pojo = MAPPER.readValue(a2q("{'date':'2000-01-01', 'name':'foo'}"),
                SqlDatePojo.class);
System.err.println("</testReadPojo>");
        assertNotNull(pojo);
    }

    @Test
    public void test4907WritePojo() throws Exception {
System.err.println("<testWritePojo>");
         String json = MAPPER.writeValueAsString(new SqlDatePojo("foobar",
                 java.sql.Date.valueOf("2000-01-01"), "abc", "def"));
 System.err.println("</testWritePojo>");
         assertNotNull(json);
    }
    /*

    @Test
    public void test4907ReadTags() throws Exception {
System.err.println("<testReadTags>");
        List<?> tags = MAPPER.readValue(a2q("['abc', 'def']"),
                List.class);
System.err.println("</testReadTags>");
        assertNotNull(tags);
    }

    @Test
    public void test4907WriteTags() throws Exception {
System.err.println("<testWriteTags>");
         String json = MAPPER.writeValueAsString(Arrays.asList("abc", "def"));
 System.err.println("</testWriteTags>");
         assertNotNull(json);
    }
    */

    /*
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
    */
}
