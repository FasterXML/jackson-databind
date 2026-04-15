package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5897]: fast path that skips TokenBuffer allocation for unknown
// properties must preserve observable behavior across the relevant toggles.
public class RecordUnknownProps5897Test extends DatabindTestUtil
{
    record Point(int x, int y) { }

    private static final String JSON_WITH_EXTRAS =
            "{\"x\":1,\"extra\":{\"nested\":[1,2,3]},\"y\":2,\"trailing\":\"ignored\"}";

    @Test
    public void testSkipUnknowns_defaultConfig() throws Exception {
        JsonMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        Point p = mapper.readValue(JSON_WITH_EXTRAS, Point.class);
        assertEquals(1, p.x());
        assertEquals(2, p.y());
    }

    @Test
    public void testFailOnUnknown_stillThrows() throws Exception {
        JsonMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        assertThrows(UnrecognizedPropertyException.class,
                () -> mapper.readValue(JSON_WITH_EXTRAS, Point.class));
    }

    @Test
    public void testProblemHandler_stillInvoked() throws Exception {
        final java.util.List<String> seen = new java.util.ArrayList<>();
        JsonMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public boolean handleUnknownProperty(
                            tools.jackson.databind.DeserializationContext ctxt,
                            tools.jackson.core.JsonParser p,
                            tools.jackson.databind.ValueDeserializer<?> deserializer,
                            Object beanOrClass, String propertyName) {
                        seen.add(propertyName);
                        try { p.skipChildren(); } catch (Exception e) { throw new RuntimeException(e); }
                        return true;
                    }
                })
                .build();
        Point p = mapper.readValue(JSON_WITH_EXTRAS, Point.class);
        assertEquals(1, p.x());
        assertEquals(2, p.y());
        assertTrue(seen.contains("extra"), "handler saw: " + seen);
        assertTrue(seen.contains("trailing"), "handler saw: " + seen);
    }

    // Non-final base + property-based creator + polymorphism: tokens unknown to
    // the base may be known to the resolved subtype, so the TokenBuffer must be
    // preserved for replay. This is the scenario that the `_beanType.isFinal()`
    // guard exists to protect — if the fast-path ever triggered for non-final
    // types, sub-only properties would be silently dropped.
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
    @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
    static class Animal {
        public final String name;
        @JsonCreator
        public Animal(@JsonProperty("name") String name) { this.name = name; }
    }

    static class Dog extends Animal {
        public String breed;
        @JsonCreator
        public Dog(@JsonProperty("name") String name, @JsonProperty("breed") String breed) {
            super(name);
            this.breed = breed;
        }
    }

    @Test
    public void testNonFinalPolymorphic_subPropertyPreserved() throws Exception {
        JsonMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        // "breed" appears before the type discriminator and is unknown to Animal;
        // it must be buffered and replayed through Dog's deserializer.
        String json = "{\"breed\":\"poodle\",\"name\":\"Rex\",\"kind\":\"dog\"}";
        Animal a = mapper.readValue(json, Animal.class);
        assertInstanceOf(Dog.class, a);
        assertEquals("Rex", a.name);
        assertEquals("poodle", ((Dog) a).breed);
    }
}
