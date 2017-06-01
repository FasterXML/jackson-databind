package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TestTypedSerializationMultiThreaded extends BaseMapTest {
    /*
     * /********************************************************** /* Helper types
     * /**********************************************************
     */

    /**
     * Polymorphic base class
     */
    @JsonTypeInfo(property = "type", use = Id.NAME)
    @JsonSubTypes({ @JsonSubTypes.Type(Dog.class), @JsonSubTypes.Type(Cat.class) })
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static abstract class Animal {
        public String name;

        protected Animal(String n)
        {
            name = n;
        }
    }

    static class Dog extends Animal {
        public int boneCount;

        private Dog()
        {
            super(null);
        }

        public Dog(String name, int b)
        {
            super(name);
            boneCount = b;
        }
    }

    static class Cat extends Animal {
        private String furColor;

        private Cat()
        {
            super(null);
        }

        public Cat(String name, String c)
        {
            super(name);
            furColor = c;
        }
    }

    public static class AnimalWrapper {
        public Animal animal;

        public AnimalWrapper(Animal a)
        {
            animal = a;
        }
    }

    /*
     * /********************************************************** /* Unit tests
     * /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    /**
     */
    public void testSimpleClassAsProperty() throws Exception
    {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<Cat>> points = new ArrayList<Future<Cat>>();
//        final ObjectWriter writer = MAPPER.writerFor(Cat.class);
        for (int b = 0; b < 1000000; b++) {
            final int i = b;
            points.add(executorService.submit(new Callable<Cat>() {
                @Override
                public Cat call() throws Exception
                {
                    Cat cat = new Cat("hello kitty " + i, "" + i);
                    AnimalWrapper catWrapper = new AnimalWrapper(cat);
                    final String s = MAPPER.writeValueAsString(catWrapper);
//                    final String s = writer.writeValueAsString(catWrapper);
                    if (!s.contains("\"type\":\"TestTypedSerializationMultiThreaded$Cat\"")) {
                        System.out.println("\n" + s);
                        fail(s);
                    }
                    return cat;
                }
            }));
        }

        for (Future<Cat> future : points) {
            future.get();
        }

        executorService.shutdown();
    }
}
