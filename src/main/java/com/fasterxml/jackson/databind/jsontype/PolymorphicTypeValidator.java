package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;

/**
 * Interface for classes that handle validation of class-name - based subtypes used
 * with Polymorphic Deserialization: both via "default typing" and explicit
 * {@code @JsonTypeInfo} when using Java Class name as Type Identifier.
 * The main purpose, initially, is to allow pluggable allow lists to avoid
 * security problems that occur with unlimited class names
 * (See <a href="https://medium.com/@cowtowncoder/on-jackson-cves-dont-panic-here-is-what-you-need-to-know-54cd0d6e8062">
 * this article</a> for full explanation).
 *<p>
 * Calls to methods are done as follows:
 * <ol>
 *  <li>When a deserializer is needed for a polymorphic property (including root values) -- either
 *     for explicitly annotated polymorphic type, or "default typing" -- {@link #validateBaseType}
 *     is called to see if validity can be determined for all possible types: if
 *     {@link Validity#ALLOWED} is returned no futher checks are made for any subtypes; of
 *     {@link Validity#DENIED} is returned, an exception will be thrown to indicate invalid polymorphic
 *     property
 *   </li>
 *  <li>If neither deny nor allowed was returned for property with specific base type, first time
 *     specific Type Id (Class Name) is encountered, method {@link #validateSubClassName} is called
 *     with resolved class name: it may indicate allowed/denied, resulting in either allowed use or
 *     denial with exception
 *   </li>
 *  <li>If no denial/allowance indicated, class name is resolved to actual {@link Class}, and
 *  {@link #validateSubType(MapperConfig, JavaType, JavaType)} is called: if
 *  {@link Validity#ALLOWED} is returned, usage is accepted; otherwise (denied or indeterminate)
 *  usage is not allowed and exception is thrown
 *   </li>
 * </ol>
 *<p>
 * Notes on implementations: implementations must be thread-safe and shareable (usually meaning they
 * are stateless). Determinations for validity are usually effectively cached on per-property
 * basis (by virtue of subtype deserializers being cached by polymorphic deserializers) so
 * caching at validator level is usually not needed. If caching is used, however, it must be done
 * in thread-safe manner as validators are shared within {@link ObjectMapper} as well as possible
 * across mappers (in case of default/standard validator).
 *<p>
 * Also note that it is strongly recommended that all implementations are based on provided
 * abstract base class, {@link PolymorphicTypeValidator.Base} which contains helper methods
 * and default implementations for returning {@link Validity#INDETERMINATE} for validation
 * methods (to allow only overriding relevant methods implementation cares about)
 *
 * @since 2.10
 */
public abstract class PolymorphicTypeValidator
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

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
     * Method called when a property with polymorphic value is encountered, and a
     * {@code TypeResolverBuilder} is needed. Intent is to allow early determination
     * of cases where subtyping is completely denied (for example for security reasons),
     * or, conversely, allowed for allow subtypes (when base type guarantees that all subtypes
     * are known to be safe). Check can be thought of as both optimization (for latter case)
     * and eager-fail (for former case) to give better feedback.
     *
     * @param config Configuration for resolution: typically will be {@code DeserializationConfig}
     * @param baseType Nominal base type used for polymorphic handling: subtypes MUST be instances
     *   of this type and assignment compatibility is verified by Jackson core
     *
     * @return Determination of general validity of all subtypes of given base type; if
     *    {@link Validity#ALLOWED} returned, all subtypes will automatically be accepted without
     *    further checks; is {@link Validity#DENIED} returned no subtyping allowed at all
     *    (caller will usually throw an exception); otherwise (return {@link Validity#INDETERMINATE})
     *    per sub-type validation calls are made for each new subclass encountered.
     */
    public abstract Validity validateBaseType(MapperConfig<?> config, JavaType baseType);

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
     * @param config Configuration for resolution: typically will be {@code DeserializationConfig}
     * @param baseType Nominal base type used for polymorphic handling: subtypes MUST be instances
     *   of this type and assignment compatibility is verified by Jackson core
     * @param subClassName Name of class that will be resolved to {@link java.lang.Class} if
     *   (and only if) validity check is not denied.
     *
     * @return Determination of validity of given class name, as a subtype of given base type:
     *   should NOT return {@code null}
     */
    public abstract Validity validateSubClassName(MapperConfig<?> config, JavaType baseType,
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
     * @param config Configuration for resolution: typically will be {@code DeserializationConfig}
     * @param baseType Nominal base type used for polymorphic handling: subtypes MUST be instances
     *   of this type and assignment compatibility has been verified by Jackson core
     * @param subType Resolved subtype to validate
     *
     * @return Determination of validity of given class name, as a subtype of given base type:
     *   should NOT return {@code null}
     */
    public abstract Validity validateSubType(MapperConfig<?> config, JavaType baseType,
            JavaType subType) throws JsonMappingException;

    /**
     * Shared base class with partial implementation (with all validation calls returning
     * {@link Validity#INDETERMINATE}) and convenience methods for indicating failure reasons.
     * Use of this base class is strongly recommended over directly implement
     */
    public abstract static class Base
        extends PolymorphicTypeValidator
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public Validity validateBaseType(MapperConfig<?> config, JavaType baseType) {
            return Validity.INDETERMINATE;
        }

        @Override
        public Validity validateSubClassName(MapperConfig<?> config, JavaType baseType, String subClassName)
                throws JsonMappingException {
            return Validity.INDETERMINATE;
        }

        @Override
        public Validity validateSubType(MapperConfig<?> config, JavaType baseType, JavaType subType)
                throws JsonMappingException {
            return Validity.INDETERMINATE;
        }
    }
}
