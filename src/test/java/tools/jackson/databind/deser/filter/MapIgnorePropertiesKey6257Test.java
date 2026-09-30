package tools.jackson.databind.deser.filter;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#6257]: ignorals must be checked before key deserialization
public class MapIgnorePropertiesKey6257Test extends DatabindTestUtil
{
    enum ABC { A, B }

    static class IntKeyMap {
        @JsonIgnoreProperties("comment")
        public Map<Integer, String> map;
    }

    static class EnumKeyMap {
        @JsonIgnoreProperties("unknown")
        public Map<ABC, String> map;
    }

    static class IncludeIntKeyMap {
        @JsonIncludeProperties("1")
        public Map<Integer, String> map;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void ignoredNonConvertibleIntKey() throws Exception {
        IntKeyMap result = MAPPER.readValue(a2q(
                "{'map':{'1':'a','comment':{'x':[1]}}}"), IntKeyMap.class);
        assertEquals(Map.of(1, "a"), result.map);
    }

    @Test
    void ignoredNonConvertibleEnumKey() throws Exception {
        EnumKeyMap result = MAPPER.readValue(a2q(
                "{'map':{'A':'a','unknown':'x'}}"), EnumKeyMap.class);
        assertEquals(Map.of(ABC.A, "a"), result.map);
    }

    @Test
    void notIncludedNonConvertibleKey() throws Exception {
        IncludeIntKeyMap result = MAPPER.readValue(a2q(
                "{'map':{'1':'a','comment':'x'}}"), IncludeIntKeyMap.class);
        assertEquals(Map.of(1, "a"), result.map);
    }
}
