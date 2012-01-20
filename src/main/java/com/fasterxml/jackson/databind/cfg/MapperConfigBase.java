package com.fasterxml.jackson.databind.cfg;

import java.util.Map;

import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.type.ClassKey;

public abstract class MapperConfigBase<CFG extends ConfigFeature,
    T extends MapperConfigBase<CFG,T>>
    extends MapperConfig<T>
{
    private final static int DEFAULT_MAPPER_FEATURES = collectFeatureDefaults(MapperConfig.Feature.class);

    /*
    /**********************************************************
    /* Immutable config
    /**********************************************************
     */

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

    /**
     * Constructor used when creating a new instance (compared to
     * that of creating fluent copies)
     */
    protected MapperConfigBase(BaseSettings base,
            SubtypeResolver str, Map<ClassKey,Class<?>> mixins)
    {
        super(base, DEFAULT_MAPPER_FEATURES);
        _mixInAnnotations = mixins;
        _subtypeResolver = str;
    }
    
    /**
     * Pass-through constructor used when no changes are needed to the
     * base class.
     */
    protected MapperConfigBase(MapperConfigBase<CFG,T> src) {
        super(src);
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = src._subtypeResolver;
    }
    
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, int mapperFeatures)
    {
        super(src._base, mapperFeatures);
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = src._subtypeResolver;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, SubtypeResolver str) {
        super(src);
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = str;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, BaseSettings base)
    {
        super(base, src._mapperFeatures);
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = src._subtypeResolver;
    }

    /*
    /**********************************************************
    /* Simple accessors
    /**********************************************************
     */

    public abstract int getFeatureFlags();

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