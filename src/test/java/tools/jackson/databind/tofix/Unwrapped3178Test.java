package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#3178]
public class Unwrapped3178Test extends DatabindTestUtil
{
    static class Location {
        public int x;
        public int y;
        public Location() { }
        public Location(int x, int y) { this.x = x; this.y = y; }
    }

    static class Inner {
        public String name;
        public Location location;
        public Inner() { }
        public Inner(String str, int x, int y) {
            name = str;
            location = new Location(x, y);
        }
    }

    static class WithPrefix {
        @JsonUnwrapped(prefix = "_")
        public Inner unwrapped;
        public WithPrefix() { }
        public WithPrefix(String str, int x, int y) {
            unwrapped = new Inner(str, x, y);
        }
    }

    static class WithoutPrefix {
        @JsonUnwrapped
        public Inner unwrapped;
        public WithoutPrefix() { }
        public WithoutPrefix(String str, int x, int y) {
            unwrapped = new Inner(str, x, y);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testUnwrappingDeserialize() throws Exception {
        WithoutPrefix source = new WithoutPrefix("Bubba", 2, 3);
        String json = MAPPER.writeValueAsString(source);
        WithoutPrefix bean = MAPPER.readValue(json, WithoutPrefix.class);
        assertNotNull(bean.unwrapped);
        assertNotNull(bean.unwrapped.location);
        assertEquals(source.unwrapped.name, bean.unwrapped.name);
        assertEquals(source.unwrapped.location.x, bean.unwrapped.location.x);
        assertEquals(source.unwrapped.location.y, bean.unwrapped.location.y);
    }

    @JacksonTestFailureExpected
    @Test
    public void testPrefixedUnwrappingDeserialize() throws Exception {
        WithPrefix source = new WithPrefix("Bubba", 2, 3);
        String json = MAPPER.writeValueAsString(source);
        WithPrefix bean = MAPPER.readValue(json, WithPrefix.class);
        assertNotNull(bean.unwrapped);
        assertNotNull(bean.unwrapped.location);
        assertEquals(source.unwrapped.name, bean.unwrapped.name);
        assertEquals(source.unwrapped.location.x, bean.unwrapped.location.x);
        assertEquals(source.unwrapped.location.y, bean.unwrapped.location.y);
    }
}
