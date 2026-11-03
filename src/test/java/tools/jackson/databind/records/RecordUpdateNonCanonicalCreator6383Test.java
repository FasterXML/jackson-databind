package tools.jackson.databind.records;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
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

    // Renamed and reversed: can only be linked with parameter names
    public record SurGiven(String first, String last) {
        @JsonCreator
        public static SurGiven of(@JsonProperty("sur") String last,
                @JsonProperty("given") String first) {
            return new SurGiven(first, last);
        }
    }

    // Creator adapts parameter type: accessor type not assignable to parameter
    public record Opt(String id, Optional<String> nick) {
        @JsonCreator
        public static Opt of(@JsonProperty("id") String id,
                @JsonProperty("nick") String nick) {
            return new Opt(id, Optional.ofNullable(nick));
        }
    }

    // Ignored component must keep its existing value
    public record Secret(String id, @JsonIgnore String secret) {
        @JsonCreator
        public static Secret of(@JsonProperty("secret") String secret,
                @JsonProperty("id") String id) {
            return new Secret(id, secret);
        }
    }

    // WRITE_ONLY component has no getter for deserialization: must keep its value
    public record Account(String id,
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {
        @JsonCreator
        public static Account of(@JsonProperty("password") String password,
                @JsonProperty("id") String id) {
            return new Account(id, password);
        }
    }

    // Generic component: accessor type (`Object`) differs from parameter type
    public record Box<T>(T value, String tag) {
        @JsonCreator
        public static Box<String> of(@JsonProperty("tag") String tag,
                @JsonProperty("value") String value) {
            return new Box<>(value, tag);
        }
    }

    // Parameter type narrower than component type
    public record Items(List<String> items, String tag) {
        @JsonCreator
        public static Items of(@JsonProperty("tag") String tag,
                @JsonProperty("items") ArrayList<String> items) {
            return new Items(items, tag);
        }
    }

    // Creator parameter with no Record component: no existing value to retain
    public record Temp(double celsius) {
        @JsonCreator
        public Temp(@JsonProperty("celsius") Double celsius,
                @JsonProperty("fahrenheit") Double fahrenheit) {
            this((celsius != null) ? celsius : (fahrenheit - 32.0) / 1.8);
        }
    }

    // Parameter of wider primitive type than component
    public record Wide(int count, String name) {
        @JsonCreator
        public static Wide of(@JsonProperty("name") String name,
                @JsonProperty("count") long count) {
            return new Wide((int) count, name);
        }
    }

    // Parameter of narrower primitive type than component: cannot be passed
    public record Narrow(long count, String name) {
        @JsonCreator
        public static Narrow of(@JsonProperty("name") String name,
                @JsonProperty("count") int count) {
            return new Narrow(count, name);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper NO_PARAM_NAMES_MAPPER = jsonMapperBuilder()
            .disable(MapperFeature.DETECT_PARAMETER_NAMES)
            .build();

    @Test
    public void updateWithReorderedFactoryOfSameTypes() throws Exception
    {
        FullName result = MAPPER.readerForUpdating(new FullName("John", "Smith"))
                .readValue("""
                        {"first":"Jane"}
                        """);
        assertEquals(new FullName("Jane", "Smith"), result);
    }

    @Test
    public void updateWithReorderedFactoryOfDifferentTypes() throws Exception
    {
        IdName result = MAPPER.readerForUpdating(new IdName(1, "a"))
                .readValue("""
                        {"name":"b"}
                        """);
        assertEquals(new IdName(1, "b"), result);
    }

    @Test
    public void updateWithReorderedConstructor() throws Exception
    {
        Point result = MAPPER.readerForUpdating(new Point(3, "a"))
                .readValue("""
                        {"x":4}
                        """);
        assertEquals(new Point(4, "a"), result);
    }

    @Test
    public void updateWithRenamedPropertyOnSameOrderFactory() throws Exception
    {
        Person result = MAPPER.readerForUpdating(new Person("John", 30))
                .readValue("""
                        {"age":31}
                        """);
        assertEquals(new Person("John", 31), result);
    }

    @Test
    public void updateWithNamingStrategyOnSameOrderFactory() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        Member result = mapper.readerForUpdating(new Member("John", 30))
                .readValue("""
                        {"age":31}
                        """);
        assertEquals(new Member("John", 31), result);
    }

    @Test
    public void updateWithRenamedAndReorderedFactory() throws Exception
    {
        SurGiven result = MAPPER.readerForUpdating(new SurGiven("John", "Smith"))
                .readValue("""
                        {"given":"Jane"}
                        """);
        assertEquals(new SurGiven("Jane", "Smith"), result);
    }

    @Test
    public void updateWithNoPropertiesKeepsAllValues() throws Exception
    {
        FullName result = MAPPER.readerForUpdating(new FullName("John", "Smith"))
                .readValue("{}");
        assertEquals(new FullName("John", "Smith"), result);
    }

    @Test
    public void updateKeepsIgnoredComponent() throws Exception
    {
        Secret result = MAPPER.readerForUpdating(new Secret("1", "s"))
                .readValue("""
                        {"id":"2","secret":"x"}
                        """);
        assertEquals(new Secret("2", "s"), result);
    }

    @Test
    public void updateKeepsWriteOnlyComponent() throws Exception
    {
        Account result = MAPPER.readerForUpdating(new Account("1", "pw"))
                .readValue("""
                        {"id":"2"}
                        """);
        assertEquals(new Account("2", "pw"), result);
    }

    // Type check uses the actual value, not the declared accessor type
    @Test
    public void updateWithGenericComponent() throws Exception
    {
        Box<String> result = MAPPER.readerForUpdating(new Box<>("v", "t"))
                .forType(new TypeReference<Box<String>>() { })
                .readValue("""
                        {"tag":"u"}
                        """);
        assertEquals(new Box<>("v", "u"), result);
    }

    @Test
    public void updateWithNarrowerParameterType() throws Exception
    {
        Items result = MAPPER.readerForUpdating(new Items(new ArrayList<>(List.of("a")), "t"))
                .readValue("""
                        {"tag":"u"}
                        """);
        assertEquals(new Items(List.of("a"), "u"), result);

        // but value of other type cannot be passed
        ObjectReader r = MAPPER.readerForUpdating(new Items(List.of("a"), "t"));
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> r.readValue("""
                        {"tag":"u"}
                        """));
        verifyException(e, "Cannot update Record", "'items'");
    }

    // Getters not visible: accessors are still linked by component name
    @Test
    public void updateWithGettersNotVisible() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc -> vc
                        .withVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
                        .withVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE))
                .build();
        FullName result = mapper.readerForUpdating(new FullName("John", "Smith"))
                .readValue("""
                        {"first":"Jane"}
                        """);
        assertEquals(new FullName("Jane", "Smith"), result);
    }

    // Without parameter names, renamed Creator properties cannot be linked to
    // Record accessors: must fail instead of using (possibly wrong) position
    @Test
    public void updateFailsForUnlinkedRenamedAndReorderedFactory() throws Exception
    {
        ObjectReader r = NO_PARAM_NAMES_MAPPER.readerForUpdating(new SurGiven("John", "Smith"));
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> r.readValue("""
                        {"given":"Jane"}
                        """));
        verifyException(e, "Cannot update Record", "'sur'");
    }

    @Test
    public void updateFailsForUnlinkedRenamedSameOrderFactory() throws Exception
    {
        ObjectReader r = NO_PARAM_NAMES_MAPPER.readerForUpdating(new Person("John", 30));
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> r.readValue("""
                        {"age":31}
                        """));
        verifyException(e, "Cannot update Record", "'first_name'");
    }

    // ... but if input has values for unlinked properties, update works
    @Test
    public void updateWithUnlinkedPropertiesInInput() throws Exception
    {
        SurGiven result = NO_PARAM_NAMES_MAPPER.readerForUpdating(new SurGiven("John", "Smith"))
                .readValue("""
                        {"given":"Jane","sur":"Doe"}
                        """);
        assertEquals(new SurGiven("Jane", "Doe"), result);
    }

    @Test
    public void updateFailsForIncompatibleAccessorType() throws Exception
    {
        ObjectReader r = MAPPER.readerForUpdating(new Opt("1", Optional.of("x")));
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> r.readValue("""
                        {"id":"2"}
                        """));
        verifyException(e, "Cannot update Record", "'nick'");

        Opt result = MAPPER.readerForUpdating(new Opt("1", Optional.of("x")))
                .readValue("""
                        {"id":"2","nick":"z"}
                        """);
        assertEquals(new Opt("2", Optional.of("z")), result);
    }

    @Test
    public void updateWithCreatorPropertyWithoutComponent() throws Exception
    {
        Temp result = MAPPER.readerForUpdating(new Temp(1.0))
                .readValue("""
                        {"celsius":5}
                        """);
        assertEquals(new Temp(5.0), result);

        // and with no input at all, existing value is kept
        result = MAPPER.readerForUpdating(new Temp(1.0)).readValue("{}");
        assertEquals(new Temp(1.0), result);
    }

    @Test
    public void updateWithWiderPrimitiveParameter() throws Exception
    {
        Wide result = MAPPER.readerForUpdating(new Wide(7, "a"))
                .readValue("""
                        {"name":"b"}
                        """);
        assertEquals(new Wide(7, "b"), result);
    }

    @Test
    public void updateFailsForNarrowerPrimitiveParameter() throws Exception
    {
        ObjectReader r = MAPPER.readerForUpdating(new Narrow(7L, "a"));
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> r.readValue("""
                        {"name":"b"}
                        """));
        verifyException(e, "Cannot update Record", "'count'");
    }
}
