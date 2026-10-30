package tools.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// [databind#6332]: `@JsonUnwrapped` value of a polymorphic type with a property-based
// type id. After #650, only names from `collectAllPropertyNamesTo(...)` were routed to
// unwrapped deserializers, so the type id was dropped (or rejected as unknown) even
// though the unwrapped value's type deserializer requires it.
class UnwrappedPolymorphicTypeId6332Test extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Dog.class, name = "DOG"),
        @JsonSubTypes.Type(value = Cat.class, name = "CAT"),
        @JsonSubTypes.Type(value = Puppy.class, name = "PUPPY")
    })
    interface Pet { }

    static class Dog implements Pet {
        final String name;

        @JsonCreator
        Dog(@JsonProperty("name") String name) { this.name = name; }

        public String getName() { return name; }
        public String getType() { return "DOG"; }
    }

    static class Cat implements Pet {
        @JsonCreator
        Cat(@JsonProperty("name") String name) { }

        public String getType() { return "CAT"; }
    }

    static class NewDog {
        @JsonUnwrapped
        Dog dog;

        final String note;

        @JsonCreator
        NewDog(@JsonProperty("note") String note) { this.note = note; }

        public Dog getDog() { return dog; }
        public String getNote() { return note; }
    }

    // Pre-fix workaround: no-op setter makes the type id a regular property name
    static class Puppy implements Pet {
        final String name;

        @JsonCreator
        Puppy(@JsonProperty("name") String name) { this.name = name; }

        public String getName() { return name; }
        public String getType() { return "PUPPY"; }

        @JsonProperty("type")
        private void setType(String ignored) { }
    }

    static class NewPuppy {
        @JsonUnwrapped
        Puppy puppy;

        final String note;

        @JsonCreator
        NewPuppy(@JsonProperty("note") String note) { this.note = note; }
    }

    // Same, but without a Creator (field-based)
    static class NewDogFields {
        @JsonUnwrapped
        public Dog dog;

        public String note;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({ @JsonSubTypes.Type(value = Bird.class, name = "BIRD") })
    interface Animal { }

    static class Bird implements Animal {
        public String name;
    }

    static class NewBird {
        @JsonUnwrapped
        public Bird bird;

        public String note;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper STRICT_MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private static final String DOG_JSON = """
            {"type":"DOG","name":"Rex","note":"hi"}
            """;

    @Test
    void existingPropertyTypeIdWithCreator() throws Exception
    {
        for (ObjectMapper mapper : new ObjectMapper[] { MAPPER, STRICT_MAPPER }) {
            NewDog result = mapper.readValue(DOG_JSON, NewDog.class);
            assertEquals("Rex", result.dog.name);
            assertEquals("hi", result.note);
        }
    }

    @Test
    void existingPropertyTypeIdWithFields() throws Exception
    {
        for (ObjectMapper mapper : new ObjectMapper[] { MAPPER, STRICT_MAPPER }) {
            NewDogFields result = mapper.readValue(DOG_JSON, NewDogFields.class);
            assertEquals("Rex", result.dog.name);
            assertEquals("hi", result.note);
        }
    }

    @Test
    void propertyTypeId() throws Exception
    {
        for (ObjectMapper mapper : new ObjectMapper[] { MAPPER, STRICT_MAPPER }) {
            NewBird result = mapper.readValue("""
                    {"kind":"BIRD","name":"Tweety","note":"hi"}
                    """, NewBird.class);
            assertEquals("Tweety", result.bird.name);
            assertEquals("hi", result.note);
        }
    }

    @Test
    void roundTrip() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        NewDog input = new NewDog("hi");
        input.dog = new Dog("Rex");
        NewDog result = mapper.readValue(mapper.writeValueAsString(input), NewDog.class);
        assertEquals("Rex", result.dog.name);
        assertEquals("hi", result.note);
    }

    @Test
    void typeIdAlsoDeclaredAsProperty() throws Exception
    {
        for (ObjectMapper mapper : new ObjectMapper[] { MAPPER, STRICT_MAPPER }) {
            NewPuppy result = mapper.readValue("""
                    {"type":"PUPPY","name":"Rex","note":"hi"}
                    """, NewPuppy.class);
            assertEquals("Rex", result.puppy.name);
            assertEquals("hi", result.note);
        }
        assertThrows(InvalidTypeIdException.class, () -> STRICT_MAPPER.readValue(DOG_JSON, NewPuppy.class));
    }

    @Test
    void typeIdStillValidated() throws Exception
    {
        // Sibling subtype is not assignable to unwrapped `Dog`
        assertThrows(InvalidTypeIdException.class, () -> STRICT_MAPPER.readValue("""
                {"type":"CAT","name":"Tom","note":"hi"}
                """, NewDog.class));
        // Missing type id
        assertThrows(InvalidTypeIdException.class, () -> STRICT_MAPPER.readValue("""
                {"name":"Rex","note":"hi"}
                """, NewDog.class));
    }

    // [databind#650] behavior retained: genuinely unknown properties still fail
    @Test
    void unknownPropertyStillFails() throws Exception
    {
        UnrecognizedPropertyException e = assertThrows(UnrecognizedPropertyException.class,
                () -> STRICT_MAPPER.readValue("""
                        {"type":"DOG","name":"Rex","note":"hi","bogus":1}
                        """, NewDog.class));
        assertEquals("bogus", e.getPropertyName());
    }
}
