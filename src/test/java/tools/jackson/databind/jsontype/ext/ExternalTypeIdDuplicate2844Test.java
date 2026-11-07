package tools.jackson.databind.jsontype.ext;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#2844]: EXTERNAL_PROPERTY type id duplicated when subtype
// has a bean property with the same name as the type id property.
public class ExternalTypeIdDuplicate2844Test extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type",
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
    @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
    static abstract class AnimalDetails {
    }

    @JsonTypeName("dog")
    static class Dog extends AnimalDetails {
        public String type;

        public Dog() { }
        public Dog(String type) { this.type = type; }
    }

    static class Animal {
        public String type;
        public String name;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type",
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
        @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
        public AnimalDetails animalDetails;

        public Animal() { }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Direct root-level serialization: bean's own "type" property should win;
    // no duplicate type id key should be emitted.
    @Test
    public void testDirectSerializationSuppressesDuplicateTypeId() throws Exception
    {
        Dog dog = new Dog("GermanShepherd");
        String json = MAPPER.writerFor(AnimalDetails.class).writeValueAsString(dog);
        assertEquals(a2q("{'type':'GermanShepherd'}"), json);
    }

    // Targeting the subtype directly should also not emit two type keys.
    // The reported bug output was: {"type":"dog","type":"GermanShepherd"}
    @Test
    public void testDirectSerializationTargetingSubtype() throws Exception
    {
        Dog dog = new Dog("GermanShepherd");
        String json = MAPPER.writeValueAsString(dog);
        assertEquals(a2q("{'type':'GermanShepherd'}"), json);
    }

    // Regression: wrapped case must still round-trip correctly. The outer
    // Animal has its own "type" discriminator; the inner Dog carries its
    // own "type" field.
    @Test
    public void testWrappedCaseUnchanged() throws Exception
    {
        Animal animal = new Animal();
        animal.type = "dog";
        animal.name = "Rex";
        animal.animalDetails = new Dog("GermanShepherd");

        String json = MAPPER.writeValueAsString(animal);
        Animal back = MAPPER.readValue(json, Animal.class);
        assertNotNull(back.animalDetails);
        assertInstanceOf(Dog.class, back.animalDetails);
        assertEquals("GermanShepherd", ((Dog) back.animalDetails).type);
        assertEquals("dog", back.type);
        assertEquals("Rex", back.name);
    }
}
