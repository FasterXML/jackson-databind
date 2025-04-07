package tools.jackson.databind.introspect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Objects;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.util.ClassUtil;

import static tools.jackson.databind.util.ClassUtil.sneakyThrow;

public final class AnnotatedMethod
    extends AnnotatedWithParams
{
    final protected transient Method _method;
    final protected transient MethodHandle _invoker;
    final protected transient MethodHandle _nullaryInvoker;
    final protected transient MethodHandle _unaryConstructorInvoker;

    // // Simple lazy-caching:

    protected Class<?>[] _paramClasses;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    private AnnotatedMethod(TypeResolutionContext ctxt, Method method,
            AnnotationMap classAnn, AnnotationMap[] paramAnnotations, MethodHandles.Lookup lookup)
    {
        super(ctxt, classAnn, paramAnnotations);
        if (method == null) {
            throw new IllegalArgumentException("Cannot construct AnnotatedMethod with null Method");
        }
        _method = method;

        MethodHandle invoker;
        try {
            invoker = lookup.unreflect(_method);
        } catch (final IllegalAccessException e) {
            invoker = MethodHandles.dropArguments(
                    MethodHandles.throwException(Object.class, IllegalAccessException.class)
                            .bindTo(e),
                    0, Object.class);
        }
        _invoker = invoker;
        boolean staticMethod = Modifier.isStatic(method.getModifiers());
        int argCount = method.getParameterCount() + (staticMethod ? 0 : 1);
        _nullaryInvoker = argCount > 0 ? null : invoker.asType(MethodType.methodType(Object.class));
        _unaryConstructorInvoker = argCount != 1 ? null : invoker.asType(MethodType.methodType(Object.class, Object.class));
    }

    public static AnnotatedMethod of(TypeResolutionContext ctxt, Method method,
            AnnotationMap classAnn, AnnotationMap[] paramAnnotations)
    {
        Objects.requireNonNull(method);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        if (!ClassUtil.isJDKClass(method.getDeclaringClass())) {
            try {
                lookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), lookup);
            } catch (final IllegalAccessException ign) {
                lookup = lookup.in(method.getDeclaringClass());
            }
        } else {
            lookup = lookup.in(method.getDeclaringClass());
        }
        if (lookup.lookupModes() == 0) {
            // disallowed cross-module access
            return null;
        }
        return new AnnotatedMethod(ctxt, method, classAnn, paramAnnotations, lookup);
    }

    @Override
    public AnnotatedMethod withAnnotations(AnnotationMap ann) {
        return AnnotatedMethod.of(_typeContext, _method, ann, _paramAnnotations);
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
    /**********************************************************************
    /* AnnotatedWithParams
    /**********************************************************************
     */

    @Override
    public final Object call() throws Exception {
        try {
            return _nullaryInvoker.invokeExact();
        } catch (final Throwable e) {
            throw sneakyThrow(e);
        }
    }

    @Override
    public final Object call(Object[] args) throws Exception {
        try {
            return _invoker.invokeWithArguments(args);
        } catch (final Throwable e) {
            throw sneakyThrow(e);
        }
    }

    @Override
    public final Object call1(Object arg) throws Exception {
        if (_unaryConstructorInvoker == null) {
            throw new UnsupportedOperationException("method " + _method + " requires arg count != 1");
        }
        try {
            Object ret = _unaryConstructorInvoker.invokeExact(arg);
            return ret == null ? arg : ret;
        } catch (final Throwable e) {
            throw sneakyThrow(e);
        }
    }

    public final Object callOn(Object pojo) throws Exception {
        if (_nullaryInvoker == null) {
            throw new UnsupportedOperationException("method " + _method + " requires args, but none provided");
        }
        try {
            return _nullaryInvoker.invoke(pojo);
        } catch (Throwable e) {
            throw sneakyThrow(e);
        }
    }

    public final Object callOnWith(Object pojo, Object... args) throws Exception {
        try {
            MethodHandle invoker = _invoker;
            if (!Modifier.isStatic(_method.getModifiers())) {
                invoker = invoker.bindTo(pojo);
            }
            return invoker.invokeWithArguments(args);
        } catch (Throwable e) {
            throw sneakyThrow(e);
        }
    }

    /*
    /**********************************************************************
    /* AnnotatedMember impl
    /**********************************************************************
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
    public Parameter[] getNativeParameters() {
        return _method.getParameters();
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
    /**********************************************************************
    /* Extended API, generic
    /**********************************************************************
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

    public Class<?> getRawReturnType() {
        return _method.getReturnType();
    }

    /*
    /**********************************************************************
    /* Other
    /**********************************************************************
     */

    @Override
    public String toString() {
        return "[method "+getFullName()+"]";
    }

    @Override
    public int hashCode() {
        return _method.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!ClassUtil.hasClass(o, getClass())) {
            return false;
        }
        AnnotatedMethod other = (AnnotatedMethod) o;
        return Objects.equals(_method, other._method);
    }
}
