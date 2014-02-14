package com.fasterxml.jackson.databind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


import com.fasterxml.jackson.annotation.JacksonAnnotation;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

/**
 * Annotation that can be used to plug a custom type identifier handler
 * ({@link TypeIdResolver})
 * to be used by
 * {@link com.fasterxml.jackson.databind.jsontype.TypeSerializer}s
 * and {@link com.fasterxml.jackson.databind.jsontype.TypeDeserializer}s
 * for converting between java types and type id included in JSON content.
 * In simplest cases this can be a simple class with static mapping between
 * type names and matching classes.
 *<p>
 * NOTE: since 2.4, applicable to properties as well (should have been long time
 *  ago, but problem only found then)
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonTypeIdResolver
{
    /**
     * Defines implementation class of {@link TypeIdResolver} to use for
     * converting between external type id (type name) and actual
     * type of object.
     */
    public Class<? extends TypeIdResolver> value();
}
