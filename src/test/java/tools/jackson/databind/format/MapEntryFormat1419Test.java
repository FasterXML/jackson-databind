package tools.jackson.databind.format;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [databind#1419]
public class MapEntryFormat1419Test extends DatabindTestUtil {
    static class BeanWithMapEntryAsPOJO {
        @JsonFormat(shape = JsonFormat.Shape.POJO)
        public Map.Entry<String, String> entry;

        protected BeanWithMapEntryAsPOJO() {
        }

        public BeanWithMapEntryAsPOJO(String key, String value) {
            Map<String, String> map = new HashMap<>();
            map.put(key, value);
            entry = map.entrySet().iterator().next();
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void wrappedAsObjectRoundtrip() throws Exception {
        BeanWithMapEntryAsPOJO input = new BeanWithMapEntryAsPOJO("foo", "bar");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'entry':{'key':'foo','value':'bar'}}"), json);
        BeanWithMapEntryAsPOJO result = MAPPER.readValue(json, BeanWithMapEntryAsPOJO.class);
        assertEquals("foo", result.entry.getKey());
        assertEquals("bar", result.entry.getValue());
    }
}
