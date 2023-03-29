package tools.jackson.databind.introspect;

import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.util.Collections;
import java.util.Map;

import static tools.jackson.databind.MapperFeature.ALLOW_IS_GETTERS_FOR_NON_BOOLEAN;

public class IsGetterBooleanTest extends BaseMapTest {

    static class POJO3609 {
        int isEnabled;

        protected POJO3609() { }
        public POJO3609(int b) {
            isEnabled = b;
        }

        public int isEnabled() { return isEnabled; }
        public void setEnabled(int b) { isEnabled = b; }
    }

    public void testAllowIntIsGetter() throws Exception
    {
        ObjectMapper MAPPER = jsonMapperBuilder()
                .enable(ALLOW_IS_GETTERS_FOR_NON_BOOLEAN)
                .build();

        POJO3609 input = new POJO3609(12);
        final String json = MAPPER.writeValueAsString(input);

        Map<?, ?> props = MAPPER.readValue(json, Map.class);
        assertEquals(Collections.singletonMap("enabled", 12),
                props);

        POJO3609 output = MAPPER.readValue(json, POJO3609.class);
        assertEquals(input.isEnabled, output.isEnabled);
    }

    public void testDisallowIntIsGetter() throws Exception
    {
        ObjectMapper MAPPER = jsonMapperBuilder()
                .disable(ALLOW_IS_GETTERS_FOR_NON_BOOLEAN)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();

        POJO3609 input = new POJO3609(12);
        final String json = MAPPER.writeValueAsString(input);

        assertEquals("{}", json);

    }
}
