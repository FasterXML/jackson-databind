package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

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
     * Transient since it can not be persisted directly using
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

    public AnnotatedField(AnnotatedClass contextClass, Field field, AnnotationMap annMap)
    {
        super(contextClass, annMap);
        _field = field;
    }
    
    @Override
    public AnnotatedField withAnnotations(AnnotationMap ann) {
        return new AnnotatedField(_context, _field, ann);
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
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        return (_annotations == null) ? null : _annotations.get(acls);
    }

    @Override
    public Type getGenericType() {
        return _field.getGenericType();
    }

    @Override
    public Class<?> getRawType() {
        return _field.getType();
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

    public String getFullName() {
        return getDeclaringClass().getName() + "#" + getName();
    }

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
        if (o == null || o.getClass() != getClass()) return false;
        return ((AnnotatedField) o)._field == _field;
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
                ClassUtil.checkAndFixAccess(f);
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

