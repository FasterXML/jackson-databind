package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.util.List;

// [databind#3964] MismatchedInputException, Bean not yet resolved
public class JsonIdentityInfoAndBackReferences3964Test extends BaseMapTest 
{
    /**
     * Combination of one in question
     * Testing lean without getters and setters only
     */
    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id",
            scope = Animal.class
    )
    public static class Animal {
        public final int id;
        public List<Cat> cats;

        @JsonCreator
        public Animal(@JsonProperty("id") int id, @JsonProperty("cats") List<Cat> cats) {
            this.id = id;
            this.cats = cats;
        }
    }

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id",
            scope = Cat.class
    )
    public static class Cat {
        public int id;
        public List<Food> foods;
        @JsonBackReference("id")
        public Animal animal;

        @JsonCreator
        public Cat(@JsonProperty("id") int id, @JsonProperty("foods") List<Food> foods) {
            this.id = id;
            this.foods = foods;
        }
    }

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id",
            scope = Food.class
    )
    public static class Food {
        public int id;
        public Cat cat;

        @JsonCreator
        public Food(@JsonProperty("id") int id) {
            this.id = id;
        }
    }

    /**
     * Testing lean without getters and setters
     * and also without {@link JsonCreator}
     */
    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id",
            scope = Fish.class
    )
    public static class Fish {
        public int id;
        public List<Squid> squids;
    }

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id",
            scope = Squid.class
    )
    public static class Squid {
        public int id;
        public List<Shrimp> shrimps;
        @JsonBackReference("id")
        public Fish fish;
    }

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id",
            scope = Shrimp.class
    )
    public static class Shrimp {
        public int id;
        public Squid squid;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
    */

    final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    /**
     * This one in question by [databind#3964]
     * Testing lean without getters and setters only
     */
    public void testOnlyLean() throws JsonProcessingException {
        String json = a2q("{" +
                "              'id': 1," +
                "              'cats': [" +
                "                {" +
                "                  'id': 2," +
                "                  'animal': 1," + // reference here
                "                  'foods': [" +
                "                    {" +
                "                      'id': 3," +
                "                      'cat': 2" +
                "                    }" +
                "                  ]" +
                "                }" +
                "              ]" +
                "            }");
        /*
         * Below is what everyone expects (from the issue)
         *********************************** 
        Animal animal = MAPPER.readValue(json, Animal.class);
        assertEquals(animal, animal.cats.get(0).animal);
         *********************************** 
         */

        /*
         * In reality... fails with MismatchedInputException like below
         */
        try {
            MAPPER.readValue(json, Animal.class);
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot resolve ObjectId forward reference using property 'animal'");
            verifyException(e, "Bean not yet resolved");
            fail("Should not reach");
        }
    }

    /**
     * Testing lean without getters and setters
     * and also without {@link JsonCreator}
     */
    public void testLeanAndWithoutCreator() throws Exception {
        String json = a2q("{" +
                "              'id': 1," +
                "              'squids': [" +
                "                {" +
                "                  'id': 2," +
                "                  'fish': 1," + // back reference
                "                  'shrimps': [" +
                "                    {" +
                "                      'id': 3," +
                "                      'squid': 2" +
                "                    }" +
                "                  ]" +
                "                }" +
                "              ]" +
                "            }");

        Fish fish = MAPPER.readValue(json, Fish.class);
        assertEquals(fish, fish.squids.get(0).fish);
    }
}
