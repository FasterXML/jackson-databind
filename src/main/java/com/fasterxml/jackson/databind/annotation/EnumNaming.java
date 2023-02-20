package com.fasterxml.jackson.databind.annotation;

import com.fasterxml.jackson.databind.EnumNamingStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@com.fasterxml.jackson.annotation.JacksonAnnotation
public @interface EnumNaming {
    public Class<? extends EnumNamingStrategy> value() default EnumNamingStrategy.class;
}
