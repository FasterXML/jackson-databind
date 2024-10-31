package com.fasterxml.jackson.databind.tofix;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// [databind#4742] Deserialization with Builder, External type id,
//                @JsonCreator not yet implemented
public class JacksonBuilderCreatorSubtype4742Test
        extends DatabindTestUtil
{

    public static class Animals {
        @JsonProperty("animals")
        public List<Animal> animals;
    }

    @JsonDeserialize(builder = Animal.Builder.class)
    public static class Animal {
        @JsonProperty("kind")
        public String kind;

        @JsonProperty("properties")
        public AnimalProperties properties;

        @Override
        public String toString() {
            return "Animal{kind='" + kind + '\'' + ", properties=" + properties + '}';
        }

        public static abstract class Builder {
            @JsonProperty("kind")
            public abstract Builder kind(String kind);

            @JsonProperty("properties")
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "kind")
            @JsonSubTypes({
                    @JsonSubTypes.Type(name = "bird", value = BirdProperties.class),
                    @JsonSubTypes.Type(name = "mammal", value = MammalProperties.class)
            })
            public abstract Builder properties(AnimalProperties properties);

            @JsonCreator
            public static BuilderImpl create() {
                return new BuilderImpl();
            }

            public abstract Animal build();
        }

        public static class BuilderImpl extends Builder {
            private String kind;
            private AnimalProperties properties;

            public BuilderImpl kind(String kind) {
                this.kind = kind;
                return this;
            }

            public BuilderImpl properties(AnimalProperties properties) {
                this.properties = properties;
                return this;
            }

            @Override
            public Animal build() {
                final Animal animal = new Animal();
                animal.kind = kind;
                animal.properties = properties;
                return animal;
            }
        }
    }

    public interface AnimalProperties {
    }

    public static class MammalProperties implements AnimalProperties {
        @JsonProperty("num_teeth")
        public int teeth;

        @Override
        public String toString() {
            return "MammalProperties{teeth=" + teeth + '}';
        }
    }

    public static class BirdProperties implements AnimalProperties {
        @JsonProperty("color")
        public String color;

        @Override
        public String toString() {
            return "BirdProperties{color='" + color + '\'' + '}';
        }
    }

    final ObjectMapper MAPPER = newJsonMapper();

    @JacksonTestFailureExpected
    @Test
    public void testDeser()
            throws Exception
    {
        final Animals animals = MAPPER.readValue(
                "{\n" +
                        "  \"animals\": [\n" +
                        "    {\"kind\": \"bird\", \"properties\": {\"color\": \"yellow\"}},\n" +
                        "    {\"kind\": \"mammal\", \"properties\": {\"num_teeth\": 2}}\n" +
                        "  ]\n" +
                        "}", Animals.class);

        assertEquals(2, animals.animals.size());
        assertInstanceOf(BirdProperties.class, animals.animals.get(0).properties);
        assertInstanceOf(MammalProperties.class, animals.animals.get(1).properties);

    }
}
