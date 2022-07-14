package tools.jackson.databind.introspect;

import java.util.HashMap;
import java.util.Map;

import tools.jackson.core.util.Snapshottable;
import tools.jackson.databind.type.ClassKey;

/**
 * Basic {@link MixInResolver} implementation that both allows simple "local"
 * override definitions (with simple Mix-in class over Target class mapping)
 * and allows optional custom overrides for lookup.
 *<p>
 * Implementation is only thread-safe after initialization (that is,
 * when underlying Map is not modified but only read).
 */
public class MixInHandler
    implements MixInResolver,
        java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * External resolver that gets called before looking at any locally defined
     * mix-in target classes.
     */
    protected final MixInResolver _overrides;

    /**
     * Simple mix-in targets defined locally.
     */
    protected Map<ClassKey,Class<?>> _localMixIns;

    /*
    /**********************************************************************
    /* Construction, mutant factories
    /**********************************************************************
     */

    public MixInHandler(MixInResolver overrides) {
        _overrides = overrides;
    }

    protected MixInHandler(MixInResolver overrides,
            Map<ClassKey,Class<?>> mixins) {
        _overrides = overrides;
        _localMixIns = mixins;
    }

    /**
     * Mutant factory for constructor a new resolver instance with given
     * mix-in resolver override.
     */
    public MixInHandler withOverrides(MixInResolver overrides) {
        return new MixInHandler(overrides, _localMixIns);
    }

    
    /**
     * Mutant factory method that constructs a new instance that has no locally
     * defined mix-in/target mappings.
     */
    public MixInHandler withoutLocalDefinitions() {
        return new MixInHandler(_overrides, null);
    }
    
    /*
    /**********************************************************************
    /* Mutators
    /**********************************************************************
     */

    public MixInHandler addLocalDefinitions(Map<Class<?>, Class<?>> sourceMixins) {
        if (!sourceMixins.isEmpty()) {
            if (_localMixIns == null) {
                _localMixIns = new HashMap<>(sourceMixins.size());
            }
            for (Map.Entry<Class<?>,Class<?>> en : sourceMixins.entrySet()) {
                _localMixIns.put(new ClassKey(en.getKey()), en.getValue());
            }
        }
        return this;
    }

    public MixInHandler addLocalDefinition(Class<?> target, Class<?> mixinSource) {
        if (_localMixIns == null) {
            _localMixIns = new HashMap<ClassKey,Class<?>>();
        }
        _localMixIns.put(new ClassKey(target), mixinSource);
        return this;
    }

    public MixInHandler clearLocalDefinitions(Map<Class<?>, Class<?>> sourceMixins) {
        _localMixIns = null;
        return this;
    }

    /*
    /**********************************************************************
    /* MixInResolver API implementation
    /**********************************************************************
     */

    @Override
    public MixInHandler snapshot() {
        MixInResolver overrides = Snapshottable.takeSnapshot(_overrides);
        Map<ClassKey,Class<?>> mixIns = (_localMixIns == null)
                ? null : new HashMap<ClassKey,Class<?>>(_localMixIns);
        return new MixInHandler(overrides, mixIns);
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

    @Override
    public boolean hasMixIns() {
        return (_localMixIns != null)
                || ((_overrides != null) && _overrides.hasMixIns());
    }

    /*
    /**********************************************************************
    /* Other
    /**********************************************************************
     */

    public int localSize() { // for tests
        return (_localMixIns == null) ? 0 : _localMixIns.size();
    }
}
