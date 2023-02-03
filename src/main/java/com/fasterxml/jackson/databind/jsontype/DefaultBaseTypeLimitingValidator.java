package com.fasterxml.jackson.databind.jsontype;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;

/**
 * {@link PolymorphicTypeValidator} that will only allow polymorphic handling if
 * the base type is NOT one of potential dangerous base types (see {@link #isUnsafeBaseType}
 * for specific list of such base types). No further validation is performed on subtype.
 *<p>
 * Note that when using potentially unsafe base type like {@link java.lang.Object} a custom
 * implementation (or subtype with override) is needed. Most commonly subclasses would
 * override both {@link #isUnsafeBaseType} and {@link #isSafeSubType}: former to allow
 * all (or just more) base types, and latter to add actual validation of subtype.
 *
 * @since 2.11
 */
public class DefaultBaseTypeLimitingValidator
    extends PolymorphicTypeValidator
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    @Override
    public Validity validateBaseType(MapperConfig<?> config, JavaType baseType)
    {
        // Immediately block potentially unsafe base types
        if (isUnsafeBaseType(config, baseType)) {
            return Validity.DENIED;
        }
        // otherwise indicate that type may be ok (so further calls are made --
        // does not matter with base implementation but allows easier sub-classing)
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubClassName(MapperConfig<?> config,
            JavaType baseType, String subClassName) {
        // return INDETERMINATE just for easier sub-classing
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubType(MapperConfig<?> config, JavaType baseType,
            JavaType subType)
    {
        return isSafeSubType(config, baseType, subType)
                ? Validity.ALLOWED
                : Validity.DENIED;
    }

    /**
     * Helper method called to determine if the given base type is known to be
     * problematic regarding possible "gadget types".
     * Currently includes following types:
     *<ul>
     *  <li>{@link java.lang.Object}</li>
     *  <li>{@link java.io.Closeable}</li>
     *  <li>{@link java.io.Serializable}</li>
     *  <li>{@link java.lang.AutoCloseable}</li>
     *  <li>{@link java.lang.Cloneable}</li>
     *  <li>{@link java.util.logging.Handler}</li>
     *  <li>{@link javax.naming.Referenceable}</li>
     *  <li>{@link javax.sql.DataSource}</li>
     *</ul>
     * which are JDK-included super types of at least one gadget type (not necessarily
     * included in JDK)
     *
     * @param config Current mapper configuration
     * @param baseType Base type to test
     */
    protected boolean isUnsafeBaseType(MapperConfig<?> config, JavaType baseType)
    {
        return UnsafeBaseTypes.instance.isUnsafeBaseType(baseType.getRawClass());
    }

    /**
     * Helper called to determine whether given actual subtype is considered safe
     * to process: this will only be called if subtype was considered acceptable
     * earlier.
     *
     * @param config Current mapper configuration
     * @param baseType Base type of sub type (validated earlier)
     * @param subType Sub type to test
     */
    protected boolean isSafeSubType(MapperConfig<?> config,
            JavaType baseType, JavaType subType)
    {
        return true;
    }

    private final static class UnsafeBaseTypes {
        public final static UnsafeBaseTypes instance = new UnsafeBaseTypes();

        private final Set<String> UNSAFE = new HashSet<>();
        {
            // first add names of types in `java.base`
            UNSAFE.add(Object.class.getName());
            UNSAFE.add(java.io.Closeable.class.getName());
            UNSAFE.add(java.io.Serializable.class.getName());
            UNSAFE.add(AutoCloseable.class.getName());
            UNSAFE.add(Cloneable.class.getName());

            // and then couple others typically included in JDK, but that we
            // prefer not adding direct reference to
            UNSAFE.add("java.util.logging.Handler");
            UNSAFE.add("javax.naming.Referenceable");
            UNSAFE.add("javax.sql.DataSource");
        }

        public boolean isUnsafeBaseType(Class<?> rawBaseType)
        {
            return UNSAFE.contains(rawBaseType.getName());
        }
    }
}
