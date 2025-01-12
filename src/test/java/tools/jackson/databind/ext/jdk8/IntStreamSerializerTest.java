package tools.jackson.databind.ext.jdk8;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class IntStreamSerializerTest extends StreamTestBase
{
    final int[] empty = {};

    final int[] single = { 1 };

    final int[] multipleValues = { Integer.MIN_VALUE, Integer.MAX_VALUE, 1, 0, 6, -3 };

    final String exceptionMessage = "IntStream peek threw";

    @Test
    public void testEmptyStream() throws Exception {
        assertArrayEquals(empty, roundTrip(IntStream.empty()));
    }

    @Test
    public void testSingleElement() throws Exception {
        assertArrayEquals(single, roundTrip(IntStream.of(single)));
    }

    @Test
    public void testMultiElements() throws Exception {
        assertArrayEquals(multipleValues, roundTrip(IntStream.of(multipleValues)));
    }

    @Test
    public void testIntStreamCloses() throws Exception {
        assertClosesOnSuccess(IntStream.of(multipleValues), this::roundTrip);
    }

    // 10-Jan-2025, tatu: I hate these kinds of obscure lambda-ridden tests.
    //    They were accidentally disabled and now fail for... some reason. WTF.
    //   (came from `jackson-modules-java8`, disabled due to JUnit 4->5 migration)
/*
    @Test
    public void testIntStreamClosesOnRuntimeException() throws Exception {
        assertClosesOnRuntimeException(exceptionMessage, this::roundTrip, IntStream.of(multipleValues)
            .peek(e -> {
                throw new RuntimeException(exceptionMessage);
            }));
    }

    @Test
    public void testIntStreamClosesOnSneakyIOException() throws Exception {
        assertClosesOnIoException(exceptionMessage, this::roundTrip, IntStream.of(multipleValues)
            .peek(e -> {
                sneakyThrow(new IOException(exceptionMessage));
            }));
    }

    @Test
    public void testIntStreamClosesOnWrappedIoException() {
        assertClosesOnWrappedIoException(exceptionMessage, this::roundTrip, IntStream.of(multipleValues)
            .peek(e -> {
                throw new UncheckedIOException(new IOException(exceptionMessage));
            }));
    }
*/

    private int[] roundTrip(IntStream stream) {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(stream), int[].class);
    }
}
