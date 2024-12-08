package com.fasterxml.jackson.databind;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [modules-java8#86] Cannot read `Optional`s written with `StdTypeResolverBuilder`
//
public class OptionalWithTypeResolver86Test
    extends DatabindTestUtil
{

    public static class Foo<T> {
        public Optional<T> value;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Foo<?> foo = (Foo<?>) o;
            return Objects.equals(value, foo.value);
        }
    }

    public static class Pojo86 {
        public String name;

        // with static method
        public static Pojo86 valueOf(String name) {
            Pojo86 pojo = new Pojo86();
            pojo.name = name;
            return pojo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pojo86 pojo86 = (Pojo86) o;
            return Objects.equals(name, pojo86.name);
        }

    }

    // Base class for polymorphic types
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    public static abstract class Animal {
        public String name;

        protected Animal(String name) {
            this.name = name;
        }
    }

    // Subclass: Dog
    public static class Dog extends Animal {
        @JsonCreator
        public Dog(@JsonProperty("name") String name) {
            super(name);
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Dog) && name.equals(((Dog) obj).name);
        }
    }

    @Test
    public void testRoundTrip()
            throws Exception
    {
        _testOptionalWith(Optional.of("MyName"), String.class, "MyName");
        _testOptionalWith(Optional.of(42), Integer.class, 42);
        _testOptionalWith(Optional.of(Pojo86.valueOf("PojoName")),
                Pojo86.class, Pojo86.valueOf("PojoName"));
    }

    @Test
    public void testRoundTripPolymorphic()
            throws Exception
    {
        _testOptionalPolymorphicWith(
                Optional.of(new Dog("Buddy"))
        );
    }

    private <T> void _testOptionalPolymorphicWith(Optional<T> value)
            throws Exception
    {
        ObjectMapper mapper = configureObjectMapper();

        // Serialize
        Foo<T> foo = new Foo<>();
        foo.value = value;
        String json = mapper.writeValueAsString(foo);

        // Deserialize
        Foo<T> bean = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(Foo.class, Animal.class));
        assertEquals(foo, bean); // Compare Foo objects directly
    }

    private <T> void _testOptionalWith(Optional<T> value, Class<T> type, T expectedValue)
            throws Exception
    {
        ObjectMapper mapper = configureObjectMapper();

        // Serialize
        Foo<T> foo = new Foo<>();
        foo.value = value;
        String json = mapper.writeValueAsString(foo);

        // Deserialize
        Foo<T> bean = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(Foo.class, type));
        assertEquals(value, bean.value);
        assertEquals(expectedValue, bean.value.get());
    }

    private ObjectMapper configureObjectMapper(){
        ObjectMapper mapper = jsonMapperBuilder().addModule(new Jdk8Module()).build();
        mapper.setDefaultTyping(
                new StdTypeResolverBuilder()
                        .init(JsonTypeInfo.Id.CLASS, null)
                        .inclusion(JsonTypeInfo.As.WRAPPER_OBJECT)
        );
        return mapper;
    }

}