package com.fasterxml.jackson.databind.introspect;

import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Intermediate base class that encapsulates features that
 * constructors and methods share.
 */
public abstract class AnnotatedWithParams
    extends AnnotatedMember
{
    private static final long serialVersionUID = 1L;

    /**
     * Annotations associated with parameters of the annotated
     * entity (method or constructor parameters)
     */
    protected final AnnotationMap[] _paramAnnotations;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected AnnotatedWithParams(TypeResolutionContext ctxt, AnnotationMap annotations, AnnotationMap[] paramAnnotations)
    {
        super(ctxt, annotations);
        _paramAnnotations = paramAnnotations;
    }

    protected AnnotatedWithParams(AnnotatedWithParams base, AnnotationMap[] paramAnnotations) {
        super(base);
        _paramAnnotations = paramAnnotations;
    }

    /**
     * Method called to override a method parameter annotation,
     * usually due to a mix-in
     * annotation masking or overriding an annotation 'real' method
     * has.
     *
    @Deprecated // since 3.0
    public final void addOrOverrideParam(int paramIndex, Annotation a)
    {
        AnnotationMap old = _paramAnnotations[paramIndex];
        if (old == null) {
            old = new AnnotationMap();
            _paramAnnotations[paramIndex] = old;
        }
        old.add(a);
    }
    */

    /**
     * Method called by parameter object when an augmented instance is created;
     * needs to replace parameter with new instance
     */
    protected AnnotatedParameter replaceParameterAnnotations(int index, AnnotationMap ann)
    {
        _paramAnnotations[index] = ann;
        return getParameter(index);
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    public final AnnotationMap getParameterAnnotations(int index)
    {
        if (_paramAnnotations != null) {
            if (index >= 0 && index < _paramAnnotations.length) {
                return _paramAnnotations[index];
            }
        }
        return null;
    }

    public final AnnotatedParameter getParameter(int index) {
        return new AnnotatedParameter(this, getParameterType(index),
                _typeContext, getParameterAnnotations(index), index);
    }

    public abstract int getParameterCount();
    public abstract Class<?> getRawParameterType(int index);
    public abstract JavaType getParameterType(int index);

    /**
     * @since 3.0
     */
    public abstract Parameter[] getNativeParameters();

    /**
     * @since 3.0
     */
    public boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }

    public final int getAnnotationCount() { return _annotations.size(); }

    /**
     * Method that can be used to (try to) call this object without arguments.
     * This may succeed or fail, depending on expected number
     * of arguments: caller needs to take care to pass correct number.
     * Exceptions are thrown directly from actual low-level call.
     *<p>
     * Note: only works for constructors and static methods.
     */
    public abstract Object call() throws Exception;

    /**
     * Method that can be used to (try to) call this object with specified arguments.
     * This may succeed or fail, depending on expected number
     * of arguments: caller needs to take care to pass correct number.
     * Exceptions are thrown directly from actual low-level call.
     *<p>
     * Note: only works for constructors and static methods.
     */
    public abstract Object call(Object[] args) throws Exception;

    /**
     * Method that can be used to (try to) call this object with single arguments.
     * This may succeed or fail, depending on expected number
     * of arguments: caller needs to take care to pass correct number.
     * Exceptions are thrown directly from actual low-level call.
     *<p>
     * Note: only works for constructors and static methods.
     */
    public abstract Object call1(Object arg) throws Exception;
}
