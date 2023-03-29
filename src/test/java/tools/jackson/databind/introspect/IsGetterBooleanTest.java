package tools.jackson.databind.introspect;

import java.util.Collections;
import java.util.Map;

import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

public class IsGetterBooleanTest extends BaseMapTest
{
    // [databind#3609]
    static class POJO3609 {
        int isEnabled;

        protected POJO3609() { }
        public POJO3609(int b) {
            isEnabled = b;
        }

        public int isEnabled() { return isEnabled; }
        public void setEnabled(int b) { isEnabled = b; }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    // [databind#3609]
    public void testAllowIntIsGetter() throws Exception
    {
        ObjectMapper MAPPER = jsonMapperBuilder()
                .enable(MapperFeature.ALLOW_IS_GETTERS_FOR_NON_BOOLEAN)
                .build();

        POJO3609 input = new POJO3609(12);
        final String json = MAPPER.writeValueAsString(input);

        Map<?, ?> props = MAPPER.readValue(json, Map.class);
        assertEquals(Collections.singletonMap("enabled", 12),
                props);

        POJO3609 output = MAPPER.readValue(json, POJO3609.class);
        assertEquals(input.isEnabled, output.isEnabled);
    }

    // [databind#3609]
    public void testDisallowIntIsGetter() throws Exception
    {
        ObjectMapper MAPPER = jsonMapperBuilder()
                .disable(MapperFeature.ALLOW_IS_GETTERS_FOR_NON_BOOLEAN)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();

        POJO3609 input = new POJO3609(12);
        final String json = MAPPER.writeValueAsString(input);

        assertEquals("{}", json);

    }
}
