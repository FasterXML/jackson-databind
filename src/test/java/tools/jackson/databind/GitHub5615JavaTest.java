package tools.jackson.databind;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @RepeatedTest(1000)
    void trySerializationAndDeserialization() throws Exception {
        int threadCount = 10;
        var barrier = new java.util.concurrent.CyclicBarrier(threadCount);
        var threads = new java.util.ArrayList<Thread>();
        var errors = new java.util.concurrent.CopyOnWriteArrayList<Throwable>();
        
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                try {
                    barrier.await();
                    for (int j = 0; j < 100; j++) {
                        serializeWithDeserialization(result);
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertTrue(
                errors.isEmpty(),
                () -> format(
                        "test failed with %s",
                        errors.stream()
                                .map(Throwable::toString)
                                .collect(Collectors.joining("\n"))
                )
        );
    }

    private void serializeWithDeserialization(Result result) {
        testJsonMapper.readValue(testJsonMapper.writeValueAsString(result), Result.class);
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
