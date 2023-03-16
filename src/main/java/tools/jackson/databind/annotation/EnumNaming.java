package tools.jackson.databind.annotation;

import tools.jackson.databind.EnumNamingStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to indicate a {@link EnumNamingStrategy}
 * to use for annotated class.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@com.fasterxml.jackson.annotation.JacksonAnnotation
public @interface EnumNaming {

    /**
     * @return Type of {@link EnumNamingStrategy} to use, if any. Default value
     * of <code>EnumNamingStrategy.class</code> means "no strategy specified"
     * (and may also be used for overriding to remove otherwise applicable
     * naming strategy)
     */
    public Class<? extends EnumNamingStrategy> value();
}
