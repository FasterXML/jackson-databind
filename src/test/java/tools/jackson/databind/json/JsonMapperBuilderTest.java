package tools.jackson.databind.json;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.*;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Test(s) to verify behaviors in JsonMapper.Builder
public class JsonMapperBuilderTest extends DatabindTestUtil
{
    @Test
    public void testBuilderWithJackson2Defaults()
    {
        ObjectMapper mapper = JsonMapper.builderWithJackson2Defaults().build();
        JsonFactory jsonFactory = (JsonFactory) mapper.tokenStreamFactory();
        assertFalse(mapper.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        assertFalse(mapper.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        assertFalse(jsonFactory.isEnabled(JsonWriteFeature.ESCAPE_FORWARD_SLASHES));
        assertFalse(jsonFactory.isEnabled(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8));
        assertTrue(mapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
        assertTrue(mapper.isEnabled(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS));
        assertTrue(mapper.isEnabled(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS));
        assertFalse(mapper.isEnabled(EnumFeature.WRITE_ENUMS_USING_TO_STRING));
        assertTrue(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES));
        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS));
        assertFalse(mapper.isEnabled(EnumFeature.READ_ENUMS_USING_TO_STRING));
        assertTrue(mapper.isEnabled(MapperFeature.USE_GETTERS_AS_SETTERS));
        assertTrue(mapper.isEnabled(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS));
        assertFalse(mapper.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        // [databind#6011]
        assertFalse(mapper.isEnabled(MapperFeature.FIX_FIELD_NAME_UPPER_CASE_PREFIX));
    }

    // Test 1: Builder with stream read features
    @Test
    public void testBuilderWithStreamReadFeatures() {
        JsonMapper mapper = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
            .configure(StreamReadFeature.IGNORE_UNDEFINED, true)
            .build();

        assertNotNull(mapper);
        assertTrue(mapper.isEnabled(StreamReadFeature.STRICT_DUPLICATE_DETECTION));
        assertFalse(mapper.isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE));
        assertTrue(mapper.isEnabled(StreamReadFeature.IGNORE_UNDEFINED));
    }

    @Test
    public void testBuilderWithStreamWriteFeatures() {
        JsonMapper mapper = JsonMapper.builder()
            .enable(StreamWriteFeature.STRICT_DUPLICATE_DETECTION)
            .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
            .configure(StreamWriteFeature.IGNORE_UNKNOWN, true)
            .build();

        assertNotNull(mapper);
        assertTrue(mapper.isEnabled(StreamWriteFeature.STRICT_DUPLICATE_DETECTION));
        assertFalse(mapper.isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET));
        assertTrue(mapper.isEnabled(StreamWriteFeature.IGNORE_UNKNOWN));
    }

    @Test
    public void testBuilderWithJsonReadFeatures() {
        JsonMapper mapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .disable(JsonReadFeature.ALLOW_MISSING_VALUES)
            .configure(JsonReadFeature.ALLOW_TRAILING_COMMA, false)
            .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, true)
            .build();

        assertNotNull(mapper);
        assertTrue(mapper.isEnabled(JsonReadFeature.ALLOW_JAVA_COMMENTS));
        assertFalse(mapper.isEnabled(JsonReadFeature.ALLOW_MISSING_VALUES));
        assertFalse(mapper.isEnabled(JsonReadFeature.ALLOW_TRAILING_COMMA));
        assertTrue(mapper.isEnabled(JsonReadFeature.ALLOW_SINGLE_QUOTES));
    }

    @Test
    public void testBuilderWithJsonWriteFeatures() {
        JsonMapper mapper = JsonMapper.builder()
                .enable(JsonWriteFeature.ESCAPE_NON_ASCII)
                .disable(JsonWriteFeature.QUOTE_PROPERTY_NAMES)
                .configure(JsonWriteFeature.ESCAPE_FORWARD_SLASHES, false)
                .configure(JsonWriteFeature.WRITE_HEX_UPPER_CASE, true)
                .build();

            assertNotNull(mapper);
            assertTrue(mapper.isEnabled(JsonWriteFeature.ESCAPE_NON_ASCII));
            assertFalse(mapper.isEnabled(JsonWriteFeature.QUOTE_PROPERTY_NAMES));
            assertFalse(mapper.isEnabled(JsonWriteFeature.ESCAPE_FORWARD_SLASHES));
            assertTrue(mapper.isEnabled(JsonWriteFeature.WRITE_HEX_UPPER_CASE));
    }

    // Test 2: Builder with mapper features
    @Test
    public void testBuilderWithMapperFeatures() {
        JsonMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .disable(MapperFeature.USE_GETTERS_AS_SETTERS)
            .build();

        assertNotNull(mapper);
        assertTrue(mapper.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        assertFalse(mapper.isEnabled(MapperFeature.USE_GETTERS_AS_SETTERS));
    }

    // Test 3: Builder with multiple feature configurations
    @Test
    public void testBuilderWithMultipleFeatures() {
        JsonMapper mapper = JsonMapper.builder()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

        assertNotNull(mapper);
        assertTrue(mapper.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertTrue(mapper.isEnabled(SerializationFeature.INDENT_OUTPUT));
    }

    // Test 4: Builder configuration inheritance
    @Test
    public void testBuilderConfigurationChaining() {
        JsonMapper.Builder builder = JsonMapper.builder();
        builder.enable(SerializationFeature.INDENT_OUTPUT);
        builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        JsonMapper mapper = builder.build();
        assertTrue(mapper.isEnabled(SerializationFeature.INDENT_OUTPUT));
        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    // Test 5: Builder creates independent mappers
    @Test
    public void testBuilderCreatesIndependentMappers() {
        JsonMapper.Builder builder = JsonMapper.builder();
        JsonMapper mapper1 = builder.build();
        JsonMapper mapper2 = builder.build();

        // Mappers should be different instances
        assertNotSame(mapper1, mapper2);

        // But should have same configuration
        assertEquals(mapper1.isEnabled(SerializationFeature.INDENT_OUTPUT),
                     mapper2.isEnabled(SerializationFeature.INDENT_OUTPUT));
    }

    @Test
    void configureForJackson2IncludesUnannotatedByDefault() throws Exception {
        _assertViewBehavior(JsonMapper.builder().configureForJackson2().build(), true);
    }

    @Test
    void builderWithJackson2DefaultsIncludesUnannotatedByDefault() throws Exception {
        _assertViewBehavior(JsonMapper.builderWithJackson2Defaults().build(), true);
    }

    @Test
    void mapperBuilderWithoutCompatibilityExcludesUnannotatedByDefault() throws Exception {
        _assertViewBehavior(JsonMapper.builder().build(), false);
    }

    @Test
    void disablingDefaultViewInclusionOverridesCompatibilityBuilders() throws Exception {
        ObjectMapper mapper1 = JsonMapper.builder().configureForJackson2()
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION).build();
        ObjectMapper mapper2 = JsonMapper.builderWithJackson2Defaults()
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION).build();
        for (ObjectMapper mapper : new ObjectMapper[] { mapper1, mapper2 }) {
            _assertViewBehavior(mapper, false);
        }
    }

    private void _assertViewBehavior(ObjectMapper mapper, boolean includeUnannotated)
            throws Exception {
        String serialized = mapper.writerWithView(PublicView.class)
                .writeValueAsString(new ViewBean());
        JsonNode expected = includeUnannotated
                ? mapper.readTree("""
                        {"annotated":1,"unannotated":2}
                        """)
                : mapper.readTree("""
                        {"annotated":1}
                        """);
        assertEquals(expected, mapper.readTree(serialized));

        String input = """
                {"annotated":3,"unannotated":4}
                """;
        ViewBean bean = mapper.readerWithView(PublicView.class)
                .forType(ViewBean.class).readValue(input);
        assertEquals(3, bean.annotated);
        if (includeUnannotated) {
            assertEquals(4, bean.unannotated);
        } else {
            assertEquals(2, bean.unannotated);
        }
    }

    public static class PublicView { }

    public static class ViewBean {
        @JsonView(PublicView.class)
        public int annotated = 1;

        public int unannotated = 2;
    }
}
