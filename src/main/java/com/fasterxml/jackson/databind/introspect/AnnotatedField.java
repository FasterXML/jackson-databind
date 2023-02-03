package com.fasterxml.jackson.databind.introspect;

import java.lang.reflect.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Object that represents non-static (and usually non-transient/volatile)
 * fields of a class.
 */
public final class AnnotatedField
    extends AnnotatedMember
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Actual {@link Field} used for access.
     *<p>
     * Transient since it cannot be persisted directly using
     * JDK serialization
     */
    protected final transient Field _field;

    /**
     * Temporary field required for JDK serialization support
     */
    protected Serialization _serialization;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public AnnotatedField(TypeResolutionContext contextClass, Field field, AnnotationMap annMap)
    {
        super(contextClass, annMap);
        _field = field;
    }

    @Override
    public AnnotatedField withAnnotations(AnnotationMap ann) {
        return new AnnotatedField(_typeContext, _field, ann);
    }

    /**
     * Method used for JDK serialization support
     */
    protected AnnotatedField(Serialization ser)
    {
        super(null, null);
        _field = null;
        _serialization = ser;
    }

    /*
    /**********************************************************
    /* Annotated impl
    /**********************************************************
     */

    @Override
    public Field getAnnotated() { return _field; }

    @Override
    public int getModifiers() { return _field.getModifiers(); }

    @Override
    public String getName() { return _field.getName(); }

    @Override
    public Class<?> getRawType() {
        return _field.getType();
    }

    @Override
    public JavaType getType() {
        return _typeContext.resolveType(_field.getGenericType());
    }

    /*
    /**********************************************************
    /* AnnotatedMember impl
    /**********************************************************
     */

    @Override
    public Class<?> getDeclaringClass() { return _field.getDeclaringClass(); }

    @Override
    public Member getMember() { return _field; }

    @Override
    public void setValue(Object pojo, Object value) throws IllegalArgumentException
    {
        try {
            _field.set(pojo, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to setValue() for field "
                    +getFullName()+": "+e.getMessage(), e);
        }
    }

    @Override
    public Object getValue(Object pojo) throws IllegalArgumentException
    {
        try {
            return _field.get(pojo);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to getValue() for field "
                    +getFullName()+": "+e.getMessage(), e);
        }
    }

    /*
    /**********************************************************
    /* Extended API, generic
    /**********************************************************
     */

    public int getAnnotationCount() { return _annotations.size(); }

    /**
     * @since 2.6
     */
    public boolean isTransient() { return Modifier.isTransient(getModifiers()); }

    @Override
    public int hashCode() {
        return _field.getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!ClassUtil.hasClass(o, getClass())) {
            return false;
        }

        AnnotatedField other = (AnnotatedField) o;
        if (other._field == null) {
            return _field == null;
        } else {
            return other._field.equals(_field);
        }
    }

    @Override
    public String toString() {
        return "[field "+getFullName()+"]";
    }

    /*
    /**********************************************************
    /* JDK serialization handling
    /**********************************************************
     */

    Object writeReplace() {
        return new AnnotatedField(new Serialization(_field));
    }

    Object readResolve() {
        Class<?> clazz = _serialization.clazz;
        try {
            Field f = clazz.getDeclaredField(_serialization.name);
            // 06-Oct-2012, tatu: Has "lost" its security override, may need to force back
            if (!f.isAccessible()) {
                ClassUtil.checkAndFixAccess(f, false);
            }
            return new AnnotatedField(null, f, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find method '"+_serialization.name
                        +"' from Class '"+clazz.getName());
        }
    }

    /**
     * Helper class that is used as the workaround to persist
     * Field references. It basically just stores declaring class
     * and field name.
     */
    private final static class Serialization
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;
        protected Class<?> clazz;
        protected String name;

        public Serialization(Field f) {
            clazz = f.getDeclaringClass();
            name = f.getName();

        }
    }
}

