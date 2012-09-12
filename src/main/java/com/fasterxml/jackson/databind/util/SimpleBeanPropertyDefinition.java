package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

/**
 * Simple immutable {@link BeanPropertyDefinition} implementation that can
 * be wrapped around a {@link AnnotatedMember} that is a simple
 * accessor (getter) or mutator (setter, constructor parameter)
 * (or both, for fields).
 */
public class SimpleBeanPropertyDefinition
    extends BeanPropertyDefinition
{
    /**
     * Member that defines logical property. Assumption is that it
     * should be a 'simple' accessor; meaning a zero-argument getter,
     * single-argument setter or constructor parameter.
     */
    protected final AnnotatedMember _member;

    protected final String _name;
    
    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */
    
    public SimpleBeanPropertyDefinition(AnnotatedMember member) {
        this(member, member.getName());
    }

    public SimpleBeanPropertyDefinition(AnnotatedMember member, String name) {
        _member = member;
        _name = name;
    }

    /*
    /**********************************************************
    /* Fluent factories
    /**********************************************************
     */

    @Override
    public SimpleBeanPropertyDefinition withName(String newName) {
        if (_name.equals(newName)) {
            return this;
        }
        return new SimpleBeanPropertyDefinition(_member, newName);
    }
    
    /*
    /**********************************************************
    /* Basic property information, name, type
    /**********************************************************
     */

    @Override
    public String getName() { return _name; }

    @Override
    public String getInternalName() { return getName(); }

    // hmmh. what should we claim here?
    @Override
    public boolean isExplicitlyIncluded() { return false; }
    
    /*
    /**********************************************************
    /* Access to accessors (fields, methods etc)
    /**********************************************************
     */

    @Override
    public boolean hasGetter() {
        return (getGetter() != null);
    }

    @Override
    public boolean hasSetter() {
        return (getSetter() != null);
    }

    @Override
    public boolean hasField() {
        return (_member instanceof AnnotatedField);
    }

    @Override
    public boolean hasConstructorParameter() {
        return (_member instanceof AnnotatedParameter);
    }
    
    @Override
    public AnnotatedMethod getGetter() {
        if ((_member instanceof AnnotatedMethod)
                && ((AnnotatedMethod) _member).getParameterCount() == 0) {
            return (AnnotatedMethod) _member;
        }
        return null;
    }
        
    @Override
    public AnnotatedMethod getSetter() {
        if ((_member instanceof AnnotatedMethod)
                && ((AnnotatedMethod) _member).getParameterCount() == 1) {
            return (AnnotatedMethod) _member;
        }
        return null;
    }

    @Override
    public AnnotatedField getField() {
        return (_member instanceof AnnotatedField) ?
                (AnnotatedField) _member : null;
    }

    @Override
    public AnnotatedParameter getConstructorParameter() {
        return (_member instanceof AnnotatedParameter) ?
                (AnnotatedParameter) _member : null;
    }

    /**
     * Method used to find accessor (getter, field to access) to use for accessing
     * value of the property.
     * Null if no such member exists.
     */
    @Override
    public AnnotatedMember getAccessor() {
        AnnotatedMember acc = getGetter();
        if (acc == null) {
            acc = getField();
        }
        return acc;
    }

    /**
     * Method used to find mutator (constructor parameter, setter, field) to use for
     * changing value of the property.
     * Null if no such member exists.
     */
    @Override
    public AnnotatedMember getMutator() {
        AnnotatedMember acc = getConstructorParameter();
        if (acc == null) {
            acc = getSetter();
            if (acc == null) {
                acc = getField();
            }
        }
        return acc;
    }

    @Override
    public AnnotatedMember getPrimaryMember() {
        return _member;
    }
}
