package com.fasterxml.jackson.databind.type;

/**
 * Set of logical "type classes" for which coercion actions can be defined:
 * this is a more general option for targeting specific concrete types.
 *
 * @since 2.12
 */
public enum CoercionTargetType
{
    // // // General structured types

    /**
     * Array types of other values.
     *<p>
     * Note: excludes binary type {@code byte[]}.
     */
    Array,

    /**
     * {@link java.util.Collection} values (and "Collection-like" for JVM
     * languages and datatype libraries with semantically similar types)
     */
    Collection,

    /**
     * {@link java.util.Map} values (and "Map-like" for JVM
     * languages and datatype libraries with semantically similar types)
     */
    Map,

    // // // Good old Beans / Plain-Old-Java-Objects

    /**
     * Types that are handled by default "set of key/value pairs" serialization,
     * also known as "Beans".
     */
    POJO,

    // // // Basic scalar types

    /**
     * Basic integral numbers types like {@code short}, {@code int}, {@code long}
     * and matching wrapper types, {@link java.math.BigInteger}.
     */
    Integer,

    /**
     * Basic floating-point numbers types like {@code short}, {@code int}, {@code long}
     * and matching wrapper types, {@link java.math.BigInteger}.
     */
    Float,

    /**
     * {@link java.lang.Boolean}, {@code boolean}, {@link java.util.concurrent.atomic.AtomicBoolean}.
     */
    Boolean,

    /**
     * Various {@link java.lang.Enum} types.
     */
    Enum,

    /**
     * Purely textual types, {@link java.lang.String} and similar (but not types that
     * are generally expressed as Strings in input).
     */
    Textual,

    /**
     * Binary data such as {@code byte[]} and {@link java.nio.ByteBuffer}.
     */
    Binary,

    /**
     * Date/time datatypes such as {@link java.util.Date}, {@link java.util.Calendar}.
     */
    DateTime,

    /**
     * Scalar types other than ones listed above: includes types like {@link java.net.URL}
     * and {@link java.util.UUID}.
     */
    OtherScalar
    ;

    /**
     * Helper method to use for figuring out logical type from physical type,
     * in cases where caller wants a guess. Note that introspection is
     * not exhaustive and mostly covers basic {@link java.util.Collection},
     * {@link java.util.Map} and {@link java.lang.Enum} cases; but not
     * more specific types (for example datatype-provided extension types).
     *
     * @param raw Type-erased class to classify
     * @param defaultIfNotRecognized if no type recognized, value to return
     *    (for example, {@code null})
     */
    public static CoercionTargetType fromClass(Class<?> raw,
            CoercionTargetType defaultIfNotRecognized)
    {
        if (raw.isEnum()) {
            return Enum;
        }
        if (raw.isArray()) {
            if (raw == byte[].class) {
                return Binary;
            }
            return Array;
        }
        if (java.util.Collection.class.isAssignableFrom(raw)) {
            return Collection;
        }
        if (java.util.Map.class.isAssignableFrom(raw)) {
            return Map;
        }
        if (raw == String.class) {
            return Textual;
        }
        return defaultIfNotRecognized;
    }
}
