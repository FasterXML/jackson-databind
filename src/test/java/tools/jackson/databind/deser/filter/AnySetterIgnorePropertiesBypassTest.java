package tools.jackson.databind.deser.filter;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A name listed in {@code @JsonIgnoreProperties} that is not itself a mapped
 * property must not be routed to an {@code @JsonAnySetter}. The create path
 * enforces this, but two paths skipped the check: the Record "update" path
 * ({@code readerForUpdating}/{@code updateValue}) and the property-based
 * creator path when the type also has {@code @JsonUnwrapped} properties.
 */
public class AnySetterIgnorePropertiesBypassTest extends DatabindTestUtil
{
    @JsonIgnoreProperties({"secret"})
    public record AnyRecord(String name, @JsonAnySetter Map<String, Object> extras) {}

    static class Name {
        public String first, last;
    }

    @JsonIgnoreProperties({"secret"})
    static class UnwrapCreatorBean {
        final int id;
        @JsonUnwrapped
        public Name name;
        final Map<String, Object> extras = new HashMap<>();

        @JsonCreator
        public UnwrapCreatorBean(@JsonProperty("id") int id) {
            this.id = id;
        }

        @JsonAnySetter
        public void any(String key, Object value) { extras.put(key, value); }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Create path already filters the ignored name (control)
    @Test
    public void recordCreateFiltersIgnored() throws Exception {
        AnyRecord r = MAPPER.readValue("""
                {"name":"alice","secret":"nope","other":"ok"}
                """, AnyRecord.class);
        assertEquals(Map.of("other", "ok"), r.extras());
    }

    // Update path must filter it too, and retain the existing any-setter contents
    @Test
    public void recordUpdateDoesNotLeakIgnoredToAnySetter() throws Exception {
        AnyRecord original = new AnyRecord("alice", new HashMap<>());
        AnyRecord updated = MAPPER.readerForUpdating(original).readValue("""
                {"name":"alice","secret":"nope","other":"ok"}
                """);
        assertFalse(updated.extras().containsKey("secret"),
                "ignored 'secret' leaked into any-setter on Record update path: " + updated.extras());
        assertEquals(Map.of("other", "ok"), updated.extras());
    }

    // Property-based creator plus @JsonUnwrapped must also filter it
    @Test
    public void unwrappedCreatorDoesNotLeakIgnoredToAnySetter() throws Exception {
        UnwrapCreatorBean bean = MAPPER.readValue("""
                {"id":1,"first":"a","last":"b","secret":"nope","other":"ok"}
                """, UnwrapCreatorBean.class);
        assertFalse(bean.extras.containsKey("secret"),
                "ignored 'secret' leaked into any-setter on creator+unwrapped path: " + bean.extras);
        assertEquals(Map.of("other", "ok"), bean.extras);
        // unwrapped property still binds
        assertNotNull(bean.name);
        assertEquals("a", bean.name.first);
    }
}
