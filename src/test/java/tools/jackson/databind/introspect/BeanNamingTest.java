package tools.jackson.databind.introspect;

import tools.jackson.databind.*;

// Tests for [databind#653]
public class BeanNamingTest extends BaseMapTest
{
    static class URLBean {
        public String getURL() {
            return "http:";
        }
    }

    static class ABean {
        public int getA() {
            return 3;
        }
    }

    // 24-Sep-2017, tatu: Used to test for `MapperFeature.USE_STD_BEAN_NAMING`, but with 3.x
    //    that is always enabled.
    public void testMultipleLeadingCapitalLetters() throws Exception
    {
        ObjectMapper mapper = objectMapper();
        assertEquals(a2q("{'URL':'http:'}"),
                mapper.writeValueAsString(new URLBean()));
        assertEquals(a2q("{'a':3}"),
                mapper.writeValueAsString(new ABean()));
    }
}
