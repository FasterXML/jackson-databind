package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// https://github.com/FasterXML/jackson-databind/issues/1037
public class JsonIgnorePropertiesInheritance1037Test
    extends DatabindTestUtil
{
    @JsonIgnoreProperties(value = {"generated"}, allowGetters = true)
    static class BaseBean1037 {
        public String getGenerated() {
            return "http://bar.com";
        }
    }

    @JsonIgnoreProperties(value = {"computed"}, allowGetters = true)
    static class ReadOnlyBean1037 extends BaseBean1037 {
        public int getComputed() {
            return 32;
        }
    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    public void testReadOnlyProp() throws Exception
    {
        String json = MAPPER.writeValueAsString(new ReadOnlyBean1037());
        assertTrue(json.contains("generated"));
        assertTrue(json.contains("computed"));
        ReadOnlyBean1037 bean = MAPPER.readValue(json, ReadOnlyBean1037.class);
        assertNotNull(bean);
    }
}
