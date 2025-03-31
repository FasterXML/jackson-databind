package tools.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// Tests for [databind#653]
public class BeanNamingTest extends DatabindTestUtil
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

    // [databind#2882]
    static class Bean2882 {
        // These should NOT be detected as "getters" due to naming conventions
        public boolean island() { return true; }
        public boolean is_bad() { return true; }

        public int get_value() { return -2; }
        public int getter() { return -3; }

        // This is regular and should be detected
        public int getX() { return 1; }

        // And bad "setter" too
        public void setter(int x) {
            throw new IllegalStateException("Should not get called");
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();
    
    // 24-Sep-2017, tatu: Used to test for `MapperFeature.USE_STD_BEAN_NAMING`, but with 3.x
    //    that is always enabled.
    @Test
    public void testMultipleLeadingCapitalLetters() throws Exception
    {
        assertEquals(a2q("{'URL':'http:'}"),
                MAPPER.writeValueAsString(new URLBean()));
        assertEquals(a2q("{'a':3}"),
                MAPPER.writeValueAsString(new ABean()));
    }

    // [databind#2882]
    @Test
    void testBadCasingForGetters() throws Exception
    {
        assertEquals(a2q("{'x':1}"),
                MAPPER.writeValueAsString(new Bean2882()));
    }

    @Test
    void testBadCasingForSetters() throws Exception
    {
        try {
            MAPPER.readerFor(Bean2882.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(a2q("{'ter':1}"));
            fail("Should not pass");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property \"ter\"");
        }
    }
}
