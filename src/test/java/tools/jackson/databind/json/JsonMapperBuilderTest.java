package tools.jackson.databind.json;

import org.junit.jupiter.api.Test;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
