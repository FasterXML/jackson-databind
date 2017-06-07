package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * Test(s) for [Issue#113], problems with polymorphic types, JsonCreator.
 */
public class TestCreatorWithPolymorphic113 extends BaseMapTest
{
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "_class")
    @JsonSubTypes({  @JsonSubTypes.Type(Dog.class) })
    public static abstract class Animal {
        public final static String ID = "id";

        private String id;

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
        String json = mapper.writeValueAsString(new AnimalWrapper(new Dog(id)));
//System.err.println("JSON = "+json);
        AnimalWrapper wrapper = mapper.readValue(json, AnimalWrapper.class);
        assertEquals(id, wrapper.getAnimal().getId());
    }
}
