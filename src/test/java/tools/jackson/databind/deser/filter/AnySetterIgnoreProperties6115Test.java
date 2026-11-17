package tools.jackson.databind.deser.filter;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.IgnoredPropertyException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#6115]: a name listed in {@code @JsonIgnoreProperties} that is not
 * itself a mapped property must not be routed to an {@code @JsonAnySetter}.
 * The create path enforces this, but two paths skipped the check: the Record
 * "update" path ({@code readerForUpdating}/{@code updateValue}) and the
 * property-based creator path when the type also has {@code @JsonUnwrapped}
 * properties.
 */
public class AnySetterIgnoreProperties6115Test extends DatabindTestUtil
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

    // [databind#6001]: unwrapped target with its own any-setter makes the unwrapped
    // handler "accept all" names -- the ignore check must still win
    static class NameWithAny {
        public String first;
        @JsonAnySetter
        public Map<String, Object> rest = new HashMap<>();
    }

    @JsonIgnoreProperties({"secret"})
    static class UnwrapAllCreatorBean {
        final int id;
        @JsonUnwrapped
        public NameWithAny name;
        final Map<String, Object> extras = new HashMap<>();

        @JsonCreator
        public UnwrapAllCreatorBean(@JsonProperty("id") int id) {
            this.id = id;
        }

        @JsonAnySetter
        public void any(String key, Object value) { extras.put(key, value); }
    }

    // Ignored name that IS a genuine unwrapped sub-property: must be ignored,
    // not bound, exactly as on the non-creator path
    @JsonIgnoreProperties({"last"})
    static class UnwrapCreatorIgnoringSubProp {
        final int id;
        @JsonUnwrapped
        public Name name;

        @JsonCreator
        public UnwrapCreatorIgnoringSubProp(@JsonProperty("id") int id) {
            this.id = id;
        }
    }

    @JsonIgnoreProperties({"last"})
    static class UnwrapFieldIgnoringSubProp { // no creator: field-based path, for comparison
        public int id;
        @JsonUnwrapped
        public Name name;
    }

    // @JsonIncludeProperties is the other half of `shouldIgnore(...)`
    @JsonIncludeProperties({"name", "other"})
    public record IncludeOnlyRecord(String name, @JsonAnySetter Map<String, Object> extras) {}

    // Explicit ignore list COMBINED with ignoreUnknown=true: the explicit ignoral
    // must still win, and unknown-but-not-ignored names still reach the any-setter
    @JsonIgnoreProperties(value = {"secret"}, ignoreUnknown = true)
    static class UnwrapCreatorIgnoreUnknownBean {
        final int id;
        @JsonUnwrapped
        public Name name;
        final Map<String, Object> extras = new HashMap<>();

        @JsonCreator
        public UnwrapCreatorIgnoreUnknownBean(@JsonProperty("id") int id) {
            this.id = id;
        }

        @JsonAnySetter
        public void any(String key, Object value) { extras.put(key, value); }
    }

    @JsonIgnoreProperties(value = {"secret"}, ignoreUnknown = true)
    static class PlainCreatorIgnoreUnknownBean { // reference: plain create path
        final int id;
        final Map<String, Object> extras = new HashMap<>();

        @JsonCreator
        public PlainCreatorIgnoreUnknownBean(@JsonProperty("id") int id) {
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

    // Update path must filter it too.
    // NOTE: the existing any-setter contents ("kept") are NOT carried over -- an
    // any-setter Record component is not pre-populated from the instance being
    // updated. That is a separate, pre-existing gap in the Record update path
    // (it predates this fix); asserted here only to pin down current behavior.
    @Test
    public void recordUpdateDoesNotLeakIgnoredToAnySetter() throws Exception {
        Map<String, Object> existing = new HashMap<>();
        existing.put("kept", "from-original");
        AnyRecord updated = MAPPER.readerForUpdating(new AnyRecord("alice", existing)).readValue("""
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

    // Unwrapped target that accepts all names ([databind#6001]): per [databind#1075]
    // unwrapped routing is checked BEFORE ignoral, so the name reaches the unwrapped
    // target rather than the outer any-setter. It must still never reach the OUTER
    // any-setter, which is what #6115 is about.
    @Test
    public void unwrappedAcceptAllCreatorDoesNotLeakIgnoredToOuterAnySetter() throws Exception {
        UnwrapAllCreatorBean bean = MAPPER.readValue("""
                {"id":1,"first":"a","secret":"nope","other":"ok"}
                """, UnwrapAllCreatorBean.class);
        assertNotNull(bean.name);
        assertFalse(bean.extras.containsKey("secret"),
                "ignored 'secret' leaked into OUTER any-setter: " + bean.extras);
        assertEquals("a", bean.name.first);
        // [databind#1075]: an "accept all" unwrapped target claims names before the
        // ignore check, so 'secret' lands there -- consistent with the field-based path
        assertEquals(Map.of("other", "ok", "secret", "nope"), bean.name.rest);
    }

    // [databind#1075]: a name that IS an unwrapped sub-property binds even when the
    // outer class marks it ignorable -- and the creator path must agree with the
    // field-based path (which UnwrappedWithIgnore1075Test covers)
    @Test
    public void unwrappedSubPropertyBindsConsistently() throws Exception {
        final String json = """
                {"id":1,"first":"a","last":"b"}
                """;
        UnwrapFieldIgnoringSubProp field = MAPPER.readValue(json, UnwrapFieldIgnoringSubProp.class);
        assertNotNull(field.name);
        assertEquals("a", field.name.first);
        assertEquals("b", field.name.last);

        UnwrapCreatorIgnoringSubProp creator = MAPPER.readValue(json, UnwrapCreatorIgnoringSubProp.class);
        assertNotNull(creator.name);
        assertEquals("a", creator.name.first);
        assertEquals("b", creator.name.last,
                "creator path did not bind unwrapped sub-property 'last' (see [databind#1075])");
    }

    // Names excluded by @JsonIncludeProperties must not reach the any-setter either
    @Test
    public void recordUpdateHonorsIncludeProperties() throws Exception {
        IncludeOnlyRecord updated = MAPPER.readerForUpdating(new IncludeOnlyRecord("alice", new HashMap<>()))
                .readValue("""
                        {"name":"alice","secret":"nope","other":"ok"}
                        """);
        assertEquals(Map.of("other", "ok"), updated.extras());
    }

    // Explicit ignoral must beat ignoreUnknown=true on the creator+unwrapped path,
    // exactly as it does on the plain create path
    @Test
    public void explicitIgnoralBeatsIgnoreUnknown() throws Exception {
        final String json = """
                {"id":1,"first":"a","secret":"nope","other":"ok"}
                """;
        PlainCreatorIgnoreUnknownBean plain = MAPPER.readValue("""
                {"id":1,"secret":"nope","other":"ok"}
                """, PlainCreatorIgnoreUnknownBean.class);
        assertEquals(Map.of("other", "ok"), plain.extras);

        UnwrapCreatorIgnoreUnknownBean bean = MAPPER.readValue(json, UnwrapCreatorIgnoreUnknownBean.class);
        assertFalse(bean.extras.containsKey("secret"),
                "explicitly ignored 'secret' leaked into any-setter when ignoreUnknown=true: " + bean.extras);
        assertEquals(Map.of("other", "ok"), bean.extras);
        assertNotNull(bean.name);
        assertEquals("a", bean.name.first);
    }

    // Since these names are now "ignored" rather than passed to the any-setter,
    // FAIL_ON_IGNORED_PROPERTIES becomes applicable on these paths
    @Test
    public void failOnIgnoredPropertiesAppliesOnUpdatePath() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
                .build();
        assertThrows(IgnoredPropertyException.class,
                () -> mapper.readerForUpdating(new AnyRecord("alice", new HashMap<>()))
                        .readValue("""
                                {"secret":"nope"}
                                """));
    }

    @Test
    public void failOnIgnoredPropertiesAppliesOnUnwrappedCreatorPath() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
                .build();
        assertThrows(IgnoredPropertyException.class,
                () -> mapper.readValue("""
                        {"id":1,"first":"a","secret":"nope"}
                        """, UnwrapCreatorBean.class));
    }
}
