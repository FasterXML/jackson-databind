package tools.jackson.databind.ser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonInclude4741Test
    extends DatabindTestUtil
{
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class MyString {
        public String value = null;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void testSerialization() throws Exception
    {
        MyString input = new MyString();
        input.value = "";

        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'value':''}"), json);

        MyString output = MAPPER.readValue(json, MyString.class);

        assertEquals(input.value, output.value);
    }
}
