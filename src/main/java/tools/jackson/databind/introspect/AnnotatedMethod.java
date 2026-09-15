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

import static java.lang.invoke.MethodType.methodType;

import static tools.jackson.databind.util.ClassUtil.sneakyThrow;

public final class AnnotatedMethod
    extends AnnotatedWithParams
{
    // Invocation types for the arity-specific handles; constant so no per-use lookup
    private static final MethodType INVOKER_NULLARY_TYPE = methodType(Object.class);
    private static final MethodType INVOKER_UNARY_TYPE = methodType(Object.class, Object.class);

    final protected transient Method _method;

    /**
     * Lazily resolved invocation handles, one per arity we support;
     * {@code volatile} so a racy first use is safely published (duplicate
     * resolution is harmless, handles are equivalent).
     */
    protected volatile MethodHandle _invokerFixedArity;
    protected volatile MethodHandle _invokerNullary;
    protected volatile MethodHandle _invokerUnary;

    // // Simple lazy-caching:

    /**
     * Lazily resolved raw parameter types; {@code volatile} to ensure safe
     * publication of the array contents (racy re-resolution is harmless).
     */
    protected volatile Class<?>[] _paramClasses;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
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
    /**********************************************************************
    /* AnnotatedWithParams
    /**********************************************************************
     */

    @Override
    public final Object call() throws Exception {
        try {
            return invokerNullary().invokeExact();
        } catch (final Throwable e) {
            throw sneakyThrow(e);
        }
    }

    @Override
    public final Object call(Object[] args) throws Exception {
        try {
            return invokerFixedArity().invokeWithArguments(args);
        } catch (final Throwable e) {
            throw sneakyThrow(e);
        }
    }

    @Override
    public final Object call1(Object arg) throws Exception {
        try {
            return invokerUnary().invokeExact(arg);
        } catch (final Throwable e) {
            throw sneakyThrow(e);
        }
    }

    public final Object callOn(Object pojo) throws Exception {
        try {
            return invokerUnary().invokeExact(pojo);
        } catch (Throwable e) {
            throw sneakyThrow(e);
        }
    }

    public final Object callOnWith(Object pojo, Object... args) throws Exception {
        try {
            MethodHandle invoker = invokerFixedArity();
            if (!Modifier.isStatic(_method.getModifiers())) {
                invoker = invoker.bindTo(pojo);
            }
            return invoker.invokeWithArguments(args);
        } catch (Throwable e) {
            throw sneakyThrow(e);
        }
    }

    private MethodHandle invokerNullary() throws IllegalAccessException {
        MethodHandle h = _invokerNullary;
        if (h == null) {
            h = unreflect().asType(INVOKER_NULLARY_TYPE);
            _invokerNullary = h;
        }
        return h;
    }

    private MethodHandle invokerUnary() throws IllegalAccessException {
        MethodHandle h = _invokerUnary;
        if (h == null) {
            h = unreflect().asType(INVOKER_UNARY_TYPE);
            _invokerUnary = h;
        }
        return h;
    }

    private MethodHandle invokerFixedArity() throws IllegalAccessException {
        MethodHandle h = _invokerFixedArity;
        if (h == null) {
            h = unreflect().asFixedArity();
            _invokerFixedArity = h;
        }
        return h;
    }

    /**
     * Note: caller is expected to have called {@code ClassUtil.checkAndFixAccess()}
     * already; access checks are suppressed for an accessible {@link Method}.
     */
    private MethodHandle unreflect() throws IllegalAccessException {
        return MethodHandles.lookup().unreflect(_method);
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
        return "%s(%d params)".formatted(super.getFullName(), getParameterCount());
    }

    public Class<?>[] getRawParameterTypes()
    {
        Class<?>[] types = _paramClasses;
        if (types == null) {
            types = _method.getParameterTypes();
            _paramClasses = types;
        }
        return types;
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
