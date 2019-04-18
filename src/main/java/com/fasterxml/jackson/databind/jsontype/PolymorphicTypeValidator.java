package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;

/**
 * Interface for classes that handle validation of class name-based subtypes used
 * with Polymorphic Deserialization: both via "default typing" and explicit
 * {@code @JsonTypeInfo} when using class name as Type Identifier.
 * The main purpose, initially, is to allow pluggable allow/deny lists to avoid
 * security problems that occur with unlimited class names
 * (See <a href="https://medium.com/@cowtowncoder/on-jackson-cves-dont-panic-here-is-what-you-need-to-know-54cd0d6e8062">
 * this article</a> for full explanation).
 *<p>
 * Notes on implementations: implementations must be thread-safe and shareable (usually meaning they
 * are stateless). Determinations for validity are usually effectively cached on per-property
 * basis (by virtue of subtype deserializers being cached by polymorphic deserializers) so
 * caching at validator level is usually not needed. If caching is used, however, it must be done
 * in thread-safe manner as validators are shared within {@link ObjectMapper} as well as possible
 * across mappers (in case of default/standard validator).
 *
 * @since 2.10
 */
public abstract class PolymorphicTypeValidator
{
    /**
     * Definition of return values to indicate determination regarding validity.
     */
    public enum Validity {
        /**
         * Value that indicates that Class name or Class is allowed for use without further checking
         */
        ALLOWED,
        /**
         * Value that indicates that Class name or Class is NOT allowed and no further checks are
         * needed or allowed
         */
        DENIED,

        /**
         * Value that indicates that Class name or Class validity can not be confirmed by validator
         * and further checks are needed.
         *<p>
         * Typically if validator can not establish validity from Type Id or Class (name), eventual
         * determination will be {@code DENIED}, for safety reasons.
         */
        INDETERMINATE
        ;
    }

    /**
     * Method called after intended class name for subtype has been read (and in case of minimal
     * class name, expanded to fully-qualified class name) but before attempt is made to
     * look up actual {@link java.lang.Class} or {@link JavaType}.
     * Validator may be able to
     * determine validity of eventual type (and return {@link Validity#ALLOWED} or
     * {@link Validity#DENIED}) or, if not able to, can defer validation to actual
     * resolved type by returning {@link Validity#INDETERMINATE}.
     *<p>
     * Validator may also choose to indicate denial by throwing a {@link JsonMappingException}
     * (such as {@link com.fasterxml.jackson.databind.exc.InvalidTypeIdException})
     *
     * @param ctxt Context for resolution: typically will be {@code DeserializationContext}
     * @param baseType Nominal base type used for polymorphic handling: subtypes MUST be instances
     *   of this type and assignment compatibility is verified by Jackson core
     * @param subClassName Name of class that will be resolved to {@link java.lang.Class} if
     *   (and only if) validity check is not denied.
     *
     * @return Determination of validity of given class name, as a subtype of given base type:
     *   should NOT return {@code null}
     */
    public abstract Validity validateSubClassName(MapperConfig<?> ctxt, JavaType baseType,
            String subClassName) throws JsonMappingException;

    /**
     * Method called after class name has been resolved to actual type, in cases where previous
     * call to {@link #validateSubClassName} returned {@link Validity#INDETERMINATE}.
     * Validator should be able to determine validity and return appropriate {@link Validity}
     * value, although it may also
     *<p>
     * Validator may also choose to indicate denial by throwing a {@link JsonMappingException}
     * (such as {@link com.fasterxml.jackson.databind.exc.InvalidTypeIdException})
     *
     * @param ctxt Context for resolution: typically will be {@code DeserializationContext}
     * @param baseType Nominal base type used for polymorphic handling: subtypes MUST be instances
     *   of this type and assignment compatibility has been verified by Jackson core
     * @param subType Resolved subtype to validate
     *
     * @return Determination of validity of given class name, as a subtype of given base type:
     *   should NOT return {@code null}
     */
    public abstract Validity validateSubType(MapperConfig<?> ctxt, JavaType baseType,
            JavaType subType) throws JsonMappingException;
}
