package tools.jackson.databind.deser;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.DeferredBindingException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for error-collecting deserialization feature (issue #1196).
 * Verifies opt-in per-call error collection via ObjectReader.problemCollectingReader().
 */
public class CollectingErrorsTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test POJOs
    /**********************************************************************
     */

    static class Person {
        public String name;
        public int age;
        public boolean active;
    }

    static class Order {
        public int orderId;
        public List<Item> items;
    }

    static class Item {
        public String sku;
        public double price;
        public int quantity;
    }

    static class TypedData {
        public int intValue;
        public long longValue;
        public double doubleValue;
        public float floatValue;
        public boolean boolValue;
        public Integer boxedInt;
        public String stringValue;
    }

    static class JsonPointerTestBean {
        public String normalField;
        public String fieldWithSlash;
        public String fieldWithTilde;
        public String fieldWithBoth;
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    /**
     * Helper to reduce boilerplate: captures DeferredBindingException and returns it.
     * Use with AssertJ for cleaner assertions.
     */
    private DeferredBindingException expectDeferredBinding(ObjectReader reader, String json) {
        return catchThrowableOfType(
            DeferredBindingException.class,
            () -> reader.readValueCollectingProblems(json)
        );
    }

    /**
     * Overload for byte[] input
     */
    private DeferredBindingException expectDeferredBinding(ObjectReader reader, byte[] json) {
        return catchThrowableOfType(
            DeferredBindingException.class,
            () -> reader.readValueCollectingProblems(json)
        );
    }

    /**
     * Overload for File input
     */
    private DeferredBindingException expectDeferredBinding(ObjectReader reader, File json) {
        return catchThrowableOfType(
            DeferredBindingException.class,
            () -> reader.readValueCollectingProblems(json)
        );
    }

    /**
     * Overload for InputStream input
     */
    private DeferredBindingException expectDeferredBinding(ObjectReader reader, InputStream json) {
        return catchThrowableOfType(
            DeferredBindingException.class,
            () -> reader.readValueCollectingProblems(json)
        );
    }

    /**
     * Overload for Reader input
     */
    private DeferredBindingException expectDeferredBinding(ObjectReader reader, Reader json) {
        return catchThrowableOfType(
            DeferredBindingException.class,
            () -> reader.readValueCollectingProblems(json)
        );
    }

    /**
     * Helper to build JSON with specified number of invalid order items.
     * Used for testing limit behavior and hard failures.
     */
    private String buildInvalidOrderJson(int itemCount) {
        StringBuilder json = new StringBuilder("{\"items\":[");
        for (int i = 0; i < itemCount; i++) {
            if (i > 0) json.append(",");
            json.append("{\"price\":\"invalid").append(i).append("\"}");
        }
        json.append("]}");
        return json.toString();
    }

    /*
    /**********************************************************************
    /* Test: Default behavior (fail-fast unchanged)
    /**********************************************************************
     */

    @Nested
    @DisplayName("Default fail-fast behavior")
    class DefaultBehaviorTests {

        @Test
        @DisplayName("should fail fast by default when error encountered")
        void failFastDefault() {
            // setup
            String json = "{\"name\":\"John\",\"age\":\"not-a-number\"}";

            // when/then
            assertThatThrownBy(() -> MAPPER.readValue(json, Person.class))
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("not-a-number");
        }

        @Test
        @DisplayName("should fail fast when using regular readValue even after problemCollectingReader")
        void failFastAfterCollectErrors() {
            // setup
            String json = "{\"name\":\"John\",\"age\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(Person.class).problemCollectingReader();

            // when/then - using regular readValue, not readValueCollectingProblems
            assertThatThrownBy(() -> reader.readValue(json))
                .isInstanceOf(DatabindException.class);
        }
    }

    /*
    /**********************************************************************
    /* Test: Per-call bucket isolation
    /**********************************************************************
     */

    @Nested
    @DisplayName("Per-call bucket isolation")
    class BucketIsolationTests {

        @Test
        @DisplayName("should isolate errors between successive calls")
        void successiveCalls() {
            // setup
            ObjectReader reader = MAPPER.readerFor(Person.class).problemCollectingReader();
            String json1 = "{\"name\":\"Alice\",\"age\":\"invalid1\"}";
            String json2 = "{\"name\":\"Bob\",\"age\":\"invalid2\"}";

            // when
            DeferredBindingException ex1 = expectDeferredBinding(reader, json1);
            DeferredBindingException ex2 = expectDeferredBinding(reader, json2);

            // then
            assertThat(ex1).isNotNull();
            assertThat(ex2).isNotNull();
            assertThat(ex1.getProblems()).hasSize(1);
            assertThat(ex2.getProblems()).hasSize(1);
            assertThat(ex1.getProblems().get(0).getRawValue()).isEqualTo("invalid1");
            assertThat(ex2.getProblems().get(0).getRawValue()).isEqualTo("invalid2");
        }

        @Test
        @DisplayName("should isolate errors in concurrent calls")
        void concurrentCalls() throws Exception {
            // setup
            ObjectReader reader = MAPPER.readerFor(Person.class).problemCollectingReader();
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            List<DeferredBindingException> exceptions =
                Collections.synchronizedList(new ArrayList<>());
            List<Throwable> unexpectedErrors =
                Collections.synchronizedList(new ArrayList<>());

            // when
            try {
                for (int i = 0; i < threadCount; i++) {
                    final int index = i;
                    executor.submit(() -> {
                        try {
                            String json = String.format("{\"name\":\"User%d\",\"age\":\"invalid%d\"}",
                                index, index);
                            reader.readValueCollectingProblems(json);
                            unexpectedErrors.add(new AssertionError("Should have thrown DeferredBindingException"));
                        } catch (DeferredBindingException e) {
                            exceptions.add(e);
                            successCount.incrementAndGet();
                        } catch (Throwable t) {
                            unexpectedErrors.add(t);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                // Wait for all threads with assertion
                assertThat(latch.await(5, TimeUnit.SECONDS))
                    .as("All threads should complete within timeout")
                    .isTrue();

            } finally {
                executor.shutdown();
                try {
                    assertThat(executor.awaitTermination(2, TimeUnit.SECONDS))
                        .as("Executor should terminate within timeout")
                        .isTrue();
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            // then
            assertThat(unexpectedErrors)
                .as("No unexpected exceptions should occur")
                .isEmpty();
            assertThat(successCount.get()).isEqualTo(threadCount);

            // Synchronize iteration per JDK contract for Collections.synchronizedList()
            synchronized (exceptions) {
                assertThat(exceptions).hasSize(threadCount);

                // Verify each exception has exactly 1 problem and collect all raw values
                List<String> rawValues = new ArrayList<>();
                for (DeferredBindingException ex : exceptions) {
                    assertThat(ex.getProblems()).hasSize(1);
                    String rawValue = (String) ex.getProblems().get(0).getRawValue();
                    rawValues.add(rawValue);
                }

                // Verify we have exactly the unique values from each thread (no bucket sharing)
                assertThat(rawValues)
                    .as("Each thread should have its own isolated error bucket")
                    .containsExactlyInAnyOrder(
                        "invalid0", "invalid1", "invalid2", "invalid3", "invalid4",
                        "invalid5", "invalid6", "invalid7", "invalid8", "invalid9"
                    );
            }
        }
    }

    /*
    /**********************************************************************
    /* Test: JSON Pointer escaping (RFC 6901)
    /**********************************************************************
     */

    @Nested
    @DisplayName("JSON Pointer escaping (RFC 6901)")
    class JsonPointerEscapingTests {

        @Test
        @DisplayName("should escape tilde in property names")
        void escapeTilde() {
            // setup
            String json = "{\"field~name\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(JsonPointerTestBean.class)
                .problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            // Tilde should be escaped as ~0
            assertThat(ex.getProblems().get(0).getPath().toString())
                .isEqualTo("/field~0name");
        }

        @Test
        @DisplayName("should escape slash in property names")
        void escapeSlash() {
            // setup
            String json = "{\"field/name\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(JsonPointerTestBean.class)
                .problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            // Slash should be escaped as ~1
            assertThat(ex.getProblems().get(0).getPath().toString())
                .isEqualTo("/field~1name");
        }

        @Test
        @DisplayName("should escape both tilde and slash correctly")
        void escapeBoth() {
            // setup
            String json = "{\"field~/name\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(JsonPointerTestBean.class)
                .problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            // Must escape ~ first (to ~0), then / (to ~1)
            assertThat(ex.getProblems().get(0).getPath().toString())
                .isEqualTo("/field~0~1name");
        }

        @Test
        @DisplayName("should handle array indices in pointer")
        void arrayIndices() {
            // setup
            String json = "{\"orderId\":123,\"items\":[" +
                "{\"sku\":\"ABC\",\"price\":\"invalid\",\"quantity\":5}," +
                "{\"sku\":\"DEF\",\"price\":99.99,\"quantity\":\"bad\"}" +
                "]}";
            ObjectReader reader = MAPPER.readerFor(Order.class).problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(2);
            assertThat(ex.getProblems().get(0).getPath().toString())
                .contains("/items/0/price");
            assertThat(ex.getProblems().get(1).getPath().toString())
                .contains("/items/1/quantity");
        }
    }

    /*
    /**********************************************************************
    /* Test: Limit reached behavior
    /**********************************************************************
     */

    @Nested
    @DisplayName("Limit reached behavior")
    class LimitReachedTests {

        @Test
        @DisplayName("should stop collecting when default limit reached")
        void defaultLimit() {
            // setup - create JSON with 101 errors (default limit is 100)
            String json = buildInvalidOrderJson(101);
            ObjectReader reader = MAPPER.readerFor(Order.class).problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then - should get DeferredBindingException as primary when limit reached
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(100); // Stopped at limit
            assertThat(ex.isLimitReached()).isTrue();
            assertThat(ex.getMessage()).contains("limit reached");

            // Original DatabindException should be in suppressed for debugging
            Throwable[] suppressed = ex.getSuppressed();
            assertThat(suppressed).hasSizeGreaterThanOrEqualTo(1);
            assertThat(suppressed[0]).isInstanceOf(DatabindException.class);
        }

        @Test
        @DisplayName("should respect custom limit")
        void customLimit() {
            // setup
            String json = buildInvalidOrderJson(20);
            ObjectReader reader = MAPPER.readerFor(Order.class).problemCollectingReader(10);

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then - should get DeferredBindingException as primary when limit reached
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(10); // Custom limit
            assertThat(ex.isLimitReached()).isTrue();

            // Original DatabindException should be in suppressed for debugging
            Throwable[] suppressed = ex.getSuppressed();
            assertThat(suppressed).hasSizeGreaterThanOrEqualTo(1);
            assertThat(suppressed[0]).isInstanceOf(DatabindException.class);
        }

        @Test
        @DisplayName("should not set limit reached when under limit")
        void underLimit() {
            // setup
            String json = "{\"name\":\"John\",\"age\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(Person.class).problemCollectingReader(100);

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            assertThat(ex.isLimitReached()).isFalse();
            assertThat(ex.getMessage()).doesNotContain("limit reached");
        }
    }

    /*
    /**********************************************************************
    /* Test: Unknown property handling
    /**********************************************************************
     */

    @Nested
    @DisplayName("Unknown property handling")
    class UnknownPropertyTests {

        @Test
        @DisplayName("should collect unknown property errors when FAIL_ON_UNKNOWN_PROPERTIES enabled")
        void unknownProperty() {
            // setup
            String json = "{\"name\":\"Alice\",\"unknownField\":\"value\",\"age\":30}";
            ObjectReader reader = MAPPER.readerFor(Person.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then - unknown property error is collected
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            assertThat(ex.getProblems().get(0).getMessage())
                .contains("Unknown property 'unknownField'");
        }

        @Test
        @DisplayName("should skip unknown property children")
        void skipUnknownChildren() {
            // setup
            String json = "{\"name\":\"Bob\",\"unknownObject\":{\"nested\":\"value\"},\"age\":25}";
            ObjectReader reader = MAPPER.readerFor(Person.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            assertThat(ex.getProblems().get(0).getMessage())
                .contains("Unknown property 'unknownObject'");
        }
    }

    /*
    /**********************************************************************
    /* Test: Default value policy (primitives vs references)
    /**********************************************************************
     */

    @Nested
    @DisplayName("Default value policy")
    class DefaultValuePolicyTests {

        @Test
        @DisplayName("should collect error for primitive int coercion")
        void primitiveInt() {
            // setup
            String json = "{\"intValue\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(TypedData.class).problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then - error collected with default value used
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            assertThat(ex.getProblems().get(0).getRawValue()).isEqualTo("invalid");
        }

        @Test
        @DisplayName("should collect error for primitive long coercion")
        void primitiveLong() {
            // setup
            String json = "{\"longValue\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(TypedData.class).problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
        }

        @Test
        @DisplayName("should collect error for primitive double coercion")
        void primitiveDouble() {
            // setup
            String json = "{\"doubleValue\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(TypedData.class).problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
        }

        @Test
        @DisplayName("should collect error for primitive boolean coercion")
        void primitiveBoolean() {
            // setup
            String json = "{\"boolValue\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(TypedData.class).problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
        }

        @Test
        @DisplayName("should collect error for boxed Integer coercion")
        void boxedInteger() {
            // setup
            String json = "{\"boxedInt\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(TypedData.class).problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then - error collected for reference type
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
        }

        @Test
        @DisplayName("should handle multiple type coercion errors")
        void multipleTypeErrors() {
            // setup
            String json = "{\"intValue\":\"bad1\",\"longValue\":\"bad2\",\"doubleValue\":\"bad3\"}";
            ObjectReader reader = MAPPER.readerFor(TypedData.class).problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(3);
            assertThat(ex.getProblems())
                .extracting(p -> p.getPath().toString())
                .containsExactlyInAnyOrder("/intValue", "/longValue", "/doubleValue");
        }
    }

    /*
    /**********************************************************************
    /* Test: Root-level problems
    /**********************************************************************
     */

    @Nested
    @DisplayName("Root-level problems")
    class RootLevelTests {

        @Test
        @DisplayName("should not collect root-level type mismatches")
        void rootLevelTypeMismatch() {
            // setup - root value is invalid for Person (non-recoverable)
            String json = "\"not-an-object\"";
            ObjectReader reader = MAPPER.readerFor(Person.class).problemCollectingReader();

            // when/then - root-level type mismatches are non-recoverable
            // They occur before property deserialization, so handler is never invoked
            assertThatThrownBy(() -> reader.readValueCollectingProblems(json))
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("Cannot construct instance")
                .satisfies(ex -> {
                    // Verify no problems were collected (root errors are non-recoverable)
                    assertThat(ex.getSuppressed()).isEmpty();
                });
        }

        @Test
        @DisplayName("should format property paths correctly without double slashes")
        void propertyPathFormatting() {
            // setup
            String json = "{\"age\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(Person.class).problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then
            assertThat(ex).isNotNull();
            String pointer = ex.getProblems().get(0).getPath().toString();
            assertThat(pointer).isEqualTo("/age");
            assertThat(pointer).doesNotContain("//"); // No double slashes
        }
    }

    /*
    /**********************************************************************
    /* Test: Hard failure with suppressed exceptions
    /**********************************************************************
     */

    @Nested
    @DisplayName("Hard failure with suppressed exceptions")
    class HardFailureTests {

        @Test
        @DisplayName("should attach collected problems as suppressed on hard failure")
        void suppressedProblems() {
            // setup - create JSON with 101 errors to trigger limit
            // When limit is reached, DeferredBindingException is thrown as primary,
            // with original DatabindException as suppressed
            String json = buildInvalidOrderJson(101);
            ObjectReader reader = MAPPER.readerFor(Order.class).problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then - verify suppressed exception attachment mechanism
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(100);
            assertThat(ex.isLimitReached()).isTrue();

            // Original DatabindException should be in suppressed for debugging
            assertThat(ex.getSuppressed())
                .as("Original DatabindException should be attached as suppressed")
                .hasSizeGreaterThanOrEqualTo(1);
            assertThat(ex.getSuppressed()[0]).isInstanceOf(DatabindException.class);
        }
    }

    /*
    /**********************************************************************
    /* Test: Message formatting
    /**********************************************************************
     */

    @Nested
    @DisplayName("Message formatting")
    class MessageFormattingTests {

        @Test
        @DisplayName("should format single error message")
        void singleError() {
            // setup
            String json = "{\"age\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(Person.class).problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getMessage()).contains("1 deserialization problem");
        }

        @Test
        @DisplayName("should format multiple errors with first 5 shown")
        void multipleErrors() {
            // setup
            String json = buildInvalidOrderJson(10);
            ObjectReader reader = MAPPER.readerFor(Order.class).problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, json);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getMessage())
                .contains("10 deserialization problems")
                .contains("showing first 5")
                .contains("... and 5 more");
        }
    }

    /*
    /**********************************************************************
    /* Test: Edge cases
    /**********************************************************************
     */

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should validate positive maxProblems")
        void validateMaxProblems() {
            // when/then
            assertThatThrownBy(() -> MAPPER.readerFor(Person.class).problemCollectingReader(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxProblems must be positive");

            assertThatThrownBy(() -> MAPPER.readerFor(Person.class).problemCollectingReader(-1))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should handle empty JSON")
        void emptyJson() {
            // setup
            String json = "{}";
            ObjectReader reader = MAPPER.readerFor(Person.class).problemCollectingReader();

            // when
            Person result = reader.readValueCollectingProblems(json);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name).isNull();
            assertThat(result.age).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle null parser gracefully")
        void nullParser() {
            // setup
            ObjectReader reader = MAPPER.readerFor(Person.class).problemCollectingReader();

            // when/then
            assertThatThrownBy(() -> reader.readValueCollectingProblems((String) null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should collect errors via byte[] overload")
        void collectFromByteArray() {
            // setup
            String jsonString = "{\"name\":\"Alice\",\"age\":\"invalid\"}";
            byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);
            ObjectReader reader = MAPPER.readerFor(Person.class).problemCollectingReader();

            // when
            DeferredBindingException ex = expectDeferredBinding(reader, jsonBytes);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            assertThat(ex.getProblems().get(0).getPath().toString()).isEqualTo("/age");
        }

        @Test
        @DisplayName("should collect errors via File overload")
        void collectFromFile() throws Exception {
            // setup
            File tempFile = File.createTempFile("test", ".json");
            try {
                java.nio.file.Files.writeString(tempFile.toPath(),
                    "{\"name\":\"Bob\",\"age\":\"notANumber\"}");
                ObjectReader reader = MAPPER.readerFor(Person.class).problemCollectingReader();

                // when
                DeferredBindingException ex = expectDeferredBinding(reader, tempFile);

                // then
                assertThat(ex).isNotNull();
                assertThat(ex.getProblems()).hasSize(1);
                assertThat(ex.getProblems().get(0).getMessage())
                    .contains("notANumber");
            } finally {
                java.nio.file.Files.deleteIfExists(tempFile.toPath());
            }
        }

        @Test
        @DisplayName("should collect errors via InputStream overload")
        void collectFromInputStream() throws Exception {
            // setup
            String json = "{\"name\":\"Charlie\",\"age\":\"bad\"}";
            ObjectReader reader = MAPPER.readerFor(Person.class).problemCollectingReader();

            // when
            DeferredBindingException ex;
            try (InputStream input = new java.io.ByteArrayInputStream(
                    json.getBytes(StandardCharsets.UTF_8))) {
                ex = expectDeferredBinding(reader, input);
            }

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            assertThat(ex.getProblems().get(0).getPath().toString()).isEqualTo("/age");
        }

        @Test
        @DisplayName("should collect errors via Reader overload")
        void collectFromReader() throws Exception {
            // setup
            String json = "{\"name\":\"Diana\",\"age\":\"invalid\"}";
            ObjectReader objectReader = MAPPER.readerFor(Person.class).problemCollectingReader();

            // when
            DeferredBindingException ex;
            try (Reader reader = new java.io.StringReader(json)) {
                ex = expectDeferredBinding(objectReader, reader);
            }

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            assertThat(ex.getProblems().get(0).getPath().toString()).isEqualTo("/age");
        }
    }
}
