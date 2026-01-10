package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MapperFeature#INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY}
 * which controls whether only Record component getters are auto-detected
 * or if all JavaBean-style getters are detected (backward compatible behavior).
 */
public class RecordIgnoreGetters4157Test extends DatabindTestUtil
{
    // Test Case 1: Basic record with helper getter
    record PersonRecord(String name, int age) {
        // Helper method that is NOT a record component
        public String getDisplayName() {
            return name.toUpperCase();
        }
    }

    // Test Case 2: Record implementing interface with getter
    interface Identifiable {
        String getId();
    }

    @JsonPropertyOrder({"name", "id"})
    record UserRecord(String name) implements Identifiable {
        @Override
        public String getId() {
            return "ID:" + name;
        }
    }

    // Test Case 3: Record with explicit annotation on helper method
    record AnnotatedHelperRecord(String name) {
        @JsonProperty("display")
        public String getDisplayName() {
            return name.toUpperCase();
        }
    }

    // Test Case 4: Record with is-getter helper
    record BooleanHelperRecord(String name, boolean active) {
        // Helper method - not a component
        public boolean isSpecial() {
            return name.startsWith("Special");
        }
    }

    // Test Case 5: Record with both component getter and helper
    record MixedRecord(int value) {
        // Component accessor - should always work
        @Override
        public int value() {
            return value;
        }

        // Helper - behavior depends on feature
        public int getDoubleValue() {
            return value * 2;
        }
    }

    // Test Case 6: Empty record with static getter
    record EmptyWithStatic() {
        public static String getStaticValue() {
            return "static";
        }
    }

    private final ObjectMapper MAPPER_DEFAULT = newJsonMapper();

    private final ObjectMapper MAPPER_RESTRICTED = jsonMapperBuilder()
            .enable(MapperFeature.INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY)
            .build();

    /*
     * Test: Feature is DISABLED by default (backward compatibility)
     */
    @Test
    public void testFeatureDisabledByDefault() throws Exception {
        assertFalse(MAPPER_DEFAULT.isEnabled(MapperFeature.INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY));
    }

    /*
     * Test: With feature DISABLED, helper getters ARE serialized
     */
    @Test
    public void testHelperGetterIncluded_FeatureDisabled() throws Exception {
        PersonRecord person = new PersonRecord("john", 30);
        String json = MAPPER_DEFAULT.writeValueAsString(person);

        // Should include helper method
        assertTrue(json.contains("displayName"), "Should include displayName with feature disabled");
        assertTrue(json.contains("JOHN"), "Should have uppercase name");
        assertTrue(json.contains("name"), "Should include actual component");
        assertTrue(json.contains("age"), "Should include actual component");
    }

    /*
     * Test: With feature ENABLED, helper getters are NOT serialized
     */
    @Test
    public void testHelperGetterExcluded_FeatureEnabled() throws Exception {
        PersonRecord person = new PersonRecord("john", 30);
        String json = MAPPER_RESTRICTED.writeValueAsString(person);

        // Should NOT include helper method
        assertFalse(json.contains("displayName"), "Should NOT include displayName with feature enabled");
        assertFalse(json.contains("JOHN"), "Should NOT have uppercase name");
        // Should still include actual components
        assertTrue(json.contains("name"), "Should include actual component");
        assertTrue(json.contains("john"), "Should have original name");
        assertTrue(json.contains("age"), "Should include actual component");
    }

    /*
     * Test: Interface getter excluded when feature enabled
     */
    @Test
    public void testInterfaceGetterExcluded_FeatureEnabled() throws Exception {
        UserRecord user = new UserRecord("alice");
        String json = MAPPER_RESTRICTED.writeValueAsString(user);

        assertEquals(a2q("{'name':'alice'}"), json);
        assertFalse(json.contains("id"), "Should NOT include interface getter");
    }

    /*
     * Test: Interface getter included when feature disabled
     */
    @Test
    public void testInterfaceGetterIncluded_FeatureDisabled() throws Exception {
        UserRecord user = new UserRecord("alice");
        String json = MAPPER_DEFAULT.writeValueAsString(user);

        assertTrue(json.contains("id"), "Should include interface getter");
        assertTrue(json.contains("ID:alice"), "Should have computed id");
    }

    /*
     * Test: Explicit @JsonProperty ALWAYS works regardless of feature
     */
    @Test
    public void testExplicitAnnotation_AlwaysWorks_FeatureEnabled() throws Exception {
        AnnotatedHelperRecord record = new AnnotatedHelperRecord("test");
        String json = MAPPER_RESTRICTED.writeValueAsString(record);

        assertTrue(json.contains("display"), "Explicit @JsonProperty should always work");
        assertTrue(json.contains("TEST"), "Should have uppercase value");
    }

    @Test
    public void testExplicitAnnotation_AlwaysWorks_FeatureDisabled() throws Exception {
        AnnotatedHelperRecord record = new AnnotatedHelperRecord("test");
        String json = MAPPER_DEFAULT.writeValueAsString(record);

        assertTrue(json.contains("display"), "Explicit @JsonProperty should always work");
        assertTrue(json.contains("TEST"), "Should have uppercase value");
    }

    /*
     * Test: Is-getter helpers excluded when feature enabled
     */
    @Test
    public void testIsGetterHelper_FeatureEnabled() throws Exception {
        BooleanHelperRecord record = new BooleanHelperRecord("Special Case", true);
        String json = MAPPER_RESTRICTED.writeValueAsString(record);

        assertTrue(json.contains("active"), "Should include actual boolean component");
        assertFalse(json.contains("special"), "Should NOT include is-getter helper");
    }

    @Test
    public void testIsGetterHelper_FeatureDisabled() throws Exception {
        BooleanHelperRecord record = new BooleanHelperRecord("Special Case", true);
        String json = MAPPER_DEFAULT.writeValueAsString(record);

        assertTrue(json.contains("active"), "Should include actual boolean component");
        assertTrue(json.contains("special"), "Should include is-getter helper with feature disabled");
    }

    /*
     * Test: Component accessor with same name as helper
     */
    @Test
    public void testComponentAccessor_AlwaysWorks() throws Exception {
        MixedRecord record = new MixedRecord(42);

        // Feature enabled - only component
        String jsonRestricted = MAPPER_RESTRICTED.writeValueAsString(record);
        assertTrue(jsonRestricted.contains("value"), "Component should be included");
        assertTrue(jsonRestricted.contains("42"), "Should have value 42");
        assertFalse(jsonRestricted.contains("doubleValue"), "Helper should be excluded");

        // Feature disabled - both
        String jsonDefault = MAPPER_DEFAULT.writeValueAsString(record);
        assertTrue(jsonDefault.contains("value"), "Component should be included");
        assertTrue(jsonDefault.contains("doubleValue"), "Helper should be included");
        assertTrue(jsonDefault.contains("84"), "Should have doubled value");
    }

    /*
     * Test: Round-trip with feature enabled maintains data integrity
     */
    @Test
    public void testRoundTrip_FeatureEnabled() throws Exception {
        PersonRecord original = new PersonRecord("alice", 25);

        String json = MAPPER_RESTRICTED.writeValueAsString(original);
        PersonRecord deserialized = MAPPER_RESTRICTED.readValue(json, PersonRecord.class);

        assertEquals(original, deserialized, "Round-trip should preserve data");
        assertEquals("alice", deserialized.name());
        assertEquals(25, deserialized.age());
    }

    /*
     * Test: Deserialization ignores extra properties (helper getters not in components)
     */
    @Test
    public void testDeserialization_IgnoresNonComponentProperties() throws Exception {
        // JSON with helper property that was serialized with feature disabled
        String json = a2q("{'name':'bob','age':30,'displayName':'BOB'}");

        PersonRecord deserialized = MAPPER_RESTRICTED.readValue(json, PersonRecord.class);

        assertEquals("bob", deserialized.name());
        assertEquals(30, deserialized.age());
        // displayName is ignored during deserialization (not a component)
    }

    /*
     * Test: Static methods are never included (baseline behavior)
     */
    @Test
    public void testStaticGetter_NeverIncluded() throws Exception {
        EmptyWithStatic record = new EmptyWithStatic();

        String jsonRestricted = MAPPER_RESTRICTED.writeValueAsString(record);
        String jsonDefault = MAPPER_DEFAULT.writeValueAsString(record);

        assertEquals("{}", jsonRestricted, "Static getter should not be included");
        assertEquals("{}", jsonDefault, "Static getter should not be included");
    }

    /*
     * Test: Feature configuration via builder
     */
    @Test
    public void testFeatureConfiguration_ViaBuilder() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY)
                .build();

        assertTrue(mapper.isEnabled(MapperFeature.INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY));

        PersonRecord person = new PersonRecord("test", 1);
        String json = mapper.writeValueAsString(person);
        assertFalse(json.contains("displayName"));
    }

    /*
     * Test: Feature can be disabled explicitly
     */
    @Test
    public void testFeatureConfiguration_ExplicitDisable() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY)
                .build();

        assertFalse(mapper.isEnabled(MapperFeature.INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY));

        PersonRecord person = new PersonRecord("test", 1);
        String json = mapper.writeValueAsString(person);
        assertTrue(json.contains("displayName"));
    }
}
