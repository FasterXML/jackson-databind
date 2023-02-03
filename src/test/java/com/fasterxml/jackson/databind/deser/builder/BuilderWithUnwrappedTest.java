package com.fasterxml.jackson.databind.deser.builder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public class BuilderWithUnwrappedTest extends BaseMapTest
{
    final static class Name {
        private final String first;
        private final String last;

        @JsonCreator
        Name(
                @JsonProperty("first_name") String first,
                @JsonProperty("last_name") String last
        ) {
            this.first = first;
            this.last = last;
        }

        String getFirst() {
            return first;
        }

        String getLast() {
            return last;
        }
    }

    @JsonDeserialize(builder = Person.Builder.class)
    final static class Person {
        private final long id;
        private final Name name;
        private final int age;
        private final boolean alive;

        Person(Builder builder) {
            id = builder.id;
            name = builder.name;
            age = builder.age;
            alive = builder.alive;
        }

        long getId() {
            return id;
        }

        Name getName() {
            return name;
        }

        int getAge() {
            return age;
        }

        boolean isAlive() {
            return alive;
        }

        @JsonPOJOBuilder(withPrefix = "set")
        final static class Builder {
            final long id;
            Name name;
            int age;
            boolean alive;

            Builder(@JsonProperty("person_id") long id) {
                this.id = id;
            }

            @JsonUnwrapped
            void setName(Name name) {
                this.name = name;
            }

            @JsonProperty("years_old")
            void setAge(int age) {
                this.age = age;
            }

            @JsonProperty("living")
            void setAlive(boolean alive) {
                this.alive = alive;
            }

            Person build() {
                return new Person(this);
            }
        }
    }

    @JsonDeserialize(builder = Animal.Builder.class)
    final static class Animal {
        private final long id;
        private final Name name;
        private final int age;
        private final boolean alive;

        Animal(Builder builder) {
            id = builder.id;
            name = builder.name;
            age = builder.age;
            alive = builder.alive;
        }

        long getId() {
            return id;
        }

        Name getName() {
            return name;
        }

        int getAge() {
            return age;
        }

        boolean isAlive() {
            return alive;
        }

        @JsonPOJOBuilder(withPrefix = "set")
        final static class Builder {
            final long id;
            Name name;
            int age;
            final boolean alive;

            Builder(
                    @JsonProperty("animal_id") long id,
                    @JsonProperty("living") boolean alive
            ) {
                this.id = id;
                this.alive = alive;
            }

            @JsonUnwrapped
            void setName(Name name) {
                this.name = name;
            }

            @JsonProperty("years_old")
            void setAge(int age) {
                this.age = age;
            }

            Animal build() {
                return new Animal(this);
            }
        }
    }

    @JsonDeserialize(builder = AnimalNoCreator.Builder.class)
    final static class AnimalNoCreator {
        private final long id;
        private final Name name;
        private final String age;

        AnimalNoCreator(Builder builder) {
            id = builder.id;
            name = builder.name;
            age = builder.age;
        }

        long getId() {
            return id;
        }

        Name getName() {
            return name;
        }

        String getAge() {
            return age;
        }

        @JsonPOJOBuilder(withPrefix = "set")
        final static class Builder {
            long id;
            Name name;
            String age;

            Builder() { }

            @JsonProperty("animal_id")
            public void setId(long i) {
                id = i;
            }

            @JsonUnwrapped
            void setName(Name name) {
                this.name = name;
            }

            @JsonProperty("years_old")
            void setAge(String age) {
                this.age = age;
            }

            AnimalNoCreator build() {
                return new AnimalNoCreator(this);
            }
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testWithUnwrappedAndCreatorSingleParameterAtBeginning() throws Exception {
        final String json = a2q("{'person_id':1234,'first_name':'John','last_name':'Doe','years_old':30,'living':true}");

        final ObjectMapper mapper = new ObjectMapper();
        Person person = mapper.readValue(json, Person.class);
        assertEquals(1234, person.getId());
        assertNotNull(person.getName());
        assertEquals("John", person.getName().getFirst());
        assertEquals("Doe", person.getName().getLast());
        assertEquals(30, person.getAge());
        assertEquals(true, person.isAlive());
    }

    public void testWithUnwrappedAndCreatorSingleParameterInMiddle() throws Exception {
        final String json = a2q("{'first_name':'John','last_name':'Doe','person_id':1234,'years_old':30,'living':true}");

        final ObjectMapper mapper = new ObjectMapper();
        Person person = mapper.readValue(json, Person.class);
        assertEquals(1234, person.getId());
        assertNotNull(person.getName());
        assertEquals("John", person.getName().getFirst());
        assertEquals("Doe", person.getName().getLast());
        assertEquals(30, person.getAge());
        assertEquals(true, person.isAlive());
    }

    public void testWithUnwrappedAndCreatorSingleParameterAtEnd() throws Exception {
        final String json = a2q("{'first_name':'John','last_name':'Doe','years_old':30,'living':true,'person_id':1234}");

        final ObjectMapper mapper = new ObjectMapper();
        Person person = mapper.readValue(json, Person.class);
        assertEquals(1234, person.getId());
        assertNotNull(person.getName());
        assertEquals("John", person.getName().getFirst());
        assertEquals("Doe", person.getName().getLast());
        assertEquals(30, person.getAge());
        assertEquals(true, person.isAlive());
    }

    public void testWithUnwrappedAndCreatorMultipleParametersAtBeginning() throws Exception {
        final String json = a2q("{'animal_id':1234,'living':true,'first_name':'John','last_name':'Doe','years_old':30}");

        final ObjectMapper mapper = new ObjectMapper();
        Animal animal = mapper.readValue(json, Animal.class);
        assertEquals(1234, animal.getId());
        assertNotNull(animal.getName());
        assertEquals("John", animal.getName().getFirst());
        assertEquals("Doe", animal.getName().getLast());
        assertEquals(30, animal.getAge());
        assertEquals(true, animal.isAlive());
    }

    public void testWithUnwrappedAndCreatorMultipleParametersInMiddle() throws Exception {
        final String json = a2q("{'first_name':'John','animal_id':1234,'last_name':'Doe','living':true,'years_old':30}");

        final ObjectMapper mapper = new ObjectMapper();
        Animal animal = mapper.readValue(json, Animal.class);
        assertEquals(1234, animal.getId());
        assertNotNull(animal.getName());
        assertEquals("John", animal.getName().getFirst());
        assertEquals("Doe", animal.getName().getLast());
        assertEquals(30, animal.getAge());
        assertEquals(true, animal.isAlive());
    }

    public void testWithUnwrappedAndCreatorMultipleParametersAtEnd() throws Exception {
        final String json = a2q("{'first_name':'John','last_name':'Doe','years_old':30,'living':true,'animal_id':1234}");

        final ObjectMapper mapper = new ObjectMapper();
        Animal animal = mapper.readValue(json, Animal.class);
        assertEquals(1234, animal.getId());
        assertNotNull(animal.getName());
        assertEquals("John", animal.getName().getFirst());
        assertEquals("Doe", animal.getName().getLast());
        assertEquals(30, animal.getAge());
        assertEquals(true, animal.isAlive());
    }

    public void testWithUnwrappedNoCreator() throws Exception {
        final String json = a2q("{'first_name':'John','last_name':'Doe','years_old':'30','animal_id':1234}");

        final ObjectMapper mapper = new ObjectMapper();
        AnimalNoCreator animal = mapper.readValue(json, AnimalNoCreator.class);
        assertEquals(1234, animal.getId());
        assertNotNull(animal.getName());
        assertEquals("John", animal.getName().getFirst());
        assertEquals("Doe", animal.getName().getLast());
        assertEquals("30", animal.getAge());
    }
}
