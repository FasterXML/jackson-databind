package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestIssueGH113 extends BaseMapTest
{
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "_class")
    @JsonSubTypes({  @JsonSubTypes.Type(Dog.class) })
    public static abstract class Animal {
        public final static String ID = "id";

        private String id;
        public Animal() {
        }

        @JsonCreator
        public Animal(@JsonProperty(ID) String id) {
            this.id = id;
        }

        @JsonProperty(ID)
        public String getId() {
            return id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dog extends Animal {
        public Dog() {
            super();
        }

        @JsonCreator
        public Dog(@JsonProperty(ID) String id) {
            super(id);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnimalWrapper {
        private Animal animal;

        @JsonCreator
        public AnimalWrapper(@JsonProperty("animal") Animal animal) {
            this.animal = animal;
        }

        public Animal getAnimal() {
            return animal;
        }
    }

    public void testSubtypes() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        String id = "nice dogy";
        String serializedDog = mapper.writeValueAsString(new AnimalWrapper(new Dog(id)));
        AnimalWrapper wrapper = mapper.readValue(serializedDog, AnimalWrapper.class);
        assertEquals(id, wrapper.getAnimal().getId());
    }
}
