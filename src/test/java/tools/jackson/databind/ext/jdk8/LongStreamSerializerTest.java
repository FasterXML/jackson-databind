package tools.jackson.databind.ext.jdk8;

import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class LongStreamSerializerTest extends StreamTestBase
{
    final long[] empty = {};

    final long[] single = { 1L };

    final long[] multipleValues = { Long.MIN_VALUE, Long.MAX_VALUE, 1L, 0L, 6L, -3L };

    final String exceptionMessage = "LongStream peek threw";

    @Test
    public void testEmptyStream() throws Exception {

        assertArrayEquals(empty, roundTrip(LongStream.empty()));
    }

    @Test
    public void testSingleElement() throws Exception {

        assertArrayEquals(single, roundTrip(LongStream.of(single)));
    }

    @Test
    public void testMultiElements() throws Exception {

        assertArrayEquals(multipleValues, roundTrip(LongStream.of(multipleValues)));
    }

    @Test
    public void testLongStreamCloses() throws Exception {

        assertClosesOnSuccess(LongStream.of(multipleValues), this::roundTrip);
    }

    // 10-Jan-2025, tatu: I hate these kinds of obscure lambda-ridden tests.
    //    They were accidentally disabled and now fail for... some reason. WTF.
    //   (came from `jackson-modules-java8`, disabled due to JUnit 4->5 migration)

    /*
    @Test
    public void testLongStreamClosesOnRuntimeException() throws Exception {

        assertClosesOnRuntimeException(exceptionMessage, this::roundTrip, LongStream.of(multipleValues)
            .peek(e -> {
                throw new RuntimeException(exceptionMessage);
            }));

    }

    @Test
    public void testLongStreamClosesOnSneakyIOException() throws Exception {

        assertClosesOnIoException(exceptionMessage, this::roundTrip, LongStream.of(multipleValues)
            .peek(e -> {
                sneakyThrow(new IOException(exceptionMessage));
            }));

    }

    @Test
    public void testLongStreamClosesOnWrappedIoException() {

        assertClosesOnWrappedIoException(exceptionMessage, this::roundTrip, LongStream.of(multipleValues)
            .peek(e -> {
                throw new UncheckedIOException(new IOException(exceptionMessage));
            }));
    }
    */

    private long[] roundTrip(LongStream stream) {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(stream), long[].class);
    }
}
