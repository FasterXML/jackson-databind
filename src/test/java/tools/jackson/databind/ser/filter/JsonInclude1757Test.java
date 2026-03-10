package tools.jackson.databind.ser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#1757]: Global NON_DEFAULT should use real bean defaults
//  for round-trip consistency when USE_REAL_NON_DEFAULT is enabled
public class JsonInclude1757Test extends DatabindTestUtil
{
    static class Entity {
        private String someFieldWithDefault = "a default";

        public void setSomeFieldWithDefault(String someFieldWithDefault) {
            this.someFieldWithDefault = someFieldWithDefault;
        }

        public String getSomeFieldWithDefault() {
            return someFieldWithDefault;
        }
    }

    static class IntDefaultEntity {
        public int value = 42;
    }

    // [databind#1757]: default values should round-trip with global NON_DEFAULT
    @Test
    public void testGlobalNonDefaultRoundTrip() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.USE_REAL_INCLUDE_NON_DEFAULT)
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();

        // Serializing a default instance should produce {}
        Entity entity = new Entity();
        String json = mapper.writeValueAsString(entity);
        assertEquals("{}", json);

        // Deserializing {} should restore the default value
        Entity deserialized = mapper.readValue(json, Entity.class);
        assertEquals("a default", deserialized.getSomeFieldWithDefault());
    }

    // [databind#1757]: non-default values should be included
    @Test
    public void testGlobalNonDefaultIncludesNonDefault() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.USE_REAL_INCLUDE_NON_DEFAULT)
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();

        Entity entity = new Entity();
        entity.setSomeFieldWithDefault("custom value");
        String json = mapper.writeValueAsString(entity);
        assertEquals(a2q("{'someFieldWithDefault':'custom value'}"), json);
    }

    // [databind#1757]: empty string should not be suppressed if it's not the default
    @Test
    public void testGlobalNonDefaultEmptyStringNonDefault() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.USE_REAL_INCLUDE_NON_DEFAULT)
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();

        Entity entity = new Entity();
        entity.setSomeFieldWithDefault("");
        String json = mapper.writeValueAsString(entity);
        // Empty string is NOT the default ("a default"), so it should be included
        assertEquals(a2q("{'someFieldWithDefault':''}"), json);
    }

    // [databind#1757]: null should be suppressed with global NON_DEFAULT
    @Test
    public void testGlobalNonDefaultSuppressesNull() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.USE_REAL_INCLUDE_NON_DEFAULT)
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();

        Entity entity = new Entity();
        entity.setSomeFieldWithDefault(null);
        String json = mapper.writeValueAsString(entity);
        assertEquals("{}", json);
    }

    // [databind#1757]: global NON_DEFAULT with primitive field having non-zero default
    @Test
    public void testGlobalNonDefaultPrimitiveField() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.USE_REAL_INCLUDE_NON_DEFAULT)
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();

        // Default value (42) should be suppressed
        IntDefaultEntity entity = new IntDefaultEntity();
        assertEquals("{}", mapper.writeValueAsString(entity));

        // Non-default value should be included
        entity.value = 0;
        assertEquals(a2q("{'value':0}"), mapper.writeValueAsString(entity));

        // Another non-default value
        entity.value = 99;
        assertEquals(a2q("{'value':99}"), mapper.writeValueAsString(entity));
    }

    // [databind#1757]: global NON_DEFAULT should match per-class NON_DEFAULT behavior
    @Test
    public void testGlobalMatchesPerClassBehavior() throws Exception {
        ObjectMapper globalMapper = jsonMapperBuilder()
                .enable(MapperFeature.USE_REAL_INCLUDE_NON_DEFAULT)
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();

        ObjectMapper defaultMapper = newJsonMapper();

        // Per-class annotated bean
        AnnotatedEntity annotated = new AnnotatedEntity();
        String perClassJson = defaultMapper.writeValueAsString(annotated);

        // Global NON_DEFAULT bean (same structure, no annotation)
        Entity global = new Entity();
        String globalJson = globalMapper.writeValueAsString(global);

        // Both should produce {}
        assertEquals("{}", perClassJson);
        assertEquals("{}", globalJson);
    }

    // [databind#1757]: Without USE_REAL_NON_DEFAULT, old behavior is preserved
    @Test
    public void testOldBehaviorWithoutFeature() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                // USE_REAL_NON_DEFAULT is disabled by default
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();

        // With old behavior (Mode 2), String type default is "".
        // "a default" != "" so the field IS included.
        Entity entity = new Entity();
        String json = mapper.writeValueAsString(entity);
        assertEquals(a2q("{'someFieldWithDefault':'a default'}"), json);
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class AnnotatedEntity {
        private String someFieldWithDefault = "a default";

        public void setSomeFieldWithDefault(String v) {
            this.someFieldWithDefault = v;
        }

        public String getSomeFieldWithDefault() {
            return someFieldWithDefault;
        }
    }
}
