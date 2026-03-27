package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// For [databind#5840]: no-arg @JsonCreator and multi-arg @JsonCreator should co-exist
public class JsonCreatorDefaultAndPropertiesCtors5840Test extends DatabindTestUtil
{
    // Class with both a no-arg @JsonCreator (default values) and a multi-arg @JsonCreator
    static final class AllDefaultsBean
    {
        private final String name;
        private final int count;

        @JsonCreator
        public AllDefaultsBean(
                @JsonProperty("name") String name,
                @JsonProperty("count") int count
        ) {
            this.name = name;
            this.count = count;
        }

        @JsonCreator
        public AllDefaultsBean() {
            this("default-name", 42);
        }

        public String getName() { return name; }
        public int getCount() { return count; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#5840]: verify no exception is thrown and multi-arg creator is used
    @Test
    public void testDeserWithPropertiesCreator5840() throws Exception
    {
        AllDefaultsBean result = MAPPER.readValue(
                "{\"name\":\"hello\", \"count\":7}", AllDefaultsBean.class);
        assertNotNull(result);
        assertEquals("hello", result.getName());
        assertEquals(7, result.getCount());
    }

    // [databind#5840]: verify no-arg @JsonCreator is used when JSON is empty
    @Test
    public void testDeserWithDefaultCreator5840() throws Exception
    {
        AllDefaultsBean result = MAPPER.readValue("{}", AllDefaultsBean.class);
        assertNotNull(result);
        assertEquals("default-name", result.getName());
        assertEquals(42, result.getCount());
    }
}
