package tools.jackson.databind.json;

import org.junit.jupiter.api.Test;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
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
    }

    // Test 1: Builder with stream read features
    @Test
    public void testBuilderWithStreamReadFeatures() {
        JsonMapper mapper = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
            .build();

        assertNotNull(mapper);
        assertTrue(mapper.isEnabled(StreamReadFeature.STRICT_DUPLICATE_DETECTION));
        assertFalse(mapper.isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE));
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
}
