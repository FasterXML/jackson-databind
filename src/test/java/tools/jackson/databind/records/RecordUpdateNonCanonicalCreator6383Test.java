package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6383]: Record update must not assume that Creator parameter
// order matches Record component order
public class RecordUpdateNonCanonicalCreator6383Test extends DatabindTestUtil
{
    // Same parameter types, reversed order: wrong mapping goes unnoticed
    public record FullName(String first, String last) {
        @JsonCreator
        public static FullName of(@JsonProperty("last") String last,
                @JsonProperty("first") String first) {
            return new FullName(first, last);
        }
    }

    // Different parameter types, reversed order
    public record IdName(int id, String name) {
        @JsonCreator
        public static IdName of(@JsonProperty("name") String name,
                @JsonProperty("id") int id) {
            return new IdName(id, name);
        }
    }

    // Non-canonical constructor with reversed order
    public record Point(int x, String label) {
        @JsonCreator
        public Point(@JsonProperty("label") String label, @JsonProperty("x") int x) {
            this(x, label);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void updateWithReorderedFactoryOfSameTypes() throws Exception
    {
        FullName result = MAPPER.readerForUpdating(new FullName("John", "Smith"))
                .readValue(a2q("{'first':'Jane'}"));
        assertEquals(new FullName("Jane", "Smith"), result);
    }

    @Test
    public void updateWithReorderedFactoryOfDifferentTypes() throws Exception
    {
        IdName result = MAPPER.readerForUpdating(new IdName(1, "a"))
                .readValue(a2q("{'name':'b'}"));
        assertEquals(new IdName(1, "b"), result);
    }

    @Test
    public void updateWithReorderedConstructor() throws Exception
    {
        Point result = MAPPER.readerForUpdating(new Point(3, "a"))
                .readValue(a2q("{'x':4}"));
        assertEquals(new Point(4, "a"), result);
    }

    @Test
    public void updateWithNoPropertiesKeepsAllValues() throws Exception
    {
        FullName result = MAPPER.readerForUpdating(new FullName("John", "Smith"))
                .readValue("{}");
        assertEquals(new FullName("John", "Smith"), result);
    }
}
