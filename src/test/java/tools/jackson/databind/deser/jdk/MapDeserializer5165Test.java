package tools.jackson.databind.deser.jdk;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidNullException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

// For [databind#5165]
public class MapDeserializer5165Test
{
    static class Dst {
        private Map<String, Integer> map;

        public Map<String, Integer> getMap() {
            return map;
        }

        public void setMap(Map<String, Integer> map) {
            this.map = map;
        }
    }

    @Test
    public void nullsFailTest() {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.FAIL))
                .build();
        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"map\":{\"key\":\"\"}}", new TypeReference<Dst>(){})
        );
    }

    @Test
    public void nullsSkipTest() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();
        Dst dst = mapper.readValue("{\"map\":{\"key\":\"\"}}", new TypeReference<Dst>() {});
        assertTrue(dst.getMap().isEmpty());
    }
}
