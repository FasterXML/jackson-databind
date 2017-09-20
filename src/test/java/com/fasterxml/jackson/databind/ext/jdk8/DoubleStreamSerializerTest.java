package com.fasterxml.jackson.databind.ext.jdk8;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.DoubleStream;

import org.junit.Test;

public class DoubleStreamSerializerTest extends StreamTestBase
{
    final double[] empty = {};

    final double[] single = { 1L };

    final double[] multipleValues = { Double.MIN_VALUE, Double.MAX_VALUE, 1.0, 0.0, 6.0, -3.0 };

    final String exceptionMessage = "DoubleStream peek threw";

    @Test
    public void testEmptyStream() throws Exception {

        assertArrayEquals(empty, roundTrip(DoubleStream.empty()), 0.0);
    }

    @Test
    public void testSingleElement() throws Exception {

        assertArrayEquals(single, roundTrip(DoubleStream.of(single)), 0.0);
    }

    @Test
    public void testMultiElements() throws Exception {

        assertArrayEquals(multipleValues, roundTrip(DoubleStream.of(multipleValues)), 0.0);
    }

    @Test
    public void testDoubleStreamCloses() throws Exception {

        assertClosesOnSuccess(DoubleStream.of(multipleValues), this::roundTrip);
    }

    @Test
    public void testDoubleStreamClosesOnRuntimeException() throws Exception {

        assertClosesOnRuntimeException(exceptionMessage, this::roundTrip, DoubleStream.of(multipleValues)
            .peek(e -> {
                throw new RuntimeException(exceptionMessage);
            }));

    }

    @Test
    public void testDoubleStreamClosesOnSneakyIOException() throws Exception {
        assertClosesOnIoException(exceptionMessage, this::roundTrip, DoubleStream.of(multipleValues)
            .peek(e -> {
                sneakyThrow(new IOException(exceptionMessage));
            }));

    }

    @Test
    public void testDoubleStreamClosesOnWrappedIoException() {

        assertClosesOnWrappedIoException(exceptionMessage, this::roundTrip, DoubleStream.of(multipleValues)
            .peek(e -> {
                throw new UncheckedIOException(new IOException(exceptionMessage));
            }));

    }

    private double[] roundTrip(DoubleStream stream) {
        try {
            String json = objectMapper.writeValueAsString(stream);
            return objectMapper.readValue(json, double[].class);
        } catch (IOException e) {
            sneakyThrow(e);
            return null;
        }
    }
}
