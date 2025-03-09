package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// https://github.com/FasterXML/jackson-databind/issues/5016
public class TestPolymorphicDeserialization5016 extends DatabindTestUtil
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
    public void testWrongSubtype() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        PlantInfo plantInfo = new PlantInfo();
        plantInfo.thisType = new Tree();
        String serialized = mapper.writeValueAsString(plantInfo);
        PlantInfo newInfo0 = mapper.readValue(serialized, PlantInfo.class);
        assertEquals(plantInfo.thisType.name, newInfo0.thisType.name);
        AnimalInfo newInfo1 = mapper.readValue(serialized, AnimalInfo.class);
        assertEquals(plantInfo.thisType.name, newInfo1.thisType.name);
    }

    @Test
    public void testRunnable() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        AnimalInfo animalInfo = new AnimalInfo();
        animalInfo.thisType = new Dog();
        String serialized = mapper.writeValueAsString(animalInfo);
        AnimalInfo newInfo0 = mapper.readValue(serialized, AnimalInfo.class);
        assertEquals(animalInfo.thisType.name, newInfo0.thisType.name);
        RunnableInfo newInfo1 = mapper.readValue(serialized, RunnableInfo.class);
        assertNotNull(newInfo1);
    }
}
