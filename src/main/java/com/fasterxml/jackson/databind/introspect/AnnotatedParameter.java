package com.fasterxml.jackson.databind.introspect;

import java.lang.reflect.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Object that represents method parameters, mostly so that associated
 * annotations can be processed conveniently. Note that many of accessors
 * cannot return meaningful values since parameters do not have stand-alone
 * JDK objects associated; so access should mostly be limited to checking
 * annotation values which are properly aggregated and included.
 */
public final class AnnotatedParameter
    extends AnnotatedMember
{
    private static final long serialVersionUID = 1L;

    /**
     * Member (method, constructor) that this parameter belongs to
     */
    protected final AnnotatedWithParams _owner;

    /**
     * JDK type of the parameter, possibly contains generic type information
     */
    protected final JavaType _type;

    /**
     * Index of the parameter within argument list
     */
    protected final int _index;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public AnnotatedParameter(AnnotatedWithParams owner, JavaType type,
            TypeResolutionContext typeContext,
            AnnotationMap annotations, int index)
    {
        super(typeContext, annotations);
        _owner = owner;
        _type = type;
        _index = index;
    }

    @Override
    public AnnotatedParameter withAnnotations(AnnotationMap ann) {
        if (ann == _annotations) {
            return this;
        }
        return _owner.replaceParameterAnnotations(_index, ann);
    }

    /*
    /**********************************************************
    /* Annotated impl
    /**********************************************************
     */

    /**
     * Since there is no matching JDK element, this method will
     * always return null
     */
    @Override
    public AnnotatedElement getAnnotated() { return null; }

    /**
     * Returns modifiers of the constructor, as parameters do not
     * have independent modifiers.
     */
    @Override
    public int getModifiers() { return _owner.getModifiers(); }

    /**
     * Parameters have no names in bytecode (unlike in source code),
     * will always return empty String ("").
     */
    @Override
    public String getName() { return ""; }

    @Override
    public Class<?> getRawType() {
        return _type.getRawClass();
    }

    @Override
    public JavaType getType() {
        return _type;
    }

    /*
    /**********************************************************
    /* AnnotatedMember extras
    /**********************************************************
     */

    @Override
    public Class<?> getDeclaringClass() {
        return _owner.getDeclaringClass();
    }

    @Override
    public Member getMember() {
        // This is bit tricky: since there is no JDK equivalent; can either
        // return null or owner... let's do latter, for now.
        return _owner.getMember();
    }

    @Override
    public void setValue(Object pojo, Object value) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Cannot call setValue() on constructor parameter of "
                +getDeclaringClass().getName());
    }

    @Override
    public Object getValue(Object pojo) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Cannot call getValue() on constructor parameter of "
                +getDeclaringClass().getName());
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    public Type getParameterType() { return _type; }

    /**
     * Accessor for 'owner' of this parameter; method or constructor that
     * has this parameter as member of its argument list.
     *
     * @return Owner (member or creator) object of this parameter
     */
    public AnnotatedWithParams getOwner() { return _owner; }

    /**
     * Accessor for index of this parameter within argument list
     *
     * @return Index of this parameter within argument list
     */
    public int getIndex() { return _index; }

    /*
    /********************************************************
    /* Other
    /********************************************************
     */

    @Override
    public int hashCode() {
        return _owner.hashCode() + _index;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!ClassUtil.hasClass(o, getClass())) {
            return false;
        }
        AnnotatedParameter other = (AnnotatedParameter) o;
        return other._owner.equals(_owner) && (other._index == _index);
    }

    @Override
    public String toString() {
        return "[parameter #"+getIndex()+", annotations: "+_annotations+"]";
    }
}

