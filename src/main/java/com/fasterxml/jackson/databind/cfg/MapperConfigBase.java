package com.fasterxml.jackson.databind.cfg;

import java.text.DateFormat;
import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.TypeResolverProvider;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.RootNameLookup;

@SuppressWarnings("serial")
public abstract class MapperConfigBase<CFG extends ConfigFeature,
    T extends MapperConfigBase<CFG,T>>
    extends MapperConfig<T>
    implements java.io.Serializable
{
    protected final static ConfigOverride EMPTY_OVERRIDE = ConfigOverride.empty();

    /*
    /**********************************************************************
    /* Immutable config, factories
    /**********************************************************************
     */

    /**
     * Specific factory used for creating {@link JavaType} instances;
     * needed to allow modules to add more custom type handling
     * (mostly to support types of non-Java JVM languages)
     */
    protected final TypeFactory _typeFactory;

    protected final ClassIntrospector _classIntrospector;

    /**
     * @since 3.0
     */
    protected final TypeResolverProvider _typeResolverProvider;

    /**
     * Registered concrete subtypes that can be used instead of (or
     * in addition to) ones declared using annotations.
     *<p>
     * Note that instances are stateful and as such may need to be copied,
     * and may NOT be demoted down to {@link BaseSettings}.
     */
    protected final SubtypeResolver _subtypeResolver;

    /**
     * Mix-in annotation mappings to use, if any.
     */
    protected final MixInHandler _mixIns;

    /*
    /**********************************************************************
    /* Immutable config, factories
    /**********************************************************************
     */
    
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
     */
    protected final ContextAttributes _attributes;

    /**
     * Simple cache used for finding out possible root name for root name
     * wrapping.
     *<p>
     * Note that instances are stateful (for caching) and as such may need to be copied,
     * and may NOT be demoted down to {@link BaseSettings}.
     */
    protected final RootNameLookup _rootNames;

    /**
     * Configuration overrides to apply, keyed by type of property.
     */
    protected final ConfigOverrides _configOverrides;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    /**
     * Constructor used when creating a new instance (compared to
     * that of creating fluent copies)
     */
    protected MapperConfigBase(MapperBuilder<?,?> b, long mapperFeatures,
            TypeFactory tf, ClassIntrospector classIntr, MixInHandler mixins, SubtypeResolver str,
            ConfigOverrides configOverrides, ContextAttributes defaultAttrs,
            RootNameLookup rootNames)
    {
        super(b.baseSettings(), mapperFeatures);

        _typeFactory = tf;
        _classIntrospector = classIntr;
        _typeResolverProvider = b.typeResolverProvider();
        _subtypeResolver = str;

        _mixIns = mixins;
        _rootNames = rootNames;
        _rootName = null;
        _view = null;
        _attributes = defaultAttrs;
        _configOverrides = configOverrides;
    }

    /**
     * Pass-through constructor used when no changes are needed to the
     * base class.
     */
    protected MapperConfigBase(MapperConfigBase<CFG,T> src)
    {
        super(src);
        _typeFactory = src._typeFactory;
        _classIntrospector = src._classIntrospector;
        _typeResolverProvider = src._typeResolverProvider;
        _subtypeResolver = src._subtypeResolver;

        _mixIns = src._mixIns;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = src._attributes;
        _configOverrides = src._configOverrides;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, BaseSettings base)
    {
        super(src, base);
        _typeFactory = src._typeFactory;
        _classIntrospector = src._classIntrospector;
        _typeResolverProvider = src._typeResolverProvider;
        _subtypeResolver = src._subtypeResolver;

        _mixIns = src._mixIns;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = src._attributes;
        _configOverrides = src._configOverrides;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, PropertyName rootName) {
        super(src);
        _typeFactory = src._typeFactory;
        _classIntrospector = src._classIntrospector;
        _typeResolverProvider = src._typeResolverProvider;
        _subtypeResolver = src._subtypeResolver;

        _mixIns = src._mixIns;
        _rootNames = src._rootNames;
        _rootName = rootName;
        _view = src._view;
        _attributes = src._attributes;
        _configOverrides = src._configOverrides;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, Class<?> view)
    {
        super(src);
        _typeFactory = src._typeFactory;
        _classIntrospector = src._classIntrospector;
        _typeResolverProvider = src._typeResolverProvider;
        _subtypeResolver = src._subtypeResolver;

        _mixIns = src._mixIns;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = view;
        _attributes = src._attributes;
        _configOverrides = src._configOverrides;
    }

    protected MapperConfigBase(MapperConfigBase<CFG,T> src, ContextAttributes attr)
    {
        super(src);
        _typeFactory = src._typeFactory;
        _classIntrospector = src._classIntrospector;
        _typeResolverProvider = src._typeResolverProvider;
        _subtypeResolver = src._subtypeResolver;

        _mixIns = src._mixIns;
        _rootNames = src._rootNames;
        _rootName = src._rootName;
        _view = src._view;
        _attributes = attr;
        _configOverrides = src._configOverrides;
    }

    /*
    /**********************************************************************
    /* Abstract fluent factory methods to be implemented by subtypes
    /**********************************************************************
     */

    protected abstract T _withBase(BaseSettings newBase);

    /*
    /**********************************************************************
    /* Additional shared fluent factory methods; attributes
    /**********************************************************************
     */

    /**
     * Method for constructing an instance that has specified
     * contextual attributes.
     */
    public abstract T with(ContextAttributes attrs);

    /**
     * Method for constructing an instance that has only specified
     * attributes, removing any attributes that exist before the call.
     */
    public T withAttributes(Map<?,?> attributes) {
        return with(getAttributes().withSharedAttributes(attributes));
    }
    
    /**
     * Method for constructing an instance that has specified
     * value for attribute for given key.
     */
    public T withAttribute(Object key, Object value) {
        return with(getAttributes().withSharedAttribute(key, value));
    }

    /**
     * Method for constructing an instance that has no
     * value for attribute for given key.
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
     * {@link TypeResolverBuilder} to use.
     */
    public final T with(TypeResolverBuilder<?> trb) {
        return _withBase(_base.with(trb));
    }

    /*
    /**********************************************************************
    /* Additional shared fluent factory methods; other
    /**********************************************************************
     */

    /**
     * Fluent factory method that will construct a new instance with
     * specified {@link JsonNodeFactory}.
     */
    public final T with(JsonNodeFactory f) {
        return _withBase(_base.with(f));
    }

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
        return _withBase(_base.with(df));
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
     * view to use.
     */
    public abstract T withView(Class<?> view);

    /*
    /**********************************************************************
    /* Simple factory access, related
    /**********************************************************************
     */

    @Override
    public final TypeFactory getTypeFactory() {
        return _typeFactory;
    }

    @Override
    public ClassIntrospector classIntrospectorInstance() {
        return _classIntrospector.forOperation(this);
    }

    @Override
    public TypeResolverProvider getTypeResolverProvider() {
        return _typeResolverProvider;
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

    @Override
    public final JavaType constructType(Class<?> cls) {
        return _typeFactory.constructType(cls);
    }

    @Override
    public final JavaType constructType(TypeReference<?> valueTypeRef) {
        return _typeFactory.constructType(valueTypeRef.getType());
    }

    /*
    /**********************************************************************
    /* Simple config property access
    /**********************************************************************
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
    public final VisibilityChecker getDefaultVisibilityChecker()
    {
        return _configOverrides.getDefaultVisibility();
    }

    @Override
    public final VisibilityChecker getDefaultVisibilityChecker(Class<?> baseType,
            AnnotatedClass actualClass)
    {
        // 14-Apr-2021, tatu: [databind#3117] JDK types should be limited
        //    to "public-only" regardless of settings for other types
        VisibilityChecker vc;

        if (ClassUtil.isJDKClass(baseType)) {
            vc = VisibilityChecker.allPublicInstance();
        } else {
            vc = getDefaultVisibilityChecker();
        }
        AnnotationIntrospector intr = getAnnotationIntrospector();
        if (intr != null) {
            vc = intr.findAutoDetectVisibility(this, actualClass, vc);
        }
        ConfigOverride overrides = _configOverrides.findOverride(baseType);
        if (overrides != null) {
            vc = vc.withOverrides(overrides.getVisibility()); // ok to pass null
        }
        return vc;
    }

    @Override
    public final JsonSetter.Value getDefaultNullHandling() {
        return _configOverrides.getDefaultNullHandling();
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
    public PropertyName findRootName(DatabindContext ctxt, JavaType rootType) {
        if (_rootName != null) {
            return _rootName;
        }
        return _rootNames.findRootName(ctxt, rootType);
    }

    @Override
    public PropertyName findRootName(DatabindContext ctxt, Class<?> rawRootType) {
        if (_rootName != null) {
            return _rootName;
        }
        return _rootNames.findRootName(ctxt, rawRootType);
    }

    /*
    /**********************************************************************
    /* MixInResolver impl:
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

    @Override
    public boolean hasMixIns() {
        return _mixIns.hasMixIns();
    }
    
    // Not really relevant here (should not get called)
    @Override
    public MixInResolver snapshot() {
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
