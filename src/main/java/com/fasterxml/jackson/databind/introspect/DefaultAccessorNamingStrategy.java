package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Default {@link AccessorNamingStrategy} used by Jackson: to be used either as-is,
 * or as base-class with overrides.
 *
 * @since 2.12
 */
public class DefaultAccessorNamingStrategy
    extends AccessorNamingStrategy
{
    protected final MapperConfig<?> _config;
    protected final AnnotatedClass _forClass;

    protected final boolean _stdBeanNaming;

    /**
     * Prefix used by auto-detected mutators ("setters"): usually "set",
     * but differs for builder objects ("with" by default).
     */
    protected final String _mutatorPrefix;

    protected DefaultAccessorNamingStrategy(MapperConfig<?> config, AnnotatedClass forClass,
            String mutatorPrefix) {
        _config = config;
        _forClass = forClass;

        _stdBeanNaming = config.isEnabled(MapperFeature.USE_STD_BEAN_NAMING);
        _mutatorPrefix = mutatorPrefix;
    }
    
    @Override
    public String findNameForIsGetter(AnnotatedMethod am, String name)
    {
        final Class<?> rt = am.getRawType();
        if (rt == Boolean.class || rt == Boolean.TYPE) {
            if (name.startsWith("is")) { // plus, must return a boolean
                return _stdBeanNaming
                        ? stdManglePropertyName(name, 2)
                        : legacyManglePropertyName(name, 2);
            }
        }
        return null;
    }

    @Override
    public String findNameForRegularGetter(AnnotatedMethod am, String name)
    {
        if (name.startsWith("get")) {
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
            return _stdBeanNaming
                    ? stdManglePropertyName(name, 3)
                    : legacyManglePropertyName(name, 3);
        }
        return null;
    }

    @Override
    public String findNameForMutator(AnnotatedMethod am, String name)
    {
        if (name.startsWith(_mutatorPrefix)) {
            return _stdBeanNaming
                    ? stdManglePropertyName(name, _mutatorPrefix.length())
                    : legacyManglePropertyName(name, _mutatorPrefix.length());
        }
        return null;
    }

    /*
    /**********************************************************************
    /* Name-mangling methods copied in 2.12 from "BeanUtil"
    /**********************************************************************
     */

    /**
     * Method called to figure out name of the property, given 
     * corresponding suggested name based on a method or field name.
     *
     * @param basename Name of accessor/mutator method, not including prefix
     *  ("get"/"is"/"set")
     */
    protected static String legacyManglePropertyName(final String basename, final int offset)
    {
        final int end = basename.length();
        if (end == offset) { // empty name, nope
            return null;
        }
        // next check: is the first character upper case? If not, return as is
        char c = basename.charAt(offset);
        char d = Character.toLowerCase(c);
        
        if (c == d) {
            return basename.substring(offset);
        }
        // otherwise, lower case initial chars. Common case first, just one char
        StringBuilder sb = new StringBuilder(end - offset);
        sb.append(d);
        int i = offset+1;
        for (; i < end; ++i) {
            c = basename.charAt(i);
            d = Character.toLowerCase(c);
            if (c == d) {
                sb.append(basename, i, end);
                break;
            }
            sb.append(d);
        }
        return sb.toString();
    }

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
            String pkgName = ClassUtil.getPackageName(compType);
            if (pkgName != null) {
                if (pkgName.contains(".cglib")) {
                    return pkgName.startsWith("net.sf.cglib")
                        // also, as per [JACKSON-177]
                        || pkgName.startsWith("org.hibernate.repackage.cglib")
                        // and [core#674]
                        || pkgName.startsWith("org.springframework.cglib");
                }
            }
        }
        return false;
    }

    // Another helper method to deal with Groovy's problematic metadata accessors
    private static boolean isGroovyMetaClassGetter(AnnotatedMethod am)
    {
        String pkgName = ClassUtil.getPackageName(am.getRawType());
        return (pkgName != null) && pkgName.startsWith("groovy.lang");
    }

    /*
    /**********************************************************************
    /* Standard Provider implementation
    /**********************************************************************
     */

    /**
     * Provider for {@link DefaultAccessorNamingStrategy}.
     */
    protected static class Provider
        extends AccessorNamingStrategy.Provider
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public AccessorNamingStrategy forPOJO(MapperConfig<?> config, AnnotatedClass ac,
                String mutatorPrefix) {
            return new DefaultAccessorNamingStrategy(config, ac, mutatorPrefix);
        }

        @Override
        public AccessorNamingStrategy forBuilder(MapperConfig<?> config, AnnotatedClass builderClass,
                AnnotatedClass targetClass, String mutatorPrefix)
        {
            return new DefaultAccessorNamingStrategy(config, builderClass, mutatorPrefix);
        }
    
    }
}
