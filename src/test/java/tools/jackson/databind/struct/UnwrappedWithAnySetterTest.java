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

    static class Inner6118 {
        private final String name;
        private final Map<String, String> extra = new HashMap<>();

        public Inner6118(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @JsonAnyGetter
        public Map<String, String> getExtra() {
            return extra;
        }

        @JsonAnySetter
        public void addExtra(String key, String value) {
            extra.put(key, value);
        }
    }

    private static class Outer6118 {
        @JsonUnwrapped(prefix = "a-")
        private final Inner6118 a;

        public Outer6118(Inner6118 a) {
            this.a = a;
        }

        public Inner6118 getA() {
            return a;
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

    @Test
    public void testUnwrappedWithPrefixWithAnyGetter() throws Exception
    {
        Outer6118 outer = new Outer6118(new Inner6118("aaa"));
        outer.getA().addExtra("age", "64");
        String json = MAPPER.writeValueAsString(outer);
        assertEquals(a2q("{'name':'aaa','a-age':'64'}"), json);
    }
}
