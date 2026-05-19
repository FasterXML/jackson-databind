package tools.jackson.databind.objectid;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#3964] MismatchedInputException, Bean not yet resolved
public class JsonIdentityInfoAndBackReferences3964Test extends DatabindTestUtil
{
    /**
     * Fails : Original test
     */
    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id",
            scope = Tree.class
    )
    static class Tree {
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
    static class Fruit {
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
    static class Calories {
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
    static class Animal {
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
    static class Cat {
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
    static class Food {
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
    static class Fish {
        public int id;
        public List<Squid> squids;
    }

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id",
            scope = Squid.class
    )
    static class Squid {
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
    static class Shrimp {
        public int id;
        public Squid squid;
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    /**
     * Original test: used to fail
     */
    @Test
    public void testOriginalBackReference() throws Exception {
        String json = """
                {\
                              "id": 1,
                              "fruits": [
                                {
                                  "id": 2,
                                  "tree": 1,
                                  "calories": [
                                    {
                                      "id": 3,
                                      "fruit": 2
                                    }
                                  ]
                                }
                              ]
                            }
                """;

        Tree tree = MAPPER.readValue(json, Tree.class);
        // should reach here and pass... but throws Exception and fails
        assertEquals(tree, tree.fruits.get(0).tree);
    }

    /**
     * Lean version that used to fail, and Without getters and setters
     */
    @Test
    public void testLeanWithoutGetterAndSetters() throws Exception {
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

        Animal animal = MAPPER.readValue(json, Animal.class);
        // should reach here and pass... but throws Exception and fails
        assertEquals(animal, animal.cats.get(0).animal);
    }

    /**
     * Passes : Testing lean without getters and setters
     * and also without {@link JsonCreator}.
     */
    @Test
    public void testLeanWithoutGetterSettersOrCreator() throws Exception {
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
