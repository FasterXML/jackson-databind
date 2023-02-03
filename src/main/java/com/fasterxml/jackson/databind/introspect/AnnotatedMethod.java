package com.fasterxml.jackson.databind.introspect;

import java.lang.reflect.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.util.ClassUtil;

public final class AnnotatedMethod
    extends AnnotatedWithParams
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    final protected transient Method _method;

    // // Simple lazy-caching:

    protected Class<?>[] _paramClasses;

    /**
     * Field that is used to make JDK serialization work with this
     * object.
     *
     * @since 2.1
     */
    protected Serialization _serialization;

    /*
    /*****************************************************
    /* Life-cycle
    /*****************************************************
     */

    public AnnotatedMethod(TypeResolutionContext ctxt, Method method,
            AnnotationMap classAnn, AnnotationMap[] paramAnnotations)
    {
        super(ctxt, classAnn, paramAnnotations);
        if (method == null) {
            throw new IllegalArgumentException("Cannot construct AnnotatedMethod with null Method");
        }
        _method = method;
    }

    /**
     * Method used for JDK serialization support
     * @since 2.1
     */
    protected AnnotatedMethod(Serialization ser)
    {
        super(null, null, null);
        _method = null;
        _serialization = ser;
    }

    @Override
    public AnnotatedMethod withAnnotations(AnnotationMap ann) {
        return new AnnotatedMethod(_typeContext, _method, ann, _paramAnnotations);
    }

    @Override
    public Method getAnnotated() { return _method; }

    @Override
    public int getModifiers() { return _method.getModifiers(); }

    @Override
    public String getName() { return _method.getName(); }

    /**
     * For methods, this returns declared return type, which is only
     * useful with getters (setters do not return anything; hence `Void`
     * would be returned here)
     */
    @Override
    public JavaType getType() {
        return _typeContext.resolveType(_method.getGenericReturnType());
    }

    /**
     * For methods, this returns declared return type, which is only
     * useful with getters (setters do not usually return anything;
     * hence "void" type is returned here)
     */
    @Override
    public Class<?> getRawType() {
        return _method.getReturnType();
    }

    /*
    /*****************************************************
    /* AnnotatedWithParams
    /*****************************************************
     */

    @Override
    public final Object call() throws Exception {
        // 31-Mar-2021, tatu: Note! This is faster than calling without arguments
        //   because JDK in its wisdom would otherwise allocate `new Object[0]` to pass
        return _method.invoke(null);
    }

    @Override
    public final Object call(Object[] args) throws Exception {
        return _method.invoke(null, args);
    }

    @Override
    public final Object call1(Object arg) throws Exception {
        return _method.invoke(null, arg);
    }

    public final Object callOn(Object pojo) throws Exception {
        return _method.invoke(pojo, (Object[]) null);
    }

    public final Object callOnWith(Object pojo, Object... args) throws Exception {
        return _method.invoke(pojo, args);
    }

    /*
    /********************************************************
    /* AnnotatedMember impl
    /********************************************************
     */

    @Override
    public int getParameterCount() {
        return _method.getParameterCount();
    }

    @Override
    public Class<?> getRawParameterType(int index)
    {
        Class<?>[] types = getRawParameterTypes();
        return (index >= types.length) ? null : types[index];
    }

    @Override
    public JavaType getParameterType(int index) {
        Type[] types = _method.getGenericParameterTypes();
        if (index >= types.length) {
            return null;
        }
        return _typeContext.resolveType(types[index]);
    }

    @Override
    @Deprecated // since 2.7
    public Type getGenericParameterType(int index) {
        Type[] types = getGenericParameterTypes();
        if (index >= types.length) {
            return null;
        }
        return types[index];
    }

    @Override
    public Class<?> getDeclaringClass() { return _method.getDeclaringClass(); }

    @Override
    public Method getMember() { return _method; }

    @Override
    public void setValue(Object pojo, Object value) throws IllegalArgumentException
    {
        try {
            _method.invoke(pojo, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to setValue() with method "
                    +getFullName()+": "+ClassUtil.exceptionMessage(e), e);
        }
    }

    @Override
    public Object getValue(Object pojo) throws IllegalArgumentException
    {
        try {
            return _method.invoke(pojo, (Object[]) null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to getValue() with method "
                    +getFullName()+": "+ClassUtil.exceptionMessage(e), e);
        }
    }

    /*
    /*****************************************************
    /* Extended API, generic
    /*****************************************************
     */

    @Override
    public String getFullName() {
        final String methodName = super.getFullName();
        switch (getParameterCount()) {
        case 0:
            return methodName+"()";
        case 1:
            return methodName+"("+getRawParameterType(0).getName()+")";
        default:
        }
        return String.format("%s(%d params)", super.getFullName(), getParameterCount());
    }

    public Class<?>[] getRawParameterTypes()
    {
        if (_paramClasses == null) {
            _paramClasses = _method.getParameterTypes();
        }
        return _paramClasses;
    }

    @Deprecated // since 2.7
    public Type[] getGenericParameterTypes() {
        return _method.getGenericParameterTypes();
    }

    public Class<?> getRawReturnType() {
        return _method.getReturnType();
    }

    /**
     * Helper method that can be used to check whether method returns
     * a value or not; if return type declared as <code>void</code>, returns
     * false, otherwise true
     *
     * @since 2.4
     *
     * @deprecated Since 2.12 (related to [databind#2675]), needs to be configurable
     */
    @Deprecated
    public boolean hasReturnType() {
        Class<?> rt = getRawReturnType();
        // also, as per [databind#2675], only consider `void` to be real "No return type"
        return (rt != Void.TYPE);
    }

    /*
    /********************************************************
    /* Other
    /********************************************************
     */

    @Override
    public String toString() {
        return "[method "+getFullName()+"]";
    }

    @Override
    public int hashCode() {
        return _method.getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!ClassUtil.hasClass(o, getClass())) {
            return false;
        }

        AnnotatedMethod other = (AnnotatedMethod) o;
        if (other._method == null) {
            return _method == null;
        } else {
            return other._method.equals(_method);
        }
    }

    /*
    /**********************************************************
    /* JDK serialization handling
    /**********************************************************
     */

    Object writeReplace() {
        return new AnnotatedMethod(new Serialization(_method));
    }

    Object readResolve() {
        Class<?> clazz = _serialization.clazz;
        try {
            Method m = clazz.getDeclaredMethod(_serialization.name,
                    _serialization.args);
            // 06-Oct-2012, tatu: Has "lost" its security override, may need to force back
            if (!m.isAccessible()) {
                ClassUtil.checkAndFixAccess(m, false);
            }
            return new AnnotatedMethod(null, m, null, null);
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
        protected Class<?>[] args;

        public Serialization(Method setter) {
            clazz = setter.getDeclaringClass();
            name = setter.getName();
            args = setter.getParameterTypes();
        }
    }
}
