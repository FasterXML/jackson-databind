package com.fasterxml.jackson.databind.util;

/**
 * Helper interface for things that convert Objects of
 * one type to another.
 *
 * @param <IN>
 * @param <OUT>
 * 
 * @seealso {@link com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer}
 * 
 * @since 2.1
 */
public interface Converter<IN,OUT>
{
    /**
     * Main conversion methods
     */
    public OUT convert(IN value);
}
