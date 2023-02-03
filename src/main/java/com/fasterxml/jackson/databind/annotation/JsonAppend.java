package com.fasterxml.jackson.databind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;

/**
 * Annotation that may be used to add "virtual" properties to be written
 * after regular properties (although ordering may be changed using
 * both standard <code>@JsonPropertyOrder</code> annotation, and
 * properties of this annotation).
 *
 * @since 2.5
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@com.fasterxml.jackson.annotation.JacksonAnnotation
public @interface JsonAppend
{
    /**
     * Set of attribute-backed properties to include when serializing
     * a POJO.
     */
    public Attr[] attrs() default { };

    /**
     * Set of general virtual properties to include when serializing a POJO.
     */
    public Prop[] props() default { };

    /**
     * Indicator used to determine whether properties defined are to be
     * appended after (false) or prepended before (true) regular properties.
     * Affects all kinds of properties defined using this annotation.
     */
    public boolean prepend() default false;

    /**
     * Definition of a single attribute-backed property.
     * Attribute-backed properties will be appended after (or prepended before,
     * as per {@link #prepend}) regular properties
     * in specified order, although their placement may be further changed
     * by the usual property-ordering functionality (alphabetic sorting;
     * explicit ordering)
     */
    public @interface Attr
    {
        /**
         * Name of attribute of which value to serialize. Is also used as the
         * name of external property to write, unless overridden by
         * assigning a value for {@link #propName()}.
         */
        public String value();

        /**
         * Name to use for serializing value of the attribute; if not defined,
         * {@link #value} will be used instead.
         */
        public String propName() default "";

        /**
         * Optional namespace to use; only relevant for data formats that use
         * namespaces (like XML).
         */
        public String propNamespace() default "";

        /**
         * When to include attribute-property. Default value indicates that
         * property should only be written if specified attribute has a non-null
         * value.
         */
        public JsonInclude.Include include() default JsonInclude.Include.NON_NULL;

        /**
         * Metadata about property, similar to
         * {@link com.fasterxml.jackson.annotation.JsonProperty#required()}.
         */
        public boolean required() default false;
    }

    /**
     * Definition of a single general virtual property.
     */
    public @interface Prop
    {
        /**
         * Actual implementation class (a subtype of {@link VirtualBeanPropertyWriter})
         * of the property to instantiate (using the no-argument default constructor).
         */
        public Class<? extends VirtualBeanPropertyWriter> value();

        /**
         * Name of the property to possibly use for serializing (although implementation
         * may choose to not use this information).
         */
        public String name() default "";

        /**
         * Optional namespace to use along with {@link #name};
         * only relevant for data formats that use namespaces (like XML).
         */
        public String namespace() default "";

        /**
         * When to include  value of the property. Default value indicates that
         * property should only be written if specified attribute has a non-null
         * value. As with other properties, actual property implementation may or may
         * not choose to use this inclusion information.
         */
        public JsonInclude.Include include() default JsonInclude.Include.NON_NULL;

        /**
         * Metadata about property, similar to
         * {@link com.fasterxml.jackson.annotation.JsonProperty#required()}.
         */
        public boolean required() default false;

        /**
         * Nominal type of the property. Passed as type information for related
         * virtual objects, and may (or may not be) used by implementation
         * for choosing serializer to use.
         */
        public Class<?> type() default Object.class;
    }
}
