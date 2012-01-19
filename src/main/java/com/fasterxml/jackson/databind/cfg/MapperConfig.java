package com.fasterxml.jackson.databind.cfg;

import java.text.DateFormat;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Interface that defines functionality accessible through both
 * serialization and deserialization configuration objects;
 * accessors to mode-independent configuration settings
 * and such.
 * In addition, shared features are defined
 * in {@link MapperConfig.Feature}
 *<p>
 * Small part of implementation is included here by aggregating
 * {@link BaseSettings} instance that contains configuration
 * that is shared between different types of instances.
 */
public abstract class MapperConfig<T extends MapperConfig<T>>
    implements ClassIntrospector.MixInResolver
{
    /**
     * Enumeration that defines simple on/off features to set
     * for {@link ObjectMapper}, and accessible (but NOT mutable!)
     * via {@link SerializationConfig} and {@link DeserializationConfig}.
     * Note that in addition to being only mutable via {@link ObjectMapper},
     * changes only take effect when done <b>before any serialization or
     * deserialization</b> calls -- that is, caller must follow
     * "configure-then-use" pattern.
     */
    public enum Feature implements ConfigFeature
    {
        /*
        /******************************************************
        /*  Introspection features
        /******************************************************
         */
        
        /**
         * Feature that determines whether annotation introspection
         * is used for configuration; if enabled, configured
         * {@link AnnotationIntrospector} will be used: if disabled,
         * no annotations are considered.
         *<p>
         * Feature is enabled by default.
         */
        USE_ANNOTATIONS(true),

        /**
         * Feature that determines whether "creator" methods are
         * automatically detected by consider public constructors,
         * and static single argument methods with name "valueOf".
         * If disabled, only methods explicitly annotated are considered
         * creator methods (except for the no-arg default constructor which
         * is always considered a factory method).
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<P>
         * Feature is enabled by default.
         */
        AUTO_DETECT_CREATORS(true),
        
        /**
         * Feature that determines whether non-static fields are recognized as
         * properties.
         * If yes, then all public member fields
         * are considered as properties. If disabled, only fields explicitly
         * annotated are considered property fields.
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<p>
         * Feature is enabled by default.
         */
         AUTO_DETECT_FIELDS(true),
        
        /**
         * Feature that determines whether regualr "getter" methods are
         * automatically detected based on standard Bean naming convention
         * or not. If yes, then all public zero-argument methods that
         * start with prefix "get" 
         * are considered as getters.
         * If disabled, only methods explicitly  annotated are considered getters.
         *<p>
         * Note that since version 1.3, this does <b>NOT</b> include
         * "is getters" (see {@link #AUTO_DETECT_IS_GETTERS} for details)
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<p>
         * Feature is enabled by default.
         */
        AUTO_DETECT_GETTERS(true),

        /**
         * Feature that determines whether "is getter" methods are
         * automatically detected based on standard Bean naming convention
         * or not. If yes, then all public zero-argument methods that
         * start with prefix "is", and whose return type is boolean
         * are considered as "is getters".
         * If disabled, only methods explicitly annotated are considered getters.
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<p>
         * Feature is enabled by default.
         */
        AUTO_DETECT_IS_GETTERS(true),

         /**
          * Feature that determines whether "setter" methods are
          * automatically detected based on standard Bean naming convention
          * or not. If yes, then all public one-argument methods that
          * start with prefix "set"
          * are considered setters. If disabled, only methods explicitly
          * annotated are considered setters.
          *<p>
          * Note that this feature has lower precedence than per-class
          * annotations, and is only used if there isn't more granular
          * configuration available.
          *<P>
          * Feature is enabled by default.
          */
         AUTO_DETECT_SETTERS(true),
         
         /**
          * Feature that determines whether getters (getter methods)
          * can be auto-detected if there is no matching mutator (setter,
          * constructor parameter or field) or not: if set to true,
          * only getters that match a mutator are auto-discovered; if
          * false, all auto-detectable getters can be discovered.
          *<p>
          * Feature is disabled by default.
          */
         REQUIRE_SETTERS_FOR_GETTERS(false),

         /**
          * Feature that determines whether otherwise regular "getter"
          * methods (but only ones that handle Collections and Maps,
          * not getters of other type)
          * can be used for purpose of getting a reference to a Collection
          * and Map to modify the property, without requiring a setter
          * method.
          * This is similar to how JAXB framework sets Collections and
          * Maps: no setter is involved, just setter.
          *<p>
          * Note that such getters-as-setters methods have lower
          * precedence than setters, so they are only used if no
          * setter is found for the Map/Collection property.
          *<p>
          * Feature is enabled by default.
          */
         USE_GETTERS_AS_SETTERS(true),

         /**
         * Feature that determines whether method and field access
         * modifier settings can be overridden when accessing
         * properties. If enabled, method
         * {@link java.lang.reflect.AccessibleObject#setAccessible}
         * may be called to enable access to otherwise unaccessible
         * objects.
         *<p>
         * Feature is enabled by default.
         */
        CAN_OVERRIDE_ACCESS_MODIFIERS(true),

        /*
        /******************************************************
        /* Type-handling features
        /******************************************************
         */

        /**
         * Feature that determines whether the type detection for
         * serialization should be using actual dynamic runtime type,
         * or declared static type.
         * Note that deserialization always uses declared static types
         * since no runtime types are available (as we are creating
         * instances after using type information).
         *<p>
         * This global default value can be overridden at class, method
         * or field level by using {@link JsonSerialize#typing} annotation
         * property.
         *<p>
         * Feature is disabled by default which means that dynamic runtime types
         * are used (instead of declared static types) for serialization.
         */
        USE_STATIC_TYPING(false),

        /*
        /******************************************************
        /* View-related features
        /******************************************************
         */
        
        /**
         * Feature that determines whether properties that have no view
         * annotations are included in JSON serialization views (see
         * {@link com.fasterxml.jackson.annotation.JsonView} for more
         * details on JSON Views).
         * If enabled, non-annotated properties will be included;
         * when disabled, they will be excluded. So this feature
         * changes between "opt-in" (feature disabled) and
         * "opt-out" (feature enabled) modes.
         *<p>
         * Default value is enabled, meaning that non-annotated
         * properties are included in all views if there is no
         * {@link com.fasterxml.jackson.annotation.JsonView} annotation.
         *<p>
         * Feature is enabled by default.
         */
        DEFAULT_VIEW_INCLUSION(true),
        
        /*
        /******************************************************
        /* Generic output features
        /******************************************************
         */

        /**
         * Feature that defines default property serialization order used
         * for POJO fields (note: does <b>not</b> apply to {@link java.util.Map}
         * serialization!):
         * if enabled, default ordering is alphabetic (similar to
         * how {@link com.fasterxml.jackson.annotation.JsonPropertyOrder#alphabetic()}
         * works); if disabled, order is unspecified (based on what JDK gives
         * us, which may be declaration order, but is not guaranteed).
         *<p>
         * Note that this is just the default behavior, and can be overridden by
         * explicit overrides in classes.
         *<p>
         * Feature is disabled by default.
         */
        SORT_PROPERTIES_ALPHABETICALLY(false)

        ;

        private final boolean _defaultState;
        
        private Feature(boolean defaultState) {
            _defaultState = defaultState;
        }
        
        @Override
        public boolean enabledByDefault() { return _defaultState; }

        @Override
        public int getMask() { return (1 << ordinal()); }
    }
    
    /*
    /**********************************************************
    /* Simple immutable basic settings
    /**********************************************************
     */

    /**
     * Set of shared mapper features enabled.
     */
    protected final int _mapperFeatures;
    
    /**
     * Immutable container object for simple configuration settings.
     */
    protected final BaseSettings _base;
    
    /*
    /**********************************************************
    /* Life-cycle: constructors
    /**********************************************************
     */

    protected MapperConfig(BaseSettings base, int mapperFeatures)
    {
        _base = base;
        _mapperFeatures = mapperFeatures;
    }

    protected MapperConfig(MapperConfig<T> src)
    {
        _base = src._base;
        _mapperFeatures = src._mapperFeatures;
    }
    
    /**
     * Method that calculates bit set (flags) of all features that
     * are enabled by default.
     */
    public static <F extends Enum<F> & ConfigFeature> int collectFeatureDefaults(Class<F> enumClass)
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
    /* Life-cycle: factory methods
    /**********************************************************
     */

    /**
     * Method for constructing and returning a new instance with specified
     * mapper features enabled.
     */
    public abstract T with(MapperConfig.Feature... features);

    /**
     * Method for constructing and returning a new instance with specified
     * mapper features disabled.
     */
    public abstract T without(MapperConfig.Feature... features);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link ClassIntrospector}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T withClassIntrospector(ClassIntrospector<? extends BeanDescription> ci);

    /**
     * Method for constructing and returning a new instance with different
     * {@link AnnotationIntrospector} to use (replacing old one).
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T withAnnotationIntrospector(AnnotationIntrospector ai);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link VisibilityChecker}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T withVisibilityChecker(VisibilityChecker<?> vc);

    /**
     * Method for constructing and returning a new instance with different
     * minimal visibility level for specified property type
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T withVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link TypeResolverBuilder}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T withTypeResolverBuilder(TypeResolverBuilder<?> trb);
    
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
     * {@link PropertyNamingStrategy}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T withPropertyNamingStrategy(PropertyNamingStrategy strategy);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link TypeFactory}
     * to use.
     *<p>
     * NOTE: make sure to register new instance with <code>ObjectMapper</code>
     * if directly calling this method.
     */
    public abstract T withTypeFactory(TypeFactory typeFactory);
    
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
     * Method for constructing and returning a new instance with additional
     * {@link AnnotationIntrospector} inserted (as the highest priority one)
     */
    public abstract T withInsertedAnnotationIntrospector(AnnotationIntrospector introspector);

    /**
     * Method for constructing and returning a new instance with additional
     * {@link AnnotationIntrospector} appended (as the lowest priority one)
     */
    public abstract T withAppendedAnnotationIntrospector(AnnotationIntrospector introspector);
    
    /*
    /**********************************************************
    /* Configuration: simple features
    /**********************************************************
     */

    /**
     * Accessor for simple mapper features (which are shared for
     * serialization, deserialization)
     */
    public final boolean isEnabled(MapperConfig.Feature f) {
        return (_mapperFeatures & f.getMask()) != 0;
    }
    
    /**
     * Method for determining whether annotation processing is enabled or not
     * (default settings are typically that it is enabled; must explicitly disable).
     * 
     * @return True if annotation processing is enabled; false if not
     */
    public final boolean isAnnotationProcessingEnabled() {
        return isEnabled(MapperConfig.Feature.USE_ANNOTATIONS);
    }

    /**
     * Accessor for determining whether it is ok to try to force override of access
     * modifiers to be able to get or set values of non-public Methods, Fields;
     * to invoke non-public Constructors, Methods; or to instantiate non-public
     * Classes. By default this is enabled, but on some platforms it needs to be
     * prevented since if this would violate security constraints and cause failures.
     * 
     * @return True if access modifier overriding is allowed (and may be done for
     *   any Field, Method, Constructor or Class); false to prevent any attempts
     *   to override.
     */
    public final boolean canOverrideAccessModifiers() {
        return isEnabled(MapperConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);
    }

    /**
     * Accessor for checking whether default settings for property handling
     * indicate that properties should be alphabetically ordered or not.
     */
    public final boolean shouldSortPropertiesAlphabetically() {
        return isEnabled(MapperConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY);
    }
    
    /*
    /**********************************************************
    /* Configuration: introspectors, mix-ins
    /**********************************************************
     */
    
    public ClassIntrospector<? extends BeanDescription> getClassIntrospector() {
        return _base.getClassIntrospector();
    }

    /**
     * Method for getting {@link AnnotationIntrospector} configured
     * to introspect annotation values used for configuration.
     *<p>
     * Non-final since it is actually overridden by sub-classes (for now?)
     */
    public AnnotationIntrospector getAnnotationIntrospector() {
        return _base.getAnnotationIntrospector();
    }

    /**
     * Accessor for object used for determining whether specific property elements
     * (method, constructors, fields) can be auto-detected based on
     * their visibility (access modifiers). Can be changed to allow
     * different minimum visibility levels for auto-detection. Note
     * that this is the global handler; individual types (classes)
     * can further override active checker used (using
     * {@link JsonAutoDetect} annotation)
     */
    public VisibilityChecker<?> getDefaultVisibilityChecker() {
        return _base.getVisibilityChecker();
    }
    
    public final PropertyNamingStrategy getPropertyNamingStrategy() {
        return _base.getPropertyNamingStrategy();
    }

    public final HandlerInstantiator getHandlerInstantiator() {
        return _base.getHandlerInstantiator();
    }
    
    /*
    /**********************************************************
    /* Configuration: type and subtype handling
    /**********************************************************
     */

    /**
     * Method called to locate a type info handler for types that do not have
     * one explicitly declared via annotations (or other configuration).
     * If such default handler is configured, it is returned; otherwise
     * null is returned.
     */
    public final TypeResolverBuilder<?> getDefaultTyper(JavaType baseType) {
        return _base.getTypeResolverBuilder();
    }
    
    public abstract SubtypeResolver getSubtypeResolver();

    public final TypeFactory getTypeFactory() {
        return _base.getTypeFactory();
    }

    /**
     * Helper method that will construct {@link JavaType} for given
     * raw class.
     * This is a simple short-cut for:
     *<pre>
     *    getTypeFactory().constructType(cls);
     *</pre>
     */
    public final JavaType constructType(Class<?> cls) {
        return getTypeFactory().constructType(cls, (TypeBindings) null);
    }

    /**
     * Helper method that will construct {@link JavaType} for given
     * type reference
     * This is a simple short-cut for:
     *<pre>
     *    getTypeFactory().constructType(valueTypeRef);
     *</pre>
     */
    public final JavaType constructType(TypeReference<?> valueTypeRef) {
        return getTypeFactory().constructType(valueTypeRef.getType(), (TypeBindings) null);
    }

    public JavaType constructSpecializedType(JavaType baseType, Class<?> subclass) {
        return getTypeFactory().constructSpecializedType(baseType, subclass);
    }
    
    /*
    /**********************************************************
    /* Configuration: other
    /**********************************************************
     */
    
    /**
     * Method for accessing currently configured (textual) date format
     * that will be used for reading or writing date values (in case
     * of writing, only if textual output is configured; not if dates
     * are to be serialized as time stamps).
     *<p>
     * Note that typically {@link DateFormat} instances are <b>not thread-safe</b>
     * (at least ones provided by JDK):
     * this means that calling code should clone format instance before
     * using it.
     *<p>
     * This method is usually only called by framework itself, since there
     * are convenience methods available via
     * {@link DeserializationContext} and {@link SerializerProvider} that
     * take care of cloning and thread-safe reuse.
     */
    public final DateFormat getDateFormat() { return _base.getDateFormat(); }

    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
     */
    @SuppressWarnings("unchecked")
    public <DESC extends BeanDescription> DESC introspectClassAnnotations(Class<?> cls) {
        return (DESC) introspectClassAnnotations(constructType(cls));
    }
    
    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
     */
    public abstract <DESC extends BeanDescription> DESC introspectClassAnnotations(JavaType type);

    /**
     * Accessor for getting bean description that only contains immediate class
     * annotations: ones from the class, and its direct mix-in, if any, but
     * not from super types.
     */
    @SuppressWarnings("unchecked")
    public <DESC extends BeanDescription> DESC introspectDirectClassAnnotations(Class<?> cls) {
        return (DESC) introspectDirectClassAnnotations(constructType(cls));
    }
    /**
     * Accessor for getting bean description that only contains immediate class
     * annotations: ones from the class, and its direct mix-in, if any, but
     * not from super types.
     */
    public abstract <DESC extends BeanDescription> DESC introspectDirectClassAnnotations(JavaType type);
        
    /*
    /**********************************************************
    /* Methods for instantiating handlers
    /**********************************************************
     */

    /**
     * Method that can be called to obtain an instance of <code>TypeIdResolver</code> of
     * specified type.
     */
    public TypeResolverBuilder<?> typeResolverBuilderInstance(Annotated annotated,
            Class<? extends TypeResolverBuilder<?>> builderClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            TypeResolverBuilder<?> builder = hi.typeResolverBuilderInstance(this, annotated, builderClass);
            if (builder != null) {
                return builder;
            }
        }
        return (TypeResolverBuilder<?>) ClassUtil.createInstance(builderClass, canOverrideAccessModifiers());
    }

    /**
     * Method that can be called to obtain an instance of <code>TypeIdResolver</code> of
     * specified type.
     */
    public TypeIdResolver typeIdResolverInstance(Annotated annotated,
            Class<? extends TypeIdResolver> resolverClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            TypeIdResolver builder = hi.typeIdResolverInstance(this, annotated, resolverClass);
            if (builder != null) {
                return builder;
            }
        }
        return (TypeIdResolver) ClassUtil.createInstance(resolverClass, canOverrideAccessModifiers());
    }
}

