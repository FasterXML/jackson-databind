package com.fasterxml.jackson.databind.cfg;

import java.util.Map;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.fasterxml.jackson.databind.type.ClassKey;
import com.fasterxml.jackson.databind.type.TypeFactory;

public abstract class MapperConfigBase<CFG extends MapperConfig.ConfigFeature,
    T extends MapperConfigBase<CFG,T>>
    extends MapperConfig<T>
{
    /*
    /**********************************************************
    /* Immutable config
    /**********************************************************
     */

    protected int _featureFlags;

    /**
     * Mix-in annotation mappings to use, if any: immutable,
     * can not be changed once defined.
     */
    protected final Map<ClassKey,Class<?>> _mixInAnnotations;

    /*
    /**********************************************************
    /* "Late bound" settings
    /**********************************************************
     */

    /**
     * Registered concrete subtypes that can be used instead of (or
     * in addition to) ones declared using annotations.
     * Unlike most other settings, it is not configured as early
     * as it is set, but rather only when a non-shared instance
     * is constructed by <code>ObjectMapper</code> (or -Reader
     * or -Writer)
     *<p>
     * Note: this is the only property left as non-final, to allow
     * lazy construction of the instance as necessary.
     */
    protected SubtypeResolver _subtypeResolver;
    
    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */
    
    protected MapperConfigBase(ClassIntrospector<? extends BeanDescription> ci, AnnotationIntrospector ai,
            VisibilityChecker<?> vc, SubtypeResolver str, PropertyNamingStrategy pns, TypeFactory tf,
            HandlerInstantiator hi,
            int defaultFeatures, Map<ClassKey,Class<?>> mixins)
    {
        super(ci, ai, vc, pns, tf, hi);
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

    /**
     * Accessor for object used for finding out all reachable subtypes
     * for supertypes; needed when a logical type name is used instead
     * of class name (or custom scheme).
     */
    public final SubtypeResolver getSubtypeResolver() {
        if (_subtypeResolver == null) {
            _subtypeResolver = new StdSubtypeResolver();
        }
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