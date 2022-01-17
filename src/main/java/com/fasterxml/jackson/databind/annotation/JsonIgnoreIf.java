package com.fasterxml.jackson.databind.annotation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.deser.JsonIgnoreValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation that indicates that the logical property that
 * the accessor (field, getter/setter method or Creator parameter
 * [of {@link JsonCreator}-annotated constructor or factory method])
 * is to be ignored by introspection-based
 * serialization and deserialization functionality in case the customized
 * check of {@link JsonIgnoreValidator}
 * returned false.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@com.fasterxml.jackson.annotation.JacksonAnnotation
public @interface JsonIgnoreIf
{

    /**
     * Required Argument that defines the class exstending the {@link JsonIgnoreValidator}
     * which is used to check if the property can be ignored.
     */
    public Class<? extends JsonIgnoreValidator> value();

}