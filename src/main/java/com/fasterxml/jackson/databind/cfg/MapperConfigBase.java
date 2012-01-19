package com.fasterxml.jackson.databind.cfg;

import java.util.Map;

import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.type.ClassKey;

public abstract class MapperConfigBase<CFG extends ConfigFeature,
    T extends MapperConfigBase<CFG,T>>
    extends MapperConfig<T>
{
    /*
    /**********************************************************
    /* Immutable config
    /**********************************************************
     */

    /**
     * Set of features enabled; actual type (kind of features)
     * depends on sub-classes.
     */
    protected int _featureFlags;

    /**
     * Mix-in annotation mappings to use, if any: immutable,
     * can not be changed once defined.
     */
    protected final Map<ClassKey,Class<?>> _mixInAnnotations;

    /**
     * Registered concrete subtypes that can be used instead of (or
     * in addition to) ones declared using annotations.
     */
    protected final SubtypeResolver _subtypeResolver;
    
    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    protected MapperConfigBase(BaseSettings base,
            int defaultFeatures, SubtypeResolver str, Map<ClassKey,Class<?>> mixins)
    {
        super(base);
        _featureFlags = defaultFeatures;
        _mixInAnnotations = mixins;
        _subtypeResolver = str;
    }
    
    protected MapperConfigBase(MapperConfigBase<CFG,T> src) {
        super(src._base);
        _featureFlags = src._featureFlags;
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = src._subtypeResolver;
    }
    
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, int features) {
        super(src._base);
        _featureFlags = features;
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = src._subtypeResolver;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, SubtypeResolver str) {
        super(src._base);
        _featureFlags = src._featureFlags;
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = str;
    }
    
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, SubtypeResolver str,
            int features)
    {
        super(src._base);
        _featureFlags = features;
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = str;
    }
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, BaseSettings base, SubtypeResolver str)
    {
        super(base);
        _featureFlags = src._featureFlags;
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = str;
    }
    
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, BaseSettings base, SubtypeResolver str,
            int features)
    {
        super(base);
        _featureFlags = features;
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = str;
    }
    
    /*
    /**********************************************************
    /* Additional fluent-factory methods
    /**********************************************************
     */
    
    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public abstract T with(CFG... features);
    
    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public abstract T without(CFG... features);

    /*
    /**********************************************************
    /* Simple accessors
    /**********************************************************
     */

    public final int getFeatureFlags() {
        return _featureFlags;
    }

    /**
     * Accessor for object used for finding out all reachable subtypes
     * for supertypes; needed when a logical type name is used instead
     * of class name (or custom scheme).
     */
    public final SubtypeResolver getSubtypeResolver() {
        return _subtypeResolver;
    }
    
    /*
    /**********************************************************
    /* ClassIntrospector.MixInResolver impl:
    /**********************************************************
     */

    /**
     * Method that will check if there are "mix-in" classes (with mix-in
     * annotations) for given class
     */
    @Override
    public final Class<?> findMixInClassFor(Class<?> cls) {
        return (_mixInAnnotations == null) ? null : _mixInAnnotations.get(new ClassKey(cls));
    }

    public final int mixInCount() {
        return (_mixInAnnotations == null) ? 0 : _mixInAnnotations.size();
    }
}