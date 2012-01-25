package com.fasterxml.jackson.databind.cfg;

import java.text.DateFormat;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.HandlerInstantiator;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.ClassKey;
import com.fasterxml.jackson.databind.type.TypeFactory;

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

    /**
     * Explicitly definite root name to use, if any; if empty
     * String, will disable root-name wrapping; if null, will
     * use defaults
     */
    protected final String _rootName;

    /**
     * View to use for filtering out properties to serialize
     * or deserialize.
     * Null if none (will also be assigned null if <code>Object.class</code>
     * is defined), meaning that all properties are to be included.
     */
    protected final Class<?> _view;
    
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
        _rootName = null;
        _view = null;
    }
    
    /**
     * Pass-through constructor used when no changes are needed to the
     * base class.
     */
    protected MapperConfigBase(MapperConfigBase<CFG,T> src)
    {
        super(src);
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = src._subtypeResolver;
        _rootName = src._rootName;
        _view = src._view;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, BaseSettings base)
    {
        super(base, src._mapperFeatures);
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = src._subtypeResolver;
        _rootName = src._rootName;
        _view = src._view;
    }
    
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, int mapperFeatures)
    {
        super(src._base, mapperFeatures);
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = src._subtypeResolver;
        _rootName = src._rootName;
        _view = src._view;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, SubtypeResolver str) {
        super(src);
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = str;
        _rootName = src._rootName;
        _view = src._view;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, String rootName) {
        super(src);
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = src._subtypeResolver;
        _rootName = rootName;
        _view = src._view;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, Class<?> view)
    {
        super(src);
        _mixInAnnotations = src._mixInAnnotations;
        _subtypeResolver = src._subtypeResolver;
        _rootName = src._rootName;
        _view = view;
    }
    
    /*
    /**********************************************************
    /* Addition fluent factory methods, common to all sub-types
    /**********************************************************
     */

    /**
     * Method for constructing and returning a new instance with different
     * {@link AnnotationIntrospector} to use (replacing old one).
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T withAnnotationIntrospector(AnnotationIntrospector ai);

    /**
     * Method for constructing and returning a new instance with additional
     * {@link AnnotationIntrospector} appended (as the lowest priority one)
     */
    public abstract T withAppendedAnnotationIntrospector(AnnotationIntrospector introspector);

    /**
     * Method for constructing and returning a new instance with additional
     * {@link AnnotationIntrospector} inserted (as the highest priority one)
     */
    public abstract T withInsertedAnnotationIntrospector(AnnotationIntrospector introspector);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link ClassIntrospector}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T withClassIntrospector(ClassIntrospector ci);

    /**
     * Method for constructing and returning a new instance with different
     * {@link DateFormat}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T withDateFormat(DateFormat df);

    /**
     * Method for constructing and returning a new instance with different
     * {@link HandlerInstantiator}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T withHandlerInstantiator(HandlerInstantiator hi);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link PropertyNamingStrategy}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T withPropertyNamingStrategy(PropertyNamingStrategy strategy);
    
    /**
     * Method for constructing and returning a new instance with different
     * root name to use (none, if null).
     *<p>
     * Note that when a root name is set to a non-Empty String, this will automatically force use
     * of root element wrapping with given name. If empty String passed, will
     * disable root name wrapping; and if null used, will instead use
     * <code>Feature</code> to determine if to use wrapping, and annotation
     * (or default name) for actual root name to use.
     * 
     * @param rootName to use: if null, means "use default" (clear setting);
     *   if empty String ("") means that no root name wrapping is used;
     *   otherwise defines root name to use.
     */
    public abstract T withRootName(String rootName);

    /**
     * Method for constructing and returning a new instance with different
     * {@link SubtypeResolver}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T withSubtypeResolver(SubtypeResolver str);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link TypeFactory}
     * to use.
     */
    public abstract T withTypeFactory(TypeFactory typeFactory);

    /**
     * Method for constructing and returning a new instance with different
     * {@link TypeResolverBuilder} to use.
     */
    public abstract T withTypeResolverBuilder(TypeResolverBuilder<?> trb);

    /**
     * Method for constructing and returning a new instance with different
     * view to use.
     */
    public abstract T withView(Class<?> view);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link VisibilityChecker}
     * to use.
     */
    public abstract T withVisibilityChecker(VisibilityChecker<?> vc);

    /**
     * Method for constructing and returning a new instance with different
     * minimal visibility level for specified property type
     */
    public abstract T withVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility);
    
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

    public final String getRootName() {
        return _rootName;
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