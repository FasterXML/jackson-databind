package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class CreatorWithRenamedParamTest
    extends DatabindTestUtil
{
 // [databind#4545]
    static class Payload4545 {
        private final String key1;
        private final String key2;

        @JsonCreator
        public Payload4545(
                @ImplicitName("key1")
                @JsonProperty("key")
                String key1, // NOTE: the mismatch `key` / `key1` is important

                @ImplicitName("key2")
                @JsonProperty("key2")
                String key2
        ) {
            this.key1 = key1;
            this.key2 = key2;
        }

        public String getKey1() {
            return key1;
        }

        public String getKey2() {
            return key2;
        }
    }

    // [databind#4810]
    static class DataClass4810 {
        private String x;

        private DataClass4810(String x) {
            this.x = x;
        }
        
        @JsonProperty("bar")
        public String getFoo() {
            return x;
        }

        // NOTE: mode-less, should be properly detected as properties-based
        @JsonCreator
        public static DataClass4810 create(@ImplicitName("bar") String bar) {
            return new DataClass4810(bar);
        }
    }

    // [databind#4545]
    @Test
    public void creatorWithRename4545() throws Exception
    {
        final ObjectMapper mapper4545 = jsonMapperBuilder()
                .disable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .annotationIntrospector(new ImplicitNameIntrospector())
                .build();
        String jsonPayload = a2q("{ 'key1': 'val1', 'key2': 'val2'}");

        try {
            mapper4545.readerFor(Payload4545.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(jsonPayload);
            fail("Should not pass");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized");
            verifyException(e, "key1");
        }
    }

    // [databind#4810]
    @Test
    void shouldSupportPropertyRenaming4810() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .annotationIntrospector(new ImplicitNameIntrospector())
                .build();

        JsonNode serializationResult = mapper.valueToTree(DataClass4810.create("42"));

        assertEquals(a2q("{'bar':'42'}"), serializationResult.toString());

        DataClass4810 deserializationResult = mapper.treeToValue(serializationResult, DataClass4810.class);

        assertEquals("42", deserializationResult.getFoo());
    }
}
