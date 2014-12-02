package com.fasterxml.jackson.databind.annotation;

import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;

/**
 * Annotation that may be used to add "virtual" properties to be written
 * after regular properties (although ordering may be changed using
 * both standard <code>@JsonPropertyOrder</code> annotation, and
 * properties of this annotation).
 * 
 * @since 2.5
 */
public @interface JsonAppend
{
    public String[] attrs() default { };

    public @interface Prop
    {
        public Class<? extends VirtualBeanPropertyWriter> value();
    }
}
