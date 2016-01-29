package com.fasterxml.jackson.databind.introspect;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.type.ClassKey;

/**
 * Simple implementation of {@link ClassIntrospector.MixInResolver}
 * that just uses a {@link java.util.Map} for containing mapping
 * from target to mix-in classes.
 *<p>
 * Implementation is only thread-safe after initialization (that is,
 * when underlying Map is not modified but only read).
 * 
 * @since 2.6
 */
public class SimpleMixInResolver
    implements ClassIntrospector.MixInResolver,
        java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * External resolver that gets called before looking at any locally defined
     * mix-in target classes.
     */
    protected final ClassIntrospector.MixInResolver _overrides;

    /**
     * Simple mix-in targets defined locally.
     */
    protected Map<ClassKey,Class<?>> _localMixIns;

    public SimpleMixInResolver(ClassIntrospector.MixInResolver overrides) {
        _overrides = overrides;
    }

    protected SimpleMixInResolver(ClassIntrospector.MixInResolver overrides,
            Map<ClassKey,Class<?>> mixins) {
        _overrides = overrides;
        _localMixIns = mixins;
    }

    /**
     * Mutant factory for constructor a new resolver instance with given
     * mix-in resolver override.
     */
    public SimpleMixInResolver withOverrides(ClassIntrospector.MixInResolver overrides) {
        return new SimpleMixInResolver(overrides, _localMixIns);
    }

    /**
     * Mutant factory method that constructs a new instance that has no locally
     * defined mix-in/target mappings.
     */
    public SimpleMixInResolver withoutLocalDefinitions() {
        return new SimpleMixInResolver(_overrides, null);
    }

    public void setLocalDefinitions(Map<Class<?>, Class<?>> sourceMixins) {
        if (sourceMixins == null || sourceMixins.isEmpty()) {
            _localMixIns = null;
        } else {
            Map<ClassKey,Class<?>> mixIns = new HashMap<ClassKey,Class<?>>(sourceMixins.size());
            for (Map.Entry<Class<?>,Class<?>> en : sourceMixins.entrySet()) {
                mixIns.put(new ClassKey(en.getKey()), en.getValue());
            }
            _localMixIns = mixIns;
        }
    }

    public void addLocalDefinition(Class<?> target, Class<?> mixinSource) {
        if (_localMixIns == null) {
            _localMixIns = new HashMap<ClassKey,Class<?>>();
        }
        _localMixIns.put(new ClassKey(target), mixinSource);
    }

    @Override
    public SimpleMixInResolver copy() {
        ClassIntrospector.MixInResolver overrides = (_overrides == null)
                ? null : _overrides.copy();
        Map<ClassKey,Class<?>> mixIns = (_localMixIns == null)
                ? null : new HashMap<ClassKey,Class<?>>(_localMixIns);
        return new SimpleMixInResolver(overrides, mixIns);
    }

    @Override
    public Class<?> findMixInClassFor(Class<?> cls)
    {
        Class<?> mixin = (_overrides == null) ? null : _overrides.findMixInClassFor(cls);
        if (mixin == null && (_localMixIns != null)) {
            mixin = _localMixIns.get(new ClassKey(cls));
        }
        return mixin;
    }

    public int localSize() {
        return (_localMixIns == null) ? 0 : _localMixIns.size();
    }
}
