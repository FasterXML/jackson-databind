package com.fasterxml.jackson.databind.ext;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;

/**
 * To support Java7-incomplete platforms, we will offer support for JDK 7
 * annotations through this class, loaded dynamically; if loading fails,
 * support will be missing. This class is the non-JDK-7-dependent API,
 * and {@link Java7SupportImpl} is JDK7-dependent implementation of
 * functionality.
 */
public abstract class Java7Support
{
    private final static Java7Support IMPL = new Java7SupportImpl();

    public static Java7Support instance() {
        return IMPL;
    }
    
    public abstract Boolean findTransient(Annotated a);

    public abstract Boolean hasCreatorAnnotation(Annotated a);

    public abstract PropertyName findConstructorName(AnnotatedParameter p);
}
