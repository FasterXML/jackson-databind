package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.util.Named;

/**
 * Simple value classes that contain definitions of properties,
 * used during introspection of properties to use for
 * serialization and deserialization purposes.
 * These instances are created before actual {@link BeanProperty}
 * instances are created, i.e. they are used earlier in the process
 * flow, and are typically use to construct actual
 * {@link BeanProperty} instances.
 */
public abstract class BeanPropertyDefinition
    implements Named
{
    /*
    /*****************************************************
    /* Fluent factory methods for creating modified copies
    /*****************************************************
     */

    /**
     * Method that can be used to create a definition with
     * same settings as this one, but with different
     * (external) name; that is, one for which
     * {@link #getName()} would return <code>newName</code>.
     */
    public abstract BeanPropertyDefinition withName(String newName);
    
    /*
    /*****************************************************
    /* Basic property information, name, type
    /*****************************************************
     */

    /**
     * Accessor for name used for external representation (in JSON).
     */
    @Override // from Named
    public abstract String getName();

    /**
     * Accessor that can be used to determine implicit name from underlying
     * element(s) before possible renaming. This is the "internal"
     * name derived from accessor ("x" from "getX"), and is not based on
     * annotations or naming strategy.
     */
    public abstract String getInternalName();

    /*
    /*****************************************************
    /* Capabilities
    /*****************************************************
     */

    public boolean couldDeserialize() {
        return getMutator() != null;
    }
    public boolean couldSerialize() {
        return getAccessor() != null;
    }
    
    /*
    /*****************************************************
    /* Access to accessors (fields, methods etc)
    /*****************************************************
     */
    
    public abstract boolean hasGetter();
    public abstract boolean hasSetter();
    public abstract boolean hasField();
    public abstract boolean hasConstructorParameter();
    
    public abstract AnnotatedMethod getGetter();
    public abstract AnnotatedMethod getSetter();
    public abstract AnnotatedField getField();
    public abstract AnnotatedParameter getConstructorParameter();

    /**
     * Method used to find accessor (getter, field to access) to use for accessing
     * value of the property.
     * Null if no such member exists.
     */
    public abstract AnnotatedMember getAccessor();

    /**
     * Method used to find mutator (constructor parameter, setter, field) to use for
     * changing value of the property.
     * Null if no such member exists.
     */
    public abstract AnnotatedMember getMutator();

    /*
    /*****************************************************
    /* More refined access to configuration features
    /* (usually based on annotations)
    /*****************************************************
     */
    
    /**
     * Method used to find View-inclusion definitions for the property.
     */
    public Class<?>[] findViews() { return null; }

    /**
     * Method used to find whether property is part of a bi-directional
     * reference.
     */
    public AnnotationIntrospector.ReferenceProperty findReferenceType() {
        return null;
    }

    /**
     * Method used to check whether this logical property has marker(s)
     * to indicate it should be used as the type id for polymorphic type
     * handling.
     */
    public boolean isTypeId() {
        return false;
    }
}
