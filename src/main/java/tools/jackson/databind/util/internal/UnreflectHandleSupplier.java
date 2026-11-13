package tools.jackson.databind.util.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

import tools.jackson.databind.util.ClassUtil;

/**
 * Lazy memoized holder for MethodHandles.
 * Defers binding of the method handle until after access checks are suppressed
 * (which happens in a virtual method call after construction) and avoids serialization of
 * MethodHandle.
 */
public abstract class UnreflectHandleSupplier implements Supplier<MethodHandle> {
    private final MethodType asType;
    private volatile MethodHandle cachedHandle;

    public UnreflectHandleSupplier(MethodType asType) {
        this.asType = asType;
    }

    @Override
    public MethodHandle get() {
        MethodHandle h = cachedHandle;
        if (h == null) {
            h = initialize();
        }
        return h;
    }

    private synchronized MethodHandle initialize() {
        MethodHandle h = cachedHandle;
        if (h == null) {
            try {
                h = postprocess(unreflect());
            } catch (IllegalAccessException e) {
                throw ClassUtil.sneakyThrow(e);
            }
            cachedHandle = h;
        }
        return h;
    }

    protected MethodHandle postprocess(MethodHandle mh) {
        if (mh == null) {
            return mh;
        }
        // [databind#5231], [databind#6426] A varargs member unreflects to a variable-arity
        // handle, which would collect the (already array-valued) trailing argument into
        // another array. That applies both to `asType()` below and, when `asType` is
        // `null`, to callers using `invokeWithArguments()`: so all handles must be fixed-arity
        mh = mh.asFixedArity();
        return (asType == null) ? mh : mh.asType(asType);
    }

    protected abstract MethodHandle unreflect() throws IllegalAccessException;

    @Override
    public String toString() {
        return get().toString();
    }
}
