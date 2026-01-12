package tools.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#4708]: DEDUCTION mode should ignore abstract classes
 * and interfaces since they cannot be instantiated.
 */
public class DeductionWithAbstractSubtype4708Test extends DatabindTestUtil
{
    // Simulating Kotlin sealed class hierarchy with abstract intermediate class
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({
        @JsonSubTypes.Type(Ingredient.AbstractItemById.class), // Abstract class registered!
        @JsonSubTypes.Type(Ingredient.ItemById.class),
        @JsonSubTypes.Type(Ingredient.ItemByTag.class)
    })
    sealed interface Ingredient permits Ingredient.Item {

        @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
        @JsonSubTypes({
            @JsonSubTypes.Type(Ingredient.AbstractItemById.class), // Abstract class registered!
            @JsonSubTypes.Type(Ingredient.ItemById.class),
            @JsonSubTypes.Type(Ingredient.ItemByTag.class)
        })
        sealed interface Item extends Ingredient
            permits Ingredient.AbstractItemById, Ingredient.ItemByTag {
        }

        // Abstract class with properties - should be IGNORED during deduction
        // Previously this would cause signature conflicts
        non-sealed abstract class AbstractItemById implements Item {
            @JsonProperty("item")
            public String id;
            public int count = 1;

            public AbstractItemById() {}
            public AbstractItemById(String id, int count) {
                this.id = id;
                this.count = count;
            }
        }

        // Concrete implementation of the abstract class
        final class ItemById extends AbstractItemById {
            public ItemById() {}
            public ItemById(String id, int count) {
                super(id, count);
            }
        }

        // Another concrete class with different signature
        final class ItemByTag implements Item {
            @JsonProperty("tag")
            public String tag;
            public int count = 1;

            public ItemByTag() {}
            public ItemByTag(String tag, int count) {
                this.tag = tag;
                this.count = count;
            }
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testDeductionWithAbstractIntermediateClass() throws Exception
    {
        // Should deduce to ItemById, ignoring the abstract AbstractItemById
        String json1 = a2q("{'item':'minecraft:stone','count':64}");
        Ingredient result1 = MAPPER.readValue(json1, Ingredient.class);

        assertNotNull(result1);
        assertTrue(result1 instanceof Ingredient.ItemById,
            "Should deserialize to concrete ItemById, not abstract class");
        Ingredient.ItemById item1 = (Ingredient.ItemById) result1;
        assertEquals("minecraft:stone", item1.id);
        assertEquals(64, item1.count);

        // Should deduce to ItemByTag
        String json2 = a2q("{'tag':'minecraft:logs','count':32}");
        Ingredient result2 = MAPPER.readValue(json2, Ingredient.class);

        assertNotNull(result2);
        assertTrue(result2 instanceof Ingredient.ItemByTag);
        Ingredient.ItemByTag item2 = (Ingredient.ItemByTag) result2;
        assertEquals("minecraft:logs", item2.tag);
        assertEquals(32, item2.count);
    }

    @Test
    public void testDeductionWithItemInterface() throws Exception
    {
        // When deserializing as Item interface, should also work
        String json = a2q("{'item':'test','count':1}");

        JavaType itemType = MAPPER.constructType(Ingredient.Item.class);
        Ingredient.Item result = MAPPER.readValue(json, itemType);

        assertNotNull(result);
        assertTrue(result instanceof Ingredient.ItemById);
        assertEquals("test", ((Ingredient.ItemById) result).id);
    }

    // Simpler test case with just abstract class and concrete subclass
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({
        @JsonSubTypes.Type(Animal.class),         // Abstract class registered!
        @JsonSubTypes.Type(ConcreteAnimal.class)
    })
    abstract static class Animal {
        public String name;
    }

    static class ConcreteAnimal extends Animal {
        public int age;
    }

    @Test
    public void testSimpleAbstractClassIgnored() throws Exception
    {
        // Abstract Animal should be ignored, only ConcreteAnimal should be considered
        String json = a2q("{'name':'Fido','age':5}");
        Animal result = MAPPER.readValue(json, Animal.class);

        assertNotNull(result);
        assertTrue(result instanceof ConcreteAnimal);
        assertEquals("Fido", result.name);
        assertEquals(5, ((ConcreteAnimal) result).age);
    }

    // Test with interface in the mix
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({
        @JsonSubTypes.Type(Dog.class),
        @JsonSubTypes.Type(Cat.class)
    })
    interface Pet {
        // Interface should be ignored
    }

    static class Dog implements Pet {
        public String breed;
    }

    static class Cat implements Pet {
        public boolean indoor;
    }

    @Test
    public void testInterfaceIgnored() throws Exception
    {
        // Interface Pet should be ignored during fingerprinting
        String json1 = a2q("{'breed':'Labrador'}");
        Pet result1 = MAPPER.readValue(json1, Pet.class);

        assertNotNull(result1);
        assertTrue(result1 instanceof Dog);
        assertEquals("Labrador", ((Dog) result1).breed);

        String json2 = a2q("{'indoor':true}");
        Pet result2 = MAPPER.readValue(json2, Pet.class);

        assertNotNull(result2);
        assertTrue(result2 instanceof Cat);
        assertTrue(((Cat) result2).indoor);
    }

    // Test that the feature can be disabled to get old behavior
    @Test
    public void testFeatureCanBeDisabled() throws Exception
    {
        // When feature is disabled, abstract types participate in deduction
        // which causes signature conflicts (old buggy behavior)
        ObjectMapper mapper = jsonMapperBuilder()
            .disable(DeserializationFeature.IGNORE_ABSTRACT_TYPES_FOR_DEDUCTION)
            .build();

        String json = a2q("{'item':'minecraft:stone','count':64}");

        try {
            mapper.readValue(json, Ingredient.class);
            fail("Should have failed with signature conflict");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Subtypes");
            verifyException(e, "have the same signature");
            verifyException(e, "cannot be uniquely deduced");
            // Verify it mentions both the abstract class and concrete class
            verifyException(e, "AbstractItemById");
            verifyException(e, "ItemById");
        }
    }

    // Test that feature is enabled by default
    @Test
    public void testFeatureEnabledByDefault() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        assertTrue(mapper.isEnabled(DeserializationFeature.IGNORE_ABSTRACT_TYPES_FOR_DEDUCTION),
            "IGNORE_ABSTRACT_TYPES_FOR_DEDUCTION should be enabled by default");
    }
}
