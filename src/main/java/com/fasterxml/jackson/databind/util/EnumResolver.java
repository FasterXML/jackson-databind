package com.fasterxml.jackson.databind.util;

import java.lang.reflect.Method;
import java.util.*;

import com.fasterxml.jackson.databind.AnnotationIntrospector;

/**
 * Helper class used to resolve String values (either JSON Object field
 * names or regular String values) into Java Enum instances.
 */
public class EnumResolver implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final Class<Enum<?>> _enumClass;

    protected final Enum<?>[] _enums;

    protected final HashMap<String, Enum<?>> _enumsById;

    protected final Enum<?> _defaultValue;

    protected EnumResolver(Class<Enum<?>> enumClass, Enum<?>[] enums, HashMap<String, Enum<?>> map, Enum<?> defaultValue)
    {
        _enumClass = enumClass;
        _enums = enums;
        _enumsById = map;
        _defaultValue = defaultValue;
    }

    /**
     * Factory method for constructing resolver that maps from Enum.name() into
     * Enum value
     */
    public static EnumResolver constructFor(Class<Enum<?>> enumCls, AnnotationIntrospector ai)
    {
        Enum<?>[] enumValues = enumCls.getEnumConstants();
        if (enumValues == null) {
            throw new IllegalArgumentException("No enum constants for class "+enumCls.getName());
        }
        String[] names = ai.findEnumValues(enumCls, enumValues, new String[enumValues.length]);
        HashMap<String, Enum<?>> map = new HashMap<String, Enum<?>>();
        for (int i = 0, len = enumValues.length; i < len; ++i) {
            String name = names[i];
            if (name == null) {
                name = enumValues[i].name();
            }
            map.put(name, enumValues[i]);
        }

        Enum<?> defaultEnum = ai.findDefaultEnumValue(enumCls);

        return new EnumResolver(enumCls, enumValues, map, defaultEnum);
    }

    /**
     * @deprecated Since 2.8, use {@link #constructUsingToString(Class, AnnotationIntrospector)} instead
     */
    @Deprecated
    public static EnumResolver constructUsingToString(Class<Enum<?>> enumCls) {
        return constructUsingToString(enumCls, null);
    }

    /**
     * Factory method for constructing resolver that maps from Enum.toString() into
     * Enum value
     *
     * @since 2.8
     */
    public static EnumResolver constructUsingToString(Class<Enum<?>> enumCls,
            AnnotationIntrospector ai)
    {
        Enum<?>[] enumValues = enumCls.getEnumConstants();
        HashMap<String, Enum<?>> map = new HashMap<String, Enum<?>>();
        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumValues.length; --i >= 0; ) {
            Enum<?> e = enumValues[i];
            map.put(e.toString(), e);
        }
        Enum<?> defaultEnum = (ai == null) ? null : ai.findDefaultEnumValue(enumCls);
        return new EnumResolver(enumCls, enumValues, map, defaultEnum);
    }

    /**
     * @deprecated Since 2.8, use {@link #constructUsingMethod(Class, Method, AnnotationIntrospector)} instead
     */
    @Deprecated
    public static EnumResolver constructUsingMethod(Class<Enum<?>> enumCls, Method accessor) {
        return constructUsingMethod(enumCls, accessor, null);
    }

    /**
     * @since 2.8
     */
    public static EnumResolver constructUsingMethod(Class<Enum<?>> enumCls, Method accessor,
            AnnotationIntrospector ai)
    {
        Enum<?>[] enumValues = enumCls.getEnumConstants();
        HashMap<String, Enum<?>> map = new HashMap<String, Enum<?>>();
        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumValues.length; --i >= 0; ) {
            Enum<?> en = enumValues[i];
            try {
                Object o = accessor.invoke(en);
                if (o != null) {
                    map.put(o.toString(), en);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to access @JsonValue of Enum value "+en+": "+e.getMessage());
            }
        }
        Enum<?> defaultEnum = (ai != null) ? ai.findDefaultEnumValue(enumCls) : null;
        return new EnumResolver(enumCls, enumValues, map, defaultEnum);
    }    

    /**
     * This method is needed because of the dynamic nature of constructing Enum
     * resolvers.
     */
    @SuppressWarnings({ "unchecked" })
    public static EnumResolver constructUnsafe(Class<?> rawEnumCls, AnnotationIntrospector ai)
    {            
        /* This is oh so wrong... but at least ugliness is mostly hidden in just
         * this one place.
         */
        Class<Enum<?>> enumCls = (Class<Enum<?>>) rawEnumCls;
        return constructFor(enumCls, ai);
    }

    /**
     * @deprecated Since 2.8, use {@link #constructUnsafeUsingToString(Class, AnnotationIntrospector)} instead
     */
    @Deprecated
    public static EnumResolver constructUnsafeUsingToString(Class<?> rawEnumCls)
    {
        return constructUnsafeUsingToString(rawEnumCls, null);
    }

    /**
     * Method that needs to be used instead of {@link #constructUsingToString}
     * if static type of enum is not known.
     *
     * @since 2.8
     */
    @SuppressWarnings({ "unchecked" })
    public static EnumResolver constructUnsafeUsingToString(Class<?> rawEnumCls,
            AnnotationIntrospector ai)
    {
        // oh so wrong... not much that can be done tho
        Class<Enum<?>> enumCls = (Class<Enum<?>>) rawEnumCls;
        return constructUsingToString(enumCls, ai);
    }

    /**
     * @deprecated Since 2.8, use {@link #constructUnsafeUsingMethod(Class, Method, AnnotationIntrospector)} instead.
     */
    @Deprecated
    public static EnumResolver constructUnsafeUsingMethod(Class<?> rawEnumCls, Method accessor) {
        return constructUnsafeUsingMethod(rawEnumCls, accessor, null);
    }

    /**
     * Method used when actual String serialization is indicated using @JsonValue
     * on a method.
     *
     * @since 2.8
     */
    @SuppressWarnings({ "unchecked" })
    public static EnumResolver constructUnsafeUsingMethod(Class<?> rawEnumCls, Method accessor,
            AnnotationIntrospector ai)
    {            
        // wrong as ever but:
        Class<Enum<?>> enumCls = (Class<Enum<?>>) rawEnumCls;
        return constructUsingMethod(enumCls, accessor, ai);
    }

    public CompactStringObjectMap constructLookup() {
        return CompactStringObjectMap.construct(_enumsById);
    }

    public Enum<?> findEnum(String key) { return _enumsById.get(key); }

    public Enum<?> getEnum(int index) {
        if (index < 0 || index >= _enums.length) {
            return null;
        }
        return _enums[index];
    }

    public Enum<?> getDefaultValue(){
        return _defaultValue;
    }

    public Enum<?>[] getRawEnums() {
        return _enums;
    }
    
    public List<Enum<?>> getEnums() {
        ArrayList<Enum<?>> enums = new ArrayList<Enum<?>>(_enums.length);
        for (Enum<?> e : _enums) {
            enums.add(e);
        }
        return enums;
    }

    /**
     * @since 2.7.3
     */
    public Collection<String> getEnumIds() {
        return _enumsById.keySet();
    }
    
    public Class<Enum<?>> getEnumClass() { return _enumClass; }

    public int lastValidIndex() { return _enums.length-1; }
}

