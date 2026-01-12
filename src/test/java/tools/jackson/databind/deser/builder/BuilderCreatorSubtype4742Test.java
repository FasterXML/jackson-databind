package tools.jackson.databind.deser.builder;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// [databind#4742] Deserialization with Builder, External type id,
// @JsonCreator failing
public class BuilderCreatorSubtype4742Test
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

            @Override
            public BuilderImpl kind(String kind) {
                this.kind = kind;
                return this;
            }

            @Override
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

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testDeser4742() throws Exception
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
