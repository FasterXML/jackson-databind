package com.fasterxml.jackson.databind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.util.Converter;

/**
 * Annotation use for configuring deserialization aspects, by attaching
 * to "setter" methods or fields, or to value classes.
 * When annotating value classes, configuration is used for instances
 * of the value class but can be overridden by more specific annotations
 * (ones that attach to methods or fields).
 *<p>
 * An example annotation would be:
 *<pre>
 *  &#64;JsonDeserialize(using=MySerializer.class,
 *    as=MyHashMap.class,
 *    keyAs=MyHashKey.class,
 *    contentAs=MyHashValue.class
 *  )
 *</pre>
 *<p>
 * Something to note on usage:
 *<ul>
 * <li>All other annotations regarding behavior during building should be on <b>Builder</b>
 *    class and NOT on target POJO class: for example &#64;JsonIgnoreProperties should be on
 *    Builder to prevent "unknown property" errors.
 *  </li>
 * <li>Similarly configuration overrides (see {@link com.fasterxml.jackson.databind.ObjectMapper#configOverride})
 *    should be targeted at Builder class, not target POJO class.
 *  </li>
 * </ul>
 *
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@com.fasterxml.jackson.annotation.JacksonAnnotation
public @interface JsonDeserialize
{
    // // // Annotations for explicitly specifying deserialize/builder

    /**
     * Deserializer class to use for deserializing associated value.
     * Depending on what is annotated,
     * value is either an instance of annotated class (used globablly
     * anywhere where class deserializer is needed); or only used for
     * deserializing the value of the property annotated.
     */
    @SuppressWarnings("rawtypes") // to work around JDK8 bug wrt Class-valued annotation properties
    public Class<? extends JsonDeserializer> using()
        default JsonDeserializer.None.class;

    /**
     * Deserializer class to use for deserializing contents (elements
     * of a Collection/array, values of Maps) of annotated property.
     * Can only be used on accessors (methods, fields, constructors), to
     * apply to values of {@link java.util.Map}-valued properties; not
     * applicable for value types used as Array elements
     * or {@link java.util.Collection} and {@link java.util.Map} values.
     */
    @SuppressWarnings("rawtypes") // to work around JDK8 bug wrt Class-valued annotation properties
    public Class<? extends JsonDeserializer> contentUsing()
        default JsonDeserializer.None.class;

    /**
     * Deserializer class to use for deserializing Map keys
     * of annotated property or Map keys of value type so annotated.
     * Can be used both on accessors (methods, fields, constructors), to
     * apply to values of {@link java.util.Map}-valued properties, and
     * on "key" classes, to apply to use of annotated type as
     * {@link java.util.Map} keys (latter starting with Jackson 2.11).
     */
    public Class<? extends KeyDeserializer> keyUsing()
        default KeyDeserializer.None.class;

    /**
     * Annotation for specifying if an external Builder class is to
     * be used for building up deserialized instances of annotated
     * class. If so, an instance of referenced class is first constructed
     * (possibly using a Creator method; or if none defined, using default
     * constructor), and its "with-methods" are used for populating fields;
     * and finally "build-method" is invoked to complete deserialization.
     */
    public Class<?> builder() default Void.class;

    // // // Annotations for specifying intermediate Converters (2.2+)

    /**
     * Which helper object (if any) is to be used to convert from Jackson-bound
     * intermediate type (source type of converter) into actual property type
     * (which must be same as result type of converter). This is often used
     * for two-step deserialization; Jackson binds data into suitable intermediate
     * type (like Tree representation), and converter then builds actual property
     * type.
     *
     * @since 2.2
     */
    @SuppressWarnings("rawtypes") // to work around JDK8 bug wrt Class-valued annotation properties
    public Class<? extends Converter> converter() default Converter.None.class;

    /**
     * Similar to {@link #converter}, but used for values of structures types
     * (List, arrays, Maps).
     *
     * @since 2.2
     */
    @SuppressWarnings("rawtypes") // to work around JDK8 bug wrt Class-valued annotation properties
    public Class<? extends Converter> contentConverter() default Converter.None.class;


    // // // Annotations for explicitly specifying deserialization type
    // // // (which is used for choosing deserializer, if not explicitly
    // // // specified

    /**
     * Concrete type to deserialize values as, instead of type otherwise
     * declared. Must be a subtype of declared type; otherwise an
     * exception may be thrown by deserializer.
     *<p>
     * Bogus type {@link Void} can be used to indicate that declared
     * type is used as is (i.e. this annotation property has no setting);
     * this since annotation properties are not allowed to have null value.
     *<p>
     * Note: if {@link #using} is also used it has precedence
     * (since it directly specified
     * deserializer, whereas this would only be used to locate the
     * deserializer)
     * and value of this annotation property is ignored.
     */
    public Class<?> as() default Void.class;

    /**
     * Concrete type to deserialize keys of {@link java.util.Map} as,
     * instead of type otherwise declared.
     * Must be a subtype of declared type; otherwise an exception may be
     * thrown by deserializer.
     */
    public Class<?> keyAs() default Void.class;

    /**
     * Concrete type to deserialize content (elements
     * of a Collection/array, values of Maps) values as,
     * instead of type otherwise declared.
     * Must be a subtype of declared type; otherwise an exception may be
     * thrown by deserializer.
     */
    public Class<?> contentAs() default Void.class;
}
