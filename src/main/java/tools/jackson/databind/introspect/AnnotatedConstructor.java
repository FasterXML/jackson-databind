package tools.jackson.databind.introspect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Objects;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.util.ClassUtil;

import static java.lang.invoke.MethodType.methodType;

public final class AnnotatedConstructor
    extends AnnotatedWithParams
{
    protected final Constructor<?> _constructor;

    /**
     * Lazily resolved invocation handles, one per arity we support;
     * {@code volatile} so a racy first use is safely published (duplicate
     * resolution is harmless, handles are equivalent).
     */
    protected volatile MethodHandle _invokerNullary;
    protected volatile MethodHandle _invokerUnary;
    protected volatile MethodHandle _invokerFixedArity;

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

    public AnnotatedConstructor(TypeResolutionContext ctxt, Constructor<?> constructor,
            AnnotationMap classAnn, AnnotationMap[] paramAnn)
    {
        super(ctxt, classAnn, paramAnn);
        _constructor = Objects.requireNonNull(constructor);
    }

    @Override
    public AnnotatedConstructor withAnnotations(AnnotationMap ann) {
        return new AnnotatedConstructor(_typeContext, _constructor, ann, _paramAnnotations);
    }

    /*
    /**********************************************************************
    /* Annotated impl
    /**********************************************************************
     */

    @Override
    public Constructor<?> getAnnotated() { return _constructor; }

    @Override
    public int getModifiers() { return _constructor.getModifiers(); }

    @Override
    public String getName() { return _constructor.getName(); }

    @Override
    public JavaType getType() {
        return _typeContext.resolveType(getRawType());
    }

    @Override
    public Class<?> getRawType() {
        return _constructor.getDeclaringClass();
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    @Override
    public int getParameterCount() {
        return _constructor.getParameterCount();
    }

    @Override
    public Class<?> getRawParameterType(int index)
    {
        Class<?>[] types = _paramClasses;
        if (types == null) {
            types = _constructor.getParameterTypes();
            _paramClasses = types;
        }
        return (index >= types.length) ? null : types[index];
    }

    @Override
    public JavaType getParameterType(int index) {
        Type[] types = _constructor.getGenericParameterTypes();
        if (index >= types.length) {
            return null;
        }
        return _typeContext.resolveType(types[index]);
    }

    @Override
    public Parameter[] getNativeParameters() {
        return _constructor.getParameters();
    }

    @Override
    public final Object call() throws Exception {
        try {
            return invokerNullary().invokeExact();
        } catch (Throwable e) {
            throw ClassUtil.sneakyThrow(e);
        }
    }

    @Override
    public final Object call(Object[] args) throws Exception {
        try {
            return invokerFixedArity().invokeWithArguments(args);
        } catch (Throwable e) {
            throw ClassUtil.sneakyThrow(e);
        }
    }

    @Override
    public final Object call1(Object arg) throws Exception {
        try {
            return invokerUnary().invokeExact(arg);
        } catch (Throwable e) {
            throw ClassUtil.sneakyThrow(e);
        }
    }

    private MethodHandle invokerNullary() throws IllegalAccessException {
        MethodHandle h = _invokerNullary;
        if (h == null) {
            h = unreflect().asType(methodType(Object.class));
            _invokerNullary = h;
        }
        return h;
    }

    private MethodHandle invokerUnary() throws IllegalAccessException {
        MethodHandle h = _invokerUnary;
        if (h == null) {
            h = unreflect().asType(methodType(Object.class, Object.class));
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
     * already; access checks are suppressed for an accessible {@link Constructor}.
     */
    private MethodHandle unreflect() throws IllegalAccessException {
        return MethodHandles.lookup().unreflectConstructor(_constructor);
    }

    /*
    /**********************************************************************
    /* AnnotatedMember impl
    /**********************************************************************
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
    /**********************************************************************
    /* Extended API, specific annotations
    /**********************************************************************
     */

    @Override
    public String toString() {
        final int argCount = _constructor.getParameterCount();
        return "[constructor for %s (%d arg%s), annotations: %s".formatted(
                ClassUtil.nameOf(_constructor.getDeclaringClass()), argCount,
                (argCount == 1) ? "" : "s", _annotations);
    }

    @Override
    public int hashCode() {
        // _constructor can be null for special case of JDK serialization so:
        return Objects.hashCode(_constructor);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!ClassUtil.hasClass(o, getClass())) {
            return false;
        }
        AnnotatedConstructor other = (AnnotatedConstructor) o;
        return Objects.equals(_constructor, other._constructor);
    }
}
