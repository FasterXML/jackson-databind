package com.fasterxml.jackson.failing;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// [databind#3964] MismatchedInputException, Bean not yet resolved
class JsonIdentityInfoAndBackReferences3964Test extends DatabindTestUtil {
    /**
     * Fails : Original test
     */
    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id",
            scope = Tree.class
    )
    public static class Tree {
        protected final int id;
        protected List<Fruit> fruits;

        @JsonCreator
        public Tree(@JsonProperty("id") int id, @JsonProperty("fruits") List<Fruit> fruits) {
            this.id = id;
            this.fruits = fruits;
        }

        public int getId() {
            return id;
        }

        public List<Fruit> getFruits() {
            return fruits;
        }

        public void setFruits(List<Fruit> fruits) {
            this.fruits = fruits;
        }
    }

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id",
            scope = Fruit.class
    )
    public static class Fruit {
        protected final int id;
        protected List<Calories> calories;

        @JsonBackReference("id")
        protected Tree tree;

        @JsonCreator
        public Fruit(@JsonProperty("id") int id, @JsonProperty("calories") List<Calories> calories) {
            this.id = id;
            this.calories = calories;
        }

        public int getId() {
            return id;
        }

        public Tree getTree() {
            return tree;
        }

        public void setTree(Tree tree) {
            this.tree = tree;
        }

        public List<Calories> getCalories() {
            return calories;
        }

        public void setCalories(List<Calories> calories) {
            this.calories = calories;
        }
    }

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id",
            scope = Calories.class
    )
    public static class Calories {
        protected final int id;
        protected Fruit fruit;

        @JsonCreator
        public Calories(@JsonProperty("id") int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public Fruit getFruit() {
            return fruit;
        }

        public void setFruit(Fruit fruit) {
            this.fruit = fruit;
        }
    }

    /**
     * Fails : Lean version that fails and Without getters and setters
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
     * Passes : Testing lean without getters and setters
     * and also without {@link JsonCreator}.
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

    final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    /**
     * Fails : Original test
     */
    @Test
    void original() throws Exception {
        String json = "{" +
                "              \"id\": 1,\n" +
                "              \"fruits\": [\n" +
                "                {\n" +
                "                  \"id\": 2,\n" +
                "                  \"tree\": 1,\n" +
                "                  \"calories\": [\n" +
                "                    {\n" +
                "                      \"id\": 3,\n" +
                "                      \"fruit\": 2\n" +
                "                    }\n" +
                "                  ]\n" +
                "                }\n" +
                "              ]\n" +
                "            }";

        try {
            Tree tree = MAPPER.readValue(json, Tree.class);
            // should reach here and pass... but throws Exception and fails
            assertEquals(tree, tree.fruits.get(0).tree);
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot resolve ObjectId forward reference using property 'animal'");
            verifyException(e, "Bean not yet resolved");
            fail("Should not reach");
        }
    }

    /**
     * Fails : Lean version that fails and Without getters and setters
     */
    @Test
    void leanWithoutGetterAndSetters() throws Exception {
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

        try {
            Animal animal = MAPPER.readValue(json, Animal.class);
            // should reach here and pass... but throws Exception and fails
            assertEquals(animal, animal.cats.get(0).animal);
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot resolve ObjectId forward reference using property 'animal'");
            verifyException(e, "Bean not yet resolved");
            fail("Should not reach");
        }
    }

    /**
     * Passes : Testing lean without getters and setters
     * and also without {@link JsonCreator}.
     */
    @Test
    void leanWithoutGetterAndSettersAndCreator() throws Exception {
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
