package com.fasterxml.jackson.databind.introspect;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jdk14.JDK14Util;

/**
 * Default {@link AccessorNamingStrategy} used by Jackson: to be used either as-is,
 * or as base-class with overrides.
 */
public class DefaultAccessorNamingStrategy
    extends AccessorNamingStrategy
{
    protected final MapperConfig<?> _config;
    protected final AnnotatedClass _forClass;

    protected final String _getterPrefix;
    protected final String _isGetterPrefix;

    /**
     * Prefix used by auto-detected mutators ("setters"): usually "set",
     * but differs for builder objects ("with" by default).
     */
    protected final String _mutatorPrefix;

    protected DefaultAccessorNamingStrategy(MapperConfig<?> config, AnnotatedClass forClass,
            String mutatorPrefix, String getterPrefix, String isGetterPrefix)
    {
        _config = config;
        _forClass = forClass;

        _mutatorPrefix = mutatorPrefix;
        _getterPrefix = getterPrefix;
        _isGetterPrefix = isGetterPrefix;
    }
    
    @Override
    public String findNameForIsGetter(AnnotatedMethod am, String name)
    {
        if (_isGetterPrefix != null) {
            final Class<?> rt = am.getRawType();
            if (rt == Boolean.class || rt == Boolean.TYPE) {
                if (name.startsWith(_isGetterPrefix)) { // plus, must return a boolean
                    return stdManglePropertyName(name, 2);
                }
            }
        }
        return null;
    }

    @Override
    public String findNameForRegularGetter(AnnotatedMethod am, String name)
    {
        if ((_getterPrefix != null) && name.startsWith(_getterPrefix)) {
            // 16-Feb-2009, tatu: To handle [JACKSON-53], need to block CGLib-provided
            // method "getCallbacks". Not sure of exact safe criteria to get decent
            // coverage without false matches; but for now let's assume there is
            // no reason to use any such getter from CGLib.
            if ("getCallbacks".equals(name)) {
                if (isCglibGetCallbacks(am)) {
                    return null;
                }
            } else if ("getMetaClass".equals(name)) {
                // 30-Apr-2009, tatu: Need to suppress serialization of a cyclic reference
                if (isGroovyMetaClassGetter(am)) {
                    return null;
                }
            }
            return stdManglePropertyName(name, _getterPrefix.length());
        }
        return null;
    }

    @Override
    public String findNameForMutator(AnnotatedMethod am, String name)
    {
        if ((_mutatorPrefix != null) && name.startsWith(_mutatorPrefix)) {
            return stdManglePropertyName(name, _mutatorPrefix.length());
        }
        return null;
    }

    // Default implementation simply returns name as-is
    @Override
    public String modifyFieldName(AnnotatedField field, String name) {
        return name;
    }

    /*
    /**********************************************************************
    /* Name-mangling methods copied in 2.12 from "BeanUtil"
    /**********************************************************************
     */

    // 24-Sep-2017, tatu: note that "std" here refers to earlier (1.x, 2.x) distinction
    //   between "legacy" (slightly non-conforming) and "std" (fully conforming): with 3.x
    //   only latter exists.
    protected static String stdManglePropertyName(final String basename, final int offset)
    {
        final int end = basename.length();
        if (end == offset) { // empty name, nope
            return null;
        }
        // first: if it doesn't start with capital, return as-is
        char c0 = basename.charAt(offset);
        char c1 = Character.toLowerCase(c0);
        if (c0 == c1) {
            return basename.substring(offset);
        }
        // 17-Dec-2014, tatu: As per [databind#653], need to follow more
        //   closely Java Beans spec; specifically, if two first are upper-case,
        //   then no lower-casing should be done.
        if ((offset + 1) < end) {
            if (Character.isUpperCase(basename.charAt(offset+1))) {
                return basename.substring(offset);
            }
        }
        StringBuilder sb = new StringBuilder(end - offset);
        sb.append(c1);
        sb.append(basename, offset+1, end);
        return sb.toString();
    }

    /*
    /**********************************************************************
    /* Legacy methods copied in 2.12 from "BeanUtil" -- are these still needed?
    /**********************************************************************
     */

    // This method was added to address the need to weed out CGLib-injected
    // "getCallbacks" method. 
    // At this point caller has detected a potential getter method with
    // name "getCallbacks" and we need to determine if it is indeed injected
    // by Cglib. We do this by verifying that the  result type is "net.sf.cglib.proxy.Callback[]"
    private static boolean isCglibGetCallbacks(AnnotatedMethod am)
    {
        Class<?> rt = am.getRawType();
        // Ok, first: must return an array type
        if (rt.isArray()) {
            // And that type needs to be "net.sf.cglib.proxy.Callback".
            // Theoretically could just be a type that implements it, but
            // for now let's keep things simple, fix if need be.

            Class<?> compType = rt.getComponentType();
            // Actually, let's just verify it's a "net.sf.cglib.*" class/interface
            final String className = compType.getName();
            if (className.contains(".cglib")) {
                return className.startsWith("net.sf.cglib")
                    // also, as per [JACKSON-177]
                    || className.startsWith("org.hibernate.repackage.cglib")
                    // and [core#674]
                    || className.startsWith("org.springframework.cglib");
            }
        }
        return false;
    }

    // Another helper method to deal with Groovy's problematic metadata accessors
    private static boolean isGroovyMetaClassGetter(AnnotatedMethod am) {
        return am.getRawType().getName().startsWith("groovy.lang");
    }

    /*
    /**********************************************************************
    /* Standard Provider implementation
    /**********************************************************************
     */

    /**
     * Provider for {@link DefaultAccessorNamingStrategy}.
     *<p>
     * Default instance will use following default prefixes:
     *<ul>
     * <li>Setter for regular POJOs: "set"
     *  </li>
     * <li>Builder-mutator: "with"
     *  </li>
     * <li>Regular getter: "get"
     *  </li>
     * <li>Is-getter (for Boolean values): "is"
     *  </li>
     * <ul>
     *<p>
     * 
     */
    public static class Provider
        extends AccessorNamingStrategy.Provider
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        protected final String _setterPrefix;
        protected final String _withPrefix;

        protected final String _getterPrefix;
        protected final String _isGetterPrefix;

        public Provider() {
            this("set", JsonPOJOBuilder.DEFAULT_WITH_PREFIX,
                    "get", "is");
        }

        public Provider(String setterPrefix, String withPrefix,
                String getterPrefix, String isGetterPrefix) {
            _setterPrefix = setterPrefix;
            _withPrefix = withPrefix;
            _getterPrefix = getterPrefix;
            _isGetterPrefix = isGetterPrefix;
        }

        public Provider withSetterPrefix(String p) {
            return new Provider(p, _withPrefix, _getterPrefix, _isGetterPrefix);
        }
        
        public Provider withBuilderPrefix(String p) {
            return new Provider(_setterPrefix, p, _getterPrefix, _isGetterPrefix);
        }

        public Provider withGetterPrefix(String p) {
            return new Provider(_setterPrefix, _withPrefix, p, _isGetterPrefix);
        }

        public Provider withIsGetterPrefix(String p) {
            return new Provider(_setterPrefix, _withPrefix, _getterPrefix, p);
        }

        @Override
        public AccessorNamingStrategy forPOJO(MapperConfig<?> config, AnnotatedClass targetClass)
        {
            return new DefaultAccessorNamingStrategy(config, targetClass,
                    _setterPrefix, _getterPrefix, _isGetterPrefix);
        }

        @Override
        public AccessorNamingStrategy forBuilder(MapperConfig<?> config,
                AnnotatedClass builderClass, BeanDescription valueTypeDesc)
        {
            AnnotationIntrospector ai = config.isAnnotationProcessingEnabled() ? config.getAnnotationIntrospector() : null;
            JsonPOJOBuilder.Value builderConfig = (ai == null) ? null : ai.findPOJOBuilderConfig(config, builderClass);
            String mutatorPrefix = (builderConfig == null) ? _withPrefix : builderConfig.withPrefix;
            return new DefaultAccessorNamingStrategy(config, builderClass,
                    mutatorPrefix, _getterPrefix, _isGetterPrefix);
        }

        @Override
        public AccessorNamingStrategy forRecord(MapperConfig<?> config, AnnotatedClass recordClass)
        {
            return new RecordNaming(config, recordClass);
        }
    }

    /**
     * Implementation used for supporting "non-prefix" naming convention of
     * Java 14 {@code java.lang.Record} types, and in particular find default
     * accessors for declared record fields.
     *<p>
     * Current / initial implementation will also recognize additional "normal"
     * getters ("get"-prefix) and is-getters ("is"-prefix and boolean return value)
     * by name.
     */
    public static class RecordNaming
        extends DefaultAccessorNamingStrategy
    {
        /**
         * Names of actual Record fields from definition; auto-detected.
         */
        protected final Set<String> _fieldNames;

        public RecordNaming(MapperConfig<?> config, AnnotatedClass forClass) {
            super(config, forClass,
                    // no setters for (immutable) Records:
                    null,
                    // trickier: regular fields are ok (handled differently), but should
                    // we also allow getter discovery? For now let's do so
                    "get", "is");
            _fieldNames = new HashSet<>();
            for (String name : JDK14Util.getRecordFieldNames(forClass.getRawType())) {
                _fieldNames.add(name);
            }
        }

        @Override
        public String findNameForRegularGetter(AnnotatedMethod am, String name)
        {
            // By default, field names are un-prefixed, but verify so that we will not
            // include "toString()" or additional custom methods (unless latter are
            // annotated for inclusion)
            if (_fieldNames.contains(name)) {
                return name;
            }
            // but also allow auto-detecting additional getters, if any?
            return super.findNameForRegularGetter(am, name);
        }
    }
}
