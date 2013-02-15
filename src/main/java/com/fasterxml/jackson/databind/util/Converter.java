package com.fasterxml.jackson.databind.util;

/**
 * Helper interface for things that convert Objects of
 * one type to another.
 *
 * @param <IN> Type of values converter takes
 * @param <OUT> Result type from conversion
 * 
 * @see com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer
 * 
 * @since 2.1
 */
public interface Converter<IN,OUT>
{
    /**
     * Main conversion method.
     */
    public OUT convert(IN value);

    /*
    /**********************************************************
    /* Helper class(es)
    /**********************************************************
     */

    /**
     * This marker class is only to be used with annotations, to
     * indicate that <b>no converter is to be used</b>.
     *<p>
     * Specifically, this class is to be used as the marker for
     * annotation {@link com.fasterxml.jackson.databind.annotation.JsonSerialize},
     * property <code>converter</code> (and related)
     * 
     * @since 2.2
     */
    public abstract static class None
        implements Converter<Object,Object> { }
}
