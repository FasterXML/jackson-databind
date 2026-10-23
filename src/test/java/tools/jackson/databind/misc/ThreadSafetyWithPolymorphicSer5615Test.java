package tools.jackson.databind.misc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.junit.jupiter.api.RepeatedTest;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for [databind#5615]: Race condition with {@code @JsonIgnoreProperties}
 * and {@code @JsonTypeInfo(include = As.PROPERTY)} on serialization side.
 *<p>
 * NOTE: modified for [databind#1410].
 */
public class ThreadSafetyWithPolymorphicSer5615Test
    extends DatabindTestUtil
{
    @JsonIgnoreProperties(value = {"type"}, allowSetters = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = LivingRoom.class, name = "Living"),
            @JsonSubTypes.Type(value = SleepingRoom.class, name = "Sleeping")
    })
    interface Room {
        @JsonProperty("typ")
        RoomType getTyp();
    }

    record LivingRoom(@JsonProperty("typ") RoomType typ,
            @JsonProperty("animals") List<Cat> animals)
        implements Room
    {
        @Override
        public RoomType getTyp() { return typ; }
    }

    record SleepingRoom(@JsonProperty("typ") RoomType typ,
            @JsonProperty("animals") List<Dog> animals)
        implements Room
    {
        @Override
        public RoomType getTyp() { return typ; }
    }

    enum RoomType { Living, Sleeping }

    @JsonIgnoreProperties(value = {"type"}, allowSetters = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Cat.class, name = "Cat"),
            @JsonSubTypes.Type(value = Dog.class, name = "Dog")
    })
    interface Animal {
        @JsonProperty("typ")
        AnimalType getTyp();
    }

    record Cat(@JsonProperty("typ") AnimalType typ) implements Animal {
        @Override
        public AnimalType getTyp() { return typ; }
    }

    record Dog(@JsonProperty("typ") AnimalType typ) implements Animal {
        @Override
        public AnimalType getTyp() { return typ; }
    }

    enum AnimalType { Dog, Cat }

    record Result(@JsonProperty("rooms") List<Room> rooms) { }

    private static final String ROOMS_JSON = """
            {
                "rooms": [
                    {
                      "type": "Living",
                      "typ": "Living",
                      "animals": [
                            { "type": "Cat", "typ": "Cat" },
                            { "type": "Cat", "typ": "Cat" },
                            { "type": "Cat", "typ": "Cat" },
                            { "type": "Cat", "typ": "Cat" }
                      ]
                    },
                    {
                      "type": "Sleeping",
                      "typ": "Sleeping",
                      "animals": [
                            { "type": "Dog", "typ": "Dog" },
                            { "type": "Dog", "typ": "Dog" },
                            { "type": "Dog", "typ": "Dog" },
                            { "type": "Dog", "typ": "Dog" }
                      ]
                    }
                ]
            }
            """;

    // Use 50 repetitions; original test uses 1000 but 50 should be enough
    // to trigger the race condition
    @RepeatedTest(50)
    public void testConcurrentDeserializationWithJsonIgnoreAndTypeInfo() throws Exception {
        final JsonMapper mapper = newJsonMapper();
        final Result result = mapper.readValue(ROOMS_JSON, Result.class);

        int threadCount = 10;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();
        List<Thread> threads = new java.util.ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                try {
                    barrier.await();
                    for (int j = 0; j < 100; j++) {
                        String json = mapper.writeValueAsString(result);
                        try {
                            mapper.readValue(json, Result.class);
                        } catch (Throwable e) {
                            // Capture the JSON that caused the failure
                            errors.add(new RuntimeException("Failed on JSON: " + json, e));
                            return;
                        }
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

        assertTrue(errors.isEmpty(),
                () -> "test failed with %d error(s):\n%s".formatted(
                        errors.size(),
                        errors.stream()
                                .map(Throwable::toString)
                                .collect(Collectors.joining("\n"))));
    }
}
