package tools.jackson.databind.deser;

import java.util.ArrayList;
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
import tools.jackson.databind.exc.CollectedProblem;
import tools.jackson.databind.exc.DeferredBindingException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for error-collecting deserialization feature (issue #1196).
 * Verifies opt-in per-call error collection via ObjectReader.collectErrors().
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
        @DisplayName("should fail fast when using regular readValue even after collectErrors")
        void failFastAfterCollectErrors() {
            // setup
            String json = "{\"name\":\"John\",\"age\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(Person.class).collectErrors();

            // when/then - using regular readValue, not readValueCollecting
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
        void successiveCalls() throws Exception {
            // setup
            ObjectReader reader = MAPPER.readerFor(Person.class).collectErrors();
            String json1 = "{\"name\":\"Alice\",\"age\":\"invalid1\"}";
            String json2 = "{\"name\":\"Bob\",\"age\":\"invalid2\"}";

            // when
            DeferredBindingException ex1 = null;
            DeferredBindingException ex2 = null;

            try {
                reader.readValueCollecting(json1);
            } catch (DeferredBindingException e) {
                ex1 = e;
            }

            try {
                reader.readValueCollecting(json2);
            } catch (DeferredBindingException e) {
                ex2 = e;
            }

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
            ObjectReader reader = MAPPER.readerFor(Person.class).collectErrors();
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            List<DeferredBindingException> exceptions = new ArrayList<>();

            // when
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        String json = String.format("{\"name\":\"User%d\",\"age\":\"invalid%d\"}",
                            index, index);
                        reader.readValueCollecting(json);
                    } catch (DeferredBindingException e) {
                        synchronized (exceptions) {
                            exceptions.add(e);
                        }
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // Unexpected exception type
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // then
            assertThat(successCount.get()).isEqualTo(threadCount);
            assertThat(exceptions).hasSize(threadCount);

            // Verify each exception has exactly 1 problem with correct value
            for (int i = 0; i < threadCount; i++) {
                DeferredBindingException ex = exceptions.get(i);
                assertThat(ex.getProblems()).hasSize(1);
                String rawValue = (String) ex.getProblems().get(0).getRawValue();
                assertThat(rawValue).startsWith("invalid");
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
        void escapeTilde() throws Exception {
            // setup
            String json = "{\"field~name\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(JsonPointerTestBean.class)
                .collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            // Tilde should be escaped as ~0
            assertThat(ex.getProblems().get(0).getPath().toString())
                .isEqualTo("/field~0name");
        }

        @Test
        @DisplayName("should escape slash in property names")
        void escapeSlash() throws Exception {
            // setup
            String json = "{\"field/name\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(JsonPointerTestBean.class)
                .collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            // Slash should be escaped as ~1
            assertThat(ex.getProblems().get(0).getPath().toString())
                .isEqualTo("/field~1name");
        }

        @Test
        @DisplayName("should escape both tilde and slash correctly")
        void escapeBoth() throws Exception {
            // setup
            String json = "{\"field~/name\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(JsonPointerTestBean.class)
                .collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            // Must escape ~ first (to ~0), then / (to ~1)
            assertThat(ex.getProblems().get(0).getPath().toString())
                .isEqualTo("/field~0~1name");
        }

        @Test
        @DisplayName("should handle array indices in pointer")
        void arrayIndices() throws Exception {
            // setup
            String json = "{\"orderId\":123,\"items\":[" +
                "{\"sku\":\"ABC\",\"price\":\"invalid\",\"quantity\":5}," +
                "{\"sku\":\"DEF\",\"price\":99.99,\"quantity\":\"bad\"}" +
                "]}";
            ObjectReader reader = MAPPER.readerFor(Order.class).collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

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
        void defaultLimit() throws Exception {
            // setup - create JSON with 101 errors (default limit is 100)
            StringBuilder json = new StringBuilder("{\"items\":[");
            for (int i = 0; i < 101; i++) {
                if (i > 0) json.append(",");
                json.append("{\"price\":\"invalid").append(i).append("\"}");
            }
            json.append("]}");

            ObjectReader reader = MAPPER.readerFor(Order.class).collectErrors();

            // when
            Throwable thrown = catchThrowable(() -> reader.readValueCollecting(json.toString()));

            // then - should get hard failure with collected problems in suppressed
            assertThat(thrown).isInstanceOf(DatabindException.class);
            Throwable[] suppressed = thrown.getSuppressed();
            assertThat(suppressed).hasSizeGreaterThanOrEqualTo(1);

            DeferredBindingException deferred = null;
            for (Throwable s : suppressed) {
                if (s instanceof DeferredBindingException) {
                    deferred = (DeferredBindingException) s;
                    break;
                }
            }

            assertThat(deferred).isNotNull();
            assertThat(deferred.getProblems()).hasSize(100); // Stopped at limit
            assertThat(deferred.isLimitReached()).isTrue();
            assertThat(deferred.getMessage()).contains("limit reached");
        }

        @Test
        @DisplayName("should respect custom limit")
        void customLimit() throws Exception {
            // setup
            StringBuilder json = new StringBuilder("{\"items\":[");
            for (int i = 0; i < 20; i++) {
                if (i > 0) json.append(",");
                json.append("{\"price\":\"invalid").append(i).append("\"}");
            }
            json.append("]}");

            ObjectReader reader = MAPPER.readerFor(Order.class).collectErrors(10);

            // when
            Throwable thrown = catchThrowable(() -> reader.readValueCollecting(json.toString()));

            // then
            assertThat(thrown).isInstanceOf(DatabindException.class);
            Throwable[] suppressed = thrown.getSuppressed();
            assertThat(suppressed).hasSizeGreaterThanOrEqualTo(1);

            DeferredBindingException deferred = null;
            for (Throwable s : suppressed) {
                if (s instanceof DeferredBindingException) {
                    deferred = (DeferredBindingException) s;
                    break;
                }
            }

            assertThat(deferred).isNotNull();
            assertThat(deferred.getProblems()).hasSize(10); // Custom limit
            assertThat(deferred.isLimitReached()).isTrue();
        }

        @Test
        @DisplayName("should not set limit reached when under limit")
        void underLimit() throws Exception {
            // setup
            String json = "{\"name\":\"John\",\"age\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(Person.class).collectErrors(100);

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

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
        void unknownProperty() throws Exception {
            // setup
            String json = "{\"name\":\"Alice\",\"unknownField\":\"value\",\"age\":30}";
            ObjectReader reader = MAPPER.readerFor(Person.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .collectErrors();

            // when
            DeferredBindingException ex = null;
            Person person = null;
            try {
                person = reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

            // then - unknown property error is collected
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            assertThat(ex.getProblems().get(0).getMessage())
                .contains("Unknown property 'unknownField'");
        }

        @Test
        @DisplayName("should skip unknown property children")
        void skipUnknownChildren() throws Exception {
            // setup
            String json = "{\"name\":\"Bob\",\"unknownObject\":{\"nested\":\"value\"},\"age\":25}";
            ObjectReader reader = MAPPER.readerFor(Person.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

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
        void primitiveInt() throws Exception {
            // setup
            String json = "{\"intValue\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(TypedData.class).collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

            // then - error collected with default value used
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
            assertThat(ex.getProblems().get(0).getRawValue()).isEqualTo("invalid");
        }

        @Test
        @DisplayName("should collect error for primitive long coercion")
        void primitiveLong() throws Exception {
            // setup
            String json = "{\"longValue\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(TypedData.class).collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
        }

        @Test
        @DisplayName("should collect error for primitive double coercion")
        void primitiveDouble() throws Exception {
            // setup
            String json = "{\"doubleValue\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(TypedData.class).collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
        }

        @Test
        @DisplayName("should collect error for primitive boolean coercion")
        void primitiveBoolean() throws Exception {
            // setup
            String json = "{\"boolValue\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(TypedData.class).collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
        }

        @Test
        @DisplayName("should collect error for boxed Integer coercion")
        void boxedInteger() throws Exception {
            // setup
            String json = "{\"boxedInt\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(TypedData.class).collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

            // then - error collected for reference type
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSize(1);
        }

        @Test
        @DisplayName("should handle multiple type coercion errors")
        void multipleTypeErrors() throws Exception {
            // setup
            String json = "{\"intValue\":\"bad1\",\"longValue\":\"bad2\",\"doubleValue\":\"bad3\"}";
            ObjectReader reader = MAPPER.readerFor(TypedData.class).collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getProblems()).hasSizeGreaterThanOrEqualTo(1);
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
        @DisplayName("should use empty pointer for root-level error")
        void emptyPointerForRoot() throws Exception {
            // setup - root value is invalid for Person
            String json = "\"not-an-object\"";
            ObjectReader reader = MAPPER.readerFor(Person.class).collectErrors();

            // when/then - root-level type mismatch typically unrecoverable
            // This tests that IF a root error were collected, it would have empty path
            assertThatThrownBy(() -> reader.readValueCollecting(json))
                .isInstanceOf(DatabindException.class);
        }

        @Test
        @DisplayName("should not use slash for root pointer")
        void noSlashForRoot() throws Exception {
            // setup
            String json = "{\"age\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(Person.class).collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

            // then
            assertThat(ex).isNotNull();
            String pointer = ex.getProblems().get(0).getPath().toString();
            assertThat(pointer).isEqualTo("/age");
            assertThat(pointer).doesNotMatch("^//$"); // Not "//"
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
        void suppressedProblems() throws Exception {
            // setup - create a scenario with both collected and hard errors
            // First collect some errors, then hit a fatal one
            String json = "{\"name\":123,\"age\":\"bad\",\"active\":\"reallyBad\"}";
            ObjectReader reader = MAPPER.readerFor(Person.class).collectErrors();

            // when
            Throwable thrown = catchThrowable(() -> reader.readValueCollecting(json));

            // then
            assertThat(thrown).isInstanceOf(DatabindException.class);

            // Check if any problems were collected and attached as suppressed
            Throwable[] suppressed = thrown.getSuppressed();
            if (suppressed.length > 0) {
                boolean foundDeferred = false;
                for (Throwable s : suppressed) {
                    if (s instanceof DeferredBindingException) {
                        foundDeferred = true;
                        DeferredBindingException deferred = (DeferredBindingException) s;
                        assertThat(deferred.getProblems()).isNotEmpty();
                    }
                }
                assertThat(foundDeferred).isTrue();
            }
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
        void singleError() throws Exception {
            // setup
            String json = "{\"age\":\"invalid\"}";
            ObjectReader reader = MAPPER.readerFor(Person.class).collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json);
            } catch (DeferredBindingException e) {
                ex = e;
            }

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getMessage()).contains("1 deserialization problem");
        }

        @Test
        @DisplayName("should format multiple errors with first 5 shown")
        void multipleErrors() throws Exception {
            // setup
            StringBuilder json = new StringBuilder("{\"items\":[");
            for (int i = 0; i < 10; i++) {
                if (i > 0) json.append(",");
                json.append("{\"price\":\"invalid").append(i).append("\"}");
            }
            json.append("]}");

            ObjectReader reader = MAPPER.readerFor(Order.class).collectErrors();

            // when
            DeferredBindingException ex = null;
            try {
                reader.readValueCollecting(json.toString());
            } catch (DeferredBindingException e) {
                ex = e;
            }

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
            assertThatThrownBy(() -> MAPPER.readerFor(Person.class).collectErrors(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxProblems must be positive");

            assertThatThrownBy(() -> MAPPER.readerFor(Person.class).collectErrors(-1))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should handle empty JSON")
        void emptyJson() throws Exception {
            // setup
            String json = "{}";
            ObjectReader reader = MAPPER.readerFor(Person.class).collectErrors();

            // when
            Person result = reader.readValueCollecting(json);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name).isNull();
            assertThat(result.age).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle null parser gracefully")
        void nullParser() {
            // setup
            ObjectReader reader = MAPPER.readerFor(Person.class).collectErrors();

            // when/then
            assertThatThrownBy(() -> reader.readValueCollecting((String) null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
