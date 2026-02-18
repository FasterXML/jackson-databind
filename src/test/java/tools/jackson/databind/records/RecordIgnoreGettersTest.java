package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MapperFeature#INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY}
 * which controls whether only Record component getters are auto-detected
 * or if all JavaBean-style getters are detected (backward compatible behavior).
 * Also covers non-component getter filtering from interface implementations.
 */
public class RecordIgnoreGettersTest extends DatabindTestUtil
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

    // [databind#3628] Record implementing interface with multiple getters
    interface InterfaceWithGetter {
        String getId();
        String getName();
    }

    @JsonPropertyOrder({"id", "name", "count"}) // easier to assert when JSON field ordering is always the same
    record RecordWithInterfaceWithGetter(String name) implements InterfaceWithGetter {

        @Override
        public String getId() {
            return "ID:" + name;
        }

        @Override
        public String getName() {
            return name;
        }

        // [databind#3895]
        public int getCount() {
            return 999;
        }
    }

    private final ObjectMapper MAPPER_DEFAULT = newJsonMapper();

    private final ObjectMapper MAPPER_RESTRICTED = jsonMapperBuilder()
            .enable(MapperFeature.INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY)
            .build();

    /*
    /**********************************************************************
    /* Test methods, INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY feature [databind#4157]
    /**********************************************************************
     */

    @Test
    public void testFeatureDisabledByDefault() throws Exception {
        assertFalse(MAPPER_DEFAULT.isEnabled(MapperFeature.INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY));
    }

    @Test
    public void testHelperGetterIncluded_FeatureDisabled() throws Exception {
        PersonRecord person = new PersonRecord("john", 30);
        String json = MAPPER_DEFAULT.writeValueAsString(person);

        assertTrue(json.contains("displayName"), "Should include displayName with feature disabled");
        assertTrue(json.contains("JOHN"), "Should have uppercase name");
        assertTrue(json.contains("name"), "Should include actual component");
        assertTrue(json.contains("age"), "Should include actual component");
    }

    @Test
    public void testHelperGetterExcluded_FeatureEnabled() throws Exception {
        PersonRecord person = new PersonRecord("john", 30);
        String json = MAPPER_RESTRICTED.writeValueAsString(person);

        assertFalse(json.contains("displayName"), "Should NOT include displayName with feature enabled");
        assertFalse(json.contains("JOHN"), "Should NOT have uppercase name");
        assertTrue(json.contains("name"), "Should include actual component");
        assertTrue(json.contains("john"), "Should have original name");
        assertTrue(json.contains("age"), "Should include actual component");
    }

    @Test
    public void testInterfaceGetterExcluded_FeatureEnabled() throws Exception {
        UserRecord user = new UserRecord("alice");
        String json = MAPPER_RESTRICTED.writeValueAsString(user);

        assertEquals(a2q("{'name':'alice'}"), json);
        assertFalse(json.contains("id"), "Should NOT include interface getter");
    }

    @Test
    public void testInterfaceGetterIncluded_FeatureDisabled() throws Exception {
        UserRecord user = new UserRecord("alice");
        String json = MAPPER_DEFAULT.writeValueAsString(user);

        assertTrue(json.contains("id"), "Should include interface getter");
        assertTrue(json.contains("ID:alice"), "Should have computed id");
    }

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

    @Test
    public void testComponentAccessor_AlwaysWorks() throws Exception {
        MixedRecord record = new MixedRecord(42);

        String jsonRestricted = MAPPER_RESTRICTED.writeValueAsString(record);
        assertTrue(jsonRestricted.contains("value"), "Component should be included");
        assertTrue(jsonRestricted.contains("42"), "Should have value 42");
        assertFalse(jsonRestricted.contains("doubleValue"), "Helper should be excluded");

        String jsonDefault = MAPPER_DEFAULT.writeValueAsString(record);
        assertTrue(jsonDefault.contains("value"), "Component should be included");
        assertTrue(jsonDefault.contains("doubleValue"), "Helper should be included");
        assertTrue(jsonDefault.contains("84"), "Should have doubled value");
    }

    @Test
    public void testRoundTrip_FeatureEnabled() throws Exception {
        PersonRecord original = new PersonRecord("alice", 25);

        String json = MAPPER_RESTRICTED.writeValueAsString(original);
        PersonRecord deserialized = MAPPER_RESTRICTED.readValue(json, PersonRecord.class);

        assertEquals(original, deserialized, "Round-trip should preserve data");
        assertEquals("alice", deserialized.name());
        assertEquals(25, deserialized.age());
    }

    @Test
    public void testDeserialization_IgnoresNonComponentProperties() throws Exception {
        String json = a2q("{'name':'bob','age':30,'displayName':'BOB'}");

        PersonRecord deserialized = MAPPER_RESTRICTED.readValue(json, PersonRecord.class);

        assertEquals("bob", deserialized.name());
        assertEquals(30, deserialized.age());
    }

    @Test
    public void testStaticGetter_NeverIncluded() throws Exception {
        EmptyWithStatic record = new EmptyWithStatic();

        assertEquals("{}", MAPPER_RESTRICTED.writeValueAsString(record));
        assertEquals("{}", MAPPER_DEFAULT.writeValueAsString(record));
    }

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

    /*
    /**********************************************************************
    /* Test methods, non-component interface getters [databind#3628]
    /**********************************************************************
     */

    // [databind#3628]
    @Test
    public void testSerializeIgnoreInterfaceGetter_WithoutUsingVisibilityConfig()
    {
        String json = MAPPER_DEFAULT.writeValueAsString(new RecordWithInterfaceWithGetter("Bob"));
        assertEquals("{\"id\":\"ID:Bob\",\"name\":\"Bob\",\"count\":999}", json);
    }

    @Test
    public void testSerializeIgnoreInterfaceGetter_UsingVisibilityConfig()
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc ->
                    vc.withVisibility(PropertyAccessor.GETTER, Visibility.NONE)
                        .withVisibility(PropertyAccessor.FIELD, Visibility.ANY)
                )
                .build();

        String json = mapper.writeValueAsString(new RecordWithInterfaceWithGetter("Bob"));

        assertEquals("{\"name\":\"Bob\"}", json);
    }

    // [databind#4157]
    @Test
    public void testWithInferGettersFromComponentsOnlyFeature()
    {
        String json = MAPPER_RESTRICTED.writeValueAsString(new RecordWithInterfaceWithGetter("Bob"));

        // With feature enabled, only the actual record component should be serialized
        assertEquals("{\"name\":\"Bob\"}", json);
    }
}
