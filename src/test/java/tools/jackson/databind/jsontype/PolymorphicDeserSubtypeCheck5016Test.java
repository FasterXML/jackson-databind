package tools.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

// https://github.com/FasterXML/jackson-databind/issues/5016
public class PolymorphicDeserSubtypeCheck5016Test extends DatabindTestUtil
{
    static abstract class Animal {
        public String name = "animal";
    }

    static abstract class Plant {
        public String name = "plant";
    }

    static class Cat extends Animal {
        public String name = "cat";
    }

    static class Dog extends Animal implements Runnable {
        public String name = "dog";

        @Override
        public void run() {
            System.out.println("Dog is running");
        }
    }

    static class Tree extends Plant {
        public String name = "tree";
    }

    static class AnimalInfo {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
                include = JsonTypeInfo.As.PROPERTY,
                property = "@class")
        public Animal thisType;
    }

    static class PlantInfo {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
                include = JsonTypeInfo.As.PROPERTY,
                property = "@class")
        public Plant thisType;
    }

    static class RunnableInfo {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
                include = JsonTypeInfo.As.PROPERTY,
                property = "@class")
        public Runnable thisType;
    }

    @Test
    public void testWrongSubtype() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        PlantInfo plantInfo = new PlantInfo();
        plantInfo.thisType = new Tree();
        String serialized = mapper.writeValueAsString(plantInfo);
        PlantInfo newInfo0 = mapper.readValue(serialized, PlantInfo.class);
        assertEquals(plantInfo.thisType.name, newInfo0.thisType.name);
        // AnimalInfo has same JSON structure but incompatible type for `thisType`
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class, () ->
                mapper.readValue(serialized, AnimalInfo.class));
        verifyException(e, "Could not resolve type id ");
        verifyException(e, "Not a subtype");
    }

    // java.lang.Runnable not acceptable as safe base type
    @Test
    public void testBlockingOfRunnable() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                // Default behavior in Jackson 3.x, not feature-enabled
                //.enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .build();
        AnimalInfo animalInfo = new AnimalInfo();
        animalInfo.thisType = new Dog();
        String serialized = mapper.writeValueAsString(animalInfo);
        AnimalInfo newInfo0 = mapper.readValue(serialized, AnimalInfo.class);
        assertEquals(animalInfo.thisType.name, newInfo0.thisType.name);
        try {
            mapper.readValue(serialized, RunnableInfo.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Configured `PolymorphicTypeValidator`");
            verifyException(e, "denies resolution of all subtypes of base type `java.lang.Runnable`");
        }
    }
}
