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

    // Same order as components, but external name differs from component name
    public record Person(String firstName, int age) {
        @JsonCreator
        public static Person of(@JsonProperty("first_name") String firstName,
                @JsonProperty("age") int age) {
            return new Person(firstName, age);
        }
    }

    // Same order as components, names come from naming strategy
    public record Member(String firstName, int age) {
        @JsonCreator
        public static Member of(String firstName, int age) {
            return new Member(firstName, age);
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
    public void updateWithRenamedPropertyOnSameOrderFactory() throws Exception
    {
        Person result = MAPPER.readerForUpdating(new Person("John", 30))
                .readValue(a2q("{'age':31}"));
        assertEquals(new Person("John", 31), result);
    }

    @Test
    public void updateWithNamingStrategyOnSameOrderFactory() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        Member result = mapper.readerForUpdating(new Member("John", 30))
                .readValue(a2q("{'age':31}"));
        assertEquals(new Member("John", 31), result);
    }

    // Without parameter names, renamed properties can only be matched by position
    @Test
    public void updateWithRenamedPropertyWithoutParameterNames() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.DETECT_PARAMETER_NAMES)
                .build();
        Person result = mapper.readerForUpdating(new Person("John", 30))
                .readValue(a2q("{'age':31}"));
        assertEquals(new Person("John", 31), result);
    }

    @Test
    public void updateWithNoPropertiesKeepsAllValues() throws Exception
    {
        FullName result = MAPPER.readerForUpdating(new FullName("John", "Smith"))
                .readValue("{}");
        assertEquals(new FullName("John", "Smith"), result);
    }
}
