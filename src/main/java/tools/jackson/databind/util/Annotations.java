package tools.jackson.databind.util;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

/**
 * Interface that defines interface for accessing contents of a
 * collection of annotations. This is needed when introspecting
 * annotation-based features from different kinds of things, not
 * just objects that Java Reflection interface exposes.
 *<p>
 * Standard mutable implementation is {@link tools.jackson.databind.introspect.AnnotationMap}
 */
public interface Annotations
{
    /**
     * Main access method used to find value for given annotation.
     */
    public <A extends Annotation> A get(Class<A> cls);

    /**
     * Access method that returns a stream of all annotations contained.
     *
     * @since 3.0
     */
    public abstract Stream<Annotation> values();
    
    public boolean has(Class<? extends Annotation> cls);

    public boolean hasOneOf(Class<? extends Annotation>[] annoClasses);

    /**
     * Returns number of annotation entries in this collection.
     */
    public int size();
}
