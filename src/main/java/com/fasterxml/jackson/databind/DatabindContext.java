package com.fasterxml.jackson.databind;

import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Shared base class for {@link DeserializationContext} and
 * {@link SerializerProvider}, context objects passed through data-binding
 * process. Designed so that some of implementations can rely on shared
 * aspects like access to secondary contextual objects like type factories
 * or handler instantiators.
 * 
 * @since 2.2
 */
public abstract class DatabindContext
{
    /*
    /**********************************************************
    /* Generic config access
    /**********************************************************
     */

    /**
     * Accessor to currently active configuration (both per-request configs
     * and per-mapper config).
     */
    public abstract MapperConfig<?> getConfig();

    /**
     * Convenience method for accessing serialization view in use (if any); equivalent to:
     *<pre>
     *   getConfig().getAnnotationIntrospector();
     *</pre>
     */
    public abstract AnnotationIntrospector getAnnotationIntrospector();
    
    /*
    /**********************************************************
    /* Access to specific config settings
    /**********************************************************
     */
    
    /**
     * Convenience method for checking whether specified serialization
     * feature is enabled or not.
     * Shortcut for:
     *<pre>
     *  getConfig().isEnabled(feature);
     *</pre>
     */
    public final boolean isEnabled(MapperFeature feature) {
        return getConfig().isEnabled(feature);
    }

    /**
     * Convenience method for accessing serialization view in use (if any); equivalent to:
     *<pre>
     *   getConfig().canOverrideAccessModifiers();
     *</pre>
     */
    public final boolean canOverrideAccessModifiers() {
        return getConfig().canOverrideAccessModifiers();
    }

    /**
     * Accessor for locating currently active view, if any;
     * returns null if no view has been set.
     */
    public abstract Class<?> getActiveView();
    
    /*
    /**********************************************************
    /* Type instantiation/resolution
    /**********************************************************
     */

    /**
     * Convenience method for constructing {@link JavaType} for given JDK
     * type (usually {@link java.lang.Class})
     */
    public JavaType constructType(Type type) {
         return getTypeFactory().constructType(type);
    }

    /**
     * Convenience method for constructing subtypes, retaining generic
     * type parameter (if any)
     */
    public JavaType constructSpecializedType(JavaType baseType, Class<?> subclass) {
        return getConfig().constructSpecializedType(baseType, subclass);
    }

    public abstract TypeFactory getTypeFactory();
}
