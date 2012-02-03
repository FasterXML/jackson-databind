package com.fasterxml.jackson.databind.util;

import java.lang.annotation.Annotation;

/**
 * Interface that defines interface for accessing contents of a
 * collection of annotations. This is needed when introspecting
 * annotation-based features from different kinds of things, not
 * just objects that Java Reflection interface exposes.
 *<p>
 * Standard mutable implementation is {@link com.fasterxml.jackson.databind.introspect.AnnotationMap}
 */
public interface Annotations
{
    /**
     * Main access method used to find value for given annotation.
     */
    public <A extends Annotation> A get(Class<A> cls);

    /**
     * Returns number of annotation entries in this collection.
     */
    public int size();
}
