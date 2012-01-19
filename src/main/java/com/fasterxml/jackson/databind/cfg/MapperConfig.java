package com.fasterxml.jackson.databind.cfg;

import java.text.DateFormat;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * Interface that defines functionality accessible through both
 * serialization and deserialization configuration objects;
 * accessors to mode-independent configuration settings
 * and such.
 *<p>
 * As of version 1.9, the goal is to make this class eventually immutable.
 * Because of this, existing methods that allow changing state of this
 * instance are deprecated in favor of methods that create new instances
 * with different configuration ("fluent factories").
 * One major remaining issue is that of handling mix-in annotations, which
 * still represent a bit of mutable state; may need to implement a
 * functional-style immutable map for storing those.
 */
public abstract class MapperConfig<T extends MapperConfig<T>>
    implements ClassIntrospector.MixInResolver
{
    /*
    /**********************************************************
    /* Constants, default values
    /**********************************************************
     */

    /**
     * This is the default {@link DateFormat} used unless overridden by
     * custom implementation.
     */
    protected final static DateFormat DEFAULT_DATE_FORMAT = StdDateFormat.instance;

    /*
    /**********************************************************
    /* Simple immutable basic settings
    /**********************************************************
     */

    /**
     * Immutable container object for simple configuration settings.
     */
    protected final BaseSettings _base;
    
    /*
    /**********************************************************
    /* Life-cycle: constructors
    /**********************************************************
     */

    protected MapperConfig(ClassIntrospector<? extends BeanDescription> ci, AnnotationIntrospector ai,
            VisibilityChecker<?> vc, PropertyNamingStrategy pns, TypeFactory tf,
            HandlerInstantiator hi)
    {
        _base = new BaseSettings(ci, ai, vc, pns, tf, null, DEFAULT_DATE_FORMAT, hi);
    }

    protected MapperConfig(BaseSettings base)
    {
        _base = base;
    }
    
    /*
    /**********************************************************
    /* Life-cycle: factory methods
    /**********************************************************
     */
    
    /**
     * Method to use for constructing an instance that is not shared
     * between multiple operations but only used for a single one
     * (which may be this instance, if it is immutable; if not, a copy
     * is constructed with same settings)
     */
    public abstract T createUnshared(SubtypeResolver subtypeResolver);

    /**
     * Method to use for constructing an instance that is not shared
     * between multiple operations but only used for a single one
     * (which may be this instance, if it is immutable; if not, a copy
     * is constructed with same settings)
     */
    public abstract T createUnshared(SubtypeResolver subtypeResolver,
            int features);

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
     * Method for determining whether annotation processing is enabled or not
     * (default settings are typically that it is enabled; must explicitly disable).
     * 
     * @return True if annotation processing is enabled; false if not
     */
    public abstract boolean isAnnotationProcessingEnabled();

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
    public abstract boolean canOverrideAccessModifiers();

    /**
     * Accessor for checking whether default settings for property handling
     * indicate that properties should be alphabetically ordered or not.
     */
    public abstract boolean shouldSortPropertiesAlphabetically();
    
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

    /*
    /**********************************************************
    /* Helper interface used with simple on/off features
    /**********************************************************
     */
    
    /**
     * Interface that actual Feature enumerations used by
     * {@link MapperConfig} implementations must implement.
     * Necessary since enums can not be extended using normal
     * inheritance, but can implement interfaces
     */
    public interface ConfigFeature
    {
        /**
         * Accessor for checking whether this feature is enabled by default.
         */
        public boolean enabledByDefault();

        /**
         * Accessor for checking whether feature can be used on per-call basis
         * (true), or not (false): in latter case it can only be configured once
         * before any serialization or deserialization.
         */
        public boolean canUseForInstance();
        
        /**
         * Returns bit mask for this feature instance
         */
        public int getMask();
    }
}

