package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#4119]: READ_ONLY for Creator param (Record or POJO)
// 2024-03-25, tatu: Appears to be passing for 3.0...
class CreatorWithReadOnlyParam4119Test extends DatabindTestUtil {
    static class Bean4119 {
        String foo, bar;

        @JsonCreator
        public Bean4119(@JsonProperty("foo") String foo,
                        @JsonProperty(value = "bar", access = JsonProperty.Access.READ_ONLY) String bar) {
            this.foo = foo;
            this.bar = bar;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#4119]: READ_ONLY for Creator param (Record or POJO)
    @Test
    void creatorWithReadOnly() throws Exception {
        Bean4119 bean = MAPPER.readerFor(Bean4119.class)
                .readValue(a2q("{'foo':'a', 'bar':'b'}"));
        assertNotNull(bean);
        assertEquals("a", bean.foo);
        // should either pass `null` (same as [databind#1890]), or, fail
        // with useful exception (and not claiming no name specified)
        assertNull(bean.bar);
    }
}
