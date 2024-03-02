package com.fasterxml.jackson.databind.jsontype.ext;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#2588] / [databind#2610] / [databind#4354]
public class ExternalTypeId2588Test extends DatabindTestUtil
{
    // [databind#2588]
    interface Animal { }

    static class Cat implements Animal {
        public int lives = 9;
    }

    public static class Dog implements Animal { }

    static class Wolf implements Animal {
        public boolean alive;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Pet {
        final String type;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "type")
        @JsonTypeIdResolver(AnimalTypeIdResolver.class)
        private final Animal animal;

        @JsonCreator
        public Pet(@JsonProperty("type") String type,
                   @JsonProperty("animal") Animal animal) {
            this.type = type;
            this.animal = animal;
        }
    }

    static class AnimalTypeIdResolver extends TypeIdResolverBase {
        @Override
        public String idFromValue(Object value) {
            return idFromValueAndType(value, value.getClass());
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {
            if (suggestedType.isAssignableFrom(Cat.class)) {
                return "cat";
            } else if (suggestedType.isAssignableFrom(Dog.class)) {
                return "dog";
            } else if (suggestedType.isAssignableFrom(Wolf.class)) {
                return "wolf";
            }
            return null;
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) {
            if ("cat".equals(id)) {
                return context.constructType(Cat.class);
            } else if ("dog".equals(id)) {
                return context.constructType(Dog.class);
            }
            return null;
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            return JsonTypeInfo.Id.NAME;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2588]
    @Test
    public void testExternalTypeId2588Read() throws Exception
    {
        Pet pet;

        // works?

        pet = MAPPER.readValue(a2q(
"{\n" +
"  'type': 'cat',\n" +
"  'animal': { },\n" +
"  'ignoredObject\": {\n" +
"    'someField': 'someValue'\n" +
"  }"+
"}"
                ), Pet.class);
        assertNotNull(pet);

        // fails:
        pet = MAPPER.readValue(a2q(
"{\n" +
"  'animal\": { },\n" +
"  'ignoredObject': {\n" +
"    'someField': 'someValue'\n" +
"  },\n" +
"  'type': 'cat'\n" +
"}"
                ), Pet.class);
        assertNotNull(pet);
    }

    @Test
    public void testExternalTypeId2588Write() throws Exception
    {
        String json = MAPPER.writeValueAsString(new Pet("cat", new Wolf()));
        assertEquals(a2q("{'animal':{'alive':false},'type':'wolf'}"), json);
    }
}
