package tools.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class PolymorphicDeserErrorHandlingTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
            property = "clazz")
    abstract static class BaseForUnknownClass {
    }

    static class BaseUnknownWrapper {
        public BaseForUnknownClass value;
    }

    // [databind#2668]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Child1.class, name = "child1"),
        @JsonSubTypes.Type(value = Child2.class, name = "child2")
    })
    static class Parent2668 {
    }

    static class Child1 extends Parent2668 {
        public String bar;
    }

    static class Child2 extends Parent2668 {
        public String baz;
    }

    // [databind#5016]
    static abstract class Animal5016 {
        public String name = "animal";
    }

    static abstract class Plant {
        public String name = "plant";
    }

    static class Cat5016 extends Animal5016 {
        public String name = "cat";
    }

    static class Dog5016 extends Animal5016 implements Runnable {
        public String name = "dog";

        @Override
        public void run() { }
    }

    static class Tree extends Plant {
        public String name = "tree";
    }

    static class AnimalInfo {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
                include = JsonTypeInfo.As.PROPERTY,
                property = "@class")
        public Animal5016 thisType;
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

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testUnknownClassAsSubtype() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(BaseUnknownWrapper.class)
                .without(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        BaseUnknownWrapper w = reader.readValue(a2q
                ("{'value':{'clazz':'com.foobar.Nothing'}}"));
        assertNotNull(w);
    }

    // [databind#2668]
    @Test
    public void testSubType2668() throws Exception
    {
        String json = "{\"type\": \"child2\", \"baz\":\"1\"}"; // JSON for Child2

        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> MAPPER.readValue(json, Child1.class)); // Deserializing into Child1
        verifyException(e, "not subtype of");
    }

    // [databind#5016]
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

    // [databind#5016]: java.lang.Runnable not acceptable as safe base type
    @Test
    public void testBlockingOfRunnable() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .build();
        AnimalInfo animalInfo = new AnimalInfo();
        animalInfo.thisType = new Dog5016();
        String serialized = mapper.writeValueAsString(animalInfo);
        AnimalInfo newInfo0 = mapper.readValue(serialized, AnimalInfo.class);
        assertEquals(animalInfo.thisType.name, newInfo0.thisType.name);
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue(serialized, RunnableInfo.class));
        verifyException(e, "Configured `PolymorphicTypeValidator`");
        verifyException(e, "denies resolution of all subtypes of base type `java.lang.Runnable`");
    }
}
