package com.fasterxml.jackson.databind.jsontype.ext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

public class ExternalTypeIdWithEnum1328Test extends BaseMapTest
{
    public interface Animal { }

    public static class Dog implements Animal {
        public String dogStuff;
    }

    public enum AnimalType {
        Dog;
    }

    public static class AnimalAndType {
        public AnimalType type;

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "type")
        @JsonTypeIdResolver(AnimalResolver.class)
        private Animal animal;

        public AnimalAndType() { }

        // problem is this annotation
        @java.beans.ConstructorProperties({"type", "animal"})
        public AnimalAndType(final AnimalType type, final Animal animal) {
            this.type = type;
            this.animal = animal;
        }
    }

    static class AnimalResolver implements TypeIdResolver {
        @Override
        public void init(JavaType bt) { }

        @Override
        public String idFromValue(Object value) {
            return null;
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {
            return null;
        }

        @Override
        public String idFromBaseType() {
            throw new UnsupportedOperationException("Missing action type information - Can not construct");
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) throws IOException {
            if (AnimalType.Dog.toString().equals(id)) {
                return context.constructType(Dog.class);
            }
            throw new IllegalArgumentException("What is a " + id);
        }

        @Override
        public String getDescForKnownTypeIds() {
            return null;
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            return JsonTypeInfo.Id.CUSTOM;
        }
    }

    public void testExample() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(Arrays.asList(new AnimalAndType(AnimalType.Dog, new Dog())));
        List<AnimalAndType> list = mapper.readerFor(new TypeReference<List<AnimalAndType>>() { })
            .readValue(json);
        assertNotNull(list);
    }
}
