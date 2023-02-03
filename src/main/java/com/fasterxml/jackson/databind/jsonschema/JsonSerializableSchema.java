package com.fasterxml.jackson.databind.jsonschema;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotation;

/**
 * Annotation that can be used to define JSON Schema definition for
 * the annotated class.
 *<p>
 * Note that annotation is often not needed: for example, regular
 * Jackson beans that Jackson can introspect can be used without
 * annotations, to produce JSON schema definition.
 *
 * @author Ryan Heaton
 * @author Tatu Saloranta
 * @deprecated Since 2.15, we recommend use of external
 * <a href="https://github.com/FasterXML/jackson-module-jsonSchema">JSON Schema generator module</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
@Deprecated
public @interface JsonSerializableSchema
{
    /**
     * Marker value used to indicate that property has "no value";
     * needed because annotations cannot have null as default
     * value.
     */
    public final static String NO_VALUE = "##irrelevant";

    /**
     * Property that can be used to indicate id of the type when
     * generating JSON Schema; empty String indicates that no id
     * is defined.
     */
    public String id() default "";

    /**
     * The schema type for this JsonSerializable instance.
     * Possible values: "string", "number", "boolean", "object", "array", "null", "any"
     *
     * @return The schema type for this JsonSerializable instance.
     */
    public String schemaType() default "any";

    /**
     * If the schema type is "object", JSON definition of properties of the object as
     * a String.
     *
     * @return The node representing the schema properties, or "##irrelevant" if irrelevant.
     *
     * @deprecated (since 2.1) -- support will be dropped in future, since JSON-as-String is
     *   fundamentally bad way for customizing anything. No direct replacements offered.
     */
    @Deprecated
    public String schemaObjectPropertiesDefinition() default NO_VALUE;

    /**
     * If the schema type if "array", JSON definition of the schema for item types contained.
     *
     * @return The schema for the items in the array, or "##irrelevant" if irrelevant.
     *
     * @deprecated (since 2.1) -- support will be dropped in future, since JSON-as-String is
     *   fundamentally bad way for customizing anything. No direct replacements offered.
     */
    @Deprecated
    public String schemaItemDefinition() default NO_VALUE;
}
