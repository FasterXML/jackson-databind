package tools.jackson.databind.objectid;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for [databind#4433]: Deserialization fails when using
 * {@code @JsonIdentityInfo} + {@code @JsonTypeInfo} with self-referencing
 * type that only has an all-args constructor (no default constructor).
 */
public class ObjectIdWithPolymorphicCreator4433Test extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Dog.class, name = "Dog"),
        @JsonSubTypes.Type(value = Cat.class, name = "Cat")
    })
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    interface Animal {
        String getName();
        Animal getChildren();
        Animal getParent();
    }

    // Dog: NO default constructor — only all-args @JsonCreator
    static class Dog implements Animal {
        private final String name;
        private final Animal children;
        private final Animal parent;

        @JsonCreator
        public Dog(
                @JsonProperty("name") String name,
                @JsonProperty("children") Animal children,
                @JsonProperty("parent") Animal parent) {
            this.name = name;
            this.children = children;
            this.parent = parent;
        }

        @Override public String getName() { return name; }
        @Override public Animal getChildren() { return children; }
        @Override public Animal getParent() { return parent; }
    }

    // Cat: HAS default constructor + setters (works fine)
    static class Cat implements Animal {
        private String name;
        private Animal children;
        private Animal parent;

        public Cat() { }

        @Override public String getName() { return name; }
        @Override public Animal getChildren() { return children; }
        @Override public Animal getParent() { return parent; }

        public void setName(String name) { this.name = name; }
        public void setChildren(Animal children) { this.children = children; }
        public void setParent(Animal parent) { this.parent = parent; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Baseline: Cat with default constructor + self-reference should work
    @Test
    public void testWithDefaultConstructorAndSelfReference() throws Exception
    {
        // Use pre-built JSON to avoid serialization format issues
        // Parent Cat (id=1) has child Cat (id=2) whose parent references back to id=1
        String json = a2q(
            "{'@type':'Cat','@id':1,'name':'ParentCat',"
            + "'children':{'@type':'Cat','@id':2,'name':'ChildCat','children':null,"
            + "'parent':1},"
            + "'parent':null}");

        Animal result = MAPPER.readValue(json, Animal.class);
        assertNotNull(result);
        assertInstanceOf(Cat.class, result);
        assertEquals("ParentCat", result.getName());
        assertNotNull(result.getChildren());
        assertEquals("ChildCat", result.getChildren().getName());
        assertSame(result, result.getChildren().getParent());
    }

    // [databind#4433]: Dog with NO default constructor + self-reference fails
    @Test
    public void testWithCreatorOnlyAndSelfReference() throws Exception
    {
        // Same structure as Cat test but with Dog (no default constructor)
        String json = a2q(
            "{'@type':'Dog','@id':1,'name':'ParentDog',"
            + "'children':{'@type':'Dog','@id':2,'name':'ChildDog','children':null,"
            + "'parent':1},"
            + "'parent':null}");

        // This should work but fails with UnresolvedForwardReference in 2.16
        Animal result = MAPPER.readValue(json, Animal.class);
        assertNotNull(result);
        assertInstanceOf(Dog.class, result);
        assertEquals("ParentDog", result.getName());
        assertNotNull(result.getChildren());
        assertEquals("ChildDog", result.getChildren().getName());
        // Key assertion: parent reference should resolve to same instance
        assertSame(result, result.getChildren().getParent());
    }

    // Simpler case: Dog without self-reference should work fine
    @Test
    public void testWithCreatorOnlyNoSelfReference() throws Exception
    {
        Dog dog = new Dog("SimpleDog", null, null);
        String json = MAPPER.writeValueAsString(dog);

        Animal result = MAPPER.readValue(json, Animal.class);
        assertNotNull(result);
        assertInstanceOf(Dog.class, result);
        assertEquals("SimpleDog", result.getName());
    }
}
