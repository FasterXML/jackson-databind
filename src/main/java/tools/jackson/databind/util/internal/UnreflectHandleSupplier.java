package tools.jackson.databind.util.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

import tools.jackson.databind.util.ClassUtil;

/**
 * Lazy, memoized holder for a {@link MethodHandle} unreflected from a
 * {@link java.lang.reflect.Member}.
 *<p>
 * Resolution is deferred until first actual use, for two reasons:
 *<ul>
 * <li>Access to non-public members is only enabled by
 *   {@code ClassUtil.checkAndFixAccess()}, which callers apply via
 *   {@code AnnotatedMember.fixAccess()} <i>after</i> the owning member has been
 *   constructed. Unreflecting during construction could hence fail.
 *   </li>
 * <li>Many members are introspected but never actually invoked, so the handle
 *   lookup is often avoidable altogether.
 *   </li>
 * </ul>
 * The resolved handle is adapted once -- to the {@link MethodType} given to the
 * constructor, or {@link MethodHandle#asFixedArity()} if that is {@code null} --
 * and then memoized; resolution happens at most once per holder.
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
