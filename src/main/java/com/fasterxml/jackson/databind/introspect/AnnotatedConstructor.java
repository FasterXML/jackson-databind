package com.fasterxml.jackson.databind.introspect;

import java.lang.reflect.*;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.util.ClassUtil;

public final class AnnotatedConstructor
    extends AnnotatedWithParams
{
    private static final long serialVersionUID = 1L;

    protected final Constructor<?> _constructor;

    /**
     * Field that is used to make JDK serialization work with this
     * object.
     * 
     * @since 2.1
     */
    protected Serialization _serialization;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public AnnotatedConstructor(AnnotatedClass ctxt, Constructor<?> constructor,
            AnnotationMap classAnn, AnnotationMap[] paramAnn)
    {
        super(ctxt, classAnn, paramAnn);
        if (constructor == null) {
            throw new IllegalArgumentException("Null constructor not allowed");
        }
        _constructor = constructor;
    }

    /**
     * Method used for JDK serialization support
     * @since 2.1
     */
    protected AnnotatedConstructor(Serialization ser)
    {
        super(null, null, null);
        _constructor = null;
        _serialization = ser;
    }
    
    @Override
    public AnnotatedConstructor withAnnotations(AnnotationMap ann) {
        return new AnnotatedConstructor(_context, _constructor, ann, _paramAnnotations);
    }
    
    /*
    /**********************************************************
    /* Annotated impl
    /**********************************************************
     */

    @Override
    public Constructor<?> getAnnotated() { return _constructor; }

    @Override
    public int getModifiers() { return _constructor.getModifiers(); }

    @Override
    public String getName() { return _constructor.getName(); }

    @Override
    public Type getGenericType() {
        return getRawType();
    }

    @Override
    public Class<?> getRawType() {
        return _constructor.getDeclaringClass();
    }

    // note: copied verbatim from AnnotatedMethod; hard to generalize
    /**
     * As per [JACKSON-468], we need to also allow declaration of local
     * type bindings; mostly it will allow defining bounds.
     */
    @Override
    public JavaType getType(TypeBindings bindings)
    {
        return getType(bindings, _constructor.getTypeParameters());
    }
    
    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    @Override
    public int getParameterCount() {
        return _constructor.getParameterTypes().length;
    }

    @Override
    public Class<?> getRawParameterType(int index)
    {
        Class<?>[] types = _constructor.getParameterTypes();
        return (index >= types.length) ? null : types[index];
    }

    @Override
    public Type getGenericParameterType(int index)
    {
        Type[] types = _constructor.getGenericParameterTypes();
        return (index >= types.length) ? null : types[index];
    }

    @Override
    public final Object call() throws Exception {
        return _constructor.newInstance();
    }

    @Override
    public final Object call(Object[] args) throws Exception {
        return _constructor.newInstance(args);
    }

    @Override
    public final Object call1(Object arg) throws Exception {
        return _constructor.newInstance(arg);
    }
    
    /*
    /**********************************************************
    /* AnnotatedMember impl
    /**********************************************************
     */

    @Override
    public Class<?> getDeclaringClass() { return _constructor.getDeclaringClass(); }

    @Override
    public Member getMember() { return _constructor; }

    @Override
    public void setValue(Object pojo, Object value)
        throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Cannot call setValue() on constructor of "
                +getDeclaringClass().getName());
    }

    @Override
    public Object getValue(Object pojo)
        throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Cannot call getValue() on constructor of "
                +getDeclaringClass().getName());
    }
    
    /*
    /**********************************************************
    /* Extended API, specific annotations
    /**********************************************************
     */

    @Override
    public String toString() {
        return "[constructor for "+getName()+", annotations: "+_annotations+"]";
    }

    @Override
    public int hashCode() {
        return _constructor.getName().hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        return ((AnnotatedConstructor) o)._constructor == _constructor;
    }
    
    /*
    /**********************************************************
    /* JDK serialization handling
    /**********************************************************
     */

    Object writeReplace() {
        return new AnnotatedConstructor(new Serialization(_constructor));
    }

    Object readResolve() {
        Class<?> clazz = _serialization.clazz;
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor(_serialization.args);
            // 06-Oct-2012, tatu: Has "lost" its security override, must force back
            if (!ctor.isAccessible()) {
                ClassUtil.checkAndFixAccess(ctor);
            }
            return new AnnotatedConstructor(null, ctor, null, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find constructor with "
                    +_serialization.args.length+" args from Class '"+clazz.getName());
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
        protected Class<?>[] args;

        public Serialization(Constructor<?> ctor) {
            clazz = ctor.getDeclaringClass();
            args = ctor.getParameterTypes();
        }
    }
}
