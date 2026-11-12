package tools.jackson.databind.introspect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Objects;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.internal.UnreflectHandleSupplier;

import static java.lang.invoke.MethodType.methodType;

public final class AnnotatedConstructor
    extends AnnotatedWithParams
{
    protected final Constructor<?> _constructor;
    private final InvokerHolder _invokerNullary = new InvokerHolder(methodType(Object.class));
    private final InvokerHolder _invokerUnary = new InvokerHolder(methodType(Object.class, Object.class));
    private final InvokerHolder _invokerFixedArity = new InvokerHolder(null);

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
        Class<?>[] types = _constructor.getParameterTypes();
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
            return _invokerNullary.get().invokeExact();
        } catch (Throwable e) {
            throw ClassUtil.sneakyThrow(e);
        }
    }

    @Override
    public final Object call(Object[] args) throws Exception {
        try {
            return _invokerFixedArity.get().invokeWithArguments(args);
        } catch (Throwable e) {
            throw ClassUtil.sneakyThrow(e);
        }
    }

    @Override
    public final Object call1(Object arg) throws Exception {
        try {
            return _invokerUnary.get().invokeExact(arg);
        } catch (Throwable e) {
            throw ClassUtil.sneakyThrow(e);
        }
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
        return String.format("[constructor for %s (%d arg%s), annotations: %s",
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

    class InvokerHolder extends UnreflectHandleSupplier {
        private static final long serialVersionUID = 1L;

        InvokerHolder(MethodType asType) {
            super(asType);
        }

        @Override
        protected MethodHandle unreflect() throws IllegalAccessException {
            return MethodHandles.lookup().unreflectConstructor(_constructor);
        }
    }
}
