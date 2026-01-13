package tools.jackson.databind.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.MapperConfig;

/**
 * Helper class that contains functionality needed by both serialization
 * and deserialization side.
 */
public class BeanUtil
{
    /*
    /**********************************************************************
    /* Name mangling
    /**********************************************************************
     */

    /**
     * @deprecated since 3.0.0-rc2 Use {@link tools.jackson.databind.introspect.DefaultAccessorNamingStrategy}
     *    instead
     */
    @Deprecated // since 3.0.0-rc2
    public static String stdManglePropertyName(final String basename, final int offset)
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
    /* Value defaulting helpers
    /**********************************************************************
     */

    /**
     * Accessor used to find out "default value" to use for comparing values to
     * serialize, to determine whether to exclude value from serialization with
     * inclusion type of {@link com.fasterxml.jackson.annotation.JsonInclude.Include#NON_DEFAULT}.
     *<p>
     * Default logic is such that for primitives, expected defaults (0 for `int`, `false` for
     * `boolean`) are returned; for primitive wrappers (`Integer`, `Boolean`, etc), `null` is
     * returned (since that is the default value for reference types); for Strings, empty String;
     * and for structured (Maps, Collections, arrays) and reference types, criteria
     * {@link com.fasterxml.jackson.annotation.JsonInclude.Include#NON_DEFAULT}
     * is used.
     *<p>
     * Changed in 3.1 updated to return {@code null} for wrapper types instead of primitive
     * defaults).
     *
     * @param type Type for which default value requested
     * @param wrappersAsNulls If {@code true}, default for primitive wrapper types
     *    like {@link java.lang.Boolean} will be {@code null}; if {@code false} will be
     *    wrapped default of matching primitive type (for {@code java.lang.Boolean} that
     *    would be {@code Boolean.FALSE})
     */
    public static Object getDefaultValue(JavaType type, boolean wrappersAsNulls)
    {
        // 06-Nov-2015, tatu: Returning null is fine for Object types; but need special
        //   handling for primitives since they are never passed as nulls.
        final Class<?> cls = type.getRawClass();

        // 11-Jan-2026, tatu: [databind#5570] Only use primitive defaults for actual
        //   primitive types, not wrapper types (which default to null)
        if (cls.isPrimitive()) {
            return ClassUtil.defaultValue(cls);
        }
        // as per above, for wrapper types (Integer, Boolean, etc.), default is null,
        // not the wrapped primitive default -- but does not need special casing
        /*
        Class<?> prim = ClassUtil.primitiveType(cls);
        if (prim != null) {
            return null;
        }
        */

        if (cls == String.class) {
            return "";
        }
        if (type.isContainerType() || type.isReferenceType()) {
            return JsonInclude.Include.NON_EMPTY;
        }
        // 09-Mar-2016, tatu: Not sure how far this path we want to go but for now
        //   let's add `java.util.Date` and `java.util.Calendar`, as per [databind#1550]
        if (type.isTypeOrSubTypeOf(Date.class)) {
            return new Date(0L);
        }
        if (type.isTypeOrSubTypeOf(Calendar.class)) {
            Calendar c = new GregorianCalendar();
            c.setTimeInMillis(0L);
            return c;
        }
        return null;
    }

    // @since 3.1
    public static Object getDefaultValue(JavaType type, DatabindContext ctxt)
    {
        return getDefaultValue(type,
                ctxt.isEnabled(MapperFeature.WRAPPERS_DEFAULT_TO_NULL));
    }

    // @since 3.1
    public static Object getDefaultValue(JavaType type, MapperConfig<?> config)
    {
        return getDefaultValue(type,
                config.isEnabled(MapperFeature.WRAPPERS_DEFAULT_TO_NULL));
    }
    
    /**
     * @deprecated Since 3.1 use one of overloads that take second argument instead
     */
    @Deprecated // since 3.1
    public static Object getDefaultValue(JavaType type) {
        return getDefaultValue(type, true);
    }
    
    /*
    /**********************************************************************
    /* Package-specific type detection for error handling
    /**********************************************************************
     */

    /**
     * Helper method called by {@link tools.jackson.databind.deser.BeanDeserializerFactory}
     * and {@link tools.jackson.databind.ser.BeanSerializerFactory} to check
     * if given unrecognized type (to be (de)serialized as general POJO) is one of
     * "well-known" types for which there would be a datatype module; and if so,
     * return appropriate failure message to give to caller.
     */
    public static String checkUnsupportedType(MapperConfig<?> config, JavaType type) {
        final String className = type.getRawClass().getName();
        String typeName, moduleName;

        if (isJodaTimeClass(className)) {
            typeName =  "Joda date/time";
            moduleName = "com.fasterxml.jackson.datatype:jackson-datatype-joda";
        } else {
            return null;
        }
        return String.format("%s type %s not supported by default: add Module \"%s\" to enable handling",
                typeName, ClassUtil.getTypeDescription(type), moduleName);
    }
    
    public static boolean isJodaTimeClass(Class<?> rawType) {
        return isJodaTimeClass(rawType.getName());
    }

    private static boolean isJodaTimeClass(String className) {
        return className.startsWith("org.joda.time.");
    }
}
