package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;

public class JsonSubType2950Test extends BaseMapTest {
    
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
    */

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "_class"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Dog.class, name = "_dog"),
            @JsonSubTypes.Type(value = Cat.class, name = "_cat")
    })
    static abstract class Animal {
    }

    static class Cat extends Animal {
        public int age;

        public Cat(int age) {
            this.age = age;
        }
    }

    static class Dog extends Animal {
        public int age;
    }
    
    /*
    /**********************************************************
    /* Tests
    /**********************************************************
    */

    public void testDeserializeWithSingleParam() throws Exception {
        String json = a2q("{'_class': '_cat', 'age': 15} ");

        Cat cat = (Cat) newJsonMapper().readValue(json, Animal.class);

        assertEquals(15, cat.age);
    }

    public void testDeserializeWithoutSingleParam() throws Exception {
        String json = a2q("{'_class': '_dog', 'age':15} ");

        Dog dog = (Dog) newJsonMapper().readValue(json, Animal.class);
        assertEquals(15, dog.age);
    }
}
