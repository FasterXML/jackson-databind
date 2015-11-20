package com.fasterxml.jackson.databind.cfg;

import java.text.DateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector.MixInResolver;
import com.fasterxml.jackson.databind.introspect.SimpleMixInResolver;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.RootNameLookup;

@SuppressWarnings("serial")
public abstract class MapperConfigBase<CFG extends ConfigFeature,
    T extends MapperConfigBase<CFG,T>>
    extends MapperConfig<T>
    implements java.io.Serializable
{
    private final static int DEFAULT_MAPPER_FEATURES = collectFeatureDefaults(MapperFeature.class);

    /*
    /**********************************************************
    /* Immutable config
    /**********************************************************
     */

    /**
     * Mix-in annotation mappings to use, if any: immutable,
     * can not be changed once defined.
     * 
     * @since 2.6
     */
    protected final SimpleMixInResolver _mixIns;

    /**
     * Registered concrete subtypes that can be used instead of (or
     * in addition to) ones declared using annotations.
     */
    protected final SubtypeResolver _subtypeResolver;

    /**
     * Explicitly defined root name to use, if any; if empty
     * String, will disable root-name wrapping; if null, will
     * use defaults
     */
    protected final PropertyName _rootName;

    /**
     * View to use for filtering out properties to serialize
     * or deserialize.
     * Null if none (will also be assigned null if <code>Object.class</code>
     * is defined), meaning that all properties are to be included.
     */
    protected final Class<?> _view;

    /**
     * Contextual attributes accessible (get and set) during processing,
     * on per-call basis.
     * 
     * @since 2.3
     */
    protected final ContextAttributes _attributes;

    /**
     * @since 2.6
     */
    protected final RootNameLookup _rootNames;
    
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
            SubtypeResolver str, SimpleMixInResolver mixins,
            RootNameLookup rootNames)
    {
        super(base, DEFAULT_MAPPER_FEATURES);
        _mixIns = mixins;
        _subtypeResolver = str;
        _rootNames = rootNames;
        _rootName = null;
        _view = null;
        // default to "no attributes"
        _attributes = ContextAttributes.getEmpty();
    }
    
    /**
     * Pass-through constructor used when no changes are needed to the
     * base class.
     */
    protected MapperConfigBase(MapperConfigBase<CFG,T> src)
    {
        super(src);
        _mixIns = src._mixIns;
        _subtypeResolver = src._subtypeResolver;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = src._attributes;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, BaseSettings base)
    {
        super(src, base);
        _mixIns = src._mixIns;
        _subtypeResolver = src._subtypeResolver;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = src._attributes;
    }
    
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, int mapperFeatures)
    {
        super(src, mapperFeatures);
        _mixIns = src._mixIns;
        _subtypeResolver = src._subtypeResolver;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = src._attributes;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, SubtypeResolver str) {
        super(src);
        _mixIns = src._mixIns;
        _subtypeResolver = str;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = src._attributes;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, PropertyName rootName) {
        super(src);
        _mixIns = src._mixIns;
        _subtypeResolver = src._subtypeResolver;
        _rootNames = src._rootNames;
        _rootName = rootName;
        _view = src._view;
        _attributes = src._attributes;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, Class<?> view)
    {
        super(src);
        _mixIns = src._mixIns;
        _subtypeResolver = src._subtypeResolver;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = view;
        _attributes = src._attributes;
    }

    /**
     * @since 2.1
     */
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, SimpleMixInResolver mixins)
    {
        super(src);
        _mixIns = mixins;
        _subtypeResolver = src._subtypeResolver;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = src._attributes;
    }
    
    /**
     * @since 2.3
     */
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, ContextAttributes attr)
    {
        super(src);
        _mixIns = src._mixIns;
        _subtypeResolver = src._subtypeResolver;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = attr;
    }

    /**
     * @since 2.6
     */
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, SimpleMixInResolver mixins,
            RootNameLookup rootNames)
    {
        super(src);
        _mixIns = mixins;
        _subtypeResolver = src._subtypeResolver;
        _rootNames = rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = src._attributes;
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
    public abstract T with(AnnotationIntrospector ai);

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
    public abstract T with(ClassIntrospector ci);

    /**
     * Method for constructing and returning a new instance with different
     * {@link DateFormat}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T with(DateFormat df);

    /**
     * Method for constructing and returning a new instance with different
     * {@link HandlerInstantiator}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T with(HandlerInstantiator hi);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link PropertyNamingStrategy}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T with(PropertyNamingStrategy strategy);
    
    /**
     * Method for constructing and returning a new instance with different
     * root name to use (none, if null).
     *<p>
     * Note that when a root name is set to a non-Empty String, this will automatically force use
     * of root element wrapping with given name. If empty String passed, will
     * disable root name wrapping; and if null used, will instead use
     * <code>SerializationFeature</code> to determine if to use wrapping, and annotation
     * (or default name) for actual root name to use.
     * 
     * @param rootName to use: if null, means "use default" (clear setting);
     *   if empty String ("") means that no root name wrapping is used;
     *   otherwise defines root name to use.
     *   
     * @since 2.6
     */
    public abstract T withRootName(PropertyName rootName);

    public T withRootName(String rootName) {
        if (rootName == null) {
            return withRootName((PropertyName) null);
        }
        return withRootName(PropertyName.construct(rootName));
    }
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link SubtypeResolver}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T with(SubtypeResolver str);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link TypeFactory}
     * to use.
     */
    public abstract T with(TypeFactory typeFactory);

    /**
     * Method for constructing and returning a new instance with different
     * {@link TypeResolverBuilder} to use.
     */
    public abstract T with(TypeResolverBuilder<?> trb);

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
    public abstract T with(VisibilityChecker<?> vc);

    /**
     * Method for constructing and returning a new instance with different
     * minimal visibility level for specified property type
     */
    public abstract T withVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility);

    /**
     * Method for constructing and returning a new instance with different
     * default {@link java.util.Locale} to use for formatting.
     */
    public abstract T with(Locale l);

    /**
     * Method for constructing and returning a new instance with different
     * default {@link java.util.TimeZone} to use for formatting of date values.
     */
    public abstract T with(TimeZone tz);

    /**
     * Method for constructing and returning a new instance with different
     * default {@link Base64Variant} to use with base64-encoded binary values.
     */
    public abstract T with(Base64Variant base64);

    /**
     * Method for constructing an instance that has specified
     * contextual attributes.
     * 
     * @since 2.3
     */
    public abstract T with(ContextAttributes attrs);

    /**
     * Method for constructing an instance that has only specified
     * attributes, removing any attributes that exist before the call.
     * 
     * @since 2.3
     */
    public T withAttributes(Map<?,?> attributes) {
        return with(getAttributes().withSharedAttributes(attributes));
    }
    
    /**
     * Method for constructing an instance that has specified
     * value for attribute for given key.
     * 
     * @since 2.3
     */
    public T withAttribute(Object key, Object value) {
        return with(getAttributes().withSharedAttribute(key, value));
    }

    /**
     * Method for constructing an instance that has no
     * value for attribute for given key.
     * 
     * @since 2.3
     */
    public T withoutAttribute(Object key) {
        return with(getAttributes().withoutSharedAttribute(key));
    }
    
    /*
    /**********************************************************
    /* Simple accessors
    /**********************************************************
     */
    
    /**
     * Accessor for object used for finding out all reachable subtypes
     * for supertypes; needed when a logical type name is used instead
     * of class name (or custom scheme).
     */
    @Override
    public final SubtypeResolver getSubtypeResolver() {
        return _subtypeResolver;
    }

    /**
     * @deprecated Since 2.6 use {@link #getFullRootName} instead.
     */
    @Deprecated // since 2.6
    public final String getRootName() {
        return (_rootName == null) ? null : _rootName.getSimpleName();
    }

    /**
     * @since 2.6
     */
    public final PropertyName getFullRootName() {
        return _rootName;
    }

    @Override
    public final Class<?> getActiveView() {
        return _view;
    }

    @Override
    public final ContextAttributes getAttributes() {
        return _attributes;
    }

    /*
    /**********************************************************
    /* Other config access
    /**********************************************************
     */

    @Override
    public PropertyName findRootName(JavaType rootType) {
        if (_rootName != null) {
            return _rootName;
        }
        return _rootNames.findRootName(rootType, this);
    }

    @Override
    public PropertyName findRootName(Class<?> rawRootType) {
        if (_rootName != null) {
            return _rootName;
        }
        return _rootNames.findRootName(rawRootType, this);
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
        return _mixIns.findMixInClassFor(cls);
    }

    // Not really relevant here (should not get called)
    @Override
    public MixInResolver copy() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Test-only method -- does not reflect possibly open-ended set that external
     * mix-in resolver might provide.
     */
    public final int mixInCount() {
        return _mixIns.localSize();
    }
}
