package tools.jackson.databind.deser.jdk;

import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class MapRawWithGeneric2846Test
{
    @SuppressWarnings("rawtypes")
    static class GenericEntity<T> {
        public Map map;
    }

    static class SimpleEntity {
        public Integer number;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testIssue2821Part2() throws Exception {
        final String JSON = "{ \"map\": { \"key\": \"value\" } }";
        GenericEntity<SimpleEntity> genericEntity = MAPPER.readValue(JSON,
                new TypeReference<GenericEntity<SimpleEntity>>() {});
        assertNotNull(genericEntity);
    }
}
