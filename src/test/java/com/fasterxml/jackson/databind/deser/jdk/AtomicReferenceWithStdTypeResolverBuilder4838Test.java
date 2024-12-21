package com.fasterxml.jackson.databind.deser.jdk;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Reported via [modules-java8#86] Cannot read `Optional`s written with `StdTypeResolverBuilder`
public class AtomicReferenceWithStdTypeResolverBuilder4838Test
    extends DatabindTestUtil
{
    static class Foo<T> {
        public AtomicReference<T> value;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Foo<?> foo = (Foo<?>) o;
            return Objects.equals(value.get(), foo.value.get());
        }
    }

    static class Pojo86 {
        public String name;

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

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    static abstract class Animal {
        public String name;

        protected Animal(String name) {
            this.name = name;
        }
    }

    static class Dog extends Animal {
        @JsonCreator
        public Dog(@JsonProperty("name") String name) {
            super(name);
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Dog) && name.equals(((Dog) obj).name);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
    static abstract class Animal2 {
        public String name;

        protected Animal2(String name) {
            this.name = name;
        }
    }

    static class Dog2 extends Animal2 {
        @JsonCreator
        public Dog2(@JsonProperty("name") String name) {
            super(name);
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Dog2) && name.equals(((Dog2) obj).name);
        }
    }

    @Test
    public void testRoundTrip() throws Exception {
        _test(new AtomicReference<>("MyName"), String.class);
        _test(new AtomicReference<>(42), Integer.class);
        _test(new AtomicReference<>(Pojo86.valueOf("PojoName")), Pojo86.class);
    }

    @Test
    public void testPolymorphic() throws Exception {
        _test(new AtomicReference<>(new Dog("Buddy")), Animal.class);
        _test(new AtomicReference<>(new Dog2("Buttercup")), Animal2.class);
    }

    private final ObjectMapper STD_RESOLVER_MAPPER = jsonMapperBuilder()
            // this is what's causing failure in later versions.....
            .setDefaultTyping(
                new StdTypeResolverBuilder()
                        .init(JsonTypeInfo.Id.CLASS, null)
                        .inclusion(JsonTypeInfo.As.WRAPPER_OBJECT)
            ).build();

    private <T> void _test(AtomicReference<T> value, Class<?> type) throws Exception {
        // Serialize
        Foo<T> foo = new Foo<>();
        foo.value = value;
        String json = STD_RESOLVER_MAPPER.writeValueAsString(foo);

        // Deserialize
        Foo<T> bean = STD_RESOLVER_MAPPER.readValue(json,
                STD_RESOLVER_MAPPER.getTypeFactory().constructParametricType(Foo.class, type));

        // Compare the underlying values of AtomicReference
        assertEquals(foo.value.get(), bean.value.get());
    }
}
