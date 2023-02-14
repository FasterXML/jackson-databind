package com.fasterxml.jackson.databind.cfg;

import java.text.DateFormat;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.Base64Variant;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector.MixInResolver;
import com.fasterxml.jackson.databind.introspect.AccessorNamingStrategy;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.SimpleMixInResolver;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.RootNameLookup;

@SuppressWarnings("serial")
public abstract class MapperConfigBase<CFG extends ConfigFeature,
    T extends MapperConfigBase<CFG,T>>
    extends MapperConfig<T>
    implements java.io.Serializable
{
    /**
     * @since 2.9
     */
    protected final static ConfigOverride EMPTY_OVERRIDE = ConfigOverride.empty();

    private final static long DEFAULT_MAPPER_FEATURES = MapperFeature.collectLongDefaults();

    /**
     * @since 2.9
     */
    private final static long AUTO_DETECT_MASK =
            MapperFeature.AUTO_DETECT_FIELDS.getLongMask()
            | MapperFeature.AUTO_DETECT_GETTERS.getLongMask()
            | MapperFeature.AUTO_DETECT_IS_GETTERS.getLongMask()
            | MapperFeature.AUTO_DETECT_SETTERS.getLongMask()
            | MapperFeature.AUTO_DETECT_CREATORS.getLongMask()
            ;

    /*
    /**********************************************************
    /* Immutable config
    /**********************************************************
     */

    /**
     * Mix-in annotation mappings to use, if any: immutable,
     * cannot be changed once defined.
     *
     * @since 2.6
     */
    protected final SimpleMixInResolver _mixIns;

    /**
     * Registered concrete subtypes that can be used instead of (or
     * in addition to) ones declared using annotations.
     *<p>
     * Note that instances are stateful and as such may need to be copied,
     * and may NOT be demoted down to {@link BaseSettings}.
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
     * Simple cache used for finding out possible root name for root name
     * wrapping.
     *<p>
     * Note that instances are stateful (for caching) and as such may need to be copied,
     * and may NOT be demoted down to {@link BaseSettings}.
     *
     * @since 2.6
     */
    protected final RootNameLookup _rootNames;

    /**
     * Configuration overrides to apply, keyed by type of property.
     *
     * @since 2.8
     */
    protected final ConfigOverrides _configOverrides;

    /**
     * Set of {@link DatatypeFeature}s enabled.
     *
     * @since 2.14
     */
    protected final DatatypeFeatures _datatypeFeatures;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    /**
     * Constructor used when creating a new instance (compared to
     * that of creating fluent copies)
     *
     * @since 2.14
     */
    protected MapperConfigBase(BaseSettings base,
            SubtypeResolver str, SimpleMixInResolver mixins, RootNameLookup rootNames,
            ConfigOverrides configOverrides, DatatypeFeatures datatypeFeatures)
    {
        super(base, DEFAULT_MAPPER_FEATURES);
        _mixIns = mixins;
        _subtypeResolver = str;
        _rootNames = rootNames;
        _rootName = null;
        _view = null;
        // default to "no attributes"
        _attributes = ContextAttributes.getEmpty();
        _configOverrides = configOverrides;
        _datatypeFeatures = datatypeFeatures;
    }

    /**
     * Copy constructor usually called to make a copy for use by
     * ObjectMapper that is copied.
     *
     * @since 2.14
     */
    protected MapperConfigBase(MapperConfigBase<CFG,T> src,
            SubtypeResolver str, SimpleMixInResolver mixins, RootNameLookup rootNames,
            ConfigOverrides configOverrides)
    {
        // 18-Apr-2018, tatu: [databind#1898] need to force copying of `ClassIntrospector`
        //    (to clear its cache) to avoid leakage
        super(src, src._base.copy());
        _mixIns = mixins;
        _subtypeResolver = str;
        _rootNames = rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = src._attributes;
        _configOverrides = configOverrides;
        _datatypeFeatures = src._datatypeFeatures;
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
        _configOverrides = src._configOverrides;
        _datatypeFeatures = src._datatypeFeatures;
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
        _configOverrides = src._configOverrides;
        _datatypeFeatures = src._datatypeFeatures;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, long mapperFeatures)
    {
        super(src, mapperFeatures);
        _mixIns = src._mixIns;
        _subtypeResolver = src._subtypeResolver;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = src._attributes;
        _configOverrides = src._configOverrides;
        _datatypeFeatures = src._datatypeFeatures;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, SubtypeResolver str) {
        super(src);
        _mixIns = src._mixIns;
        _subtypeResolver = str;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = src._attributes;
        _configOverrides = src._configOverrides;
        _datatypeFeatures = src._datatypeFeatures;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, PropertyName rootName) {
        super(src);
        _mixIns = src._mixIns;
        _subtypeResolver = src._subtypeResolver;
        _rootNames = src._rootNames;
        _rootName = rootName;
        _view = src._view;
        _attributes = src._attributes;
        _configOverrides = src._configOverrides;
        _datatypeFeatures = src._datatypeFeatures;
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
        _configOverrides = src._configOverrides;
        _datatypeFeatures = src._datatypeFeatures;
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
        _configOverrides = src._configOverrides;
        _datatypeFeatures = src._datatypeFeatures;
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
        _configOverrides = src._configOverrides;
        _datatypeFeatures = src._datatypeFeatures;
    }

    /**
     * @since 2.14
     */
    protected MapperConfigBase(MapperConfigBase<CFG,T> src, DatatypeFeatures datatypeFeatures)
    {
        super(src);
        _mixIns = src._mixIns;
        _subtypeResolver = src._subtypeResolver;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = src._attributes;
        _configOverrides = src._configOverrides;
        _datatypeFeatures = datatypeFeatures;
    }

    /*
    /**********************************************************************
    /* Abstract fluent factory methods to be implemented by subtypes
    /**********************************************************************
     */

    /**
     * @since 2.9 (in this case, demoted from sub-classes)
     */
    protected abstract T _withBase(BaseSettings newBase);

    /**
     * @since 2.9 (in this case, demoted from sub-classes)
     */
    protected abstract T _withMapperFeatures(long mapperFeatures);

    /**
     * @since 2.14
     */
    protected abstract T _with(DatatypeFeatures dtFeatures);

    /**
     * @since 2.14
     */
    protected DatatypeFeatures _datatypeFeatures() {
        return _datatypeFeatures;
    }

    /*
    /**********************************************************************
    /* Additional shared fluent factory methods; MapperFeatures
    /**********************************************************************
     */

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    @SuppressWarnings("unchecked")
    @Override
    public final T with(MapperFeature... features)
    {
        long newMapperFlags = _mapperFeatures;
        for (MapperFeature f : features) {
            newMapperFlags |= f.getLongMask();
        }
        if (newMapperFlags == _mapperFeatures) {
            return (T) this;
        }
        return _withMapperFeatures(newMapperFlags);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    @SuppressWarnings("unchecked")
    @Override
    public final T without(MapperFeature... features)
    {
        long newMapperFlags = _mapperFeatures;
        for (MapperFeature f : features) {
             newMapperFlags &= ~f.getLongMask();
        }
        if (newMapperFlags == _mapperFeatures) {
            return (T) this;
        }
        return _withMapperFeatures(newMapperFlags);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final T with(MapperFeature feature, boolean state)
    {
        long newMapperFlags;
        if (state) {
            newMapperFlags = _mapperFeatures | feature.getLongMask();
        } else {
            newMapperFlags = _mapperFeatures & ~feature.getLongMask();
        }
        if (newMapperFlags == _mapperFeatures) {
            return (T) this;
        }
        return _withMapperFeatures(newMapperFlags);
    }

    /*
    /**********************************************************************
    /* Additional shared fluent factory methods; DatatypeFeatures
    /**********************************************************************
     */

    /**
     * Fluent factory method that will return a configuration
     * object instance with specified feature enabled: this may be
     * {@code this} instance (if no changes effected), or a newly
     * constructed instance.
     */
    public final T with(DatatypeFeature feature) {
        return _with(_datatypeFeatures().with(feature));
    }

    /**
     * Fluent factory method that will return a configuration
     * object instance with specified features enabled: this may be
     * {@code this} instance (if no changes effected), or a newly
     * constructed instance.
     */
    public final T withFeatures(DatatypeFeature... features) {
        return _with(_datatypeFeatures().withFeatures(features));
    }

    /**
     * Fluent factory method that will return a configuration
     * object instance with specified feature disabled: this may be
     * {@code this} instance (if no changes effected), or a newly
     * constructed instance.
     */
    public final T without(DatatypeFeature feature) {
        return _with(_datatypeFeatures().without(feature));
    }

    /**
     * Fluent factory method that will return a configuration
     * object instance with specified features disabled: this may be
     * {@code this} instance (if no changes effected), or a newly
     * constructed instance.
     */
    public final T withoutFeatures(DatatypeFeature... features) {
        return _with(_datatypeFeatures().withoutFeatures(features));
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */

    public final T with(DatatypeFeature feature, boolean state)
    {
        DatatypeFeatures features = _datatypeFeatures();
        features = state ? features.with(feature) : features.without(feature);
        return _with(features);
    }

    /*
    /**********************************************************************
    /* Additional shared fluent factory methods; introspectors
    /**********************************************************************
     */

    /**
     * Method for constructing and returning a new instance with different
     * {@link AnnotationIntrospector} to use (replacing old one).
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public final T with(AnnotationIntrospector ai) {
        return _withBase(_base.withAnnotationIntrospector(ai));
    }

    /**
     * Method for constructing and returning a new instance with additional
     * {@link AnnotationIntrospector} appended (as the lowest priority one)
     */
    public final T withAppendedAnnotationIntrospector(AnnotationIntrospector ai) {
        return _withBase(_base.withAppendedAnnotationIntrospector(ai));
    }

    /**
     * Method for constructing and returning a new instance with additional
     * {@link AnnotationIntrospector} inserted (as the highest priority one)
     */
    public final T withInsertedAnnotationIntrospector(AnnotationIntrospector ai) {
        return _withBase(_base.withInsertedAnnotationIntrospector(ai));
    }

    /**
     * Method for constructing and returning a new instance with different
     * {@link ClassIntrospector}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public final T with(ClassIntrospector ci) {
        return _withBase(_base.withClassIntrospector(ci));
    }

    /*
    /**********************************************************************
    /* Additional shared fluent factory methods; attributes
    /**********************************************************************
     */

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
    /**********************************************************************
    /* Additional shared fluent factory methods; factories
    /**********************************************************************
     */

    /**
     * Method for constructing and returning a new instance with different
     * {@link TypeFactory}
     * to use.
     */
    public final T with(TypeFactory tf) {
        return _withBase( _base.withTypeFactory(tf));
    }

    /**
     * Method for constructing and returning a new instance with different
     * {@link TypeResolverBuilder} to use.
     */
    public final T with(TypeResolverBuilder<?> trb) {
        return _withBase(_base.withTypeResolverBuilder(trb));
    }

    /**
     * Method for constructing and returning a new instance with different
     * {@link PropertyNamingStrategy}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public final T with(PropertyNamingStrategy pns) {
        return _withBase(_base.withPropertyNamingStrategy(pns));
    }

    /**
     * Method for constructing and returning a new instance with different
     * {@link PropertyNamingStrategy}
     * to use.
     *
     * @since 2.12
     */
    public final T with(AccessorNamingStrategy.Provider p) {
        return _withBase(_base.withAccessorNaming(p));
    }

    /**
     * Method for constructing and returning a new instance with different
     * {@link HandlerInstantiator}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public final T with(HandlerInstantiator hi) {
        return _withBase(_base.withHandlerInstantiator(hi));
    }

    /*
    /**********************************************************************
    /* Additional shared fluent factory methods; other
    /**********************************************************************
     */

    /**
     * Method for constructing and returning a new instance with different
     * default {@link Base64Variant} to use with base64-encoded binary values.
     */
    public final T with(Base64Variant base64) {
        return _withBase(_base.with(base64));
    }

    /**
     * Method for constructing and returning a new instance with different
     * {@link DateFormat}
     * to use.
     *<p>
     * NOTE: non-final since <code>SerializationConfig</code> needs to override this
     */
    public T with(DateFormat df) {
        return _withBase(_base.withDateFormat(df));
    }

    /**
     * Method for constructing and returning a new instance with different
     * default {@link java.util.Locale} to use for formatting.
     */
    public final T with(Locale l) {
        return _withBase(_base.with(l));
    }

    /**
     * Method for constructing and returning a new instance with different
     * default {@link java.util.TimeZone} to use for formatting of date values.
     */
    public final T with(TimeZone tz) {
        return _withBase(_base.with(tz));
    }

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
     * view to use.
     */
    public abstract T withView(Class<?> view);

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    @Override
    public final DatatypeFeatures getDatatypeFeatures() {
        return _datatypeFeatures;
    }

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
    /**********************************************************************
    /* Configuration access; default/overrides
    /**********************************************************************
     */

    @Override
    public final ConfigOverride getConfigOverride(Class<?> type) {
        ConfigOverride override = _configOverrides.findOverride(type);
        return (override == null) ? EMPTY_OVERRIDE : override;
    }

    @Override
    public final ConfigOverride findConfigOverride(Class<?> type) {
        return _configOverrides.findOverride(type);
    }

    @Override
    public final JsonInclude.Value getDefaultPropertyInclusion() {
        return _configOverrides.getDefaultInclusion();
    }

    @Override
    public final JsonInclude.Value getDefaultPropertyInclusion(Class<?> baseType) {
        JsonInclude.Value v = getConfigOverride(baseType).getInclude();
        JsonInclude.Value def = getDefaultPropertyInclusion();
        if (def == null) {
            return v;
        }
        return def.withOverrides(v);
    }

    @Override
    public final JsonInclude.Value getDefaultInclusion(Class<?> baseType,
            Class<?> propertyType) {
        JsonInclude.Value v = getConfigOverride(propertyType).getIncludeAsProperty();
        JsonInclude.Value def = getDefaultPropertyInclusion(baseType);
        if (def == null) {
            return v;
        }
        return def.withOverrides(v);
    }

    @Override
    public final JsonFormat.Value getDefaultPropertyFormat(Class<?> type) {
        return _configOverrides.findFormatDefaults(type);
    }

    @Override
    public final JsonIgnoreProperties.Value getDefaultPropertyIgnorals(Class<?> type) {
        ConfigOverride overrides = _configOverrides.findOverride(type);
        if (overrides != null) {
            JsonIgnoreProperties.Value v = overrides.getIgnorals();
            if (v != null) {
                return v;
            }
        }
        // 01-May-2015, tatu: Could return `Value.empty()` but for now `null`
        //   seems simpler as callers can avoid processing.
        return null;
    }

    @Override
    public final JsonIgnoreProperties.Value getDefaultPropertyIgnorals(Class<?> baseType,
            AnnotatedClass actualClass)
    {
        AnnotationIntrospector intr = getAnnotationIntrospector();
        JsonIgnoreProperties.Value base = (intr == null) ? null
                : intr.findPropertyIgnoralByName(this, actualClass);
        JsonIgnoreProperties.Value overrides = getDefaultPropertyIgnorals(baseType);
        return JsonIgnoreProperties.Value.merge(base, overrides);
    }

    @Override
    public final JsonIncludeProperties.Value getDefaultPropertyInclusions(Class<?> baseType,
            AnnotatedClass actualClass)
    {
        AnnotationIntrospector intr = getAnnotationIntrospector();
        return (intr == null) ? null : intr.findPropertyInclusionByName(this, actualClass);
    }

    @Override
    public final VisibilityChecker<?> getDefaultVisibilityChecker()
    {
        VisibilityChecker<?> vchecker = _configOverrides.getDefaultVisibility();
        // then global overrides (disabling)
        // 05-Mar-2018, tatu: As per [databind#1947], need to see if any disabled
        if ((_mapperFeatures & AUTO_DETECT_MASK) != AUTO_DETECT_MASK) {
            if (!isEnabled(MapperFeature.AUTO_DETECT_FIELDS)) {
                vchecker = vchecker.withFieldVisibility(Visibility.NONE);
            }
            if (!isEnabled(MapperFeature.AUTO_DETECT_GETTERS)) {
                vchecker = vchecker.withGetterVisibility(Visibility.NONE);
            }
            if (!isEnabled(MapperFeature.AUTO_DETECT_IS_GETTERS)) {
                vchecker = vchecker.withIsGetterVisibility(Visibility.NONE);
            }
            if (!isEnabled(MapperFeature.AUTO_DETECT_SETTERS)) {
                vchecker = vchecker.withSetterVisibility(Visibility.NONE);
            }
            if (!isEnabled(MapperFeature.AUTO_DETECT_CREATORS)) {
                vchecker = vchecker.withCreatorVisibility(Visibility.NONE);
            }
        }
        return vchecker;
    }

    @Override // since 2.9
    public final VisibilityChecker<?> getDefaultVisibilityChecker(Class<?> baseType,
            AnnotatedClass actualClass)
    {
        // 14-Apr-2021, tatu: [databind#3117] JDK types should be limited
        //    to "public-only" regardless of settings for other types
        VisibilityChecker<?> vc;

        if (ClassUtil.isJDKClass(baseType)) {
            vc = VisibilityChecker.Std.allPublicInstance();
        } else {
            vc = getDefaultVisibilityChecker();
        }
        AnnotationIntrospector intr = getAnnotationIntrospector();
        if (intr != null) {
            vc = intr.findAutoDetectVisibility(actualClass, vc);
        }
        ConfigOverride overrides = _configOverrides.findOverride(baseType);
        if (overrides != null) {
            vc = vc.withOverrides(overrides.getVisibility()); // ok to pass null
        }
        return vc;
    }

    @Override
    public final JsonSetter.Value getDefaultSetterInfo() {
        return _configOverrides.getDefaultSetterInfo();
    }

    @Override
    public Boolean getDefaultMergeable() {
        return _configOverrides.getDefaultMergeable();
    }

    @Override
    public Boolean getDefaultMergeable(Class<?> baseType) {
        Boolean b;
        ConfigOverride cfg = _configOverrides.findOverride(baseType);
        if (cfg != null) {
            b = cfg.getMergeable();
            if (b != null) {
                return b;
            }
        }
        return _configOverrides.getDefaultMergeable();
    }

    /*
    /**********************************************************************
    /* Other config access
    /**********************************************************************
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
    /**********************************************************************
    /* ClassIntrospector.MixInResolver impl:
    /**********************************************************************
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
