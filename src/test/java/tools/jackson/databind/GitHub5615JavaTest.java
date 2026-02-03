package tools.jackson.databind;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/*
 * Execute with: mvnw test -Dtest=tools.jackson.databind.GitHub5615JavaTest -f pom.xml
 */
@Execution(ExecutionMode.CONCURRENT)
public class GitHub5615JavaTest {

    private static JsonMapper testJsonMapper;

    @BeforeAll
    static void setup() {
        testJsonMapper = new JsonMapper();
    }

    private final String roomsString = """
            {
                "rooms": [
                    {
                      "typ": "Living",
                      "animals": [
                            {
                                "typ": "Cat"
                            },
                            {
                                "typ": "Cat"
                            },
                            {
                                "typ": "Cat"
                            },
                            {
                                "typ": "Cat"
                            }
                      ]
                    },
                    {
                      "typ": "Sleeping",
                      "animals": [
                            {
                                "typ": "Dog"
                            },
                            {
                                "typ": "Dog"
                            },
                            {
                                "typ": "Dog"
                            },
                            {
                                "typ": "Dog"
                            }
                      ]
                    }
                ]
            }
            """;

    private final Result result = testJsonMapper.readValue(roomsString, Result.class);

    @RepeatedTest(2000)
    void trySerializationAndDeserialization() {
        serializeWithDeserialization(result);
    }

    private void serializeWithDeserialization(Result result) {
        String personalbetriebslageJson = testJsonMapper.writeValueAsString(result);
        testJsonMapper.readValue(personalbetriebslageJson, Result.class);
    }

    // Data class definitions

    public record Result(@JsonProperty("rooms") List<Room> rooms) {
    }

    @JsonIgnoreProperties(value = {"typ"}, allowSetters = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "typ", visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = LivingRoom.class, name = "Living"),
            @JsonSubTypes.Type(value = SleepingRoom.class, name = "Sleeping")
    })
    public interface Room {
        @JsonProperty("typ")
        RoomType getTyp();
    }

    public record LivingRoom(@JsonProperty("typ") RoomType typ,
                             @JsonProperty("animals") List<Cat> animals) implements Room {
        @Override
        public RoomType getTyp() {
            return typ;
        }
    }

    public record SleepingRoom(@JsonProperty("typ") RoomType typ,
                               @JsonProperty("animals") List<Dog> animals) implements Room {
        @Override
        public RoomType getTyp() {
            return typ;
        }
    }

    public enum RoomType {
        Living,
        Sleeping
    }

    @JsonIgnoreProperties(value = {"typ"}, allowSetters = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "typ", visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Cat.class, name = "Cat"),
            @JsonSubTypes.Type(value = Dog.class, name = "Dog")
    })
    public interface Animal {
        @JsonProperty("typ")
        AnimalType getTyp();
    }

    public record Cat(@JsonProperty("typ") AnimalType typ) implements Animal {
        @Override
        public AnimalType getTyp() {
            return typ;
        }
    }

    public record Dog(@JsonProperty("typ") AnimalType typ) implements Animal {
        @Override
        public AnimalType getTyp() {
            return typ;
        }
    }

    public enum AnimalType {
        Dog,
        Cat
    }
}
