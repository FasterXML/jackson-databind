package tools.jackson.databind.util;

import tools.jackson.databind.*;
import tools.jackson.databind.type.TypeFactory;

/**
 * Helper interface for things that convert Objects of
 * one type to another.
 *<p>
 * NOTE: implementors are strongly encouraged to extend {@link StdConverter}
 * instead of directly implementing {@link Converter}, since that can
 * help with default implementation of typically boiler-plate code.
 *
 * @param <IN> Type of values converter takes
 * @param <OUT> Result type from conversion
 *
 * @see tools.jackson.databind.ser.std.StdDelegatingSerializer
 * @see tools.jackson.databind.deser.std.StdConvertingDeserializer
 */
public interface Converter<IN,OUT>
{
    /**
     * Conversion method to use on deserialization side.
     */
    public OUT convert(DeserializationContext ctxt, IN value);

    /**
     * Conversion method to use on serialization side.
     */
    public OUT convert(SerializationContext ctxt, IN value);

    /**
     * Method that can be used to find out actual input (source) type; this
     * usually can be determined from type parameters, but may need
     * to be implemented differently from programmatically defined
     * converters (which cannot change static type parameter bindings).
     */
    public JavaType getInputType(TypeFactory typeFactory);

    /**
     * Method that can be used to find out actual output (target) type; this
     * usually can be determined from type parameters, but may need
     * to be implemented differently from programmatically defined
     * converters (which cannot change static type parameter bindings).
     */
    public JavaType getOutputType(TypeFactory typeFactory);

    /*
    /**********************************************************************
    /* Helper class(es)
    /**********************************************************************
     */

    /**
     * This marker class is only to be used with annotations, to
     * indicate that <b>no converter is to be used</b>.
     *<p>
     * Specifically, this class is to be used as the marker for
     * annotation {@link tools.jackson.databind.annotation.JsonSerialize},
     * property <code>converter</code> (and related)
     */
    public abstract static class None
        implements Converter<Object,Object> {
        private None() { }
    }
}
