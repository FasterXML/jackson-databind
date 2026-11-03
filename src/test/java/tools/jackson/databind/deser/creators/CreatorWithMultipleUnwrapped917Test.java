package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#917] Unable to have two @JsonUnwrapped annotated objects of same type
// within one constructor
public class CreatorWithMultipleUnwrapped917Test
    extends DatabindTestUtil
{
    static class Unwrapped {
        @JsonProperty
        public String value;
    }

    static class FullExample {
        @JsonUnwrapped(prefix = "pre_")
        public final Unwrapped pre;
        @JsonUnwrapped(prefix = "post_")
        public final Unwrapped post;

        @JsonCreator
        public FullExample(
                @JsonUnwrapped(prefix = "pre_") Unwrapped pre,
                @JsonUnwrapped(prefix = "post_") Unwrapped post) {
            this.pre = pre;
            this.post = post;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void testMultipleUnwrappedInCreator() throws Exception
    {
        String json = "{\"pre_value\":\"a\",\"post_value\":\"b\"}";
        FullExample result = MAPPER.readValue(json, FullExample.class);
        assertNotNull(result);
        assertNotNull(result.pre);
        assertNotNull(result.post);
        assertEquals("a", result.pre.value);
        assertEquals("b", result.post.value);
    }

    // Also verify round-trip (serialize then deserialize)
    @Test
    void testMultipleUnwrappedRoundTrip() throws Exception
    {
        FullExample input = new FullExample(new Unwrapped(), new Unwrapped());
        input.pre.value = "hello";
        input.post.value = "world";

        String json = MAPPER.writeValueAsString(input);
        FullExample result = MAPPER.readValue(json, FullExample.class);
        assertNotNull(result);
        assertEquals("hello", result.pre.value);
        assertEquals("world", result.post.value);
    }

    // Test with same type, different prefixes (the core of #917)
    @Test
    void testMultipleUnwrappedSameTypeDifferentPrefixes() throws Exception
    {
        // Ensure both are independently deserialized with correct prefix
        String json = "{\"pre_value\":\"first\",\"post_value\":\"second\"}";
        FullExample result = MAPPER.readValue(json, FullExample.class);
        assertEquals("first", result.pre.value);
        assertEquals("second", result.post.value);

        // Ensure they don't bleed into each other
        json = "{\"pre_value\":\"only_pre\"}";
        result = MAPPER.readValue(json, FullExample.class);
        assertEquals("only_pre", result.pre.value);
        assertNull(result.post.value);
    }
}
