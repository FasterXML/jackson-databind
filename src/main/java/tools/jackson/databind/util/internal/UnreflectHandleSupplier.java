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
    private boolean initialized;
    private Supplier<MethodHandle> delegate = this::initialize;

    public UnreflectHandleSupplier(MethodType asType) {
        this.asType = asType;
    }

    @Override
    public MethodHandle get() {
        return delegate.get();
    }

    synchronized MethodHandle initialize() {
        if (!initialized) {
            MethodHandle mh;
            try {
                mh = postprocess(unreflect());
            } catch (IllegalAccessException e) {
                throw ClassUtil.sneakyThrow(e);
            }
            delegate = () -> mh;
            initialized = true;
        }
        return delegate.get();
    }

    protected MethodHandle postprocess(MethodHandle mh) {
        if (mh == null) {
            return mh;
        }
        if (asType == null) {
            return mh.asFixedArity();
        }
        return mh.asType(asType);
    }

    protected abstract MethodHandle unreflect() throws IllegalAccessException;

    @Override
    public String toString() {
        return get().toString();
    }
}
