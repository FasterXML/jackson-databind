package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Shared base class used for anything on which annotations (included
 * within a {@link AnnotationMap}).
 */
public abstract class Annotated
{
    protected Annotated() { }

    public abstract <A extends Annotation> A getAnnotation(Class<A> acls);

    public abstract boolean hasAnnotation(Class<?> acls);

    /**
     * @since 2.7
     */
    public abstract boolean hasOneOf(Class<? extends Annotation>[] annoClasses);

    /**
     * Method that can be used to find actual JDK element that this instance
     * represents. It is non-null, except for method/constructor parameters
     * which do not have a JDK counterpart.
     */
    public abstract AnnotatedElement getAnnotated();

    protected abstract int getModifiers();

    public boolean isPublic() {
        return Modifier.isPublic(getModifiers());
    }

    public abstract String getName();

    /**
     * Full generic type of the annotated element; definition
     * of what exactly this means depends on sub-class.
     *
     * @since 2.7
     */
    public abstract JavaType getType();

    /**
     * "Raw" type (type-erased class) of the annotated element; definition
     * of what exactly this means depends on sub-class.
     */
    public abstract Class<?> getRawType();

    /**
     * Accessor that can be used to iterate over all the annotations
     * associated with annotated component.
     *
     * @since 2.3
     * @deprecated Since 2.9 should instead use {@link #getAnnotated()}
     */
    @Deprecated
    public abstract Iterable<Annotation> annotations();

    // Also: ensure we can use #equals, #hashCode

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
