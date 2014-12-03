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
     * Definition of a single attribute-backed property.
     * Attribute-backed properties will be appended after regular properties
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
        public Class<? extends VirtualBeanPropertyWriter> value();
    }
}
