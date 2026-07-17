package tools.jackson.databind.ser.filter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#6101]: a NON_EMPTY Map property whose entries are all removed by a
// CUSTOM content filter should be treated as empty and omitted, matching the
// behavior for content=NON_EMPTY and the (already correct) write path.
public class JsonIncludeMapContentFilter6101Test extends DatabindTestUtil
{
    // equals() returns true for values the content filter should suppress
    static class FooFilter {
        @Override
        public boolean equals(Object other) {
            return "foo".equals(other);
        }

        @Override
        public int hashCode() { return 0; }
    }

    // Statically-typed value serializer (String) -> isEmpty() static-serializer loop
    static class Bean {
        @JsonInclude(value = JsonInclude.Include.NON_EMPTY,
                content = JsonInclude.Include.CUSTOM,
                contentFilter = FooFilter.class)
        public Map<String, String> stuff = new LinkedHashMap<>();

        public Bean add(String key, String value) {
            stuff.put(key, value);
            return this;
        }
    }

    // Dynamically-resolved value serializer (Object) -> isEmpty() fallback loop
    static class DynBean {
        @JsonInclude(value = JsonInclude.Include.NON_EMPTY,
                content = JsonInclude.Include.CUSTOM,
                contentFilter = FooFilter.class)
        public Map<String, Object> stuff = new LinkedHashMap<>();

        public DynBean add(String key, Object value) {
            stuff.put(key, value);
            return this;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Before the fix this produced {"stuff":{}} because MapSerializer.isEmpty()
    // compared the content filter to the whole Map instead of to each entry value.
    @Test
    public void mapEmptyAfterContentFilter() throws Exception {
        Bean bean = new Bean().add("a", "foo").add("b", "foo");
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void mapKeepsSurvivingEntries() throws Exception {
        Bean bean = new Bean().add("a", "foo").add("b", "keep");
        assertEquals("{\"stuff\":{\"b\":\"keep\"}}",
                MAPPER.writeValueAsString(bean));
    }

    @Test
    public void genuinelyEmptyMapOmitted() throws Exception {
        assertEquals("{}", MAPPER.writeValueAsString(new Bean()));
    }

    // Same bug on the fallback loop taken when the value serializer is not
    // statically known (Map<String,Object>).
    @Test
    public void dynamicValueMapEmptyAfterContentFilter() throws Exception {
        DynBean bean = new DynBean().add("a", "foo").add("b", "foo");
        assertEquals("{}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void dynamicValueMapKeepsSurvivingEntries() throws Exception {
        DynBean bean = new DynBean().add("a", "foo").add("b", "keep");
        assertEquals("{\"stuff\":{\"b\":\"keep\"}}",
                MAPPER.writeValueAsString(bean));
    }
}
