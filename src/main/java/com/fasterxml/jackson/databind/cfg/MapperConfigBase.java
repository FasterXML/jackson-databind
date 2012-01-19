package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

public abstract class MapperConfigBase<CFG extends MapperConfig.ConfigFeature,
    T extends MapperConfigBase<CFG,T>>
    extends MapperConfig<T>
{
    protected int _featureFlags;
    
    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */
    
    protected MapperConfigBase(ClassIntrospector<? extends BeanDescription> ci, AnnotationIntrospector ai,
            VisibilityChecker<?> vc, SubtypeResolver str, PropertyNamingStrategy pns, TypeFactory tf,
            HandlerInstantiator hi,
            int defaultFeatures)
    {
        super(ci, ai, vc, str, pns, tf, hi);
        _featureFlags = defaultFeatures;
    }
    
    protected MapperConfigBase(MapperConfigBase<CFG,T> src) {
        super(src);
        _featureFlags = src._featureFlags;
    }
    
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, int features) {
        super(src);
        _featureFlags = features;
    }
    
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, BaseSettings base, SubtypeResolver str)
    {
        super(src, base, str);
        _featureFlags = src._featureFlags;
    }
    
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, BaseSettings base, SubtypeResolver str,
            int features)
    {
        super(src, base, str);
        _featureFlags = features;
    }
    
    /**
     * Method that calculates bit set (flags) of all features that
     * are enabled by default.
     */
    protected static <F extends Enum<F> & MapperConfig.ConfigFeature> int collectFeatureDefaults(Class<F> enumClass)
    {
        int flags = 0;
        for (F value : enumClass.getEnumConstants()) {
            if (value.enabledByDefault()) {
                flags |= value.getMask();
            }
        }
        return flags;
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
}