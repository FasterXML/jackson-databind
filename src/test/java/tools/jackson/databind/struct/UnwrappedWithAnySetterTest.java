package tools.jackson.databind.struct;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for [databind#1811]: {@code @JsonUnwrapped} and {@code @JsonAnySetter}
 * should not cause unwrapped properties to be deserialized twice (once into
 * the unwrapped POJO and again into the any-setter map).
 */
public class UnwrappedWithAnySetterTest extends DatabindTestUtil
{
    static class Outer {
        public Long id;

        @JsonUnwrapped
        public Inner inner;

        private Map<String, Object> extra = new HashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getExtra() {
            return extra;
        }

        @JsonAnySetter
        public void set(String key, Object value) {
            extra.put(key, value);
        }
    }

    static class Inner {
        public String name;
    }

    // Support classes for testUnwrappedWithPrefixWithAnyGetter

    static class InnerWithAnyGetter {
        private Map<String, Object> extra = new HashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getExtra() {
            return extra;
        }

        @JsonAnySetter
        public void setExtra(String key, Object value) {
            extra.put(key, value);
        }
    }

    @JsonPropertyOrder({"name"})
    static class OuterWithPrefixedInnerAnyGetter {
        public String name = "aaa";

        @JsonUnwrapped(prefix = "a-")
        public InnerWithAnyGetter inner;

        OuterWithPrefixedInnerAnyGetter() {
            inner = new InnerWithAnyGetter();
            inner.setExtra("age", 64);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1811]
    @Test
    public void testUnwrappedWithAnySetter() throws Exception
    {
        String json = a2q("{'id':1,'name':'aaa','age':12}");
        Outer outer = MAPPER.readValue(json, Outer.class);

        // "id" goes to Outer.id
        assertEquals(Long.valueOf(1), outer.id);

        // "name" should ONLY go to the unwrapped Inner, not also to the any-setter map
        assertNotNull(outer.inner);
        assertEquals("aaa", outer.inner.name);

        // "age" is truly unknown, so it should go to the any-setter map
        assertEquals(12, outer.extra.get("age"));

        // Key assertion: "name" must NOT appear in the any-setter map
        // This was the bug in [databind#1811]: "name" was deserialized twice
        assertFalse(outer.extra.containsKey("name"),
                "Property 'name' handled by @JsonUnwrapped should not also appear in @JsonAnySetter map, but extra=" + outer.extra);
        assertEquals(1, outer.extra.size(), "Only 'age' should be in extra map, but got: " + outer.extra);
    }

    // Test for @JsonUnwrapped with prefix combined with @JsonAnyGetter on the inner bean
    @Test
    public void testUnwrappedWithPrefixWithAnyGetter() throws Exception
    {
        OuterWithPrefixedInnerAnyGetter outer = new OuterWithPrefixedInnerAnyGetter();
        String json = MAPPER.writeValueAsString(outer);
        // "name" from Outer is serialized directly (no prefix);
        // "age" from Inner's @JsonAnyGetter must be serialized with the "a-" prefix applied
        assertEquals(a2q("{'name':'aaa','a-age':64}"), json);
    }

    // Test that @JsonInclude(NON_NULL) on the any-getter map is respected
    // even when @JsonUnwrapped with prefix is in play (content inclusion
    // must not be silently dropped by the name-transform path).
    static class InnerWithAnyGetterNonNull {
        public int age = 64;

        private Map<String, Object> extra = new HashMap<>();

        @JsonInclude(content = JsonInclude.Include.NON_NULL)
        @JsonAnyGetter
        public Map<String, Object> getExtra() { return extra; }

        @JsonAnySetter
        public void setExtra(String key, Object value) { extra.put(key, value); }
    }

    static class OuterWithPrefixedInnerNonNull {
        public String name = "aaa";

        @JsonUnwrapped(prefix = "a-")
        public InnerWithAnyGetterNonNull inner;

        public OuterWithPrefixedInnerNonNull() {
            inner = new InnerWithAnyGetterNonNull();
            inner.extra.put("nickname", "Ace");
            inner.extra.put("nullVal", null);
        }
    }

    @Test
    public void testUnwrappedWithPrefixAndContentInclusion() throws Exception
    {
        OuterWithPrefixedInnerNonNull outer = new OuterWithPrefixedInnerNonNull();
        String json = MAPPER.writeValueAsString(outer);
        // "nullVal" entry must be suppressed by @JsonInclude(content=NON_NULL);
        // "nickname" and "age" must have the "a-" prefix
        Map<?,?> result = MAPPER.readValue(json, Map.class);
        assertEquals(3, result.size(), "null entry should be suppressed, got: " + json);
        assertEquals("aaa", result.get("name"));
        assertEquals(64, result.get("a-age"));
        assertEquals("Ace", result.get("a-nickname"));
        assertNull(result.get("a-nullVal"), "null-valued entry should be suppressed");
    }
}
