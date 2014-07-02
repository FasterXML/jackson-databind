package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
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
    /**********************************************************
    /* Fluent factory methods for creating modified copies
    /**********************************************************
     */

    @Deprecated // since 2.3
    public BeanPropertyDefinition withName(String newName) { return withSimpleName(newName); }
    
    /**
     * Method that can be used to create a definition with
     * same settings as this one, but with different
     * (external) name; that is, one for which
     * {@link #getName()} would return <code>newName</code>.
     * 
     * @since 2.3
     */
    public abstract BeanPropertyDefinition withName(PropertyName newName);

    /**
     * Alternate "mutant factory" that will only change simple name, but
     * leave other optional parts (like namespace) as is.
     * 
     * @since 2.3
     */
    public abstract BeanPropertyDefinition withSimpleName(String newSimpleName);
    
    /*
    /**********************************************************
    /* Basic property information, name, type
    /**********************************************************
     */

    /**
     * Accessor for name used for external representation (in JSON).
     */
    @Override // from Named
    public abstract String getName();

    public abstract PropertyName getFullName();
    
    /**
     * Accessor that can be used to determine implicit name from underlying
     * element(s) before possible renaming. This is the "internal"
     * name derived from accessor ("x" from "getX"), and is not based on
     * annotations or naming strategy.
     */
    public abstract String getInternalName();
    
    /**
     * Accessor for finding wrapper name to use for property (if any).
     * 
     * @since 2.2
     */
    public abstract PropertyName getWrapperName();

    /**
     * Method for accessing additional metadata.
     * NOTE: will never return null, so deferencing return value
     * is safe.
     * 
     * @since 2.3
     */
    public abstract PropertyMetadata getMetadata();
    
    /**
     * Accessor that can be called to check whether property was included
     * due to an explicit marker (usually annotation), or just by naming
     * convention.
     * 
     * @return True if property was explicitly included (usually by having
     *   one of components being annotated); false if inclusion was purely
     *   due to naming or visibility definitions (that is, implicit)
     */
    public abstract boolean isExplicitlyIncluded();

    /**
     * Accessor that can be called to check whether property name was
     * due to an explicit marker (usually annotation), or just by naming
     * convention or use of "use-default-name" marker (annotation).
     *<p>
     * Note that entries that return true from this method will always
     * return true for {@link #isExplicitlyIncluded()}, but not necessarily
     * vice versa.
     *
     * @since 2.4
     */
    public boolean isExplicitlyNamed() {
        return isExplicitlyIncluded();
    }
    
    /*
    /**********************************************************
    /* Capabilities
    /**********************************************************
     */

    public boolean couldDeserialize() { return getMutator() != null; }
    public boolean couldSerialize() { return getAccessor() != null; }

    /*
    /**********************************************************
    /* Access to accessors (fields, methods etc)
    /**********************************************************
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

    /**
     * @since 2.3
     */
    public abstract AnnotatedMember getNonConstructorMutator();
    
    /**
     * Method used to find the property member (getter, setter, field) that has
     * the highest precedence in current context (getter method when serializing,
     * if available, and so forth), if any.
     * 
     * @since 2.1
     */
    public AnnotatedMember getPrimaryMember() { return null; }
    
    /*
    /**********************************************************
    /* More refined access to configuration features
    /* (usually based on annotations)
    /* Since most trivial implementations do not support
    /* these methods, they are implemented as no-ops.
    /**********************************************************
     */
    
    /**
     * Method used to find View-inclusion definitions for the property.
     */
    public Class<?>[] findViews() { return null; }

    /**
     * Method used to find whether property is part of a bi-directional
     * reference.
     */
    public AnnotationIntrospector.ReferenceProperty findReferenceType() { return null; }

    /**
     * Method used to check whether this logical property has a marker
     * to indicate it should be used as the type id for polymorphic type
     * handling.
     */
    public boolean isTypeId() { return false; }

    /**
     * Method used to check whether this logical property indicates that
     * value POJOs should be written using additional Object Identifier
     * (or, when multiple references exist, all but first AS Object Identifier).
     */
    public ObjectIdInfo findObjectIdInfo() { return null; }
    
    /**
     * Method used to check if this property is expected to have a value;
     * and if none found, should either be considered invalid (and most likely
     * fail deserialization), or handled by other means (by providing default
     * value)
     */
    public final boolean isRequired() {
        PropertyMetadata md = getMetadata();
        return (md != null)  && md.isRequired();
    }
}
