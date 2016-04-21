package com.fasterxml.jackson.databind.introspect;

import java.lang.reflect.*;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Placeholder used by virtual properties as placeholder for
 * underlying {@link AnnotatedMember}.
 * 
 * @since 2.5
 */
public class VirtualAnnotatedMember extends AnnotatedMember
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final Class<?> _declaringClass;

    /**
     * @since 2.8 with this signature; had <code>_rawType</code> earlier
     */
    protected final JavaType _type;

    protected final String _name;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public VirtualAnnotatedMember(TypeResolutionContext typeContext, Class<?> declaringClass,
            String name, JavaType type)
    {
        super(typeContext, /* AnnotationMap*/ null);
        _declaringClass = declaringClass;
        _type = type;
        _name = name;
    }

    /**
     * @deprecated Since 2.8
     */
    @Deprecated
    public VirtualAnnotatedMember(TypeResolutionContext typeContext, Class<?> declaringClass,
            String name, Class<?> rawType) {
        this(typeContext, declaringClass, name, typeContext.resolveType(rawType));
    }

    @Override
    public Annotated withAnnotations(AnnotationMap fallback) {
        return this;
    }
    
    /*
    /**********************************************************
    /* Annotated impl
    /**********************************************************
     */

    @Override
    public Field getAnnotated() { return null; }

    @Override
    public int getModifiers() { return 0; }

    @Override
    public String getName() { return _name; }

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
    /* AnnotatedMember impl
    /**********************************************************
     */

    @Override
    public Class<?> getDeclaringClass() { return _declaringClass; }

    @Override
    public Member getMember() { return null; }

    @Override
    public void setValue(Object pojo, Object value) throws IllegalArgumentException {
        throw new IllegalArgumentException("Can not set virtual property '"+_name+"'");
    }

    @Override
    public Object getValue(Object pojo) throws IllegalArgumentException {
        throw new IllegalArgumentException("Can not get virtual property '"+_name+"'");
    }
    
    /*
    /**********************************************************
    /* Extended API, generic
    /**********************************************************
     */

    public String getFullName() {
        return getDeclaringClass().getName() + "#" + getName();
    }

    public int getAnnotationCount() { return 0; }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        VirtualAnnotatedMember other = (VirtualAnnotatedMember) o;
        return (other._declaringClass == _declaringClass)
                && other._name.equals(_name);
    }

    @Override
    public String toString() {
        return "[field "+getFullName()+"]";
    }
}
