package com.fasterxml.jackson.databind.ext.jdk8;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.IntStream;

import org.junit.Test;

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

    private int[] roundTrip(IntStream stream) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsBytes(stream), int[].class);
        } catch (IOException e) {
            sneakyThrow(e);
            return null;
        }
    }
}
