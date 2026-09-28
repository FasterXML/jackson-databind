package tools.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.exc.IgnoredPropertyException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for [databind#6243]: names to ignore as per {@code @JsonIgnoreProperties}
 * or {@code @JsonIncludeProperties} find no Creator property, separately for each
 * contextual deserializer, so property-based deserialization handles them like any
 * other ignored property.
 */
public class IgnorePropertiesCreator6243Test extends DatabindTestUtil
{
    static class Point {
        public final int x, y;

        @JsonCreator
        public Point(@JsonProperty("x") int x, @JsonProperty("y") int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Renamed {
        public final String value;

        @JsonCreator
        public Renamed(@JsonProperty("newName") @JsonAlias("oldName") String value) {
            this.value = value;
        }
    }

    // Ignores an alias of a Creator property, not the property itself
    static class RenamedWrapper {
        @JsonIgnoreProperties("oldName")
        public Renamed ignoringAlias;

        public Renamed plain;
    }

    // Ignores a Creator property, which then must not be found by its alias either
    static class RenamedIgnoringWrapper {
        @JsonIgnoreProperties("newName")
        public Renamed ignoring;
    }

    @JsonIgnoreProperties("newName")
    static class RenamedIgnored {
        public final String value;

        @JsonCreator
        public RenamedIgnored(@JsonProperty("newName") @JsonAlias("oldName") String value) {
            this.value = value;
        }
    }

    // Ignored name differs from the Creator property's name only by case; no fallback
    // setter or field, so it can only be bound via Creator
    @JsonIgnoreProperties("NAME")
    static class NameIgnoredOtherCase {
        private final String _name;

        @JsonCreator
        public NameIgnoredOtherCase(@JsonProperty("name") String name) {
            _name = name;
        }

        public String name() { return _name; }
    }

    static class NameIgnoredOtherCaseWrapper {
        @JsonIgnoreProperties("Y")
        public Point p;
    }

    static class Points {
        @JsonIgnoreProperties("y")
        public Point ignoring;

        @JsonIncludeProperties("x")
        public Point including;

        public Point plain;
    }

    static abstract class Animal {
        public String name;
    }

    static class Dog extends Animal { }

    static class ExtTypeValue {
        public final String secret;
        public final Animal value;

        @JsonCreator
        public ExtTypeValue(@JsonProperty("secret") String secret,
                @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                        include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
                @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
                @JsonProperty("value") Animal value) {
            this.secret = secret;
            this.value = value;
        }
    }

    // Ignores the Creator property that the external type id is for
    static class ExtTypeValueWrapper {
        @JsonIgnoreProperties("value")
        public ExtTypeValue child;
    }

    // External type id property is not a bean property, so it is not included here...
    static class ExtTypeIncludeWrapper {
        @JsonIncludeProperties({ "secret", "value" })
        public ExtTypeValue child;
    }

    // ... and ignoring it (as a bean property) should not prevent its use as type id either
    static class ExtTypeIgnoreTypeWrapper {
        @JsonIgnoreProperties("type")
        public ExtTypeValue child;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void ignoralsApplyOnlyWhereDeclared() throws Exception
    {
        Points result = MAPPER.readValue("""
                {"ignoring":{"x":1,"y":2},"including":{"x":3,"y":4},"plain":{"x":5,"y":6}}
                """, Points.class);
        assertEquals(1, result.ignoring.x);
        assertEquals(0, result.ignoring.y);
        assertEquals(3, result.including.x);
        assertEquals(0, result.including.y);
        assertEquals(5, result.plain.x);
        assertEquals(6, result.plain.y);

        Point point = MAPPER.readValue("""
                {"x":7,"y":8}
                """, Point.class);
        assertEquals(8, point.y);
    }

    @Test
    public void ignoredAliasOfCreatorProperty() throws Exception
    {
        RenamedWrapper result = MAPPER.readValue("""
                {"ignoringAlias":{"oldName":"a"},"plain":{"oldName":"b"}}
                """, RenamedWrapper.class);
        assertNull(result.ignoringAlias.value);
        assertEquals("b", result.plain.value);

        result = MAPPER.readValue("""
                {"ignoringAlias":{"newName":"c"}}
                """, RenamedWrapper.class);
        assertEquals("c", result.ignoringAlias.value);
    }

    @Test
    public void ignoredCreatorPropertyNotFoundByAlias() throws Exception
    {
        RenamedIgnoringWrapper wrapper = MAPPER.readValue("""
                {"ignoring":{"oldName":"a"}}
                """, RenamedIgnoringWrapper.class);
        assertNull(wrapper.ignoring.value);

        RenamedIgnored result = MAPPER.readValue("""
                {"oldName":"b"}
                """, RenamedIgnored.class);
        assertNull(result.value);
    }

    @Test
    public void ignoredCreatorPropertyCaseInsensitive() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .build();
        Points result = mapper.readValue("""
                {"ignoring":{"x":1,"Y":2},"including":{"X":5,"Y":6},"plain":{"X":3,"Y":4}}
                """, Points.class);
        assertEquals(1, result.ignoring.x);
        assertEquals(0, result.ignoring.y);
        assertEquals(5, result.including.x);
        assertEquals(0, result.including.y);
        assertEquals(3, result.plain.x);
        assertEquals(4, result.plain.y);
    }

    // Case-insensitive matching of input does not make ignoral case-insensitive:
    // a Creator property is only ignored by its exact name
    @Test
    public void ignoredNameInOtherCaseCaseInsensitive() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .build();
        NameIgnoredOtherCase result = mapper.readValue("""
                {"name":"v"}
                """, NameIgnoredOtherCase.class);
        assertEquals("v", result.name());

        NameIgnoredOtherCaseWrapper wrapper = mapper.readValue("""
                {"p":{"x":1,"y":2}}
                """, NameIgnoredOtherCaseWrapper.class);
        assertEquals(1, wrapper.p.x);
        assertEquals(2, wrapper.p.y);
    }

    @Test
    public void ignoredCreatorPropertyReportedAsIgnored() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(Points.class)
                .with(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        IgnoredPropertyException e = assertThrows(IgnoredPropertyException.class,
                () -> reader.readValue("""
                        {"ignoring":{"x":1,"y":2}}
                        """));
        verifyException(e, "Ignored field \"y\"");
    }

    @Test
    public void ignoredExternalTypedCreatorProperty() throws Exception
    {
        ExtTypeValueWrapper result = MAPPER.readValue("""
                {"child":{"secret":"s","value":{"name":"Rex"}}}
                """, ExtTypeValueWrapper.class);
        assertEquals("s", result.child.secret);
        assertNull(result.child.value);
    }

    // External type id name is handled as type id even if not a name to include
    // (or is a name to ignore): only ignored Creator properties are skipped
    @Test
    public void externalTypeIdNotIncludedOrIgnored() throws Exception
    {
        final String json = """
                {"child":{"secret":"s","type":"dog","value":{"name":"Rex"}}}
                """;
        ExtTypeIncludeWrapper incl = MAPPER.readValue(json, ExtTypeIncludeWrapper.class);
        assertEquals("s", incl.child.secret);
        assertInstanceOf(Dog.class, incl.child.value);
        assertEquals("Rex", incl.child.value.name);

        ExtTypeIgnoreTypeWrapper ign = MAPPER.readValue(json, ExtTypeIgnoreTypeWrapper.class);
        assertEquals("s", ign.child.secret);
        assertInstanceOf(Dog.class, ign.child.value);
        assertEquals("Rex", ign.child.value.name);
    }
}
