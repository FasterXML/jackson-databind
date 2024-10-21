package com.fasterxml.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for verifying that it is possible to annotate
 * various kinds of things with {@link JsonCreator} annotation.
 */
class PolymorphicPropsCreatorsTest extends DatabindTestUtil
{
    static class Animal
    {
        // All animals have names, for our demo purposes...
        public String name;

        protected Animal() { }

        /**
         * Creator method that can instantiate instances of
         * appropriate polymoprphic type
         */
        @JsonCreator
        public static Animal create(@JsonProperty("type") String type)
        {
            if ("dog".equals(type)) {
                return new Dog();
            }
            if ("cat".equals(type)) {
                return new Cat();
            }
            throw new IllegalArgumentException("No such animal type ('"+type+"')");
        }
    }

    static class Dog extends Animal
    {
        double barkVolume; // in decibels
        public Dog() { }
        public void setBarkVolume(double v) { barkVolume = v; }
    }

    static class Cat extends Animal
    {
        boolean likesCream;
        public int lives;
        public Cat() { }
        public void setLikesCream(boolean likesCreamSurely) { likesCream = likesCreamSurely; }
    }

    abstract static class AbstractRoot
    {
        protected final String opt;

        protected AbstractRoot(String opt) {
            this.opt = opt;
        }

        @JsonCreator
        public static final AbstractRoot make(@JsonProperty("which") int which,
            @JsonProperty("opt") String opt) {
            if (1 == which) {
                return new One(opt);
            }
            throw new RuntimeException("cannot instantiate " + which);
        }

        abstract public int getWhich();

        public final String getOpt() {
            return opt;
        }
    }

    static final class One extends AbstractRoot {
        protected One(String opt) {
            super(opt);
        }

        @Override public int getWhich() {
            return 1;
        }
    }

    // [databind#113]
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "_class")
    @JsonSubTypes({  @JsonSubTypes.Type(Dog113.class) })
    public static abstract class Animal113 {
        public final static String ID = "id";

        private String id;

        @JsonCreator
        public Animal113(@JsonProperty(ID) String id) {
            this.id = id;
        }

        @JsonProperty(ID)
        public String getId() {
            return id;
        }
    }

    public static class Dog113 extends Animal113 {
        @JsonCreator
        public Dog113(@JsonProperty(ID) String id) {
            super(id);
        }
    }

    public static class AnimalWrapper113 {
        private Animal113 animal;

        @JsonCreator
        public AnimalWrapper113(@JsonProperty("animal") Animal113 animal) {
            this.animal = animal;
        }

        public Animal113 getAnimal() {
            return animal;
        }
    }

    /*
    /**********************************************************
    /* Actual tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * Simple test to verify that it is possible to implement polymorphic
     * deserialization manually.
     */
    @Test
    void manualPolymorphicDog() throws Exception
    {
        // first, a dog, start with type
        Animal animal = MAPPER.readValue("{ \"type\":\"dog\", \"name\":\"Fido\", \"barkVolume\" : 95.0 }", Animal.class);
        assertEquals(Dog.class, animal.getClass());
        assertEquals("Fido", animal.name);
        assertEquals(95.0, ((Dog) animal).barkVolume);
    }

    @Test
    void manualPolymorphicCatBasic() throws Exception
    {
        // and finally, lactose-intolerant, but otherwise robust super-cat:
        Animal animal = MAPPER.readValue("{ \"name\" : \"Macavity\", \"type\":\"cat\", \"lives\":18, \"likesCream\":false }", Animal.class);
        assertEquals(Cat.class, animal.getClass());
        assertEquals("Macavity", animal.name); // ... there's no one like Macavity!
        Cat cat = (Cat) animal;
        assertEquals(18, cat.lives);
        // ok, he can't drink dairy products. Let's verify:
        assertEquals(false, cat.likesCream);
    }

    @Test
    void manualPolymorphicCatWithReorder() throws Exception
    {
        // Then cat; shuffle order to mandate buffering
        Animal animal = MAPPER.readValue("{ \"likesCream\":true, \"name\" : \"Venla\", \"type\":\"cat\" }", Animal.class);
        assertEquals(Cat.class, animal.getClass());
        assertEquals("Venla", animal.name);
        // bah, of course cats like cream. But let's ensure Jackson won't mess with laws of nature!
        assertTrue(((Cat) animal).likesCream);
    }

    @Test
    void manualPolymorphicWithNumbered() throws Exception
    {
         final ObjectWriter w = MAPPER.writerFor(AbstractRoot.class);
         final ObjectReader r = MAPPER.readerFor(AbstractRoot.class);

         AbstractRoot input = AbstractRoot.make(1, "oh hai!");
         String json = w.writeValueAsString(input);
         AbstractRoot result = r.readValue(json);
         assertNotNull(result);
         assertEquals("oh hai!", result.getOpt());
    }

    // [databind#113]
    @Test
    void subtypes113() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String id = "nice dogy";
        String json = mapper.writeValueAsString(new AnimalWrapper113(new Dog113(id)));
//System.err.println("JSON = "+json);
        AnimalWrapper113 wrapper = mapper.readValue(json, AnimalWrapper113.class);
        assertEquals(id, wrapper.getAnimal().getId());
    }    
}
