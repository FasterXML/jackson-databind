package tools.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for issue #4277: Combining {@code @JsonFormat(shape=ARRAY)} with
 * {@code @JsonTypeInfo(include=EXTERNAL_PROPERTY)} should fail with a clear
 * error message, as these features are architecturally incompatible.
 */
public class ExternalPropertyWithArrayShape4277Test extends DatabindTestUtil
{
    // Base classes for polymorphism
    static class Animal {
        public String name;

        protected Animal() { }
        public Animal(String n) { name = n; }
    }

    @JsonTypeName("cat")
    static class Cat extends Animal {
        public Cat() { }
        public Cat(String n) { super(n); }
    }

    @JsonTypeName("dog")
    static class Dog extends Animal {
        public Dog() { }
        public Dog(String n) { super(n); }
    }

    // Test class combining ARRAY shape with EXTERNAL_PROPERTY (should fail)
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"type", "uniqueId", "animal"})
    static class WrapperWithExternalProperty {
        public String type;
        public String uniqueId;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                      include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                      property = "type")
        @JsonSubTypes({
            @JsonSubTypes.Type(value = Cat.class, name = "cat"),
            @JsonSubTypes.Type(value = Dog.class, name = "dog")
        })
        public Animal animal;

        protected WrapperWithExternalProperty() { }
        public WrapperWithExternalProperty(String type, String id, Animal a) {
            this.type = type;
            this.uniqueId = id;
            this.animal = a;
        }
    }

    // Test class with ARRAY shape and PROPERTY inclusion (should work)
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"uniqueId", "animal"})
    static class WrapperWithPropertyInclusion {
        public String uniqueId;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                      include = JsonTypeInfo.As.PROPERTY,
                      property = "type")
        @JsonSubTypes({
            @JsonSubTypes.Type(value = Cat.class, name = "cat"),
            @JsonSubTypes.Type(value = Dog.class, name = "dog")
        })
        public Animal animal;

        protected WrapperWithPropertyInclusion() { }
        public WrapperWithPropertyInclusion(String id, Animal a) {
            this.uniqueId = id;
            this.animal = a;
        }
    }

    // Test class with ARRAY shape and WRAPPER_ARRAY inclusion (should work)
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"uniqueId", "animal"})
    static class WrapperWithWrapperArrayInclusion {
        public String uniqueId;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                      include = JsonTypeInfo.As.WRAPPER_ARRAY)
        @JsonSubTypes({
            @JsonSubTypes.Type(value = Cat.class, name = "cat"),
            @JsonSubTypes.Type(value = Dog.class, name = "dog")
        })
        public Animal animal;

        protected WrapperWithWrapperArrayInclusion() { }
        public WrapperWithWrapperArrayInclusion(String id, Animal a) {
            this.uniqueId = id;
            this.animal = a;
        }
    }


    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .registerSubtypes(Cat.class, Dog.class)
            .build();

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    /**
     * Main test for issue #4277: combining EXTERNAL_PROPERTY with ARRAY shape
     * should fail with a clear error message.
     */
    @Test
    public void testExternalPropertyWithArrayShapeFailsClearly() throws Exception
    {
        // Try to deserialize - should fail with clear error
        String json = "[\"cat\",\"id123\",{\"name\":\"Fluffy\"}]";

        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class, () -> {
            MAPPER.readValue(json, WrapperWithExternalProperty.class);
        });

        // Verify error message is present
        assertNotNull(e.getMessage());
    }

    /**
     * Verify that the error message contains helpful information about
     * why the combination doesn't work and what alternatives exist.
     */
    @Test
    public void testErrorMessageContainsAlternatives() throws Exception
    {
        String json = "[\"cat\",\"id123\",{\"name\":\"Fluffy\"}]";

        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class, () -> {
            MAPPER.readValue(json, WrapperWithExternalProperty.class);
        });

        String msg = e.getMessage();

        // Check that error mentions both features
        assertTrue(msg.contains("ARRAY") || msg.contains("array"),
                "Error should mention ARRAY shape: " + msg);
        assertTrue(msg.contains("EXTERNAL_PROPERTY") || msg.contains("external"),
                "Error should mention EXTERNAL_PROPERTY: " + msg);

        // Check that error mentions at least one alternative
        assertTrue(msg.contains("PROPERTY") || msg.contains("WRAPPER_ARRAY") ||
                   msg.contains("alternative") || msg.contains("custom deserializer"),
                "Error should mention alternatives: " + msg);
    }

    /**
     * Verify that ARRAY shape works fine with PROPERTY inclusion (type ID inside object).
     */
    @Test
    public void testArrayShapeWithPropertyInclusion() throws Exception
    {
        WrapperWithPropertyInclusion input = new WrapperWithPropertyInclusion("id123",
                new Cat("Fluffy"));

        // Serialize
        String json = MAPPER.writeValueAsString(input);

        // JSON should be an array with embedded type
        assertTrue(json.startsWith("["), "Should be JSON array");

        // Deserialize
        WrapperWithPropertyInclusion result = MAPPER.readValue(json,
                WrapperWithPropertyInclusion.class);

        assertNotNull(result);
        assertEquals("id123", result.uniqueId);
        assertNotNull(result.animal);
        assertTrue(result.animal instanceof Cat);
        assertEquals("Fluffy", result.animal.name);
    }

    /**
     * Verify that ARRAY shape works fine with WRAPPER_ARRAY inclusion.
     */
    @Test
    public void testArrayShapeWithWrapperArrayInclusion() throws Exception
    {
        WrapperWithWrapperArrayInclusion input = new WrapperWithWrapperArrayInclusion("id123",
                new Cat("Fluffy"));

        // Serialize
        String json = MAPPER.writeValueAsString(input);

        // JSON should be an array
        assertTrue(json.startsWith("["), "Should be JSON array");

        // Deserialize
        WrapperWithWrapperArrayInclusion result = MAPPER.readValue(json,
                WrapperWithWrapperArrayInclusion.class);

        assertNotNull(result);
        assertEquals("id123", result.uniqueId);
        assertNotNull(result.animal);
        assertTrue(result.animal instanceof Cat);
        assertEquals("Fluffy", result.animal.name);
    }

}
