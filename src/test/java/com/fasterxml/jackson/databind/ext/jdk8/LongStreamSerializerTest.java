package com.fasterxml.jackson.databind.ext.jdk8;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.LongStream;

import org.junit.Test;

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

    private long[] roundTrip(LongStream stream) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsBytes(stream), long[].class);
        } catch (IOException e) {
            sneakyThrow(e);
            return null;
        }
    }
}
