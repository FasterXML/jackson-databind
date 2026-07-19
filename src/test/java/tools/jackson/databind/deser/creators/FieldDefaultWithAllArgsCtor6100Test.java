package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6100]: when a properties-based creator is used and a JSON property
// is absent, prefer values from a no-arg-constructed default instance (field
// initializers / no-arg body) over bare JVM defaults (false/0/null).
public class FieldDefaultWithAllArgsCtor6100Test extends DatabindTestUtil
{
    // Mutable: public non-final field + no-arg + all-args (Lombok-ish)
    static class WithBothCtors {
        public boolean historical = true;

        public WithBothCtors() { }

        public WithBothCtors(boolean historical) {
            this.historical = historical;
        }
    }

    // Mutable: private field + accessors + both constructors
    static class WithAccessors {
        private boolean historical = true;

        public WithAccessors() { }

        public WithAccessors(boolean historical) {
            this.historical = historical;
        }

        public boolean isHistorical() { return historical; }
        public void setHistorical(boolean historical) { this.historical = historical; }
    }

    // Explicit @JsonCreator + no-arg: missing args still take field defaults
    // from the no-arg instance when one is available.
    static class ExplicitAllArgs {
        public boolean historical = true;

        public ExplicitAllArgs() { }

        @JsonCreator
        public ExplicitAllArgs(@JsonProperty("historical") boolean historical) {
            this.historical = historical;
        }
    }

    // Creator only (no usable no-arg): missing primitive stays JVM default
    static class CreatorOnly {
        public final boolean historical;

        @JsonCreator
        public CreatorOnly(@JsonProperty("historical") boolean historical) {
            this.historical = historical;
        }
    }

    static class OnlyNoArgs {
        public boolean historical = true;

        public OnlyNoArgs() { }
    }

    // Immutable: final fields (#5318) — multi-arg creator must still win
    static class ImmutableBoth {
        public final int productId;
        public final String name;

        public ImmutableBoth() {
            this(0, null);
        }

        public ImmutableBoth(int productId, String name) {
            this.productId = productId;
            this.name = name;
        }
    }

    static class ScalarDefaults {
        public int count = 5;
        public String label = "def";

        public ScalarDefaults() { }

        public ScalarDefaults(int count, String label) {
            this.count = count;
            this.label = label;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void emptyObjectKeepsBooleanFieldDefault() throws Exception {
        WithBothCtors v = MAPPER.readValue("{}", WithBothCtors.class);
        assertTrue(v.historical);
    }

    @Test
    void presentFalseOverridesFieldDefault() throws Exception {
        WithBothCtors v = MAPPER.readValue("""
                {"historical":false}""", WithBothCtors.class);
        assertFalse(v.historical);
    }

    @Test
    void presentTrue() throws Exception {
        WithBothCtors v = MAPPER.readValue("""
                {"historical":true}""", WithBothCtors.class);
        assertTrue(v.historical);
    }

    @Test
    void accessorsEmptyKeepsFieldDefault() throws Exception {
        WithAccessors v = MAPPER.readValue("{}", WithAccessors.class);
        assertTrue(v.isHistorical());
    }

    @Test
    void accessorsPresentFalse() throws Exception {
        WithAccessors v = MAPPER.readValue("""
                {"historical":false}""", WithAccessors.class);
        assertFalse(v.isHistorical());
    }

    @Test
    void onlyNoArgsKeepsDefault() throws Exception {
        OnlyNoArgs v = MAPPER.readValue("{}", OnlyNoArgs.class);
        assertTrue(v.historical);
    }

    @Test
    void explicitCreatorWithNoArgKeepsFieldDefault() throws Exception {
        ExplicitAllArgs v = MAPPER.readValue("{}", ExplicitAllArgs.class);
        assertTrue(v.historical);
    }

    @Test
    void creatorOnlyMissingUsesPrimitiveDefault() throws Exception {
        CreatorOnly v = MAPPER.readValue("{}", CreatorOnly.class);
        assertFalse(v.historical);
    }

    // [databind#5318]
    @Test
    void immutableBothStillUsesAllArgsCreator() throws Exception {
        ImmutableBoth v = MAPPER.readValue("""
                {"productId":1,"name":"foo"}""", ImmutableBoth.class);
        assertEquals(1, v.productId);
        assertEquals("foo", v.name);
    }

    @Test
    void scalarFieldDefaultsPreserved() throws Exception {
        ScalarDefaults v = MAPPER.readValue("{}", ScalarDefaults.class);
        assertEquals(5, v.count);
        assertEquals("def", v.label);
    }

    @Test
    void scalarPartialOverride() throws Exception {
        ScalarDefaults v = MAPPER.readValue("""
                {"count":9}""", ScalarDefaults.class);
        assertEquals(9, v.count);
        assertEquals("def", v.label);
    }

    @Test
    void featureDisabledUsesNoArgForImmutable() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .constructorDetector(ConstructorDetector.DEFAULT
                        .withAllowImplicitWithDefaultConstructor(false))
                .build();
        ImmutableBoth v = mapper.readValue("{}", ImmutableBoth.class);
        assertEquals(0, v.productId);
        assertNull(v.name);
    }
}
