package com.fasterxml.jackson.databind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.util.Converter;

/**
 * Annotation used for configuring serialization aspects, by attaching
 * to "getter" methods or fields, or to value classes.
 * When annotating value classes, configuration is used for instances
 * of the value class but can be overridden by more specific annotations
 * (ones that attach to methods or fields).
 *<p>
 * An example annotation would be:
 *<pre>
 *  &#64;JsonSerialize(using=MySerializer.class,
 *    as=MySubClass.class,
 *    typing=JsonSerialize.Typing.STATIC
 *  )
 *</pre>
 * (which would be redundant, since some properties block others:
 * specifically, 'using' has precedence over 'as', which has precedence
 * over 'typing' setting)
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@com.fasterxml.jackson.annotation.JacksonAnnotation
public @interface JsonSerialize
{
    // // // Annotations for explicitly specifying deserializer

    /**
     * Serializer class to use for
     * serializing associated value. Depending on what is annotated,
     * value is either an instance of annotated class (used globablly
     * anywhere where class serializer is needed); or only used for
     * serializing the value of the property annotated.
     */
    @SuppressWarnings("rawtypes") // to work around JDK8 bug wrt Class-valued annotation properties
    public Class<? extends JsonSerializer> using() default JsonSerializer.None.class;

    /**
     * Serializer class to use for serializing contents (elements
     * of a Collection/array, values of Maps) of annotated property.
     * Can only be used on accessors (methods, fields, constructors), to
     * apply to values of {@link java.util.Map}-valued properties; not
     * applicable for value types used as Array elements
     * or {@link java.util.Collection} and {@link java.util.Map} values.
     */
    @SuppressWarnings("rawtypes") // to work around JDK8 bug wrt Class-valued annotation properties
    public Class<? extends JsonSerializer> contentUsing()
        default JsonSerializer.None.class;

    /**
     * Serializer class to use for deserializing Map keys
     * of annotated property or Map keys of value type so annotated.
     * Can be used both on accessors (methods, fields, constructors), to
     * apply to values of {@link java.util.Map}-valued properties, and
     * on "key" classes, to apply to use of annotated type as
     * {@link java.util.Map} keys (latter starting with Jackson 2.11).
     */
    @SuppressWarnings("rawtypes") // to work around JDK8 bug wrt Class-valued annotation properties
    public Class<? extends JsonSerializer> keyUsing()
        default JsonSerializer.None.class;

    /**
     * Serializer class to use for serializing nulls for properties that
     * are annotated, instead of the
     * default null serializer.
     * Note that using this property when annotation types (classes) has
     * no effect currently (it is possible this could be improved in future).
     *
     * @since 2.3
     */
    @SuppressWarnings("rawtypes") // to work around JDK8 bug wrt Class-valued annotation properties
    public Class<? extends JsonSerializer> nullsUsing()
        default JsonSerializer.None.class;

    // // // Annotations for type handling, explicit declaration
    // // // (type used for choosing deserializer, if not explicitly
    // // // specified)

    /**
     * Supertype (of declared type, which itself is supertype of runtime type)
     * to use as type when locating serializer to use.
     *<p>
     * Bogus type {@link Void} can be used to indicate that declared
     * type is used as is (i.e. this annotation property has no setting);
     * this since annotation properties are not allowed to have null value.
     *<p>
     * Note: if {@link #using} is also used it has precedence
     * (since it directly specifies
     * serializer, whereas this would only be used to locate the
     * serializer)
     * and value of this annotation property is ignored.
     */
    public Class<?> as() default Void.class;

    /**
     * Concrete type to serialize keys of {@link java.util.Map} as,
     * instead of type otherwise declared.
     * Must be a supertype of declared type; otherwise an exception may be
     * thrown by serializer.
     */
    public Class<?> keyAs() default Void.class;

    /**
     * Concrete type to serialize content value (elements
     * of a Collection/array, values of Maps) as,
     * instead of type otherwise declared.
     * Must be a supertype of declared type; otherwise an exception may be
     * thrown by serializer.
     */
    public Class<?> contentAs() default Void.class;

    /**
     * Whether type detection used is dynamic or static: that is,
     * whether actual runtime type is used (dynamic), or just the
     * declared type (static).
     *<p>
     * Note that Jackson 2.3 changed default to <code>DEFAULT_TYPING</code>,
     * which is roughly same as saying "whatever".
     * This is important as it allows avoiding accidental overrides
     * at property level.
     */
    public Typing typing() default Typing.DEFAULT_TYPING;

    // // // Annotations for specifying intermediate Converters (2.2+)

    /**
     * Which helper object is to be used to convert type into something
     * that Jackson knows how to serialize; either because base type
     * cannot be serialized easily, or just to alter serialization.
     *
     * @since 2.2
     */
    @SuppressWarnings("rawtypes") // to work around JDK8 bug wrt Class-valued annotation properties
    public Class<? extends Converter> converter() default Converter.None.class;

    /**
     * Similar to {@link #converter}, but used for values of structures types
     * (List, arrays, Maps).
     * Note that this property does NOT have effect when used as Class annotation;
     * it can only be used as property annotation: this because association between
     * container and value types is loose and as such converters seldom make sense
     * for such usage.
     *
     * @since 2.2
     */
    @SuppressWarnings("rawtypes") // to work around JDK8 bug wrt Class-valued annotation properties
    public Class<? extends Converter> contentConverter() default Converter.None.class;

    // // // Annotation(s) for inclusion criteria

    /**
     * Which properties of annotated Bean are
     * to be included in serialization (has no effect on other types
     * like enums, primitives or collections).
     * Choices are "all", "properties that have value other than null"
     * and "properties that have non-default value" (i.e. default value
     * being property setting for a Bean constructed with default no-arg
     * constructor, often null).
     *<p>
     * This property has been replaced by special-purpose {@link com.fasterxml.jackson.annotation.JsonInclude}
     * annotation, introduced in Jackson 2.0.
     *<p>
     * Note that Jackson 2.3 changed default to <code>DEFAULT_INCLUSION</code>,
     * which is roughly same as saying "whatever". This is important because
     * it allows hierarchic default values to be used.
     *
     * @deprecated As of Jackson 2.0, this annotation has been replaced
     *    by {@link com.fasterxml.jackson.annotation.JsonInclude}
     */
    @Deprecated
    public Inclusion include() default Inclusion.DEFAULT_INCLUSION;

    /*
    /**********************************************************
    /* Value enumerations needed
    /**********************************************************
     */

    /**
     * Enumeration used with {@link JsonSerialize#include} property
     * to define which properties
     * of Java Beans are to be included in serialization
     */
    @Deprecated // since 2.0, marked deprecated in 2.6
    public enum Inclusion
    {
        /**
         * Value that indicates that properties are to be always included,
         * independent of value
         */
        ALWAYS,

        /**
         * Value that indicates that only properties with non-null
         * values are to be included.
         */
        NON_NULL,

        /**
         * Value that indicates that only properties that have values
         * that differ from default settings (meaning values they have
         * when Bean is constructed with its no-arguments constructor)
         * are to be included. Value is generally not useful with
         * {@link java.util.Map}s, since they have no default values;
         * and if used, works same as {@link #ALWAYS}.
         */
        NON_DEFAULT,

        /**
         * Value that indicates that only properties that have values
         * that values that are null or what is considered empty are
         * not to be included.
         * Emptiness is defined for following type:
         *<ul>
         * <li>For {@link java.util.Collection}s and {@link java.util.Map}s,
         *    method <code>isEmpty()</code> is called;
         *   </li>
         * <li>For Java arrays, empty arrays are ones with length of 0
         *   </li>
         * <li>For Java {@link java.lang.String}s, <code>length()</code> is called,
         *   and return value of 0 indicates empty String
         *   </li>
         * </ul>
         *  For other types, non-null values are to be included.
         */
        NON_EMPTY,

        /**
         * Pseudo-value that is used to indicate
         * "use whatever is default used at higher level".
         *
         * @since 2.3
         */
        DEFAULT_INCLUSION
        ;
    }

    /**
     * Enumeration used with {@link JsonSerialize#typing} property
     * to define whether type detection is based on dynamic runtime
     * type (DYNAMIC) or declared type (STATIC).
     */
    public enum Typing
    {
        /**
         * Value that indicates that the actual dynamic runtime type is to
         * be used.
         */
        DYNAMIC,

        /**
         * Value that indicates that the static declared type is to
         * be used.
         */
        STATIC,

        /**
         * Pseudo-value that is used to indicate
         * "use whatever is default used at higher level".
         *
         * @since 2.3
         */
        DEFAULT_TYPING
        ;
    }
}
