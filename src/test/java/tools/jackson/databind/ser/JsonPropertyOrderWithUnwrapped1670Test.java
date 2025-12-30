package tools.jackson.databind.ser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tools.jackson.databind.testutil.JacksonTestUtilBase.a2q;

// https://github.com/FasterXML/jackson-databind/issues/1670
public class JsonPropertyOrderWithUnwrapped1670Test {

    @JsonPropertyOrder({"value4", "value3", "value2", "value1"})
    static class Issue1670
    {
        private int value1;
        private int value2;
        @JsonUnwrapped
        private Issue1670Child b;

        public Issue1670(int value1, int value2)
        {
            this.value1 = value1;
            this.value2 = value2;
            this.b = new Issue1670Child(3, 4);
        }
    }

    @JsonPropertyOrder({"value2", "value3", "value1", "value4"})
    static class JsonPropertyOrderTest
    {
        private int value1;
        private int value2;
        @JsonUnwrapped
        private Issue1670Child b;

        public JsonPropertyOrderTest(int value1, int value2)
        {
            this.value1 = value1;
            this.value2 = value2;
            this.b = new Issue1670Child(3, 4);
        }
    }

    static class Issue1670Child
    {
        private int value3;
        private int value4;

        public Issue1670Child(int value3, int value4)
        {
            this.value3 = value3;
            this.value4 = value4;
        }
    }

    @Test
    public void testSerialize1670() throws Exception
    {
        String json = a2q("{'value4':4,'value3':3,'value2':2,'value1':1}");
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultVisibility(vc ->
                        vc.withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY))
                .build();

        assertEquals(json, mapper.writeValueAsString(new Issue1670(1, 2)));
    }

    @Test
    public void testJsonPropertyOrderTest() throws Exception
    {
        String json = a2q("{'value2':2,'value3':3,'value1':1,'value4':4}");
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultVisibility(vc ->
                        vc.withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY))
                .build();

        assertEquals(json, mapper.writeValueAsString(new JsonPropertyOrderTest(1, 2)));
    }
}
