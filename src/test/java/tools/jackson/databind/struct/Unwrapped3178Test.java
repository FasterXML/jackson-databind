package tools.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

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

    // Creator-based Inner: forces unwrapping rename through PropertyBasedCreator
    static class InnerCreator {
        public String name;
        public Location location;

        @JsonCreator
        public InnerCreator(@JsonProperty("name") String name,
                @JsonProperty("location") Location location) {
            this.name = name;
            this.location = location;
        }
    }

    static class WithPrefixCreator {
        @JsonUnwrapped(prefix = "_")
        public InnerCreator unwrapped;
        public WithPrefixCreator() { }
        public WithPrefixCreator(String str, int x, int y) {
            unwrapped = new InnerCreator(str, new Location(x, y));
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testUnwrappingDeserialize() throws Exception {
        WithoutPrefix source = new WithoutPrefix("Bubba", 2, 3);
        String json = MAPPER.writeValueAsString(source);
        assertEquals("{\"location\":{\"x\":2,\"y\":3},\"name\":\"Bubba\"}", json);
        WithoutPrefix bean = MAPPER.readValue(json, WithoutPrefix.class);
        assertNotNull(bean.unwrapped);
        assertNotNull(bean.unwrapped.location);
        assertEquals(source.unwrapped.name, bean.unwrapped.name);
        assertEquals(source.unwrapped.location.x, bean.unwrapped.location.x);
        assertEquals(source.unwrapped.location.y, bean.unwrapped.location.y);
    }

    @Test
    public void testPrefixedUnwrappingDeserialize() throws Exception {
        WithPrefix source = new WithPrefix("Bubba", 2, 3);
        String json = MAPPER.writeValueAsString(source);
        assertEquals("{\"_location\":{\"x\":2,\"y\":3},\"_name\":\"Bubba\"}", json);
        WithPrefix bean = MAPPER.readValue(json, WithPrefix.class);
        assertNotNull(bean.unwrapped);
        assertNotNull(bean.unwrapped.location);
        assertEquals(source.unwrapped.name, bean.unwrapped.name);
        assertEquals(source.unwrapped.location.x, bean.unwrapped.location.x);
        assertEquals(source.unwrapped.location.y, bean.unwrapped.location.y);
    }

    // Variant of #3178 that exercises the PropertyBasedCreator rename path
    @Test
    public void testPrefixedUnwrappingDeserializeWithCreator() throws Exception {
        WithPrefixCreator source = new WithPrefixCreator("Bubba", 2, 3);
        String json = MAPPER.writeValueAsString(source);
        assertEquals("{\"_name\":\"Bubba\",\"_location\":{\"x\":2,\"y\":3}}", json);
        WithPrefixCreator bean = MAPPER.readValue(json, WithPrefixCreator.class);
        assertNotNull(bean.unwrapped);
        assertNotNull(bean.unwrapped.location);
        assertEquals(source.unwrapped.name, bean.unwrapped.name);
        assertEquals(source.unwrapped.location.x, bean.unwrapped.location.x);
        assertEquals(source.unwrapped.location.y, bean.unwrapped.location.y);
    }
}
